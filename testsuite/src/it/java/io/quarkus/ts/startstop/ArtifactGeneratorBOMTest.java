/*
 * Copyright (c) 2020 Contributors to the Quarkus StartStop project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.quarkus.ts.startstop;

import io.quarkus.ts.startstop.utils.App;
import io.quarkus.ts.startstop.utils.Apps;
import io.quarkus.ts.startstop.utils.Commands;
import io.quarkus.ts.startstop.utils.Config;
import io.quarkus.ts.startstop.utils.FakeOIDCServer;
import io.quarkus.ts.startstop.utils.ITCommands;
import io.quarkus.ts.startstop.utils.ITContext;
import io.quarkus.ts.startstop.utils.ITLogs;
import io.quarkus.ts.startstop.utils.ITMvnCmd;
import io.quarkus.ts.startstop.utils.RunnerMvnCmd;
import io.quarkus.ts.startstop.utils.TestFlags;
import io.quarkus.ts.startstop.utils.WebpageTester;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static io.quarkus.ts.startstop.ArtifactGeneratorTest.supportedExtensionsSetA;
import static io.quarkus.ts.startstop.ArtifactGeneratorTest.supportedExtensionsSetB;
import static io.quarkus.ts.startstop.utils.Commands.getArtifactGeneBaseDir;
import static io.quarkus.ts.startstop.utils.ITCommands.cleanDir;
import static io.quarkus.ts.startstop.utils.ITCommands.confAppPropsForSkeleton;
import static io.quarkus.ts.startstop.utils.ITCommands.getBuildCommand;
import static io.quarkus.ts.startstop.utils.ITCommands.getLocalMavenRepoDir;
import static io.quarkus.ts.startstop.utils.ITCommands.getRunCommand;
import static io.quarkus.ts.startstop.utils.ITCommands.parsePort;
import static io.quarkus.ts.startstop.utils.ITCommands.processStopper;
import static io.quarkus.ts.startstop.utils.ITCommands.runCommand;
import static io.quarkus.ts.startstop.utils.ITCommands.waitForTcpClosed;

/**
 * @author Michal Karm Babacek <karm@redhat.com>
 */
@Tag("bomtests")
public class ArtifactGeneratorBOMTest {

    private static final Map<String, App> apps = Config.loadAppDefinitions("apps.yaml");

    private static final Logger LOGGER = Logger.getLogger(ArtifactGeneratorBOMTest.class.getName());

    public void testRuntime(TestInfo testInfo, String[] extensions, Set<TestFlags> flags) throws Exception {
        Process pA = null;
        File generateLog = null;
        File buildLogA = null;
        File runLogA = null;
        String repoDir = getLocalMavenRepoDir();
        String mn = testInfo.getTestMethod().get().getName();

        RunnerContext runnerContext = ITContext.testContext(Apps.GENERATED_SKELETON.dir
                , getArtifactGeneBaseDir()
                , testInfo.getTestClass().get().getCanonicalName()
                , testInfo.getTestMethod().get().getName()
        );

        String logsDir = runnerContext.getAppFullPath() + "-logs";

        List<String> generatorCmd = ITCommands.getGeneratorCommand(flags, ITMvnCmd.GENERATOR.cmds[0], extensions, repoDir);
        generatorCmd = generatorCmd.stream().map(cmd -> cmd.replaceAll("ARTIFACT_ID", Apps.GENERATED_SKELETON.dir)).collect(Collectors.toList());

        List<String> buildCmd = getBuildCommand(RunnerMvnCmd.JVM.cmds[0], repoDir);

        List<String> runCmd = getRunCommand(RunnerMvnCmd.JVM.cmds[1]);

        String  skeletonAppUrl = apps.get(Apps.GENERATED_SKELETON.dir).validationUrls().keySet().stream().findFirst().get();

        FakeOIDCServer fakeOIDCServer = new FakeOIDCServer(6661, "localhost");

        try {
            // Cleanup
            cleanDir(runnerContext.getAppFullPath(), logsDir);
            Files.createDirectories(Paths.get(logsDir));
            Files.createDirectories(Paths.get(repoDir));

            //Generator
            LOGGER.info(mn + ": Generator command " + String.join(" ", generatorCmd));
            generateLog = new File(logsDir + File.separator + "bom-artifact-generator.log");
            ExecutorService buildService = Executors.newFixedThreadPool(1);
            buildService.submit(new Commands.ProcessRunner( new File(runnerContext.getBaseDir()), generateLog, generatorCmd, 20));
            buildService.shutdown();
            buildService.awaitTermination(30, TimeUnit.MINUTES);

            runnerContext.getRuntimeAssertion().assertTrue(generateLog.exists());
            runnerContext.getLog().checkLog(apps.get(Apps.GENERATED_SKELETON.dir), ITMvnCmd.GENERATOR, generateLog, runnerContext);

            // Config, see app-generated-skeleton/README.md
            confAppPropsForSkeleton(runnerContext.getAppFullPath());

            // Build
            LOGGER.info(mn + ": Build command " + String.join(" ", buildCmd));
            buildLogA = new File(logsDir + File.separator + "bom-artifact-build.log");
            buildService = Executors.newFixedThreadPool(1);
            buildService.submit(new Commands.ProcessRunner(runnerContext.getAppFullPathFile(), buildLogA, buildCmd, 20));
            buildService.shutdown();
            buildService.awaitTermination(30, TimeUnit.MINUTES);

            runnerContext.getRuntimeAssertion().assertTrue(buildLogA.exists());
            runnerContext.getLog().checkLog(apps.get(Apps.GENERATED_SKELETON.dir), RunnerMvnCmd.JVM, buildLogA, runnerContext);

            // Run
            LOGGER.info(mn + ": Run command " + String.join(" ", RunnerMvnCmd.JVM.cmds[1]));
            LOGGER.info("Running...");
            runLogA = new File(logsDir + File.separator + "bom-artifact-run.log");
            pA = runCommand(runCmd, new File(runnerContext.getAppFullPath()), runLogA);

            // Test web pages
            WebpageTester.testWeb(skeletonAppUrl, 20,
                    apps.get(Apps.GENERATED_SKELETON.dir).validationUrls().get(skeletonAppUrl), false, runnerContext);

            LOGGER.info("Terminating test and scanning logs...");
            pA.getInputStream().available();
            runnerContext.getLog().checkLog(apps.get(Apps.GENERATED_SKELETON.dir), RunnerMvnCmd.JVM, runLogA, runnerContext);
            processStopper(pA, false);
            LOGGER.info("Gonna wait for ports closed after run...");
            // Release ports
            runnerContext.getRuntimeAssertion().assertTrue(waitForTcpClosed("localhost", parsePort(skeletonAppUrl), 60),
                    "Main port is still open after run");

            runnerContext.getLog().checkLog(apps.get(Apps.GENERATED_SKELETON.dir), RunnerMvnCmd.JVM, runLogA, runnerContext);

            ((ITLogs)runnerContext.getLog()).checkJarSuffixes(flags, new File(runnerContext.getAppFullPath()));
        } finally {
            fakeOIDCServer.stop();

            // Make sure processes are down even if there was an exception / failure
            if (pA != null) {
                processStopper(pA, true);
            }
            // Archive logs no matter what
            runnerContext.getLog().archiveLog(runnerContext, generateLog);
            if (buildLogA != null) {
                runnerContext.getLog().archiveLog(runnerContext, buildLogA);
            }
            if (runLogA != null) {
                runnerContext.getLog().archiveLog(runnerContext, runLogA);
            }
            cleanDir(runnerContext.getAppFullPath(), logsDir);
        }
    }

    @Test
    @Tag("community")
    public void quarkusBomExtensionsA(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedExtensionsSetA, EnumSet.of(TestFlags.QUARKUS_BOM));
    }

    @Test
    @Tag("community")
    public void quarkusBomExtensionsB(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedExtensionsSetB, EnumSet.of(TestFlags.QUARKUS_BOM));
    }

    @Test
    @Tag("product")
    public void quarkusProductBomExtensionsA(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedExtensionsSetA, EnumSet.of(TestFlags.PRODUCT_BOM));
    }

    @Test
    @Tag("product")
    public void quarkusProductBomExtensionsB(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedExtensionsSetB, EnumSet.of(TestFlags.PRODUCT_BOM));
    }

    @Test
    @Tag("community")
    public void quarkusUniverseBomExtensionsA(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedExtensionsSetA, EnumSet.of(TestFlags.UNIVERSE_BOM));
    }

    @Test
    @Tag("community")
    public void quarkusUniverseBomExtensionsB(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedExtensionsSetB, EnumSet.of(TestFlags.UNIVERSE_BOM));
    }

    @Test
    @Tag("product")
    public void quarkusUniverseProductBomExtensionsA(TestInfo testInfo) throws Exception {


        testRuntime(testInfo, supportedExtensionsSetA, EnumSet.of(TestFlags.UNIVERSE_PRODUCT_BOM));
    }

    @Test
    @Tag("product")
    public void quarkusUniverseProductBomExtensionsB(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedExtensionsSetB, EnumSet.of(TestFlags.UNIVERSE_PRODUCT_BOM));
    }
}

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

import io.quarkus.ts.startstop.context.BuildResult;
import io.quarkus.ts.startstop.context.RunResult;
import io.quarkus.ts.startstop.context.RunnerContext;
import io.quarkus.ts.startstop.utils.App;
import io.quarkus.ts.startstop.utils.Apps;
import io.quarkus.ts.startstop.utils.Commands;
import io.quarkus.ts.startstop.utils.Config;
import io.quarkus.ts.startstop.utils.FakeOIDCServer;
import io.quarkus.ts.startstop.utils.ITCommands;
import io.quarkus.ts.startstop.utils.ITContext;
import io.quarkus.ts.startstop.utils.ITLogs;
import io.quarkus.ts.startstop.utils.ITMvnCmd;
import io.quarkus.ts.startstop.utils.LogHandler;
import io.quarkus.ts.startstop.utils.RunnerMvnCmd;
import io.quarkus.ts.startstop.utils.SingleExecutorService;
import io.quarkus.ts.startstop.utils.TestFlags;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.quarkus.ts.startstop.ArtifactGeneratorTest.supportedExtensionsSubsetSetA;
import static io.quarkus.ts.startstop.ArtifactGeneratorTest.supportedExtensionsSubsetSetB;
import static io.quarkus.ts.startstop.utils.Commands.getArtifactGeneBaseDir;
import static io.quarkus.ts.startstop.utils.Commands.getRunCommand;


/**
 * @author Michal Karm Babacek <karm@redhat.com>
 */
@Tag("bomtests")
public class ArtifactGeneratorBOMTest {

    private static final Map<String, App> apps = Config.loadAppDefinitions("apps.yaml");

    private static final Logger LOGGER = Logger.getLogger(ArtifactGeneratorBOMTest.class.getName());

    public void testRuntime(TestInfo testInfo, String[] extensions, Set<TestFlags> flags) throws Exception {
        File generateLog = null;
        File buildLogA = null;
        File runLogA = null;

        String repoDir = ITCommands.getLocalMavenRepoDir();
        StringBuilder whatIDidReport = new StringBuilder();
        String cn = testInfo.getTestClass().get().getCanonicalName();
        String mn = testInfo.getTestMethod().get().getName();

        RunnerContext runnerContext = ITContext.testContext(Apps.GENERATED_SKELETON.dir
                , getArtifactGeneBaseDir()
                , cn
                , mn
        );

        LogHandler logHandler = runnerContext.getLogHandler();

        String logsDir = runnerContext.getAppFullPath() + "-logs";

        List<String> generatorCmd = ITCommands.getGeneratorCommand(flags, ITMvnCmd.GENERATOR.cmds[0], extensions, repoDir);
        generatorCmd = generatorCmd.stream().map(cmd -> cmd.replaceAll("ARTIFACT_ID", Apps.GENERATED_SKELETON.dir)).collect(Collectors.toList());

        List<String> buildCmd = ITCommands.getBuildCommand(RunnerMvnCmd.JVM.cmds[0], repoDir);

        List<String> runCmd = ITCommands.getRunCommand(RunnerMvnCmd.JVM.cmds[1]);

        App app = apps.get(Apps.GENERATED_SKELETON.dir);

        String  skeletonAppUrl = app.validationUrls().keySet().stream().findFirst().get();

        FakeOIDCServer fakeOIDCServer = new FakeOIDCServer(6661, "localhost");

        RunResult runResult = null;

        try {
            // Cleanup
            Commands.cleanDirOrFile(runnerContext.getAppFullPath(), logsDir);

            //Initialise Dirs
            Commands.createDirs(logsDir, repoDir);

            //Generator
            LOGGER.info(mn + ": Generator command " + String.join(" ", generatorCmd));
            generateLog = new File(logsDir + File.separator + "bom-artifact-generator.log");

            SingleExecutorService.execute(runnerContext.getBaseDir(), generateLog, generatorCmd);
            logHandler.appendln(whatIDidReport, "# " + cn + ", " + mn);
            logHandler.appendln(whatIDidReport, (new Date()).toString());
            logHandler.appendln(whatIDidReport, runnerContext.getBaseDir());
            logHandler.appendlnSection(whatIDidReport, String.join(" ", generatorCmd));

            runnerContext.getRuntimeAssertion().assertTrue(generateLog.exists());
            logHandler.checkLog(app, ITMvnCmd.GENERATOR, generateLog, runnerContext);

            // Config, see app-generated-skeleton/README.md
            ITCommands.confAppPropsForSkeleton(runnerContext.getAppFullPath());


            // Build
            LOGGER.info(mn + ": Build command " + String.join(" ", buildCmd));
            BuildResult buildResult = ITCommands.buildProject(
                    app
                    , new File(logsDir + File.separator + "bom-artifact-build.log")
                    , runnerContext.getAppFullPath()
                    , runnerContext
                    , RunnerMvnCmd.JVM
                    , buildCmd
            );
            logHandler.appendln(whatIDidReport, runnerContext.getAppFullPath());
            logHandler.appendlnSection(whatIDidReport, String.join(" ", buildCmd));

            // Run
            LOGGER.info(mn + ": Run command " + String.join(" ", RunnerMvnCmd.JVM.cmds[1]));
            runResult = ITCommands.runProject(
                    app
                    , new File(logsDir + File.separator + (flags.contains(TestFlags.WARM_UP) ? "warmup-dev-run.log" : "dev-run.log"))
                    , runnerContext
                    , ITMvnCmd.GENERATOR
                    , getRunCommand(RunnerMvnCmd.DEV.cmds[0])
                    , skeletonAppUrl
                    , flags.contains(TestFlags.WARM_UP)
            );
//            TODO:: FIX THIS
//            logHandler.appendln(whatIDidReport, appDir.getAbsolutePath());
            logHandler.appendlnSection(whatIDidReport, String.join(" ", runCmd));
            // Test web pages
//            WebpageTester.testWeb(skeletonApp.urlContent[0][0], 20,
//                    skeletonApp.urlContent[0][1], false);

            LOGGER.info("Terminating test and scanning logs...");
            runResult.getProcess().getInputStream().available();
            logHandler.checkLog(app, RunnerMvnCmd.JVM, runResult.getLogFile(), runnerContext);
            ITCommands.processStopper(runResult.getProcess(), false);
            LOGGER.info("Gonna wait for ports closed after run...");
            // Release ports
            runnerContext.getRuntimeAssertion().assertTrue(ITCommands.waitForTcpClosed("localhost", ITCommands.parsePort(skeletonAppUrl), 60),
                    "Main port is still open after run");

            logHandler.checkLog(app, RunnerMvnCmd.JVM, runResult.getLogFile(), runnerContext);

            ((ITLogs)logHandler).checkJarSuffixes(flags, new File(runnerContext.getAppFullPath()));
        } catch (Exception e){
            e.printStackTrace();
        }
        finally {
            fakeOIDCServer.stop();

            // Make sure processes are down even if there was an exception / failure
            if (runResult != null && runResult.getProcess() != null) {
                ITCommands.processStopper(runResult.getProcess(), true);
            }
            // Archive logs no matter what
            logHandler.archiveLog(runnerContext, generateLog);
            if (buildLogA != null) {
                logHandler.archiveLog(runnerContext, buildLogA);
            }
            if (runLogA != null) {
                logHandler.archiveLog(runnerContext, runLogA);
            }
            ITCommands.cleanDirOrFile(runnerContext.getAppFullPath(), logsDir);
            logHandler.writeReport(runnerContext, whatIDidReport.toString());
        }
    }

    @Test
    @Tag("community")
    public void quarkusBomExtensionsA(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedExtensionsSubsetSetA, EnumSet.of(TestFlags.QUARKUS_BOM));
    }

    @Test
    @Tag("community")
    public void quarkusBomExtensionsB(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedExtensionsSubsetSetB, EnumSet.of(TestFlags.QUARKUS_BOM));
    }

    @Test
    @Tag("product")
    public void quarkusProductBomExtensionsA(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedExtensionsSubsetSetA, EnumSet.of(TestFlags.PRODUCT_BOM));
    }

    @Test
    @Tag("product")
    public void quarkusProductBomExtensionsB(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedExtensionsSubsetSetB, EnumSet.of(TestFlags.PRODUCT_BOM));
    }

    @Test
    @Tag("community")
    public void quarkusUniverseBomExtensionsA(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedExtensionsSubsetSetA, EnumSet.of(TestFlags.UNIVERSE_BOM));
    }

    @Test
    @Tag("community")
    public void quarkusUniverseBomExtensionsB(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedExtensionsSubsetSetB, EnumSet.of(TestFlags.UNIVERSE_BOM));
    }

    @Test
    @Tag("product")
    public void quarkusUniverseProductBomExtensionsA(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedExtensionsSubsetSetA, EnumSet.of(TestFlags.UNIVERSE_PRODUCT_BOM));
    }

    @Test
    @Tag("product")
    public void quarkusUniverseProductBomExtensionsB(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedExtensionsSubsetSetB, EnumSet.of(TestFlags.UNIVERSE_PRODUCT_BOM));
    }
}

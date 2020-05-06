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
import io.quarkus.ts.startstop.utils.ITMvnCmd;
import io.quarkus.ts.startstop.utils.LogBuilder;
import io.quarkus.ts.startstop.utils.LogHandler;
import io.quarkus.ts.startstop.utils.RunnerMvnCmd;
import io.quarkus.ts.startstop.utils.TestFlags;
import io.quarkus.ts.startstop.context.TestRunnerContext;
import io.quarkus.ts.startstop.utils.WebpageTester;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import static io.quarkus.ts.startstop.utils.Logs.SKIP;


/**
 * @author Michal Karm Babacek <karm@redhat.com>
 */
@Tag("generator")
public class ArtifactGeneratorTest {

    private static final Map<String, App> apps = Config.loadAppDefinitions("apps.yaml");

    private static final Logger LOGGER = Logger.getLogger(ArtifactGeneratorTest.class.getName());

    public static final String[] supportedExtensionsSubsetSetA = new String[]{
            "agroal",
            "config-yaml",
            "core",
            "hibernate-orm",
            "hibernate-orm-panache",
            "hibernate-validator",
            "jackson",
            "jaxb",
            "jdbc-mysql",
            "jdbc-postgresql",
            "jsonb",
            "jsonp",
            "kafka-client",
            "logging-json",
            "narayana-jta",
            "oidc",
            "quarkus-quartz", // "quartz" is ambiguous with org.apache.camel.quarkus:camel-quarkus-quartz
            "reactive-pg-client",
            "rest-client",
            "resteasy",
            "resteasy-jackson",
            "resteasy-jaxb",
            "resteasy-jsonb",
            "scheduler",
            "spring-boot-properties",
            "smallrye-reactive-messaging-amqp",
            "spring-data-jpa",
            "spring-di",
            "spring-security",
            "spring-web",
            "undertow",
            "undertow-websockets",
            "vertx",
            "vertx-web",
    };

    public static final String[] supportedExtensionsSubsetSetB = new String[]{
            "agroal",
            "config-yaml",
            "core",
            "hibernate-orm",
            "hibernate-orm-panache",
            "hibernate-validator",
            "jackson",
            "jaxb",
            "jdbc-mariadb",
            "jdbc-mssql",
            "smallrye-context-propagation",
            "smallrye-fault-tolerance",
            "smallrye-health",
            "smallrye-jwt",
            "smallrye-metrics",
            "smallrye-openapi",
            "smallrye-opentracing",
            "smallrye-reactive-messaging",
            "smallrye-reactive-messaging-kafka",
            "smallrye-reactive-streams-operators",
            "spring-data-jpa",
            "spring-di",
            "spring-security",
            "spring-web",
    };

    public void testRuntime(TestInfo testInfo, String[] extensions, Set<TestFlags> flags) throws Exception {

        String mn = testInfo.getTestMethod().get().getName();
        String cn = testInfo.getTestClass().get().getCanonicalName();

        RunnerContext runnerContext = ITContext.testContext(Apps.GENERATED_SKELETON.dir
                , ITCommands.getArtifactGeneBaseDir()
                , cn
                , mn
        );

        LogHandler logHandler = runnerContext.getLogHandler();

        String logsDir = runnerContext.getAppFullPath() + "-logs";
        StringBuilder whatIDidReport = new StringBuilder();
        List<String> generatorCmd = ITCommands.getGeneratorCommand(ITMvnCmd.GENERATOR.cmds[0], extensions);
        generatorCmd = generatorCmd.stream().map(cmd -> cmd.replaceAll("ARTIFACT_ID", Apps.GENERATED_SKELETON.dir)).collect(Collectors.toList());

        String skeletonAppUrl = apps.get(Apps.GENERATED_SKELETON.dir).validationUrls().keySet().stream().findFirst().get();


        if (flags.contains(TestFlags.WARM_UP)) {
            LOGGER.info(mn + ": Warming up setup: " + String.join(" ", generatorCmd));
        } else {
            LOGGER.info(mn + ": Testing setup: " + String.join(" ", generatorCmd));
        }
        FakeOIDCServer fakeOIDCServer = new FakeOIDCServer(6661, "localhost");

        App app = apps.get(Apps.GENERATED_SKELETON.dir);

        RunResult runResult = null;
        BuildResult buildResult = null;


        try {
            // Cleanup
            ITCommands.cleanDirOrFile(runnerContext.getAppFullPath(), logsDir);

            //Initialise Dirs
            Commands.createDirs(logsDir);
            Files.createDirectories(Paths.get(logsDir));

            // Build
            logHandler.appendln(whatIDidReport, "# " + cn + ", " + mn + ", warmup run: " + flags.contains(TestFlags.WARM_UP));
            logHandler.appendln(whatIDidReport, (new Date()).toString());
            logHandler.appendln(whatIDidReport, runnerContext.getAppFullPath());
            logHandler.appendlnSection(whatIDidReport, String.join(" ", generatorCmd));
            long buildStarts = System.currentTimeMillis();
            buildResult = ITCommands.buildProject(
                    app
                    , new File(logsDir + File.separator + (flags.contains(TestFlags.WARM_UP) ? "warmup-artifact-build.log" : "artifact-build.log"))
                    , runnerContext.getBaseDir()
                    , runnerContext
                    , ITMvnCmd.GENERATOR
                    , generatorCmd
                    );
            long buildEnds = System.currentTimeMillis();

            // Config, see app-generated-skeleton/README.md
            String appFullPath = runnerContext.getAppFullPath();
            ITCommands.confAppPropsForSkeleton(appFullPath);

            // Run
            List<String> runCmd = ITCommands.getRunCommand(RunnerMvnCmd.DEV.cmds[0]);

            runResult = ITCommands.runProject(
                    app
                    , new File(logsDir + File.separator + (flags.contains(TestFlags.WARM_UP) ? "warmup-dev-run.log" : "dev-run.log"))
                    , runnerContext
                    , RunnerMvnCmd.DEV
                    , runCmd
                    , skeletonAppUrl
                    , flags.contains(TestFlags.WARM_UP)
            );

            logHandler.appendln(whatIDidReport, appFullPath);
            logHandler.appendlnSection(whatIDidReport, String.join(" ", runCmd));

            if (runResult == null){
                LOGGER.info("Finishing warm up...");

                return;
            }

            LOGGER.info("Testing reload...");

            Path srcFile = Paths.get(runnerContext.getAppFullPath() + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator +
                    "org" + File.separator + "my" + File.separator + "group" + File.separator + "MyResource.java");
            logHandler.appendlnSection(whatIDidReport, "Reloading class: " + srcFile.toAbsolutePath());
            try (Stream<String> src = Files.lines(srcFile)) {
                Files.write(srcFile, src.map(l -> l.replaceAll("hello", "bye")).collect(Collectors.toList()));
            }

            String skeletonAppByeUrl = apps.get(Apps.GENERATED_SKELETON.dir).validationUrls().keySet().stream().skip(1).findFirst().get();

            long timeToReloadedOKRequest = WebpageTester.testWeb(skeletonAppByeUrl, 60,
                    apps.get(Apps.GENERATED_SKELETON.dir).validationUrls().get(skeletonAppByeUrl), true, runnerContext);

            LOGGER.info("Terminate and scan logs...");
            runResult.getProcess().getInputStream().available();

            long rssKb = ITCommands.getRSSkB(runResult.getProcess().pid());
            long openedFiles = ITCommands.getOpenedFDs(runResult.getProcess().pid());

            ITCommands.processStopper(runResult.getProcess(), false);

            LOGGER.info("Gonna wait for ports closed...");
            // Release ports
            runnerContext.getRuntimeAssertion().assertTrue(ITCommands.waitForTcpClosed("localhost", ITCommands.parsePort(skeletonAppUrl), 60),
                    "Main port is still open");
            runnerContext.getLogHandler().checkLog(apps.get(Apps.GENERATED_SKELETON.dir), ITMvnCmd.GENERATOR, runResult.getLogFile(), runnerContext);

            float[] startedStopped = runnerContext.getLogHandler().parseStartStopTimestamps(runResult.getLogFile());

            Path measurementsLog = Paths.get(((TestRunnerContext)runnerContext).getArchiveDir(), runnerContext.getLogHandler().getLogsDir(runnerContext).toString(), "measurements.csv");
            LogBuilder.Log log = new LogBuilder()
                    .app(apps.get(Apps.GENERATED_SKELETON.dir))
                    .mode(ITMvnCmd.GENERATOR)
                    .buildTimeMs(buildResult.getBuildEnds() - buildResult.getBuildStarts())
                    .timeToFirstOKRequestMs(runResult.getTimeToFirstOKRequest())
                    .timeToReloadedOKRequest(timeToReloadedOKRequest)
                    .startedInMs((long) (startedStopped[0] * 1000))
                    .stoppedInMs((long) (startedStopped[1] * 1000))
                    .rssKb(rssKb)
                    .openedFiles(openedFiles)
                    .build();
            logHandler.logMeasurements(log, measurementsLog);
            logHandler.appendln(whatIDidReport, "Measurements:");
            logHandler.appendln(whatIDidReport, log.headerMarkdown + "\n" + log.lineMarkdown);
            runnerContext.getLogHandler().checkThreshold(apps.get(Apps.GENERATED_SKELETON.dir), ITMvnCmd.GENERATOR, SKIP, runResult.getTimeToFirstOKRequest(), timeToReloadedOKRequest, runnerContext);

        } finally {
            fakeOIDCServer.stop();

            if ( runResult != null) {
                // Make sure processes are down even if there was an exception / failure
                if (runResult.getProcess() != null) {
                    ITCommands.processStopper(runResult.getProcess(), true);
                }
            }
            // Archive logs no matter what
            if(buildResult != null) {
                runnerContext.getLogHandler().archiveLog(runnerContext, buildResult.getBuildLog());
            }
            if (runResult != null && runResult.getLogFile() != null) {
                // If build failed it is actually expected to have no runtime log.
                runnerContext.getLogHandler().archiveLog(runnerContext, runResult.getLogFile());
            }
            ITCommands.cleanDirOrFile(runnerContext.getAppFullPath(), logsDir);
            logHandler.writeReport(runnerContext, whatIDidReport.toString());
        }
    }

    @Test
    public void manyExtensionsSetA(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedExtensionsSubsetSetA, EnumSet.of(TestFlags.WARM_UP));
        testRuntime(testInfo, supportedExtensionsSubsetSetA, EnumSet.noneOf(TestFlags.class));
    }

    @Test
    public void manyExtensionsSetB(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedExtensionsSubsetSetB, EnumSet.of(TestFlags.WARM_UP));
        testRuntime(testInfo, supportedExtensionsSubsetSetB, EnumSet.noneOf(TestFlags.class));
    }
}

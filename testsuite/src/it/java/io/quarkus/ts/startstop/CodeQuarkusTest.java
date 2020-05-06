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
import io.quarkus.ts.startstop.utils.CodeQuarkusExtensions;
import io.quarkus.ts.startstop.utils.Config;
import io.quarkus.ts.startstop.utils.ITCommands;
import io.quarkus.ts.startstop.utils.ITContext;
import io.quarkus.ts.startstop.utils.LogHandler;
import io.quarkus.ts.startstop.utils.RunnerMvnCmd;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.quarkus.ts.startstop.utils.Commands.cleanDirOrFile;
import static io.quarkus.ts.startstop.utils.Commands.getArtifactGeneBaseDir;

/**
 * @author Michal Karm Babacek <karm@redhat.com>
 */
@Tag("codequarkus")
public class CodeQuarkusTest {


    private static final Map<String, App> apps = Config.loadAppDefinitions("apps.yaml");

    private static final Logger LOGGER = Logger.getLogger(CodeQuarkusTest.class.getName());

    public static final String GEN_BASE_DIR = getArtifactGeneBaseDir();

    public static final List<List<CodeQuarkusExtensions>> supportedEx = CodeQuarkusExtensions.partition(4, CodeQuarkusExtensions.Flag.SUPPORTED);
    public static final List<List<CodeQuarkusExtensions>> notSupportedEx = CodeQuarkusExtensions.partition(1, CodeQuarkusExtensions.Flag.NOT_SUPPORTED);
    public static final List<List<CodeQuarkusExtensions>> mixedEx = CodeQuarkusExtensions.partition(1, CodeQuarkusExtensions.Flag.MIXED);

    public void testRuntime(TestInfo testInfo, List<CodeQuarkusExtensions> extensions) throws Exception {
        File runLogA = null;

        StringBuilder whatIDidReport = new StringBuilder();
        String cn = testInfo.getTestClass().get().getCanonicalName();
        String mn = testInfo.getTestMethod().get().getName();

        RunnerContext runnerContext = ITContext.testContext(Apps.GENERATED_SKELETON.dir
                , ITCommands.getArtifactGeneBaseDir()
                , cn
                , mn
        );

        LogHandler logHandler = runnerContext.getLogHandler();


        String logsDir = runnerContext.getAppFullPath() + "-logs";


        String skeletonAppUrl = apps.get(Apps.GENERATED_SKELETON.dir).validationUrls().keySet().stream().findFirst().get();

        RunResult runResult = null;

        App app = apps.get(Apps.GENERATED_SKELETON.dir);

        List<String> devCmd = ITCommands.getGeneratorCommand(RunnerMvnCmd.DEV.cmds[0], extensions);

        LOGGER.info(mn + ": Testing Code Quarkus generator with these " + extensions.size() + " extensions: " + extensions.toString());
        File appDir = new File(GEN_BASE_DIR + File.separator + "code-with-quarkus");
        String zipFile = GEN_BASE_DIR + File.separator + "code-with-quarkus.zip";
        File unzipLog = ITCommands.unzip(zipFile, GEN_BASE_DIR);

        try {
            cleanDirOrFile(appDir.getAbsolutePath(), logsDir);
            Files.createDirectories(Paths.get(logsDir));
            logHandler.appendln(whatIDidReport, "# " + cn + ", " + mn);
            logHandler.appendln(whatIDidReport, (new Date()).toString());
            LOGGER.info("Downloading...");
            logHandler.appendln(whatIDidReport, "Download URL: " + ITCommands.download(extensions, zipFile));
            LOGGER.info("Unzipping...");
            LOGGER.info("Running command: " + devCmd + " in directory: " + appDir);
            logHandler.appendln(whatIDidReport, "Extensions: " + extensions.toString());
            logHandler.appendln(whatIDidReport, appDir.getAbsolutePath());
            logHandler.appendlnSection(whatIDidReport, String.join(" ", devCmd));

            runResult = ITCommands.runProject(
                    app
                    , new File(logsDir + File.separator + "dev-run.log")
                    , runnerContext
                    , RunnerMvnCmd.DEV
                    , devCmd
                    , skeletonAppUrl
                    , true
            );

        } finally {

            logHandler.archiveLog(runnerContext, unzipLog);
            logHandler.archiveLog(runnerContext, runLogA);
            logHandler.writeReport(runnerContext, whatIDidReport.toString());
            cleanDirOrFile(appDir.getAbsolutePath(), logsDir);
        }
    }

    @Test
    public void supportedExtensionsSubsetA(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedEx.get(0));
    }

    @Test
    public void supportedExtensionsSubsetB(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedEx.get(1));
    }

    @Test
    public void supportedExtensionsSubsetC(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedEx.get(2));
    }

    @Test
    public void supportedExtensionsSubsetD(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, supportedEx.get(3));
    }

    @Test
    public void notsupportedExtensionsSubset(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, notSupportedEx.get(0).subList(0, Math.min(10, mixedEx.get(0).size())));
    }

    @Test
    public void mixExtensions(TestInfo testInfo) throws Exception {
        testRuntime(testInfo, mixedEx.get(0).subList(0, Math.min(15, mixedEx.get(0).size())));
    }
}

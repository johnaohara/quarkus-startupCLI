package io.quarkus.ts.startstop;

import io.quarkus.ts.startstop.utils.App;
import io.quarkus.ts.startstop.utils.Commands;
import io.quarkus.ts.startstop.utils.LogBuilder;
import io.quarkus.ts.startstop.utils.Logs;
import io.quarkus.ts.startstop.utils.MvnCmd;
import io.quarkus.ts.startstop.utils.WebpageTester;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class StartStopRunner {
    private static final Logger LOGGER = Logger.getLogger(StartStopRunner.class.getName());

    public static void testStartup(App app, RunnerContext runnerContext, MvnCmd mvnCmd) throws IOException, InterruptedException {
        if (runnerContext.appDir == null){
            throw new IllegalArgumentException("App Directory has not been set");
        }
        if (mvnCmd == null){
            throw new IllegalArgumentException("Maven commands have not been defined for build");
        }

        Process pA = null;
        File buildLogA = null;
        File runLogA = null;
        File appDir = new File(runnerContext.getAppFullPath());

        try {
            // Cleanup
            Commands.cleanTarget(runnerContext);
            Files.createDirectories(Paths.get(appDir.getAbsolutePath() + File.separator + "logs"));

            // Build
            buildLogA = new File(appDir.getAbsolutePath() + File.separator + "logs" + File.separator + mvnCmd.name().toLowerCase() + "-build.log");
            ExecutorService buildService = Executors.newFixedThreadPool(1);
            buildService.submit(new Commands.ProcessRunner(appDir, buildLogA, Commands.getBuildCommand(mvnCmd.cmds()[0]), 20));
            long buildStarts = System.currentTimeMillis();
            buildService.shutdown();
            buildService.awaitTermination(30, TimeUnit.MINUTES);
            long buildEnds = System.currentTimeMillis();

            assert(buildLogA.exists());
            runnerContext.log.checkLog(app, mvnCmd, buildLogA, runnerContext);

            // Run
            LOGGER.info("Running...");
            runLogA = new File(appDir.getAbsolutePath() + File.separator + "logs" + File.separator + mvnCmd.name().toLowerCase() + "-run.log");
//            runLogA = new File(runnerContext.getLogsDir() + File.separator + app.getName().toLowerCase() + "-run.log");
            pA = Commands.runCommand(Commands.getRunCommand(mvnCmd.cmds()[1]), appDir, runLogA);

            // Test web pages
            String firstValidationUrl = app.validationUrls().keySet().stream().findFirst().get();
            long timeToFirstOKRequest = WebpageTester.testWeb(firstValidationUrl, 10, app.validationUrls().get(firstValidationUrl), true, runnerContext);
            LOGGER.info("Testing web page content...");
            app.validationUrls().keySet()
                    .stream()
                    .skip(1) //ski first url, we have already tested it
                    .forEach( url -> {
                        try {
                            WebpageTester.testWeb(url, 5, app.validationUrls().get(url), false, runnerContext);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });


            LOGGER.info("Terminate and scan logs...");
            pA.getInputStream().available();

            long rssKb = Commands.getRSSkB(pA.pid());
            long openedFiles = Commands.getOpenedFDs(pA.pid());

            Commands.processStopper(pA, false);

            LOGGER.info("Gonna wait for ports closed...");
            // Release ports
            runnerContext.runtimeAssertion.assertTrue(Commands.waitForTcpClosed("localhost", Commands.parsePort(firstValidationUrl), 60),
                    "Main port is still open");
            if(!Commands.waitForTcpClosed("localhost", Commands.parsePort(firstValidationUrl), 60)){
                LOGGER.warning("Main port is still open");
            }

            //TODO:: split out IT test log checker and normal log checker
            runnerContext.log.checkLog(app, mvnCmd, runLogA, runnerContext);

            float[] startedStopped = runnerContext.log.parseStartStopTimestamps(runLogA);

            Path measurementsLog = Paths.get(runnerContext.log.getLogsDir(runnerContext).toString(), "measurements.csv");
            LogBuilder.Log log = new LogBuilder()
                    .app(app)
                    .mode(mvnCmd)
                    .buildTimeMs(buildEnds - buildStarts)
                    .timeToFirstOKRequestMs(timeToFirstOKRequest)
                    .startedInMs((long) (startedStopped[0] * 1000))
                    .stoppedInMs((long) (startedStopped[1] * 1000))
                    .rssKb(rssKb)
                    .openedFiles(openedFiles)
                    .build();
            runnerContext.log.logMeasurements(log, measurementsLog);

            runnerContext.log.checkThreshold(app, mvnCmd, rssKb, timeToFirstOKRequest, Logs.SKIP, runnerContext);

        } catch (Exception e){
            LOGGER.severe("Fatal error occured:");
            e.printStackTrace();
        }
        finally {
            // Make sure processes are down even if there was an exception / failure
            if (pA != null) {
                Commands.processStopper(pA, true);
            }
            // Archive logs no matter what
            runnerContext.log.archiveLog(runnerContext, buildLogA);
            runnerContext.log.archiveLog(runnerContext, runLogA);
            Commands.cleanTarget(runnerContext);
        }
    }

}

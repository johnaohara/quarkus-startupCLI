package io.quarkus.ts.startstop;

import io.quarkus.ts.startstop.context.RunnerContext;
import io.quarkus.ts.startstop.utils.App;
import io.quarkus.ts.startstop.utils.Commands;
import io.quarkus.ts.startstop.utils.LogBuilder;
import io.quarkus.ts.startstop.utils.Logs;
import io.quarkus.ts.startstop.utils.MvnCmd;
import io.quarkus.ts.startstop.utils.SingleExecutorService;
import io.quarkus.ts.startstop.utils.WebpageTester;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class StartStopRunner {
    private static final Logger LOGGER = Logger.getLogger(StartStopRunner.class.getName());

    public static void testStartup(App app, RunnerContext runnerContext, MvnCmd mvnCmd) throws IOException, InterruptedException {
        if (runnerContext.getAppDir() == null){
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

            //Initialise Dirs
            Commands.createDirs(runnerContext.getLogsDir());

            // Build
            buildLogA = new File(runnerContext.getLogsDir() + File.separator + mvnCmd.name().toLowerCase() + "-build.log");
            long buildStarts = System.currentTimeMillis();

            SingleExecutorService.execute(runnerContext.getAppFullPath(), buildLogA, Commands.getBuildCommand(mvnCmd.cmds()[0]));

            long buildEnds = System.currentTimeMillis();

            assert(buildLogA.exists());
            runnerContext.getLog().checkLog(app, mvnCmd, buildLogA, runnerContext);

            // Run
            LOGGER.info("Running...");
            runLogA = new File(runnerContext.getLogsDir() + File.separator + mvnCmd.name().toLowerCase() + "-run.log");
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
            runnerContext.getRuntimeAssertion().assertTrue(Commands.waitForTcpClosed("localhost", Commands.parsePort(firstValidationUrl), 60),
                    "Main port is still open");
            if(!Commands.waitForTcpClosed("localhost", Commands.parsePort(firstValidationUrl), 60)){
                LOGGER.warning("Main port is still open");
            }

            //TODO:: split out IT test log checker and normal log checker
            runnerContext.getLog().checkLog(app, mvnCmd, runLogA, runnerContext);

            float[] startedStopped = runnerContext.getLog().parseStartStopTimestamps(runLogA);

            Path measurementsLog = Paths.get(runnerContext.getLog().getLogsDir(runnerContext).toString(), "measurements.csv");
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
            runnerContext.getLog().logMeasurements(log, measurementsLog);

            runnerContext.getLog().checkThreshold(app, mvnCmd, rssKb, timeToFirstOKRequest, Logs.SKIP, runnerContext);

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
            runnerContext.getLog().archiveLog(runnerContext, buildLogA);
            runnerContext.getLog().archiveLog(runnerContext, runLogA);
            Commands.cleanTarget(runnerContext);
        }
    }

}

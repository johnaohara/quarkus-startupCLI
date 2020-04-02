package io.quarkus.ts.startstop;

import io.quarkus.ts.startstop.utils.App;
import io.quarkus.ts.startstop.utils.Config;
import io.quarkus.ts.startstop.utils.Logs;
import io.quarkus.ts.startstop.utils.RunnerMvnCmd;
import io.quarkus.ts.startstop.utils.RunnerAssertions;
import org.aesh.AeshRuntimeRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class StartStopCli {

    public static void main(String[] args) {
        AeshRuntimeRunner.builder().interactive(true).command(StartStopCommand.class).args(args).execute();
    }

    @CommandDefinition(name = "startStop", description = "Run start-stop tests on a web application")
    public static class StartStopCommand implements Command {

        @Option(required = true, shortName = 'a', description = "Path to application src under test")
        private String applicationSrcPath;

        @Option(required = true, shortName = 'c', description = "Path to yaml configuration file for validation")
        private String configYaml;

        @Option(required = true, shortName = 'l', description = "Path to logging directory")
        private String logDir;

        @Option(required = true, shortName = 'm', description = "Modes to test")
        private List<String> modes;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {

            try {
                App app = Config.loadAppDefinition(new FileInputStream(new File(configYaml)));

                if (app == null) {
                    throw new IllegalArgumentException("Can not load configuration: " + configYaml);
                }

                File appFile = new File(applicationSrcPath);

                if (appFile == null){
                    throw new IllegalArgumentException("Can not find application: " + applicationSrcPath);
                }

                commandInvocation.println("Testing: " + applicationSrcPath);

                StartStopRunner startStopRunner = new StartStopRunner();

                RunnerContext runnerContext = RunnerContext.Builder.instance()
                        .logsDir(logDir)
                        .appDir(appFile.getParent())
                        .appFileName(appFile.getName())
                        .runtimeAssertion(new RunnerAssertions())
                        .log(Logs.instance())
                        .build();

                startStopRunner.testStartup(app
                        , runnerContext
                        , RunnerMvnCmd.JVM);


                return CommandResult.SUCCESS;
            } catch (IOException | InterruptedException fne) {

                commandInvocation.println("Exception occured: " + fne.getLocalizedMessage());

                return CommandResult.FAILURE;
            }
        }
    }

}

package io.quarkus.ts.startstop;

import io.quarkus.ts.startstop.utils.App;
import io.quarkus.ts.startstop.utils.Config;
import io.quarkus.ts.startstop.utils.Logs;
import io.quarkus.ts.startstop.utils.MvnCmd;
import io.quarkus.ts.startstop.utils.RunnerMvnCmd;
import io.quarkus.ts.startstop.utils.RunnerAssertions;
import org.aesh.AeshRuntimeRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.validator.OptionValidator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.validator.ValidatorInvocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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

        @Option(required = true, shortName = 'm', description = "Mode to test", converter = MvnModeConverter.class, validator = MvnModeConverter.class)
        private MvnCmd mode;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {

            try {
                App app = Config.loadAppDefinition(new FileInputStream(new File(configYaml)));

                if (app == null) {
                    throw new IllegalArgumentException("Can not load configuration: " + configYaml);
                }

                commandInvocation.println("Testing: " + applicationSrcPath);

                StartStopRunner startStopRunner = new StartStopRunner();

                RunnerContext runnerContext = RunnerContext.Builder.instance()
                        .logsDir(logDir)
                        .appDir(applicationSrcPath)
                        .runtimeAssertion(new RunnerAssertions())
                        .log(Logs.instance())
                        .build();

                startStopRunner.testStartup(app
                        , runnerContext
                        , mode);


                return CommandResult.SUCCESS;
            } catch (IOException | InterruptedException fne) {

                commandInvocation.println("Exception occured: " + fne.getLocalizedMessage());

                return CommandResult.FAILURE;
            }
        }
    }

    private class MvnModeConverter implements Converter<MvnCmd, ConverterInvocation>, OptionValidator {
        @Override
        public MvnCmd convert(ConverterInvocation input) {
            try {
                return RunnerMvnCmd.valueOf(input.getInput().toUpperCase());
            } catch (Exception e){
                return  null;
            }
        }

        @Override
        public void validate(ValidatorInvocation validatorInvocation) throws OptionValidatorException {
            if(validatorInvocation.getValue() == null){
                throw new OptionValidatorException("Invalid option for param: mode");
            }
        }
    }
}

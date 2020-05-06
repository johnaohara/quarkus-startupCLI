package io.quarkus.ts.startstop.utils;

import io.quarkus.ts.startstop.context.RunnerContext;
import io.quarkus.ts.startstop.context.TestRunnerContext;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

public class ITLogs extends Logs {

    private static final Logger LOGGER = Logger.getLogger(Logs.class.getName());

    public static final String jarSuffix = "redhat";

    protected ITLogs() {
    }

    public static ITLogs getInstance(){
        return new ITLogs();
    }

    public void checkJarSuffixes(Set<TestFlags> flags, File appDir) throws IOException {
        if (flags.contains(TestFlags.PRODUCT_BOM) || flags.contains(TestFlags.UNIVERSE_PRODUCT_BOM)) {
            List<Path> possiblyUnwantedArtifacts = super.listJarsFailingNameCheck(
                    appDir.getAbsolutePath() + File.separator + "target" + File.separator + "lib");
            List<String> reportArtifacts = new ArrayList<>();
            boolean containsNotWhitelisted = false;
            for (Path p : possiblyUnwantedArtifacts) {
                boolean found = false;
                for (String w : WhitelistProductBomJars.PRODUCT_BOM.jarNames) {
                    if (p.toString().contains(w)) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    reportArtifacts.add("WHITELISTED: " + p);
                } else {
                    containsNotWhitelisted = true;
                    reportArtifacts.add(p.toString());
                }
            }
            Assertions.assertFalse(containsNotWhitelisted, "There are not-whitelisted artifacts without expected string " + jarSuffix + " suffix, see: \n"
                    + String.join("\n", reportArtifacts));
            LOGGER.warn("There are whitelisted artifacts without expected string " + jarSuffix + " suffix, see: \n"
                    + String.join("\n", reportArtifacts));
        }
    }

    @Override
    public void checkLog(App app, MvnCmd cmd, File logFile, RunnerContext context) throws FileNotFoundException {
        try (Scanner sc = new Scanner(logFile)) {
            Set<String> offendingLines = new HashSet<>();
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                boolean error = line.matches("(?i:.*ERROR.*)");
                boolean whiteListed = false;
                if (error) {
                    if(app.getWhitelistLogLines() != null) {
                        for (String pattern : app.getWhitelistLogLines()) {
                            Pattern p = Pattern.compile(".*".concat(pattern).concat(".*"));
                            if (p.matcher(line).matches()) {
                                whiteListed = true;
                                LOGGER.info(cmd.name() + "log for " + ((TestRunnerContext) context).getTestMethod() + " contains whitelisted error: `" + line + "'");
                                break;
                            }
                        }
                        if (!whiteListed) {
                            offendingLines.add(line);
                        }
                    }
                }
                context.getRuntimeAssertion().assertTrue(offendingLines.isEmpty(),
                        cmd.name() + " log should not contain error or warning lines that are not whitelisted. " +
                                "See testsuite" + File.separator + "target" + File.separator + "archived-logs" +
                                File.separator + ((TestRunnerContext) context).getTestClass() + File.separator + ((TestRunnerContext) context).getTestMethod() +
                                File.separator + logFile.getName() +
                                " and check these offending lines: \n" + String.join("\n", offendingLines));            }
        }
    }
}

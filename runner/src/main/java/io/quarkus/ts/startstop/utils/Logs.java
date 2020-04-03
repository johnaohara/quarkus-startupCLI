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
package io.quarkus.ts.startstop.utils;

import io.quarkus.ts.startstop.RunnerContext;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.quarkus.ts.startstop.utils.Commands.isThisWindows;

/**
 * @author Michal Karm Babacek <karm@redhat.com>
 */
public class Logs implements Log {
    private static final Logger LOGGER = Logger.getLogger(Logs.class.getName());

    private static final Pattern jarNamePattern = Pattern.compile("^((?!" + jarSuffix + ").)*jar$");

    private static final Pattern startedPattern = Pattern.compile(".* started in ([0-9\\.]+)s.*", Pattern.DOTALL);
    private static final Pattern stoppedPattern = Pattern.compile(".* stopped in ([0-9\\.]+)s.*", Pattern.DOTALL);

    public static Logs instance(){
        return new Logs();
    }
    /*
     Due to console colouring, Windows has control characters in the sequence.
     So "1.778s" in "started in 1.778s." becomes  "[38;5;188m1.778".
     e.g.
     //started in [38;5;188m1.228[39ms.
     //stopped in [38;5;188m0.024[39ms[39m[38;5;203m[39m[38;5;227m

     Although when run from Jenkins service account; those symbols might not be present
     depending on whether you checked AllowInteractingWithDesktop.
     // TODO to make it smoother?
     */
    private static final Pattern startedPatternControlSymbols = Pattern.compile(".* started in .*188m([0-9\\.]+).*", Pattern.DOTALL);
    private static final Pattern stoppedPatternControlSymbols = Pattern.compile(".* stopped in .*188m([0-9\\.]+).*", Pattern.DOTALL);

    // TODO: How about WARNING? Other unwanted messages?
    @Override
    public void checkLog(App app, MvnCmd cmd, File log, RunnerContext context) throws FileNotFoundException {
        try (Scanner sc = new Scanner(log)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                boolean error = line.matches("(?i:.*ERROR.*)");
                boolean whiteListed = false;
                if (error) {
                    if(app.getWhitelistLogLines() != null) {
                        for (String w : app.getWhitelistLogLines()) {
                            if (line.contains(w)) {
                                whiteListed = true;
                                LOGGER.info(cmd.name() + "log for " + app.getName() + " contains whitelisted error: `" + line + "'");
                                break;
                            }
                        }
                    }
                }
                context.getRuntimeAssertion().assertFalse(error && !whiteListed, cmd.name() + " log should not contain `ERROR' lines that are not whitelisted. ");
            }
        }
    }


    @Override
    public void checkThreshold(App app, MvnCmd cmd, long rssKb, long timeToFirstOKRequest, long timeToReloadedOKRequest, RunnerContext context) {
        String propPrefix = isThisWindows ? "windows" : "linux";
        propPrefix += cmd.prefix();

        if (timeToFirstOKRequest != SKIP) {
            checkThreshold(app, cmd, context, propPrefix + ".time.to.first.ok.request.threshold.ms", timeToFirstOKRequest);
        }
        if (rssKb != SKIP) {
            checkThreshold(app, cmd, context, propPrefix + ".RSS.threshold.kB", rssKb);
        }
        if (timeToReloadedOKRequest != SKIP) {
            checkThreshold(app, cmd, context, propPrefix + ".time.to.reload.threshold.ms", timeToReloadedOKRequest);
        }
    }

    private void checkThreshold(App app, MvnCmd cmd, RunnerContext context, String proptery, long measurement){
        if( app.thresholds().containsKey(proptery) ) {
            long threshold = app.thresholds().get(proptery);
            context.getRuntimeAssertion().assertTrue(measurement <= threshold,
                    "Application " + app + " in " + cmd + " mode took " + measurement
                            + " ms to get the first OK request, which is over " +
                            threshold + " ms threshold.");
        } else {
            LOGGER.warning("Missing threshold definition: " + proptery);
        }
    }

    @Override
    public void archiveLog(RunnerContext runnerContext, File log) throws IOException {
        if (log == null || !log.exists()) {
            LOGGER.severe("log must be a valid, existing file. Skipping operation.");
            return;
        }
        if (StringUtils.isBlank(runnerContext.getLogsDir())) {
            throw new IllegalArgumentException("Log dir must not be blank");
        }
        Path destDir = getLogsDir(runnerContext);
//        Path destDir = getLogsDir(runnerContext.getLogsDir());
        Files.createDirectories(destDir);
        String filename = log.getName();
        Files.copy(log.toPath(), Paths.get(destDir.toString(), filename));
    }

    @Override
    public Path getLogsDir(RunnerContext runnerContext) throws IOException {
        Path destDir = new File(runnerContext.getLogsDir()).toPath();
        Files.createDirectories(destDir);
        return destDir;
    }

    @Override
    public Path getLogsDir(String testClass) throws IOException {
        Path destDir = new File(Environment.getBaseDir() + File.separator + "testsuite" + File.separator + "target" +
                File.separator + "archived-logs" + File.separator + testClass).toPath();
        Files.createDirectories(destDir);
        return destDir;
    }

    @Override
    public void logMeasurements(LogBuilder.Log log, Path path) throws IOException {
        if (Files.notExists(path)) {
            Files.write(path, (log.header + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        }
        Files.write(path, (log.line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        LOGGER.info("\n" + log.header + "\n" + log.line);
    }

    /**
     * List Jar file names failing regexp pattern check
     *
     * Note the pattern is hardcoded to look for jars not containing word 'redhat',
     * but it could be easily generalized if needed.
     *
     * @param path to the root of directory tree
     * @return list of offending jar paths
     */
    @Override
    public List<Path> listJarsFailingNameCheck(String path) throws IOException {
        return Files.find(Paths.get(path),
                500, //if this is not enough, something is broken anyway
                (filePath, fileAttr) -> fileAttr.isRegularFile() && jarNamePattern.matcher(filePath.getFileName().toString()).matches())
                .collect(Collectors.toList());
    }

    @Override
    public float[] parseStartStopTimestamps(File log) throws FileNotFoundException {
        float[] startedStopped = new float[]{-1f, -1f};
        try (Scanner sc = new Scanner(log)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();

                Matcher m = startedPatternControlSymbols.matcher(line);
                if (startedStopped[0] == -1f && m.matches()) {
                    startedStopped[0] = Float.parseFloat(m.group(1));
                    continue;
                }

                m = startedPattern.matcher(line);
                if (startedStopped[0] == -1f && m.matches()) {
                    startedStopped[0] = Float.parseFloat(m.group(1));
                    continue;
                }

                m = stoppedPatternControlSymbols.matcher(line);
                if (startedStopped[1] == -1f && m.matches()) {
                    startedStopped[1] = Float.parseFloat(m.group(1));
                    continue;
                }

                m = stoppedPattern.matcher(line);
                if (startedStopped[1] == -1f && m.matches()) {
                    startedStopped[1] = Float.parseFloat(m.group(1));
                }
            }
        }
        if (startedStopped[0] == -1f) {
            LOGGER.severe("Parsing start time from log failed. " +
                    "Might not be the right time to call this method. The process might have ben killed before it wrote to log." +
                    "Find " + log.getName() + " in your target dir.");
        }
        if (startedStopped[1] == -1f) {
            LOGGER.severe("Parsing stop time from log failed. " +
                    "Might not be the right time to call this method. The process might have been killed before it wrote to log." +
                    "Find " + log.getName() + " in your target dir.");
        }
        return startedStopped;
    }
}

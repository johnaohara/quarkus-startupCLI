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

import io.quarkus.ts.startstop.context.BuildResult;
import io.quarkus.ts.startstop.context.RunResult;
import io.quarkus.ts.startstop.context.RunnerContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Michal Karm Babacek <karm@redhat.com>
 */
public class Commands {
    private static final Logger LOGGER = Logger.getLogger(Commands.class.getName());

    public static final boolean isThisWindows = System.getProperty("os.name").matches(".*[Ww]indows.*");
    private static final Pattern numPattern = Pattern.compile("[ \t]*[0-9]+[ \t]*");
    private static final Pattern quarkusVersionPattern = Pattern.compile("[ \t]*<quarkus.version>([^<]*)</quarkus.version>.*");
    private static final String ARTIFACT_GENERATOR_WORKSPACE = "ARTIFACT_GENERATOR_WORKSPACE";
    private static final String MAVEN_REPO_LOCAL = "tests.maven.repo.local";

    public static String getArtifactGeneBaseDir() {
        String env = System.getenv().get(ARTIFACT_GENERATOR_WORKSPACE);
        if (StringUtils.isNotBlank(env)) {
            return env;
        }
        String sys = System.getProperty(ARTIFACT_GENERATOR_WORKSPACE);
        if (StringUtils.isNotBlank(sys)) {
            return sys;
        }
        return System.getProperty("java.io.tmpdir");
    }

    public static String getLocalMavenRepoDir() {
        String env = System.getenv().get(MAVEN_REPO_LOCAL);
        if (StringUtils.isNotBlank(env)) {
            return env;
        }
        String sys = System.getProperty(MAVEN_REPO_LOCAL);
        if (StringUtils.isNotBlank(sys)) {
            return sys;
        }
        return System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
    }

    public static String getQuarkusPlatformVersion(String appPath) {
        for (String p : new String[]{"QUARKUS_PLATFORM_VERSION", "quarkus.platform.version"}) {
            String env = System.getenv().get(p);
            if (StringUtils.isNotBlank(env)) {
                return env;
            }
            String sys = System.getProperty(p);
            if (StringUtils.isNotBlank(sys)) {
                return sys;
            }
        }
        LOGGER.warning("Failed to detect quarkus.platform.version/QUARKUS_PLATFORM_VERSION, defaulting to getQuarkusVersion().");
        return getQuarkusVersion(appPath);
    }

    public static String getQuarkusVersion(String appPath) {
        return getQuarkusVersion(null, appPath);
    }

    public static String getQuarkusVersion(String pomFilePath, String appPath) {
        for (String p : new String[]{"QUARKUS_VERSION", "quarkus.version"}) {
            String env = System.getenv().get(p);
            if (StringUtils.isNotBlank(env)) {
                return env;
            }
            String sys = System.getProperty(p);
            if (StringUtils.isNotBlank(sys)) {
                return sys;
            }
        }
        String failure = "Failed to determine quarkus.version. Check pom.xml, check env and sys vars QUARKUS_VERSION";
        if (pomFilePath == null && appPath != null) {
            pomFilePath = appPath + File.separator + "pom.xml";
        }

        if (pomFilePath != null) {
            try (Scanner sc = new Scanner(new File(pomFilePath))) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    Matcher m = quarkusVersionPattern.matcher(line);
                    if (m.matches()) {
                        return m.group(1);
                    }
                }
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException(failure);
            }
        }
        throw new IllegalArgumentException(failure);
    }

    public static void cleanTarget(RunnerContext runnerContext) {
        String target = runnerContext.getAppDir() + File.separator + "target";
        String logs = runnerContext.getAppDir() + File.separator + "logs";
        cleanDir(target, logs);
    }

    public static void cleanDir(String... dir) {
        for (String s : dir) {
            try {
                FileUtils.forceDelete(new File(s));
            } catch (IOException e) {
                //Silence is golden
            }
        }
    }

    public static void createDirs(String... dir) {
        for (String s : dir) {
            try {
                Files.createDirectories(Paths.get(s));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static BuildResult buildProject(App app, File logFile, String targetDir, RunnerContext runnerContext, MvnCmd mvnCmd, List<String> cmd) throws FileNotFoundException, InterruptedException {
        BuildResult result = new BuildResult();
        result.setBuildLog(logFile);

        try {
            SingleExecutorService.execute(targetDir, logFile, cmd);
        } catch (InterruptedException e) {
            //TODO:: handle interupted thread correctly
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }

        result.setBuildEnds(System.currentTimeMillis());

        runnerContext.getRuntimeAssertion().assertTrue(logFile.exists());
        runnerContext.getLogHandler().checkLog(app, mvnCmd, logFile, runnerContext);

        return result;
    }

    public static RunResult runProject(App app, File runLog, RunnerContext runnerContext, MvnCmd mvnCmd, List<String> cmd, String skeletonAppUrl, boolean warmUp) throws IOException, InterruptedException {

        LOGGER.info("Running...");

        RunResult runResult = new RunResult();
        runResult.setLogFile(runLog);
        // Run
        runResult.setProcess(runCommand(cmd, new File(runnerContext.getAppFullPath()), runLog));

        // Test web pages
        // The reason for a seemingly large timeout of 20 minutes is that dev mode will be downloading the Internet on the first fresh run.
        long timeoutS = warmUp ? 20 * 60 : 60;
        runResult.setTimeToFirstOKRequest(WebpageTester.testWeb(skeletonAppUrl, timeoutS,
                app.validationUrls().get(skeletonAppUrl), true, runnerContext));


        if (warmUp) {
            LOGGER.info("Terminating warmup and scanning logs...");
            runResult.getProcess().getInputStream().available();
            runnerContext.getLogHandler().checkLog(app, mvnCmd, runLog, runnerContext);
            processStopper(runResult.getProcess(), false);
            LOGGER.info("Gonna wait for ports closed after warmup...");
            // Release ports
            runnerContext.getRuntimeAssertion().assertTrue(waitForTcpClosed("localhost", parsePort(skeletonAppUrl), 60),
                    "Main port is still open after warmup");
            runnerContext.getLogHandler().checkLog(app, mvnCmd, runLog, runnerContext);
            return null;
        }

        return runResult;

    }
    public static List<String> getRunCommand(String[] baseCommand) {
        List<String> runCmd = new ArrayList<>();
        if (isThisWindows) {
            runCmd.add("cmd");
            runCmd.add("/C");
        }
        runCmd.addAll(Arrays.asList(baseCommand));

        return Collections.unmodifiableList(runCmd);
    }

    public static List<String> getBuildCommand(String[] baseCommand) {
        List<String> buildCmd = new ArrayList<>();
        if (isThisWindows) {
            buildCmd.add("cmd");
            buildCmd.add("/C");
        }
        buildCmd.addAll(Arrays.asList(baseCommand));
        buildCmd.add("-Dmaven.repo.local=" + getLocalMavenRepoDir());

        return Collections.unmodifiableList(buildCmd);
    }

    public static List<String> getBuildCommand(String[] baseCommand, String repoDir) {
        List<String> buildCmd = new ArrayList<>();
        if (isThisWindows) {
            buildCmd.add("cmd");
            buildCmd.add("/C");
        }
        buildCmd.addAll(Arrays.asList(baseCommand));
        buildCmd.add("-Dmaven.repo.local=" + repoDir);
//        buildCmd.add("--settings=" + Environment.getBaseDir() + File.separator + Apps.GENERATED_SKELETON.dir + File.separator + "settings.xml");

        return Collections.unmodifiableList(buildCmd);
    }

    public static List<String> getGeneratorCommand(String[] baseCommand, String[] extensions) {
        List<String> generatorCmd = new ArrayList<>();
        if (isThisWindows) {
            generatorCmd.add("cmd");
            generatorCmd.add("/C");
        }
        generatorCmd.addAll(Arrays.asList(baseCommand));
        generatorCmd.add("-Dextensions=" + String.join(",", extensions));
        generatorCmd.add("-Dmaven.repo.local=" + getLocalMavenRepoDir());

        return Collections.unmodifiableList(generatorCmd);
    }

    public static boolean waitForTcpClosed(String host, int port, long loopTimeoutS) throws InterruptedException, UnknownHostException {
        InetAddress address = InetAddress.getByName(host);
        long now = System.currentTimeMillis();
        long startTime = now;
        InetSocketAddress socketAddr = new InetSocketAddress(address, port);
        while (now - startTime < 1000 * loopTimeoutS) {
            try (Socket socket = new Socket()) {
                // If it let's you write something there, it is still ready.
                socket.connect(socketAddr, 1000);
                socket.setSendBufferSize(1);
                socket.getOutputStream().write(1);
                socket.shutdownInput();
                socket.shutdownOutput();
                LOGGER.info("Socket still available: " + host + ":" + port);
            } catch (IOException e) {
                // Exception thrown - socket is likely closed.
                return true;
            }
            Thread.sleep(1000);
            now = System.currentTimeMillis();
        }
        return false;
    }

    public static int parsePort(String url) {
        return Integer.parseInt(url.split(":")[2].split("/")[0]);
    }

    public static Process runCommand(List<String> command, File directory, File logFile) {
        StringBuilder cmdBuilder = new StringBuilder();
        command.stream().forEach(cmd -> cmdBuilder.append(cmd).append(" "));

        ProcessBuilder pa = new ProcessBuilder(command);
        Map<String, String> envA = pa.environment();
        envA.put("PATH", System.getenv("PATH"));
        pa.directory(directory);
        pa.redirectErrorStream(true);
        pa.redirectOutput(ProcessBuilder.Redirect.to(logFile));
        Process pA = null;
        try {
            LOGGER.info("Running process: " + cmdBuilder.toString());
            LOGGER.info("In Dir: " + directory);
            pA = pa.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pA;
    }

    public static void pidKiller(long pid, boolean force) {
        try {
            if (isThisWindows) {
                if (!force) {
                    Process p = Runtime.getRuntime().exec(new String[]{
                            Environment.getBaseDir() + File.separator + "testsuite" + File.separator + "src" + File.separator + "it" + File.separator + "resources" + File.separator +
                                    "CtrlC.exe ", Long.toString(pid)});
                    p.waitFor(1, TimeUnit.MINUTES);
                }
                Runtime.getRuntime().exec(new String[]{"cmd", "/C", "taskkill", "/PID", Long.toString(pid), "/F", "/T"});
            } else {
                Runtime.getRuntime().exec(new String[]{"kill", force ? "-9" : "-15", Long.toString(pid)});
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.severe(e.getMessage());
        }
    }

    public static long getRSSkB(long pid) throws IOException, InterruptedException {
        ProcessBuilder pa;
        if (isThisWindows) {
            // Note that PeakWorkingSetSize might be better, but we would need to change it on Linux too...
            // https://docs.microsoft.com/en-us/windows/win32/cimwin32prov/win32-process
            pa = new ProcessBuilder("wmic", "process", "where", "processid=" + pid, "get", "WorkingSetSize");
        } else {
            pa = new ProcessBuilder("ps", "-p", Long.toString(pid), "-o", "rss=");
        }
        Map<String, String> envA = pa.environment();
        envA.put("PATH", System.getenv("PATH"));
        pa.redirectErrorStream(true);
        Process p = pa.start();
        try (BufferedReader processOutputReader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String l;
            while ((l = processOutputReader.readLine()) != null) {
                if (numPattern.matcher(l).matches()) {
                    if (isThisWindows) {
                        // Qualifiers: DisplayName ("Working Set Size"), Units ("bytes")
                        return Long.parseLong(l.trim()) / 1024L;
                    } else {
                        return Long.parseLong(l.trim());
                    }
                }
            }
            p.waitFor();
        }
        return -1L;
    }

    public static long getOpenedFDs(long pid) throws IOException, InterruptedException {
        ProcessBuilder pa;
        long count = 0;
        if (isThisWindows) {
            pa = new ProcessBuilder("wmic", "process", "where", "processid=" + pid, "get", "HandleCount");
        } else {
            pa = new ProcessBuilder("lsof", "-F0n", "-p", Long.toString(pid));
        }
        Map<String, String> envA = pa.environment();
        envA.put("PATH", System.getenv("PATH"));
        pa.redirectErrorStream(true);
        Process p = pa.start();
        try (BufferedReader processOutputReader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            if (isThisWindows) {
                String l;
                // TODO: We just get a magical number with all FDs... Is it O.K.?
                while ((l = processOutputReader.readLine()) != null) {
                    if (numPattern.matcher(l).matches()) {
                        return Long.parseLong(l.trim());
                    }
                }
            } else {
                // TODO: For the time being we count apples and oranges; we might want to distinguish .so and .jar ?
                while (processOutputReader.readLine() != null) {
                    count++;
                }
            }
            p.waitFor();
        }
        return count;
    }

    /*
    TODO: CPU cycles used

    Pros: good data
    Cons: dependency on perf tool; will not translate to Windows data

    karm@local:~/workspaceRH/fooBar$ perf stat java -jar target/fooBar-1.0.0-SNAPSHOT-runner.jar
    2020-02-25 16:07:00,870 INFO  [io.quarkus] (main) fooBar 1.0.0-SNAPSHOT (running on Quarkus 999-SNAPSHOT) started in 0.776s.
    2020-02-25 16:07:00,873 INFO  [io.quarkus] (main) Profile prod activated.
    2020-02-25 16:07:00,873 INFO  [io.quarkus] (main) Installed features: [amazon-lambda, cdi, resteasy]
    2020-02-25 16:07:03,360 INFO  [io.quarkus] (main) fooBar stopped in 0.018s

    Performance counter stats for 'java -jar target/fooBar-1.0.0-SNAPSHOT-runner.jar':

       1688.910052      task-clock:u (msec)       #    0.486 CPUs utilized
                 0      context-switches:u        #    0.000 K/sec
                 0      cpu-migrations:u          #    0.000 K/sec
            12,865      page-faults:u             #    0.008 M/sec
     4,274,799,448      cycles:u                  #    2.531 GHz
     4,325,761,598      instructions:u            #    1.01  insn per cycle
       919,713,769      branches:u                #  544.561 M/sec
        29,310,015      branch-misses:u           #    3.19% of all branches

       3.473028811 seconds time elapsed
    */

    public static void processStopper(Process p, boolean force) throws InterruptedException, IOException {
        p.children().forEach(child -> {
            if (child.supportsNormalTermination()) {
                child.destroy();
            }
            pidKiller(child.pid(), force);
        });
        if (p.supportsNormalTermination()) {
            p.destroy();
            p.waitFor(3, TimeUnit.MINUTES);
        }
        pidKiller(p.pid(), force);
    }

    public static class ProcessRunner implements Runnable {
        final File directory;
        final File log;
        final List<String> command;
        final long timeoutMinutes;

        public ProcessRunner(File directory, File log, List<String> command, long timeoutMinutes) {
            this.directory = directory;
            this.log = log;
            this.command = command;
            this.timeoutMinutes = timeoutMinutes;
        }

        @Override
        public void run() {
            Process p = Commands.runCommand(command, this.directory, this.log);
            try {
                Objects.requireNonNull(p).waitFor(timeoutMinutes, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

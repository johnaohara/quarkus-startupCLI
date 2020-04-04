package io.quarkus.ts.startstop.utils;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SingleExecutorService {

    public static void execute(String targetDir, File logFile, List<String> cmd) throws InterruptedException {
        ExecutorService buildService = Executors.newFixedThreadPool(1);
        buildService.submit(new Commands.ProcessRunner( new File(targetDir), logFile, cmd, 20));
        buildService.shutdown();
        buildService.awaitTermination(30, TimeUnit.MINUTES);

    }
}

package io.quarkus.ts.startstop.utils;

import io.quarkus.ts.startstop.context.RunnerContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface LogHandler {
    String jarSuffix = "redhat";
    long SKIP = -1L;

    // TODO: How about WARNING? Other unwanted messages?
    void checkLog(App app, MvnCmd mvnCmd, File log, RunnerContext context) throws FileNotFoundException;

    void checkThreshold(App app, MvnCmd cmd, long rssKb, long timeToFirstOKRequest, long timeToReloadedOKRequest, RunnerContext context);

    void archiveLog(RunnerContext context, File log) throws IOException;

    void writeReport(RunnerContext context, String text) throws IOException;

    void appendln(StringBuilder s, String text);

    void appendlnSection(StringBuilder s, String text);

    Path getArchiveLogsDir(RunnerContext context) throws IOException;

    Path getLogsDir(RunnerContext context) throws IOException;

    Path getLogsDir(String testClass) throws IOException;

    Path getLogsDir(String testClass, String testMethod) throws IOException;

    void logMeasurements(LogBuilder.Log log, Path path) throws IOException;

    List<Path> listJarsFailingNameCheck(String path) throws IOException;

    float[] parseStartStopTimestamps(File log) throws FileNotFoundException;

}

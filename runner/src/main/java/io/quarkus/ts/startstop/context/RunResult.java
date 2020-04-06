package io.quarkus.ts.startstop.context;

import java.io.File;

public class RunResult {
    private Process process;
    private long timeToFirstOKRequest;
    private File logFile;

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public long getTimeToFirstOKRequest() {
        return timeToFirstOKRequest;
    }

    public void setTimeToFirstOKRequest(long timeToFirstOKRequest) {
        this.timeToFirstOKRequest = timeToFirstOKRequest;
    }

    public File getLogFile() {
        return logFile;
    }

    public void setLogFile(File logFile) {
        this.logFile = logFile;
    }
}

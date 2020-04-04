package io.quarkus.ts.startstop.context;

public class RunResult {
    private Process process;
    private long timeToFirstOKRequest;

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
}

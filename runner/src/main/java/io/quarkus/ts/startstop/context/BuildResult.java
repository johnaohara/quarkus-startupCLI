package io.quarkus.ts.startstop.context;

import java.io.File;

public class BuildResult {
    long buildStarts;
    long buildEnds;
    File buildLog;


    public long getBuildStarts() {
        return buildStarts;
    }

    public void setBuildStarts(long buildStarts) {
        this.buildStarts = buildStarts;
    }

    public long getBuildEnds() {
        return buildEnds;
    }

    public void setBuildEnds(long buildEnds) {
        this.buildEnds = buildEnds;
    }

    public File getBuildLog() {
        return buildLog;
    }

    public void setBuildLog(File buildLog) {
        this.buildLog = buildLog;
    }
}

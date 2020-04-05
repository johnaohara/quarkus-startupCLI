package io.quarkus.ts.startstop.context;

public class BuildResult {
    long buildStarts;
    long buildEnds;


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
}

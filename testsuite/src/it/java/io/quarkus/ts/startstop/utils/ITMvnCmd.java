package io.quarkus.ts.startstop.utils;

import static io.quarkus.ts.startstop.utils.Commands.getQuarkusVersion;

public enum ITMvnCmd implements MvnCmd{

    GENERATOR(new String[][]{
            new String[]{
                    "mvn",
                    "io.quarkus:quarkus-maven-plugin:" + getQuarkusVersion() + ":create",
                    "-DprojectGroupId=my-groupId",
                    "-DprojectArtifactId=ARTIFACT_ID", // + Apps.GENERATED_SKELETON.dir,
                    "-DprojectVersion=1.0.0-SNAPSHOT",
                    "-DclassName=org.my.group.MyResource"
            }
    }
    ,".generated.dev");

    public final String[][] cmds;
    public final String prefix;

    ITMvnCmd(String[][] mvnCmds, String prefix) {
        this.cmds = mvnCmds; this.prefix = prefix;
    }

    @Override
    public String[][] cmds() {
        return cmds;
    }

    @Override
    public String prefix() {
        return prefix;
    }
}

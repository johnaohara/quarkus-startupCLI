package io.quarkus.ts.startstop.utils;

public interface MvnCmd {

    String[][] cmds();
    String prefix();
    String name();
}

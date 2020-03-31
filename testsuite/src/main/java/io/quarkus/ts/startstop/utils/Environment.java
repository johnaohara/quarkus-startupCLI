package io.quarkus.ts.startstop.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class Environment {

    public static String getBaseDir() {
        String env = System.getenv().get("basedir");
        String sys = System.getProperty("basedir");
        if (StringUtils.isNotBlank(env)) {
            return new File(env).getParent();
        }
        if (StringUtils.isBlank(sys)) {
            throw new IllegalArgumentException("Unable to determine project.basedir.");
        }
        return new File(sys).getParent();
    }
}

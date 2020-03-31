package io.quarkus.ts.startstop.utils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class App {
    private String name;
    private String dir;
    private List<String> modes;
    private List<String> whitelistLogLines;
    private Map<String, String> validationUrls;
    private Map<String, Long> thresholds;

    public App() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getWhitelistLogLines() {
        return whitelistLogLines;
    }

    public void setWhitelistLogLines(List<String> whitelistLogLines) {
        this.whitelistLogLines = whitelistLogLines;
    }

    public Map<String, String> validationUrls() {
        return validationUrls;
    }

    public void setValidationUrls(Map<String, String> validationUrls) {
        this.validationUrls = validationUrls;
    }

    public Map<String, Long> thresholds() {
        return thresholds;
    }

    public void setThresholds(Map<String, Long> thresholds) {
        this.thresholds = thresholds;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public List<String> modes() {
        return modes;
    }

    public void setModes(List<String> modes) {
        this.modes = modes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        App app = (App) o;
        return Objects.equals(name, app.name) &&
                Objects.equals(dir, app.dir) &&
                Objects.equals(whitelistLogLines, app.whitelistLogLines) &&
                Objects.equals(validationUrls, app.validationUrls) &&
                Objects.equals(thresholds, app.thresholds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, dir, whitelistLogLines, validationUrls, thresholds);
    }

    @Override
    public String toString() {
        return "App{" +
                "name='" + name + '\'' +
                ", dir='" + dir + '\'' +
                ", whitelistLogLines=" + whitelistLogLines +
                ", validationUrls=" + validationUrls +
                ", thresholds=" + thresholds +
                '}';
    }
}

package io.quarkus.ts.startstop.context;

import io.quarkus.ts.startstop.utils.LogHandler;
import io.quarkus.ts.startstop.utils.Assertions;

import java.io.File;

public class RunnerContext {
    protected final String baseDir;
    protected final String appDir;

    protected final String logsDir;
    protected final String archiveDir;
    protected final Assertions runtimeAssertion;
    protected final LogHandler logHandler;

    protected final boolean archiveLogs;

    protected RunnerContext(String appDir
            , String baseDir
            , String logsDir
            , String archiveDir
            , Assertions runtimeAssertion
            , LogHandler logHandler
            , boolean archiveLogs) {

        this.appDir = appDir;
        this.baseDir = baseDir;
        this.logsDir = logsDir;
        this.archiveDir = archiveDir;
        this.runtimeAssertion = runtimeAssertion;
        this.logHandler = logHandler;
        this.archiveLogs = archiveLogs;
    }

    public String getAppDir() {
        return this.appDir;
    }

    public String getBaseDir() {
        return this.baseDir;
    }

    public String getLogsDir() {
        return logsDir;
    }

    public String getArchiveDir() {
        return archiveDir;
    }

    public Assertions getRuntimeAssertion() {
        return runtimeAssertion;
    }

    public LogHandler getLogHandler() {
        return logHandler;
    }

    public String getAppFullPath() {
        //TODO:: what scenarios do we have a null base dir?
        if (this.baseDir == null) {
            return this.appDir;
        }
        return this.baseDir + File.separator + this.appDir;
    }

    public boolean archiveLogs() {
        return this.archiveLogs;
    }


    public static class Builder {

        protected String appDir;
        protected String baseDir;
        protected String logsDir;
        protected String archiveDir;
        protected Assertions runtimeAssertion;
        protected LogHandler logHandler;
        protected boolean archiveLogs;

        public static Builder instance() {
            return new Builder();
        }

        public RunnerContext.Builder appDir(String appDir) {
            this.appDir = appDir;
            return this;
        }

        public RunnerContext.Builder logsDir(String logsDir) {
            this.logsDir = logsDir;
            return this;
        }

        public RunnerContext.Builder runtimeAssertion(Assertions runtimeAssertion) {
            this.runtimeAssertion = runtimeAssertion;
            return this;
        }

        public RunnerContext.Builder log(LogHandler log) {
            this.logHandler = log;
            return this;
        }

        public RunnerContext.Builder baseDir(String baseDir) {
            this.baseDir = baseDir;
            return this;
        }

        public RunnerContext.Builder archiveDir(String archiveDir) {
            this.archiveDir = archiveDir;
            return this;
        }

        public RunnerContext.Builder archiveLogs(boolean archiveLogs) {
            this.archiveLogs = archiveLogs;
            return this;
        }

        public RunnerContext build() {
            return new RunnerContext(this.appDir, this.baseDir, this.logsDir, this.archiveDir, this.runtimeAssertion, this.logHandler, this.archiveLogs);
        }


    }

}



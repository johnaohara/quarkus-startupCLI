package io.quarkus.ts.startstop;

import io.quarkus.ts.startstop.utils.Log;
import io.quarkus.ts.startstop.utils.MvnCmd;
import io.quarkus.ts.startstop.utils.RuntimeAssertion;

public class RunnerContext {
    protected final String baseDir;
    protected final String logsDir;
    protected final RuntimeAssertion runtimeAssertion;
    protected final Log log;

    protected RunnerContext(String baseDir, String logsDir, RuntimeAssertion runtimeAssertion, Log log) {
        this.baseDir = baseDir;
        this.logsDir = logsDir;
        this.runtimeAssertion = runtimeAssertion;
        this.log = log;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public String getLogsDir() {
        return logsDir;
    }

    public RuntimeAssertion getRuntimeAssertion() {
        return runtimeAssertion;
    }

    public Log getLog() {
        return log;
    }

    public static class Builder {

        protected String baseDir;
        protected MvnCmd mvnCmd;
        protected String logsDir;
        protected RuntimeAssertion runtimeAssertion;
        protected Log log;


        public static Builder instance(){
            return new Builder();
        }

        public RunnerContext.Builder baseDir(String baseDir){
            this.baseDir = baseDir;
            return this;
        }

        public RunnerContext.Builder logsDir(String logsDir){
            this.logsDir = logsDir;
            return this;
        }

        public RunnerContext.Builder runtimeAssertion(RuntimeAssertion runtimeAssertion){
            this.runtimeAssertion = runtimeAssertion;
            return this;
        }

        public RunnerContext.Builder log(Log log){
            this.log = log;
            return this;
        }


        public RunnerContext build(){
            return new RunnerContext(this.baseDir, logsDir, runtimeAssertion, log);
        }


    }

}



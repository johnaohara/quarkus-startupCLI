package io.quarkus.ts.startstop;

import io.quarkus.ts.startstop.utils.Log;
import io.quarkus.ts.startstop.utils.RuntimeAssertion;

import java.io.File;

public class RunnerContext {
    protected final String baseDir;
    protected final String appDir;
//    protected final String appFileName;

    protected final String logsDir;
    protected final RuntimeAssertion runtimeAssertion;
    protected final Log log;

    protected RunnerContext(String appDir, String baseDir, String logsDir, RuntimeAssertion runtimeAssertion, Log log) {
        this.appDir = appDir;
        this.baseDir = baseDir;
//        this.appFileName = appFileName;
        this.logsDir = logsDir;
        this.runtimeAssertion = runtimeAssertion;
        this.log = log;
    }

    public String getAppDir() {
        return this.appDir;
    }
    public String getBaseDir() {
        return this.baseDir;
    }

//    public String getAppFileName() {
//        return appFileName;
//    }

    public String getLogsDir() {
        return logsDir;
    }

    public RuntimeAssertion getRuntimeAssertion() {
        return runtimeAssertion;
    }

    public Log getLog() {
        return log;
    }

    public String getAppFullPath(){
        //TODO:: what scenarios do we have a null base dir?
        if ( this.baseDir == null){
            return this.appDir;
        }
        return this.baseDir +  File.separator + this.appDir;
    }

    public File getAppFullPathFile(){
        return new File(getAppFullPath());
    }

    public static class Builder {

        protected String appDir;
        protected String baseDir;
//        protected String appFileName;
        protected String logsDir;
        protected RuntimeAssertion runtimeAssertion;
        protected Log log;


        public static Builder instance(){
            return new Builder();
        }

        public RunnerContext.Builder appDir(String appDir){
            this.appDir = appDir;
            return this;
        }

//        public RunnerContext.Builder appFileName(String appName){
//            this.appFileName = appName;
//            return this;
//        }

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

        public RunnerContext.Builder baseDir(String baseDir) {
            this.baseDir = baseDir;
            return this;
        }

        public RunnerContext build(){
            return new RunnerContext(this.appDir, this.baseDir, logsDir, runtimeAssertion, log);
        }


    }

}



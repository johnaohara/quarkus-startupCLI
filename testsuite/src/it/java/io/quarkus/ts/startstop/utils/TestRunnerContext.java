package io.quarkus.ts.startstop.utils;

import io.quarkus.ts.startstop.context.RunnerContext;

import java.io.File;

public class TestRunnerContext extends RunnerContext {

    private final String testClass;
    private final String testMethod;

    protected TestRunnerContext(String appDir, String baseDir,  String logsDir, RuntimeAssertion runtimeAssertion, Log log, String testClass, String testMethod) {
        super(appDir, baseDir, logsDir, runtimeAssertion, log);
        this.testClass = testClass;
        this.testMethod = testMethod;
    }

    public String getLogsDir() {
        return testClass + File.separator + testMethod;
    }

    public String getTestClass() {
        return testClass;
    }

    public String getTestMethod() {
        return testMethod;
    }

    public String getAppDir() {
        return Environment.getBaseDir() + File.separator + super.appDir;
    }


    public static class Builder extends RunnerContext.Builder{

        private String testClass;
        private String testMethod;

        public static TestRunnerContext.Builder instance(){
            return new TestRunnerContext.Builder();
        }

        public TestRunnerContext.Builder testClass(String testClass){
            this.testClass = testClass;
            return this;
        }

        public TestRunnerContext.Builder testMethod(String testMethod){
            this.testMethod = testMethod;
            return this;
        }

        public TestRunnerContext build(){
            return new TestRunnerContext(this.appDir, this.baseDir, logsDir, runtimeAssertion, log, this.testClass, this.testMethod);
        }
    }
}

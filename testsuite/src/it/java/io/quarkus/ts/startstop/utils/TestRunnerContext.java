package io.quarkus.ts.startstop.utils;

import io.quarkus.ts.startstop.RunnerContext;

import java.io.File;

public class TestRunnerContext extends RunnerContext {

    private final String testClass;
    private final String testMethod;

    protected TestRunnerContext(String appDir, String baseDir, String appName, String logsDir, RuntimeAssertion runtimeAssertion, Log log, String testClass, String testMethod) {
        super(appDir, baseDir, appName, logsDir, runtimeAssertion, log);
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
            return new TestRunnerContext(this.appDir, this.baseDir, this.appFileName, logsDir, runtimeAssertion, log, this.testClass, this.testMethod);
        }
    }
}

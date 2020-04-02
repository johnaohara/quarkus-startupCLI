package io.quarkus.ts.startstop.utils;

public class ITContext {

    public static TestRunnerContext testContext(String appDir, String baseDir, String cn, String mn){
        TestRunnerContext.Builder builder = TestRunnerContext.Builder.instance();

        builder.appDir(appDir);
        builder.baseDir(baseDir);
        builder.testClass(cn);
        builder.testMethod(mn);
        builder.runtimeAssertion(new ITAssertion());
        builder.log(new ITLogs());

        return builder.build();

    }
}

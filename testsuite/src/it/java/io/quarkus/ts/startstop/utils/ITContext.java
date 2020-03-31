package io.quarkus.ts.startstop.utils;

import io.quarkus.ts.startstop.RunnerContext;

import static io.quarkus.ts.startstop.utils.Environment.getBaseDir;

public class ITContext {

    public static TestRunnerContext testContext(String cn, String mn){
        TestRunnerContext.Builder builder = TestRunnerContext.Builder.instance();

        builder.baseDir(getBaseDir());
        builder.testClass(cn);
        builder.testMethod(mn);
        builder.runtimeAssertion(new ITAssertion());
        builder.log(new ITLogs());

        return builder.build();

    }
}

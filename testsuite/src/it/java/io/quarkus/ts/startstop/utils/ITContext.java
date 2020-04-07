package io.quarkus.ts.startstop.utils;

import java.io.File;

import static io.quarkus.ts.startstop.utils.Environment.getBaseDir;

public class ITContext {

    public static TestRunnerContext testContext(String appDir, String baseDir, String cn, String mn){
        TestRunnerContext.Builder builder = TestRunnerContext.Builder.instance();

        builder.appDir(appDir);
        builder.baseDir(baseDir);
        builder.archiveDir(getBaseDir() + File.separator + "testsuite" + File.separator + "target" +
                File.separator + "archived-logs");
        builder.testClass(cn);
        builder.testMethod(mn);
        builder.runtimeAssertion(new ITAssertion());
        builder.log(new ITLogs());
        // Archive logs no matter what
        builder.archiveLogs(true);

        return builder.build();

    }
}

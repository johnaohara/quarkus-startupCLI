/*
 * Copyright (c) 2020 Contributors to the Quarkus StartStop project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.quarkus.ts.startstop;

import io.quarkus.ts.startstop.utils.App;
import io.quarkus.ts.startstop.utils.Apps;
import io.quarkus.ts.startstop.utils.Config;
import io.quarkus.ts.startstop.utils.Environment;
import io.quarkus.ts.startstop.utils.ITContext;
import io.quarkus.ts.startstop.utils.RunnerMvnCmd;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;



/**
 * @author Michal Karm Babacek <karm@redhat.com>
 */
@Tag("startstop")
public class StartStopTest {
    private static final Logger LOGGER = Logger.getLogger(StartStopTest.class.getName());

    public void testRuntime(TestInfo testInfo, Apps testApp, RunnerMvnCmd mvnCmd) throws IOException, InterruptedException {
        App app = Config.loadAppDefinitions("apps.yaml").get(testApp.dir);
        if (app == null) {
            throw new IllegalArgumentException("Can not find definition for: " + testApp.dir);
        }

        StartStopRunner.testStartup(app
                , ITContext.testContext(testApp.dir
                        , Environment.getBaseDir()
                        , testInfo.getTestClass().get().getCanonicalName()
                        , testInfo.getTestMethod().get().getName()
                )
                , mvnCmd);
    }


    @Test
    public void jaxRsMinimalJVM(TestInfo testInfo) throws IOException, InterruptedException {
        testRuntime(testInfo, Apps.JAX_RS_MINIMAL, RunnerMvnCmd.JVM);
    }

    @Test
    public void jaxRsMinimalJVMUber(TestInfo testInfo) throws IOException, InterruptedException {
        testRuntime(testInfo, Apps.JAX_RS_MINIMAL, RunnerMvnCmd.JVM_UBER);
    }

    @Test
    @Tag("native")
    public void jaxRsMinimalNative(TestInfo testInfo) throws IOException, InterruptedException {
        testRuntime(testInfo, Apps.JAX_RS_MINIMAL, RunnerMvnCmd.NATIVE);
    }

    @Test
    public void fullMicroProfileJVM(TestInfo testInfo) throws IOException, InterruptedException {
        testRuntime(testInfo, Apps.FULL_MICROPROFILE, RunnerMvnCmd.JVM);
    }

    @Test
    public void fullMicroProfileJVMUber(TestInfo testInfo) throws IOException, InterruptedException {
        testRuntime(testInfo, Apps.FULL_MICROPROFILE, RunnerMvnCmd.JVM_UBER);
    }

    @Test
    @Tag("native")
    public void fullMicroProfileNative(TestInfo testInfo) throws IOException, InterruptedException {
        testRuntime(testInfo, Apps.FULL_MICROPROFILE, RunnerMvnCmd.NATIVE);
    }
}

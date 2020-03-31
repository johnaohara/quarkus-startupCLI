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
import io.quarkus.ts.startstop.utils.ITContext;
import io.quarkus.ts.startstop.utils.MvnCmd;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;

/**
 * @author Michal Karm Babacek <karm@redhat.com>
 */
@Tag("startstop")
public class StartStopTest {

    public static void testRuntime(TestInfo testInfo, Apps testApp, MvnCmd mvnCmd) throws IOException, InterruptedException {
        App app = Config.loadAppDefinitions("apps.yaml").get(testApp.dir);
        if (app == null) {
            throw new IllegalArgumentException("Can not find definition for: " + testApp.dir);
        }

        StartStopRunner.testRuntime(app
                , ITContext.testContext(testInfo.getTestClass().get().getCanonicalName(), testInfo.getTestMethod().get().getName())
                , mvnCmd);
    }


    @Test
    public void jaxRsMinimalJVM(TestInfo testInfo) throws IOException, InterruptedException {
        testRuntime(testInfo, Apps.JAX_RS_MINIMAL, MvnCmd.JVM);
    }

    @Test
    @Tag("native")
    public void jaxRsMinimalNative(TestInfo testInfo) throws IOException, InterruptedException {
        testRuntime(testInfo, Apps.JAX_RS_MINIMAL, MvnCmd.NATIVE);
    }

    @Test
    public void fullMicroProfileJVM(TestInfo testInfo) throws IOException, InterruptedException {
        testRuntime(testInfo, Apps.FULL_MICROPROFILE, MvnCmd.JVM);
    }

    @Test
    @Tag("native")
    public void fullMicroProfileNative(TestInfo testInfo) throws IOException, InterruptedException {
        testRuntime(testInfo, Apps.FULL_MICROPROFILE, MvnCmd.NATIVE);
    }
}

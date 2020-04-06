package io.quarkus.ts.startstop;

import io.quarkus.ts.startstop.utils.FakeOIDCServer;
import org.junit.jupiter.api.Test;

public class FakeOIDCServerTestCase {

    @Test
    public void startFakeOIDCServerTest() throws Exception {
        FakeOIDCServer fakeOIDCServer = new FakeOIDCServer(6661, "localhost");
        fakeOIDCServer.run();
        fakeOIDCServer.stop();
    }
}

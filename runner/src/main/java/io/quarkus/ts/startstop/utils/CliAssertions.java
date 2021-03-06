package io.quarkus.ts.startstop.utils;

import io.quarkus.ts.startstop.StartStopRunner;
import org.jboss.logging.Logger;


public class CliAssertions implements Assertions {
    private static final Logger LOGGER = Logger.getLogger(StartStopRunner.class.getName());

    @Override
    public void assertFalse(boolean condition, String message) {
        if(condition){
            LOGGER.fatal(message);
            throw new RuntimeException("Run failed: ".concat(message));
        }
    }

    @Override
    public void assertTrue(boolean condition, String message) {
        this.assertFalse(!condition, message);
    }

    @Override
    public void assertTrue(boolean condition) {
        if(!condition){
            LOGGER.fatal("expected: <true> but was: <false>");
            throw new RuntimeException("Run failed");
        }
    }
}

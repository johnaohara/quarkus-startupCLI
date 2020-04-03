package io.quarkus.ts.startstop.utils;

import io.quarkus.ts.startstop.StartStopRunner;

import java.util.logging.Logger;

public class RunnerAssertions implements RuntimeAssertion {
    private static final Logger LOGGER = Logger.getLogger(StartStopRunner.class.getName());

    @Override
    public void assertFalse(boolean condition, String message) {
        if(condition){
            LOGGER.severe(message);
        }
    }

    @Override
    public void assertTrue(boolean condition, String message) {
        this.assertFalse(!condition, message);
    }

    @Override
    public void assertTrue(boolean condition) {
        if(!condition){
            LOGGER.severe("expected: <true> but was: <false>");
        }
    }
}

package io.quarkus.ts.startstop.utils;

import org.junit.jupiter.api.Assertions;

public class ITAssertion implements RuntimeAssertion {
    @Override
    public void assertFalse(boolean condition, String message) {
        Assertions.assertFalse(condition, message);
    }

    @Override
    public void assertTrue(boolean condition, String message) {
        Assertions.assertTrue(condition, message);
    }

    @Override
    public void assertTrue(boolean condition) {
        Assertions.assertTrue(condition);
    }
}

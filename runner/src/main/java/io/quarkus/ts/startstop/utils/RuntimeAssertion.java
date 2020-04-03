package io.quarkus.ts.startstop.utils;

public interface RuntimeAssertion {

    void assertFalse(boolean condition, String message);

    void assertTrue(boolean condition, String message);

    void assertTrue(boolean condition);
}

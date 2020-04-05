package io.quarkus.ts.startstop.utils;

public interface Assertions {

    void assertFalse(boolean condition, String message);

    void assertTrue(boolean condition, String message);

    void assertTrue(boolean condition);
}

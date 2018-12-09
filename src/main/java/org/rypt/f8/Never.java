package org.rypt.f8;

public class Never extends RuntimeException {
    private Never() {
        throw new AssertionError("Never can never be instantiated!");
    }
}

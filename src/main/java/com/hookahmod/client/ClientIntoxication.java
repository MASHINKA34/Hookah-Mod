package com.hookahmod.client;

public final class ClientIntoxication {

    private static float value;

    private ClientIntoxication() {}

    public static float get() {
        return value;
    }

    public static void set(float nextValue) {
        value = nextValue;
    }
}

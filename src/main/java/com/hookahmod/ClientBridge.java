package com.hookahmod;

import com.hookahmod.trip.TripVisionType;
import net.neoforged.fml.loading.FMLEnvironment;

public final class ClientBridge {

    private static final String HANDLERS = "com.hookahmod.client.ClientPayloadHandlers";

    private ClientBridge() {}

    public static void openGuide() {
        invoke("openGuide");
    }

    public static void setIntoxication(float value) {
        invoke("setIntoxication", new Class<?>[] { float.class }, value);
    }

    public static void triggerTrip(TripVisionType visionType, long seed) {
        invoke("triggerTrip", new Class<?>[] { TripVisionType.class, long.class }, visionType, seed);
    }

    public static void startHashishTrip(int durationTicks, float intensity) {
        invoke("startHashishTrip", new Class<?>[] { int.class, float.class }, durationTicks, intensity);
    }

    public static void startVideoTrip() {
        invoke("startVideoTrip");
    }

    private static void invoke(String method) {
        invoke(method, new Class<?>[0]);
    }

    private static void invoke(String method, Class<?>[] parameterTypes, Object... args) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        try {
            Class.forName(HANDLERS).getMethod(method, parameterTypes).invoke(null, args);
        } catch (ReflectiveOperationException ex) {
            HookahMod.LOGGER.error("Failed to run client hook {}", method, ex);
        }
    }
}

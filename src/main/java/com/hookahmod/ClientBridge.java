package com.hookahmod;

import com.hookahmod.trip.TripVisionType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public final class ClientBridge {

    private ClientBridge() {}

    public static void openGuide() {
        if (isClient()) com.hookahmod.client.ClientPayloadHandlers.openGuide();
    }

    public static void setIntoxication(float value) {
        if (isClient()) com.hookahmod.client.ClientPayloadHandlers.setIntoxication(value);
    }

    public static void triggerTrip(TripVisionType visionType, long seed) {
        if (isClient()) com.hookahmod.client.ClientPayloadHandlers.triggerTrip(visionType, seed);
    }

    public static void startHashishTrip(int durationTicks, float intensity) {
        if (isClient()) com.hookahmod.client.ClientPayloadHandlers.startHashishTrip(durationTicks, intensity);
    }

    public static void startVideoTrip() {
        if (isClient()) com.hookahmod.client.ClientPayloadHandlers.startVideoTrip();
    }

    private static boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }
}

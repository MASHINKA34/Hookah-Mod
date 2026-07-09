package com.hookahmod.client;

import com.hookahmod.client.trip.HashishTripManager;
import com.hookahmod.client.trip.TripManager;
import com.hookahmod.client.trip.VideoTripManager;
import com.hookahmod.trip.TripVisionType;
import net.minecraft.client.Minecraft;

public final class ClientPayloadHandlers {

    private ClientPayloadHandlers() {}

    public static void openGuide() {
        Minecraft.getInstance().setScreen(new GuideScreen());
    }

    public static void setIntoxication(float value) {
        ClientIntoxication.set(value);
    }

    public static void triggerTrip(TripVisionType visionType, long seed) {
        TripManager.trigger(visionType, seed);
    }

    public static void startHashishTrip(int durationTicks, float intensity) {
        HashishTripManager.start(durationTicks, intensity);
    }

    public static void startVideoTrip() {
        VideoTripManager.start();
    }
}

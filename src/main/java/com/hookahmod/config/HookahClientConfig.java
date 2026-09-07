package com.hookahmod.config;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class HookahClientConfig {

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue TRIP_VISUALS;
    private static final ModConfigSpec.BooleanValue TRIP_VISIONS;
    private static final ModConfigSpec.BooleanValue SCREAMER;
    private static final ModConfigSpec.BooleanValue VIDEO_TRIP;
    private static final ModConfigSpec.BooleanValue SPIRAL_SHADER;
    private static final ModConfigSpec.DoubleValue CAMERA_MOTION;

    public static boolean tripVisuals = true;
    public static boolean tripVisions = true;
    public static boolean screamer = true;
    public static boolean videoTrip = true;
    public static boolean spiralShader = true;
    public static float cameraMotion = 1.0f;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment(
                        "Visual effects of the trip bands.",
                        "These are local to this client and never affect gameplay, so they are safe to turn off",
                        "if you are sensitive to flashing images, sudden jump scares or camera motion.")
                .push("trips");
        TRIP_VISUALS = builder
                .comment("Master switch. Turning this off disables every trip visual below.")
                .define("enabled", true);
        TRIP_VISIONS = builder
                .comment("Show the figures that appear in the world during a trip.")
                .define("visions", true);
        SCREAMER = builder
                .comment("Show the full-screen jump scare when a runner vision reaches you.")
                .define("screamer", true);
        VIDEO_TRIP = builder
                .comment("Play the full-screen PalPalych video.")
                .define("videoTrip", true);
        SPIRAL_SHADER = builder
                .comment("Apply the hashish spiral post-processing shader.")
                .define("spiralShader", true);
        CAMERA_MOTION = builder
                .comment("Scales trip camera sway, roll and field-of-view pulsing. 0 disables the motion.")
                .defineInRange("cameraMotion", 1.0, 0.0, 1.0);
        builder.pop();

        SPEC = builder.build();
    }

    private HookahClientConfig() {}

    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) bake();
    }

    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) bake();
    }

    public static boolean visionsEnabled() {
        return tripVisuals && tripVisions;
    }

    public static boolean screamerEnabled() {
        return tripVisuals && screamer;
    }

    public static boolean videoTripEnabled() {
        return tripVisuals && videoTrip;
    }

    public static boolean spiralShaderEnabled() {
        return tripVisuals && spiralShader;
    }

    public static float cameraMotionScale() {
        return tripVisuals ? cameraMotion : 0.0f;
    }

    private static void bake() {
        tripVisuals = TRIP_VISUALS.get();
        tripVisions = TRIP_VISIONS.get();
        screamer = SCREAMER.get();
        videoTrip = VIDEO_TRIP.get();
        spiralShader = SPIRAL_SHADER.get();
        cameraMotion = CAMERA_MOTION.get().floatValue();
    }
}

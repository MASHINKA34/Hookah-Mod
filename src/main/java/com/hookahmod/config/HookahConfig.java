package com.hookahmod.config;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class HookahConfig {

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue SHORT_HOSE_RANGE;
    private static final ModConfigSpec.IntValue LONG_HOSE_RANGE;
    private static final ModConfigSpec.IntValue EXHALE_COOLDOWN_TICKS;
    private static final ModConfigSpec.IntValue MINIMUM_CHARGE_TICKS;
    private static final ModConfigSpec.IntValue SOLID_PUFFS_PER_CHARGE;
    private static final ModConfigSpec.IntValue COMBAT_PUFFS_PER_CHARGE;
    private static final ModConfigSpec.IntValue WATER_PUFFS_PER_BOTTLE;

    private static final ModConfigSpec.DoubleValue MAX_INTOXICATION;
    private static final ModConfigSpec.DoubleValue INTOXICATION_DECAY_PER_SECOND;
    private static final ModConfigSpec.DoubleValue PLAIN_TOBACCO_INTOXICATION;
    private static final ModConfigSpec.DoubleValue RELAXED_THRESHOLD;
    private static final ModConfigSpec.DoubleValue HIGH_THRESHOLD;
    private static final ModConfigSpec.DoubleValue TRIP_THRESHOLD;
    private static final ModConfigSpec.DoubleValue OVERDOSE_THRESHOLD;

    private static final ModConfigSpec.BooleanValue COMBAT_ENABLED;
    private static final ModConfigSpec.BooleanValue COMBAT_BLOCK_CHANGES;
    private static final ModConfigSpec.DoubleValue COMBAT_BASE_RANGE;
    private static final ModConfigSpec.DoubleValue COMBAT_CONE_HALF_ANGLE;

    private static final ModConfigSpec.BooleanValue ROOM_SMOKE_ENABLED;
    private static final ModConfigSpec.IntValue SMOKE_PARTICLE_RANGE;
    private static final ModConfigSpec.IntValue MAX_ROOM_AIR_BLOCKS;
    private static final ModConfigSpec.IntValue MAX_ROOM_CLOUDS;
    private static final ModConfigSpec.IntValue ROOM_LINGER_SECONDS;
    private static final ModConfigSpec.IntValue ROOM_PROBE_COOLDOWN_TICKS;

    private static final ModConfigSpec.BooleanValue WORN_HOOKAH_LIGHT;
    private static final ModConfigSpec.DoubleValue CHICKEN_POOP_CHANCE;
    private static final ModConfigSpec.BooleanValue SHOW_LUXURY_PREVIEW;

    public static int shortHoseRange = 5;
    public static int longHoseRange = 10;
    public static int exhaleCooldownTicks = 10;
    public static int minimumChargeTicks = 5;
    public static int solidPuffsPerCharge = 20;
    public static int combatPuffsPerCharge = 10;
    public static int waterPuffsPerBottle = 200;

    public static float maxIntoxication = 220.0f;
    public static float intoxicationDecayPerSecond = 1.0f;
    public static float plainTobaccoIntoxication = 16.0f;
    public static float relaxedThreshold = 30.0f;
    public static float highThreshold = 60.0f;
    public static float tripThreshold = 100.0f;
    public static float overdoseThreshold = 150.0f;

    public static boolean combatEnabled = true;
    public static boolean combatBlockChanges = true;
    public static double combatBaseRange = 4.0;
    public static double combatConeHalfAngle = 25.0;

    public static boolean roomSmokeEnabled = true;
    public static int smokeParticleRange = 128;
    public static int maxRoomAirBlocks = 8192;
    public static int maxRoomClouds = 32;
    public static int roomLingerSeconds = 30;
    public static int roomProbeCooldownTicks = 40;

    public static boolean wornHookahLight = true;
    public static float chickenPoopChance = 0.35f;
    public static boolean showLuxuryPreview = true;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Hose reach, puff pacing and how fast consumables burn.").push("smoking");
        SHORT_HOSE_RANGE = builder
                .comment("Reach of the short hose, in blocks.")
                .defineInRange("shortHoseRange", 5, 1, 64);
        LONG_HOSE_RANGE = builder
                .comment("Reach of the long hose, in blocks.")
                .defineInRange("longHoseRange", 10, 1, 64);
        EXHALE_COOLDOWN_TICKS = builder
                .comment("Ticks a smoker must wait between two exhales. 0 disables the cooldown.")
                .defineInRange("exhaleCooldownTicks", 10, 0, 200);
        MINIMUM_CHARGE_TICKS = builder
                .comment("Ticks the mouthpiece must be held before an early release counts as a draw.")
                .defineInRange("minimumChargeTicks", 5, 1, 100);
        SOLID_PUFFS_PER_CHARGE = builder
                .comment("Puffs a regular tobacco charge lasts before tobacco and coal are consumed.")
                .defineInRange("solidPuffsPerCharge", 20, 1, 512);
        COMBAT_PUFFS_PER_CHARGE = builder
                .comment("Puffs a combat tobacco charge lasts before tobacco and coal are consumed.")
                .defineInRange("combatPuffsPerCharge", 10, 1, 512);
        WATER_PUFFS_PER_BOTTLE = builder
                .comment("Puffs one bottle of water or one can lasts.")
                .defineInRange("waterPuffsPerBottle", 200, 1, 4096);
        builder.pop();

        builder.comment("The intoxication meter read by the tonometer.").push("intoxication");
        MAX_INTOXICATION = builder
                .comment("Upper bound of the intoxication meter.")
                .defineInRange("maxValue", 220.0, 1.0, 10000.0);
        INTOXICATION_DECAY_PER_SECOND = builder
                .comment("How much intoxication decays every second.")
                .defineInRange("decayPerSecond", 1.0, 0.0, 100.0);
        PLAIN_TOBACCO_INTOXICATION = builder
                .comment("Intoxication granted by a full draw of unflavoured tobacco.")
                .defineInRange("plainTobacco", 16.0, 0.0, 1000.0);
        RELAXED_THRESHOLD = builder
                .comment("Intoxication at which the relaxed band starts.")
                .defineInRange("relaxedThreshold", 30.0, 0.0, 10000.0);
        HIGH_THRESHOLD = builder
                .comment("Intoxication at which the high band starts.")
                .defineInRange("highThreshold", 60.0, 0.0, 10000.0);
        TRIP_THRESHOLD = builder
                .comment("Intoxication at which the trip band starts.")
                .defineInRange("tripThreshold", 100.0, 0.0, 10000.0);
        OVERDOSE_THRESHOLD = builder
                .comment("Intoxication at which the overdose band starts.")
                .defineInRange("overdoseThreshold", 150.0, 0.0, 10000.0);
        builder.pop();

        builder.comment("Poison, fire, ice and heal blends.").push("combat");
        COMBAT_ENABLED = builder
                .comment("Master switch for the combat cones. Disabling keeps the blends craftable but inert.")
                .define("enabled", true);
        COMBAT_BLOCK_CHANGES = builder
                .comment("Allow the fire blend to ignite blocks and the ice blend to freeze water.")
                .define("allowBlockChanges", true);
        COMBAT_BASE_RANGE = builder
                .comment("Cone reach before tier and charge bonuses, in blocks.")
                .defineInRange("baseRange", 4.0, 0.5, 32.0);
        COMBAT_CONE_HALF_ANGLE = builder
                .comment("Half-angle of the cone, in degrees.")
                .defineInRange("coneHalfAngle", 25.0, 1.0, 90.0);
        builder.pop();

        builder.comment("Exhaled smoke that lingers and fills enclosed rooms.").push("smoke");
        ROOM_SMOKE_ENABLED = builder
                .comment("Let smoke accumulate inside enclosed rooms. Disabling removes the flood fill entirely.")
                .define("roomSmokeEnabled", true);
        SMOKE_PARTICLE_RANGE = builder
                .comment("Blocks within which smoke particles are sent to players.")
                .defineInRange("particleRange", 128, 16, 256);
        MAX_ROOM_AIR_BLOCKS = builder
                .comment("Largest room the flood fill will map. Lower values cost less CPU.")
                .defineInRange("maxRoomAirBlocks", 8192, 64, 32768);
        MAX_ROOM_CLOUDS = builder
                .comment("How many rooms may hold smoke at once, server wide.")
                .defineInRange("maxRoomClouds", 32, 1, 256);
        ROOM_LINGER_SECONDS = builder
                .comment("Seconds a room keeps its smoke after the last puff.")
                .defineInRange("lingerSeconds", 30, 1, 600);
        ROOM_PROBE_COOLDOWN_TICKS = builder
                .comment("Ticks before a spot that failed the room test is probed again. Guards against flood-fill spam outdoors.")
                .defineInRange("probeCooldownTicks", 40, 0, 600);
        builder.pop();

        builder.comment("Everything else.").push("misc");
        WORN_HOOKAH_LIGHT = builder
                .comment("Let a lit worn hookah place a temporary light block.")
                .define("wornHookahLight", true);
        CHICKEN_POOP_CHANCE = builder
                .comment("Chance a chicken drops chicken poop when it lays an egg.")
                .defineInRange("chickenPoopChance", 0.35, 0.0, 1.0);
        SHOW_LUXURY_PREVIEW = builder
                .comment("Show the unfinished luxury hookah preview in the creative tab.")
                .define("showLuxuryPreview", true);
        builder.pop();

        SPEC = builder.build();
    }

    private HookahConfig() {}

    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) bake();
    }

    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) bake();
    }

    private static void bake() {
        shortHoseRange = SHORT_HOSE_RANGE.get();
        longHoseRange = LONG_HOSE_RANGE.get();
        exhaleCooldownTicks = EXHALE_COOLDOWN_TICKS.get();
        minimumChargeTicks = MINIMUM_CHARGE_TICKS.get();
        solidPuffsPerCharge = SOLID_PUFFS_PER_CHARGE.get();
        combatPuffsPerCharge = COMBAT_PUFFS_PER_CHARGE.get();
        waterPuffsPerBottle = WATER_PUFFS_PER_BOTTLE.get();

        maxIntoxication = MAX_INTOXICATION.get().floatValue();
        intoxicationDecayPerSecond = INTOXICATION_DECAY_PER_SECOND.get().floatValue();
        plainTobaccoIntoxication = PLAIN_TOBACCO_INTOXICATION.get().floatValue();
        relaxedThreshold = RELAXED_THRESHOLD.get().floatValue();
        highThreshold = HIGH_THRESHOLD.get().floatValue();
        tripThreshold = TRIP_THRESHOLD.get().floatValue();
        overdoseThreshold = OVERDOSE_THRESHOLD.get().floatValue();

        combatEnabled = COMBAT_ENABLED.get();
        combatBlockChanges = COMBAT_BLOCK_CHANGES.get();
        combatBaseRange = COMBAT_BASE_RANGE.get();
        combatConeHalfAngle = COMBAT_CONE_HALF_ANGLE.get();

        roomSmokeEnabled = ROOM_SMOKE_ENABLED.get();
        smokeParticleRange = SMOKE_PARTICLE_RANGE.get();
        maxRoomAirBlocks = MAX_ROOM_AIR_BLOCKS.get();
        maxRoomClouds = MAX_ROOM_CLOUDS.get();
        roomLingerSeconds = ROOM_LINGER_SECONDS.get();
        roomProbeCooldownTicks = ROOM_PROBE_COOLDOWN_TICKS.get();

        wornHookahLight = WORN_HOOKAH_LIGHT.get();
        chickenPoopChance = CHICKEN_POOP_CHANCE.get().floatValue();
        showLuxuryPreview = SHOW_LUXURY_PREVIEW.get();
    }
}

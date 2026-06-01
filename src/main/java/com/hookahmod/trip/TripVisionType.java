package com.hookahmod.trip;

import net.minecraft.util.RandomSource;

public enum TripVisionType {
    OBSERVER,
    FALSE_MOB,
    PLAYER_COPY,
    SKY_SHIFT,
    RUNNER;

    public static TripVisionType byId(int id) {
        TripVisionType[] values = values();
        return values[Math.floorMod(id, values.length)];
    }

    public static TripVisionType random(RandomSource random) {
        return byId(random.nextInt(values().length));
    }
}

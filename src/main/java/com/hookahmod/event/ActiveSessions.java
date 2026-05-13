package com.hookahmod.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ActiveSessions {

    private static final Map<UUID, GlobalPos> SESSIONS = new ConcurrentHashMap<>();

    private ActiveSessions() {}

    public static void register(UUID player, ResourceKey<Level> dim, BlockPos pos) {
        SESSIONS.put(player, GlobalPos.of(dim, pos));
    }

    public static void unregister(UUID player) {
        SESSIONS.remove(player);
    }

    public static GlobalPos get(UUID player) {
        return SESSIONS.get(player);
    }
}

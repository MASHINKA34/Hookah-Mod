package com.hookahmod.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks who is currently smoking which hookah.
 *
 * <p>The client and the server each keep their own instance. The server copy is
 * authoritative; the client copy is a mirror (fed by sync packets) used only for
 * the local player's predictive {@code use()} checks. Keeping the two logical
 * sides separate avoids them sharing a single map inside an integrated-server
 * (single-player) JVM, where both threads would otherwise stomp on each other.
 */
public final class ActiveSessions {

    private static final ActiveSessions SERVER = new ActiveSessions();
    private static final ActiveSessions CLIENT = new ActiveSessions();

    private final Map<UUID, GlobalPos> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> wornSessions = new ConcurrentHashMap<>();

    private ActiveSessions() {}

    /** Pick the side-local instance based on the given level. */
    public static ActiveSessions of(Level level) {
        return level.isClientSide ? CLIENT : SERVER;
    }

    public static ActiveSessions server() { return SERVER; }

    public static ActiveSessions client() { return CLIENT; }

    public void register(UUID player, ResourceKey<Level> dim, BlockPos pos) {
        wornSessions.remove(player);
        sessions.put(player, GlobalPos.of(dim, pos));
    }

    public void registerWorn(UUID player, UUID wearer) {
        sessions.remove(player);
        wornSessions.put(player, wearer);
    }

    public void unregister(UUID player) {
        sessions.remove(player);
        wornSessions.remove(player);
    }

    public GlobalPos get(UUID player) {
        return sessions.get(player);
    }

    public UUID getWornWearer(UUID player) {
        return wornSessions.get(player);
    }
}

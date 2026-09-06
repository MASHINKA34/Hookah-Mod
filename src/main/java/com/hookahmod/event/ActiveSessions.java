package com.hookahmod.event;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.item.WornHookah;
import com.hookahmod.network.WornHookahSyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

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
    private final Map<UUID, Session> owners = new HashMap<>();

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
        owners.remove(player);
    }

    public boolean owns(UUID player, Object source) {
        Session session = owners.get(player);
        return session != null && session.source() == source;
    }

    public boolean unregister(UUID player, Object source) {
        if (!owns(player, source)) return false;
        unregister(player);
        return true;
    }

    public void beginBlock(ServerPlayer player, HookahBlockEntity hookah) {
        release(player.getUUID());
        register(player.getUUID(), player.level().dimension(), hookah.getBlockPos());
        owners.put(player.getUUID(), new Session(player, hookah, hookah::releaseMouthpiece, () ->
                player.isAlive() && !player.isRemoved() && !player.isSpectator() && !hookah.isRemoved()
                        && hookah.getLevel() == player.level()
                        && player.getUUID().equals(hookah.getActivePlayerUuid())
                        && hookah.isPlayerInRange(player)
                        && WornHookah.playerHasMouthpiece(player)));
    }

    public void beginWorn(ServerPlayer player, ServerPlayer wearer, ItemStack stack) {
        release(player.getUUID());
        registerWorn(player.getUUID(), wearer.getUUID());
        owners.put(player.getUUID(), new Session(player, stack, () -> WornHookah.releaseMouthpiece(wearer, stack), () ->
                player.isAlive() && !player.isRemoved() && !player.isSpectator()
                        && wearer.isAlive() && !wearer.isRemoved() && !wearer.isSpectator()
                        && wearer.getItemBySlot(EquipmentSlot.CHEST) == stack
                        && player.getUUID().equals(WornHookah.getActivePlayerUuid(stack))
                        && WornHookah.isUserInRange(player, wearer, stack)
                        && WornHookah.playerHasMouthpiece(player)));
    }

    public void release(UUID player) {
        Session session = owners.get(player);
        if (session == null) {
            unregister(player);
            return;
        }
        session.release().run();
        if (owners.get(player) == session) {
            unregister(player);
            PacketDistributor.sendToPlayer(session.player(), WornHookahSyncPayload.release());
        }
    }

    public void tick() {
        for (Map.Entry<UUID, Session> entry : List.copyOf(owners.entrySet())) {
            if (!entry.getValue().valid().getAsBoolean()) release(entry.getKey());
        }
    }

    public void clear() {
        for (UUID player : List.copyOf(owners.keySet())) release(player);
        sessions.clear();
        wornSessions.clear();
    }

    private record Session(ServerPlayer player, Object source, Runnable release, BooleanSupplier valid) {}

    public GlobalPos get(UUID player) {
        return sessions.get(player);
    }

    public UUID getWornWearer(UUID player) {
        return wornSessions.get(player);
    }
}

package com.hookahmod.event;

import com.hookahmod.block.HookahBlockEntity;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;

public final class ServerEvents {

    private ServerEvents() {}

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            releaseSession(sp.server, sp.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player p = event.getEntity();
        if (p instanceof ServerPlayer sp) releaseSession(sp.server, sp.getUUID());
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) releaseSession(sp.server, sp.getUUID());
    }

    private static void releaseSession(MinecraftServer server, UUID uuid) {
        GlobalPos gp = ActiveSessions.get(uuid);
        if (gp == null || server == null) return;
        ServerLevel level = server.getLevel(gp.dimension());
        if (level == null) return;
        if (level.getBlockEntity(gp.pos()) instanceof HookahBlockEntity be) {
            be.releaseMouthpieceIfHolder(uuid);
        } else {
            ActiveSessions.unregister(uuid);
        }
    }
}

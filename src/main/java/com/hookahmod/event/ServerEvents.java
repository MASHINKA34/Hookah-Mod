package com.hookahmod.event;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.item.HookahBlockItem;
import com.hookahmod.item.WornHookah;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

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

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!player.isShiftKeyDown()) return;

        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!WornHookah.isHookahStack(stack)) return;
        if (!(stack.getItem() instanceof HookahBlockItem hookahItem)) return;

        ItemStack placeStack = stack.copy();
        InteractionResult result = hookahItem.place(new BlockPlaceContext(
                player,
                InteractionHand.MAIN_HAND,
                placeStack,
                event.getHitVec()
        ));
        if (result.consumesAction()) {
            WornHookah.releaseMouthpiece(player, stack);
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof ServerPlayer wearer)) return;

        ItemStack stack = wearer.getItemBySlot(EquipmentSlot.CHEST);
        if (!WornHookah.isHookahStack(stack)) return;

        if (WornHookah.tryTakeMouthpiece(player, wearer, stack)) {
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }

    private static void releaseSession(MinecraftServer server, UUID uuid) {
        GlobalPos gp = ActiveSessions.get(uuid);
        UUID wearerUuid = ActiveSessions.getWornWearer(uuid);
        if (wearerUuid != null && server != null) {
            ServerPlayer wearer = server.getPlayerList().getPlayer(wearerUuid);
            if (wearer != null) {
                ItemStack stack = wearer.getItemBySlot(EquipmentSlot.CHEST);
                if (WornHookah.isHookahStack(stack)) {
                    WornHookah.releaseMouthpiece(wearer, stack);
                    return;
                }
            }
            ActiveSessions.unregister(uuid);
            return;
        }
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

package com.hookahmod.event;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.item.HookahBlockItem;
import com.hookahmod.item.WornHookah;
import com.hookahmod.smoke.HookahSmoke;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerEvents {

    private static final int WORN_LIGHT_LEVEL = 8;
    private static final Map<UUID, GlobalPos> WORN_LIGHTS = new ConcurrentHashMap<>();

    private ServerEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        HookahSmoke.serverTick(event.getServer());
        tickWornHookahLights(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            removeWornHookahLight(sp.server, sp.getUUID());
            releaseSession(sp.server, sp.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player p = event.getEntity();
        if (p instanceof ServerPlayer sp) {
            removeWornHookahLight(sp.server, sp.getUUID());
            releaseSession(sp.server, sp.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            removeWornHookahLight(sp.server, sp.getUUID());
            releaseSession(sp.server, sp.getUUID());
        }
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
        GlobalPos gp = ActiveSessions.server().get(uuid);
        UUID wearerUuid = ActiveSessions.server().getWornWearer(uuid);
        if (wearerUuid != null && server != null) {
            ServerPlayer wearer = server.getPlayerList().getPlayer(wearerUuid);
            if (wearer != null) {
                ItemStack stack = wearer.getItemBySlot(EquipmentSlot.CHEST);
                if (WornHookah.isHookahStack(stack)) {
                    WornHookah.releaseMouthpiece(wearer, stack);
                    return;
                }
            }
            ActiveSessions.server().unregister(uuid);
            return;
        }
        if (gp == null || server == null) return;
        ServerLevel level = server.getLevel(gp.dimension());
        if (level == null) return;
        if (level.getBlockEntity(gp.pos()) instanceof HookahBlockEntity be) {
            be.releaseMouthpieceIfHolder(uuid);
        } else {
            ActiveSessions.server().unregister(uuid);
        }
    }

    private static void tickWornHookahLights(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
            if (WornHookah.isHookahStack(stack) && WornHookah.hasCoal(stack)) {
                updateWornHookahLight(player);
            } else {
                removeWornHookahLight(server, player.getUUID());
            }
        }
    }

    private static void updateWornHookahLight(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UUID uuid = player.getUUID();
        BlockPos pos = BlockPos.containing(player.getX(), player.getY() + player.getBbHeight() * 0.72D, player.getZ());
        GlobalPos next = GlobalPos.of(level.dimension(), pos);
        GlobalPos current = WORN_LIGHTS.get(uuid);

        if (current != null && !current.equals(next)) {
            removeWornHookahLight(player.server, uuid);
        }

        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.LIGHT)) {
            if (next.equals(WORN_LIGHTS.get(uuid)) && state.getValue(LightBlock.LEVEL) != WORN_LIGHT_LEVEL) {
                level.setBlock(pos, state.setValue(LightBlock.LEVEL, WORN_LIGHT_LEVEL), 3);
            }
            return;
        }
        if (next.equals(WORN_LIGHTS.get(uuid))) {
            WORN_LIGHTS.remove(uuid);
        }
        if (!state.isAir()) return;

        level.setBlock(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, WORN_LIGHT_LEVEL), 3);
        WORN_LIGHTS.put(uuid, next);
    }

    private static void removeWornHookahLight(MinecraftServer server, UUID uuid) {
        GlobalPos old = WORN_LIGHTS.remove(uuid);
        if (old == null) return;
        ServerLevel level = server.getLevel(old.dimension());
        if (level == null) return;
        BlockState state = level.getBlockState(old.pos());
        if (state.is(Blocks.LIGHT) && state.getValue(LightBlock.LEVEL) == WORN_LIGHT_LEVEL) {
            level.removeBlock(old.pos(), false);
        }
    }
}

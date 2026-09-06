package com.hookahmod.event;

import com.hookahmod.HookahMod;
import com.hookahmod.effect.ModMobEffects;
import com.hookahmod.integration.KingdomsIntegration;
import com.hookahmod.item.HookahBlockItem;
import com.hookahmod.item.WornHookah;
import com.hookahmod.recipe.WhiteMonsterBrewingRecipe;
import com.hookahmod.recipe.SweetWaterBrewingRecipe;
import com.hookahmod.registry.ModItems;
import com.hookahmod.smoke.HookahSmoke;
import com.hookahmod.smoking.IntoxicationState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerEvents {

    private static final int WORN_LIGHT_LEVEL = 8;
    private static final Map<UUID, GlobalPos> WORN_LIGHTS = new ConcurrentHashMap<>();
    private static final float CHICKEN_POOP_CHANCE = 0.35F;
    private static final ResourceLocation PALPALYCH_LOCK_ID = HookahMod.id("palpalych_trip_lock");

    private ServerEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ActiveSessions.server().tick();
        HookahSmoke.serverTick(event.getServer());
        tickIntoxication(event.getServer());
        tickPalPalychTrip(event.getServer());
        tickWornHookahLights(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            IntoxicationState.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            IntoxicationState.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Chicken chicken)) return;
        if (!(chicken.level() instanceof ServerLevel)) return;
        if (!chicken.isAlive() || chicken.isBaby() || chicken.isChickenJockey()) return;
        if (chicken.eggTime != 1) return;
        if (chicken.getRandom().nextFloat() >= CHICKEN_POOP_CHANCE) return;

        chicken.spawnAtLocation(ModItems.CHICKEN_POOP.get());
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            releaseWornHookah(sp);
            removeWornHookahLight(sp.server, sp.getUUID());
            ActiveSessions.server().release(sp.getUUID());
            endPalPalychTrip(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player p = event.getEntity();
        if (p instanceof ServerPlayer sp) {
            releaseWornHookah(sp);
            removeWornHookahLight(sp.server, sp.getUUID());
            ActiveSessions.server().release(sp.getUUID());
            endPalPalychTrip(sp);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            releaseWornHookah(sp);
            removeWornHookahLight(sp.server, sp.getUUID());
            ActiveSessions.server().release(sp.getUUID());
            IntoxicationState.sync(sp);
            endPalPalychTrip(sp);
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getSlot() != EquipmentSlot.CHEST) return;
        if (!WornHookah.isHookahStack(event.getTo())) return;
        if (KingdomsIntegration.canEquipHookah(player)) return;

        ItemStack hookah = event.getTo().copy();
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        if (!player.getInventory().add(hookah)) {
            player.drop(hookah, false);
        }
        player.displayClientMessage(Component.translatable("message.hookahmod.protected_area"), true);
    }

    private static void tickIntoxication(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            IntoxicationState.decayTick(player);
            IntoxicationState.applyBandEffects(player);
        }
    }

    private static void tickPalPalychTrip(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean tripping = player.hasEffect(ModMobEffects.PALPALYCH_TRIP);
            AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            boolean locked = speed != null && speed.getModifier(PALPALYCH_LOCK_ID) != null;
            if (tripping && !locked) {
                applyTripLock(player);
            } else if (!tripping && locked) {
                clearTripLock(player);
            }
        }
    }

    private static void applyTripLock(ServerPlayer player) {
        player.setForcedPose(Pose.SWIMMING);
        addLockModifier(player.getAttribute(Attributes.MOVEMENT_SPEED));
        addLockModifier(player.getAttribute(Attributes.JUMP_STRENGTH));
    }

    private static void clearTripLock(ServerPlayer player) {
        player.setForcedPose(null);
        removeLockModifier(player.getAttribute(Attributes.MOVEMENT_SPEED));
        removeLockModifier(player.getAttribute(Attributes.JUMP_STRENGTH));
    }

    private static void addLockModifier(AttributeInstance attribute) {
        if (attribute == null || attribute.getModifier(PALPALYCH_LOCK_ID) != null) return;
        attribute.addTransientModifier(new AttributeModifier(PALPALYCH_LOCK_ID, -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeLockModifier(AttributeInstance attribute) {
        if (attribute != null) attribute.removeModifier(PALPALYCH_LOCK_ID);
    }

    private static void endPalPalychTrip(ServerPlayer player) {
        if (player.hasEffect(ModMobEffects.PALPALYCH_TRIP)) {
            player.removeEffect(ModMobEffects.PALPALYCH_TRIP);
        }
        clearTripLock(player);
    }

    @SubscribeEvent
    public static void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
        Item ingredient = ModList.get().isLoaded("croptopia")
                ? BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("croptopia", "coffee_beans")).orElse(Items.COCOA_BEANS)
                : Items.COCOA_BEANS;
        event.getBuilder().addRecipe(new SweetWaterBrewingRecipe());
        event.getBuilder().addRecipe(new WhiteMonsterBrewingRecipe(ingredient));
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
        BlockPlaceContext context = new BlockPlaceContext(
                player,
                InteractionHand.MAIN_HAND,
                placeStack,
                event.getHitVec()
        );
        if (!KingdomsIntegration.canMoveHookahBlock(player, context.getClickedPos())) {
            player.displayClientMessage(Component.translatable("message.hookahmod.protected_area"), true);
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }

        InteractionResult result = CommonHooks.onPlaceItemIntoWorld(context);
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

    private static void releaseWornHookah(ServerPlayer wearer) {
        ItemStack stack = wearer.getItemBySlot(EquipmentSlot.CHEST);
        if (WornHookah.isHookahStack(stack)) WornHookah.releaseMouthpiece(wearer, stack);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ActiveSessions.server().clear();
        for (UUID uuid : java.util.List.copyOf(WORN_LIGHTS.keySet())) {
            removeWornHookahLight(event.getServer(), uuid);
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

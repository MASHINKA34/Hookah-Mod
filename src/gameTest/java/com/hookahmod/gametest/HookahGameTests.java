package com.hookahmod.gametest;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.block.HookahBlock;
import com.hookahmod.block.HookahLightBlock;
import com.hookahmod.event.ActiveSessions;
import com.hookahmod.event.ServerEvents;
import com.hookahmod.item.HookahHoseType;
import com.hookahmod.item.HookahTier;
import com.hookahmod.item.WornHookah;
import com.hookahmod.menu.HookahMenu;
import com.hookahmod.registry.ModBlocks;
import com.hookahmod.registry.ModItems;
import com.hookahmod.registry.ModSounds;
import com.hookahmod.smoking.HookahProgress;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import software.bernie.geckolib.animatable.GeoItem;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Mod("hookahmod_tests")
@GameTestHolder("hookahmod_tests")
@PrefixGameTestTemplate(false)
public class HookahGameTests {
    @GameTest(template = "empty")
    public static void switchingBlocksReleasesOnlyThePreviousHookah(GameTestHelper helper) {
        try (Players players = new Players(helper)) {
            ServerPlayer player = players.create(helper.getLevel());
            HookahBlockEntity first = block(helper, new BlockPos(0, 1, 0));
            HookahBlockEntity second = block(helper, new BlockPos(1, 1, 0));
            helper.assertTrue(first.tryTakeMouthpiece(player), "First hookah must be available");
            helper.assertTrue(second.tryTakeMouthpiece(player), "Second hookah must be available");
            helper.assertTrue(first.getActivePlayerUuid() == null, "Previous hookah must be released");
            first.releaseMouthpiece();
            helper.assertTrue(player.getUUID().equals(second.getActivePlayerUuid()), "Old release must preserve new owner");
            helper.assertTrue(second.getBlockPos().equals(ActiveSessions.server().get(player.getUUID()).pos()), "New session must survive old release");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void removingWornHookahReleasesOriginalWithoutReequippingIt(GameTestHelper helper) {
        try (Players players = new Players(helper)) {
            ServerPlayer smoker = players.create(helper.getLevel());
            ServerPlayer wearer = players.create(helper.getLevel());
            ItemStack stack = equip(wearer);
            helper.assertTrue(WornHookah.tryTakeMouthpiece(smoker, wearer, stack), "Guest must be able to claim hookah");
            wearer.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            ActiveSessions.server().tick();
            helper.assertTrue(WornHookah.getActivePlayerUuid(stack) == null, "Removed stack must be released");
            helper.assertTrue(ActiveSessions.server().getWornWearer(smoker.getUUID()) == null, "Session must end after removal");
            helper.assertTrue(wearer.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "Release must not reequip the old stack");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void transferredCopyCanBeClaimedAndOldReleaseDoesNotClearIt(GameTestHelper helper) {
        try (Players players = new Players(helper)) {
            ServerPlayer smoker = players.create(helper.getLevel());
            ServerPlayer firstWearer = players.create(helper.getLevel());
            ServerPlayer nextWearer = players.create(helper.getLevel());
            ItemStack original = equip(firstWearer);
            helper.assertTrue(WornHookah.tryTakeMouthpiece(smoker, firstWearer, original), "Original hookah must be available");
            ItemStack transferred = original.copy();
            firstWearer.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            nextWearer.setItemSlot(EquipmentSlot.CHEST, transferred);
            helper.assertTrue(WornHookah.tryTakeMouthpiece(smoker, nextWearer, transferred), "Transferred copy must not remain busy");
            WornHookah.releaseMouthpiece(firstWearer, original);
            helper.assertTrue(WornHookah.getActivePlayerUuid(original) == null, "Original must be released");
            helper.assertTrue(smoker.getUUID().equals(WornHookah.getActivePlayerUuid(transferred)), "Copy must keep its new owner");
            helper.assertTrue(nextWearer.getUUID().equals(ActiveSessions.server().getWornWearer(smoker.getUUID())), "New session must survive old release");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void logoutOfWearerReleasesTheirGuest(GameTestHelper helper) {
        try (Players players = new Players(helper)) {
            ServerPlayer smoker = players.create(helper.getLevel());
            ServerPlayer wearer = players.create(helper.getLevel());
            ItemStack stack = equip(wearer);
            helper.assertTrue(WornHookah.tryTakeMouthpiece(smoker, wearer, stack), "Guest must be able to claim hookah");
            ServerEvents.onPlayerLogout(new PlayerEvent.PlayerLoggedOutEvent(wearer));
            helper.assertTrue(WornHookah.getActivePlayerUuid(stack) == null, "Logout must release worn hookah");
            helper.assertTrue(ActiveSessions.server().getWornWearer(smoker.getUUID()) == null, "Logout must release guest session");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void differentDimensionsCannotClaimEachOthersHookahs(GameTestHelper helper) {
        try (Players players = new Players(helper)) {
            ServerPlayer smoker = players.create(helper.getLevel());
            ServerPlayer wearer = players.create(helper.getLevel().getServer().getLevel(Level.NETHER));
            ItemStack stack = equip(wearer);
            helper.assertTrue(!WornHookah.tryTakeMouthpiece(smoker, wearer, stack), "Claim must require the same dimension");
            helper.assertTrue(WornHookah.getActivePlayerUuid(stack) == null, "Rejected claim must not change ownership");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void wornHoseSlotRejectsOtherItemsAndLocksDuringUse(GameTestHelper helper) {
        try (Players players = new Players(helper)) {
            ServerPlayer player = players.create(helper.getLevel());
            ItemStack stack = equip(player);
            HookahMenu menu = new HookahMenu(1, player.getInventory(), player.getUUID());
            var slot = menu.getSlot(HookahBlockEntity.SLOT_HOSE);
            helper.assertTrue(!slot.mayPlace(new ItemStack(Items.DIAMOND)), "Hose slot must reject unrelated items");
            helper.assertTrue(!slot.mayPlace(stack.copy()), "Hose slot must reject nested hookahs");
            helper.assertTrue(slot.mayPlace(new ItemStack(ModItems.SHORT_HOOKAH_HOSE.get())), "Hose slot must accept hoses");
            helper.assertTrue(slot.getMaxStackSize() == 1, "Hose slot must hold only one hose");
            helper.assertTrue(WornHookah.tryTakeMouthpiece(player, player, stack), "Player must be able to claim worn hookah");
            helper.assertTrue(!slot.mayPickup(player), "Busy hose must be locked");
            helper.assertTrue(!slot.mayPlace(new ItemStack(ModItems.SHORT_HOOKAH_HOSE.get())), "Busy hose must reject replacement");
            helper.assertTrue(menu.quickMoveStack(player, HookahBlockEntity.SLOT_HOSE).isEmpty(), "Shift click must respect the lock");
            helper.assertTrue(menu.stillValid(player), "Menu must remain valid while equipped");
            equip(player);
            helper.assertTrue(!menu.stillValid(player), "Menu must close after equipment replacement");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void hostileSmokeRespectsPvpWhileStillAffectingMobs(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        boolean previousPvp = server.isPvpAllowed();
        try (Players players = new Players(helper)) {
            ServerPlayer smoker = players.create(helper.getLevel());
            ServerPlayer target = players.create(helper.getLevel());
            target.setPos(smoker.position().add(0, 0, 2));
            var cow = helper.spawn(EntityType.COW, new BlockPos(0, 1, 3));
            server.setPvpAllowed(false);
            ModItems.TOBACCO_POISON.get().onExhale(helper.getLevel(), smoker, 1.0f, 1.0f, 1.0f);
            helper.assertTrue(!target.hasEffect(MobEffects.POISON), "Poison smoke must respect pvp=false");
            helper.assertTrue(cow.hasEffect(MobEffects.POISON), "Smoke must still affect mobs");
            ModItems.TOBACCO_ICE.get().onExhale(helper.getLevel(), smoker, 1.0f, 1.0f, 1.0f);
            helper.assertTrue(!target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Ice smoke must respect pvp=false");
            ModItems.TOBACCO_FIRE.get().onExhale(helper.getLevel(), smoker, 1.0f, 1.0f, 1.0f);
            helper.assertTrue(!target.isOnFire(), "Fire smoke must respect pvp=false");
            server.setPvpAllowed(true);
            ModItems.TOBACCO_POISON.get().onExhale(helper.getLevel(), smoker, 1.0f, 1.0f, 1.0f);
            helper.assertTrue(target.hasEffect(MobEffects.POISON), "Smoke must affect players when PvP is enabled");
            helper.succeed();
        } finally {
            server.setPvpAllowed(previousPvp);
        }
    }

    @GameTest(template = "empty")
    public static void offHandInteractionDoesNotUndoMainHandClaim(GameTestHelper helper) {
        try (Players players = new Players(helper)) {
            ServerPlayer smoker = players.create(helper.getLevel());
            ServerPlayer wearer = players.create(helper.getLevel());
            ItemStack stack = equip(wearer);
            var main = new PlayerInteractEvent.EntityInteract(smoker, InteractionHand.MAIN_HAND, wearer);
            NeoForge.EVENT_BUS.post(main);
            helper.assertTrue(main.isCanceled() && main.getCancellationResult().consumesAction(), "Main hand must consume the interaction");
            helper.assertTrue(smoker.getUUID().equals(WornHookah.getActivePlayerUuid(stack)), "Main hand must claim hookah");
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(smoker, InteractionHand.OFF_HAND, wearer));
            helper.assertTrue(smoker.getUUID().equals(WornHookah.getActivePlayerUuid(stack)), "Off hand must preserve the claim");
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(smoker, InteractionHand.MAIN_HAND, wearer));
            helper.assertTrue(WornHookah.getActivePlayerUuid(stack) == null, "A second main hand click must release hookah");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void fireSmokeConsumesTntExactlyOnce(GameTestHelper helper) {
        BlockPos target = new BlockPos(0, 2, 3);
        AABB area = new AABB(helper.absolutePos(target)).inflate(1);
        try (Players players = new Players(helper)) {
            ServerPlayer smoker = players.create(helper.getLevel());
            helper.setBlock(target, Blocks.TNT);
            ModItems.TOBACCO_FIRE.get().onExhale(helper.getLevel(), smoker, 1, 1, 1);
            helper.assertBlockPresent(Blocks.AIR, target);
            var primed = helper.getLevel().getEntitiesOfClass(PrimedTnt.class, area);
            helper.assertTrue(primed.size() == 1 && primed.getFirst().getOwner() == smoker, "One block must produce one TNT owned by smoker");
            ModItems.TOBACCO_FIRE.get().onExhale(helper.getLevel(), smoker, 1, 1, 1);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(PrimedTnt.class, area).size() == 1, "Repeated exhale must not duplicate TNT");
            helper.succeed();
        } finally {
            helper.getLevel().getEntitiesOfClass(PrimedTnt.class, area).forEach(PrimedTnt::discard);
        }
    }

    @GameTest(template = "empty")
    public static void canceledBlockBreakPreventsTntIgnition(GameTestHelper helper) {
        BlockPos target = new BlockPos(0, 2, 3);
        BlockPos absolute = helper.absolutePos(target);
        List<BlockEvent.BreakEvent> events = new ArrayList<>();
        Consumer<BlockEvent.BreakEvent> deny = event -> {
            if (event.getPos().equals(absolute)) {
                events.add(event);
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(deny);
        try (Players players = new Players(helper)) {
            ServerPlayer smoker = players.create(helper.getLevel());
            helper.setBlock(target, Blocks.TNT);
            ModItems.TOBACCO_FIRE.get().onExhale(helper.getLevel(), smoker, 1, 1, 1);
            helper.assertTrue(events.size() == 1 && events.getFirst().getPlayer() == smoker, "TNT must request a cancellable break for smoker");
            helper.assertBlockPresent(Blocks.TNT, target);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(PrimedTnt.class, new AABB(absolute).inflate(1)).isEmpty(), "Denied ignition must not spawn TNT");
            helper.succeed();
        } finally {
            NeoForge.EVENT_BUS.unregister(deny);
            helper.getLevel().getEntitiesOfClass(PrimedTnt.class, new AABB(absolute).inflate(1)).forEach(PrimedTnt::discard);
        }
    }

    @GameTest(template = "empty")
    public static void canceledPlacementRestoresAirAndSourceWater(GameTestHelper helper) {
        BlockPos target = new BlockPos(0, 2, 3);
        BlockPos fire = new BlockPos(0, 2, 2);
        List<BlockEvent.EntityPlaceEvent> events = new ArrayList<>();
        try (Players players = new Players(helper)) {
            ServerPlayer smoker = players.create(helper.getLevel());
            Consumer<BlockEvent.EntityPlaceEvent> deny = event -> {
                if (event.getEntity() == smoker) {
                    events.add(event);
                    event.setCanceled(true);
                }
            };
            NeoForge.EVENT_BUS.addListener(deny);
            try {
                helper.setBlock(target, Blocks.STONE);
                helper.setBlock(fire.below(), Blocks.STONE);
                ModItems.TOBACCO_FIRE.get().onExhale(helper.getLevel(), smoker, 1, 1, 1);
                helper.assertTrue(events.size() == 1 && events.getFirst().getPlacedBlock().is(Blocks.FIRE), "Fire placement event must expose the proposed fire");
                helper.assertTrue(events.getFirst().getBlockSnapshot().getState().isAir(), "Fire snapshot must preserve previous air");
                helper.assertBlockPresent(Blocks.AIR, fire);
                helper.setBlock(target, Blocks.WATER);
                ModItems.TOBACCO_ICE.get().onExhale(helper.getLevel(), smoker, 1, 1, 1);
                helper.assertTrue(events.size() == 2 && events.getLast().getPlacedBlock().is(Blocks.ICE), "Ice placement event must expose the proposed ice");
                helper.assertBlockPresent(Blocks.WATER, target);
                helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(target)).isSource(), "Denied freezing must preserve a water source");
                helper.assertTrue(!helper.getLevel().captureBlockSnapshots && !helper.getLevel().restoringBlockSnapshots
                        && helper.getLevel().capturedBlockSnapshots.isEmpty(), "Canceled changes must leave snapshot tracking clean");
                helper.succeed();
            } finally {
                NeoForge.EVENT_BUS.unregister(deny);
            }
        }
    }

    @GameTest(template = "empty")
    public static void allowedSmokeCreatesFireAndFreezesOnlySourceWater(GameTestHelper helper) {
        try (Players players = new Players(helper)) {
            ServerPlayer smoker = players.create(helper.getLevel());
            BlockPos target = new BlockPos(0, 2, 3);
            BlockPos fire = new BlockPos(0, 2, 2);
            BlockPos flowing = target.east();
            helper.setBlock(target, Blocks.STONE);
            helper.setBlock(fire.below(), Blocks.STONE);
            ModItems.TOBACCO_FIRE.get().onExhale(helper.getLevel(), smoker, 1, 1, 1);
            helper.assertBlockPresent(Blocks.FIRE, fire);
            helper.setBlock(fire, Blocks.AIR);
            helper.setBlock(target, Blocks.WATER);
            helper.setBlock(flowing, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
            ModItems.TOBACCO_ICE.get().onExhale(helper.getLevel(), smoker, 1, 1, 1);
            helper.assertBlockPresent(Blocks.ICE, target);
            helper.assertBlockPresent(Blocks.WATER, flowing);
            helper.assertTrue(!helper.getLevel().getFluidState(helper.absolutePos(flowing)).isSource(), "Flowing water must remain flowing");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void adventureSmokeCannotChangeBlocks(GameTestHelper helper) {
        BlockPos target = new BlockPos(0, 2, 3);
        try (Players players = new Players(helper)) {
            ServerPlayer smoker = players.create(helper.getLevel());
            smoker.setGameMode(GameType.ADVENTURE);
            BlockPos fire = new BlockPos(0, 2, 2);
            helper.setBlock(target, Blocks.STONE);
            helper.setBlock(fire.below(), Blocks.STONE);
            ModItems.TOBACCO_FIRE.get().onExhale(helper.getLevel(), smoker, 1, 1, 1);
            helper.assertBlockPresent(Blocks.AIR, fire);
            helper.setBlock(target, Blocks.TNT);
            ModItems.TOBACCO_FIRE.get().onExhale(helper.getLevel(), smoker, 1, 1, 1);
            helper.assertBlockPresent(Blocks.TNT, target);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(PrimedTnt.class, new AABB(helper.absolutePos(target)).inflate(1)).isEmpty(), "Adventure smoke must not spawn TNT");
            helper.setBlock(target, Blocks.WATER);
            ModItems.TOBACCO_ICE.get().onExhale(helper.getLevel(), smoker, 1, 1, 1);
            helper.assertBlockPresent(Blocks.WATER, target);
            helper.succeed();
        } finally {
            helper.getLevel().getEntitiesOfClass(PrimedTnt.class, new AABB(helper.absolutePos(target)).inflate(1)).forEach(PrimedTnt::discard);
        }
    }

    @GameTest(template = "empty")
    public static void monsterSoundOriginatesAtTheDrinkersMouth(GameTestHelper helper) {
        List<PlayLevelSoundEvent.AtPosition> sounds = new ArrayList<>();
        Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
            if (event.getSound().value() == ModSounds.WHITE_MONSTER_BURP.get()) sounds.add(event);
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try (Players players = new Players(helper)) {
            ServerPlayer drinker = players.create(helper.getLevel());
            Vec3 mouth = drinker.getEyePosition().add(drinker.getLookAngle().scale(0.35)).add(0, -0.18, 0);
            ItemStack result = ModItems.WHITE_MONSTER.get().finishUsingItem(new ItemStack(ModItems.WHITE_MONSTER.get()), helper.getLevel(), drinker);
            helper.assertTrue(sounds.size() == 1, "Drinking must emit one positional world sound");
            helper.assertTrue(sounds.getFirst().getPosition().distanceToSqr(mouth) < 0.000001, "Sound must originate at drinker");
            helper.assertTrue(sounds.getFirst().getSound().value().getRange(sounds.getFirst().getNewVolume()) == 16, "Burp must have a local audible range");
            helper.assertTrue(result.is(ModItems.EMPTY_WHITE_MONSTER.get()) && drinker.hasEffect(MobEffects.MOVEMENT_SPEED), "Drinking must preserve its can and speed effect");
            helper.succeed();
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
    }

    private static ItemStack equip(ServerPlayer wearer) {
        ItemStack stack = new ItemStack(ModItems.HOOKAH.get());
        WornHookah.setItems(stack, List.of(
                new ItemStack(ModItems.LONG_HOOKAH_HOSE.get()),
                new ItemStack(ModItems.HOOKAH_TOBACCO.get(), 2),
                new ItemStack(ModItems.HOOKAH_CHARCOAL.get(), 2),
                new ItemStack(ModItems.HOOKAH_WATER_BOTTLE.get(), 2)));
        wearer.setItemSlot(EquipmentSlot.CHEST, stack);
        return stack;
    }

    @GameTest(template = "empty")
    public static void pickupRespectsAdventureAndBreakCancellation(GameTestHelper helper) {
        try (Players players = new Players(helper)) {
            ServerPlayer player = players.create(helper.getLevel());
            HookahBlockEntity hookah = block(helper, new BlockPos(1, 1, 0));
            ItemStack original = equip(player).copy();
            original.set(DataComponents.CUSTOM_NAME, Component.literal("Named hookah"));
            new HookahProgress(19, 199).write(original);
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            hookah.loadItemsFromStack(original);
            helper.assertTrue(hookah.tryTakeMouthpiece(player), "Hookah must be claimable before pickup");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setShiftKeyDown(true);
            player.setGameMode(GameType.ADVENTURE);
            pickup(player, hookah);
            helper.assertTrue(helper.getLevel().getBlockEntity(hookah.getBlockPos()) == hookah, "Adventure pickup must leave the block intact");
            helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "Adventure pickup must not give an item");
            player.setGameMode(GameType.SURVIVAL);
            List<BlockEvent.BreakEvent> events = new ArrayList<>();
            Consumer<BlockEvent.BreakEvent> deny = event -> {
                if (event.getPlayer() == player && event.getPos().equals(hookah.getBlockPos())) {
                    events.add(event);
                    event.setCanceled(true);
                }
            };
            NeoForge.EVENT_BUS.addListener(deny);
            try {
                pickup(player, hookah);
                helper.assertTrue(events.size() == 1, "Pickup must check break protection");
                helper.assertTrue(helper.getLevel().getBlockEntity(hookah.getBlockPos()) == hookah, "Canceled pickup must leave the block intact");
                helper.assertTrue(player.getUUID().equals(hookah.getActivePlayerUuid()), "Canceled pickup must preserve the session");
                helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "Canceled pickup must not equip a copy");
            } finally {
                NeoForge.EVENT_BUS.unregister(deny);
            }
            pickup(player, hookah);
            ItemStack pickedUp = player.getItemBySlot(EquipmentSlot.CHEST);
            helper.assertTrue(helper.getLevel().getBlockState(hookah.getBlockPos()).isAir(), "Allowed pickup must remove the block");
            helper.assertTrue(original.get(DataComponents.CONTAINER).equals(pickedUp.get(DataComponents.CONTAINER)), "Pickup must preserve contents");
            helper.assertTrue(original.get(DataComponents.CUSTOM_NAME).equals(pickedUp.get(DataComponents.CUSTOM_NAME)), "Pickup must preserve the name");
            helper.assertTrue(new HookahProgress(19, 199).equals(HookahProgress.read(pickedUp)), "Pickup must preserve partial consumption");
            helper.assertTrue(ActiveSessions.server().get(player.getUUID()) == null && WornHookah.getActivePlayerUuid(pickedUp) == null, "Pickup must end the session");
            helper.succeed();
        }
    }

    private static void pickup(ServerPlayer player, HookahBlockEntity hookah) {
        BlockPos pos = hookah.getBlockPos();
        player.gameMode.useItemOn(player, player.level(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    @GameTest(template = "empty")
    public static void breakingDropsOneHookahWithItsRemainingResources(GameTestHelper helper) {
        try (Players players = new Players(helper)) {
            ServerPlayer player = players.create(helper.getLevel());
            BlockPos relative = new BlockPos(2, 1, 0);
            HookahBlockEntity hookah = block(helper, relative);
            ItemStack original = equip(player).copy();
            original.set(DataComponents.CUSTOM_NAME, Component.literal("Used hookah"));
            new HookahProgress(19, 199).write(original);
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            hookah.loadItemsFromStack(original);
            HookahMenu oldMenu = new HookahMenu(1, player.getInventory(), hookah.getBlockPos());
            helper.assertTrue(player.gameMode.destroyBlock(hookah.getBlockPos()), "Survival breaking must succeed");
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(hookah.getBlockPos()).inflate(0.5));
            helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(ModItems.HOOKAH.get()), "Breaking must drop one packed hookah without loose duplicates");
            ItemStack drop = drops.getFirst().getItem().copy();
            helper.assertTrue(original.get(DataComponents.CUSTOM_NAME).equals(drop.get(DataComponents.CUSTOM_NAME)), "Breaking must preserve the name");
            helper.assertTrue(original.get(DataComponents.CONTAINER).equals(drop.get(DataComponents.CONTAINER)), "Breaking must preserve all contents");
            HookahBlockEntity replacement = block(helper, relative);
            replacement.loadItemsFromStack(drop);
            helper.assertTrue(!oldMenu.stillValid(player), "A menu must not retain access to a removed block entity");
            ItemStack restored = new ItemStack(ModItems.HOOKAH.get());
            replacement.saveItemsToStack(restored);
            NonNullList<ItemStack> items = WornHookah.getItems(restored);
            var consumed = HookahProgress.read(restored).consume(items);
            helper.assertTrue(consumed.progress().equals(HookahProgress.EMPTY), "Breaking and replacing must not reset resource counters");
            for (int slot = 1; slot < HookahBlockEntity.SLOT_COUNT; slot++) {
                helper.assertTrue(items.get(slot).getCount() == 1, "The next puff must consume nearly exhausted resources");
            }
            drops.forEach(ItemEntity::discard);
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void creativeBreakingKeepsContentsWithoutDuplicatingThem(GameTestHelper helper) {
        try (Players players = new Players(helper)) {
            ServerPlayer player = players.create(helper.getLevel());
            HookahBlockEntity hookah = block(helper, new BlockPos(2, 1, 0));
            ItemStack original = equip(player).copy();
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            hookah.loadItemsFromStack(original);
            player.setGameMode(GameType.CREATIVE);
            helper.assertTrue(player.gameMode.destroyBlock(hookah.getBlockPos()), "Creative breaking must succeed");
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(hookah.getBlockPos()).inflate(0.5));
            helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(ModItems.HOOKAH.get()), "Creative breaking must drop one packed hookah");
            helper.assertTrue(original.get(DataComponents.CONTAINER).equals(drops.getFirst().getItem().get(DataComponents.CONTAINER)), "Creative breaking must preserve contents");
            drops.forEach(ItemEntity::discard);
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void middleClickKeepsEveryHookahTier(GameTestHelper helper) {
        try (Players players = new Players(helper)) {
            ServerPlayer player = players.create(helper.getLevel());
            HookahBlockEntity hookah = block(helper, new BlockPos(1, 1, 0));
            var expected = List.of(ModItems.HOOKAH.get(), ModItems.HOOKAH_LEATHER.get(), ModItems.HOOKAH_GOLD.get(),
                    ModItems.HOOKAH_IRON.get(), ModItems.HOOKAH_DIAMOND.get(), ModItems.HOOKAH_NETHERITE.get());
            for (HookahTier tier : HookahTier.values()) {
                var state = ModBlocks.HOOKAH.get().defaultBlockState().setValue(HookahBlock.TIER, tier);
                ItemStack copy = state.getCloneItemStack(new BlockHitResult(Vec3.atCenterOf(hookah.getBlockPos()), Direction.UP, hookah.getBlockPos(), false),
                        helper.getLevel(), hookah.getBlockPos(), player);
                helper.assertTrue(copy.is(expected.get(tier.ordinal())), "Middle click must preserve tier " + tier);
                helper.assertTrue(!copy.has(DataComponents.CONTAINER), "Ordinary middle click must not duplicate the inventory");
            }
            var preview = ModBlocks.LUXURY_HOOKAH_PREVIEW.get().defaultBlockState();
            ItemStack copy = preview.getCloneItemStack(new BlockHitResult(Vec3.atCenterOf(hookah.getBlockPos()), Direction.UP, hookah.getBlockPos(), false),
                    helper.getLevel(), hookah.getBlockPos(), player);
            helper.assertTrue(copy.is(ModItems.LUXURY_HOOKAH_PREVIEW.get()), "Preview must clone its own item");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void openWornMenuSynchronizesGuestAndOwnerClaims(GameTestHelper helper) {
        try (Players players = new Players(helper)) {
            ServerPlayer wearer = players.create(helper.getLevel());
            ServerPlayer guest = players.create(helper.getLevel());
            ItemStack stack = equip(wearer);
            HookahMenu menu = new HookahMenu(1, wearer.getInventory(), wearer.getUUID());
            List<Integer> statuses = new ArrayList<>();
            menu.setSynchronizer(new ContainerSynchronizer() {
                @Override
                public void sendInitialData(AbstractContainerMenu container, NonNullList<ItemStack> items, ItemStack carriedItem, int[] initialData) {
                    statuses.add(initialData[0]);
                }

                @Override
                public void sendSlotChange(AbstractContainerMenu container, int slot, ItemStack itemStack) {}

                @Override
                public void sendCarriedChange(AbstractContainerMenu container, ItemStack itemStack) {}

                @Override
                public void sendDataChange(AbstractContainerMenu container, int id, int value) {
                    if (id == 0) statuses.add(value);
                }
            });
            helper.assertTrue(WornHookah.tryTakeMouthpiece(guest, wearer, stack), "Guest must claim the hookah");
            menu.broadcastChanges();
            helper.assertTrue(menu.isInUse() && !menu.isInUseByMe(), "Wearer's menu must recognize the guest");
            helper.assertTrue(!menu.getSlot(0).mayPickup(wearer), "Guest's hose must remain locked");
            WornHookah.releaseMouthpiece(wearer, stack);
            menu.broadcastChanges();
            helper.assertTrue(WornHookah.tryTakeMouthpiece(wearer, wearer, stack), "Owner must claim the released hookah");
            menu.broadcastChanges();
            helper.assertTrue(menu.isInUseByMe(), "Owner's menu must recognize their own claim");
            WornHookah.releaseMouthpiece(wearer, stack);
            menu.broadcastChanges();
            helper.assertTrue(statuses.equals(List.of(0, 2, 0, 1, 0)), "All occupancy changes must reach the open menu's synchronizer");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void clickingHookahStartsTheMouthpieceAnimation(GameTestHelper helper) {
        try (Players players = new Players(helper)) {
            ServerPlayer player = players.create(helper.getLevel());
            HookahBlockEntity hookah = block(helper, new BlockPos(1, 1, 0));
            ItemStack contents = equip(player).copy();
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            hookah.loadItemsFromStack(contents);
            helper.assertTrue(hookah.tryTakeMouthpiece(player), "Player must claim hookah");
            ItemStack mouthpiece = new ItemStack(ModItems.HOOKAH_MOUTHPIECE.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, mouthpiece);
            helper.assertTrue(GeoItem.getId(mouthpiece) == Long.MAX_VALUE, "New mouthpiece must have no animation id");
            player.gameMode.useItemOn(player, helper.getLevel(), mouthpiece, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(hookah.getBlockPos()), Direction.UP, hookah.getBlockPos(), false));
            helper.assertTrue(player.isUsingItem(), "Clicking the claimed block must start smoking");
            helper.assertTrue(GeoItem.getId(mouthpiece) != Long.MAX_VALUE, "Block interaction must trigger GeckoLib animation setup");
            player.stopUsingItem();
            helper.succeed();
        }
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void abandonedLightExpiresAndItsCleanupTickIsSaved(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 0);
        BlockPos pos = helper.absolutePos(relative);
        var light = ModBlocks.HOOKAH_LIGHT.get();
        helper.setBlock(relative, light.defaultBlockState().setValue(LightBlock.WATERLOGGED, true));
        var chunk = helper.getLevel().getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        var saved = (ListTag) chunk.getTicksForSerialization().blocks().save(helper.getLevel().getGameTime(), block -> BuiltInRegistries.BLOCK.getKey(block).toString());
        var loaded = LevelChunkTicks.load(saved, id -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id)), chunk.getPos());
        helper.assertTrue(loaded.hasScheduledTick(pos, light), "Cleanup must survive chunk tick serialization");
        BlockPos vanilla = new BlockPos(0, 3, 0);
        helper.setBlock(vanilla, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, HookahLightBlock.LIGHT_LEVEL));
        helper.runAfterDelay(HookahLightBlock.CHECK_INTERVAL + 2, () -> {
            helper.assertBlockPresent(Blocks.WATER, relative);
            helper.assertBlockPresent(Blocks.LIGHT, vanilla);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void lightRemainsWhileAnotherWearerStillUsesIt(GameTestHelper helper) {
        try (Players players = new Players(helper)) {
            ServerPlayer first = players.create(helper.getLevel());
            ServerPlayer second = players.create(helper.getLevel());
            equip(first);
            equip(second);
            BlockPos pos = HookahLightBlock.lightPosition(first);
            var state = ModBlocks.HOOKAH_LIGHT.get().defaultBlockState();
            helper.getLevel().setBlock(pos, state, 3);
            first.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            state.tick(helper.getLevel(), pos, helper.getLevel().random);
            helper.assertTrue(helper.getLevel().getBlockState(pos).is(ModBlocks.HOOKAH_LIGHT.get()), "Remaining wearer must keep the shared light");
            second.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            state.tick(helper.getLevel(), pos, helper.getLevel().random);
            helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "Light must disappear after its last source is gone");
            helper.succeed();
        }
    }

    private static HookahBlockEntity block(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModBlocks.HOOKAH.get());
        HookahBlockEntity hookah = helper.getBlockEntity(pos);
        hookah.setHoseType(HookahHoseType.LONG);
        return hookah;
    }

    private static final class Players implements AutoCloseable {
        private final GameTestHelper helper;
        private final List<ServerPlayer> players = new ArrayList<>();

        private Players(GameTestHelper helper) {
            this.helper = helper;
        }

        private ServerPlayer create(ServerLevel level) {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "HookahTest");
            ServerPlayer player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
            player.connection = new FakePlayer(level, profile).connection;
            BlockPos pos = helper.absolutePos(new BlockPos(0, 1, 0));
            player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            player.setYRot(0);
            player.setXRot(0);
            player.getInventory().add(new ItemStack(ModItems.HOOKAH_MOUTHPIECE.get()));
            level.addNewPlayer(player);
            players.add(player);
            return player;
        }

        @Override
        public void close() {
            for (ServerPlayer player : players) {
                ActiveSessions.server().release(player.getUUID());
                player.discard();
            }
        }
    }
}

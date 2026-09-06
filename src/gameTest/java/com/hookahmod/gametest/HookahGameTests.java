package com.hookahmod.gametest;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.event.ActiveSessions;
import com.hookahmod.event.ServerEvents;
import com.hookahmod.item.HookahHoseType;
import com.hookahmod.item.WornHookah;
import com.hookahmod.menu.HookahMenu;
import com.hookahmod.registry.ModBlocks;
import com.hookahmod.registry.ModItems;
import com.hookahmod.registry.ModSounds;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

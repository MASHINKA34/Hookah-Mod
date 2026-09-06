package com.hookahmod.gametest;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.event.ActiveSessions;
import com.hookahmod.event.ServerEvents;
import com.hookahmod.item.HookahHoseType;
import com.hookahmod.item.WornHookah;
import com.hookahmod.menu.HookahMenu;
import com.hookahmod.registry.ModBlocks;
import com.hookahmod.registry.ModItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

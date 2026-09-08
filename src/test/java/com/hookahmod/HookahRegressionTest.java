package com.hookahmod;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.config.HookahConfig;
import com.hookahmod.item.HookahHoseType;
import com.hookahmod.item.WornHookah;
import com.hookahmod.recipe.HookahUpgradeRecipe;
import com.hookahmod.registry.ModBlocks;
import com.hookahmod.registry.ModItems;
import com.hookahmod.smoking.HookahProgress;
import com.hookahmod.smoking.IntoxicationBand;
import com.hookahmod.smoking.IntoxicationState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EphemeralTestServerProvider.class)
class HookahRegressionTest {
    @Test
    void allCraftingUpgradesPreserveContentsAndProgress(MinecraftServer server) {
        for (String tier : List.of("leather", "gold", "iron", "diamond")) {
            var holder = server.getRecipeManager().byKey(HookahMod.id("hookah_" + tier)).orElseThrow();
            HookahUpgradeRecipe recipe = assertInstanceOf(HookahUpgradeRecipe.class, holder.value());
            ItemStack base = filledHookah();
            base.set(DataComponents.CUSTOM_NAME, Component.literal("My hookah"));
            new HookahProgress(19, 199).write(base);
            WornHookah.setActivePlayerUuid(base, UUID.randomUUID());
            List<ItemStack> grid = new ArrayList<>();
            recipe.getIngredients().forEach(ingredient -> grid.add(ingredient.getItems()[0].copy()));
            grid.set(4, base);
            CraftingInput input = CraftingInput.of(3, 3, grid);
            assertTrue(recipe.matches(input, server.overworld()));
            ItemStack result = recipe.assemble(input, server.registryAccess());
            assertEquals(HookahMod.id("hookah_" + tier), net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem()));
            assertEquals(base.get(DataComponents.CONTAINER), result.get(DataComponents.CONTAINER));
            assertEquals(base.get(DataComponents.CUSTOM_NAME), result.get(DataComponents.CUSTOM_NAME));
            assertEquals(HookahProgress.read(base), HookahProgress.read(result));
            assertNull(WornHookah.getActivePlayerUuid(result));
            assertNotNull(WornHookah.getActivePlayerUuid(base));
        }
    }

    @Test
    void blockItemRoundTripPreservesPartialConsumption(MinecraftServer server) {
        ItemStack original = filledHookah();
        original.set(DataComponents.CUSTOM_NAME, Component.literal("My hookah"));
        original.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Keeps its history"))));
        CustomData.update(DataComponents.CUSTOM_DATA, original, tag -> tag.putString("OwnerNote", "Keep me"));
        WornHookah.setActivePlayerUuid(original, UUID.randomUUID());
        new HookahProgress(19, 199).write(original);
        HookahBlockEntity hookah = new HookahBlockEntity(BlockPos.ZERO, ModBlocks.HOOKAH.get().defaultBlockState());
        hookah.loadItemsFromStack(original);
        hookah = assertInstanceOf(HookahBlockEntity.class, BlockEntity.loadStatic(BlockPos.ZERO, hookah.getBlockState(),
                hookah.saveWithFullMetadata(server.registryAccess()), server.registryAccess()));
        ItemStack pickedUp = new ItemStack(ModItems.HOOKAH.get());
        hookah.saveItemsToStack(pickedUp);
        assertEquals(original.get(DataComponents.CUSTOM_NAME), pickedUp.get(DataComponents.CUSTOM_NAME));
        assertEquals(original.get(DataComponents.LORE), pickedUp.get(DataComponents.LORE));
        assertEquals("Keep me", pickedUp.get(DataComponents.CUSTOM_DATA).copyTag().getString("OwnerNote"));
        assertNull(WornHookah.getActivePlayerUuid(pickedUp));
        assertEquals(original.get(DataComponents.CONTAINER), pickedUp.get(DataComponents.CONTAINER));
        assertEquals(HookahProgress.read(original), HookahProgress.read(pickedUp));
        NonNullList<ItemStack> items = WornHookah.getItems(pickedUp);
        var consumed = HookahProgress.read(pickedUp).consume(items);
        assertEquals(HookahProgress.EMPTY, consumed.progress());
        assertEquals(1, items.get(HookahBlockEntity.SLOT_TOBACCO).getCount());
        assertEquals(1, items.get(HookahBlockEntity.SLOT_COAL).getCount());
        assertEquals(1, items.get(HookahBlockEntity.SLOT_WATER).getCount());
    }

    @Test
    void creativeDataCopyUsesTheCurrentContents(MinecraftServer server) {
        HookahBlockEntity hookah = new HookahBlockEntity(BlockPos.ZERO, ModBlocks.HOOKAH.get().defaultBlockState());
        hookah.loadItemsFromStack(filledHookah());
        hookah.getInventory().removeItem(HookahBlockEntity.SLOT_TOBACCO, 1);
        hookah.getInventory().removeItemNoUpdate(HookahBlockEntity.SLOT_COAL);
        ItemStack copy = new ItemStack(ModItems.HOOKAH.get());
        hookah.saveToItem(copy, server.registryAccess());
        HookahBlockEntity placed = new HookahBlockEntity(BlockPos.ZERO, hookah.getBlockState());
        placed.loadItemsFromStack(copy);
        assertEquals(1, placed.getInventory().getItem(HookahBlockEntity.SLOT_TOBACCO).getCount());
        assertTrue(placed.getInventory().getItem(HookahBlockEntity.SLOT_COAL).isEmpty());
    }

    @Test
    void combatTobaccoAndMonsterUseTheirCorrectThresholds(MinecraftServer server) {
        NonNullList<ItemStack> items = WornHookah.getItems(filledHookah());
        items.set(HookahBlockEntity.SLOT_TOBACCO, new ItemStack(ModItems.TOBACCO_FIRE.get(), 2));
        items.set(HookahBlockEntity.SLOT_WATER, new ItemStack(ModItems.WHITE_MONSTER.get(), 2));
        var consumed = new HookahProgress(9, 199).consume(items);
        assertEquals(HookahProgress.EMPTY, consumed.progress());
        assertTrue(consumed.emptyCan());
        assertEquals(1, items.get(HookahBlockEntity.SLOT_TOBACCO).getCount());
        assertEquals(1, items.get(HookahBlockEntity.SLOT_COAL).getCount());
        assertEquals(1, items.get(HookahBlockEntity.SLOT_WATER).getCount());
    }

    @Test
    void waterRecipeRejectsOtherPotions(MinecraftServer server) {
        var holder = server.getRecipeManager().byKey(HookahMod.id("hookah_water_bottle")).orElseThrow();
        var recipe = assertInstanceOf(net.minecraft.world.item.crafting.ShapelessRecipe.class, holder.value());
        for (var potion : List.of(Potions.WATER, Potions.HEALING, Potions.POISON, Potions.AWKWARD)) {
            CraftingInput input = CraftingInput.of(2, 2, List.of(
                    PotionContents.createItemStack(Items.POTION, potion),
                    new ItemStack(Items.GLASS), new ItemStack(Items.GLASS), new ItemStack(Items.GLASS)));
            assertEquals(potion == Potions.WATER, recipe.matches(input, server.overworld()));
        }
    }

    @Test
    void configuredPuffLimitsDriveConsumption(MinecraftServer server) {
        int previousSolid = HookahConfig.solidPuffsPerCharge;
        int previousWater = HookahConfig.waterPuffsPerBottle;
        try {
            HookahConfig.solidPuffsPerCharge = 3;
            HookahConfig.waterPuffsPerBottle = 5;
            NonNullList<ItemStack> items = WornHookah.getItems(filledHookah());

            var afterFirst = new HookahProgress(0, 0).consume(items);
            assertEquals(1, afterFirst.progress().smokePuffs());
            assertEquals(1, afterFirst.progress().waterPuffs());
            assertFalse(afterFirst.itemsChanged());
            assertEquals(2, items.get(HookahBlockEntity.SLOT_TOBACCO).getCount());

            var afterThird = new HookahProgress(2, 2).consume(items);
            assertEquals(0, afterThird.progress().smokePuffs());
            assertTrue(afterThird.itemsChanged());
            assertEquals(1, items.get(HookahBlockEntity.SLOT_TOBACCO).getCount());
            assertEquals(1, items.get(HookahBlockEntity.SLOT_COAL).getCount());

            var afterWater = new HookahProgress(0, 4).consume(items);
            assertEquals(0, afterWater.progress().waterPuffs());
            assertEquals(1, items.get(HookahBlockEntity.SLOT_WATER).getCount());
        } finally {
            HookahConfig.solidPuffsPerCharge = previousSolid;
            HookahConfig.waterPuffsPerBottle = previousWater;
        }
    }

    @Test
    void configuredThresholdsDriveIntoxicationBands(MinecraftServer server) {
        float previousTrip = HookahConfig.tripThreshold;
        float previousOverdose = HookahConfig.overdoseThreshold;
        try {
            assertEquals(IntoxicationBand.HIGH, IntoxicationState.band(80.0f));
            assertEquals(IntoxicationBand.TRIP, IntoxicationState.band(120.0f));
            HookahConfig.tripThreshold = 70.0f;
            HookahConfig.overdoseThreshold = 90.0f;
            assertEquals(IntoxicationBand.TRIP, IntoxicationState.band(80.0f));
            assertEquals(IntoxicationBand.OVERDOSE, IntoxicationState.band(120.0f));
        } finally {
            HookahConfig.tripThreshold = previousTrip;
            HookahConfig.overdoseThreshold = previousOverdose;
        }
    }

    @Test
    void configuredHoseRangeIsUsedForReachChecks(MinecraftServer server) {
        int previousShort = HookahConfig.shortHoseRange;
        int previousLong = HookahConfig.longHoseRange;
        try {
            HookahConfig.shortHoseRange = 3;
            HookahConfig.longHoseRange = 12;
            assertEquals(0, HookahHoseType.NONE.getMaxLength());
            assertEquals(3, HookahHoseType.SHORT.getMaxLength());
            assertEquals(12, HookahHoseType.LONG.getMaxLength());
            assertEquals(144.0, HookahHoseType.maxRangeSqr());
        } finally {
            HookahConfig.shortHoseRange = previousShort;
            HookahConfig.longHoseRange = previousLong;
        }
    }

    @Test
    void tripVisualsFollowTheConfiguredBands(MinecraftServer server) {
        float previousTrip = HookahConfig.tripThreshold;
        float previousOverdose = HookahConfig.overdoseThreshold;
        try {
            assertEquals(0.0f, IntoxicationState.tripVisualStrength(99.0f));
            assertTrue(IntoxicationState.tripVisualStrength(100.0f) > 0.0f);
            assertTrue(IntoxicationState.tripVisualStrength(140.0f) > IntoxicationState.tripVisualStrength(110.0f));
            // Only the overdose band is allowed to reach full strength.
            assertTrue(IntoxicationState.tripVisualStrength(149.0f) <= 0.72f);
            assertEquals(1.0f, IntoxicationState.tripVisualStrength(400.0f), 0.0001f);

            HookahConfig.tripThreshold = 40.0f;
            HookahConfig.overdoseThreshold = 60.0f;
            assertEquals(0.0f, IntoxicationState.tripVisualStrength(39.0f),
                    "Visuals must stay off below the configured trip band");
            assertTrue(IntoxicationState.tripVisualStrength(40.0f) > 0.0f,
                    "Visuals must start at the configured trip band");
            assertTrue(IntoxicationState.tripVisualStrength(59.0f) <= 0.72f);
            assertEquals(1.0f, IntoxicationState.tripVisualStrength(400.0f), 0.0001f);
        } finally {
            HookahConfig.tripThreshold = previousTrip;
            HookahConfig.overdoseThreshold = previousOverdose;
        }
    }

    @Test
    void consumableCheckIsSharedByEveryCarrier(MinecraftServer server) {
        ItemStack stack = filledHookah();
        assertTrue(WornHookah.hasAllConsumables(stack));

        HookahBlockEntity hookah = new HookahBlockEntity(BlockPos.ZERO, ModBlocks.HOOKAH.get().defaultBlockState());
        hookah.loadItemsFromStack(stack);
        assertTrue(hookah.hasAllConsumables());

        hookah.getInventory().removeItemNoUpdate(HookahBlockEntity.SLOT_COAL);
        assertFalse(hookah.hasAllConsumables());

        NonNullList<ItemStack> items = WornHookah.getItems(stack);
        items.set(HookahBlockEntity.SLOT_WATER, ItemStack.EMPTY);
        WornHookah.setItems(stack, items);
        assertFalse(WornHookah.hasAllConsumables(stack));
    }

    @Test
    void switchingTobaccosKeepsTheirConsumptionAcrossBlockAndItemSaves(MinecraftServer server) {
        ItemStack stack = filledHookah();
        var items = WornHookah.getItems(stack);
        ItemStack combat = new ItemStack(ModItems.TOBACCO_FIRE.get(), 2);
        ItemStack regular = items.get(HookahBlockEntity.SLOT_TOBACCO);
        items.set(HookahBlockEntity.SLOT_TOBACCO, combat);
        HookahProgress progress = HookahProgress.EMPTY;
        for (int puff = 0; puff < 9; puff++) progress = progress.consume(items).progress();
        items.set(HookahBlockEntity.SLOT_TOBACCO, regular);
        for (int puff = 0; puff < 11; puff++) progress = progress.consume(items).progress();
        WornHookah.setItems(stack, items);
        progress.write(stack);
        HookahBlockEntity block = new HookahBlockEntity(BlockPos.ZERO, ModBlocks.HOOKAH.get().defaultBlockState());
        block.loadItemsFromStack(stack);
        block = assertInstanceOf(HookahBlockEntity.class, BlockEntity.loadStatic(BlockPos.ZERO, block.getBlockState(),
                block.saveWithFullMetadata(server.registryAccess()), server.registryAccess()));
        ItemStack restored = new ItemStack(ModItems.HOOKAH.get());
        block.saveItemsToStack(restored);
        items = WornHookah.getItems(restored);
        items.set(HookahBlockEntity.SLOT_TOBACCO, combat);
        progress = HookahProgress.read(restored).consume(items).progress();
        assertEquals(1, combat.getCount());
        assertEquals(2, regular.getCount());
        assertEquals(1, items.get(HookahBlockEntity.SLOT_COAL).getCount());
        items.set(HookahBlockEntity.SLOT_TOBACCO, regular);
        for (int puff = 0; puff < 9; puff++) progress = progress.consume(items).progress();
        assertEquals(1, regular.getCount());
        assertTrue(items.get(HookahBlockEntity.SLOT_COAL).isEmpty());
    }

    @Test
    void switchingLiquidsCannotTransferConsumptionOrCreateFreeCans(MinecraftServer server) {
        var items = WornHookah.getItems(filledHookah());
        items.get(HookahBlockEntity.SLOT_TOBACCO).setCount(64);
        items.get(HookahBlockEntity.SLOT_COAL).setCount(64);
        ItemStack water = items.get(HookahBlockEntity.SLOT_WATER);
        HookahProgress progress = HookahProgress.EMPTY;
        for (int puff = 0; puff < 199; puff++) progress = progress.consume(items).progress();
        ItemStack monster = new ItemStack(ModItems.WHITE_MONSTER.get(), 2);
        items.set(HookahBlockEntity.SLOT_WATER, monster);
        var consumed = progress.consume(items);
        assertFalse(consumed.emptyCan());
        assertEquals(2, monster.getCount());
        items.set(HookahBlockEntity.SLOT_WATER, water);
        consumed = consumed.progress().consume(items);
        assertEquals(1, water.getCount());
        assertFalse(consumed.emptyCan());
        items.set(HookahBlockEntity.SLOT_WATER, monster);
        progress = consumed.progress();
        for (int puff = 0; puff < 198; puff++) progress = progress.consume(items).progress();
        consumed = progress.consume(items);
        assertTrue(consumed.emptyCan());
        assertEquals(1, monster.getCount());
    }

    @Test
    void legacyProgressIsBoundBeforeReplacingWornContents(MinecraftServer server) {
        ItemStack stack = filledHookah();
        new HookahProgress(19, 199).write(stack);
        var items = WornHookah.getItems(stack);
        ItemStack regular = items.get(HookahBlockEntity.SLOT_TOBACCO);
        items.set(HookahBlockEntity.SLOT_TOBACCO, new ItemStack(ModItems.TOBACCO_FIRE.get(), 2));
        WornHookah.setItems(stack, items);
        var consumed = HookahProgress.read(stack).consume(items);
        assertEquals(2, items.get(HookahBlockEntity.SLOT_TOBACCO).getCount());
        items.set(HookahBlockEntity.SLOT_TOBACCO, regular);
        consumed.progress().consume(items);
        assertEquals(1, regular.getCount());
    }

    @Test
    void emptyHookahProgressCanBeRead(MinecraftServer server) {
        assertEquals(HookahProgress.EMPTY, HookahProgress.read(new ItemStack(ModItems.HOOKAH.get())));
    }

    @Test
    void legacyBlockCountersMigrateWithoutLosingPartialCharges(MinecraftServer server) {
        HookahBlockEntity block = new HookahBlockEntity(BlockPos.ZERO, ModBlocks.HOOKAH.get().defaultBlockState());
        block.loadItemsFromStack(filledHookah());
        var saved = block.saveWithFullMetadata(server.registryAccess());
        saved.remove("HookahProgress");
        saved.putInt("SmokeTimer", 19);
        saved.putInt("WaterTimer", 199);
        block = assertInstanceOf(HookahBlockEntity.class, BlockEntity.loadStatic(BlockPos.ZERO, block.getBlockState(),
                saved, server.registryAccess()));
        ItemStack restored = new ItemStack(ModItems.HOOKAH.get());
        block.saveItemsToStack(restored);
        var items = WornHookah.getItems(restored);
        var consumed = HookahProgress.read(restored).consume(items);
        assertEquals(HookahProgress.EMPTY, consumed.progress());
        assertEquals(1, items.get(HookahBlockEntity.SLOT_TOBACCO).getCount());
        assertEquals(1, items.get(HookahBlockEntity.SLOT_COAL).getCount());
        assertEquals(1, items.get(HookahBlockEntity.SLOT_WATER).getCount());
    }

    private static ItemStack filledHookah() {
        ItemStack stack = new ItemStack(ModItems.HOOKAH.get());
        WornHookah.setItems(stack, List.of(
                new ItemStack(ModItems.LONG_HOOKAH_HOSE.get()),
                new ItemStack(ModItems.HOOKAH_TOBACCO.get(), 2),
                new ItemStack(ModItems.HOOKAH_CHARCOAL.get(), 2),
                new ItemStack(ModItems.HOOKAH_WATER_BOTTLE.get(), 2)));
        return stack;
    }
}

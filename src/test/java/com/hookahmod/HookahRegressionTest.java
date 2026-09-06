package com.hookahmod;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.item.WornHookah;
import com.hookahmod.recipe.HookahUpgradeRecipe;
import com.hookahmod.registry.ModBlocks;
import com.hookahmod.registry.ModItems;
import com.hookahmod.smoking.HookahProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingInput;
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
            assertEquals(new HookahProgress(19, 199), HookahProgress.read(result));
            assertNull(WornHookah.getActivePlayerUuid(result));
            assertNotNull(WornHookah.getActivePlayerUuid(base));
        }
    }

    @Test
    void blockItemRoundTripPreservesPartialConsumption(MinecraftServer server) {
        ItemStack original = filledHookah();
        new HookahProgress(19, 199).write(original);
        HookahBlockEntity hookah = new HookahBlockEntity(BlockPos.ZERO, ModBlocks.HOOKAH.get().defaultBlockState());
        hookah.loadItemsFromStack(original);
        ItemStack pickedUp = new ItemStack(ModItems.HOOKAH.get());
        hookah.saveItemsToStack(pickedUp);
        assertEquals(original.get(DataComponents.CONTAINER), pickedUp.get(DataComponents.CONTAINER));
        assertEquals(new HookahProgress(19, 199), HookahProgress.read(pickedUp));
        NonNullList<ItemStack> items = WornHookah.getItems(pickedUp);
        var consumed = HookahProgress.read(pickedUp).consume(items);
        assertEquals(HookahProgress.EMPTY, consumed.progress());
        assertEquals(1, items.get(HookahBlockEntity.SLOT_TOBACCO).getCount());
        assertEquals(1, items.get(HookahBlockEntity.SLOT_COAL).getCount());
        assertEquals(1, items.get(HookahBlockEntity.SLOT_WATER).getCount());
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

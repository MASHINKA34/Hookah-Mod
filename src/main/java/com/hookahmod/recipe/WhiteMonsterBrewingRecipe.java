package com.hookahmod.recipe;

import com.hookahmod.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;

public class WhiteMonsterBrewingRecipe implements IBrewingRecipe {

    @Override
    public boolean isInput(ItemStack stack) {
        return stack.is(Items.POTION)
                && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.AWKWARD);
    }

    @Override
    public boolean isIngredient(ItemStack stack) {
        return stack.is(Items.SUGAR);
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        return isInput(input) && isIngredient(ingredient)
                ? new ItemStack(ModItems.WHITE_MONSTER.get())
                : ItemStack.EMPTY;
    }
}

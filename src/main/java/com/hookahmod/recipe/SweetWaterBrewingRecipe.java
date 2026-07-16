package com.hookahmod.recipe;

import com.hookahmod.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

public class SweetWaterBrewingRecipe extends BrewingRecipe {

    public SweetWaterBrewingRecipe() {
        super(
                DataComponentIngredient.of(false, PotionContents.createItemStack(Items.POTION, Potions.WATER)),
                Ingredient.of(Items.SUGAR),
                new ItemStack(ModItems.SWEET_WATER.get())
        );
    }
}

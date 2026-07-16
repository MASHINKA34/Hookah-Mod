package com.hookahmod.recipe;

import com.hookahmod.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

public class WhiteMonsterBrewingRecipe extends BrewingRecipe {

    public WhiteMonsterBrewingRecipe() {
        super(
                DataComponentIngredient.of(false, PotionContents.createItemStack(Items.POTION, Potions.AWKWARD)),
                Ingredient.of(Items.SUGAR),
                new ItemStack(ModItems.WHITE_MONSTER.get())
        );
    }
}

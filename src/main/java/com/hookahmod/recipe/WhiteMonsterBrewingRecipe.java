package com.hookahmod.recipe;

import com.hookahmod.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;

public class WhiteMonsterBrewingRecipe extends BrewingRecipe {

    public WhiteMonsterBrewingRecipe(ItemLike ingredient) {
        super(
                Ingredient.of(ModItems.SWEET_WATER.get()),
                Ingredient.of(ingredient),
                new ItemStack(ModItems.WHITE_MONSTER.get())
        );
    }
}

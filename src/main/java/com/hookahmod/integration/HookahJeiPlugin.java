package com.hookahmod.integration;

import com.hookahmod.HookahMod;
import com.hookahmod.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.fml.ModList;

import java.util.List;

@JeiPlugin
public final class HookahJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return HookahMod.id("jei_plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        IVanillaRecipeFactory factory = registration.getVanillaRecipeFactory();
        ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        Item coffee = ModList.get().isLoaded("croptopia")
                ? BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("croptopia", "coffee_beans")).orElse(Items.COCOA_BEANS)
                : Items.COCOA_BEANS;
        IJeiBrewingRecipe sweetWater = factory.createBrewingRecipe(
                List.of(new ItemStack(Items.SUGAR)),
                water,
                new ItemStack(ModItems.SWEET_WATER.get()),
                HookahMod.id("brewing/sweet_water")
        );
        IJeiBrewingRecipe whiteMonster = factory.createBrewingRecipe(
                List.of(new ItemStack(coffee)),
                new ItemStack(ModItems.SWEET_WATER.get()),
                new ItemStack(ModItems.WHITE_MONSTER.get()),
                HookahMod.id("brewing/white_monster")
        );
        registration.addRecipes(RecipeTypes.BREWING, List.of(sweetWater, whiteMonster));
    }
}

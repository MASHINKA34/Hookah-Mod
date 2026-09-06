package com.hookahmod.registry;

import com.hookahmod.HookahMod;
import com.hookahmod.recipe.HookahUpgradeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, HookahMod.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, HookahUpgradeRecipe.Serializer> HOOKAH_UPGRADE =
            SERIALIZERS.register("hookah_upgrade", HookahUpgradeRecipe.Serializer::new);

    private ModRecipeSerializers() {}
}

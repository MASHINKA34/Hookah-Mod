package com.hookahmod.recipe;

import com.hookahmod.item.WornHookah;
import com.hookahmod.registry.ModRecipeSerializers;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

public final class HookahUpgradeRecipe extends ShapedRecipe {
    public HookahUpgradeRecipe(ShapedRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.getResultItem(null), recipe.showNotification());
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = getResultItem(registries);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack base = input.getItem(slot);
            if (WornHookah.isHookahStack(base)) {
                ItemStack upgraded = base.transmuteCopy(result.getItem(), result.getCount());
                upgraded.applyComponents(result.getComponentsPatch());
                WornHookah.setActivePlayerUuid(upgraded, null);
                return upgraded;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.HOOKAH_UPGRADE.get();
    }

    public static final class Serializer implements RecipeSerializer<HookahUpgradeRecipe> {
        private static final MapCodec<HookahUpgradeRecipe> CODEC = ShapedRecipe.Serializer.CODEC.xmap(HookahUpgradeRecipe::new, recipe -> recipe);
        private static final StreamCodec<RegistryFriendlyByteBuf, HookahUpgradeRecipe> STREAM_CODEC =
                ShapedRecipe.Serializer.STREAM_CODEC.map(HookahUpgradeRecipe::new, recipe -> recipe);

        @Override
        public MapCodec<HookahUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HookahUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}

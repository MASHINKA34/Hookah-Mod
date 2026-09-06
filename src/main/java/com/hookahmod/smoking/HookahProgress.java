package com.hookahmod.smoking;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.item.AbstractTobaccoItem;
import com.hookahmod.item.TobaccoCategory;
import com.hookahmod.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public record HookahProgress(int smokePuffs, int waterPuffs) {
    public static final HookahProgress EMPTY = new HookahProgress(0, 0);
    private static final String SMOKE_TAG = "HookahSmokeTimer";
    private static final String WATER_TAG = "HookahWaterTimer";

    public HookahProgress {
        smokePuffs = Math.clamp(smokePuffs, 0, 19);
        waterPuffs = Math.clamp(waterPuffs, 0, 199);
    }

    public static HookahProgress read(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return new HookahProgress(tag.getInt(SMOKE_TAG), tag.getInt(WATER_TAG));
    }

    public void write(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(SMOKE_TAG, smokePuffs);
            tag.putInt(WATER_TAG, waterPuffs);
        });
    }

    public Consumption consume(List<ItemStack> items) {
        ItemStack tobacco = items.get(HookahBlockEntity.SLOT_TOBACCO);
        int solidLimit = tobacco.getItem() instanceof AbstractTobaccoItem item && item.category() == TobaccoCategory.COMBAT ? 10 : 20;
        int smoke = smokePuffs + 1;
        int water = waterPuffs + 1;
        boolean changed = false;
        boolean emptyCan = false;
        if (smoke >= solidLimit) {
            smoke = 0;
            tobacco.shrink(1);
            items.get(HookahBlockEntity.SLOT_COAL).shrink(1);
            changed = true;
        }
        if (water >= 200) {
            water = 0;
            ItemStack liquid = items.get(HookahBlockEntity.SLOT_WATER);
            emptyCan = liquid.is(ModItems.WHITE_MONSTER.get());
            liquid.shrink(1);
            changed = true;
        }
        return new Consumption(new HookahProgress(smoke, water), changed, emptyCan);
    }

    public record Consumption(HookahProgress progress, boolean itemsChanged, boolean emptyCan) {}
}

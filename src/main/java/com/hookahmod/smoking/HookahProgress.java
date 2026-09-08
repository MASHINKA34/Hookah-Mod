package com.hookahmod.smoking;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.config.HookahConfig;
import com.hookahmod.item.AbstractTobaccoItem;
import com.hookahmod.item.TobaccoCategory;
import com.hookahmod.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record HookahProgress(int smokePuffs, int waterPuffs,
                             Map<String, Integer> tobaccoUsage, Map<String, Integer> liquidUsage) {
    public static final HookahProgress EMPTY = new HookahProgress(0, 0);
    private static final String SMOKE_TAG = "HookahSmokeTimer";
    private static final String WATER_TAG = "HookahWaterTimer";
    private static final String TOBACCO_USAGE_TAG = "HookahTobaccoUsage";
    private static final String LIQUID_USAGE_TAG = "HookahLiquidUsage";

    public HookahProgress(int smokePuffs, int waterPuffs) {
        this(smokePuffs, waterPuffs, Map.of(), Map.of());
    }

    public HookahProgress {
        smokePuffs = Math.clamp(smokePuffs, 0, maxSolidPuffs() - 1);
        waterPuffs = Math.clamp(waterPuffs, 0, HookahConfig.waterPuffsPerBottle - 1);
        tobaccoUsage = Map.copyOf(tobaccoUsage);
        liquidUsage = Map.copyOf(liquidUsage);
    }

    private static int maxSolidPuffs() {
        return Math.max(HookahConfig.solidPuffsPerCharge, HookahConfig.combatPuffsPerCharge);
    }

    public static HookahProgress read(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        return read(tag).bind(itemAt(contents, HookahBlockEntity.SLOT_TOBACCO),
                itemAt(contents, HookahBlockEntity.SLOT_WATER));
    }

    private static ItemStack itemAt(ItemContainerContents contents, int slot) {
        return slot < contents.getSlots() ? contents.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    public static HookahProgress read(CompoundTag tag) {
        return new HookahProgress(tag.getInt(SMOKE_TAG), tag.getInt(WATER_TAG),
                readUsage(tag.getCompound(TOBACCO_USAGE_TAG)), readUsage(tag.getCompound(LIQUID_USAGE_TAG)));
    }

    public HookahProgress bind(ItemStack tobacco, ItemStack liquid) {
        return new HookahProgress(smokePuffs, waterPuffs, bindUsage(tobaccoUsage, tobacco, smokePuffs),
                bindUsage(liquidUsage, liquid, waterPuffs));
    }

    private static Map<String, Integer> bindUsage(Map<String, Integer> usage, ItemStack item, int legacyPuffs) {
        return usage.isEmpty() && legacyPuffs > 0 && !item.isEmpty()
                ? Map.of(itemKey(item), legacyPuffs) : usage;
    }

    private static Map<String, Integer> readUsage(CompoundTag tag) {
        Map<String, Integer> usage = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            int puffs = tag.getInt(key);
            if (id != null && BuiltInRegistries.ITEM.containsKey(id) && puffs > 0) usage.put(key, puffs);
        }
        return usage;
    }

    private static CompoundTag writeUsage(Map<String, Integer> usage) {
        CompoundTag tag = new CompoundTag();
        usage.forEach(tag::putInt);
        return tag;
    }

    private static String itemKey(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public void write(ItemStack stack) {
        stack.set(DataComponents.CUSTOM_DATA, update(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)));
    }

    public CustomData update(CustomData data) {
        CompoundTag tag = data.copyTag();
        tag.putInt(SMOKE_TAG, smokePuffs);
        tag.putInt(WATER_TAG, waterPuffs);
        tag.put(TOBACCO_USAGE_TAG, writeUsage(tobaccoUsage));
        tag.put(LIQUID_USAGE_TAG, writeUsage(liquidUsage));
        return CustomData.of(tag);
    }

    public Consumption consume(List<ItemStack> items) {
        ItemStack tobacco = items.get(HookahBlockEntity.SLOT_TOBACCO);
        ItemStack liquid = items.get(HookahBlockEntity.SLOT_WATER);
        HookahProgress bound = bind(tobacco, liquid);
        Map<String, Integer> tobaccoCounts = new HashMap<>(bound.tobaccoUsage);
        Map<String, Integer> liquidCounts = new HashMap<>(bound.liquidUsage);
        String tobaccoKey = itemKey(tobacco);
        String liquidKey = itemKey(liquid);
        int solidLimit = tobacco.getItem() instanceof AbstractTobaccoItem item && item.category() == TobaccoCategory.COMBAT
                ? HookahConfig.combatPuffsPerCharge
                : HookahConfig.solidPuffsPerCharge;
        int smoke = Math.min(tobaccoCounts.getOrDefault(tobaccoKey, 0), solidLimit - 1) + 1;
        int water = Math.min(liquidCounts.getOrDefault(liquidKey, 0), HookahConfig.waterPuffsPerBottle - 1) + 1;
        boolean changed = false;
        boolean emptyCan = false;
        if (smoke >= solidLimit) {
            smoke = 0;
            tobacco.shrink(1);
            items.get(HookahBlockEntity.SLOT_COAL).shrink(1);
            changed = true;
        }
        if (water >= HookahConfig.waterPuffsPerBottle) {
            water = 0;
            emptyCan = liquid.is(ModItems.WHITE_MONSTER.get());
            liquid.shrink(1);
            changed = true;
        }
        if (smoke == 0) tobaccoCounts.remove(tobaccoKey);
        else tobaccoCounts.put(tobaccoKey, smoke);
        if (water == 0) liquidCounts.remove(liquidKey);
        else liquidCounts.put(liquidKey, water);
        return new Consumption(new HookahProgress(smoke, water, tobaccoCounts, liquidCounts), changed, emptyCan);
    }

    public record Consumption(HookahProgress progress, boolean itemsChanged, boolean emptyCan) {}
}

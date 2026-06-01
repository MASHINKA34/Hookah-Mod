package com.hookahmod.item;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

public enum HookahTier implements StringRepresentable {
    LEATHER("leather", "", 1.0, 1.0f, 1.0f, false, "hookah"),
    GOLD("gold", "_gold", 2.0, 1.35f, 1.1f, false, "hookah_gold"),
    IRON("iron", "_iron", 3.0, 1.25f, 1.25f, false, "hookah_iron"),
    DIAMOND("diamond", "_diamond", 5.0, 1.5f, 1.5f, false, "hookah_diamond"),
    NETHERITE("netherite", "_netherite", 7.0, 1.75f, 1.75f, true, "hookah_netherite");

    private final String serializedName;
    private final String suffix;
    private final double armor;
    private final float effectMult;
    private final float combatMult;
    private final boolean fireResistant;
    private final String itemId;

    HookahTier(String serializedName, String suffix, double armor, float effectMult, float combatMult, boolean fireResistant, String itemId) {
        this.serializedName = serializedName;
        this.suffix = suffix;
        this.armor = armor;
        this.effectMult = effectMult;
        this.combatMult = combatMult;
        this.fireResistant = fireResistant;
        this.itemId = itemId;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public String suffix() {
        return suffix;
    }

    public double armor() {
        return armor;
    }

    public float effectMult() {
        return effectMult;
    }

    public float combatMult() {
        return combatMult;
    }

    public boolean fireResistant() {
        return fireResistant;
    }

    public String itemId() {
        return itemId;
    }

    public String localizationKey() {
        return "hookah_tier.hookahmod." + serializedName;
    }

    public static HookahTier fromStack(ItemStack stack) {
        return stack.getItem() instanceof TieredHookahItem item ? item.tier() : LEATHER;
    }
}

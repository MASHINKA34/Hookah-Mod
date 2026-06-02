package com.hookahmod.effect;

import com.hookahmod.HookahMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class SeaLuckEffect extends MobEffect {

    public SeaLuckEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x2A8AB0);
        addAttributeModifier(Attributes.LUCK, HookahMod.id("sea_luck"), 1.0, AttributeModifier.Operation.ADD_VALUE);
    }
}

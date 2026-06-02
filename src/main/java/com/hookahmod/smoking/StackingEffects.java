package com.hookahmod.smoking;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public final class StackingEffects {

    public static final int MAX_DURATION_TICKS = 1_000_000;

    private StackingEffects() {}

    public static void addStacked(ServerPlayer player, Holder<MobEffect> effect, int amplifier,
                                  int baseSeconds, float charge, float effectMult) {
        int addTicks = Math.round(baseSeconds * 20.0f * (0.5f + 0.5f * Mth.clamp(charge, 0.0f, 1.0f)) * effectMult);
        MobEffectInstance current = player.getEffect(effect);
        int base = (current != null && current.getAmplifier() == amplifier) ? current.getDuration() : 0;
        int duration = Math.min(base + addTicks, MAX_DURATION_TICKS);
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, true, true));
    }
}

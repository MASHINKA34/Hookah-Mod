package com.hookahmod.effect;

import com.hookahmod.HookahMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, HookahMod.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> SEA_LUCK =
            MOB_EFFECTS.register("sea_luck", SeaLuckEffect::new);

    private ModMobEffects() {}
}

package com.hookahmod.registry;

import com.hookahmod.HookahMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, HookahMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SMOKING =
            SOUNDS.register("smoking", () -> SoundEvent.createVariableRangeEvent(HookahMod.id("smoking")));

    private ModSounds() {}
}

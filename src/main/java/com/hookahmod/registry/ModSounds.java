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

    public static final DeferredHolder<SoundEvent, SoundEvent> HASHISH_TRIP_MUSIC =
            SOUNDS.register("hashish_trip_music", () -> SoundEvent.createVariableRangeEvent(HookahMod.id("hashish_trip_music")));

    public static final DeferredHolder<SoundEvent, SoundEvent> PALPALYCH_TRIP =
            SOUNDS.register("palpalych_trip", () -> SoundEvent.createVariableRangeEvent(HookahMod.id("palpalych_trip")));

    public static final DeferredHolder<SoundEvent, SoundEvent> WHITE_MONSTER_BURP =
            SOUNDS.register("white_monster_burp", () -> SoundEvent.createVariableRangeEvent(HookahMod.id("white_monster_burp")));

    private ModSounds() {}
}

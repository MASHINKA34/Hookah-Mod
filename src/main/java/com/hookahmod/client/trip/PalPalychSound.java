package com.hookahmod.client.trip;

import com.hookahmod.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public final class PalPalychSound extends AbstractTickableSoundInstance {

    private final LocalPlayer player;

    public PalPalychSound(LocalPlayer player) {
        super(ModSounds.PALPALYCH_TRIP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.75F;
        this.pitch = 1.0F;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        if (player.isRemoved() || !VideoTripManager.isActive()) {
            stop();
        }
    }

    public void halt() {
        stop();
    }
}

final class PalPalychSoundController {

    private static PalPalychSound current;

    private PalPalychSoundController() {}

    static void tick(Minecraft mc, LocalPlayer player) {
        if (VideoTripManager.isActive()) {
            if (current == null || current.isStopped()) {
                current = new PalPalychSound(player);
                mc.getSoundManager().play(current);
            }
        } else {
            stop(mc);
        }
    }

    static void stop(Minecraft mc) {
        if (current == null) return;
        current.halt();
        mc.getSoundManager().stop(current);
        current = null;
    }
}

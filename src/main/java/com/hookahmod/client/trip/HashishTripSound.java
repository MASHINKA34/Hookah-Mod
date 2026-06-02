package com.hookahmod.client.trip;

import com.hookahmod.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public final class HashishTripSound extends AbstractTickableSoundInstance {

    private final LocalPlayer player;

    public HashishTripSound(LocalPlayer player) {
        super(ModSounds.HASHISH_TRIP_MUSIC.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.75F;
        this.pitch = 1.0F;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        if (player.isRemoved() || !HashishTripManager.isActive()) {
            stop();
        }
    }

    public void halt() {
        stop();
    }
}

final class HashishTripSoundController {

    private static HashishTripSound current;

    private HashishTripSoundController() {}

    static void tick(Minecraft mc, LocalPlayer player) {
        if (HashishTripManager.isActive()) {
            if (current == null || current.isStopped()) {
                current = new HashishTripSound(player);
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

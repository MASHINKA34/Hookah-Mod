package com.hookahmod.smoking;

import com.hookahmod.network.IntoxicationSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public final class IntoxicationState {

    public static final float MAX_VALUE = 220.0f;
    public static final float REGULAR_TOBACCO_INTOXICATION = 16.0f;

    private IntoxicationState() {}

    public static float get(Player player) {
        return player.getData(ModAttachments.INTOXICATION.get());
    }

    public static void add(ServerPlayer player, float amount) {
        setAndSync(player, get(player) + amount);
    }

    public static void setAndSync(ServerPlayer player, float value) {
        float clamped = Mth.clamp(value, 0.0f, MAX_VALUE);
        if (Math.abs(get(player) - clamped) < 0.001f) return;
        player.setData(ModAttachments.INTOXICATION.get(), clamped);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new IntoxicationSyncPayload(get(player)));
    }

    public static void decayTick(ServerPlayer player) {
        float value = get(player);
        if (value > 0.0f) setAndSync(player, value - 1.0f);
    }

    public static void applyBandEffects(ServerPlayer player) {
        float value = get(player);
        IntoxicationBand band = band(value);
        boolean locked = updateTripLock(player, band, value);
        if (band == IntoxicationBand.RELAXED) {
            if (!locked) player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
        } else if (band == IntoxicationBand.HIGH) {
            if (!locked) player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, true, false, true));
        } else if (band == IntoxicationBand.TRIP) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false, true));
        } else if (band == IntoxicationBand.OVERDOSE) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 1, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 0, true, false, true));
            if (player.getRandom().nextFloat() < 0.12f) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 35, 0, true, false, true));
            }
        }
    }

    private static boolean updateTripLock(ServerPlayer player, IntoxicationBand band, float value) {
        boolean locked = player.getData(ModAttachments.TRIP_LOCK.get());
        if (band.atLeast(IntoxicationBand.TRIP)) {
            if (!locked) player.setData(ModAttachments.TRIP_LOCK.get(), true);
            return true;
        }
        if (locked && value <= 0.0f) {
            player.setData(ModAttachments.TRIP_LOCK.get(), false);
            return false;
        }
        return locked;
    }

    public static IntoxicationBand band(float value) {
        if (value >= 150.0f) return IntoxicationBand.OVERDOSE;
        if (value >= 100.0f) return IntoxicationBand.TRIP;
        if (value >= 60.0f) return IntoxicationBand.HIGH;
        if (value >= 30.0f) return IntoxicationBand.RELAXED;
        return IntoxicationBand.SOBER;
    }

    public static float gain(float tobaccoIntoxication, float charge) {
        return tobaccoIntoxication * (0.4f + 0.6f * Mth.clamp(charge, 0.0f, 1.0f));
    }
}

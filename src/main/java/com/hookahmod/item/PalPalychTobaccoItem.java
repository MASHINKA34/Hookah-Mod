package com.hookahmod.item;

import com.hookahmod.effect.ModMobEffects;
import com.hookahmod.network.VideoTripPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

public class PalPalychTobaccoItem extends AbstractTobaccoItem {

    private static final float TRIP_CHANCE = 0.30f;
    private static final int DURATION_TICKS = 398;
    private static final Vector3f SMOKE_COLOR = new Vector3f(0.85f, 0.55f, 1.0f);

    public PalPalychTobaccoItem(Properties properties) {
        super(properties, TobaccoCategory.REGULAR, 20);
    }

    @Override
    public void onExhale(ServerLevel level, ServerPlayer smoker, float charge, float effectMult, float combatMult) {
        if (smoker.getEffect(ModMobEffects.PALPALYCH_TRIP) != null) return;
        if (smoker.getRandom().nextFloat() >= TRIP_CHANCE) return;

        smoker.addEffect(new MobEffectInstance(ModMobEffects.PALPALYCH_TRIP, DURATION_TICKS, 0, false, true, true), smoker);
        PacketDistributor.sendToPlayer(smoker, new VideoTripPayload(DURATION_TICKS));
    }

    @Override
    public Vector3f smokeColor() {
        return SMOKE_COLOR;
    }
}

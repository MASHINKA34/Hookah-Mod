package com.hookahmod.item;

import com.hookahmod.effect.ModMobEffects;
import com.hookahmod.smoking.IntoxicationBand;
import com.hookahmod.smoking.IntoxicationState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.joml.Vector3f;

public class AbyssTobaccoItem extends AbstractTobaccoItem {

    private static final float TRIP_CHANCE = 0.30f;
    private static final int TRIP_DURATION_TICKS = 36 * 20;

    public AbyssTobaccoItem(Properties properties) {
        super(properties, TobaccoCategory.RARE, 28);
    }

    @Override
    public void onExhale(ServerLevel level, ServerPlayer smoker, float charge, float effectMult, float combatMult) {
        if (smoker.hasEffect(ModMobEffects.ABYSS_TRIP)) return;
        if (!IntoxicationState.band(IntoxicationState.get(smoker)).atLeast(IntoxicationBand.TRIP)) return;
        if (smoker.getRandom().nextFloat() >= TRIP_CHANCE) return;

        smoker.addEffect(new MobEffectInstance(ModMobEffects.ABYSS_TRIP, TRIP_DURATION_TICKS, 0, false, true, true), smoker);
        IntoxicationState.beginAbyssTrip(smoker);
        IntoxicationState.spawnTripVision(smoker);
    }

    @Override
    public Vector3f smokeColor() {
        return new Vector3f(0.42f, 0.16f, 0.55f);
    }
}

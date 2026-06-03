package com.hookahmod.item;

import com.hookahmod.effect.ModMobEffects;
import com.hookahmod.network.HashishTripPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

public class HashishTobaccoItem extends AbstractTobaccoItem {

    private static final float TRIP_CHANCE = 0.30f;
    private static final int TRIP_DURATION_TICKS = 30 * 20;
    private static final Vector3f SMOKE_COLOR = new Vector3f(0.65f, 0.95f, 0.55f);

    public HashishTobaccoItem(Properties properties) {
        super(properties, TobaccoCategory.REGULAR, 20);
    }

    @Override
    public void onExhale(ServerLevel level, ServerPlayer smoker, float charge, float effectMult, float combatMult) {
        if (smoker.getEffect(ModMobEffects.HASHISH_TRIP) != null) return;
        if (smoker.getRandom().nextFloat() >= TRIP_CHANCE) return;

        smoker.addEffect(new MobEffectInstance(ModMobEffects.HASHISH_TRIP, TRIP_DURATION_TICKS, 0, false, true, true), smoker);

        float intensity = Mth.clamp(effectMult, 0.5f, 1.75f);
        PacketDistributor.sendToPlayer(smoker, new HashishTripPayload(TRIP_DURATION_TICKS, intensity));
    }

    @Override
    public Vector3f smokeColor() {
        return SMOKE_COLOR;
    }
}

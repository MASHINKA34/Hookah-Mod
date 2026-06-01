package com.hookahmod.item;

import com.hookahmod.network.TripEventPayload;
import com.hookahmod.smoking.IntoxicationBand;
import com.hookahmod.smoking.IntoxicationState;
import com.hookahmod.trip.TripVisionType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

public class AbyssTobaccoItem extends AbstractTobaccoItem {

    public AbyssTobaccoItem(Properties properties) {
        super(properties, TobaccoCategory.RARE, 28);
    }

    @Override
    public void onExhale(ServerLevel level, ServerPlayer smoker, float charge, float effectMult, float combatMult) {
        if (!IntoxicationState.band(IntoxicationState.get(smoker)).atLeast(IntoxicationBand.TRIP)) return;
        TripVisionType type = TripVisionType.random(smoker.getRandom());
        PacketDistributor.sendToPlayer(smoker, new TripEventPayload(type, smoker.getRandom().nextLong()));
    }

    @Override
    public Vector3f smokeColor() {
        return new Vector3f(0.42f, 0.16f, 0.55f);
    }
}

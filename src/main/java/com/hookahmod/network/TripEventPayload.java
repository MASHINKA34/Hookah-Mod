package com.hookahmod.network;

import com.hookahmod.HookahMod;
import com.hookahmod.client.trip.TripManager;
import com.hookahmod.trip.TripVisionType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TripEventPayload(TripVisionType visionType, long seed) implements CustomPacketPayload {

    public static final Type<TripEventPayload> TYPE = new Type<>(HookahMod.id("trip_event"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TripEventPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(TripVisionType::byId, TripVisionType::ordinal),
            TripEventPayload::visionType,
            ByteBufCodecs.VAR_LONG,
            TripEventPayload::seed,
            TripEventPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TripEventPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> TripManager.trigger(payload.visionType, payload.seed));
    }
}

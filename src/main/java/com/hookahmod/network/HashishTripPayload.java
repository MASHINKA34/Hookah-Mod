package com.hookahmod.network;

import com.hookahmod.ClientBridge;
import com.hookahmod.HookahMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HashishTripPayload(int durationTicks, float intensity) implements CustomPacketPayload {

    public static final Type<HashishTripPayload> TYPE = new Type<>(HookahMod.id("hashish_trip"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HashishTripPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            HashishTripPayload::durationTicks,
            ByteBufCodecs.FLOAT,
            HashishTripPayload::intensity,
            HashishTripPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HashishTripPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientBridge.startHashishTrip(payload.durationTicks, payload.intensity));
    }
}

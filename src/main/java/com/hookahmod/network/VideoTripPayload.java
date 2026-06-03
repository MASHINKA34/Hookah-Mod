package com.hookahmod.network;

import com.hookahmod.HookahMod;
import com.hookahmod.client.trip.VideoTripManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record VideoTripPayload(int durationTicks) implements CustomPacketPayload {

    public static final Type<VideoTripPayload> TYPE = new Type<>(HookahMod.id("palpalych_trip"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VideoTripPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VideoTripPayload::durationTicks, VideoTripPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VideoTripPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(VideoTripManager::start);
    }
}

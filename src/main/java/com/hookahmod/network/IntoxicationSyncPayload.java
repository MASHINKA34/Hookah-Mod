package com.hookahmod.network;

import com.hookahmod.ClientBridge;
import com.hookahmod.HookahMod;
import com.hookahmod.smoking.ModAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record IntoxicationSyncPayload(float value) implements CustomPacketPayload {

    public static final Type<IntoxicationSyncPayload> TYPE = new Type<>(HookahMod.id("intoxication_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, IntoxicationSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            IntoxicationSyncPayload::value,
            IntoxicationSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(IntoxicationSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ctx.player().setData(ModAttachments.INTOXICATION.get(), payload.value);
            ClientBridge.setIntoxication(payload.value);
        });
    }
}

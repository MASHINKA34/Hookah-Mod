package com.hookahmod.network;

import com.hookahmod.HookahMod;
import com.hookahmod.event.ActiveSessions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;
import java.util.UUID;

public record WornHookahSyncPayload(Optional<UUID> wearer) implements CustomPacketPayload {

    public static final Type<WornHookahSyncPayload> TYPE = new Type<>(HookahMod.id("worn_hookah_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WornHookahSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.optional(net.minecraft.core.UUIDUtil.STREAM_CODEC), WornHookahSyncPayload::wearer,
                    WornHookahSyncPayload::new
            );

    public static WornHookahSyncPayload claim(UUID wearer) {
        return new WornHookahSyncPayload(Optional.of(wearer));
    }

    public static WornHookahSyncPayload release() {
        return new WornHookahSyncPayload(Optional.empty());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(WornHookahSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            UUID player = ctx.player().getUUID();
            if (payload.wearer.isPresent()) {
                ActiveSessions.registerWorn(player, payload.wearer.get());
            } else {
                ActiveSessions.unregister(player);
            }
        });
    }
}

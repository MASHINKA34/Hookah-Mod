package com.hookahmod.network;

import com.hookahmod.HookahMod;
import com.hookahmod.event.ActiveSessions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public record HookahSyncPayload(BlockPos pos, Optional<UUID> activePlayer) implements CustomPacketPayload {

    public static final Type<HookahSyncPayload> TYPE = new Type<>(HookahMod.id("sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HookahSyncPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, HookahSyncPayload::pos,
            ByteBufCodecs.optional(net.minecraft.core.UUIDUtil.STREAM_CODEC), HookahSyncPayload::activePlayer,
            HookahSyncPayload::new
    );

    public HookahSyncPayload(BlockPos pos, @Nullable UUID active) {
        this(pos, Optional.ofNullable(active));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    // Block state, hose and inventory already reach the client through the
    // vanilla block-entity update packet (getUpdateTag / handleUpdateTag). This
    // payload's only remaining job is to mirror the active-player session into
    // the client-side ActiveSessions so the mouthpiece's client-side use() gate
    // works for the local player.
    public static void handle(HookahSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Level level = ctx.player().level();
            ActiveSessions sessions = ActiveSessions.of(level);
            UUID localUuid = ctx.player().getUUID();
            UUID activeUuid = payload.activePlayer.orElse(null);
            if (localUuid.equals(activeUuid)) {
                sessions.register(localUuid, level.dimension(), payload.pos);
            } else {
                var gp = sessions.get(localUuid);
                if (gp != null && gp.pos().equals(payload.pos)) {
                    sessions.unregister(localUuid);
                }
            }
        });
    }
}

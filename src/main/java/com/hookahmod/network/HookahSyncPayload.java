package com.hookahmod.network;

import com.hookahmod.HookahMod;
import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.item.HookahHoseType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;
import java.util.UUID;

public record HookahSyncPayload(BlockPos pos, HookahHoseType hoseType, Optional<UUID> activePlayer) implements CustomPacketPayload {

    public static final Type<HookahSyncPayload> TYPE = new Type<>(HookahMod.id("sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HookahSyncPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, HookahSyncPayload::pos,
            HookahHoseType.STREAM_CODEC, HookahSyncPayload::hoseType,
            ByteBufCodecs.optional(net.minecraft.core.UUIDUtil.STREAM_CODEC), HookahSyncPayload::activePlayer,
            HookahSyncPayload::new
    );

    public HookahSyncPayload(BlockPos pos, HookahHoseType type, UUID active) {
        this(pos, type, Optional.ofNullable(active));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(HookahSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Level level = ctx.player().level();
            if (!level.isLoaded(payload.pos)) return;
            BlockEntity be = level.getBlockEntity(payload.pos);
            if (be instanceof HookahBlockEntity hbe) {
                hbe.applySync(payload.hoseType, payload.activePlayer.orElse(null));
            }
        });
    }
}

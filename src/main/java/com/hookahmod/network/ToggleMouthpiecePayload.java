package com.hookahmod.network;

import com.hookahmod.HookahMod;
import com.hookahmod.block.HookahBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleMouthpiecePayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<ToggleMouthpiecePayload> TYPE = new Type<>(HookahMod.id("toggle_mouthpiece"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleMouthpiecePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ToggleMouthpiecePayload::pos,
            ToggleMouthpiecePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ToggleMouthpiecePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!player.level().isLoaded(payload.pos)) return;
            if (player.distanceToSqr(Vec3.atCenterOf(payload.pos)) > 64.0) return;
            if (player.level().getBlockEntity(payload.pos) instanceof HookahBlockEntity be) {
                be.tryTakeMouthpiece(player);
            }
        });
    }
}

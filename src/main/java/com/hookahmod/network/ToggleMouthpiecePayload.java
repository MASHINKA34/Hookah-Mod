package com.hookahmod.network;

import com.hookahmod.HookahMod;
import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.item.WornHookah;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;
import java.util.UUID;

public record ToggleMouthpiecePayload(BlockPos pos, Optional<UUID> wearer) implements CustomPacketPayload {

    public static final Type<ToggleMouthpiecePayload> TYPE = new Type<>(HookahMod.id("toggle_mouthpiece"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleMouthpiecePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ToggleMouthpiecePayload::pos,
            ByteBufCodecs.optional(net.minecraft.core.UUIDUtil.STREAM_CODEC), ToggleMouthpiecePayload::wearer,
            ToggleMouthpiecePayload::new
    );

    public ToggleMouthpiecePayload(BlockPos pos) {
        this(pos, Optional.empty());
    }

    public static ToggleMouthpiecePayload worn(UUID wearer) {
        return new ToggleMouthpiecePayload(BlockPos.ZERO, Optional.of(wearer));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ToggleMouthpiecePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (payload.wearer.isPresent()) {
                ServerPlayer wearer = player.server.getPlayerList().getPlayer(payload.wearer.get());
                if (wearer == null || player.distanceToSqr(wearer) > 64.0) return;
                ItemStack stack = wearer.getItemBySlot(EquipmentSlot.CHEST);
                if (WornHookah.isHookahStack(stack)) {
                    WornHookah.tryTakeMouthpiece(player, wearer, stack);
                }
                return;
            }
            if (!player.level().isLoaded(payload.pos)) return;
            if (player.distanceToSqr(Vec3.atCenterOf(payload.pos)) > 64.0) return;
            if (player.level().getBlockEntity(payload.pos) instanceof HookahBlockEntity be) {
                be.tryTakeMouthpiece(player);
            }
        });
    }
}

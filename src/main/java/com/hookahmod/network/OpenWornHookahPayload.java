package com.hookahmod.network;

import com.hookahmod.HookahMod;
import com.hookahmod.item.WornHookah;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenWornHookahPayload() implements CustomPacketPayload {

    public static final Type<OpenWornHookahPayload> TYPE = new Type<>(HookahMod.id("open_worn_hookah"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenWornHookahPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenWornHookahPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(OpenWornHookahPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!WornHookah.isHookahStack(stack)) {
                player.displayClientMessage(Component.translatable("message.hookahmod.no_worn_hookah"), true);
                return;
            }
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new com.hookahmod.menu.HookahMenu(id, inv, player.getUUID()),
                    Component.translatable("container.hookahmod.hookah")
            ), buf -> buf.writeBoolean(true));
        });
    }
}

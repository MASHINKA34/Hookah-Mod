package com.hookahmod.item;

import com.hookahmod.smoking.IntoxicationBand;
import com.hookahmod.smoking.IntoxicationState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TonometerItem extends Item {

    public TonometerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            float value = IntoxicationState.get(serverPlayer);
            IntoxicationBand band = IntoxicationState.band(value);
            serverPlayer.displayClientMessage(
                    Component.translatable(
                            "message.hookahmod.tonometer",
                            Math.round(value),
                            Component.translatable(band.localizationKey())
                    ),
                    true
            );
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

package com.hookahmod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GuidebookItem extends Item {

    public GuidebookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new com.hookahmod.client.GuideScreen());
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}

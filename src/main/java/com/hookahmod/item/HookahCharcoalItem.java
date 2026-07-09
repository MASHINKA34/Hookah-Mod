package com.hookahmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class HookahCharcoalItem extends Item {
    public HookahCharcoalItem(Properties props) { super(props); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hookahmod.consumption.solid").withStyle(ChatFormatting.GRAY));
    }
}

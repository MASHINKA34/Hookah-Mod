package com.hookahmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class HookahWaterBottleItem extends Item {
    public HookahWaterBottleItem(Properties props) { super(props); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hookahmod.consumption.water").withStyle(ChatFormatting.GRAY));
    }
}

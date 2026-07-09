package com.hookahmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

public abstract class AbstractTobaccoItem extends Item {

    private final TobaccoCategory category;
    private final int intoxication;

    public AbstractTobaccoItem(Properties properties, TobaccoCategory category, int intoxication) {
        super(properties);
        this.category = category;
        this.intoxication = intoxication;
    }

    public TobaccoCategory category() {
        return category;
    }

    public int intoxication() {
        return intoxication;
    }

    public void onExhale(ServerLevel level, ServerPlayer smoker, float charge, float effectMult, float combatMult) {}

    @Nullable
    public Vector3f smokeColor() {
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = category == TobaccoCategory.COMBAT
                ? "tooltip.hookahmod.consumption.combat"
                : "tooltip.hookahmod.consumption.solid";
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }
}

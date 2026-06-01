package com.hookahmod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class HookahBlockItem extends BlockItem implements Equipable, TieredHookahItem {

    private final HookahTier tier;

    public HookahBlockItem(Block block, Properties properties) {
        this(block, properties, HookahTier.LEATHER);
    }

    public HookahBlockItem(Block block, Properties properties, HookahTier tier) {
        super(block, properties);
        this.tier = tier;
    }

    @Override
    public HookahTier tier() {
        return tier;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }
}

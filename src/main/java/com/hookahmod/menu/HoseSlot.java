package com.hookahmod.menu;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.item.HookahHoseItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class HoseSlot extends Slot {

    private final HookahBlockEntity be;

    public HoseSlot(Container container, int slot, int x, int y, HookahBlockEntity be) {
        super(container, slot, x, y);
        this.be = be;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getItem() instanceof HookahHoseItem && !be.isInUse();
    }

    @Override
    public boolean mayPickup(Player player) {
        return !be.isInUse();
    }

    @Override
    public int getMaxStackSize() { return 1; }

    @Override
    public int getMaxStackSize(ItemStack stack) { return 1; }
}

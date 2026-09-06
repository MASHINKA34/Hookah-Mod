package com.hookahmod.menu;

import com.hookahmod.item.HookahHoseItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;

public class HoseSlot extends Slot {

    private final BooleanSupplier inUse;

    public HoseSlot(Container container, int slot, int x, int y, BooleanSupplier inUse) {
        super(container, slot, x, y);
        this.inUse = inUse;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getItem() instanceof HookahHoseItem && !inUse.getAsBoolean();
    }

    @Override
    public boolean mayPickup(Player player) {
        return !inUse.getAsBoolean();
    }

    @Override
    public int getMaxStackSize() { return 1; }

    @Override
    public int getMaxStackSize(ItemStack stack) { return 1; }
}

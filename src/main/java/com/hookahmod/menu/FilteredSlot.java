package com.hookahmod.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class FilteredSlot extends Slot {

    private final Item allowed;

    public FilteredSlot(Container container, int slot, int x, int y, Item allowed) {
        super(container, slot, x, y);
        this.allowed = allowed;
    }

    public Item allowedItem() { return allowed; }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(allowed);
    }
}

package com.hookahmod.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class FilteredSlot extends Slot {

    private final Item allowed;
    private final Predicate<ItemStack> filter;

    public FilteredSlot(Container container, int slot, int x, int y, Item allowed) {
        this(container, slot, x, y, allowed, stack -> stack.is(allowed));
    }

    public FilteredSlot(Container container, int slot, int x, int y, Item allowed, Predicate<ItemStack> filter) {
        super(container, slot, x, y);
        this.allowed = allowed;
        this.filter = filter;
    }

    public Item allowedItem() { return allowed; }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return filter.test(stack);
    }
}

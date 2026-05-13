package com.hookahmod.item;

import com.hookahmod.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public abstract class HookahHoseItem extends Item {
    public HookahHoseItem(Properties props) { super(props); }

    public abstract HookahHoseType getHoseType();

    public static ItemStack stackFor(HookahHoseType type) {
        return switch (type) {
            case SHORT -> new ItemStack(ModItems.SHORT_HOOKAH_HOSE.get());
            case LONG -> new ItemStack(ModItems.LONG_HOOKAH_HOSE.get());
            default -> ItemStack.EMPTY;
        };
    }
}

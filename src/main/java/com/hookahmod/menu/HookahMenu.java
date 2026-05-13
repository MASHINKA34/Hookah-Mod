package com.hookahmod.menu;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.registry.ModBlocks;
import com.hookahmod.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class HookahMenu extends AbstractContainerMenu {

    public static final int SLOT_TOBACCO = HookahBlockEntity.SLOT_TOBACCO;
    public static final int SLOT_COAL = HookahBlockEntity.SLOT_COAL;
    public static final int SLOT_WATER = HookahBlockEntity.SLOT_WATER;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final Container container;

    public HookahMenu(int id, Inventory playerInv, BlockPos pos) {
        super(ModMenuTypes.HOOKAH.get(), id);
        this.pos = pos;
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        this.container = (be instanceof HookahBlockEntity hbe) ? hbe.getInventory() : new SimpleContainer(HookahBlockEntity.SLOT_COUNT);
        this.access = ContainerLevelAccess.create(playerInv.player.level(), pos);

        // Hookah slots
        this.addSlot(new Slot(container, SLOT_TOBACCO, 44, 35));
        this.addSlot(new Slot(container, SLOT_COAL, 80, 35));
        this.addSlot(new Slot(container, SLOT_WATER, 116, 35));

        // Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    public BlockPos getPos() { return pos; }

    @Nullable
    public HookahBlockEntity getBlockEntity(Player player) {
        BlockEntity be = player.level().getBlockEntity(pos);
        return be instanceof HookahBlockEntity hbe ? hbe : null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int containerSize = HookahBlockEntity.SLOT_COUNT;
        if (slotIndex < containerSize) {
            if (!this.moveItemStackTo(stack, containerSize, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!this.moveItemStackTo(stack, 0, containerSize, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.HOOKAH.get());
    }
}

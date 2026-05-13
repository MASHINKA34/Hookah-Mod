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

    public static final int HOSE_SLOT_X = 78;
    public static final int HOSE_SLOT_Y = 52;

    public static final int CONSUME_ROW_Y = 82;
    public static final int TOBACCO_X = 47;
    public static final int COAL_X = 78;
    public static final int WATER_X = 109;

    public static final int INV_ROW_Y = 192;
    public static final int INV_HOTBAR_Y = 246;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final Container container;
    private final HookahBlockEntity blockEntity;

    public HookahMenu(int id, Inventory playerInv, BlockPos pos) {
        super(ModMenuTypes.HOOKAH.get(), id);
        this.pos = pos;
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        this.blockEntity = (be instanceof HookahBlockEntity hbe) ? hbe : null;
        this.container = (blockEntity != null) ? blockEntity.getInventory() : new SimpleContainer(HookahBlockEntity.SLOT_COUNT);
        this.access = ContainerLevelAccess.create(playerInv.player.level(), pos);

        if (blockEntity != null) {
            this.addSlot(new HoseSlot(container, HookahBlockEntity.SLOT_HOSE, HOSE_SLOT_X, HOSE_SLOT_Y, blockEntity));
        } else {
            this.addSlot(new Slot(container, HookahBlockEntity.SLOT_HOSE, HOSE_SLOT_X, HOSE_SLOT_Y));
        }
        this.addSlot(new Slot(container, HookahBlockEntity.SLOT_TOBACCO, TOBACCO_X, CONSUME_ROW_Y));
        this.addSlot(new Slot(container, HookahBlockEntity.SLOT_COAL, COAL_X, CONSUME_ROW_Y));
        this.addSlot(new Slot(container, HookahBlockEntity.SLOT_WATER, WATER_X, CONSUME_ROW_Y));

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, INV_ROW_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, INV_HOTBAR_Y));
        }
    }

    public BlockPos getPos() { return pos; }

    @Nullable
    public HookahBlockEntity getBlockEntity() { return blockEntity; }

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
            boolean placed = false;
            if (stack.getItem() instanceof com.hookahmod.item.HookahHoseItem) {
                placed = this.moveItemStackTo(stack, HookahBlockEntity.SLOT_HOSE, HookahBlockEntity.SLOT_HOSE + 1, false);
            }
            if (!placed && !this.moveItemStackTo(stack, HookahBlockEntity.SLOT_TOBACCO, containerSize, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.HOOKAH.get());
    }
}

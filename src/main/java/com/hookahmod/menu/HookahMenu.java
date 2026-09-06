package com.hookahmod.menu;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.item.AbstractTobaccoItem;
import com.hookahmod.item.HookahHoseItem;
import com.hookahmod.item.HookahHoseType;
import com.hookahmod.item.WornHookah;
import com.hookahmod.registry.ModItems;
import com.hookahmod.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class HookahMenu extends AbstractContainerMenu {

    public static final int HOSE_SLOT_X = 76;
    public static final int HOSE_SLOT_Y = 42;

    public static final int CONSUME_ROW_Y = 85;
    public static final int TOBACCO_X = 35;
    public static final int COAL_X = 76;
    public static final int WATER_X = 117;

    public static final int INV_ROW_Y = 162;
    public static final int INV_HOTBAR_Y = 218;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final Container container;
    private final HookahBlockEntity blockEntity;
    private final boolean wearable;
    private final java.util.UUID wearerUuid;
    private final Player menuPlayer;

    public HookahMenu(int id, Inventory playerInv, BlockPos pos) {
        super(ModMenuTypes.HOOKAH.get(), id);
        this.pos = pos;
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        this.blockEntity = (be instanceof HookahBlockEntity hbe) ? hbe : null;
        this.container = (blockEntity != null) ? blockEntity.getInventory() : new SimpleContainer(HookahBlockEntity.SLOT_COUNT);
        this.access = ContainerLevelAccess.create(playerInv.player.level(), pos);
        this.wearable = false;
        this.wearerUuid = null;
        this.menuPlayer = playerInv.player;

        addHookahSlots();
        addPlayerSlots(playerInv);
    }

    public HookahMenu(int id, Inventory playerInv, java.util.UUID wearerUuid) {
        super(ModMenuTypes.HOOKAH.get(), id);
        this.pos = BlockPos.ZERO;
        this.blockEntity = null;
        this.wearable = true;
        this.wearerUuid = wearerUuid;
        this.menuPlayer = playerInv.player;
        Player wearer = playerInv.player.getUUID().equals(wearerUuid)
                ? playerInv.player
                : playerInv.player.level().getPlayerByUUID(wearerUuid);
        ItemStack stack = wearer == null ? ItemStack.EMPTY : wearer.getItemBySlot(EquipmentSlot.CHEST);
        this.container = WornHookah.isHookahStack(stack)
                ? WornHookah.containerFor(stack, wearer)
                : new SimpleContainer(HookahBlockEntity.SLOT_COUNT);
        this.access = ContainerLevelAccess.NULL;

        addHookahSlots();
        addPlayerSlots(playerInv);
    }

    private void addHookahSlots() {
        this.addSlot(new HoseSlot(container, HookahBlockEntity.SLOT_HOSE, HOSE_SLOT_X, HOSE_SLOT_Y, this::isInUse));
        this.addSlot(new FilteredSlot(container, HookahBlockEntity.SLOT_TOBACCO, TOBACCO_X, CONSUME_ROW_Y, ModItems.HOOKAH_TOBACCO.get(), stack -> stack.getItem() instanceof AbstractTobaccoItem));
        this.addSlot(new FilteredSlot(container, HookahBlockEntity.SLOT_COAL, COAL_X, CONSUME_ROW_Y, ModItems.HOOKAH_CHARCOAL.get()));
        this.addSlot(new FilteredSlot(
                container,
                HookahBlockEntity.SLOT_WATER,
                WATER_X,
                CONSUME_ROW_Y,
                ModItems.HOOKAH_WATER_BOTTLE.get(),
                stack -> stack.is(ModItems.HOOKAH_WATER_BOTTLE.get()) || stack.is(ModItems.WHITE_MONSTER.get())
        ));
    }

    private void addPlayerSlots(Inventory playerInv) {
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

    public boolean isWearable() { return wearable; }

    public java.util.UUID getWearerUuid() { return wearerUuid; }

    @Nullable
    public HookahBlockEntity getBlockEntity() { return blockEntity; }

    public HookahHoseType getHoseType() {
        if (blockEntity != null) return blockEntity.getHoseType();
        ItemStack hoseStack = container.getItem(HookahBlockEntity.SLOT_HOSE);
        if (hoseStack.getItem() instanceof HookahHoseItem hose) return hose.getHoseType();
        return HookahHoseType.NONE;
    }

    public boolean isInUse() {
        if (blockEntity != null) return blockEntity.isInUse();
        return getActivePlayerUuid() != null;
    }

    public java.util.UUID getActivePlayerUuid() {
        if (blockEntity != null) return blockEntity.getActivePlayerUuid();
        ItemStack stack = getWearerStack();
        return WornHookah.isHookahStack(stack) ? WornHookah.getActivePlayerUuid(stack) : null;
    }

    public boolean hasAllConsumables() {
        if (blockEntity != null) return blockEntity.hasAllConsumables();
        return !container.getItem(HookahBlockEntity.SLOT_TOBACCO).isEmpty()
                && !container.getItem(HookahBlockEntity.SLOT_COAL).isEmpty()
                && !container.getItem(HookahBlockEntity.SLOT_WATER).isEmpty();
    }

    private ItemStack getWearerStack() {
        if (!wearable || wearerUuid == null) return ItemStack.EMPTY;
        Player wearer = menuPlayer.getUUID().equals(wearerUuid)
                ? menuPlayer
                : menuPlayer.level().getPlayerByUUID(wearerUuid);
        return wearer == null ? ItemStack.EMPTY : wearer.getItemBySlot(EquipmentSlot.CHEST);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int containerSize = HookahBlockEntity.SLOT_COUNT;
        if (slotIndex < containerSize) {
            if (!this.moveItemStackTo(stack, containerSize, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (stack.getItem() instanceof com.hookahmod.item.HookahHoseItem) {
                if (this.moveItemStackTo(stack, HookahBlockEntity.SLOT_HOSE, HookahBlockEntity.SLOT_HOSE + 1, false)) {
                    if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
                    return copy;
                }
            }
            int target = -1;
            if (stack.getItem() instanceof AbstractTobaccoItem) target = HookahBlockEntity.SLOT_TOBACCO;
            else if (stack.is(ModItems.HOOKAH_CHARCOAL.get())) target = HookahBlockEntity.SLOT_COAL;
            else if (stack.is(ModItems.HOOKAH_WATER_BOTTLE.get()) || stack.is(ModItems.WHITE_MONSTER.get())) target = HookahBlockEntity.SLOT_WATER;
            if (target == -1 || !this.moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (wearable) {
            return player.getUUID().equals(wearerUuid)
                    && WornHookah.isHookahStack(player.getItemBySlot(EquipmentSlot.CHEST))
                    && (player.level().isClientSide || container.stillValid(player));
        }
        return blockEntity != null && stillValid(access, player, blockEntity.getBlockState().getBlock());
    }
}

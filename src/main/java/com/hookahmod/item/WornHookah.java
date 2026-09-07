package com.hookahmod.item;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.event.ActiveSessions;
import com.hookahmod.network.WornHookahSyncPayload;
import com.hookahmod.registry.ModItems;
import com.hookahmod.smoking.HookahHost;
import com.hookahmod.smoking.HookahProgress;
import com.hookahmod.smoking.HookahSessions;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class WornHookah {

    private static final String ACTIVE_PLAYER_TAG = "HookahActivePlayer";
    private WornHookah() {}

    public static boolean isHookahStack(ItemStack stack) {
        return stack.getItem() instanceof TieredHookahItem;
    }

    public static NonNullList<ItemStack> getItems(ItemStack stack) {
        NonNullList<ItemStack> items = NonNullList.withSize(HookahBlockEntity.SLOT_COUNT, ItemStack.EMPTY);
        stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    public static void setItems(ItemStack stack, List<ItemStack> items) {
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    public static Container containerFor(ItemStack stack, @Nullable Player wearer) {
        return new StackContainer(stack, wearer);
    }

    public static ItemStack itemAt(ItemStack stack, int slot) {
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        return slot < contents.getSlots() ? contents.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    public static HookahHoseType getHoseType(ItemStack stack) {
        ItemStack hoseStack = itemAt(stack, HookahBlockEntity.SLOT_HOSE);
        if (hoseStack.getItem() instanceof HookahHoseItem hose) return hose.getHoseType();
        return HookahHoseType.NONE;
    }

    public static boolean hasAllConsumables(ItemStack stack) {
        return HookahHost.hasAllConsumables(slot -> itemAt(stack, slot));
    }

    public static boolean hasCoal(ItemStack stack) {
        return !itemAt(stack, HookahBlockEntity.SLOT_COAL).isEmpty();
    }

    @Nullable
    public static UUID getActivePlayerUuid(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!data.contains(ACTIVE_PLAYER_TAG)) return null;
        CompoundTag tag = data.copyTag();
        return tag.hasUUID(ACTIVE_PLAYER_TAG) ? tag.getUUID(ACTIVE_PLAYER_TAG) : null;
    }

    public static void setActivePlayerUuid(ItemStack stack, @Nullable UUID uuid) {
        if (uuid == null && getActivePlayerUuid(stack) == null) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (uuid == null) tag.remove(ACTIVE_PLAYER_TAG);
            else tag.putUUID(ACTIVE_PLAYER_TAG, uuid);
        });
    }

    public static CustomData withoutActivePlayer(CustomData data) {
        if (!data.contains(ACTIVE_PLAYER_TAG)) return data;
        CompoundTag tag = data.copyTag();
        tag.remove(ACTIVE_PLAYER_TAG);
        return CustomData.of(tag);
    }

    public static void releaseMouthpiece(ServerPlayer wearer, ItemStack stack) {
        UUID active = getActivePlayerUuid(stack);
        boolean released = active != null && ActiveSessions.server().unregister(active, stack);
        setActivePlayerUuid(stack, null);
        if (released) {
            ServerPlayer activePlayer = wearer.server.getPlayerList().getPlayer(active);
            if (activePlayer != null) {
                PacketDistributor.sendToPlayer(activePlayer, WornHookahSyncPayload.release());
            }
        }
    }

    public static boolean tryTakeMouthpiece(ServerPlayer player, ServerPlayer wearer, ItemStack stack) {
        if (!isHookahStack(stack) || wearer.getItemBySlot(EquipmentSlot.CHEST) != stack) return false;
        return HookahSessions.toggle(player, new WornHookahHost(wearer, stack));
    }

    public static boolean playerHasMouthpiece(Player player) {
        Item mouthpiece = ModItems.HOOKAH_MOUTHPIECE.get();
        if (player.getMainHandItem().is(mouthpiece)) return true;
        if (player.getOffhandItem().is(mouthpiece)) return true;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(mouthpiece)) return true;
        }
        return false;
    }

    @Nullable
    public static Player findClaimedWearer(Player user, Level level) {
        UUID wearerUuid = ActiveSessions.of(level).getWornWearer(user.getUUID());
        return wearerUuid == null ? null : level.getPlayerByUUID(wearerUuid);
    }

    public static ItemStack findClaimedStack(Player user, Level level) {
        Player wearer = findClaimedWearer(user, level);
        if (wearer == null) return ItemStack.EMPTY;
        ItemStack stack = wearer.getItemBySlot(EquipmentSlot.CHEST);
        UUID active = getActivePlayerUuid(stack);
        return isHookahStack(stack) && user.getUUID().equals(active) ? stack : ItemStack.EMPTY;
    }

    public static boolean isUserInRange(Player user, Player wearer, ItemStack stack) {
        int maxLength = getHoseType(stack).getMaxLength();
        return user.level() == wearer.level() && maxLength > 0
                && user.distanceToSqr(wearer) <= (double) maxLength * (double) maxLength;
    }

    public static void clearStaleSession(ItemStack stack) {
        UUID active = getActivePlayerUuid(stack);
        if (active != null && !ActiveSessions.server().owns(active, stack)) setActivePlayerUuid(stack, null);
    }

    public static void applyExhale(ServerPlayer player, ServerPlayer wearer, ItemStack stack, float charge) {
        HookahSessions.exhale(player, new WornHookahHost(wearer, stack), charge);
    }

    static void depleteConsumables(ItemStack stack, ServerPlayer player) {
        NonNullList<ItemStack> items = getItems(stack);
        HookahProgress.Consumption consumed = HookahProgress.read(stack).consume(items);
        consumed.progress().write(stack);
        if (consumed.emptyCan()) WhiteMonsterItem.giveEmptyCan(player);
        if (consumed.itemsChanged()) setItems(stack, items);
    }

    private static final class StackContainer implements Container {
        private final ItemStack stack;
        @Nullable
        private final Player wearer;
        private NonNullList<ItemStack> items;
        private ItemContainerContents lastSeen;

        private StackContainer(ItemStack stack, @Nullable Player wearer) {
            this.stack = stack;
            this.wearer = wearer;
            this.items = getItems(stack);
            this.lastSeen = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        }

        private void refreshIfChangedExternally() {
            ItemContainerContents current = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            if (current != lastSeen) {
                items = getItems(stack);
                lastSeen = current;
            }
        }

        @Override public int getContainerSize() { return HookahBlockEntity.SLOT_COUNT; }

        @Override
        public boolean isEmpty() {
            refreshIfChangedExternally();
            return items.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            refreshIfChangedExternally();
            return items.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            refreshIfChangedExternally();
            ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
            if (!removed.isEmpty()) setChanged();
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            refreshIfChangedExternally();
            ItemStack removed = ContainerHelper.takeItem(items, slot);
            setChanged();
            return removed;
        }

        @Override
        public void setItem(int slot, ItemStack itemStack) {
            refreshIfChangedExternally();
            items.set(slot, itemStack);
            setChanged();
        }

        @Override
        public void setChanged() {
            setItems(stack, items);
            lastSeen = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        }

        @Override public boolean stillValid(Player player) {
            return wearer == player && wearer.getItemBySlot(EquipmentSlot.CHEST) == stack;
        }

        @Override
        public void clearContent() {
            items.clear();
            setChanged();
        }
    }
}

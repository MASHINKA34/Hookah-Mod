package com.hookahmod.item;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.event.ActiveSessions;
import com.hookahmod.integration.KingdomsIntegration;
import com.hookahmod.network.WornHookahSyncPayload;
import com.hookahmod.registry.ModItems;
import com.hookahmod.smoke.HookahSmoke;
import com.hookahmod.smoking.IntoxicationState;
import com.hookahmod.smoking.HookahProgress;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

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

    public static HookahHoseType getHoseType(ItemStack stack) {
        ItemStack hoseStack = getItems(stack).get(HookahBlockEntity.SLOT_HOSE);
        if (hoseStack.getItem() instanceof HookahHoseItem hose) return hose.getHoseType();
        return HookahHoseType.NONE;
    }

    public static boolean hasAllConsumables(ItemStack stack) {
        NonNullList<ItemStack> items = getItems(stack);
        return !items.get(HookahBlockEntity.SLOT_TOBACCO).isEmpty()
                && !items.get(HookahBlockEntity.SLOT_COAL).isEmpty()
                && !items.get(HookahBlockEntity.SLOT_WATER).isEmpty();
    }

    public static boolean hasCoal(ItemStack stack) {
        return !getItems(stack).get(HookahBlockEntity.SLOT_COAL).isEmpty();
    }

    @Nullable
    public static UUID getActivePlayerUuid(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
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
        if (wearer.getItemBySlot(EquipmentSlot.CHEST) != stack || !isHookahStack(stack)
                || !player.isAlive() || !wearer.isAlive() || player.isSpectator() || wearer.isSpectator()
                || player.level() != wearer.level()) return false;
        clearStaleSession(stack);
        HookahHoseType hoseType = getHoseType(stack);
        if (!hoseType.isPresent()) {
            player.displayClientMessage(Component.translatable("message.hookahmod.install_hose"), true);
            return false;
        }
        if (!isUserInRange(player, wearer, stack)) return false;

        UUID active = getActivePlayerUuid(stack);
        if (active != null && !active.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.hookahmod.busy"), true);
            return false;
        }
        if (active != null) {
            releaseMouthpiece(wearer, stack);
            return true;
        }
        if (!playerHasMouthpiece(player)) {
            player.displayClientMessage(Component.translatable("message.hookahmod.no_mouthpiece"), true);
            return false;
        }

        ActiveSessions.server().beginWorn(player, wearer, stack);
        setActivePlayerUuid(stack, player.getUUID());
        PacketDistributor.sendToPlayer(player, WornHookahSyncPayload.claim(wearer.getUUID()));
        return true;
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
        if (!(player.level() instanceof ServerLevel server)) return;
        if (!ActiveSessions.server().owns(player.getUUID(), stack)
                || wearer.getItemBySlot(EquipmentSlot.CHEST) != stack
                || !isUserInRange(player, wearer, stack) || !hasAllConsumables(stack)) return;

        NonNullList<ItemStack> items = getItems(stack);
        ItemStack tobaccoStack = items.get(HookahBlockEntity.SLOT_TOBACCO);
        HookahTier tier = HookahTier.fromStack(stack);
        Vector3f smokeColor = tobaccoStack.getItem() instanceof AbstractTobaccoItem tobaccoForSmoke
                ? tobaccoForSmoke.smokeColor()
                : null;
        Vec3 hookahPoint = wearer.position().add(0, wearer.getBbHeight() * 0.72, 0);
        HookahSmoke.spawnExhaleSmoke(server, hookahPoint, player, charge, smokeColor);

        player.level().playSound(null, wearer.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_DRINK,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.15f + charge * 0.4f, 1.6f);

        if (tobaccoStack.getItem() instanceof AbstractTobaccoItem tobacco) {
            IntoxicationState.add(player, IntoxicationState.gain(tobacco.intoxication(), charge));
            tobacco.onExhale(
                    server,
                    player,
                    charge,
                    tier.effectMult(),
                    tier.combatMult() * KingdomsIntegration.hookahCombatMultiplier(player, wearer)
            );
        } else {
            IntoxicationState.add(player, IntoxicationState.gain(IntoxicationState.REGULAR_TOBACCO_INTOXICATION, charge));
        }

        KingdomsIntegration.onHookahPuff(player, charge);
        if (!KingdomsIntegration.hasHookahMastery(player, wearer)) {
            depleteConsumables(stack, player);
        }
    }

    private static void depleteConsumables(ItemStack stack, ServerPlayer player) {
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

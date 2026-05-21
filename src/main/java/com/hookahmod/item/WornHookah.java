package com.hookahmod.item;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.event.ActiveSessions;
import com.hookahmod.network.WornHookahSyncPayload;
import com.hookahmod.registry.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
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

import java.util.List;
import java.util.UUID;

public final class WornHookah {

    private static final String ACTIVE_PLAYER_TAG = "HookahActivePlayer";
    private static final String SMOKE_TIMER_TAG = "HookahSmokeTimer";
    private static final String WATER_TIMER_TAG = "HookahWaterTimer";

    private static final int PUFFS_PER_SOLID = 20;
    private static final int PUFFS_PER_WATER = 200;

    private WornHookah() {}

    public static boolean isHookahStack(ItemStack stack) {
        return stack.is(ModItems.HOOKAH.get());
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
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (uuid == null) tag.remove(ACTIVE_PLAYER_TAG);
            else tag.putUUID(ACTIVE_PLAYER_TAG, uuid);
        });
    }

    public static void releaseMouthpiece(ServerPlayer wearer, ItemStack stack) {
        UUID active = getActivePlayerUuid(stack);
        if (active != null) ActiveSessions.unregister(active);
        setActivePlayerUuid(stack, null);
        wearer.setItemSlot(EquipmentSlot.CHEST, stack);
        if (active != null) {
            ServerPlayer activePlayer = wearer.server.getPlayerList().getPlayer(active);
            if (activePlayer != null) {
                PacketDistributor.sendToPlayer(activePlayer, WornHookahSyncPayload.release());
            }
        }
    }

    public static boolean tryTakeMouthpiece(ServerPlayer player, ServerPlayer wearer, ItemStack stack) {
        HookahHoseType hoseType = getHoseType(stack);
        if (!hoseType.isPresent()) {
            player.displayClientMessage(Component.translatable("message.hookahmod.install_hose"), true);
            return false;
        }

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

        setActivePlayerUuid(stack, player.getUUID());
        ActiveSessions.registerWorn(player.getUUID(), wearer.getUUID());
        wearer.setItemSlot(EquipmentSlot.CHEST, stack);
        PacketDistributor.sendToPlayer(player, WornHookahSyncPayload.claim(wearer.getUUID()));
        return true;
    }

    private static boolean playerHasMouthpiece(Player player) {
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
        UUID wearerUuid = ActiveSessions.getWornWearer(user.getUUID());
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
        return maxLength > 0 && user.distanceToSqr(wearer) <= (double) maxLength * (double) maxLength;
    }

    public static void applyExhale(ServerPlayer player, ServerPlayer wearer, ItemStack stack, float charge) {
        if (!(player.level() instanceof ServerLevel server)) return;

        Vec3 hookahPoint = wearer.position().add(0, wearer.getBbHeight() * 0.72, 0);
        int hookahPuffs = 2 + (int) (charge * 5);
        var hookahPkt = new net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket(
                ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                hookahPoint.x, hookahPoint.y, hookahPoint.z,
                0.12f, 0.05f, 0.12f, 0.02f, hookahPuffs);
        for (ServerPlayer sp : server.players()) {
            if (sp.distanceToSqr(hookahPoint) <= 128 * 128) sp.connection.send(hookahPkt);
        }

        int mouthPuffs = 6 + (int) (charge * 20);
        float spread = 0.08f + charge * 0.20f;
        float speed = 0.025f + charge * 0.08f;
        Vec3 look = player.getLookAngle();
        double mx = player.getX() + look.x * 0.5;
        double my = player.getY() + player.getEyeHeight() - 0.08;
        double mz = player.getZ() + look.z * 0.5;
        var mouthPkt = new net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket(
                ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                mx, my, mz, spread, 0.06f, spread, speed, mouthPuffs);
        for (ServerPlayer sp : server.players()) {
            if (sp.distanceToSqr(mx, my, mz) <= 128 * 128) sp.connection.send(mouthPkt);
        }

        player.level().playSound(null, wearer.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_DRINK,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.15f + charge * 0.4f, 1.6f);

        depleteConsumables(stack);
        wearer.setItemSlot(EquipmentSlot.CHEST, stack);
    }

    private static void depleteConsumables(ItemStack stack) {
        NonNullList<ItemStack> items = getItems(stack);
        boolean changed = false;

        int smokeTimer = getInt(stack, SMOKE_TIMER_TAG) + 1;
        if (smokeTimer >= PUFFS_PER_SOLID) {
            smokeTimer = 0;
            items.get(HookahBlockEntity.SLOT_TOBACCO).shrink(1);
            items.get(HookahBlockEntity.SLOT_COAL).shrink(1);
            changed = true;
        }

        int waterTimer = getInt(stack, WATER_TIMER_TAG) + 1;
        if (waterTimer >= PUFFS_PER_WATER) {
            waterTimer = 0;
            items.get(HookahBlockEntity.SLOT_WATER).shrink(1);
            changed = true;
        }

        setInt(stack, SMOKE_TIMER_TAG, smokeTimer);
        setInt(stack, WATER_TIMER_TAG, waterTimer);
        if (changed) setItems(stack, items);
    }

    private static int getInt(ItemStack stack, String key) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(key);
    }

    private static void setInt(ItemStack stack, String key, int value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(key, value));
    }

    private static final class StackContainer implements Container {
        private final ItemStack stack;
        @Nullable
        private final Player wearer;
        private final NonNullList<ItemStack> items;

        private StackContainer(ItemStack stack, @Nullable Player wearer) {
            this.stack = stack;
            this.wearer = wearer;
            this.items = getItems(stack);
        }

        @Override public int getContainerSize() { return HookahBlockEntity.SLOT_COUNT; }
        @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
        @Override public ItemStack getItem(int slot) { return items.get(slot); }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
            if (!removed.isEmpty()) setChanged();
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack removed = ContainerHelper.takeItem(items, slot);
            setChanged();
            return removed;
        }

        @Override
        public void setItem(int slot, ItemStack itemStack) {
            items.set(slot, itemStack);
            setChanged();
        }

        @Override
        public void setChanged() {
            setItems(stack, items);
            if (wearer != null && !wearer.level().isClientSide) {
                wearer.setItemSlot(EquipmentSlot.CHEST, stack);
            }
        }

        @Override public boolean stillValid(Player player) { return true; }

        @Override
        public void clearContent() {
            items.clear();
            setChanged();
        }
    }
}

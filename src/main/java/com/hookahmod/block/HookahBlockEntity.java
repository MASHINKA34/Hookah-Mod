package com.hookahmod.block;

import com.hookahmod.item.HookahHoseItem;
import com.hookahmod.item.HookahHoseType;
import com.hookahmod.network.HookahSyncPayload;
import com.hookahmod.registry.ModBlockEntities;
import com.hookahmod.registry.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class HookahBlockEntity extends BlockEntity {

    public static final int SLOT_HOSE = 0;
    public static final int SLOT_TOBACCO = 1;
    public static final int SLOT_COAL = 2;
    public static final int SLOT_WATER = 3;
    public static final int SLOT_COUNT = 4;

    private static final int CONSUME_INTERVAL_TICKS = 200; // 10s
    private static final int PARTICLE_INTERVAL_TICKS = 10;
    private static final int EFFECT_DURATION_TICKS = 220;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final Container inventory = new HookahContainer(items, this::setChangedAndSync, this);

    @Nullable
    private UUID activePlayerUuid;

    private int smokeTimer = 0;

    public HookahBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HOOKAH.get(), pos, state);
    }

    public Container getInventory() { return inventory; }

    public HookahHoseType getHoseType() {
        ItemStack stack = items.get(SLOT_HOSE);
        if (stack.getItem() instanceof HookahHoseItem hose) return hose.getHoseType();
        return HookahHoseType.NONE;
    }

    public void setHoseType(HookahHoseType type) {
        if (type == HookahHoseType.NONE) {
            items.set(SLOT_HOSE, ItemStack.EMPTY);
        } else {
            items.set(SLOT_HOSE, HookahHoseItem.stackFor(type));
        }
        setChangedAndSync();
    }

    public boolean isInUse() { return activePlayerUuid != null; }

    @Nullable
    public UUID getActivePlayerUuid() { return activePlayerUuid; }

    public boolean hasAllConsumables() {
        return !items.get(SLOT_TOBACCO).isEmpty()
                && !items.get(SLOT_COAL).isEmpty()
                && !items.get(SLOT_WATER).isEmpty();
    }

    public boolean tryTakeMouthpiece(ServerPlayer player) {
        if (!getHoseType().isPresent()) {
            player.displayClientMessage(Component.translatable("message.hookahmod.install_hose"), true);
            return false;
        }
        if (activePlayerUuid != null && !activePlayerUuid.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.hookahmod.busy"), true);
            return false;
        }
        if (activePlayerUuid != null) {
            returnMouthpieceFromOffhand(player);
            releaseMouthpiece();
            return true;
        }
        if (!moveMouthpieceToOffhand(player)) {
            player.displayClientMessage(Component.translatable("message.hookahmod.no_mouthpiece"), true);
            return false;
        }
        activePlayerUuid = player.getUUID();
        if (level != null) {
            com.hookahmod.event.ActiveSessions.register(activePlayerUuid, level.dimension(), worldPosition);
        }
        setChangedAndSync();
        return true;
    }

    private static boolean moveMouthpieceToOffhand(ServerPlayer player) {
        Item mp = ModItems.HOOKAH_MOUTHPIECE.get();
        if (player.getOffhandItem().is(mp)) return true;
        int slot = -1;
        ItemStack found = ItemStack.EMPTY;
        if (player.getMainHandItem().is(mp)) {
            found = player.getMainHandItem();
            slot = -2;
        } else {
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                if (player.getInventory().items.get(i).is(mp)) {
                    found = player.getInventory().items.get(i);
                    slot = i;
                    break;
                }
            }
        }
        if (found.isEmpty()) return false;
        ItemStack one = found.copyWithCount(1);
        found.shrink(1);
        ItemStack old = player.getOffhandItem();
        player.setItemInHand(InteractionHand.OFF_HAND, one);
        if (!old.isEmpty()) {
            if (!player.getInventory().add(old)) {
                player.drop(old, false);
            }
        }
        return true;
    }

    private static void returnMouthpieceFromOffhand(ServerPlayer player) {
        ItemStack off = player.getOffhandItem();
        if (!off.is(ModItems.HOOKAH_MOUTHPIECE.get())) return;
        ItemStack back = off.copy();
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        if (!player.getInventory().add(back)) {
            player.drop(back, false);
        }
    }

    public void releaseMouthpiece() {
        if (activePlayerUuid == null) return;
        com.hookahmod.event.ActiveSessions.unregister(activePlayerUuid);
        activePlayerUuid = null;
        smokeTimer = 0;
        setChangedAndSync();
    }

    public void releaseMouthpieceIfHolder(UUID uuid) {
        if (uuid != null && uuid.equals(activePlayerUuid)) releaseMouthpiece();
    }

    public void dropHoseToPlayer(Level lvl, BlockPos pos, @Nullable Player player) {
        ItemStack hose = items.get(SLOT_HOSE);
        if (hose.isEmpty()) return;
        items.set(SLOT_HOSE, ItemStack.EMPTY);
        if (!lvl.isClientSide) {
            if (player != null) {
                if (!player.getInventory().add(hose)) player.drop(hose, false);
            } else {
                net.minecraft.world.Containers.dropItemStack(lvl, pos.getX(), pos.getY(), pos.getZ(), hose);
            }
        }
        setChangedAndSync();
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        if (activePlayerUuid == null) return;
        Player player = level.getPlayerByUUID(activePlayerUuid);
        if (player == null || player.isRemoved() || player.isDeadOrDying()
                || !player.level().dimension().equals(level.dimension())) {
            releaseMouthpiece();
            return;
        }
        // Mouthpiece must remain in offhand
        if (!player.getOffhandItem().is(ModItems.HOOKAH_MOUTHPIECE.get())) {
            player.displayClientMessage(Component.translatable("message.hookahmod.lost_mouthpiece"), true);
            releaseMouthpiece();
            return;
        }
        int maxLength = getHoseType().getMaxLength();
        if (maxLength <= 0) { releaseMouthpiece(); return; }
        double distSq = player.distanceToSqr(Vec3.atCenterOf(pos));
        if (distSq > (double) maxLength * (double) maxLength) {
            releaseMouthpiece();
            player.displayClientMessage(Component.translatable("message.hookahmod.slipped"), true);
            return;
        }

        if (hasAllConsumables() && level instanceof ServerLevel server) {
            smokeTimer++;
            if (smokeTimer >= CONSUME_INTERVAL_TICKS) {
                smokeTimer = 0;
                items.get(SLOT_TOBACCO).shrink(1);
                items.get(SLOT_COAL).shrink(1);
                items.get(SLOT_WATER).shrink(1);
                setChangedAndSync();
            }
            if (level.getGameTime() % PARTICLE_INTERVAL_TICKS == 0) {
                server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1.6, pos.getZ() + 0.5,
                        2, 0.07, 0.05, 0.07, 0.015);
            }
            if (player instanceof ServerPlayer sp) {
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_DURATION_TICKS, 0, true, false, true));
            }
        } else {
            smokeTimer = 0;
        }
    }

    public void setChangedAndSync() {
        setChanged();
        if (level instanceof ServerLevel server) {
            BlockState s = getBlockState();
            server.sendBlockUpdated(worldPosition, s, s, 3);
            HookahSyncPayload payload = new HookahSyncPayload(worldPosition, getHoseType(), activePlayerUuid);
            for (ServerPlayer p : server.players()) {
                if (p.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 128.0 * 128.0) {
                    PacketDistributor.sendToPlayer(p, payload);
                }
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, lookup);
        activePlayerUuid = tag.hasUUID("ActivePlayer") ? tag.getUUID("ActivePlayer") : null;
        smokeTimer = tag.getInt("SmokeTimer");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        ContainerHelper.saveAllItems(tag, items, lookup);
        if (activePlayerUuid != null) tag.putUUID("ActivePlayer", activePlayerUuid);
        tag.putInt("SmokeTimer", smokeTimer);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        CompoundTag tag = super.getUpdateTag(lookup);
        ContainerHelper.saveAllItems(tag, items, lookup);
        if (activePlayerUuid != null) tag.putUUID("ActivePlayer", activePlayerUuid);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookup) {
        super.handleUpdateTag(tag, lookup);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, lookup);
        activePlayerUuid = tag.hasUUID("ActivePlayer") ? tag.getUUID("ActivePlayer") : null;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookup) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) handleUpdateTag(tag, lookup);
    }

    public void applySync(HookahHoseType type, @Nullable UUID active) {
        items.set(SLOT_HOSE, HookahHoseItem.stackFor(type));
        this.activePlayerUuid = active;
    }

    public static final class HookahContainer implements Container {
        private final NonNullList<ItemStack> backing;
        private final Runnable onChange;
        private final HookahBlockEntity be;

        public HookahContainer(NonNullList<ItemStack> backing, Runnable onChange, HookahBlockEntity be) {
            this.backing = backing;
            this.onChange = onChange;
            this.be = be;
        }

        @Override public int getContainerSize() { return backing.size(); }
        @Override public boolean isEmpty() { return backing.stream().allMatch(ItemStack::isEmpty); }
        @Override public ItemStack getItem(int slot) { return backing.get(slot); }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack s = ContainerHelper.removeItem(backing, slot, amount);
            if (!s.isEmpty()) onChange.run();
            return s;
        }

        @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(backing, slot); }
        @Override public void setItem(int slot, ItemStack stack) { backing.set(slot, stack); onChange.run(); }
        @Override public void setChanged() { onChange.run(); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() { backing.clear(); onChange.run(); }
    }
}

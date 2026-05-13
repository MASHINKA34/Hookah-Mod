package com.hookahmod.block;

import com.hookahmod.item.HookahHoseType;
import com.hookahmod.network.HookahSyncPayload;
import com.hookahmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class HookahBlockEntity extends BlockEntity {

    public static final int SLOT_TOBACCO = 0;
    public static final int SLOT_COAL = 1;
    public static final int SLOT_WATER = 2;
    public static final int SLOT_COUNT = 3;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final Container inventory = new SimpleContainerView(items, this::setChangedAndSync);

    @Nullable
    private UUID activePlayerUuid;
    private HookahHoseType hoseType = HookahHoseType.NONE;

    public HookahBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HOOKAH.get(), pos, state);
    }

    public Container getInventory() {
        return inventory;
    }

    public HookahHoseType getHoseType() {
        return hoseType;
    }

    public void setHoseType(HookahHoseType type) {
        this.hoseType = type == null ? HookahHoseType.NONE : type;
        setChangedAndSync();
    }

    public boolean isInUse() {
        return activePlayerUuid != null;
    }

    @Nullable
    public UUID getActivePlayerUuid() {
        return activePlayerUuid;
    }

    public boolean tryTakeMouthpiece(ServerPlayer player) {
        if (!hoseType.isPresent()) {
            player.displayClientMessage(Component.translatable("message.hookahmod.install_hose"), true);
            return false;
        }
        if (activePlayerUuid != null && !activePlayerUuid.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.hookahmod.busy"), true);
            return false;
        }
        if (activePlayerUuid != null && activePlayerUuid.equals(player.getUUID())) {
            releaseMouthpiece();
            return true;
        }
        activePlayerUuid = player.getUUID();
        if (level != null) {
            com.hookahmod.event.ActiveSessions.register(activePlayerUuid, level.dimension(), worldPosition);
        }
        setChangedAndSync();
        return true;
    }

    public void releaseMouthpiece() {
        if (activePlayerUuid == null) return;
        com.hookahmod.event.ActiveSessions.unregister(activePlayerUuid);
        activePlayerUuid = null;
        setChangedAndSync();
    }

    public void releaseMouthpieceIfHolder(UUID uuid) {
        if (uuid != null && uuid.equals(activePlayerUuid)) releaseMouthpiece();
    }

    public void dropHoseAndClear(Level level, BlockPos pos, @Nullable Player player) {
        if (!hoseType.isPresent()) return;
        ItemStack hoseStack = com.hookahmod.item.HookahHoseItem.stackFor(hoseType);
        if (!level.isClientSide && !hoseStack.isEmpty()) {
            if (player != null) {
                if (!player.getInventory().add(hoseStack)) {
                    player.drop(hoseStack, false);
                }
            } else {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), hoseStack);
            }
        }
        setHoseType(HookahHoseType.NONE);
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide || activePlayerUuid == null) return;
        Player player = level.getPlayerByUUID(activePlayerUuid);
        if (player == null || player.isRemoved() || player.isDeadOrDying() || !player.level().dimension().equals(level.dimension())) {
            releaseMouthpiece();
            return;
        }
        int maxLength = hoseType.getMaxLength();
        if (maxLength <= 0) {
            releaseMouthpiece();
            return;
        }
        double distSq = player.distanceToSqr(Vec3.atCenterOf(pos));
        if (distSq > (double) maxLength * (double) maxLength) {
            releaseMouthpiece();
            player.displayClientMessage(Component.translatable("message.hookahmod.slipped"), true);
        }
    }

    public void setChangedAndSync() {
        setChanged();
        if (level instanceof ServerLevel server) {
            BlockState s = getBlockState();
            server.sendBlockUpdated(worldPosition, s, s, 3);
            HookahSyncPayload payload = new HookahSyncPayload(worldPosition, hoseType, activePlayerUuid);
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
        hoseType = HookahHoseType.byId(tag.getInt("HoseType"));
        if (tag.hasUUID("ActivePlayer")) {
            activePlayerUuid = tag.getUUID("ActivePlayer");
        } else {
            activePlayerUuid = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        ContainerHelper.saveAllItems(tag, items, lookup);
        tag.putInt("HoseType", hoseType.ordinal());
        if (activePlayerUuid != null) tag.putUUID("ActivePlayer", activePlayerUuid);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        CompoundTag tag = super.getUpdateTag(lookup);
        tag.putInt("HoseType", hoseType.ordinal());
        if (activePlayerUuid != null) tag.putUUID("ActivePlayer", activePlayerUuid);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookup) {
        super.handleUpdateTag(tag, lookup);
        hoseType = HookahHoseType.byId(tag.getInt("HoseType"));
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
        this.hoseType = type == null ? HookahHoseType.NONE : type;
        this.activePlayerUuid = active;
    }

    public static final class SimpleContainerView implements Container {
        private final NonNullList<ItemStack> backing;
        private final Runnable onChange;

        public SimpleContainerView(NonNullList<ItemStack> backing, Runnable onChange) {
            this.backing = backing;
            this.onChange = onChange;
        }

        @Override
        public int getContainerSize() { return backing.size(); }

        @Override
        public boolean isEmpty() { return backing.stream().allMatch(ItemStack::isEmpty); }

        @Override
        public ItemStack getItem(int slot) { return backing.get(slot); }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack s = ContainerHelper.removeItem(backing, slot, amount);
            if (!s.isEmpty()) onChange.run();
            return s;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(backing, slot); }

        @Override
        public void setItem(int slot, ItemStack stack) { backing.set(slot, stack); onChange.run(); }

        @Override
        public void setChanged() { onChange.run(); }

        @Override
        public boolean stillValid(Player player) { return true; }

        @Override
        public void clearContent() { backing.clear(); onChange.run(); }
    }
}

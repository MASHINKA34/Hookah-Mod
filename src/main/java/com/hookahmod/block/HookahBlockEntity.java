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
            releaseMouthpiece();
            return true;
        }
        if (!playerHasMouthpiece(player)) {
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

    private static boolean playerHasMouthpiece(Player player) {
        Item mp = ModItems.HOOKAH_MOUTHPIECE.get();
        if (player.getMainHandItem().is(mp)) return true;
        if (player.getOffhandItem().is(mp)) return true;
        for (ItemStack s : player.getInventory().items) {
            if (s.is(mp)) return true;
        }
        return false;
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
        int maxLength = getHoseType().getMaxLength();
        if (maxLength <= 0) { releaseMouthpiece(); return; }
        double distSq = player.distanceToSqr(Vec3.atCenterOf(pos));
        if (distSq > (double) maxLength * (double) maxLength) {
            releaseMouthpiece();
            player.displayClientMessage(Component.translatable("message.hookahmod.slipped"), true);
        }
    }

    public void applyPuff(ServerPlayer player) {
        if (level == null) return;
        if (level instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.75, worldPosition.getZ() + 0.5,
                    4, 0.15, 0.05, 0.15, 0.02);
            net.minecraft.world.phys.Vec3 look = player.getLookAngle();
            double mx = player.getX() + look.x * 0.35;
            double my = player.getY() + player.getEyeHeight() - 0.10;
            double mz = player.getZ() + look.z * 0.35;
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    mx, my, mz, 6, 0.08, 0.03, 0.08, 0.04);
        }
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.GENERIC_DRINK,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.25F, 1.7F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false, true));
        smokeTimer++;
        if (smokeTimer >= 20) {
            smokeTimer = 0;
            items.get(SLOT_TOBACCO).shrink(1);
            items.get(SLOT_COAL).shrink(1);
            items.get(SLOT_WATER).shrink(1);
            setChangedAndSync();
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

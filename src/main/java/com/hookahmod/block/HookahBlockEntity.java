package com.hookahmod.block;

import com.hookahmod.item.AbstractTobaccoItem;
import com.hookahmod.item.WhiteMonsterItem;
import com.hookahmod.item.HookahHoseItem;
import com.hookahmod.item.HookahHoseType;
import com.hookahmod.item.HookahTier;
import com.hookahmod.integration.KingdomsIntegration;
import com.hookahmod.network.HookahSyncPayload;
import com.hookahmod.registry.ModBlockEntities;
import com.hookahmod.registry.ModItems;
import com.hookahmod.registry.ModBlocks;
import com.hookahmod.smoke.HookahSmoke;
import com.hookahmod.smoking.IntoxicationState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

public class HookahBlockEntity extends BlockEntity {

    public static final int SLOT_HOSE = 0;
    public static final int SLOT_TOBACCO = 1;
    public static final int SLOT_COAL = 2;
    public static final int SLOT_WATER = 3;
    public static final int SLOT_COUNT = 4;

    private static final int PUFFS_PER_SOLID = 20;
    private static final int PUFFS_PER_WATER = 200;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final Container inventory = new HookahContainer(items, this::setChangedAndSync, this);

    @Nullable
    private UUID activePlayerUuid;

    private int smokeTimer = 0;
    private int waterTimer = 0;

    public HookahBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HOOKAH.get(), pos, state);
    }

    public Container getInventory() { return inventory; }

    public void saveItemsToStack(ItemStack stack) {
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    public void loadItemsFromStack(ItemStack stack) {
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        contents.copyInto(items);
        setChangedAndSync();
    }

    public void clearItemsForPickup() {
        items.clear();
        setChanged();
    }

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
            com.hookahmod.event.ActiveSessions.of(level).register(activePlayerUuid, level.dimension(), worldPosition);
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
        if (level != null) com.hookahmod.event.ActiveSessions.of(level).unregister(activePlayerUuid);
        activePlayerUuid = null;
        // smokeTimer/waterTimer intentionally NOT reset: bowl depletion is
        // tied to the hookah and persists across sessions, so partial puffs
        // can't be reset by un-claiming or re-seating the consumables.
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

    public void applyExhale(ServerPlayer player, float charge) {
        if (level == null || !(level instanceof ServerLevel server)) return;

        ItemStack tobaccoStack = items.get(SLOT_TOBACCO);
        Vector3f smokeColor = tobaccoStack.getItem() instanceof AbstractTobaccoItem tobaccoForSmoke
                ? tobaccoForSmoke.smokeColor()
                : null;
        Vec3 hookahPoint = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.75, worldPosition.getZ() + 0.5);
        HookahSmoke.spawnExhaleSmoke(server, hookahPoint, player, charge, smokeColor);

        float volume = 0.15f + charge * 0.4f;
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.GENERIC_DRINK,
                net.minecraft.sounds.SoundSource.PLAYERS, volume, 1.6f);

        if (tobaccoStack.getItem() instanceof AbstractTobaccoItem tobacco) {
            HookahTier tier = getBlockState().hasProperty(HookahBlock.TIER)
                    ? getBlockState().getValue(HookahBlock.TIER)
                    : HookahTier.NORMAL;
            IntoxicationState.add(player, IntoxicationState.gain(tobacco.intoxication(), charge));
            tobacco.onExhale(
                    server,
                    player,
                    charge,
                    tier.effectMult(),
                    tier.combatMult() * KingdomsIntegration.hookahCombatMultiplier(player)
            );
        } else {
            IntoxicationState.add(player, IntoxicationState.gain(IntoxicationState.REGULAR_TOBACCO_INTOXICATION, charge));
        }

        KingdomsIntegration.onHookahPuff(player, charge);
        if (KingdomsIntegration.hasHookahMastery(player)) {
            return;
        }

        boolean changed = false;
        smokeTimer++;
        if (smokeTimer >= puffsPerSolid(tobaccoStack)) {
            smokeTimer = 0;
            items.get(SLOT_TOBACCO).shrink(1);
            items.get(SLOT_COAL).shrink(1);
            changed = true;
        }
        waterTimer++;
        if (waterTimer >= PUFFS_PER_WATER) {
            waterTimer = 0;
            ItemStack water = items.get(SLOT_WATER);
            boolean whiteMonster = water.is(ModItems.WHITE_MONSTER.get());
            water.shrink(1);
            if (whiteMonster) WhiteMonsterItem.giveEmptyCan(player);
            changed = true;
        }
        if (changed) setChangedAndSync();
    }

    private static int puffsPerSolid(ItemStack tobaccoStack) {
        return tobaccoStack.getItem() instanceof AbstractTobaccoItem tobacco && tobacco.category() == com.hookahmod.item.TobaccoCategory.COMBAT
                ? 10
                : PUFFS_PER_SOLID;
    }

    public void clientTick(Level level, BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.LUXURY_HOOKAH_PREVIEW.get())) return;
        if (items.get(SLOT_COAL).isEmpty()) return;
        if (level.random.nextInt(6) == 0) {
            double px = pos.getX() + 0.35 + level.random.nextDouble() * 0.30;
            double py = pos.getY() + 1.67 + level.random.nextDouble() * 0.09;
            double pz = pos.getZ() + 0.35 + level.random.nextDouble() * 0.30;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME,
                    px, py, pz,
                    (level.random.nextDouble() - 0.5) * 0.01,
                    0.01 + level.random.nextDouble() * 0.02,
                    (level.random.nextDouble() - 0.5) * 0.01);
        }
    }

    public void setChangedAndSync() {
        setChanged();
        if (level instanceof ServerLevel server) {
            BlockState s = getBlockState();
            boolean hasCoal = !items.get(SLOT_COAL).isEmpty();
            if (s.hasProperty(com.hookahmod.block.HookahBlock.HAS_COAL)
                    && s.getValue(com.hookahmod.block.HookahBlock.HAS_COAL) != hasCoal) {
                server.setBlock(worldPosition, s.setValue(com.hookahmod.block.HookahBlock.HAS_COAL, hasCoal), 3);
            } else {
                server.sendBlockUpdated(worldPosition, s, s, 3);
            }
            HookahSyncPayload payload = new HookahSyncPayload(worldPosition, activePlayerUuid);
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
        waterTimer = tag.getInt("WaterTimer");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        ContainerHelper.saveAllItems(tag, items, lookup);
        if (activePlayerUuid != null) tag.putUUID("ActivePlayer", activePlayerUuid);
        tag.putInt("SmokeTimer", smokeTimer);
        tag.putInt("WaterTimer", waterTimer);
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

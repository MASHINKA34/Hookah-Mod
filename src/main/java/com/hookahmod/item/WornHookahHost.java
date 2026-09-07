package com.hookahmod.item;

import com.hookahmod.event.ActiveSessions;
import com.hookahmod.integration.KingdomsIntegration;
import com.hookahmod.network.WornHookahSyncPayload;
import com.hookahmod.smoking.HookahHost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The hookah worn in someone's chest slot, seen as a {@link HookahHost}. A
 * session stays bound to the exact stack it was opened on, so moving the
 * hookah to another player ends it instead of silently following the item.
 */
public record WornHookahHost(ServerPlayer wearer, ItemStack stack) implements HookahHost {

    @Override
    public Object sessionKey() {
        return stack;
    }

    @Override
    public HookahHoseType hoseType() {
        return WornHookah.getHoseType(stack);
    }

    @Override
    public HookahTier tier() {
        return HookahTier.fromStack(stack);
    }

    @Nullable
    @Override
    public UUID activePlayer() {
        return WornHookah.getActivePlayerUuid(stack);
    }

    @Override
    public ItemStack consumable(int slot) {
        return WornHookah.itemAt(stack, slot);
    }

    @Override
    public boolean hasAllConsumables() {
        return WornHookah.hasAllConsumables(stack);
    }

    @Override
    public boolean canBeUsedBy(ServerPlayer player) {
        return wearer.getItemBySlot(EquipmentSlot.CHEST) == stack
                && WornHookah.isHookahStack(stack)
                && player.isAlive() && !player.isSpectator()
                && wearer.isAlive() && !wearer.isSpectator()
                && player.level() == wearer.level();
    }

    @Override
    public boolean inRange(Player player) {
        return WornHookah.isUserInRange(player, wearer, stack);
    }

    @Override
    public Vec3 exhaleOrigin() {
        return wearer.position().add(0.0, wearer.getBbHeight() * 0.72, 0.0);
    }

    @Override
    public BlockPos soundPosition() {
        return wearer.blockPosition();
    }

    @Override
    public float combatMultiplier(ServerPlayer smoker) {
        return KingdomsIntegration.hookahCombatMultiplier(smoker, wearer);
    }

    @Override
    public boolean keepsCharge(ServerPlayer smoker) {
        return KingdomsIntegration.hasHookahMastery(smoker, wearer);
    }

    @Override
    public void consumeCharge(ServerPlayer smoker) {
        WornHookah.depleteConsumables(stack, smoker);
    }

    @Override
    public void claim(ServerPlayer player) {
        ActiveSessions.server().beginWorn(player, wearer, stack);
        WornHookah.setActivePlayerUuid(stack, player.getUUID());
        PacketDistributor.sendToPlayer(player, WornHookahSyncPayload.claim(wearer.getUUID()));
    }

    @Override
    public void release() {
        WornHookah.releaseMouthpiece(wearer, stack);
    }
}

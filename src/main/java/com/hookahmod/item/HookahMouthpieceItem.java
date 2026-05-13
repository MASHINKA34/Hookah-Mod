package com.hookahmod.item;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.event.ActiveSessions;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class HookahMouthpieceItem extends Item {

    public static final int USE_DURATION = 72000;

    public HookahMouthpieceItem(Properties props) { super(props); }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Both sides check if this player has a claimed hookah session
        if (ActiveSessions.get(player.getUUID()) == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.hookahmod.claim_first"), true);
            }
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        if (!level.isClientSide) {
            HookahBlockEntity be = findClaimedHookah(player, level);
            if (be == null) {
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            double maxLen = be.getHoseType().getMaxLength();
            if (player.distanceToSqr(Vec3.atCenterOf(be.getBlockPos())) > maxLen * maxLen) {
                player.displayClientMessage(Component.translatable("message.hookahmod.slipped"), true);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (!(entity instanceof Player player)) return;
        int held = USE_DURATION - remainingTicks;
        if (held <= 0 || held % 5 != 0) return;

        // Client-side: smoke particles at player's mouth
        if (level.isClientSide) {
            Vec3 look = entity.getLookAngle();
            double px = entity.getX() + look.x * 0.35;
            double py = entity.getY() + entity.getEyeHeight() - 0.12;
            double pz = entity.getZ() + look.z * 0.35;
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz,
                    look.x * 0.02, 0.01, look.z * 0.02);
            return;
        }

        HookahBlockEntity be = findClaimedHookah(player, level);
        if (be == null) {
            player.stopUsingItem();
            return;
        }
        double maxLen = be.getHoseType().getMaxLength();
        double distSq = player.distanceToSqr(Vec3.atCenterOf(be.getBlockPos()));
        if (distSq > maxLen * maxLen) {
            player.stopUsingItem();
            return;
        }
        if (!be.hasAllConsumables()) {
            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.translatable("gui.hookahmod.fill_slots"), true);
            }
            player.stopUsingItem();
            return;
        }
        if (player instanceof ServerPlayer sp) {
            be.applyPuff(sp);
        }
    }

    public static HookahBlockEntity findClaimedHookah(Player player, Level level) {
        GlobalPos gp = ActiveSessions.get(player.getUUID());
        if (gp == null || !gp.dimension().equals(level.dimension())) return null;
        BlockEntity be = level.getBlockEntity(gp.pos());
        if (be instanceof HookahBlockEntity hbe && player.getUUID().equals(hbe.getActivePlayerUuid())) {
            return hbe;
        }
        return null;
    }
}

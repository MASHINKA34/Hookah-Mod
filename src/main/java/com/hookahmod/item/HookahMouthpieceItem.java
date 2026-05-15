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
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class HookahMouthpieceItem extends Item implements GeoItem {

    public static final int MAX_CHARGE_TICKS = 100;

    private static final RawAnimation SMOKING_ANIM =
            RawAnimation.begin().thenPlay("animation.hookah_mouthpiece.smoking");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HookahMouthpieceItem(Properties props) {
        super(props);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    // ── GeoItem ─────────────────────────────────────────────────────
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(this, "controller", 2, state -> PlayState.STOP)
                        .triggerableAnim("smoking", SMOKING_ANIM)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ── Use animation ────────────────────────────────────────────────
    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return MAX_CHARGE_TICKS; }

    // ── Start using ─────────────────────────────────────────────────
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (ActiveSessions.get(player.getUUID()) == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.hookahmod.claim_first"), true);
            }
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        if (!level.isClientSide) {
            HookahBlockEntity be = findClaimedHookah(player, level);
            if (be == null) return InteractionResultHolder.fail(player.getItemInHand(hand));
            if (player.distanceToSqr(Vec3.atCenterOf(be.getBlockPos())) > be.getHoseType().getMaxLength() * be.getHoseType().getMaxLength()) {
                player.displayClientMessage(Component.translatable("message.hookahmod.slipped"), true);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            if (!be.hasAllConsumables()) {
                player.displayClientMessage(Component.translatable("gui.hookahmod.fill_slots"), true);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            this.triggerAnim(player, (long) player.getId(), "controller", "smoking");
            this.triggerAnim(player, 0L, "controller", "smoking");
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    // ── While holding ───────────────────────────────────────────────
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (!(entity instanceof Player player)) return;
        if (level.isClientSide || remainingTicks % 5 != 0) return;

        HookahBlockEntity be = findClaimedHookah(player, level);
        if (be == null) { player.stopUsingItem(); return; }
        if (player.distanceToSqr(Vec3.atCenterOf(be.getBlockPos())) > be.getHoseType().getMaxLength() * be.getHoseType().getMaxLength()) {
            player.stopUsingItem();
            return;
        }
        if (!be.hasAllConsumables()) {
            if (player instanceof ServerPlayer sp)
                sp.displayClientMessage(Component.translatable("gui.hookahmod.fill_slots"), true);
            player.stopUsingItem();
        }
    }

    // ── Released early: exhale based on charge ───────────────────────
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!(entity instanceof Player player)) return;
        int held = MAX_CHARGE_TICKS - timeCharged;
        if (held < 5) return;
        float charge = Math.min(held, MAX_CHARGE_TICKS) / (float) MAX_CHARGE_TICKS;
        exhale(player, level, charge);
    }

    // ── Held full 5 seconds: max exhale ─────────────────────────────
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            exhale(player, level, 1.0f);
        }
        return stack;
    }

    // ── Core exhale logic ───────────────────────────────────────────
    private void exhale(Player player, Level level, float charge) {
        if (level.isClientSide) {
            int count = 4 + (int) (charge * 14);
            Vec3 look = player.getLookAngle();
            double eyeY = player.getY() + player.getEyeHeight() - 0.1;
            for (int i = 0; i < count; i++) {
                double dist = 0.6 + i * 0.06;
                double jx = (Math.random() - 0.5) * 0.15;
                double jy = (Math.random() - 0.5) * 0.15;
                double jz = (Math.random() - 0.5) * 0.15;
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        player.getX() + look.x * dist + jx,
                        eyeY          + look.y * dist + jy,
                        player.getZ() + look.z * dist + jz,
                        look.x * (0.02 + charge * 0.04),
                        0.015 + charge * 0.01,
                        look.z * (0.02 + charge * 0.04));
            }
            return;
        }

        if (!(player instanceof ServerPlayer sp)) return;
        HookahBlockEntity be = findClaimedHookah(player, level);
        if (be == null || !be.hasAllConsumables()) return;
        be.applyExhale(sp, charge);
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

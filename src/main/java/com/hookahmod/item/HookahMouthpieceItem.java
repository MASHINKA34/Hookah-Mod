package com.hookahmod.item;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.config.HookahConfig;
import com.hookahmod.event.ActiveSessions;
import com.hookahmod.registry.ModParticles;
import com.hookahmod.smoke.HookahSmoke;
import com.hookahmod.smoking.HookahHost;
import com.hookahmod.smoking.HookahSessions;
import com.hookahmod.smoking.ModAttachments;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;


public class HookahMouthpieceItem extends Item implements GeoItem {

    public static final int MAX_CHARGE_TICKS = 100;

    private static final RawAnimation SMOKING_ANIM =
            RawAnimation.begin().thenPlay("animation.hookah_mouthpiece.smoking");
    private static final RawAnimation RETURN_ANIM =
            RawAnimation.begin().thenPlay("animation.hookah_mouthpiece.return");

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
                        .triggerableAnim("return", RETURN_ANIM)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ── Use animation ────────────────────────────────────────────────
    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return MAX_CHARGE_TICKS; }

    private void triggerMouthpieceAnimation(Level level, Player player, ItemStack stack, String triggerName) {
        if (level instanceof ServerLevel serverLevel) {
            long animId = GeoItem.getOrAssignId(stack, serverLevel);
            this.triggerAnim(player, animId, "controller", triggerName);
        }
    }

    // ── Start using ─────────────────────────────────────────────────
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!isClaimedBy(player, level)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.hookahmod.claim_first"), true);
            }
            return InteractionResultHolder.fail(held);
        }
        if (!level.isClientSide) {
            HookahHost host = claimedHost(player, level);
            if (host == null) return InteractionResultHolder.fail(held);
            DrawBlocker blocker = blocker(host, player);
            if (blocker != null) {
                player.displayClientMessage(Component.translatable(blocker.messageKey), true);
                return InteractionResultHolder.fail(held);
            }
        }
        startSmoking(level, player, hand);
        return InteractionResultHolder.consume(held);
    }

    public void startSmoking(Level level, Player player, InteractionHand hand) {
        triggerMouthpieceAnimation(level, player, player.getItemInHand(hand), "smoking");
        player.startUsingItem(hand);
    }

    // ── While holding ───────────────────────────────────────────────
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (!(entity instanceof Player player)) return;
        if (level.isClientSide || remainingTicks % 5 != 0) return;

        HookahHost host = claimedHost(player, level);
        DrawBlocker blocker = host == null ? DrawBlocker.OUT_OF_REACH : blocker(host, player);
        if (blocker == null) return;

        // Walking out of reach speaks for itself; an empty bowl does not.
        if (blocker.announceWhileDrawing) {
            player.displayClientMessage(Component.translatable(blocker.messageKey), true);
        }
        triggerMouthpieceAnimation(level, player, stack, "return");
        player.stopUsingItem();
    }

    // ── Released early: exhale based on charge ───────────────────────
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!(entity instanceof Player player)) return;
        triggerMouthpieceAnimation(level, player, stack, "return");
        int held = MAX_CHARGE_TICKS - timeCharged;
        if (held < HookahConfig.minimumChargeTicks) return;
        float charge = Math.min(held, MAX_CHARGE_TICKS) / (float) MAX_CHARGE_TICKS;
        exhale(player, level, charge);
    }

    // ── Held full 5 seconds: max exhale ─────────────────────────────
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            exhale(player, level, 1.0f);
            triggerMouthpieceAnimation(level, player, stack, "return");
        }
        return stack;
    }

    // ── Core exhale logic ───────────────────────────────────────────
    private void exhale(Player player, Level level, float charge) {
        if (!consumeExhaleCooldown(player, level)) return;
        if (level.isClientSide) {
            int count = HookahSmoke.clientMouthPuffs(charge);
            float speed = HookahSmoke.clientMouthSpeed(charge);
            Vector3f smokeColor = clientSmokeColor(player, level);
            ParticleOptions particle = smokeColor == null
                    ? ParticleTypes.CAMPFIRE_COSY_SMOKE
                    : ColorParticleOption.create(ModParticles.COLORED_HOOKAH_SMOKE.get(), smokeColor.x(), smokeColor.y(), smokeColor.z());
            Vec3 look = player.getLookAngle();
            double eyeY = player.getY() + player.getEyeHeight() - 0.1;
            for (int i = 0; i < count; i++) {
                double dist = 0.6 + i * 0.06;
                double jx = (level.random.nextDouble() - 0.5) * 0.18;
                double jy = (level.random.nextDouble() - 0.5) * 0.18;
                double jz = (level.random.nextDouble() - 0.5) * 0.18;
                level.addParticle(particle,
                        player.getX() + look.x * dist + jx,
                        eyeY          + look.y * dist + jy,
                        player.getZ() + look.z * dist + jz,
                        look.x * speed,
                        0.012 + HookahSmoke.chargeStrength(charge) * 0.012,
                        look.z * speed);
            }
            return;
        }

        if (!(player instanceof ServerPlayer sp)) return;
        HookahHost host = claimedHost(player, level);
        if (host != null) HookahSessions.exhale(sp, host, charge);
    }

    private static boolean consumeExhaleCooldown(Player player, Level level) {
        int cooldown = HookahConfig.exhaleCooldownTicks;
        if (cooldown <= 0) return true;
        long now = level.getGameTime();
        long last = player.getData(ModAttachments.LAST_EXHALE_TICK.get());
        if (last != Long.MIN_VALUE && now >= last && now - last < cooldown) return false;
        player.setData(ModAttachments.LAST_EXHALE_TICK.get(), now);
        return true;
    }

    /**
     * The mouthpiece treats a hookah on the floor and one on a back the same
     * way; only resolving the claimed hookah differs.
     */
    @Nullable
    private static HookahHost claimedHost(Player player, Level level) {
        HookahBlockEntity be = findClaimedHookah(player, level);
        if (be != null) return be;

        Player wearer = WornHookah.findClaimedWearer(player, level);
        ItemStack worn = WornHookah.findClaimedStack(player, level);
        if (wearer instanceof ServerPlayer serverWearer && !worn.isEmpty()) {
            return new WornHookahHost(serverWearer, worn);
        }
        return null;
    }

    private static boolean isClaimedBy(Player player, Level level) {
        ActiveSessions sessions = ActiveSessions.of(level);
        return sessions.get(player.getUUID()) != null || sessions.getWornWearer(player.getUUID()) != null;
    }

    /** What stops the draw, or null when it may go on. */
    @Nullable
    private static DrawBlocker blocker(HookahHost host, Player player) {
        if (!host.inRange(player)) return DrawBlocker.OUT_OF_REACH;
        if (!host.hasAllConsumables()) return DrawBlocker.EMPTY_BOWL;
        return null;
    }

    private enum DrawBlocker {
        OUT_OF_REACH("message.hookahmod.slipped", false),
        EMPTY_BOWL("gui.hookahmod.fill_slots", true);

        private final String messageKey;
        private final boolean announceWhileDrawing;

        DrawBlocker(String messageKey, boolean announceWhileDrawing) {
            this.messageKey = messageKey;
            this.announceWhileDrawing = announceWhileDrawing;
        }
    }

    @Nullable
    private static Vector3f clientSmokeColor(Player player, Level level) {
        HookahBlockEntity be = findClaimedHookah(player, level);
        if (be != null) {
            ItemStack tobacco = be.getInventory().getItem(HookahBlockEntity.SLOT_TOBACCO);
            return tobacco.getItem() instanceof AbstractTobaccoItem item ? item.smokeColor() : null;
        }

        ItemStack wornHookah = WornHookah.findClaimedStack(player, level);
        if (!wornHookah.isEmpty()) {
            ItemStack tobacco = WornHookah.getItems(wornHookah).get(HookahBlockEntity.SLOT_TOBACCO);
            return tobacco.getItem() instanceof AbstractTobaccoItem item ? item.smokeColor() : null;
        }
        return null;
    }

    public static HookahBlockEntity findClaimedHookah(Player player, Level level) {
        GlobalPos gp = ActiveSessions.of(level).get(player.getUUID());
        if (gp == null || !gp.dimension().equals(level.dimension())) return null;
        BlockEntity be = level.getBlockEntity(gp.pos());
        if (be instanceof HookahBlockEntity hbe && player.getUUID().equals(hbe.getActivePlayerUuid())) {
            return hbe;
        }
        return null;
    }
}

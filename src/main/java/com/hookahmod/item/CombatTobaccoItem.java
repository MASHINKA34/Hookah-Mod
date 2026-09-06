package com.hookahmod.item;

import com.hookahmod.combat.SmokeCone;
import com.hookahmod.combat.SmokeBlockChanges;
import com.hookahmod.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class CombatTobaccoItem extends AbstractTobaccoItem {

    private final CombatType type;

    public CombatTobaccoItem(Properties properties, CombatType type) {
        super(properties, TobaccoCategory.COMBAT, 4);
        this.type = type;
    }

    @Override
    public void onExhale(ServerLevel level, ServerPlayer smoker, float charge, float effectMult, float combatMult) {
        double range = 4.0 + combatMult * 1.5 + Mth.clamp(charge, 0.0f, 1.0f) * 2.0;
        spawnConeParticles(level, smoker, range, type.color());
        if (type == CombatType.HEAL) {
            applyHeal(level, smoker, range, combatMult);
        } else {
            applyHostile(level, smoker, range, combatMult);
            if (type == CombatType.FIRE) igniteLookTarget(level, smoker, range);
            if (type == CombatType.ICE) freezeWaterLookTarget(level, smoker, range, combatMult);
        }
    }

    @Override
    public Vector3f smokeColor() {
        return type.color();
    }

    private void applyHostile(ServerLevel level, ServerPlayer smoker, double range, float combatMult) {
        SmokeCone.applyCone(level, smoker, range, 25.0,
                target -> target != smoker && !target.isAlliedTo(smoker)
                        && (!(target instanceof Player player) || smoker.canHarmPlayer(player)), target -> {
            switch (type) {
                case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, seconds((2.0f + combatMult) * combatMult), 0, true, true, true), smoker);
                case FIRE -> {
                    int fireTicks = seconds(3.0f + combatMult);
                    if (target.hurt(new DamageSource(level.damageSources().onFire().typeHolder(), smoker), 0.75f + combatMult * 0.5f)) {
                        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), fireTicks));
                    }
                }
                case ICE -> {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, seconds(2.5f * combatMult), 1, true, true, true), smoker);
                    target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, seconds(2.5f * combatMult), 0, true, true, true), smoker);
                    target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze(), target.getTicksFrozen() + seconds(1.5f * combatMult)));
                }
                default -> {}
            }
        });
    }

    private void applyHeal(ServerLevel level, ServerPlayer smoker, double range, float combatMult) {
        smoker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, seconds(3.0f * combatMult), 1, true, true, true), smoker);
        smoker.heal(1.0f + combatMult);
        SmokeCone.applyCone(level, smoker, range, 25.0, target -> target != smoker && isFriendly(target), target -> {
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, seconds(3.0f * combatMult), 1, true, true, true), smoker);
            target.heal(0.5f + combatMult);
        });
    }

    private static boolean isFriendly(LivingEntity target) {
        if (target instanceof Player) return true;
        if (target instanceof Villager) return true;
        return target instanceof TamableAnimal animal && animal.isTame();
    }

    private static int seconds(float seconds) {
        return Math.max(20, Math.round(seconds * 20.0f));
    }

    private static void igniteLookTarget(ServerLevel level, ServerPlayer smoker, double range) {
        Vec3 start = smoker.getEyePosition();
        Vec3 end = start.add(smoker.getLookAngle().scale(range));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, smoker));
        if (hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos hitPos = hit.getBlockPos();
        Direction face = hit.getDirection();
        if (!SmokeBlockChanges.canChange(level, smoker, hitPos)) return;
        BlockState hitState = level.getBlockState(hitPos);
        if (hitState.getBlock() instanceof TntBlock) {
            if (SmokeBlockChanges.remove(level, smoker, hitPos)) {
                hitState.onCaughtFire(level, hitPos, face, smoker);
            }
            return;
        }

        BlockPos firePos = hitPos.relative(face);
        if (!SmokeBlockChanges.canChange(level, smoker, firePos)) return;
        if (!level.getBlockState(firePos).canBeReplaced()) return;
        BlockState fireState = BaseFireBlock.getState(level, firePos);
        if (fireState.canSurvive(level, firePos)) {
            SmokeBlockChanges.place(level, smoker, firePos, fireState, face);
        }
    }

    private static void freezeWaterLookTarget(ServerLevel level, ServerPlayer smoker, double range, float combatMult) {
        if (level.dimensionType().ultraWarm()) return;
        Vec3 start = smoker.getEyePosition();
        Vec3 end = start.add(smoker.getLookAngle().scale(range));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.WATER, smoker));
        if (hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos center = hit.getBlockPos();
        int radius = combatMult >= 1.5f ? 2 : 1;
        BlockPos.betweenClosed(center.offset(-radius, 0, -radius), center.offset(radius, 0, radius)).forEach(pos -> {
            if (Math.abs(pos.getX() - center.getX()) + Math.abs(pos.getZ() - center.getZ()) > radius + 1) return;
            if (!SmokeBlockChanges.canChange(level, smoker, pos)) return;
            if (!level.getFluidState(pos).is(FluidTags.WATER) || !level.getFluidState(pos).isSource()) return;
            if (!level.getBlockState(pos).is(Blocks.WATER)) return;
            SmokeBlockChanges.place(level, smoker, pos.immutable(), Blocks.ICE.defaultBlockState(), hit.getDirection());
        });
    }

    private static void spawnConeParticles(ServerLevel level, ServerPlayer smoker, double range, Vector3f color) {
        ColorParticleOption smoke = ColorParticleOption.create(ModParticles.COLORED_HOOKAH_SMOKE.get(), color.x(), color.y(), color.z());
        for (int i = 1; i <= 7; i++) {
            double step = range * i / 7.0;
            double spread = 0.12 + step * 0.07;
            var pos = smoker.getEyePosition().add(smoker.getLookAngle().scale(step));
            level.sendParticles(smoke, pos.x, pos.y - 0.08, pos.z, 8, spread, spread * 0.5, spread, 0.015);
        }
    }

    public enum CombatType {
        POISON(new Vector3f(0.36f, 0.95f, 0.22f)),
        FIRE(new Vector3f(1.0f, 0.28f, 0.08f)),
        ICE(new Vector3f(0.55f, 0.9f, 1.0f)),
        HEAL(new Vector3f(1.0f, 0.58f, 0.78f));

        private final Vector3f color;

        CombatType(Vector3f color) {
            this.color = color;
        }

        public Vector3f color() {
            return color;
        }
    }
}

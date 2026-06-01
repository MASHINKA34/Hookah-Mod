package com.hookahmod.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class SmokeCone {

    private SmokeCone() {}

    public static void applyCone(ServerLevel level, ServerPlayer smoker, double range, double halfAngleDeg,
                                 Predicate<LivingEntity> filter, Consumer<LivingEntity> effect) {
        Vec3 origin = smoker.getEyePosition();
        Vec3 look = smoker.getLookAngle().normalize();
        double minDot = Math.cos(Math.toRadians(halfAngleDeg));
        AABB area = smoker.getBoundingBox().inflate(range);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity.isAlive() && filter.test(entity))) {
            Vec3 targetPoint = target.getEyePosition();
            Vec3 delta = targetPoint.subtract(origin);
            double distance = delta.length();
            if (distance <= 0.01 || distance > range) continue;
            if (look.dot(delta.normalize()) < minDot) continue;
            if (!smoker.hasLineOfSight(target)) continue;
            effect.accept(target);
        }
    }
}

package com.hookahmod.smoking;

import com.hookahmod.registry.ModItems;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class MouthpiecePosition {
    private MouthpiecePosition() {}

    public static Vec3 hand(Player player, float partialTick) {
        InteractionHand hand = player.isUsingItem() && player.getUseItem().is(ModItems.HOOKAH_MOUTHPIECE)
                ? player.getUsedItemHand()
                : !player.getMainHandItem().is(ModItems.HOOKAH_MOUTHPIECE)
                && player.getOffhandItem().is(ModItems.HOOKAH_MOUTHPIECE)
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        double side = arm == HumanoidArm.RIGHT ? -0.35 : 0.35;
        Vec3 offset = new Vec3(side, player.getEyeHeight() * 0.62, 0.2).yRot(-yaw);
        return player.getPosition(partialTick).add(offset);
    }
}

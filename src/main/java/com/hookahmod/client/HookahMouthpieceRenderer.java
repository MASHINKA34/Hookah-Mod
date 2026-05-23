package com.hookahmod.client;

import com.hookahmod.HookahMod;
import com.hookahmod.item.HookahMouthpieceItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HookahMouthpieceRenderer extends GeoItemRenderer<HookahMouthpieceItem> {

    public HookahMouthpieceRenderer() {
        super(new DefaultedItemGeoModel<>(HookahMod.id("hookah_mouthpiece")));
    }

    @Override
    public void actuallyRender(PoseStack poseStack, HookahMouthpieceItem animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (!isReRender && this.renderPerspective == ItemDisplayContext.GUI) {
            resetPose(model);
            super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                    true, partialTick, packedLight, packedOverlay, colour);
            return;
        }

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    // GeckoLib does not read the Blockbench "Display" tab the way vanilla
    // item/generated items do. Apply the Display-tab values from
    // hookah_mouthpiece.bbmodel here, mirroring vanilla ItemTransform order
    // (translate -> rotate XYZ -> scale). Values are in 1/16-block units for
    // translations, degrees for rotations, dimensionless for scale.
    @Override
    public void preRender(PoseStack poseStack, HookahMouthpieceItem animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
        if (isReRender) return;

        ItemDisplayContext ctx = this.renderPerspective;
        if (ctx == null) return;

        switch (ctx) {
            case GUI -> {
                translate(poseStack, 0.25f, 0.25f, 0f);
                rotateXYZ(poseStack, 35f, 74f, 0f);
                poseStack.scale(0.99f, 1.0f, 1.0f);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
                rotateXYZ(poseStack, -20f, 0f, 0f);
            case FIRST_PERSON_RIGHT_HAND ->
                translate(poseStack, 0f, 0.25f, 1f);
            case FIRST_PERSON_LEFT_HAND ->
                translate(poseStack, 0f, 0f, 0.5f);
            case HEAD -> {
                translate(poseStack, 0f, 7.25f, 0f);
                rotateXYZ(poseStack, 19f, 0f, 0f);
            }
            case FIXED ->
                rotateXYZ(poseStack, 43f, 95f, 4f);
            default -> { /* GROUND / NONE / FIRST_PERSON_*_HAND fallbacks: vanilla GeckoLib defaults */ }
        }
    }

    private static void translate(PoseStack ps, float x, float y, float z) {
        ps.translate(x / 16f, y / 16f, z / 16f);
    }

    private static void rotateXYZ(PoseStack ps, float x, float y, float z) {
        if (x != 0f) ps.mulPose(Axis.XP.rotationDegrees(x));
        if (y != 0f) ps.mulPose(Axis.YP.rotationDegrees(y));
        if (z != 0f) ps.mulPose(Axis.ZP.rotationDegrees(z));
    }

    private static void resetPose(BakedGeoModel model) {
        for (GeoBone bone : model.topLevelBones()) {
            resetPose(bone);
        }
    }

    private static void resetPose(GeoBone bone) {
        BoneSnapshot initial = bone.getInitialSnapshot();
        if (initial != null) {
            bone.updateRotation(initial.getRotX(), initial.getRotY(), initial.getRotZ());
            bone.updatePosition(initial.getOffsetX(), initial.getOffsetY(), initial.getOffsetZ());
            bone.updateScale(initial.getScaleX(), initial.getScaleY(), initial.getScaleZ());
        } else {
            bone.updateRotation(0f, 0f, 0f);
            bone.updatePosition(0f, 0f, 0f);
            bone.updateScale(1f, 1f, 1f);
        }

        for (GeoBone child : bone.getChildBones()) {
            resetPose(child);
        }
    }
}

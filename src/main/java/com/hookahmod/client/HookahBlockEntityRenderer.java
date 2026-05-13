package com.hookahmod.client;

import com.hookahmod.block.HookahBlock;
import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.item.HookahHoseType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.UUID;

public class HookahBlockEntityRenderer implements BlockEntityRenderer<HookahBlockEntity> {

    private static final int SEGMENTS = 28;
    private static final float HOSE_THICKNESS = 0.05F;
    private static final float MAX_SAG = 1.1F;
    private static final ResourceLocation HOSE_TEX = ResourceLocation.fromNamespaceAndPath("hookahmod", "textures/entity/hookah_hose.png");

    public HookahBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(HookahBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        HookahHoseType type = be.getHoseType();
        if (type == HookahHoseType.NONE) return;

        BlockState state = be.getBlockState();
        Direction facing = state.hasProperty(HookahBlock.FACING) ? state.getValue(HookahBlock.FACING) : Direction.NORTH;
        BlockPos pos = be.getBlockPos();
        Vec3 connectorLocal = rotateLocal(new Vec3(0.3125, 0.96875, 0.5), facing);

        UUID active = be.getActivePlayerUuid();
        Level level = be.getLevel();
        if (level == null) return;

        VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(HOSE_TEX));

        if (active != null && level.getPlayerByUUID(active) instanceof AbstractClientPlayer player) {
            renderActiveHose(pose, vc, connectorLocal, pos, player, partialTick, type, packedLight);
        } else {
            renderIdleHose(pose, vc, connectorLocal, facing, packedLight);
        }
    }

    private void renderActiveHose(PoseStack pose, VertexConsumer vc, Vec3 connectorLocal, BlockPos pos,
                                  AbstractClientPlayer player, float partialTick, HookahHoseType type, int packedLight) {
        Vec3 worldConnector = Vec3.atLowerCornerOf(pos).add(connectorLocal);
        Vec3 hand = getPlayerHandPoint(player, partialTick);
        double distance = worldConnector.distanceTo(hand);
        int maxLength = Math.max(1, type.getMaxLength());
        float tension = (float) Mth.clamp(distance / maxLength, 0.0, 1.0);
        float sag = MAX_SAG * (1.0F - tension);

        Vec3 p0 = connectorLocal;
        Vec3 p3 = hand.subtract(Vec3.atLowerCornerOf(pos));
        Vec3 p1 = p0.add(0, -sag * 0.5, 0);
        Vec3 p2 = p3.add(0, -sag * 0.5, 0);

        pose.pushPose();
        drawBezier(pose, vc, p0, p1, p2, p3, packedLight);
        pose.popPose();
    }

    private void renderIdleHose(PoseStack pose, VertexConsumer vc, Vec3 connectorLocal, Direction facing, int packedLight) {
        Direction outward = facing.getCounterClockWise();
        Vec3 outDir = Vec3.atLowerCornerOf(outward.getNormal());
        Vec3 p0 = connectorLocal;
        Vec3 outEnd = p0.add(outDir.scale(0.55));
        Vec3 p3 = new Vec3(outEnd.x, 0.06, outEnd.z);
        Vec3 p1 = p0.add(outDir.scale(0.45)).add(0, -0.15, 0);
        Vec3 p2 = p3.add(0, 0.25, 0).add(outDir.scale(-0.15));
        pose.pushPose();
        drawBezier(pose, vc, p0, p1, p2, p3, packedLight);
        drawCap(pose, vc, p3, packedLight);
        pose.popPose();
    }

    private static void drawBezier(PoseStack pose, VertexConsumer vc, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int light) {
        Vec3 prev = p0;
        for (int i = 1; i <= SEGMENTS; i++) {
            float t = (float) i / SEGMENTS;
            Vec3 cur = bezier(p0, p1, p2, p3, t);
            drawSegment(pose, vc, prev, cur, light);
            prev = cur;
        }
    }

    private static Vec3 rotateLocal(Vec3 v, Direction facing) {
        double cx = v.x - 0.5, cz = v.z - 0.5;
        return switch (facing) {
            case EAST -> new Vec3(0.5 - cz, v.y, 0.5 + cx);
            case SOUTH -> new Vec3(0.5 - cx, v.y, 0.5 - cz);
            case WEST -> new Vec3(0.5 + cz, v.y, 0.5 - cx);
            default -> v;
        };
    }

    private static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, Vec3 d, float t) {
        float omt = 1.0F - t;
        double x = omt * omt * omt * a.x + 3 * omt * omt * t * b.x + 3 * omt * t * t * c.x + t * t * t * d.x;
        double y = omt * omt * omt * a.y + 3 * omt * omt * t * b.y + 3 * omt * t * t * c.y + t * t * t * d.y;
        double z = omt * omt * omt * a.z + 3 * omt * omt * t * b.z + 3 * omt * t * t * c.z + t * t * t * d.z;
        return new Vec3(x, y, z);
    }

    private static void drawSegment(PoseStack pose, VertexConsumer vc, Vec3 a, Vec3 b, int light) {
        Matrix4f mat = pose.last().pose();
        float th = HOSE_THICKNESS;
        Vec3 dir = b.subtract(a);
        Vec3 up = Math.abs(dir.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 nd = dir.normalize();
        Vec3 side = nd.cross(up).normalize().scale(th);
        Vec3 vert = nd.cross(side).normalize().scale(th);

        Vec3 a1 = a.add(side).add(vert);
        Vec3 a2 = a.add(side).subtract(vert);
        Vec3 a3 = a.subtract(side).subtract(vert);
        Vec3 a4 = a.subtract(side).add(vert);
        Vec3 b1 = b.add(side).add(vert);
        Vec3 b2 = b.add(side).subtract(vert);
        Vec3 b3 = b.subtract(side).subtract(vert);
        Vec3 b4 = b.subtract(side).add(vert);

        quad(mat, vc, a1, a2, b2, b1, light);
        quad(mat, vc, a2, a3, b3, b2, light);
        quad(mat, vc, a3, a4, b4, b3, light);
        quad(mat, vc, a4, a1, b1, b4, light);
    }

    private static void drawCap(PoseStack pose, VertexConsumer vc, Vec3 c, int light) {
        Matrix4f mat = pose.last().pose();
        float t = HOSE_THICKNESS * 1.6F;
        Vec3 a1 = c.add(-t, 0, -t);
        Vec3 a2 = c.add(t, 0, -t);
        Vec3 a3 = c.add(t, 0, t);
        Vec3 a4 = c.add(-t, 0, t);
        quad(mat, vc, a1, a2, a3, a4, light);
    }

    private static void quad(Matrix4f mat, VertexConsumer vc, Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4, int light) {
        vert(mat, vc, v1, 0, 0, light);
        vert(mat, vc, v2, 0, 1, light);
        vert(mat, vc, v3, 1, 1, light);
        vert(mat, vc, v4, 1, 0, light);
    }

    private static void vert(Matrix4f mat, VertexConsumer vc, Vec3 v, float u, float vTex, int light) {
        vc.addVertex(mat, (float) v.x, (float) v.y, (float) v.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, vTex)
                .setOverlay(0)
                .setLight(light)
                .setNormal(0, 1, 0);
    }

    private static Vec3 getPlayerHandPoint(AbstractClientPlayer player, float partialTick) {
        double x = Mth.lerp(partialTick, player.xo, player.getX());
        double y = Mth.lerp(partialTick, player.yo, player.getY()) + player.getEyeHeight() * 0.5;
        double z = Mth.lerp(partialTick, player.zo, player.getZ());
        float yawRad = (float) Math.toRadians(Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) + 90);
        double offX = Math.cos(yawRad) * 0.3;
        double offZ = Math.sin(yawRad) * 0.3;
        return new Vec3(x + offX, y, z + offZ);
    }

    @Override
    public boolean shouldRenderOffScreen(HookahBlockEntity be) { return true; }

    @Override
    public int getViewDistance() { return 64; }
}

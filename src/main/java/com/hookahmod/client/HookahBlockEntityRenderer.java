package com.hookahmod.client;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.item.HookahHoseType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.UUID;

public class HookahBlockEntityRenderer implements BlockEntityRenderer<HookahBlockEntity> {

    private static final int SEGMENTS = 24;
    private static final float HOSE_THICKNESS = 0.04F;
    private static final float MAX_SAG = 1.25F;
    private static final int HOSE_COLOR = 0xFF5A3A1F;

    public HookahBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(HookahBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        UUID active = be.getActivePlayerUuid();
        if (active == null || be.getHoseType() == HookahHoseType.NONE) return;
        Level level = be.getLevel();
        if (level == null) return;
        var entity = level.getPlayerByUUID(active);
        if (!(entity instanceof AbstractClientPlayer player)) return;

        BlockPos pos = be.getBlockPos();
        Vec3 hookahOut = Vec3.atLowerCornerOf(pos).add(0.3125, 0.9375, 0.5);
        Vec3 handPoint = getPlayerHandPoint(player, partialTick);
        Vec3 local = handPoint.subtract(Vec3.atLowerCornerOf(pos));

        pose.pushPose();
        pose.translate(hookahOut.x - pos.getX(), hookahOut.y - pos.getY(), hookahOut.z - pos.getZ());

        double distance = hookahOut.distanceTo(handPoint);
        int maxLength = Math.max(1, be.getHoseType().getMaxLength());
        float tension = (float) Mth.clamp(distance / maxLength, 0.0, 1.0);
        float sag = MAX_SAG * (1.0F - tension);

        Vec3 p0 = Vec3.ZERO;
        Vec3 p3 = local.subtract(hookahOut.x - pos.getX(), hookahOut.y - pos.getY(), hookahOut.z - pos.getZ());
        Vec3 p1 = p0.add(0, -sag, 0);
        Vec3 p2 = p3.add(0, -sag, 0);

        VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/block/dirt.png")));
        int light = LightTexture.FULL_BRIGHT;

        Vec3 prev = p0;
        for (int i = 1; i <= SEGMENTS; i++) {
            float t = (float) i / SEGMENTS;
            Vec3 cur = bezier(p0, p1, p2, p3, t);
            drawSegment(pose, vc, prev, cur, light);
            prev = cur;
        }
        pose.popPose();
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
        float t = HOSE_THICKNESS;
        int r = (HOSE_COLOR >> 16) & 0xFF;
        int g = (HOSE_COLOR >> 8) & 0xFF;
        int bl = HOSE_COLOR & 0xFF;
        int alpha = 0xFF;
        // crude segmented quad strip: 4 quads forming a square tube
        Vec3 dir = b.subtract(a);
        Vec3 up = Math.abs(dir.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 side = dir.normalize().cross(up).normalize().scale(t);
        Vec3 vert = dir.normalize().cross(side).normalize().scale(t);

        Vec3 a1 = a.add(side).add(vert);
        Vec3 a2 = a.add(side).subtract(vert);
        Vec3 a3 = a.subtract(side).subtract(vert);
        Vec3 a4 = a.subtract(side).add(vert);
        Vec3 b1 = b.add(side).add(vert);
        Vec3 b2 = b.add(side).subtract(vert);
        Vec3 b3 = b.subtract(side).subtract(vert);
        Vec3 b4 = b.subtract(side).add(vert);

        quad(mat, vc, a1, a2, b2, b1, r, g, bl, alpha, light);
        quad(mat, vc, a2, a3, b3, b2, r, g, bl, alpha, light);
        quad(mat, vc, a3, a4, b4, b3, r, g, bl, alpha, light);
        quad(mat, vc, a4, a1, b1, b4, r, g, bl, alpha, light);
    }

    private static void quad(Matrix4f mat, VertexConsumer vc, Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4,
                             int r, int g, int b, int a, int light) {
        vert(mat, vc, v1, r, g, b, a, 0, 0, light);
        vert(mat, vc, v2, r, g, b, a, 0, 1, light);
        vert(mat, vc, v3, r, g, b, a, 1, 1, light);
        vert(mat, vc, v4, r, g, b, a, 1, 0, light);
    }

    private static void vert(Matrix4f mat, VertexConsumer vc, Vec3 v, int r, int g, int b, int a,
                             float u, float vTex, int light) {
        vc.addVertex(mat, (float) v.x, (float) v.y, (float) v.z)
                .setColor(r, g, b, a)
                .setUv(u, vTex)
                .setOverlay(0)
                .setLight(light)
                .setNormal(0, 1, 0);
    }

    private static Vec3 getPlayerHandPoint(AbstractClientPlayer player, float partialTick) {
        double x = Mth.lerp(partialTick, player.xo, player.getX());
        double y = Mth.lerp(partialTick, player.yo, player.getY()) + player.getEyeHeight() * 0.55;
        double z = Mth.lerp(partialTick, player.zo, player.getZ());
        float yawRad = (float) Math.toRadians(Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot));
        double offX = -Math.cos(yawRad) * 0.35;
        double offZ = -Math.sin(yawRad) * 0.35;
        return new Vec3(x + offX, y, z + offZ);
    }

    @Override
    public boolean shouldRenderOffScreen(HookahBlockEntity be) { return true; }

    @Override
    public int getViewDistance() { return 64; }
}

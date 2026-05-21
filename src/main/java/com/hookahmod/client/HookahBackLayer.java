package com.hookahmod.client;

import com.hookahmod.HookahMod;
import com.hookahmod.item.HookahHoseType;
import com.hookahmod.item.WornHookah;
import com.hookahmod.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.UUID;

public class HookahBackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final int SEGMENTS = 28;
    private static final int SIDES = 8;
    private static final float HOSE_THICKNESS = 0.04F;
    private static final ResourceLocation HOSE_TEX = HookahMod.id("textures/entity/hookah_hose.png");
    private static final ResourceLocation COAL_TEX = HookahMod.id("textures/entity/hookah_charcoal_cube.png");

    private static final float BACK_SCALE = 0.58F;
    private static final float BACK_X = -BACK_SCALE * 0.5F;
    private static final float BACK_Y = 0.82F;
    private static final float BACK_Z = 0.54F;

    public HookahBackLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!WornHookah.isHookahStack(stack)) return;

        renderHookahBlockModel(pose, buffer, packedLight);

        if (WornHookah.hasCoal(stack)) {
            renderCoal(pose, buffer, packedLight, player);
            spawnCoalParticles(player, partialTick);
        }

        HookahHoseType hoseType = WornHookah.getHoseType(stack);
        if (hoseType.isPresent()) {
            renderHose(pose, buffer, packedLight, player, stack, partialTick);
        }
    }

    private void applyBackHookahTransform(PoseStack pose) {
        getParentModel().body.translateAndRotate(pose);
        pose.translate(BACK_X, BACK_Y, BACK_Z);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        pose.scale(BACK_SCALE, BACK_SCALE, BACK_SCALE);
    }

    private void renderHookahBlockModel(PoseStack pose, MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        applyBackHookahTransform(pose);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                ModBlocks.HOOKAH.get().defaultBlockState(),
                pose,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );
        pose.popPose();
    }

    private void renderCoal(PoseStack pose, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player) {
        float t = player.clientLevel.getGameTime();
        double pulse = 0.5 + 0.5 * Math.sin(t * 0.12);
        int blockLight = 11 + (int) (pulse * 4);
        int light = LightTexture.pack(blockLight, 15);

        int rTint = 255;
        int gTint = (int) (160 + (1.0 - pulse) * 80);
        int bTint = (int) (80 + (1.0 - pulse) * 120);

        VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(COAL_TEX));

        pose.pushPose();
        applyBackHookahTransform(pose);

        pose.pushPose();
        pose.translate(0.5, 1.572, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(28));
        pose.mulPose(Axis.XP.rotationDegrees(7));
        drawTintedCube(pose, vc, -0.105F, 0F, -0.08F, 0.105F, 0.055F, 0.08F,
                light, OverlayTexture.NO_OVERLAY, rTint, gTint, bTint, 255);
        pose.popPose();

        pose.pushPose();
        pose.translate(0.5, 1.618, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-38));
        pose.mulPose(Axis.XP.rotationDegrees(-9));
        drawTintedCube(pose, vc, -0.09F, 0F, -0.065F, 0.09F, 0.052F, 0.065F,
                light, OverlayTexture.NO_OVERLAY, rTint, gTint, bTint, 255);
        pose.popPose();

        pose.pushPose();
        pose.translate(0.5, 1.661, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(12));
        pose.mulPose(Axis.ZP.rotationDegrees(14));
        drawTintedCube(pose, vc, -0.072F, 0F, -0.06F, 0.072F, 0.046F, 0.06F,
                light, OverlayTexture.NO_OVERLAY, rTint, gTint, bTint, 255);
        pose.popPose();

        pose.popPose();
    }

    private void renderHose(PoseStack pose, MultiBufferSource buffer, int packedLight,
                            AbstractClientPlayer wearer, ItemStack hookahStack, float partialTick) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(HOSE_TEX));
        Vec3 p0 = new Vec3(0.3125, 0.96875, 0.5);
        Vec3 p3 = idleHoseEnd();

        UUID activeUuid = WornHookah.getActivePlayerUuid(hookahStack);
        if (activeUuid != null && wearer.level().getPlayerByUUID(activeUuid) instanceof AbstractClientPlayer active) {
            p3 = handPointInHookahSpace(wearer, active, partialTick);
        }

        Vec3 p1 = p0.add(-0.45, -0.15, 0.0);
        Vec3 p2 = p3.add(0.15, 0.25, 0.0);

        pose.pushPose();
        applyBackHookahTransform(pose);
        drawBezier(pose, vc, p0, p1, p2, p3, packedLight);
        pose.popPose();
    }

    private static Vec3 idleHoseEnd() {
        return new Vec3(-0.2375, 0.06, 0.5);
    }

    private static Vec3 handPointInHookahSpace(AbstractClientPlayer wearer, AbstractClientPlayer active, float partialTick) {
        Vec3 wearerPos = interpolatedPosition(wearer, partialTick);
        Vec3 hand = getPlayerHandPoint(active, partialTick);
        Vec3 delta = hand.subtract(wearerPos);

        float yaw = (float) Math.toRadians(Mth.lerp(partialTick, wearer.yBodyRotO, wearer.yBodyRot));
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);

        double localX = delta.x * cos + delta.z * sin;
        double localZ = -delta.x * sin + delta.z * cos;
        double localY = 0.75 - delta.y;
        return new Vec3(
                (localX - BACK_X) / BACK_SCALE,
                (BACK_Y - localY) / BACK_SCALE,
                (BACK_Z - localZ) / BACK_SCALE
        );
    }

    private static Vec3 interpolatedPosition(AbstractClientPlayer player, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, player.xo, player.getX()),
                Mth.lerp(partialTick, player.yo, player.getY()),
                Mth.lerp(partialTick, player.zo, player.getZ())
        );
    }

    private static Vec3 getPlayerHandPoint(AbstractClientPlayer player, float partialTick) {
        double x = Mth.lerp(partialTick, player.xo, player.getX());
        double y = Mth.lerp(partialTick, player.yo, player.getY()) + player.getEyeHeight() * 0.62;
        double z = Mth.lerp(partialTick, player.zo, player.getZ());
        float yawRad = (float) Math.toRadians(Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot));
        double rightX = Math.cos(yawRad) * 0.35;
        double rightZ = Math.sin(yawRad) * 0.35;
        double fwdX = -Math.sin(yawRad) * 0.2;
        double fwdZ = Math.cos(yawRad) * 0.2;
        return new Vec3(x + rightX + fwdX, y, z + rightZ + fwdZ);
    }

    private static void spawnCoalParticles(AbstractClientPlayer player, float partialTick) {
        if (player.clientLevel.random.nextInt(18) != 0) return;
        float yaw = (float) Math.toRadians(Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot));
        double px = Mth.lerp(partialTick, player.xo, player.getX()) + Math.sin(yaw) * 0.42;
        double py = Mth.lerp(partialTick, player.yo, player.getY()) + player.getBbHeight() * 0.92;
        double pz = Mth.lerp(partialTick, player.zo, player.getZ()) - Math.cos(yaw) * 0.42;
        player.clientLevel.addParticle(ParticleTypes.SMALL_FLAME,
                px + (player.clientLevel.random.nextDouble() - 0.5) * 0.12,
                py + player.clientLevel.random.nextDouble() * 0.08,
                pz + (player.clientLevel.random.nextDouble() - 0.5) * 0.12,
                0.0,
                0.015 + player.clientLevel.random.nextDouble() * 0.02,
                0.0);
    }

    private static void drawTintedCube(PoseStack pose, VertexConsumer vc,
                                       float minX, float minY, float minZ,
                                       float maxX, float maxY, float maxZ,
                                       int light, int overlay,
                                       int r, int g, int b, int a) {
        Matrix4f m = pose.last().pose();
        addVertex(m, vc, minX, maxY, minZ, 0, 0, 0, 1, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, maxY, maxZ, 0, 1, 0, 1, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, maxY, maxZ, 1, 1, 0, 1, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, maxY, minZ, 1, 0, 0, 1, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, minY, minZ, 0, 0, 0, -1, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, minY, minZ, 1, 0, 0, -1, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, minY, maxZ, 1, 1, 0, -1, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, minY, maxZ, 0, 1, 0, -1, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, minY, minZ, 0, 1, 0, 0, -1, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, maxY, minZ, 0, 0, 0, 0, -1, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, maxY, minZ, 1, 0, 0, 0, -1, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, minY, minZ, 1, 1, 0, 0, -1, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, minY, maxZ, 0, 1, 0, 0, 1, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, minY, maxZ, 1, 1, 0, 0, 1, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, maxY, maxZ, 1, 0, 0, 0, 1, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, maxY, maxZ, 0, 0, 0, 0, 1, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, minY, minZ, 0, 1, 1, 0, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, maxY, minZ, 0, 0, 1, 0, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, maxY, maxZ, 1, 0, 1, 0, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, minY, maxZ, 1, 1, 1, 0, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, minY, minZ, 0, 1, -1, 0, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, minY, maxZ, 1, 1, -1, 0, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, maxY, maxZ, 1, 0, -1, 0, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, maxY, minZ, 0, 0, -1, 0, 0, light, overlay, r, g, b, a);
    }

    private static void addVertex(Matrix4f mat, VertexConsumer vc,
                                  float x, float y, float z,
                                  float u, float v,
                                  float nx, float ny, float nz,
                                  int light, int overlay,
                                  int r, int g, int b, int a) {
        vc.addVertex(mat, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }

    private static void drawBezier(PoseStack pose, VertexConsumer vc, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int light) {
        Vec3 prev = p0;
        for (int i = 1; i <= SEGMENTS; i++) {
            float t = (float) i / SEGMENTS;
            Vec3 cur = bezier(p0, p1, p2, p3, t);
            float vOff = ((i - 1) * 0.25F) % 1.0F;
            drawSegment(pose, vc, prev, cur, vOff, light);
            prev = cur;
        }
    }

    private static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, Vec3 d, float t) {
        float omt = 1.0F - t;
        double x = omt * omt * omt * a.x + 3 * omt * omt * t * b.x + 3 * omt * t * t * c.x + t * t * t * d.x;
        double y = omt * omt * omt * a.y + 3 * omt * omt * t * b.y + 3 * omt * t * t * c.y + t * t * t * d.y;
        double z = omt * omt * omt * a.z + 3 * omt * omt * t * b.z + 3 * omt * t * t * c.z + t * t * t * d.z;
        return new Vec3(x, y, z);
    }

    private static void drawSegment(PoseStack pose, VertexConsumer vc, Vec3 a, Vec3 b, float vOff, int light) {
        Matrix4f mat = pose.last().pose();
        Vec3 dir = b.subtract(a);
        if (dir.lengthSqr() < 1.0E-6) return;
        Vec3 worldUp = Math.abs(dir.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 nd = dir.normalize();
        Vec3 right = nd.cross(worldUp).normalize().scale(HOSE_THICKNESS);
        Vec3 up = right.cross(nd).normalize().scale(HOSE_THICKNESS);

        Vec3[] aRing = new Vec3[SIDES];
        Vec3[] bRing = new Vec3[SIDES];
        for (int i = 0; i < SIDES; i++) {
            double angle = 2 * Math.PI * i / SIDES;
            Vec3 offset = right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            aRing[i] = a.add(offset);
            bRing[i] = b.add(offset);
        }

        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;
            float u0 = (float) i / SIDES;
            float u1 = (float) (i + 1) / SIDES;
            quadUV(mat, vc, aRing[i], aRing[next], bRing[next], bRing[i], u0, u1, vOff, vOff + 0.25F, light);
        }
    }

    private static void quadUV(Matrix4f mat, VertexConsumer vc, Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4,
                               float u0, float u1, float v0, float v1f, int light) {
        vertHose(mat, vc, v1, u0, v0, light);
        vertHose(mat, vc, v2, u1, v0, light);
        vertHose(mat, vc, v3, u1, v1f, light);
        vertHose(mat, vc, v4, u0, v1f, light);
    }

    private static void vertHose(Matrix4f mat, VertexConsumer vc, Vec3 v, float u, float vTex, int light) {
        vc.addVertex(mat, (float) v.x, (float) v.y, (float) v.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, vTex)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 1, 0);
    }
}

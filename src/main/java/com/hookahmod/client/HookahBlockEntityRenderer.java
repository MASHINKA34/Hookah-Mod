package com.hookahmod.client;

import com.hookahmod.HookahMod;
import com.hookahmod.block.HookahBlock;
import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.item.HookahHoseType;
import com.hookahmod.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

import java.util.UUID;

public class HookahBlockEntityRenderer implements BlockEntityRenderer<HookahBlockEntity> {

    private static final int SEGMENTS = 40;
    private static final int SIDES = 8;
    private static final float HOSE_THICKNESS = 0.065F;
    private static final float MAX_SAG = 1.1F;
    private static final ResourceLocation HOSE_TEX = HookahMod.id("textures/entity/hookah_hose.png");
    private static final ResourceLocation COAL_TEX = HookahMod.id("textures/entity/hookah_charcoal_cube.png");
    private static final ResourceLocation WATER_TEX = HookahMod.id("textures/entity/water_fill.png");

    public HookahBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(HookahBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        Direction facing = state.hasProperty(HookahBlock.FACING) ? state.getValue(HookahBlock.FACING) : Direction.NORTH;
        Level level = be.getLevel();

        // The preview OBJ already contains its decorative hose, water and
        // coals. Avoid drawing the legacy dynamic overlays on top of it.
        if (state.is(ModBlocks.LUXURY_HOOKAH_PREVIEW.get())) return;

        // Hose render
        HookahHoseType type = be.getHoseType();
        if (type != HookahHoseType.NONE) {
            BlockPos pos = be.getBlockPos();
            Vec3 connectorLocal = rotateLocal(new Vec3(0.3125, 0.96875, 0.5), facing);
            UUID active = be.getActivePlayerUuid();
            VertexConsumer hvc = buffer.getBuffer(RenderType.entitySolid(HOSE_TEX));
            if (level != null && active != null && level.getPlayerByUUID(active) instanceof AbstractClientPlayer player) {
                renderActiveHose(pose, hvc, connectorLocal, pos, player, partialTick, type, packedLight);
            } else {
                renderIdleHose(pose, hvc, connectorLocal, facing, packedLight);
            }
        }

        // Water in glass body
        ItemStack water = be.getInventory().getItem(HookahBlockEntity.SLOT_WATER);
        if (!water.isEmpty()) {
            renderWater(pose, buffer, packedLight, packedOverlay, level, partialTick);
        }

        // Tobacco (flat sprite on bowl)
        ItemStack tobacco = be.getInventory().getItem(HookahBlockEntity.SLOT_TOBACCO);
        if (!tobacco.isEmpty()) {
            renderTobaccoFlat(pose, buffer, packedLight, packedOverlay, tobacco);
        }

        // 3D charcoal cube on top with pulsing emissive
        ItemStack coal = be.getInventory().getItem(HookahBlockEntity.SLOT_COAL);
        if (!coal.isEmpty()) {
            renderCoalCube(pose, buffer, packedOverlay, level, partialTick);
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

    private void renderWater(PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay,
                             Level level, float partialTick) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(WATER_TEX));
        // subtle bob: animate water surface y over time
        float t = level == null ? 0 : (level.getGameTime() + partialTick);
        float wave = (float) (Math.sin(t * 0.08) * 0.005);
        float yMax = 0.32F + wave;
        // Cuboid inside glass body
        pose.pushPose();
        drawTintedCube(pose, vc,
                0.34F, 0.08F, 0.34F,
                0.66F, yMax,   0.66F,
                packedLight, packedOverlay,
                60, 140, 210, 180);
        pose.popPose();
    }

    private void renderTobaccoFlat(PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay, ItemStack tobacco) {
        String path = BuiltInRegistries.ITEM.getKey(tobacco.getItem()).getPath();
        VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(HookahMod.id("textures/item/" + path + ".png")));
        pose.pushPose();
        Matrix4f mat = pose.last().pose();
        // Flat disc at top of bowl (y ≈ 1.47)
        float y = 1.47F;
        float a = 0.36F, b = 0.64F;
        addVertex(mat, vc, a, y, a, 0, 0, 0, 1, 0, packedLight, packedOverlay, 255, 255, 255, 255);
        addVertex(mat, vc, a, y, b, 0, 1, 0, 1, 0, packedLight, packedOverlay, 255, 255, 255, 255);
        addVertex(mat, vc, b, y, b, 1, 1, 0, 1, 0, packedLight, packedOverlay, 255, 255, 255, 255);
        addVertex(mat, vc, b, y, a, 1, 0, 0, 1, 0, packedLight, packedOverlay, 255, 255, 255, 255);
        pose.popPose();
    }

    private void renderCoalCube(PoseStack pose, MultiBufferSource buffer, int packedOverlay,
                                Level level, float partialTick) {
        float t = level == null ? 0 : (level.getGameTime() + partialTick);
        double pulse = 0.5 + 0.5 * Math.sin(t * 0.12);
        int blockLight = 11 + (int) (pulse * 4);
        int packedLight = LightTexture.pack(blockLight, 15);

        int rTint = 255;
        int gTint = (int) (160 + (1.0 - pulse) * 80);
        int bTint = (int) (80  + (1.0 - pulse) * 120);

        VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(COAL_TEX));

        // Piece 1 — нижний крупный брикет, чуть повёрнут по Y и завален по X
        pose.pushPose();
        pose.translate(0.5, 1.572, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(28));
        pose.mulPose(Axis.XP.rotationDegrees(7));
        drawTintedCube(pose, vc, -0.105F, 0F, -0.08F, 0.105F, 0.055F, 0.08F,
                packedLight, packedOverlay, rTint, gTint, bTint, 255);
        pose.popPose();

        // Piece 2 — средний брикет, другой угол
        pose.pushPose();
        pose.translate(0.5, 1.618, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-38));
        pose.mulPose(Axis.XP.rotationDegrees(-9));
        drawTintedCube(pose, vc, -0.09F, 0F, -0.065F, 0.09F, 0.052F, 0.065F,
                packedLight, packedOverlay, rTint, gTint, bTint, 255);
        pose.popPose();

        // Piece 3 — верхний маленький кусок, наклонён в сторону
        pose.pushPose();
        pose.translate(0.5, 1.661, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(12));
        pose.mulPose(Axis.ZP.rotationDegrees(14));
        drawTintedCube(pose, vc, -0.072F, 0F, -0.06F, 0.072F, 0.046F, 0.06F,
                packedLight, packedOverlay, rTint, gTint, bTint, 255);
        pose.popPose();
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

    private static void drawSegment(PoseStack pose, VertexConsumer vc, Vec3 a, Vec3 b, float vOff, int light) {
        Matrix4f mat = pose.last().pose();
        float th = HOSE_THICKNESS;
        Vec3 dir = b.subtract(a);
        Vec3 worldUp = Math.abs(dir.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 nd = dir.normalize();
        Vec3 right = nd.cross(worldUp).normalize().scale(th);
        Vec3 up = right.cross(nd).normalize().scale(th);

        Vec3[] aRing = new Vec3[SIDES];
        Vec3[] bRing = new Vec3[SIDES];
        for (int i = 0; i < SIDES; i++) {
            double angle = 2 * Math.PI * i / SIDES;
            double ca = Math.cos(angle), sa = Math.sin(angle);
            Vec3 offset = right.scale(ca).add(up.scale(sa));
            aRing[i] = a.add(offset);
            bRing[i] = b.add(offset);
        }

        float segV = 0.25F; // texture repeats every 4 segments along length
        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;
            float u0 = (float) i / SIDES;
            float u1 = (float) (i + 1) / SIDES;
            quadUV(mat, vc, aRing[i], aRing[next], bRing[next], bRing[i], u0, u1, vOff, vOff + segV, light);
        }
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
        vertHose(mat, vc, v1, 0, 0, light);
        vertHose(mat, vc, v2, 0, 1, light);
        vertHose(mat, vc, v3, 1, 1, light);
        vertHose(mat, vc, v4, 1, 0, light);
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
                .setOverlay(0)
                .setLight(light)
                .setNormal(0, 1, 0);
    }

    private static void drawTintedCube(PoseStack pose, VertexConsumer vc,
                                       float minX, float minY, float minZ,
                                       float maxX, float maxY, float maxZ,
                                       int light, int overlay,
                                       int r, int g, int b, int a) {
        Matrix4f m = pose.last().pose();
        // Top (+Y)
        addVertex(m, vc, minX, maxY, minZ, 0, 0,  0,  1, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, maxY, maxZ, 0, 1,  0,  1, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, maxY, maxZ, 1, 1,  0,  1, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, maxY, minZ, 1, 0,  0,  1, 0, light, overlay, r, g, b, a);
        // Bottom (-Y)
        addVertex(m, vc, minX, minY, minZ, 0, 0,  0, -1, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, minY, minZ, 1, 0,  0, -1, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, minY, maxZ, 1, 1,  0, -1, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, minY, maxZ, 0, 1,  0, -1, 0, light, overlay, r, g, b, a);
        // North (-Z)
        addVertex(m, vc, minX, minY, minZ, 0, 1,  0,  0, -1, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, maxY, minZ, 0, 0,  0,  0, -1, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, maxY, minZ, 1, 0,  0,  0, -1, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, minY, minZ, 1, 1,  0,  0, -1, light, overlay, r, g, b, a);
        // South (+Z)
        addVertex(m, vc, minX, minY, maxZ, 0, 1,  0,  0, 1, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, minY, maxZ, 1, 1,  0,  0, 1, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, maxY, maxZ, 1, 0,  0,  0, 1, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, maxY, maxZ, 0, 0,  0,  0, 1, light, overlay, r, g, b, a);
        // East (+X)
        addVertex(m, vc, maxX, minY, minZ, 0, 1,  1,  0, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, maxY, minZ, 0, 0,  1,  0, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, maxY, maxZ, 1, 0,  1,  0, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, maxX, minY, maxZ, 1, 1,  1,  0, 0, light, overlay, r, g, b, a);
        // West (-X)
        addVertex(m, vc, minX, minY, minZ, 0, 1, -1,  0, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, minY, maxZ, 1, 1, -1,  0, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, maxY, maxZ, 1, 0, -1,  0, 0, light, overlay, r, g, b, a);
        addVertex(m, vc, minX, maxY, minZ, 0, 0, -1,  0, 0, light, overlay, r, g, b, a);
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

    private static Vec3 getPlayerHandPoint(AbstractClientPlayer player, float partialTick) {
        double x = Mth.lerp(partialTick, player.xo, player.getX());
        double y = Mth.lerp(partialTick, player.yo, player.getY()) + player.getEyeHeight() * 0.62;
        double z = Mth.lerp(partialTick, player.zo, player.getZ());
        // right hand offset: perpendicular to body yaw, shifted to right side
        float yawRad = (float) Math.toRadians(Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot));
        double rightX = Math.cos(yawRad) * 0.35;
        double rightZ = Math.sin(yawRad) * 0.35;
        // forward offset so hose connects near mouthpiece tip
        double fwdX = -Math.sin(yawRad) * 0.2;
        double fwdZ =  Math.cos(yawRad) * 0.2;
        return new Vec3(x + rightX + fwdX, y, z + rightZ + fwdZ);
    }

    // Coal cube, tobacco disc and the (possibly long, draped) hose render
    // outside the 1x1x1 block cell. Without a widened box the BER gets
    // frustum-culled the moment the block's own cell leaves view — e.g. the
    // coal vanishes when you tilt the camera up at close range.
    @Override
    public AABB getRenderBoundingBox(HookahBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(10.0);
    }

    @Override
    public boolean shouldRenderOffScreen(HookahBlockEntity be) { return true; }

    @Override
    public int getViewDistance() { return 64; }
}

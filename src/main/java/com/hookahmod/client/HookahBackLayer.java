package com.hookahmod.client;

import com.hookahmod.HookahMod;
import com.hookahmod.smoking.MouthpiecePosition;
import com.hookahmod.block.HookahBlock;
import com.hookahmod.item.HookahHoseType;
import com.hookahmod.item.HookahTier;
import com.hookahmod.item.WornHookah;
import com.hookahmod.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.UUID;

public class HookahBackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final int SEGMENTS = 28;
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

        boolean lit = WornHookah.hasCoal(stack);
        int light = lit ? litBlockLight(packedLight, 8) : packedLight;

        renderHookahBlockModel(pose, buffer, light, lit, HookahTier.fromStack(stack));

        if (lit) {
            renderCoal(pose, buffer, light, player);
        }

        HookahHoseType hoseType = WornHookah.getHoseType(stack);
        if (hoseType.isPresent()) {
            renderHose(pose, buffer, light, player, stack, partialTick);
        }
    }

    private void applyBackHookahTransform(PoseStack pose) {
        getParentModel().body.translateAndRotate(pose);
        pose.translate(BACK_X, BACK_Y, BACK_Z);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        pose.scale(BACK_SCALE, BACK_SCALE, BACK_SCALE);
    }

    private void renderHookahBlockModel(PoseStack pose, MultiBufferSource buffer, int packedLight, boolean lit, HookahTier tier) {
        pose.pushPose();
        applyBackHookahTransform(pose);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                ModBlocks.HOOKAH.get().defaultBlockState().setValue(HookahBlock.HAS_COAL, lit).setValue(HookahBlock.TIER, tier),
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

        pose.pushPose();
        applyBackHookahTransform(pose);
        UUID activeUuid = WornHookah.getActivePlayerUuid(hookahStack);
        if (activeUuid != null && wearer.level().getPlayerByUUID(activeUuid) instanceof AbstractClientPlayer active) {
            p3 = handPointInHookahSpace(pose, active, partialTick);
        }

        Vec3 p1 = p0.add(-0.45, -0.15, 0.0);
        Vec3 p2 = p3.add(0.15, 0.25, 0.0);

        HoseRenderer.draw(pose, vc, p0, p1, p2, p3, SEGMENTS, HOSE_THICKNESS, packedLight);
        pose.popPose();
    }

    private static Vec3 idleHoseEnd() {
        return new Vec3(-0.2375, 0.06, 0.5);
    }

    private static Vec3 handPointInHookahSpace(PoseStack pose, AbstractClientPlayer active, float partialTick) {
        Vec3 hand = MouthpiecePosition.hand(active, partialTick)
                .subtract(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
        Vector3f local = new Matrix4f(pose.last().pose()).invert()
                .transformPosition(new Vector3f((float) hand.x, (float) hand.y, (float) hand.z));
        return new Vec3(local.x, local.y, local.z);
    }

    /**
     * Swept once per client tick over the player list instead of hooking every
     * entity tick in the level: only players can wear a hookah, and there are a
     * few dozen of them against thousands of entities.
     */
    public static void tickCoalParticles(ClientLevel level) {
        for (Player player : level.players()) {
            ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!WornHookah.isHookahStack(stack) || !WornHookah.hasCoal(stack)) continue;
            if (level.random.nextInt(6) != 0) continue;
            float yaw = (float) Math.toRadians(player.yBodyRot);
            double px = player.getX() + Math.sin(yaw) * 0.42;
            double py = player.getY() + player.getBbHeight() * 0.92;
            double pz = player.getZ() - Math.cos(yaw) * 0.42;
            level.addParticle(ParticleTypes.SMALL_FLAME,
                    px + (level.random.nextDouble() - 0.5) * 0.12,
                    py + level.random.nextDouble() * 0.08,
                    pz + (level.random.nextDouble() - 0.5) * 0.12,
                    0.0,
                    0.015 + level.random.nextDouble() * 0.02,
                    0.0);
        }
    }

    private static int litBlockLight(int packedLight, int min) {
        int block = Math.max((packedLight >> 4) & 0xF, min);
        int sky = (packedLight >> 20) & 0xF;
        return LightTexture.pack(block, sky);
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

}

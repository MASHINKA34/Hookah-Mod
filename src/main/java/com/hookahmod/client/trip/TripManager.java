package com.hookahmod.client.trip;

import com.hookahmod.HookahMod;
import com.hookahmod.client.ClientIntoxication;
import com.hookahmod.smoking.IntoxicationBand;
import com.hookahmod.smoking.IntoxicationState;
import com.hookahmod.trip.TripVisionType;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class TripManager {

    private static final ResourceLocation RUNNER_TEXTURE = HookahMod.id("textures/entity/vision_runner.png");
    private static final float RUNNER_HEIGHT = 3.05f;
    private static final float RUNNER_WIDTH = 2.18f;
    private static final List<Vision> VISIONS = new ArrayList<>();
    private static int skyShiftTicks;
    private static int tickCount;

    private TripManager() {}

    public static void trigger(TripVisionType type, long seed) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (type == TripVisionType.SKY_SHIFT) {
            skyShiftTicks = Math.max(skyShiftTicks, 220);
            return;
        }
        RandomSource random = RandomSource.create(seed);
        Vec3 base = mc.player.position();
        Vec3 look = horizontal(mc.player.getLookAngle());
        Vec3 right = new Vec3(-look.z, 0.0, look.x);
        double side = (random.nextDouble() - 0.5) * 10.0;
        Vec3 pos = switch (type) {
            case OBSERVER -> base.add(look.scale(22.0 + random.nextDouble() * 10.0)).add(right.scale(side)).add(0.0, 1.0, 0.0);
            case FALSE_MOB -> base.add(look.scale(8.0 + random.nextDouble() * 5.0)).add(right.scale(side * 0.45)).add(0.0, 0.1, 0.0);
            case PLAYER_COPY -> base.add(look.scale(-3.5 - random.nextDouble() * 2.5)).add(right.scale(side * 0.25));
            case RUNNER -> base.add(look.scale(24.0)).add(right.scale(side * 0.2));
            default -> base;
        };
        int duration = switch (type) {
            case OBSERVER -> 220;
            case FALSE_MOB -> 120;
            case PLAYER_COPY -> 150;
            case RUNNER -> 180;
            default -> 80;
        };
        VISIONS.add(new Vision(type, pos, duration, seed));
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        tickCount++;
        if (mc.player == null || mc.level == null) {
            VISIONS.clear();
            skyShiftTicks = 0;
            return;
        }
        float intoxication = ClientIntoxication.get();
        if (IntoxicationState.band(intoxication).atLeast(IntoxicationBand.TRIP)) {
            spawnAmbientParticles(mc, intoxication);
        }
        if (skyShiftTicks > 0) skyShiftTicks--;
        Iterator<Vision> iterator = VISIONS.iterator();
        while (iterator.hasNext()) {
            Vision vision = iterator.next();
            vision.age++;
            if (vision.type == TripVisionType.RUNNER) {
                tickRunner(mc, vision);
            }
            if (vision.type == TripVisionType.OBSERVER && mc.player.position().distanceTo(vision.position) < 12.0) {
                vision.duration = Math.min(vision.duration, vision.age + 18);
            }
            if (vision.age >= vision.duration) {
                iterator.remove();
            }
        }
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || VISIONS.isEmpty()) return;

        PoseStack pose = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(new ByteBufferBuilder(4096));

        for (Vision vision : VISIONS) {
            float alpha = vision.type == TripVisionType.RUNNER ? vision.runnerAlpha() : vision.alpha();
            if (alpha <= 0.02f) continue;
            pose.pushPose();
            pose.translate(vision.position.x - cameraPos.x, vision.position.y - cameraPos.y, vision.position.z - cameraPos.z);
            switch (vision.type) {
                case OBSERVER -> drawBox(pose, buffer.getBuffer(RenderType.debugQuads()), -0.35f, 0.0f, -0.18f, 0.35f, 3.2f, 0.18f, 12, 10, 18, (int) (185 * alpha));
                case FALSE_MOB -> drawFalseMob(pose, buffer.getBuffer(RenderType.debugQuads()), alpha);
                case PLAYER_COPY -> drawPlayerCopy(pose, buffer.getBuffer(RenderType.debugQuads()), alpha);
                case RUNNER -> {
                    mc.getTextureManager().getTexture(RUNNER_TEXTURE).setFilter(false, false);
                    drawRunner(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(RUNNER_TEXTURE)), camera, alpha);
                }
                default -> {}
            }
            pose.popPose();
        }

        buffer.endBatch();
    }

    public static void computeFov(ViewportEvent.ComputeFov event) {
        float strength = visualStrength();
        if (strength <= 0.0f) return;
        double wave = Math.sin((tickCount + event.getPartialTick()) * 0.11) * 2.3 * strength;
        event.setFOV(event.getFOV() + wave);
    }

    public static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float strength = visualStrength();
        if (strength <= 0.0f) return;
        float roll = event.getRoll() + (float) Math.sin((tickCount + event.getPartialTick()) * 0.09) * 2.8f * strength;
        event.setRoll(roll);
    }

    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        float strength = Math.max(visualStrength(), skyShiftTicks > 0 ? 0.55f : 0.0f);
        if (strength <= 0.0f) return;
        float pulse = 0.5f + 0.5f * Mth.sin((tickCount + (float) event.getPartialTick()) * 0.04f);
        event.setRed(Mth.lerp(0.18f * strength, event.getRed(), 0.42f + pulse * 0.28f));
        event.setGreen(Mth.lerp(0.13f * strength, event.getGreen(), 0.18f + pulse * 0.2f));
        event.setBlue(Mth.lerp(0.22f * strength, event.getBlue(), 0.62f + pulse * 0.25f));
    }

    private static void tickRunner(Minecraft mc, Vision vision) {
        Vec3 target = mc.player.position().add(0.0, 0.05, 0.0);
        Vec3 delta = target.subtract(vision.position);
        double distance = delta.length();
        if (distance > 1.15) {
            vision.position = vision.position.add(delta.normalize().scale(Math.min(0.46, distance - 1.05)));
        } else if (vision.age < vision.duration - 22) {
            vision.duration = vision.age + 22;
        }
    }

    private static void spawnAmbientParticles(Minecraft mc, float intoxication) {
        if (mc.level == null || mc.player == null || tickCount % 4 != 0) return;
        float strength = visualStrength(intoxication);
        Vec3 look = mc.player.getLookAngle();
        Vec3 right = horizontal(new Vec3(-look.z, 0.0, look.x));
        double side = (mc.level.random.nextDouble() - 0.5) * 1.8;
        double forward = 1.1 + mc.level.random.nextDouble() * 1.8;
        Vec3 pos = mc.player.getEyePosition().add(look.scale(forward)).add(right.scale(side));
        mc.level.addParticle(
                mc.level.random.nextBoolean() ? ParticleTypes.PORTAL : ParticleTypes.WITCH,
                pos.x,
                pos.y + (mc.level.random.nextDouble() - 0.5) * 0.8,
                pos.z,
                (mc.level.random.nextDouble() - 0.5) * 0.02 * strength,
                (mc.level.random.nextDouble() - 0.5) * 0.02 * strength,
                (mc.level.random.nextDouble() - 0.5) * 0.02 * strength
        );
    }

    private static float visualStrength() {
        return visualStrength(ClientIntoxication.get());
    }

    private static float visualStrength(float intoxication) {
        if (intoxication < 100.0f) return 0.0f;
        return Mth.clamp((intoxication - 90.0f) / 90.0f, 0.22f, intoxication >= 150.0f ? 1.0f : 0.72f);
    }

    private static Vec3 horizontal(Vec3 vec) {
        Vec3 horizontal = new Vec3(vec.x, 0.0, vec.z);
        if (horizontal.lengthSqr() < 1.0E-5) return new Vec3(0.0, 0.0, 1.0);
        return horizontal.normalize();
    }

    private static void drawFalseMob(PoseStack pose, VertexConsumer vc, float alpha) {
        int a = (int) (145 * alpha);
        drawBox(pose, vc, -0.28f, 0.0f, -0.18f, 0.28f, 1.45f, 0.18f, 40, 48, 64, a);
        drawBox(pose, vc, -0.2f, 1.45f, -0.16f, 0.2f, 1.85f, 0.16f, 90, 95, 110, a);
        drawBox(pose, vc, -0.72f, 0.75f, -0.08f, -0.28f, 1.05f, 0.08f, 70, 80, 95, a);
        drawBox(pose, vc, 0.28f, 0.75f, -0.08f, 0.72f, 1.05f, 0.08f, 70, 80, 95, a);
    }

    private static void drawPlayerCopy(PoseStack pose, VertexConsumer vc, float alpha) {
        int a = (int) (120 * alpha);
        drawBox(pose, vc, -0.28f, 0.0f, -0.16f, 0.28f, 1.35f, 0.16f, 90, 55, 135, a);
        drawBox(pose, vc, -0.23f, 1.35f, -0.18f, 0.23f, 1.82f, 0.18f, 130, 88, 170, a);
    }

    private static void drawRunner(PoseStack pose, VertexConsumer vc, Camera camera, float alpha) {
        pose.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        Matrix4f matrix = pose.last().pose();
        int a = (int) (255 * alpha);
        float halfWidth = RUNNER_WIDTH * 0.5f;
        addTexturedVertex(matrix, vc, -halfWidth, 0.0f, 0.0f, 0.0f, 1.0f, a);
        addTexturedVertex(matrix, vc, halfWidth, 0.0f, 0.0f, 1.0f, 1.0f, a);
        addTexturedVertex(matrix, vc, halfWidth, RUNNER_HEIGHT, 0.0f, 1.0f, 0.0f, a);
        addTexturedVertex(matrix, vc, -halfWidth, RUNNER_HEIGHT, 0.0f, 0.0f, 0.0f, a);
    }

    private static void drawBox(PoseStack pose, VertexConsumer vc, float minX, float minY, float minZ,
                                float maxX, float maxY, float maxZ, int r, int g, int b, int a) {
        Matrix4f m = pose.last().pose();
        vertex(m, vc, minX, maxY, minZ, r, g, b, a);
        vertex(m, vc, minX, maxY, maxZ, r, g, b, a);
        vertex(m, vc, maxX, maxY, maxZ, r, g, b, a);
        vertex(m, vc, maxX, maxY, minZ, r, g, b, a);
        vertex(m, vc, minX, minY, minZ, r, g, b, a);
        vertex(m, vc, maxX, minY, minZ, r, g, b, a);
        vertex(m, vc, maxX, minY, maxZ, r, g, b, a);
        vertex(m, vc, minX, minY, maxZ, r, g, b, a);
        vertex(m, vc, minX, minY, minZ, r, g, b, a);
        vertex(m, vc, minX, maxY, minZ, r, g, b, a);
        vertex(m, vc, maxX, maxY, minZ, r, g, b, a);
        vertex(m, vc, maxX, minY, minZ, r, g, b, a);
        vertex(m, vc, minX, minY, maxZ, r, g, b, a);
        vertex(m, vc, maxX, minY, maxZ, r, g, b, a);
        vertex(m, vc, maxX, maxY, maxZ, r, g, b, a);
        vertex(m, vc, minX, maxY, maxZ, r, g, b, a);
        vertex(m, vc, maxX, minY, minZ, r, g, b, a);
        vertex(m, vc, maxX, maxY, minZ, r, g, b, a);
        vertex(m, vc, maxX, maxY, maxZ, r, g, b, a);
        vertex(m, vc, maxX, minY, maxZ, r, g, b, a);
        vertex(m, vc, minX, minY, minZ, r, g, b, a);
        vertex(m, vc, minX, minY, maxZ, r, g, b, a);
        vertex(m, vc, minX, maxY, maxZ, r, g, b, a);
        vertex(m, vc, minX, maxY, minZ, r, g, b, a);
    }

    private static void vertex(Matrix4f matrix, VertexConsumer vc, float x, float y, float z, int r, int g, int b, int a) {
        vc.addVertex(matrix, x, y, z).setColor(r, g, b, a);
    }

    private static void addTexturedVertex(Matrix4f matrix, VertexConsumer vc, float x, float y, float z, float u, float v, int a) {
        vc.addVertex(matrix, x, y, z)
                .setColor(255, 255, 255, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 1.0f, 0.0f);
    }

    private static final class Vision {
        private final TripVisionType type;
        private final long seed;
        private Vec3 position;
        private int age;
        private int duration;

        private Vision(TripVisionType type, Vec3 position, int duration, long seed) {
            this.type = type;
            this.position = position;
            this.duration = duration;
            this.seed = seed;
        }

        private float alpha() {
            float fadeIn = Mth.clamp(age / 18.0f, 0.0f, 1.0f);
            float fadeOut = Mth.clamp((duration - age) / 28.0f, 0.0f, 1.0f);
            float flicker = 0.72f + 0.28f * Mth.sin((age + (int) (seed & 31L)) * 0.43f);
            return fadeIn * fadeOut * flicker;
        }

        private float runnerAlpha() {
            float fadeIn = Mth.clamp(age / 4.0f, 0.0f, 1.0f);
            float fadeOut = Mth.clamp((duration - age) / 10.0f, 0.0f, 1.0f);
            return fadeIn * fadeOut;
        }
    }
}

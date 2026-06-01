package com.hookahmod.client.trip;

import com.hookahmod.HookahMod;
import com.hookahmod.client.ClientIntoxication;
import com.hookahmod.smoking.IntoxicationBand;
import com.hookahmod.smoking.IntoxicationState;
import com.hookahmod.trip.TripVisionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class TripManager {

    private static final ResourceLocation RUNNER_TEXTURE = HookahMod.id("textures/entity/vision_runner.png");
    private static final ResourceLocation OBSERVER_TEXTURE = HookahMod.id("textures/entity/vision_observer.png");
    private static final ResourceLocation FALSE_MOB_TEXTURE = HookahMod.id("textures/entity/vision_false_mob.png");
    private static final ResourceLocation PLAYER_COPY_TEXTURE = HookahMod.id("textures/entity/vision_player_copy.png");
    private static final float RUNNER_HEIGHT = 3.05f;
    private static final float RUNNER_WIDTH = 2.18f;
    private static final int SCREAMER_DURATION = 30;
    private static final List<Vision> VISIONS = new ArrayList<>();
    private static final ByteBufferBuilder VISION_BUFFER = new ByteBufferBuilder(4096);
    private static int runnerScreamerTicks;
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
        VISIONS.add(new Vision(type, pos, duration));
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        tickCount++;
        if (mc.player == null || mc.level == null) {
            VISIONS.clear();
            skyShiftTicks = 0;
            runnerScreamerTicks = 0;
            return;
        }
        float intoxication = ClientIntoxication.get();
        if (IntoxicationState.band(intoxication).atLeast(IntoxicationBand.TRIP)) {
            spawnAmbientParticles(mc, intoxication);
        } else {
            VISIONS.clear();
            skyShiftTicks = 0;
        }
        if (runnerScreamerTicks > 0) runnerScreamerTicks--;
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
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(VISION_BUFFER);

        for (Vision vision : VISIONS) {
            float alpha = vision.type == TripVisionType.RUNNER ? vision.runnerAlpha() : vision.alpha();
            if (alpha <= 0.02f) continue;
            pose.pushPose();
            pose.translate(vision.position.x - cameraPos.x, vision.position.y - cameraPos.y, vision.position.z - cameraPos.z);
            switch (vision.type) {
                case OBSERVER -> drawTexturedVision(mc, pose, buffer, camera, OBSERVER_TEXTURE, 1.6f, 3.4f, alpha, true);
                case FALSE_MOB -> drawTexturedVision(mc, pose, buffer, camera, FALSE_MOB_TEXTURE, 1.15f, 2.0f, alpha, true);
                case PLAYER_COPY -> drawTexturedVision(mc, pose, buffer, camera, PLAYER_COPY_TEXTURE, 1.0f, 1.95f, alpha, true);
                case RUNNER -> drawTexturedVision(mc, pose, buffer, camera, RUNNER_TEXTURE, RUNNER_WIDTH, RUNNER_HEIGHT, alpha, false);
                default -> {}
            }
            pose.popPose();
        }

        buffer.endBatch();
    }

    public static void renderScreamer(RenderGuiEvent.Post event) {
        if (runnerScreamerTicks <= 0) return;
        if (Minecraft.getInstance().player == null) {
            runnerScreamerTicks = 0;
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float alpha = Mth.clamp(runnerScreamerTicks / 6.0f, 0.0f, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1.0f, 1.0f, 1.0f, alpha);
        graphics.blit(RUNNER_TEXTURE, 0, 0, width, height, 0.0f, 0.0f, 16, 16, 16, 16);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
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
        } else {
            runnerScreamerTicks = SCREAMER_DURATION;
            vision.duration = vision.age;
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

    private static void drawTexturedVision(Minecraft mc, PoseStack pose, MultiBufferSource buffer, Camera camera,
                                           ResourceLocation texture, float width, float height, float alpha, boolean translucent) {
        mc.getTextureManager().getTexture(texture).setFilter(false, false);
        RenderType type = translucent ? RenderType.entityTranslucent(texture) : RenderType.entityCutoutNoCull(texture);
        drawBillboard(pose, buffer.getBuffer(type), camera, width, height, alpha);
    }

    private static void drawBillboard(PoseStack pose, VertexConsumer vc, Camera camera, float width, float height, float alpha) {
        pose.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        Matrix4f matrix = pose.last().pose();
        int a = (int) (255 * alpha);
        float halfWidth = width * 0.5f;
        addTexturedVertex(matrix, vc, -halfWidth, 0.0f, 0.0f, 0.0f, 1.0f, a);
        addTexturedVertex(matrix, vc, halfWidth, 0.0f, 0.0f, 1.0f, 1.0f, a);
        addTexturedVertex(matrix, vc, halfWidth, height, 0.0f, 1.0f, 0.0f, a);
        addTexturedVertex(matrix, vc, -halfWidth, height, 0.0f, 0.0f, 0.0f, a);
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
        private Vec3 position;
        private int age;
        private int duration;

        private Vision(TripVisionType type, Vec3 position, int duration) {
            this.type = type;
            this.position = position;
            this.duration = duration;
        }

        private float alpha() {
            float fadeIn = Mth.clamp(age / 18.0f, 0.0f, 1.0f);
            float fadeOut = Mth.clamp((duration - age) / 28.0f, 0.0f, 1.0f);
            return fadeIn * fadeOut;
        }

        private float runnerAlpha() {
            float fadeIn = Mth.clamp(age / 4.0f, 0.0f, 1.0f);
            float fadeOut = Mth.clamp((duration - age) / 10.0f, 0.0f, 1.0f);
            return fadeIn * fadeOut;
        }
    }
}

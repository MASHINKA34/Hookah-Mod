package com.hookahmod.client.trip;

import com.hookahmod.HookahMod;
import com.hookahmod.effect.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class HashishTripManager {

    private static final ResourceLocation EFFECT = HookahMod.id("shaders/post/hashish_spiral.json");
    private static final int ADD_DURATION_TICKS = 15 * 20;
    private static final int RAMP_TICKS = 5 * 20;
    private static final int FADE_TICKS = 5 * 20;

    private static int remainingTicks;
    private static int totalDurationTicks;
    private static int ageTicks;
    private static int tickCount;
    private static float intensity = 1.0f;
    private static boolean loadFailed;

    private HashishTripManager() {}

    public static void start(int durationTicks, float requestedIntensity) {
        if (remainingTicks > 0) return;

        int addTicks = durationTicks > 0 ? durationTicks : ADD_DURATION_TICKS;
        remainingTicks = addTicks;
        totalDurationTicks = addTicks;
        ageTicks = 0;
        loadFailed = false;
        intensity = Mth.clamp(requestedIntensity, 0.2f, 1.75f);
        ensurePostEffect();
        updateShaderUniforms(0.0f);
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.player.isAlive()) {
            clear();
            HashishTripSoundController.stop(mc);
            return;
        }

        if (mc.isPaused()) return;
        tickCount++;
        MobEffectInstance effect = mc.player.getEffect(ModMobEffects.HASHISH_TRIP);
        if (effect == null) {
            clear();
            HashishTripSoundController.stop(mc);
            return;
        }
        int duration = effect.isInfiniteDuration() ? ADD_DURATION_TICKS : effect.getDuration();
        if (remainingTicks <= 0) start(duration, 1.0f);
        remainingTicks = duration;
        totalDurationTicks = Math.max(totalDurationTicks, duration);
        ageTicks++;
        ensurePostEffect();
        updateShaderUniforms(0.0f);
        HashishTripSoundController.tick(mc, mc.player);
    }

    public static boolean isActive() {
        return remainingTicks > 0;
    }

    public static void computeFov(ViewportEvent.ComputeFov event) {
        float strength = visualStrength(event.getPartialTick());
        if (strength <= 0.0f) return;

        double phase = (tickCount + event.getPartialTick()) * 0.16;
        event.setFOV(event.getFOV() + Math.sin(phase) * 5.0 * strength);
    }

    public static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float strength = visualStrength(event.getPartialTick());
        if (strength <= 0.0f) return;

        float phase = (float) (tickCount + event.getPartialTick());
        event.setYaw(event.getYaw() + Mth.sin(phase * 0.075f) * 0.55f * strength);
        event.setPitch(event.getPitch() + Mth.sin(phase * 0.095f + 1.7f) * 0.45f * strength);
        event.setRoll(event.getRoll() + Mth.sin(phase * 0.12f) * 2.4f * strength);
    }

    private static void ensurePostEffect() {
        if (loadFailed || remainingTicks <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        PostChain current = mc.gameRenderer.currentEffect();
        if (isHashishEffect(current)) return;

        try {
            mc.gameRenderer.loadEffect(EFFECT);
        } catch (RuntimeException ex) {
            loadFailed = true;
            HookahMod.LOGGER.warn("Failed to load hashish trip shader: {}", EFFECT, ex);
        }
    }

    private static void updateShaderUniforms(float partialTick) {
        PostChain current = Minecraft.getInstance().gameRenderer.currentEffect();
        if (!isHashishEffect(current)) return;

        current.setUniform("Intensity", visualStrength(partialTick));
        current.setUniform("TripTime", (tickCount + partialTick) / 20.0f);
        current.setUniform("RemainingRatio", remainingRatio(partialTick));
    }

    private static float visualStrength(double partialTick) {
        if (remainingTicks <= 0) return 0.0f;

        float age = ageTicks + (float) partialTick;
        float remaining = Math.max(0.0f, remainingTicks - (float) partialTick);
        float fadeIn = Mth.clamp(age / RAMP_TICKS, 0.0f, 1.0f);
        float fadeOut = Mth.clamp(remaining / FADE_TICKS, 0.0f, 1.0f);
        float durationRatio = remainingRatio(partialTick);
        float durationScale = 0.55f + 0.45f * (float) Math.sqrt(durationRatio);

        return Mth.clamp(intensity * durationScale * fadeIn * fadeOut, 0.0f, 1.0f);
    }

    private static float remainingRatio(double partialTick) {
        if (totalDurationTicks <= 0) return 0.0f;
        return Mth.clamp((remainingTicks - (float) partialTick) / totalDurationTicks, 0.0f, 1.0f);
    }

    private static void clear() {
        remainingTicks = 0;
        totalDurationTicks = 0;
        ageTicks = 0;
        intensity = 1.0f;

        Minecraft mc = Minecraft.getInstance();
        if (isHashishEffect(mc.gameRenderer.currentEffect())) {
            mc.gameRenderer.shutdownEffect();
        }
    }

    private static boolean isHashishEffect(PostChain chain) {
        return chain != null && EFFECT.toString().equals(chain.getName());
    }
}

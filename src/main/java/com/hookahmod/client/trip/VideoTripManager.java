package com.hookahmod.client.trip;

import com.hookahmod.HookahMod;
import com.hookahmod.effect.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class VideoTripManager {

    private static final int FRAME_COUNT = 299;
    private static final float FPS = 15.0f;
    private static final int FRAME_W = 160;
    private static final int FRAME_H = 160;

    private static boolean active;
    private static int ageTicks;
    private static int lastFrame = -1;

    private VideoTripManager() {}

    public static void start() {
        active = true;
        ageTicks = 0;
        lastFrame = -1;
    }

    public static boolean isActive() {
        return active;
    }

    public static void tick() {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        boolean alive = mc.player != null && mc.level != null
                && mc.player.getEffect(ModMobEffects.PALPALYCH_TRIP) != null
                && currentFrame() < FRAME_COUNT;
        if (!alive) {
            stop();
            return;
        }
        ageTicks++;
        PalPalychSoundController.tick(mc, mc.player);
    }

    public static void render(RenderGuiEvent.Post event) {
        if (!active) return;
        int idx = currentFrame();
        if (idx < 0 || idx >= FRAME_COUNT) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        g.blit(frameId(idx), 0, 0, sw, sh, 0.0F, 0.0F, FRAME_W, FRAME_H, FRAME_W, FRAME_H);
        if (lastFrame >= 0 && lastFrame != idx) {
            mc.getTextureManager().release(frameId(lastFrame));
        }
        lastFrame = idx;
    }

    public static void stop() {
        if (!active) return;
        active = false;
        lastFrame = -1;
        PalPalychSoundController.stop(Minecraft.getInstance());
    }

    private static int currentFrame() {
        return (int) Math.floor(ageTicks / 20.0f * FPS);
    }

    private static ResourceLocation frameId(int idx) {
        return HookahMod.id(String.format("textures/gui/palpalych_trip/frame_%04d.png", idx + 1));
    }
}

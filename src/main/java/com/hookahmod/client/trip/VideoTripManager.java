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
    private static final int SHEET_COLUMNS = 8;
    private static final int SHEET_ROWS = 8;
    private static final int FRAMES_PER_SHEET = SHEET_COLUMNS * SHEET_ROWS;
    private static final int SHEET_W = FRAME_W * SHEET_COLUMNS;
    private static final int SHEET_H = FRAME_H * SHEET_ROWS;
    private static final int SHEET_COUNT = (FRAME_COUNT + FRAMES_PER_SHEET - 1) / FRAMES_PER_SHEET;
    private static final ResourceLocation[] SHEETS = new ResourceLocation[SHEET_COUNT];

    static {
        for (int index = 0; index < SHEET_COUNT; index++) {
            SHEETS[index] = HookahMod.id("textures/gui/palpalych_trip/sheet_" + index + ".png");
        }
    }

    private static boolean active;
    private static int ageTicks;

    private VideoTripManager() {}

    public static void start() {
        active = true;
        ageTicks = 0;
        preload();
    }

    public static boolean isActive() {
        return active;
    }

    public static void tick() {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        boolean alive = mc.player != null && mc.player.isAlive() && mc.level != null
                && mc.player.getEffect(ModMobEffects.PALPALYCH_TRIP) != null
                && currentFrame() < FRAME_COUNT;
        if (!alive) {
            stop();
            return;
        }
        if (mc.isPaused()) return;
        ageTicks++;
        PalPalychSoundController.tick(mc, mc.player);
    }

    public static void render(RenderGuiEvent.Post event) {
        if (!active) return;
        int frame = currentFrame();
        if (frame < 0 || frame >= FRAME_COUNT) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int local = frame % FRAMES_PER_SHEET;
        graphics.blit(
                SHEETS[frame / FRAMES_PER_SHEET],
                0, 0,
                screenWidth, screenHeight,
                (local % SHEET_COLUMNS) * FRAME_W,
                (float) (local / SHEET_COLUMNS) * FRAME_H,
                FRAME_W, FRAME_H,
                SHEET_W, SHEET_H
        );
    }

    public static void stop() {
        if (!active) return;
        active = false;
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation sheet : SHEETS) {
            mc.getTextureManager().release(sheet);
        }
        PalPalychSoundController.stop(mc);
    }

    private static void preload() {
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation sheet : SHEETS) {
            mc.getTextureManager().getTexture(sheet);
        }
    }

    private static int currentFrame() {
        return (int) Math.floor(ageTicks / 20.0f * FPS);
    }
}

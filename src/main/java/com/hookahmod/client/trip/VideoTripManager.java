package com.hookahmod.client.trip;

import com.hookahmod.HookahMod;
import com.hookahmod.config.HookahClientConfig;
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

    private static final int PRELOAD_LEAD_FRAMES = 15;

    private static boolean active;
    private static int ageTicks;
    private static int loadedSheets;
    private static int releasedSheets;

    private VideoTripManager() {}

    public static void start() {
        if (!HookahClientConfig.videoTripEnabled()) return;
        active = true;
        ageTicks = 0;
        loadedSheets = 0;
        releasedSheets = 0;
        streamSheets();
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
        streamSheets();
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
        loadedSheets = 0;
        releasedSheets = 0;
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation sheet : SHEETS) {
            mc.getTextureManager().release(sheet);
        }
        PalPalychSoundController.stop(mc);
    }

    private static void streamSheets() {
        Minecraft mc = Minecraft.getInstance();
        int frame = Math.max(0, currentFrame());
        int wanted = Math.min(SHEET_COUNT, (frame + PRELOAD_LEAD_FRAMES) / FRAMES_PER_SHEET + 1);
        for (int index = loadedSheets; index < wanted; index++) {
            mc.getTextureManager().getTexture(SHEETS[index]);
        }
        loadedSheets = Math.max(loadedSheets, wanted);

        int playing = frame / FRAMES_PER_SHEET;
        while (releasedSheets < playing) {
            mc.getTextureManager().release(SHEETS[releasedSheets]);
            releasedSheets++;
        }
    }

    private static int currentFrame() {
        return (int) Math.floor(ageTicks / 20.0f * FPS);
    }
}

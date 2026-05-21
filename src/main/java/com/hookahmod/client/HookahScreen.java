package com.hookahmod.client;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.event.ActiveSessions;
import com.hookahmod.item.HookahHoseType;
import com.hookahmod.menu.FilteredSlot;
import com.hookahmod.menu.HookahMenu;
import com.hookahmod.menu.HoseSlot;
import com.hookahmod.network.ToggleMouthpiecePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public class HookahScreen extends AbstractContainerScreen<HookahMenu> {

    // ── Colors ──────────────────────────────────────────────────────
    private static final int C_BG         = 0xFF2A1508; // dark mahogany
    private static final int C_BG_GRAIN   = 0xFF1A0C04; // wood grain line
    private static final int C_GOLD_LIGHT = 0xFFDAA520; // top/highlight
    private static final int C_GOLD_MID   = 0xFFB8860B; // side
    private static final int C_GOLD_DARK  = 0xFF6B4A00; // bottom/shadow
    private static final int C_SLOT_INNER = 0xFF0D0808; // slot dark bg
    private static final int C_SEP        = 0xFF6A4A14; // separator line
    private static final int C_STATUS_BG  = 0xFF130A03; // status panel bg
    private static final int C_BTN_BG     = 0xFF7A5808; // brass button bg

    // ── Layout ──────────────────────────────────────────────────────
    // Y boundary between hookah section and inventory section
    private static final int PANEL_H = 146;

    // Status panel (left of button)
    private static final int STA_X1 = 5,  STA_Y1 = 113, STA_X2 = 91,  STA_Y2 = 143;
    // Button panel (right)
    private static final int BTN_X1 = 95, BTN_Y1 = 113, BTN_X2 = 171, BTN_Y2 = 143;

    // Button widget area (must be inside BTN panel)
    private static final int BUTTON_X    = 97;
    private static final int BUTTON_Y    = 115;
    private static final int BUTTON_W    = 72;
    private static final int BUTTON_H    = 26;
    private static final int BUTTON_TEXT_Y = 123;

    // Status text (inside STA panel)
    private static final int STATUS_X = 9;
    private static final int STATUS_Y = 123;

    // Hose info line
    private static final int HOSE_INFO_Y = 22;

    private Button toggleButton;

    public HookahScreen(HookahMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 176;
        this.imageHeight = 236;
        this.titleLabelY = 6;
        this.titleLabelX = 8;
        this.inventoryLabelY = 150;
        this.inventoryLabelX = 8;
    }

    @Override
    protected void init() {
        super.init();
        this.toggleButton = new TransparentButton(
                this.leftPos + BUTTON_X, this.topPos + BUTTON_Y,
                BUTTON_W, BUTTON_H,
                Component.translatable("gui.hookahmod.btn_take"),
                btn -> onToggle());
        this.addRenderableWidget(toggleButton);
    }

    private void onToggle() {
        if (menu.isWearable()) {
            PacketDistributor.sendToServer(ToggleMouthpiecePayload.worn(menu.getWearerUuid()));
        } else {
            PacketDistributor.sendToServer(new ToggleMouthpiecePayload(menu.getPos()));
        }
    }

    private HookahBlockEntity be() { return menu.getBlockEntity(); }

    private UUID myUuid() {
        return Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getUUID();
    }

    private boolean isInUseByMe() {
        UUID my = myUuid();
        if (menu.isWearable()) {
            return my != null && ActiveSessions.getWornWearer(my) != null;
        }
        return my != null && my.equals(menu.getActivePlayerUuid());
    }

    private boolean isBusyByOther() {
        return menu.isInUse() && !isInUseByMe();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Component label = isInUseByMe()
                ? Component.translatable("gui.hookahmod.btn_release")
                : Component.translatable("gui.hookahmod.btn_take");
        toggleButton.setMessage(label);
        toggleButton.active = !isBusyByOther();
    }

    // ────────────────────────────────────────────────────────────────
    //  Background – drawn entirely in code, no external texture
    // ────────────────────────────────────────────────────────────────
    @Override
    protected void renderBg(GuiGraphics gg, float partialTicks, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;

        // ── Hookah section (top 146 px) ──────────────────────────
        gg.fill(x, y, x + w, y + PANEL_H, C_BG);

        // Wood grain horizontal lines
        for (int i = 0; i < 10; i++) {
            gg.fill(x + 4, y + 9 + i * 14, x + w - 4, y + 10 + i * 14, C_BG_GRAIN);
        }

        // Outer 2-px gold border
        gg.fill(x,         y,          x + w,  y + 2,       C_GOLD_LIGHT);
        gg.fill(x,         y + PANEL_H - 2, x + w,  y + PANEL_H, C_GOLD_DARK);
        gg.fill(x,         y + 2,      x + 2,  y + PANEL_H - 2, C_GOLD_MID);
        gg.fill(x + w - 2, y + 2,      x + w,  y + PANEL_H - 2, C_GOLD_MID);

        // Title separator
        gg.fill(x + 4, y + 16, x + w - 4, y + 17, C_SEP);

        // Hose slot (centered, upper area)
        goldSlotFrame(gg, x + HookahMenu.HOSE_SLOT_X, y + HookahMenu.HOSE_SLOT_Y);

        // Separator above consumables
        gg.fill(x + 4, y + 77, x + w - 4, y + 78, C_SEP);

        // Three consumable slots
        goldSlotFrame(gg, x + HookahMenu.TOBACCO_X, y + HookahMenu.CONSUME_ROW_Y);
        goldSlotFrame(gg, x + HookahMenu.COAL_X,    y + HookahMenu.CONSUME_ROW_Y);
        goldSlotFrame(gg, x + HookahMenu.WATER_X,   y + HookahMenu.CONSUME_ROW_Y);

        // Separator above status/button row
        gg.fill(x + 4, y + 110, x + w - 4, y + 111, C_GOLD_MID);

        // Status dark panel
        gg.fill(x + STA_X1, y + STA_Y1, x + STA_X2, y + STA_Y2, C_STATUS_BG);
        borderRect(gg, x + STA_X1, y + STA_Y1, STA_X2 - STA_X1, STA_Y2 - STA_Y1);

        // Brass button panel
        gg.fill(x + BTN_X1, y + BTN_Y1, x + BTN_X2, y + BTN_Y2, C_BTN_BG);
        gg.fill(x + BTN_X1, y + BTN_Y1, x + BTN_X2, y + BTN_Y1 + 2, C_GOLD_LIGHT); // top shine
        gg.fill(x + BTN_X1, y + BTN_Y2 - 2, x + BTN_X2, y + BTN_Y2, 0xFF3A2800);   // bottom shadow
        borderRect(gg, x + BTN_X1, y + BTN_Y1, BTN_X2 - BTN_X1, BTN_Y2 - BTN_Y1);

        // ── Inventory section ─────────────────────────────────────
        int invTop = y + PANEL_H;
        gg.fill(x,     invTop,     x + w, y + imageHeight, 0xFF1F140A);
        gg.fill(x + 2, invTop + 2, x + w - 2, y + imageHeight - 2, 0xFF3A2516);
        gg.fill(x + 5, invTop + 5, x + w - 5, y + imageHeight - 5, 0xFF1F140A);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(gg, x + 8 + col * 18, y + HookahMenu.INV_ROW_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBg(gg, x + 8 + col * 18, y + HookahMenu.INV_HOTBAR_Y);
        }
    }

    // 2-px beveled gold border around a rectangle
    private static void borderRect(GuiGraphics gg, int x, int y, int w, int h) {
        gg.fill(x,         y,         x + w, y + 1,     C_GOLD_LIGHT);
        gg.fill(x,         y + h - 1, x + w, y + h,     C_GOLD_DARK);
        gg.fill(x,         y + 1,     x + 1, y + h - 1, C_GOLD_MID);
        gg.fill(x + w - 1, y + 1,     x + w, y + h - 1, C_GOLD_DARK);
    }

    // Gold-framed slot (frame is 2 px, inner 16×16)
    private static void goldSlotFrame(GuiGraphics gg, int sx, int sy) {
        gg.fill(sx, sy, sx + 16, sy + 16, C_SLOT_INNER);
        gg.fill(sx - 2, sy - 2, sx + 18, sy - 1,  C_GOLD_LIGHT);
        gg.fill(sx - 2, sy + 17, sx + 18, sy + 18, C_GOLD_DARK);
        gg.fill(sx - 2, sy - 1, sx - 1,  sy + 17,  C_GOLD_MID);
        gg.fill(sx + 17, sy - 1, sx + 18, sy + 17, C_GOLD_DARK);
    }

    // Standard inventory slot background
    private static void drawSlotBg(GuiGraphics gg, int sx, int sy) {
        gg.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF000000);
        gg.fill(sx, sy, sx + 16, sy + 16, 0xFF5C4A36);
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {
        int labelColor = 0xFFE6D6B0;

        int tw = this.font.width(this.title);
        gg.drawString(this.font, this.title, (this.imageWidth - tw) / 2, this.titleLabelY, labelColor, false);
        gg.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, labelColor, false);

        // Hose info
        HookahBlockEntity be = be();
        HookahHoseType type = menu.getHoseType();
        Component hoseLine;
        int hoseColor;
        if (type == HookahHoseType.NONE) {
            hoseLine = Component.translatable("gui.hookahmod.hose_not_installed");
            hoseColor = 0xFFE08080;
        } else if (type == HookahHoseType.SHORT) {
            hoseLine = Component.translatable("gui.hookahmod.hose_short");
            hoseColor = 0xFFC0E080;
        } else {
            hoseLine = Component.translatable("gui.hookahmod.hose_long");
            hoseColor = 0xFFC0E080;
        }
        int hw = this.font.width(hoseLine);
        gg.drawString(this.font, hoseLine, (this.imageWidth - hw) / 2, HOSE_INFO_Y, hoseColor, false);

        // Status
        Component status;
        int statusColor;
        if (isInUseByMe()) {
            status = Component.translatable("gui.hookahmod.status_yours");
            statusColor = 0xFF7FE07F;
        } else if (isBusyByOther()) {
            status = Component.translatable("gui.hookahmod.status_busy");
            statusColor = 0xFFE08080;
        } else {
            status = Component.translatable("gui.hookahmod.status_free");
            statusColor = 0xFF80C0FF;
        }
        gg.drawString(this.font, status, STATUS_X, STATUS_Y, statusColor, false);

        // Button label
        Component btnText = toggleButton.getMessage();
        int btnW = this.font.width(btnText);
        int btnColor = toggleButton.active ? 0xFF1A0A02 : 0xFF5C4A36;
        gg.drawString(this.font, btnText, BUTTON_X + (BUTTON_W - btnW) / 2, BUTTON_TEXT_Y, btnColor, false);

        // "Fill slots" hint
        if (isInUseByMe() && !menu.hasAllConsumables()) {
            Component hint = Component.translatable("gui.hookahmod.fill_slots").withStyle(ChatFormatting.ITALIC);
            int hwt = this.font.width(hint);
            gg.drawString(this.font, hint, (this.imageWidth - hwt) / 2, this.inventoryLabelY - 12, 0xFFB0B0B0, false);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics gg, int mouseX, int mouseY) {
        super.renderTooltip(gg, mouseX, mouseY);
        Slot s = this.hoveredSlot;
        if (s == null || s.hasItem()) return;
        Component hint = emptySlotHint(s);
        if (hint != null) {
            gg.renderTooltip(this.font, hint, mouseX, mouseY);
        }
    }

    private static Component emptySlotHint(Slot s) {
        if (s instanceof HoseSlot) return Component.translatable("gui.hookahmod.hint_hose");
        if (s instanceof FilteredSlot fs) {
            if (fs.allowedItem() == com.hookahmod.registry.ModItems.HOOKAH_TOBACCO.get())
                return Component.translatable("gui.hookahmod.hint_tobacco");
            if (fs.allowedItem() == com.hookahmod.registry.ModItems.HOOKAH_CHARCOAL.get())
                return Component.translatable("gui.hookahmod.hint_coal");
            if (fs.allowedItem() == com.hookahmod.registry.ModItems.HOOKAH_WATER_BOTTLE.get())
                return Component.translatable("gui.hookahmod.hint_water");
        }
        return null;
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(gg, mouseX, mouseY, partialTicks);
        super.render(gg, mouseX, mouseY, partialTicks);
        this.renderTooltip(gg, mouseX, mouseY);
    }

    private static class TransparentButton extends Button {
        TransparentButton(int x, int y, int w, int h, Component msg, OnPress press) {
            super(x, y, w, h, msg, press, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            if (this.isHovered() && this.active) {
                gg.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x22FFFFFF);
            }
        }
    }
}

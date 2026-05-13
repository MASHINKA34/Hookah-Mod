package com.hookahmod.client;

import com.hookahmod.HookahMod;
import com.hookahmod.block.HookahBlockEntity;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public class HookahScreen extends AbstractContainerScreen<HookahMenu> {

    private static final ResourceLocation BG_TEXTURE = HookahMod.id("textures/gui/hookah.png");
    private static final int KALYAN_W = 176;
    private static final int KALYAN_H = 176;

    private static final int BUTTON_X = 98;
    private static final int BUTTON_Y = 117;
    private static final int BUTTON_W = 70;
    private static final int BUTTON_H = 24;

    private static final int STATUS_X = 14;
    private static final int STATUS_Y = 124;

    private static final int HOSE_INFO_Y = 22;

    private Button toggleButton;

    public HookahScreen(HookahMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 266;
        this.titleLabelY = 6;
        this.titleLabelX = 8;
        this.inventoryLabelY = 180;
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
        PacketDistributor.sendToServer(new ToggleMouthpiecePayload(menu.getPos()));
    }

    private HookahBlockEntity be() { return menu.getBlockEntity(); }

    private UUID myUuid() {
        return Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getUUID();
    }

    private boolean isInUseByMe() {
        HookahBlockEntity be = be();
        UUID my = myUuid();
        return be != null && my != null && my.equals(be.getActivePlayerUuid());
    }

    private boolean isBusyByOther() {
        HookahBlockEntity be = be();
        return be != null && be.isInUse() && !isInUseByMe();
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

    @Override
    protected void renderBg(GuiGraphics gg, float partialTicks, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gg.blit(BG_TEXTURE, x, y, 0, 0, KALYAN_W, KALYAN_H, 256, 256);

        int invTop = y + KALYAN_H;
        gg.fill(x, invTop, x + this.imageWidth, y + this.imageHeight, 0xFF1F140A);
        gg.fill(x + 2, invTop + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, 0xFF3A2516);
        gg.fill(x + 5, invTop + 5, x + this.imageWidth - 5, y + this.imageHeight - 5, 0xFF1F140A);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(gg, x + 8 + col * 18, y + HookahMenu.INV_ROW_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBg(gg, x + 8 + col * 18, y + HookahMenu.INV_HOTBAR_Y);
        }
    }

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

        HookahBlockEntity be = be();
        HookahHoseType type = be != null ? be.getHoseType() : HookahHoseType.NONE;
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

        Component btnText = toggleButton.getMessage();
        int btnW = this.font.width(btnText);
        int btnColor = toggleButton.active ? 0xFF1F140A : 0xFF5C4A36;
        gg.drawString(this.font, btnText, BUTTON_X + (BUTTON_W - btnW) / 2, BUTTON_Y + (BUTTON_H - 8) / 2, btnColor, false);

        if (isInUseByMe() && be != null && !be.hasAllConsumables()) {
            Component hint = Component.translatable("gui.hookahmod.fill_slots").withStyle(ChatFormatting.ITALIC);
            int hwt = this.font.width(hint);
            gg.drawString(this.font, hint, (this.imageWidth - hwt) / 2, 156, 0xFFB0B0B0, false);
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
                gg.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x30FFFFFF);
            }
        }
    }
}

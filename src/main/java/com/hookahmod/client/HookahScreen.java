package com.hookahmod.client;

import com.hookahmod.HookahMod;
import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.menu.HookahMenu;
import com.hookahmod.network.ToggleMouthpiecePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public class HookahScreen extends AbstractContainerScreen<HookahMenu> {

    private static final ResourceLocation TEXTURE = HookahMod.id("textures/gui/hookah.png");

    private Button toggleButton;

    public HookahScreen(HookahMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.toggleButton = Button.builder(Component.translatable("gui.hookahmod.take_mouthpiece"), btn -> onToggle())
                .bounds(this.leftPos + 38, this.topPos + 60, 100, 20)
                .build();
        this.addRenderableWidget(toggleButton);
    }

    private void onToggle() {
        PacketDistributor.sendToServer(new ToggleMouthpiecePayload(menu.getPos()));
    }

    private boolean isInUseByMe() {
        if (Minecraft.getInstance().player == null) return false;
        UUID my = Minecraft.getInstance().player.getUUID();
        HookahBlockEntity be = menu.getBlockEntity(Minecraft.getInstance().player);
        return be != null && my.equals(be.getActivePlayerUuid());
    }

    private boolean isBusy() {
        HookahBlockEntity be = menu.getBlockEntity(Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player);
        return be != null && be.isInUse() && !isInUseByMe();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (isInUseByMe()) {
            toggleButton.setMessage(Component.translatable("gui.hookahmod.release_mouthpiece"));
        } else {
            toggleButton.setMessage(Component.translatable("gui.hookahmod.take_mouthpiece"));
        }
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTicks, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gg.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF2A1810);
        gg.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFF1A0E08);
        // slot frames
        int sx = x + 43;
        int sy = y + 34;
        for (int i = 0; i < 3; i++) {
            int slotX = x + 43 + i * 36;
            gg.fill(slotX, sy, slotX + 18, sy + 18, 0xFF000000);
            gg.fill(slotX + 1, sy + 1, slotX + 17, sy + 17, 0xFF8B7355);
        }
        // status text
        Component status;
        if (isInUseByMe()) status = Component.translatable("gui.hookahmod.status_yours");
        else if (isBusy()) status = Component.translatable("gui.hookahmod.status_busy");
        else status = Component.translatable("gui.hookahmod.status_free");
        gg.drawCenteredString(this.font, status, x + this.imageWidth / 2, y + 18, 0xFFFFFFFF);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(gg, mouseX, mouseY, partialTicks);
        super.render(gg, mouseX, mouseY, partialTicks);
        this.renderTooltip(gg, mouseX, mouseY);
    }
}

package com.carrot123.yixiangshouronglu.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class GeneScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private float scale = 1;
    protected GeneScreen(T menu, Inventory inventory, Component title, int panelWidth, int panelHeight) { super(menu, inventory, title); imageWidth = panelWidth; imageHeight = panelHeight; inventoryLabelY = -10000; }
    @Override protected void init() {
        scale = Math.min(1.0F, Math.min((float) width / (imageWidth + 12), (float) height / (imageHeight + 12)));
        width = (int) (width / scale); height = (int) (height / scale); super.init(); widgets();
    }
    protected abstract void widgets();
    protected void panel(GuiGraphics graphics) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF121B25);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 3, 0xFF4B9CA7);
        graphics.fill(leftPos + 1, topPos + imageHeight - 3, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF4B9CA7);
    }
    protected void slot(GuiGraphics graphics, int x, int y, int color) { graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, color); graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF080E15); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose(); graphics.pose().scale(scale, scale, 1);
        renderBackground(graphics); super.render(graphics, (int) (mouseX / scale), (int) (mouseY / scale), partialTick);
        renderTooltip(graphics, (int) (mouseX / scale), (int) (mouseY / scale)); graphics.pose().popPose();
    }
    @Override public boolean mouseClicked(double x, double y, int button) { return clickScaled(x / scale, y / scale, button); }
    protected boolean clickScaled(double x, double y, int button) { return super.mouseClicked(x, y, button); }
    @Override public boolean mouseReleased(double x, double y, int button) { return super.mouseReleased(x / scale, y / scale, button); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { return dragScaled(x / scale, y / scale, button, dx / scale, dy / scale); }
    protected boolean dragScaled(double x, double y, int button, double dx, double dy) { return super.mouseDragged(x, y, button, dx, dy); }
    @Override public boolean mouseScrolled(double x, double y, double delta) { return scrollScaled(x / scale, y / scale, delta); }
    protected boolean scrollScaled(double x, double y, double delta) { return super.mouseScrolled(x, y, delta); }
}

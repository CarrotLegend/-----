package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.menu.ContainmentDeviceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ContainmentDeviceScreen extends AbstractContainerScreen<ContainmentDeviceMenu> {
    public ContainmentDeviceScreen(ContainmentDeviceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 190;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ChimeraDiagram.diamond(graphics, leftPos + 88, topPos + 60);
        for (int index = 0; index < 36; index++) {
            int x = leftPos + 8 + index % 9 * 18;
            int y = topPos + (index < 27 ? 108 + index / 9 * 18 : 166);
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0x88334958);
            graphics.fill(x, y, x + 16, y + 16, 0xA0080E15);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}

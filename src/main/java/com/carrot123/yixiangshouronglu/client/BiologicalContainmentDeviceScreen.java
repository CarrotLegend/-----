package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.menu.BiologicalContainmentDeviceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static com.carrot123.yixiangshouronglu.menu.BiologicalContainmentDeviceMenu.*;

public final class BiologicalContainmentDeviceScreen extends AbstractContainerScreen<BiologicalContainmentDeviceMenu> {
    public BiologicalContainmentDeviceScreen(BiologicalContainmentDeviceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 162;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ChimeraDiagram.diamond(graphics, leftPos + RESULT_X + 8, topPos + RESULT_Y + 8);
        for (int index = 0; index < 27; index++) {
            drawInventorySlot(graphics, INVENTORY_X + index % 9 * 18, INVENTORY_Y + index / 9 * 18);
        }
        for (int index = 0; index < 9; index++) {
            drawInventorySlot(graphics, INVENTORY_X + index * 18, INVENTORY_Y + 58);
        }
    }

    private void drawInventorySlot(GuiGraphics graphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        graphics.fill(left - 1, top - 1, left + 17, top + 17, 0x99445968);
        graphics.fill(left, top, left + 16, top + 16, 0x88080E15);
    }
}

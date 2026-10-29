package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.menu.ContainmentChamberMenu;
import com.carrot123.yixiangshouronglu.production.ChamberProduction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ContainmentChamberScreen extends AbstractContainerScreen<ContainmentChamberMenu> {
    public ContainmentChamberScreen(ContainmentChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 193;
        inventoryLabelX = 8;
        inventoryLabelY = 99;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xF018202B);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFFB69C6B);
        graphics.fill(x + 1, y + imageHeight - 3, x + imageWidth - 1, y + imageHeight - 2, 0xFFB69C6B);
        graphics.fill(x + 7, y + 34, x + imageWidth - 7, y + 76, 0xFF101722);
        graphics.fill(x + 7, y + 96, x + imageWidth - 7, y + 97, 0xFF786B55);

        for (int index = 0; index < 2; index++) {
            int slotX = x + 69 + index * 18;
            graphics.fill(slotX, y + 44, slotX + 18, y + 62, 0xFFBDA474);
            graphics.fill(slotX + 1, y + 45, slotX + 17, y + 61, 0xFF292F3A);
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawInventorySlot(graphics, x + 7 + column * 18, y + 109 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawInventorySlot(graphics, x + 7 + column * 18, y + 167);
        }

        ChamberProduction.Recipe recipe = ChamberProduction.forEntity(menu.getEntityId());
        if (recipe != null) {
            Component rate = Component.translatable(
                    "screen.yixiangshouronglu.chamber.rate",
                    new ItemStack(recipe.item()).getHoverName(),
                    Component.translatable(recipe.rateKey())
            );
            graphics.drawCenteredString(font, rate, x + imageWidth / 2, y + 79, 0xFFE5C994);

            int interval = menu.getInterval();
            if (interval > 0) {
                int width = Math.max(0, Math.min(100, menu.getProgress() * 100 / interval));
                graphics.fill(x + 38, y + 89, x + 138, y + 94, 0xFF353C48);
                graphics.fill(x + 38, y + 89, x + 38 + width, y + 94, 0xFFC6A363);
            }
        } else {
            graphics.drawCenteredString(font, Component.translatable("screen.yixiangshouronglu.chamber.no_production"), x + imageWidth / 2, y + 79, 0xFFB3B5BE);
        }
    }

    private void drawInventorySlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF626A77);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF252D39);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component entity = menu.getEntityId().isBlank()
                ? title
                : SoulItem.getEntityName(menu.getEntityId());
        graphics.drawCenteredString(font, entity, imageWidth / 2, 15, 0xFFF1D59D);
        graphics.drawString(font, playerInventoryTitle, 8, 99, 0xFFDBDDE2, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}

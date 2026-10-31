package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.block.entity.ContainmentChamberBlockEntity;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.menu.ContainmentChamberMenu;
import com.carrot123.yixiangshouronglu.production.ChamberProduction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ContainmentChamberScreen extends AbstractContainerScreen<ContainmentChamberMenu> {
    private static final int TAB_WIDTH = 64;
    private static final int TAB_HEIGHT = 20;

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

        if (menu.getPage() == ContainmentChamberMenu.PAGE_EXTRACTION) {
            renderExtractionSlots(graphics, x, y);
        } else {
            for (int index = 0; index < 2; index++) {
                int slotX = x + 69 + index * 18;
                graphics.fill(slotX, y + 44, slotX + 18, y + 62, 0xFFBDA474);
                graphics.fill(slotX + 1, y + 45, slotX + 17, y + 61, 0xFF292F3A);
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawInventorySlot(graphics, x + 7 + column * 18, y + 109 + row * 18);
            }
        }

        for (int column = 0; column < 9; column++) {
            drawInventorySlot(graphics, x + 7 + column * 18, y + 167);
        }

        if (menu.getPage() == ContainmentChamberMenu.PAGE_EXTRACTION) {
            renderExtraction(graphics, x, y);
        } else {
            renderProduction(graphics, x, y);
        }

        renderTabs(graphics, mouseX, mouseY);
    }

    private void renderProduction(GuiGraphics graphics, int x, int y) {
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
            graphics.drawCenteredString(
                    font,
                    Component.translatable("screen.yixiangshouronglu.chamber.no_production"),
                    x + imageWidth / 2,
                    y + 79,
                    0xFFB3B5BE
            );
        }
    }

    private void renderExtractionSlots(GuiGraphics graphics, int x, int y) {
        drawSpecialSlot(graphics, x + 50, y + 44);
        drawSpecialSlot(graphics, x + 110, y + 44);
    }

    private void renderExtraction(GuiGraphics graphics, int x, int y) {
        int progress = Math.max(0, Math.min(20, menu.getExtractionProgress()));

        graphics.fill(x + 75, y + 51, x + 104, y + 55, 0xFF353C48);
        graphics.fill(x + 99, y + 48, x + 106, y + 58, 0xFF353C48);

        if (progress > 0) {
            int width = progress * 29 / 20;
            graphics.fill(x + 75, y + 51, x + 75 + width, y + 55, 0xFFC6A363);
        }

        Component amount = Component.translatable(
                "screen.yixiangshouronglu.chamber.extract_amount",
                menu.getExtractStoredMb(),
                ContainmentChamberBlockEntity.MAX_EXTRACT_MB
        );

        graphics.drawCenteredString(font, amount, x + imageWidth / 2, y + 79, 0xFFE5C994);

        int width = Math.max(
                0,
                Math.min(
                        100,
                        menu.getExtractStoredMb() * 100
                                / ContainmentChamberBlockEntity.MAX_EXTRACT_MB
                )
        );

        graphics.fill(x + 38, y + 89, x + 138, y + 94, 0xFF353C48);
        graphics.fill(x + 38, y + 89, x + 38 + width, y + 94, 0xFFC6A363);
    }

    private void drawSpecialSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFFBDA474);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF292F3A);
    }

    private void drawInventorySlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF626A77);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF252D39);
    }

    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        renderTab(
                graphics,
                ContainmentChamberMenu.PAGE_PRODUCTION,
                Component.translatable("screen.yixiangshouronglu.chamber.production_tab"),
                0,
                mouseX,
                mouseY
        );

        renderTab(
                graphics,
                ContainmentChamberMenu.PAGE_EXTRACTION,
                Component.translatable("screen.yixiangshouronglu.chamber.extraction_tab"),
                1,
                mouseX,
                mouseY
        );
    }

    private void renderTab(
            GuiGraphics graphics,
            int page,
            Component label,
            int index,
            int mouseX,
            int mouseY
    ) {
        int x = getTabX();
        int y = getTabY(index);
        boolean selected = menu.getPage() == page;
        boolean hovered = isOverTab(mouseX, mouseY, index);

        int borderColor = selected ? 0xFFE5C994 : 0xFF786B55;
        int backgroundColor = selected ? 0xFF292F3A : hovered ? 0xFF353C48 : 0xFF18202B;
        int textColor = selected ? 0xFFF1D59D : hovered ? 0xFFE5C994 : 0xFFDBDDE2;

        graphics.fill(x, y, x + TAB_WIDTH, y + TAB_HEIGHT, borderColor);
        graphics.fill(x + 1, y + 1, x + TAB_WIDTH - 1, y + TAB_HEIGHT - 1, backgroundColor);

        graphics.drawCenteredString(
                font,
                label,
                x + TAB_WIDTH / 2,
                y + (TAB_HEIGHT - font.lineHeight) / 2,
                textColor
        );
    }

    private int getTabX() {
        if (width - leftPos - imageWidth >= TAB_WIDTH + 4) {
            return leftPos + imageWidth + 3;
        }

        return Math.max(0, leftPos - TAB_WIDTH - 3);
    }

    private int getTabY(int index) {
        return topPos + 12 + index * 24;
    }

    private boolean isOverTab(double mouseX, double mouseY, int index) {
        int x = getTabX();
        int y = getTabY(index);

        return mouseX >= x
                && mouseX < x + TAB_WIDTH
                && mouseY >= y
                && mouseY < y + TAB_HEIGHT;
    }

    private void changePage(int page) {
        if (minecraft == null
                || minecraft.player == null
                || minecraft.gameMode == null
                || menu.getPage() == page) {
            return;
        }

        menu.clickMenuButton(minecraft.player, page);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, page);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                changePage(ContainmentChamberMenu.PAGE_PRODUCTION);
                return true;
            }

            if (isOverTab(mouseX, mouseY, 1)) {
                changePage(ContainmentChamberMenu.PAGE_EXTRACTION);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
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

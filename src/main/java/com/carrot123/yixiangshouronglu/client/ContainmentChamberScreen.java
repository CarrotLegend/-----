package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.block.entity.ContainmentChamberBlockEntity;
import com.carrot123.yixiangshouronglu.menu.ContainmentChamberMenu;
import com.carrot123.yixiangshouronglu.production.ChamberProduction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ContainmentChamberScreen extends AbstractContainerScreen<ContainmentChamberMenu> {
    private Button productionButton;
    private Button extractionButton;

    public ContainmentChamberScreen(
            ContainmentChamberMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);

        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 77;
    }

    @Override
    protected void init() {
        super.init();

        productionButton = addRenderableWidget(
                Button.builder(
                        Component.translatable("screen.yixiangshouronglu.chamber.production_tab"),
                        button -> changePage(ContainmentChamberMenu.PAGE_PRODUCTION)
                ).bounds(leftPos + 7, topPos + 20, 78, 17).build()
        );

        extractionButton = addRenderableWidget(
                Button.builder(
                        Component.translatable("screen.yixiangshouronglu.chamber.extraction_tab"),
                        button -> changePage(ContainmentChamberMenu.PAGE_EXTRACTION)
                ).bounds(leftPos + 91, topPos + 20, 78, 17).build()
        );

        updateButtons();
    }

    private void changePage(int page) {
        if (minecraft == null
                || minecraft.player == null
                || minecraft.gameMode == null) {
            return;
        }

        if (menu.getPage() == page) {
            return;
        }

        menu.clickMenuButton(minecraft.player, page);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, page);

        updateButtons();
    }

    private void updateButtons() {
        productionButton.active = menu.getPage() != ContainmentChamberMenu.PAGE_PRODUCTION;
        extractionButton.active = menu.getPage() != ContainmentChamberMenu.PAGE_EXTRACTION;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        graphics.fill(
                leftPos,
                topPos,
                leftPos + imageWidth,
                topPos + imageHeight,
                0xFF373737
        );

        graphics.fill(
                leftPos + 3,
                topPos + 3,
                leftPos + imageWidth - 3,
                topPos + imageHeight - 3,
                0xFFC6C6C6
        );

        graphics.fill(
                leftPos + 5,
                topPos + 39,
                leftPos + 171,
                topPos + 79,
                0xFFADADAD
        );

        if (menu.getPage() == ContainmentChamberMenu.PAGE_EXTRACTION) {
            renderExtraction(graphics);
        } else {
            renderProduction(graphics);
        }

        renderInventorySlots(graphics);
    }

    private void renderProduction(GuiGraphics graphics) {
        renderSlotBackground(graphics, 59, 46);
        renderSlotBackground(graphics, 99, 46);

        int interval = menu.getProductionInterval();
        int progress = menu.getProductionProgress();

        int filled = interval > 0
                ? Math.min(20, Math.max(0, progress * 20 / interval))
                : 0;

        renderArrow(graphics, filled);
    }

    private void renderExtraction(GuiGraphics graphics) {
        renderSlotBackground(graphics, 59, 46);
        renderSlotBackground(graphics, 99, 46);

        int processing = menu.getExtractionProgress();
        int filled = Math.min(20, Math.max(0, processing));

        renderArrow(graphics, filled);

        int stored = menu.getExtractStoredMb();
        int barWidth = Math.min(
                158,
                Math.max(0, stored * 158 / ContainmentChamberBlockEntity.MAX_EXTRACT_MB)
        );

        graphics.fill(
                leftPos + 8,
                topPos + 69,
                leftPos + 168,
                topPos + 75,
                0xFF494949
        );

        if (barWidth > 0) {
            graphics.fill(
                    leftPos + 9,
                    topPos + 70,
                    leftPos + 9 + barWidth,
                    topPos + 74,
                    0xFF8B54C7
            );
        }
    }

    private void renderArrow(GuiGraphics graphics, int filled) {
        int startX = leftPos + 79;
        int startY = topPos + 48;

        graphics.fill(startX, startY, startX + 17, startY + 4, 0xFF777777);
        graphics.fill(startX + 13, startY - 3, startX + 17, startY + 7, 0xFF777777);

        if (filled > 0) {
            int width = Math.min(17, filled * 17 / 20);
            graphics.fill(
                    startX,
                    startY,
                    startX + width,
                    startY + 4,
                    0xFF6EBF75
            );
        }
    }

    private void renderInventorySlots(GuiGraphics graphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotBackground(
                        graphics,
                        8 + column * 18,
                        84 + row * 18
                );
            }
        }

        for (int column = 0; column < 9; column++) {
            renderSlotBackground(graphics, 8 + column * 18, 142);
        }
    }

    private void renderSlotBackground(GuiGraphics graphics, int x, int y) {
        int screenX = leftPos + x;
        int screenY = topPos + y;

        graphics.fill(
                screenX - 1,
                screenY - 1,
                screenX + 17,
                screenY + 17,
                0xFF555555
        );

        graphics.fill(
                screenX,
                screenY,
                screenX + 16,
                screenY + 16,
                0xFF8B8B8B
        );
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        if (menu.getPage() == ContainmentChamberMenu.PAGE_EXTRACTION) {
            graphics.drawString(
                    font,
                    Component.translatable(
                            "screen.yixiangshouronglu.chamber.extract_amount",
                            menu.getExtractStoredMb(),
                            ContainmentChamberBlockEntity.MAX_EXTRACT_MB
                    ),
                    8,
                    59,
                    0x404040,
                    false
            );
        } else {
            ChamberProduction.Recipe recipe = menu.getProductionRecipe();

            Component rate = recipe == null
                    ? Component.translatable("screen.yixiangshouronglu.chamber.no_production")
                    : Component.translatable(recipe.rateKey());

            graphics.drawString(font, rate, 8, 64, 0x404040, false);
        }
    }
}
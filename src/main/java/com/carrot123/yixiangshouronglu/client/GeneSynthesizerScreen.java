package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.gene.GeneTime;
import com.carrot123.yixiangshouronglu.menu.GeneSynthesizerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import com.carrot123.yixiangshouronglu.gene.recipe.ChimeraRecipe;

import static com.carrot123.yixiangshouronglu.menu.GeneSynthesizerMenu.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class GeneSynthesizerScreen extends GeneScreen<GeneSynthesizerMenu> {
    private int tabWidth;

    public GeneSynthesizerScreen(GeneSynthesizerMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, 266, 210); }
    @Override protected int extraHorizontalSpace() { return 2 * (tabWidth() + 4); }

    @Override protected void widgets() {
        tabWidth = tabWidth();
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        panel(graphics);
        boolean chimera = menu.page() == PAGE_CHIMERA;
        if (chimera) {
            drawChimera(graphics);
        } else {
            for (int index = 0; index < 4; index++) { slot(graphics, 22 + index % 2 * 22, 48 + index / 2 * 22, 0xFF445968); }
            slot(graphics, 142, 59, 0xFFB69A53);
        }
        for (int row = 0; row < 3; row++) { for (int col = 0; col < 9; col++) { slot(graphics, 51 + col * 18, 121 + row * 18, 0xFF334958); } }
        for (int col = 0; col < 9; col++) { slot(graphics, 51 + col * 18, 179, 0xFF334958); }
        for (int page = 0; page < 2; page++) {
            Rect2i area = tab(page);
            boolean selected = menu.page() == page;
            boolean hovered = area.contains(mouseX, mouseY);
            int x = area.getX();
            int y = area.getY();
            graphics.fill(x, y, x + tabWidth, y + 20, selected ? 0xFFE5C994 : 0xFF786B55);
            graphics.fill(x + 1, y + 1, x + tabWidth - 1, y + 19,
                    selected ? 0xFF292F3A : hovered ? 0xFF353C48 : 0xFF18202B);
            graphics.drawCenteredString(font, tabLabel(page), x + tabWidth / 2, y + 6,
                    selected ? 0xFFF1D59D : hovered ? 0xFFE5C994 : 0xFFDBDDE2);
        }
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 7, 0xE4F1FA, false);
        if (menu.page() == PAGE_CHIMERA) {
            graphics.drawCenteredString(font, Component.translatable("gene.yixiangshouronglu.progress",
                    GeneTime.seconds(menu.chimeraProgress()), GeneTime.totalSeconds(ChimeraRecipe.DURATION)), 133, 108, 0xFFAAB9C8);
            if (!menu.chimeraMatched()) {
                graphics.drawCenteredString(font, Component.translatable("gui.yixiangshouronglu.chimera_no_match"),
                        133, 83, 0xFFAAB9C8);
            }
            return;
        }
        graphics.drawCenteredString(font, Component.translatable("gui.yixiangshouronglu.legacy_inventory_hint"),
                133, 100, 0xFFAAB9C8);
    }

    private void drawChimera(GuiGraphics graphics) {
        int centerX = leftPos + chimeraX(4) + 8;
        int centerY = topPos + chimeraY(4) + 8;
        for (int index = 0; index < 4; index++) {
            int x = leftPos + chimeraX(index) + 8;
            int y = topPos + chimeraY(index) + 8;
            ChimeraDiagram.line(graphics, x, y, centerX, centerY, menu.chimeraProgress(), ChimeraRecipe.DURATION);
            slot(graphics, chimeraX(index), chimeraY(index), 0xFF445968);
        }
        ChimeraDiagram.diamond(graphics, centerX, centerY);
    }

    private Component tabLabel(int page) {
        return Component.translatable(page == PAGE_GENE ? "gui.yixiangshouronglu.gene_synthesis_tab" : "gui.yixiangshouronglu.chimera_tab");
    }

    private int tabWidth() {
        return Math.max(84, Math.max(font.width(tabLabel(0)), font.width(tabLabel(1))) + 8);
    }

    private boolean tabsOnTop() {
        return width - leftPos - imageWidth < tabWidth + 3 && leftPos < tabWidth + 3;
    }

    private Rect2i tab(int page) {
        if (tabsOnTop()) {
            return new Rect2i(leftPos + 8 + page * (tabWidth + 3), Math.max(0, topPos - 21), tabWidth, 20);
        }
        int x = width - leftPos - imageWidth >= tabWidth + 3 ? leftPos + imageWidth + 3 : leftPos - tabWidth - 3;
        return new Rect2i(x, topPos + 12 + page * 24, tabWidth, 20);
    }

    public Rect2i scaledTab(int page) {
        Rect2i area = tab(page);
        return new Rect2i(Math.round(area.getX() * guiScale()), Math.round(area.getY() * guiScale()),
                Math.round(area.getWidth() * guiScale()), Math.round(area.getHeight() * guiScale()));
    }

    @Override protected boolean clickScaled(double x, double y, int button) {
        if (button == 0) {
            for (int page = 0; page < 2; page++) {
                if (tab(page).contains((int) x, (int) y)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, PAGE_BUTTON + page);
                    return true;
                }
            }
        }
        return super.clickScaled(x, y, button);
    }
}

package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.gene.GeneTime;
import com.carrot123.yixiangshouronglu.block.entity.GeneSynthesizerBlockEntity;
import com.carrot123.yixiangshouronglu.menu.GeneSynthesizerMenu;
import net.minecraft.client.gui.GuiGraphics;
import com.carrot123.yixiangshouronglu.gene.recipe.ChimeraRecipe;

import static com.carrot123.yixiangshouronglu.menu.GeneSynthesizerMenu.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class GeneSynthesizerScreen extends GeneScreen<GeneSynthesizerMenu> {
    private static final Component RESEARCH = Component.translatable("gui.yixiangshouronglu.biological_research_page");
    private static final Component CHIMERA = Component.translatable("gui.yixiangshouronglu.chimera_page");
    private static final int TAB_Y = 4;
    private static final int TAB_HEIGHT = 17;

    public GeneSynthesizerScreen(GeneSynthesizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 266, 210);
    }

    @Override protected void widgets() {
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        panel(graphics);
        drawTab(graphics, RESEARCH_PAGE, RESEARCH);
        drawTab(graphics, CHIMERA_PAGE, CHIMERA);
        if (menu.page() == CHIMERA_PAGE) {
            drawChimera(graphics);
        } else {
            drawResearch(graphics);
        }
        for (int row = 0; row < 3; row++) { for (int col = 0; col < 9; col++) { slot(graphics, 51 + col * 18, 121 + row * 18, 0xFF334958); } }
        for (int col = 0; col < 9; col++) { slot(graphics, 51 + col * 18, 179, 0xFF334958); }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.page() == RESEARCH_PAGE) {
            return;
        }
        graphics.drawCenteredString(font, Component.translatable("gene.yixiangshouronglu.progress",
                GeneTime.seconds(menu.chimeraProgress()), GeneTime.totalSeconds(ChimeraRecipe.DURATION)), 133, 108, 0xFFAAB9C8);
        if (!menu.chimeraMatched()) {
            graphics.drawCenteredString(font, Component.translatable("gui.yixiangshouronglu.chimera_no_match"),
                    133, 83, 0xFFAAB9C8);
        }
    }

    private void drawResearch(GuiGraphics graphics) {
        slot(graphics, RESEARCH_INPUT_X, RESEARCH_Y, 0xFF445968);
        slot(graphics, RESEARCH_OUTPUT_X, RESEARCH_Y, 0xFF445968);
        int width = 72 * Math.max(0, Math.min(menu.researchProgress(), GeneSynthesizerBlockEntity.RESEARCH_DURATION))
                / GeneSynthesizerBlockEntity.RESEARCH_DURATION;
        graphics.fill(leftPos + 96, topPos + 60, leftPos + 170, topPos + 64, 0xFF263B47);
        if (width > 0) {
            graphics.fill(leftPos + 97, topPos + 61, leftPos + 97 + width, topPos + 63, 0xFF60CCD4);
        }
    }

    private int tabX(int page) { return 8 + page * 125; }

    private void drawTab(GuiGraphics graphics, int page, Component label) {
        int x = leftPos + tabX(page);
        int y = topPos + TAB_Y;
        boolean selected = menu.page() == page;
        graphics.fill(x, y, x + 120, y + TAB_HEIGHT, selected ? 0xFF304E59 : 0xFF192835);
        graphics.drawCenteredString(font, label, x + 60, y + 5, selected ? 0xFF8DE6EC : 0xFFD1DFE8);
    }

    @Override protected boolean clickScaled(double x, double y, int button) {
        if (button == 0 && y >= topPos + TAB_Y && y < topPos + TAB_Y + TAB_HEIGHT) {
            for (int page = RESEARCH_PAGE; page <= CHIMERA_PAGE; page++) {
                int start = leftPos + tabX(page);
                if (x >= start && x < start + 120) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, page);
                    return true;
                }
            }
        }
        return super.clickScaled(x, y, button);
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

}

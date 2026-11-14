package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.menu.GeneModificationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import static com.carrot123.yixiangshouronglu.menu.GeneModificationMenu.*;

public final class GeneModificationScreen extends GeneScreen<GeneModificationMenu> {
    private static final String LABEL = "screen.yixiangshouronglu.gene_modification.";
    private static final ResourceLocation DNA = new ResourceLocation("yixiangshouronglu", "textures/gui/gene_modification_dna.png");
    private static final String[] LIMB_LABELS = {"head", "body", "hand", "foot"};

    public GeneModificationScreen(GeneModificationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 266, 190);
    }

    @Override protected void widgets() {
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        panel(graphics);
        if (menu.page() == GENES) {
            graphics.blit(DNA, leftPos + 24, topPos + 38, 0, 0, 218, 45, 218, 45);
        }
        int start = menu.page() == GENES ? 0 : GENE_SLOTS;
        int end = menu.page() == GENES ? GENE_SLOTS : GENE_SLOTS + LIMB_LABELS.length;
        for (int index = start; index < end; index++) {
            slot(graphics, slotX(index), slotY(index), 0xFF4B9CA7);
        }
        for (int index = 0; index < 27; index++) {
            slot(graphics, INVENTORY_X + index % 9 * 18, INVENTORY_Y + index / 9 * 18, 0xFF334958);
        }
        for (int index = 0; index < 9; index++) {
            slot(graphics, INVENTORY_X + index * 18, INVENTORY_Y + 58, 0xFF334958);
        }
        for (int page = GENES; page <= LIMBS; page++) {
            int x = leftPos + tabX(page);
            int y = topPos + 5;
            boolean selected = menu.page() == page;
            boolean hovered = mouseX >= x && mouseX < x + 45 && mouseY >= y && mouseY < y + 17;
            graphics.fill(x, y, x + 45, y + 17, selected ? 0xFFE5C994 : 0xFF786B55);
            graphics.fill(x + 1, y + 1, x + 44, y + 16, hovered || selected ? 0xFF353C48 : 0xFF18202B);
            graphics.drawCenteredString(font, Component.translatable(LABEL + (page == GENES ? "gene_tab" : "limb_tab")),
                    x + 22, y + 5, selected ? 0xFFF1D59D : 0xFFDBDDE2);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 9, 9, 0xE4F1FA, false);
        if (menu.page() == LIMBS) {
            for (int index = 0; index < LIMB_LABELS.length; index++) {
                int slot = GENE_SLOTS + index;
                graphics.drawString(font, Component.translatable(LABEL + LIMB_LABELS[index]),
                        slotX(slot) + 23, slotY(slot) + 4, 0xE4F1FA, false);
            }
        }
    }

    private static int tabX(int page) { return 166 + 47 * page; }

    @Override protected boolean clickScaled(double x, double y, int button) {
        if (button == 0 && y >= topPos + 5 && y < topPos + 22) {
            for (int page = GENES; page <= LIMBS; page++) {
                int left = leftPos + tabX(page);
                if (x >= left && x < left + 45) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, page);
                    return true;
                }
            }
        }
        return super.clickScaled(x, y, button);
    }
}

package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.gene.GeneTime;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import com.carrot123.yixiangshouronglu.menu.GeneSynthesizerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class GeneSynthesizerScreen extends GeneScreen<GeneSynthesizerMenu> {
    public GeneSynthesizerScreen(GeneSynthesizerMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, 266, 210); }
    @Override protected void widgets() {
        for (int mode = 0; mode < 2; mode++) {
            final int selected = mode;
            addRenderableWidget(Button.builder(Component.translatable("gene.yixiangshouronglu.mode." + (mode == 0 ? "synthesis" : "upgrade")), button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selected)).bounds(leftPos + 95 + mode * 81, topPos + 17, 78, 18).build());
        }
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        panel(graphics);
        for (int index = 0; index < 4; index++) { slot(graphics, 22 + index % 2 * 22, 48 + index / 2 * 22, 0xFF445968); }
        slot(graphics, 142, 59, 0xFFB69A53);
        graphics.fill(leftPos + 77, topPos + 63, leftPos + 128, topPos + 73, 0xFF070D12);
        int fill = menu.duration() <= 0 ? 0 : (int) (49L * Math.max(0, Math.min(menu.progress(), menu.duration())) / menu.duration());
        graphics.fill(leftPos + 78, topPos + 64, leftPos + 78 + fill, topPos + 72, 0xFFE1B65A);
        for (int row = 0; row < 3; row++) { for (int col = 0; col < 9; col++) { slot(graphics, 51 + col * 18, 121 + row * 18, 0xFF334958); } }
        for (int col = 0; col < 9; col++) { slot(graphics, 51 + col * 18, 179, 0xFF334958); }
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 7, 0xE4F1FA, false);
        graphics.drawString(font, Component.translatable("gene.yixiangshouronglu.mode." + menu.mode().name().toLowerCase(java.util.Locale.ROOT)), 12, 100, 0x7ED6D4, false);
        if (!menu.preview().isEmpty()) {
            var name = font.split(menu.preview().getHoverName(), 86);
            for (int line = 0; line < Math.min(2, name.size()); line++) { graphics.drawString(font, name.get(line), 171, 45 + line * 9, 0xE4F1FA, false); }
            var data = GeneModuleItem.read(menu.preview());
            if (data != null) { graphics.drawString(font, Component.translatable(data.strength().key()), 171, 69, 0xE5BD68, false); graphics.drawString(font, Component.translatable(data.quality().key()), 171, 82, 0xE5BD68, false); }
        }
        graphics.drawString(font, Component.translatable("gene.yixiangshouronglu.progress", GeneTime.seconds(menu.progress()), GeneTime.seconds(menu.duration())), 77, 84, 0xAAB9C8, false);
    }
    @Override protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        if (!menu.preview().isEmpty() && isHovering(169, 43, 90, 58, mouseX, mouseY)) { graphics.renderTooltip(font, menu.preview(), mouseX, mouseY); }
    }

}

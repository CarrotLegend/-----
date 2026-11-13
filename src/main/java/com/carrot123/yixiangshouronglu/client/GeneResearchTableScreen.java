package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.gene.research.GeneResearchCapability;
import com.carrot123.yixiangshouronglu.gene.research.GeneResearchDefinition;
import com.carrot123.yixiangshouronglu.gene.research.GeneResearchRegistry;
import com.carrot123.yixiangshouronglu.gene.research.GeneResearchService;
import com.carrot123.yixiangshouronglu.gene.research.PlayerGeneResearch;
import com.carrot123.yixiangshouronglu.menu.GeneResearchTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

import static com.carrot123.yixiangshouronglu.menu.GeneResearchTableMenu.*;

public final class GeneResearchTableScreen extends GeneScreen<GeneResearchTableMenu> {
    private static final int LIST_X = 12;
    private static final int LIST_Y = 48;
    private static final int LIST_WIDTH = 122;
    private static final int ROW_HEIGHT = 16;
    private static final int ROWS = 7;
    private final Inventory inventory;
    private Button action;
    private int scroll;
    private int lastPage = -1;

    public GeneResearchTableScreen(GeneResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 302, 260);
        this.inventory = inventory;
    }

    @Override protected void widgets() {
        addRenderableWidget(Button.builder(label("tab_research"), button -> menu.request(CHANGE_PAGE, PAGE_RESEARCH, null))
                .bounds(leftPos + 12, topPos + 23, 60, 18).build());
        addRenderableWidget(Button.builder(label("tab_synthesis"), button -> menu.request(CHANGE_PAGE, PAGE_SYNTHESIS, null))
                .bounds(leftPos + 76, topPos + 23, 60, 18).build());
        action = addRenderableWidget(Button.builder(label("button_research"), button -> menu.request(RUN, menu.page(), menu.selection()))
                .bounds(leftPos + 177, topPos + 141, 102, 18).build());
        action.active = false;
    }

    private PlayerGeneResearch data() {
        return GeneResearchCapability.get(inventory.player);
    }

    private List<GeneResearchDefinition> entries() {
        PlayerGeneResearch data = data();
        return GeneResearchRegistry.values().stream().filter(definition -> menu.page() == PAGE_RESEARCH
                || data != null && data.progress(definition.id()) >= 100).toList();
    }

    private GeneResearchDefinition selected() {
        return GeneResearchRegistry.get(menu.selection());
    }

    @Override protected void containerTick() {
        super.containerTick();
        if (lastPage != menu.page()) {
            lastPage = menu.page();
            scroll = 0;
        }
        GeneResearchDefinition definition = selected();
        PlayerGeneResearch data = data();
        boolean research = menu.page() == PAGE_RESEARCH;
        action.setMessage(label(research ? "button_research" : "button_synthesis"));
        action.active = menu.ready() && definition != null && data != null && data.available(definition.id())
                && (research ? data.progress(definition.id()) < 100 : data.progress(definition.id()) == 100);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        panel(graphics);
        graphics.fill(leftPos + LIST_X, topPos + LIST_Y, leftPos + LIST_X + LIST_WIDTH, topPos + LIST_Y + ROW_HEIGHT * ROWS, 0xFF080E15);
        List<GeneResearchDefinition> definitions = entries();
        scroll = Math.min(scroll, Math.max(0, definitions.size() - ROWS));
        if (definitions.isEmpty()) {
            graphics.drawWordWrap(font, label(menu.page() == PAGE_RESEARCH ? "no_definitions" : "no_unlocked"),
                    leftPos + LIST_X + 4, topPos + LIST_Y + 5, LIST_WIDTH - 8, 0xFFE0E7EF);
        }
        for (int row = 0; row < ROWS && row + scroll < definitions.size(); row++) {
            var definition = definitions.get(row + scroll);
            int y = topPos + LIST_Y + row * ROW_HEIGHT;
            if (definition.id().equals(menu.selection())) {
                graphics.fill(leftPos + LIST_X, y, leftPos + LIST_X + LIST_WIDTH, y + ROW_HEIGHT, 0xFF284955);
            }
            String shortKey = definition.nameKey() + ".short";
            text(graphics, Component.translatable(I18n.exists(shortKey) ? shortKey : definition.nameKey()), LIST_X + 4,
                    y - topPos + 4, LIST_WIDTH - 8, data() != null && data().available(definition.id()) ? 0xFFE0E7EF : 0xFF979FAB);
        }
        diamond(graphics, leftPos + MACHINE_SLOT_X + 8, topPos + MACHINE_SLOT_Y + 8);
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 9; column++) {
                slot(graphics, 68 + column * 18, row == 3 ? 236 : 178 + row * 18, 0xFF426776);
            }
        }
    }

    private void diamond(GuiGraphics graphics, int x, int y) {
        for (int line = -14; line <= 14; line++) {
            int half = 14 - Math.abs(line);
            graphics.fill(x - half, y + line, x + half + 1, y + line + 1, 0xFF4B9CA7);
            if (half >= 2) {
                graphics.fill(x - half + 1, y + line, x + half, y + line + 1, 0xFF080E15);
            }
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 12, 8, 0xFFE0E7EF, false);
        GeneResearchDefinition definition = selected();
        PlayerGeneResearch data = data();
        if (definition == null || data == null) {
            return;
        }
        graphics.drawWordWrap(font, Component.translatable(definition.nameKey()), 143, 26, 148, 0xFFF2CC79);
        if (menu.page() == PAGE_RESEARCH) {
            int progress = data.progress(definition.id());
            graphics.drawWordWrap(font, Component.translatable(definition.effectKey()), 143, 49, 147, 0xFFE0E7EF);
            textLocal(graphics, label("research_range", definition.minProgress(), definition.maxProgress()), 143, 70, 147, 0xFFE0E7EF);
            textLocal(graphics, label("used_extracts", data.used(definition.id()).size()), 143, 82, 147, 0xFF80C9D2);
            graphics.drawString(font, label("progress", progress), 143, 94, 0xFFF2CC79, false);
            graphics.fill(143, 104, 287, 109, 0xFF080E15);
            graphics.fill(143, 104, 143 + 144 * progress / 100, 109, 0xFF4B9CA7);
        } else {
            graphics.drawString(font, label("required_materials"), 143, 50, 0xFFF2CC79, false);
            int y = 63;
            for (GeneResearchDefinition.Material material : definition.materials()) {
                int held = GeneResearchService.count(inventory, material);
                Component name = material.extract() ? label("any_extract") : material.available()
                        ? ForgeRegistries.ITEMS.getValue(material.item()).getDescription() : label("missing_spikes");
                Component count = Component.literal(held + "/" + material.count());
                int color = held >= material.count() ? 0xFFE0E7EF : 0xFFFF8C81;
                textLocal(graphics, name, 143, y, 140 - font.width(count), color);
                graphics.drawString(font, count, 289 - font.width(count), y, color, false);
                y += 11;
            }
        }
        Component status = !data.available(definition.id())
                ? label(definition.equals(GeneResearchRegistry.FROST) ? "missing_spikes" : "missing_material")
                : menu.outcome() == null ? null : menu.outcome().message();
        if (status != null) {
            graphics.drawWordWrap(font, status, 143, 161, 148, 0xFFF2CC79);
        }
    }

    @Override protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        GeneResearchDefinition definition = selected();
        PlayerGeneResearch data = data();
        if (menu.page() == PAGE_RESEARCH && definition != null && data != null
                && isHovering(143, 81, 147, 11, mouseX, mouseY)) {
            var extracts = data.used(definition.id()).stream().sorted().map(id -> {
                ResourceLocation key = ResourceLocation.tryParse(id);
                return key != null && ForgeRegistries.ENTITY_TYPES.containsKey(key)
                        ? ForgeRegistries.ENTITY_TYPES.getValue(key).getDescription() : Component.literal(id);
            }).toList();
            if (!extracts.isEmpty()) {
                graphics.renderComponentTooltip(font, extracts, mouseX, mouseY);
            }
        }
    }

    private void text(GuiGraphics graphics, Component value, int x, int y, int width, int color) {
        textLocal(graphics, value, leftPos + x, topPos + y, width, color);
    }

    private void textLocal(GuiGraphics graphics, Component value, int x, int y, int width, int color) {
        var lines = font.split(value, Math.max(1, width));
        if (!lines.isEmpty()) {
            graphics.drawString(font, lines.get(0), x, y, color, false);
        }
    }

    @Override protected boolean clickScaled(double x, double y, int button) {
        if (button == 0 && overList(x, y)) {
            int row = (int) (y - topPos - LIST_Y) / ROW_HEIGHT + scroll;
            List<GeneResearchDefinition> definitions = entries();
            if (row < definitions.size()) {
                menu.request(SELECT, menu.page(), definitions.get(row).id());
            }
            return true;
        }
        return super.clickScaled(x, y, button);
    }

    @Override protected boolean scrollScaled(double x, double y, double delta) {
        if (overList(x, y)) {
            scroll = Math.max(0, Math.min(Math.max(0, entries().size() - ROWS), scroll - (int) Math.signum(delta)));
            return true;
        }
        return super.scrollScaled(x, y, delta);
    }

    private boolean overList(double x, double y) {
        return x >= leftPos + LIST_X && x < leftPos + LIST_X + LIST_WIDTH
                && y >= topPos + LIST_Y && y < topPos + LIST_Y + ROW_HEIGHT * ROWS;
    }

    private static Component label(String key, Object... values) {
        return Component.translatable("gene_research.yixiangshouronglu." + key, values);
    }
}

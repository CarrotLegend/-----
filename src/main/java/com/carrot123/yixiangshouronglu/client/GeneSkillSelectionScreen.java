package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.gene.GeneSkills;
import com.carrot123.yixiangshouronglu.network.GeneNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class GeneSkillSelectionScreen extends Screen {
    private final List<GeneSkills.Skill> skills;
    private int page;
    public GeneSkillSelectionScreen(List<GeneSkills.Skill> skills) { super(Component.translatable("gene.yixiangshouronglu.skill.select")); this.skills = List.copyOf(skills); }
    @Override protected void init() {
        int count = Math.max(1, (height - 90) / 24); int pages = (skills.size() + count - 1) / count; page = Math.min(page, Math.max(0, pages - 1));
        for (int row = 0; row < count && page * count + row < skills.size(); row++) {
            var skill = skills.get(page * count + row);
            addRenderableWidget(Button.builder(Component.translatable(skill.nameKey()), button -> { GeneNetwork.CHANNEL.sendToServer(new GeneNetwork.SkillRequest(skill.id())); onClose(); }).bounds(width / 2 - 100, 42 + row * 24, 200, 20).build());
        }
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), button -> { page = Math.floorMod(page - 1, pages); rebuildWidgets(); }).bounds(width / 2 - 100, height - 30, 40, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), button -> { page = (page + 1) % pages; rebuildWidgets(); }).bounds(width / 2 + 60, height - 30, 40, 20).build());
        }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { renderBackground(graphics); graphics.drawCenteredString(font, title, width / 2, 20, 0xE5BD68); super.render(graphics, mouseX, mouseY, partialTick); }
    @Override public boolean isPauseScreen() { return false; }
}

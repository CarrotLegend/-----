package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.block.entity.GeneModificationChamberBlockEntity;
import com.carrot123.yixiangshouronglu.menu.GeneModificationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.player.Inventory;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static com.carrot123.yixiangshouronglu.menu.GeneModificationMenu.*;

public final class GeneModificationScreen extends GeneScreen<GeneModificationMenu> {
    private static final String TEXT = "screen.yixiangshouronglu.gene_modification.";
    private static final String[] REGIONS = {"neural", "bone", "muscle", "metabolism", "energy", "senses"};
    private static final int STATS_LEFT = 249;
    private static final int STATS_RIGHT = 385;
    private final Component[] regionLabels = new Component[REGION_COUNT];
    private final Stat[] stats = new Stat[16];
    private final DecimalFormat pointsFormat = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    public GeneModificationScreen(GeneModificationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override protected void widgets() {
        for (int index = 0; index < REGION_COUNT; index++) {
            regionLabels[index] = Component.translatable(TEXT + REGIONS[index]);
        }
        stats[0] = new Stat("health", Attributes.MAX_HEALTH, false);
        stats[1] = new Stat("armor", Attributes.ARMOR, false);
        stats[2] = new Stat("attack_damage", Attributes.ATTACK_DAMAGE, false);
        stats[3] = new Stat("attack_speed", Attributes.ATTACK_SPEED, false);
        String[] ids = {"crit_chance", "crit_damage", "armor_shred", "current_hp_damage", "fire_damage", "cold_damage",
                "dodge_chance", "life_steal", "overheal", "arrow_damage", "arrow_velocity", "draw_speed"};
        for (int index = 0; index < ids.length; index++) {
            String id = ids[index];
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib", id));
            stats[index + 4] = new Stat(id, attribute, index != 4 && index != 5);
        }
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        panel(graphics);
        graphics.fill(leftPos + 8, topPos + 25, leftPos + 89, topPos + 182, 0xFF304E59);
        graphics.fill(leftPos + 9, topPos + 26, leftPos + 88, topPos + 181, 0xFF080E15);
        graphics.fill(leftPos + 94, topPos + 25, leftPos + 238, topPos + 182, 0xFF192835);
        graphics.fill(leftPos + 243, topPos + 25, leftPos + 388, topPos + 182, 0xFF192835);
        var player = minecraft.player;
        if (player != null) {
            int centerX = leftPos + 48;
            int feetY = topPos + 161;
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, centerX, feetY, 55,
                    Mth.clamp(centerX - mouseX, -25, 25), Mth.clamp(feetY - 45 - mouseY, -20, 20), player);
        }
        for (int index = 0; index < GeneModificationChamberBlockEntity.SIZE; index++) {
            slot(graphics, slotX(index), slotY(index), 0xFF4B9CA7);
        }
        for (int index = 0; index < 27; index++) {
            slot(graphics, INVENTORY_X + index % 9 * 18, INVENTORY_Y + index / 9 * 18, 0xFF334958);
        }
        for (int index = 0; index < 9; index++) {
            slot(graphics, INVENTORY_X + index * 18, INVENTORY_Y + 58, 0xFF334958);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 9, 9, 0xE4F1FA, false);
        for (int region = 0; region < REGION_COUNT; region++) {
            drawFitted(graphics, regionLabels[region], 98, slotY(firstSlot(region)) + 4, 61);
        }
        Player player = minecraft.player;
        if (player != null) {
            for (int index = 0; index < stats.length; index++) {
                Stat stat = stats[index];
                String value = stat.display(player, index == 0);
                int y = 29 + index * 9;
                int valueX = STATS_RIGHT - font.width(value);
                drawFitted(graphics, stat.label, STATS_LEFT, y, valueX - STATS_LEFT - 5);
                graphics.drawString(font, value, valueX, y, 0xFFB8E5E9, false);
            }
        }
    }

    private void drawFitted(GuiGraphics graphics, Component label, int x, int y, int availableWidth) {
        float scale = Math.min(1, (float) Math.max(1, availableWidth) / Math.max(1, font.width(label)));
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y + (1 - scale) * 4, 0);
        pose.scale(scale, scale, 1);
        graphics.drawString(font, label, 0, 0, 0xE4F1FA, false);
        pose.popPose();
    }

    private final class Stat {
        private final Component label;
        private final Attribute attribute;
        private final boolean percentage;
        private double lastValue = Double.NaN;
        private double lastHealth = Double.NaN;
        private String display = "--";

        private Stat(String id, Attribute attribute, boolean percentage) {
            label = Component.translatable(TEXT + "attribute." + id);
            this.attribute = attribute;
            this.percentage = percentage;
        }

        private String display(Player player, boolean health) {
            var instance = attribute == null ? null : player.getAttribute(attribute);
            if (instance == null) {
                lastValue = Double.NaN;
                display = "--";
                return display;
            }
            double value = instance.getValue();
            double currentHealth = health ? player.getHealth() : 0;
            if (value != lastValue || currentHealth != lastHealth) {
                lastValue = value;
                lastHealth = currentHealth;
                if (health) {
                    display = pointsFormat.format(currentHealth) + " / " + pointsFormat.format(value);
                } else {
                    display = percentage ? pointsFormat.format(value * 100) + "%" : pointsFormat.format(value);
                }
            }
            return display;
        }
    }
}

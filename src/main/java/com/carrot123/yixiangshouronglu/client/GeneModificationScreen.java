package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.gene.*;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import com.carrot123.yixiangshouronglu.menu.GeneModificationMenu;
import com.carrot123.yixiangshouronglu.network.GeneNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public final class GeneModificationScreen extends GeneScreen<GeneModificationMenu> {
    private int selectedInventory = -1;
    private int selectedBody;
    private int scroll;
    private float rotation;
    public GeneModificationScreen(GeneModificationMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, 398, 286); }
    private void send(int action) { GeneNetwork.CHANNEL.sendToServer(new GeneNetwork.Action(menu.containerId, menu.genes().revision(), action, selectedInventory, selectedBody)); }
    @Override protected void widgets() {
        String[] actions = {"install", "uninstall", "replace"};
        for (int index = 0; index < 3; index++) {
            final int action = index + 1;
            addRenderableWidget(Button.builder(Component.translatable("gene.yixiangshouronglu." + actions[index]), button -> send(action)).bounds(leftPos + 8, topPos + 158 + index * 23, 90, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gene.yixiangshouronglu.extreme"), button -> send(4)).bounds(leftPos + 8, topPos + 227, 90, 20).build());
    }
    private ItemStack installed() { return menu.genes().get(GeneService.region(selectedBody), GeneService.localSlot(selectedBody)); }
    private ItemStack candidate() { return selectedInventory >= 0 ? minecraft.player.getInventory().getItem(selectedInventory) : ItemStack.EMPTY; }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        panel(graphics);
        graphics.fill(leftPos + 8, topPos + 25, leftPos + 98, topPos + 150, 0xFF0A121A);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, leftPos + 52, topPos + 143, 48, rotation, 0, minecraft.player);
        int body = 0;
        for (var region : GeneTypes.Region.values()) {
            for (int slot = 0; slot < region.slots; slot++, body++) {
                int x = 201 + slot * 20, y = 34 + region.ordinal() * 23;
                slot(graphics, x, y, body == selectedBody ? 0xFFE3BD68 : 0xFF476A79);
                ItemStack stack = menu.genes().get(region, slot);
                if (!stack.isEmpty()) { graphics.renderItem(stack, leftPos + x, topPos + y); }
            }
        }
        for (var slot : menu.slots) { slot(graphics, slot.x, slot.y, slot.getContainerSlot() == selectedInventory ? 0xFFE3BD68 : 0xFF334958); }
        var genes = menu.genes();
        graphics.fill(leftPos + 9, topPos + 269, leftPos + 389, topPos + 278, 0xFF070D12);
        int normal = (int) Math.min(350, 350L * genes.used() / genes.capacity().limit);
        int extra = (int) Math.min(26, Math.max(0, genes.used() - genes.capacity().limit) * 26 / 4);
        graphics.fill(leftPos + 10, topPos + 270, leftPos + 10 + normal, topPos + 277, 0xFF4C9DD5);
        graphics.fill(leftPos + 362, topPos + 270, leftPos + 362 + extra, topPos + 277, genes.rejection() == GeneTypes.Rejection.SEVERE ? 0xFFE66565 : 0xFFE7C35D);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 8, 0xE4F1FA, false);
        for (var region : GeneTypes.Region.values()) { graphics.drawString(font, Component.translatable(region.key()), 110, 39 + region.ordinal() * 23, 0xB9DAE4, false); }
        var genes = menu.genes();
        graphics.drawString(font, Component.translatable("gene.yixiangshouronglu.capacity", genes.capacity().name(), genes.used(), genes.capacity().limit, genes.unknown() ? "?" : Math.max(0, genes.limit() - genes.used())), 9, 253, 0xD6E7F2, false);
        graphics.drawString(font, Component.translatable("gene.yixiangshouronglu.rejection." + genes.rejection().name().toLowerCase(java.util.Locale.ROOT)), 280, 253, 0xE5BD68, false);
        List<Component> lines = new ArrayList<>();
        ItemStack stack = candidate().isEmpty() ? installed() : candidate();
        var definition = GeneModuleItem.definition(stack); var data = GeneModuleItem.read(stack);
        lines.add(Component.translatable("gene.yixiangshouronglu.target_region", Component.translatable(GeneService.region(selectedBody).key())));
        if (!stack.isEmpty()) {
            lines.add(stack.getHoverName());
            if (data != null) { lines.add(Component.translatable(data.strength().key()).append(" / ").append(Component.translatable(data.quality().key()))); }
            if (definition != null) { lines.add(Component.translatable("gene.yixiangshouronglu.module_region", Component.translatable(definition.region().key()))); lines.add(Component.translatable("gene.yixiangshouronglu.cost", definition.cost())); lines.add(GeneDescriptions.categories(definition)); }
            else { lines.add(Component.translatable("gene.yixiangshouronglu.invalid")); }
            if (definition != null) { lines.addAll(GeneDescriptions.effects(stack, genes.runtime())); }

        } else { lines.add(Component.translatable("gene.yixiangshouronglu.empty_rule")); }
        if (!candidate().isEmpty()) { lines.add(Component.translatable("gene.yixiangshouronglu." + GeneService.check(genes, candidate(), selectedBody, !installed().isEmpty()))); }
        lines.add(Component.translatable("gene.yixiangshouronglu." + menu.status));
        lines.add(Component.translatable(genes.extreme() ? "gene.yixiangshouronglu.extreme_on" : "gene.yixiangshouronglu.extreme_off"));
        List<net.minecraft.util.FormattedCharSequence> wrapped = new ArrayList<>();
        lines.forEach(line -> wrapped.addAll(font.split(line, 106)));
        int start = Math.min(scroll, Math.max(0, wrapped.size() - 22));
        for (int index = start; index < Math.min(wrapped.size(), start + 22); index++) { graphics.drawString(font, wrapped.get(index), 282, 30 + (index - start) * 9, 0xBDCADA, false); }
    }
    @Override protected boolean clickScaled(double x, double y, int button) {
        if (button == 0) {
            int body = 0;
            for (var region : GeneTypes.Region.values()) { for (int slot = 0; slot < region.slots; slot++, body++) {
                if (isHovering(201 + slot * 20, 34 + region.ordinal() * 23, 16, 16, x, y)) { selectedBody = body; scroll = 0; return true; }
            } }
            if (menu.getCarried().isEmpty() && !hasShiftDown()) {
                for (var slot : menu.slots) {
                    if (isHovering(slot.x, slot.y, 16, 16, x, y) && slot.getItem().getItem() instanceof GeneModuleItem) { selectedInventory = slot.getContainerSlot(); scroll = 0; send(0); return true; }
                }
            }
        }
        return super.clickScaled(x, y, button);
    }
    @Override protected boolean dragScaled(double x, double y, int button, double dx, double dy) {
        if (button == 0 && isHovering(8, 25, 90, 125, x, y)) { rotation = Math.max(-100, Math.min(100, rotation - (float) dx * 2)); return true; }
        return super.dragScaled(x, y, button, dx, dy);
    }
    @Override protected boolean scrollScaled(double x, double y, double delta) { if (isHovering(280, 25, 110, 220, x, y)) { scroll = Math.max(0, scroll - (int) Math.signum(delta)); return true; } return super.scrollScaled(x, y, delta); }
}

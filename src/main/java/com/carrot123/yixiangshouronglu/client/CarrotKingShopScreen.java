package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.menu.CarrotKingShopMenu;
import com.carrot123.yixiangshouronglu.network.ShopNetwork;
import com.carrot123.yixiangshouronglu.shop.ShopCapability;
import com.carrot123.yixiangshouronglu.shop.ShopCatalog;
import com.carrot123.yixiangshouronglu.shop.ShopInventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class CarrotKingShopScreen extends GeneScreen<CarrotKingShopMenu> {
    private Button enter;
    private Button buyTab;
    private Button sellTab;
    private int selectedSale = -1;
    private final List<Button> purchases = new ArrayList<>();
    public CarrotKingShopScreen(CarrotKingShopMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, 360, 270); }
    private Component text(String key, Object... args) { return Component.translatable("shop.yixiangshouronglu." + key, args); }
    private void send(int action, int product) {
        ShopNetwork.CHANNEL.sendToServer(new ShopNetwork.Action(menu.containerId, menu.revision,
                ShopCapability.get(minecraft.player).revision(), action, product));
    }
    @Override protected void widgets() {
        purchases.clear();
        enter = addRenderableWidget(Button.builder(text("enter"), button -> send(0, -1)).bounds(leftPos + 120, topPos + 195, 120, 20).build());
        buyTab = addRenderableWidget(Button.builder(text("buy"), button -> send(1, -1)).bounds(leftPos + 8, topPos + 37, 64, 20).build());
        sellTab = addRenderableWidget(Button.builder(text("sell"), button -> send(2, -1)).bounds(leftPos + 8, topPos + 61, 64, 20).build());
        for (int index = 0; index < 2; index++) {
            final int product = index;
            purchases.add(addRenderableWidget(Button.builder(text("purchase"), button -> send(3, product))
                    .bounds(leftPos + 282, topPos + 48 + index * 54, 66, 20).build()));
        }
    }
    private void updateButtons() {
        boolean welcome = menu.page == CarrotKingShopMenu.WELCOME;
        boolean ready = menu.revision >= 0 && menu.getCarried().isEmpty();
        enter.visible = welcome; enter.active = ready;
        buyTab.visible = !welcome; sellTab.visible = !welcome;
        buyTab.active = ready && menu.page != CarrotKingShopMenu.BUY;
        sellTab.active = ready && menu.page != CarrotKingShopMenu.SELL;
        for (int index = 0; index < purchases.size(); index++) {
            Button button = purchases.get(index);
            button.visible = menu.page == CarrotKingShopMenu.BUY;
            button.active = ready && index < menu.offers.size() && !menu.offers.get(index).stack().isEmpty()
                    && ShopCapability.get(minecraft.player).canPay(menu.offers.get(index).price());
        }
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        updateButtons();
        panel(graphics);
        if (menu.page == CarrotKingShopMenu.WELCOME) {
            graphics.fill(leftPos + 18, topPos + 36, leftPos + 342, topPos + 172, 0xFF0A121A);
            return;
        }
        graphics.fill(leftPos + 80, topPos + 30, leftPos + 352, topPos + 147, 0xFF0A121A);
        for (int index = 0; index < menu.slots.size(); index++) {
            var slot = menu.slots.get(index); slot(graphics, slot.x, slot.y, 0xFF334958);
        }
        Button selectedTab = menu.page == CarrotKingShopMenu.BUY ? buyTab : sellTab;
        graphics.fill(selectedTab.getX() - 2, selectedTab.getY(), selectedTab.getX(), selectedTab.getY() + selectedTab.getHeight(), 0xFFE1B65A);
        if (menu.page == CarrotKingShopMenu.SELL) {
            for (int index = 0; index < menu.sellOffers.size(); index++) {
                drawCard(graphics, menu.sellOffers.get(index), 88 + index % 2 * 134, 46 + index / 2 * 52,
                        126, 46, index == selectedSale, mouseX, mouseY, false);
            }
        } else {
            for (int index = 0; index < menu.offers.size(); index++) {
                drawCard(graphics, menu.offers.get(index), 88, 35 + index * 54, 260, 50, false, mouseX, mouseY, true);
            }
        }
    }
    private void drawCard(GuiGraphics graphics, ShopCatalog.Offer offer, int x, int y, int width, int height,
                          boolean selected, int mouseX, int mouseY, boolean buying) {
        boolean hovered = isHovering(x, y, width, height, mouseX, mouseY);
        int border = selected ? 0xFFE1B65A : hovered ? 0xFF63BBC7 : 0xFF334958;
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, border);
        graphics.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + width - 1, topPos + y + height - 1, hovered ? 0xFF192D3B : 0xFF101D29);
        var stack = offer.stack();
        graphics.renderItem(stack, leftPos + x + 6, topPos + y + 12);
        Component name = stack.isEmpty() ? text("offer." + offer.key()) : stack.getHoverName();
        var lines = font.split(name, buying ? 154 : 94);
        for (int line = 0; line < Math.min(2, lines.size()); line++) {
            graphics.drawString(font, lines.get(line), leftPos + x + 26, topPos + y + 4 + line * 10, 0xD6E7F2, false);
        }
        graphics.drawString(font, text("price", offer.price()), leftPos + x + 26, topPos + y + 27, 0xE1B65A, false);
        if (stack.isEmpty()) { graphics.drawString(font, text("unavailable"), leftPos + x + 26, topPos + y + 38, 0xE66565, false); }
    }
    @Override protected boolean clickScaled(double x, double y, int button) {
        if (button == 0 && menu.page == CarrotKingShopMenu.SELL) {
            for (int index = 0; index < menu.sellOffers.size(); index++) {
                if (isHovering(88 + index % 2 * 134, 46 + index / 2 * 52, 126, 46, x, y)) {
                    selectedSale = index;
                    if (menu.revision >= 0 && menu.getCarried().isEmpty()) { send(hasShiftDown() ? 5 : 4, index); }
                    return true;
                }
            }
        }
        return super.clickScaled(x, y, button);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, text(menu.page == CarrotKingShopMenu.WELCOME ? "npc" : "title"), 8, 9, 0xE4F1FA, false);
        if (menu.page == CarrotKingShopMenu.WELCOME) {
            var lines = font.split(text("welcome"), 292);
            for (int index = 0; index < lines.size(); index++) { graphics.drawString(font, lines.get(index), 34, 65 + index * 14, 0xD6E7F2, false); }
            return;
        }
        Component coins = text("coins", ShopCapability.get(minecraft.player).coins());
        int coinX = 350 - font.width(coins);
        graphics.fill(coinX - 11, 10, coinX - 4, 17, 0xFFE1B65A);
        graphics.drawString(font, coins, coinX, 9, 0xFFE1B65A, false);
        graphics.drawString(font, text("inventory"), 90, 153, 0x9FB9C9, false);
        if (menu.page == CarrotKingShopMenu.SELL) { graphics.drawString(font, text("sell_items"), 88, 33, 0xD6E7F2, false); }
        if (!menu.status.equals("ok")) {
            var lines = font.split(text("status." + menu.status), 338);
            for (int index = 0; index < Math.min(2, lines.size()); index++) { graphics.drawString(font, lines.get(index), 10, 246 + index * 10, 0xA9D5D4, false); }
        }
    }

    @Override protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        if (menu.page == CarrotKingShopMenu.SELL) {
            for (int index = 0; index < menu.sellOffers.size(); index++) {
                if (isHovering(88 + index % 2 * 134, 46 + index / 2 * 52, 126, 46, mouseX, mouseY)) {
                    var offer = menu.sellOffers.get(index);
                    ResourceLocation item = ForgeRegistries.ITEMS.getKey(offer.stack().getItem());
                    int owned = ShopInventory.count(ShopInventory.snapshot(minecraft.player.getInventory()), item);
                    graphics.renderComponentTooltip(font, List.of(offer.stack().getHoverName(), text("price", offer.price()),
                            text("owned", owned), text("sell_one"), text("sell_all")), mouseX, mouseY);
                }
            }
            return;
        }
        if (menu.page != CarrotKingShopMenu.BUY) { return; }
        for (int index = 0; index < menu.offers.size(); index++) {
            if (isHovering(94, 47 + index * 54, 16, 16, mouseX, mouseY) && !menu.offers.get(index).stack().isEmpty()) {
                graphics.renderTooltip(font, menu.offers.get(index).stack(), mouseX, mouseY);
            }
            if (isHovering(282, 48 + index * 54, 66, 20, mouseX, mouseY)) {
                var offer = menu.offers.get(index);
                List<Component> lines = new ArrayList<>();
                lines.add(text("price", offer.price()));
                if (offer.stack().isEmpty()) { lines.add(text("status.unavailable")); }
                else if (!ShopCapability.get(minecraft.player).canPay(offer.price())) { lines.add(text("status.insufficient_coins")); }
                else if (!menu.getCarried().isEmpty()) { lines.add(text("status.cursor_not_empty")); }
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            }
        }
    }
}

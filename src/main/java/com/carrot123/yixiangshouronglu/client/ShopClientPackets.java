package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.menu.CarrotKingShopMenu;
import com.carrot123.yixiangshouronglu.network.ShopNetwork;
import com.carrot123.yixiangshouronglu.shop.ShopCapability;
import net.minecraft.client.Minecraft;

public final class ShopClientPackets {
    private ShopClientPackets() {}
    public static void apply(ShopNetwork.State state) {
        var player = Minecraft.getInstance().player;
        if (player == null) { return; }
        ShopCapability.get(player).load(state.wallet());
        if (player.containerMenu instanceof CarrotKingShopMenu menu && menu.containerId == state.window()) {
            menu.page = state.page(); menu.revision = state.revision(); menu.status = state.status(); menu.offers = state.offers(); menu.sellOffers = state.sellOffers();
        }
    }
}

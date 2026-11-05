package com.carrot123.yixiangshouronglu.shop;

import com.carrot123.yixiangshouronglu.network.ShopNetwork;
import net.minecraft.server.level.ServerPlayer;

public final class ShopService {
    private ShopService() {}
    public static long coins(ServerPlayer player) { return ShopCapability.get(player).coins(); }
    public static boolean canPay(ServerPlayer player, long amount) { return ShopCapability.get(player).canPay(amount); }
    public static boolean add(ServerPlayer player, long amount) {
        if (!ShopCapability.get(player).add(amount)) { return false; }
        ShopNetwork.sync(player); return true;
    }
    public static boolean pay(ServerPlayer player, long amount) {
        if (!ShopCapability.get(player).pay(amount)) { return false; }
        ShopNetwork.sync(player); return true;
    }
}

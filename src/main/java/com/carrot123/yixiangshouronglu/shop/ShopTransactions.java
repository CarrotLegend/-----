package com.carrot123.yixiangshouronglu.shop;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ShopTransactions {
    private ShopTransactions() {}
    public static String validate(long revision, long currentRevision, long walletRevision, long currentWalletRevision) {
        return revision == currentRevision && walletRevision == currentWalletRevision ? "ok" : "stale";
    }
    public static <T> String commit(PlayerShopData wallet, long revision, long amount, boolean buying,
                                    Supplier<T> snapshot, Consumer<T> restore, BooleanSupplier inventoryChange) {
        if (revision != wallet.revision()) { return "stale"; }
        if (amount <= 0) { return "invalid"; }
        if (buying && !wallet.canPay(amount)) { return "insufficient_coins"; }
        if (!buying && !wallet.canAdd(amount)) { return "balance_limit"; }
        PlayerShopData.State oldWallet = wallet.state();
        T oldInventory = snapshot.get();
        boolean complete = false;
        try {
            boolean changed = buying ? wallet.pay(amount) : wallet.add(amount);
            if (!changed || !inventoryChange.getAsBoolean()) { return "trade_failed"; }
            complete = true;
            return buying ? "purchase_success" : "sale_success";
        } finally {
            if (!complete) { wallet.restore(oldWallet); restore.accept(oldInventory); }
        }
    }
}

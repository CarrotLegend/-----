package com.carrot123.yixiangshouronglu.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShopTest {
    @Test void newPlayersStartAtZeroAndGreetingIsOncePerPlayer() {
        PlayerShopData first = new PlayerShopData();
        assertEquals(0, first.coins()); assertFalse(first.welcomed());
        assertTrue(first.welcome());
        long revision = first.revision();
        assertFalse(first.welcome()); assertEquals(revision, first.revision());
        assertFalse(new PlayerShopData().welcomed());
    }

    @Test void walletRoundTripKeepsGreetingCoinsAndRevisionIndependently() {
        PlayerShopData original = new PlayerShopData();
        original.welcome(); assertTrue(original.add(100)); assertTrue(original.pay(2));
        PlayerShopData restored = new PlayerShopData(); restored.load(original.save());
        assertEquals(original.state(), restored.state());
        assertTrue(restored.add(2));
        assertEquals(98, original.coins()); assertEquals(100, restored.coins());
        assertEquals(0, new PlayerShopData().coins());
    }

    @Test void negativeOrZeroAmountsAndBalanceOverflowCannotMutateWallet() {
        PlayerShopData wallet = new PlayerShopData();
        assertFalse(wallet.add(-1)); assertFalse(wallet.pay(-1)); assertFalse(wallet.add(0)); assertFalse(wallet.pay(0));
        assertTrue(wallet.add(Long.MAX_VALUE));
        var before = wallet.state();
        assertFalse(wallet.add(1)); assertEquals(before, wallet.state());
        assertTrue(wallet.pay(Long.MAX_VALUE)); assertEquals(0, wallet.coins());
        assertFalse(wallet.pay(1));
    }

    @Test void missingAndNegativeSavedValuesLoadSafely() {
        CompoundTag tag = new CompoundTag(); tag.putLong("Coins", -10); tag.putLong("Revision", -5);
        PlayerShopData wallet = new PlayerShopData(); wallet.load(tag);
        assertEquals(new PlayerShopData.State(0, false, 0), wallet.state());
    }

    @Test void salePricesAndWholeStackValuesMatchEverySupportedItem() {
        String[] ids = {"iron_ingot", "gold_ingot", "diamond", "netherite_ingot"};
        long[] prices = {1, 2, 5, 20};
        for (int index = 0; index < ids.length; index++) {
            ResourceLocation item = new ResourceLocation("minecraft", ids[index]);
            assertEquals(prices[index], ShopCatalog.sellPrice(item));
            for (int count = 1; count <= 64; count++) { assertEquals(prices[index] * count, ShopCatalog.saleValue(item, count)); }
            assertEquals(0, ShopCatalog.saleValue(item, 0));
        }
        assertEquals(0, ShopCatalog.saleValue(new ResourceLocation("minecraft:dirt"), 64));
        assertEquals(0, ShopCatalog.sellPrice(null));
    }

    @Test void purchaseCatalogContainsOrdinaryPotionWithRequiredPotionTypeAndCompoundZ() {
        var potion = ShopCatalog.PRODUCTS.get(0);
        assertEquals("minecraft:potion", potion.item().toString());
        assertEquals("mutantmonsters:chemical_x", potion.potion().toString()); assertEquals(2, potion.price());
        var compound = ShopCatalog.PRODUCTS.get(1);
        assertEquals("mutantmore:compound_z", compound.item().toString()); assertNull(compound.potion()); assertEquals(20, compound.price());
    }

    @Test void purchaseDeductsOnceAndStaleReplayCannotDeliverAgain() {
        PlayerShopData wallet = new PlayerShopData(); wallet.add(10);
        long revision = wallet.revision(); List<String> inventory = new ArrayList<>();
        assertEquals("purchase_success", ShopTransactions.commit(wallet, revision, 2, true,
                () -> List.copyOf(inventory), old -> { inventory.clear(); inventory.addAll(old); }, () -> inventory.add("potion")));
        assertEquals(8, wallet.coins()); assertEquals(List.of("potion"), inventory); assertEquals(revision + 1, wallet.revision());
        assertEquals("stale", ShopTransactions.commit(wallet, revision, 2, true, () -> fail("must not snapshot"), old -> fail("must not restore"), () -> fail("must not deliver")));
        assertEquals(8, wallet.coins()); assertEquals(1, inventory.size());
    }

    @Test void saleCreditsWholeStackAndConsumesItOnce() {
        PlayerShopData wallet = new PlayerShopData(); int[] count = {64};
        assertEquals("sale_success", ShopTransactions.commit(wallet, 0, 320, false,
                () -> count[0], old -> count[0] = old, () -> { count[0] = 0; return true; }));
        assertEquals(320, wallet.coins()); assertEquals(0, count[0]);
    }

    @Test void insufficientCoinsOverflowAndInvalidAmountNeverTouchInventory() {
        PlayerShopData wallet = new PlayerShopData();
        assertEquals("insufficient_coins", ShopTransactions.commit(wallet, 0, 2, true, () -> fail("must not snapshot"), old -> fail(), () -> fail()));
        assertEquals("invalid", ShopTransactions.commit(wallet, 0, -2, true, () -> fail(), old -> fail(), () -> fail()));
        wallet.add(Long.MAX_VALUE);
        assertEquals("balance_limit", ShopTransactions.commit(wallet, wallet.revision(), 1, false, () -> fail(), old -> fail(), () -> fail()));
        assertEquals(Long.MAX_VALUE, wallet.coins());
    }

    @Test void failedAndThrowingInventoryOperationsRestoreCoinsRevisionAndInventory() {
        for (boolean buying : new boolean[]{true, false}) {
            for (boolean throwing : new boolean[]{true, false}) {
                PlayerShopData wallet = new PlayerShopData(); wallet.welcome(); wallet.add(50);
                var original = wallet.state(); List<String> inventory = new ArrayList<>(List.of("stack with metadata"));
                if (throwing) {
                    assertThrows(IllegalStateException.class, () -> ShopTransactions.commit(wallet, wallet.revision(), 20, buying,
                            () -> List.copyOf(inventory), old -> { inventory.clear(); inventory.addAll(old); },
                            () -> { inventory.clear(); throw new IllegalStateException("test"); }));
                } else {
                    assertEquals("trade_failed", ShopTransactions.commit(wallet, wallet.revision(), 20, buying,
                            () -> List.copyOf(inventory), old -> { inventory.clear(); inventory.addAll(old); }, () -> { inventory.clear(); return false; }));
                }
                assertEquals(original, wallet.state()); assertEquals(List.of("stack with metadata"), inventory);
            }
        }
    }

    @Test void changesToEitherMenuOrWalletInvalidateOldRequests() {
        assertEquals("ok", ShopTransactions.validate(3, 3, 7, 7));
        assertEquals("stale", ShopTransactions.validate(2, 3, 7, 7));
        assertEquals("stale", ShopTransactions.validate(3, 3, 6, 7));
    }

    @Test void insertionPlanRequiresCompleteSpaceBeforeDelivery() {
        assertTrue(ShopInventory.allocation(1, List.of()).isEmpty());
        assertTrue(ShopInventory.allocation(2, List.of(new ShopInventory.Space(0, 1))).isEmpty());
        assertTrue(ShopInventory.allocation(1, List.of(new ShopInventory.Space(36, 64), new ShopInventory.Space(40, 64))).isEmpty());
        var plan = ShopInventory.allocation(3, List.of(new ShopInventory.Space(4, 1), new ShopInventory.Space(9, 64))).orElseThrow();
        assertEquals(List.of(new ShopInventory.Placement(4, 1), new ShopInventory.Placement(9, 2)), plan);
    }
    @Test void saleCatalogOrderAndBulkAmountsAreStable() {
        assertEquals(List.of("iron_ingot", "gold_ingot", "diamond", "netherite_ingot"),
                ShopCatalog.SELL_PRODUCTS.stream().map(ShopCatalog.Product::key).toList());
        for (var product : ShopCatalog.SELL_PRODUCTS) {
            assertEquals(product.price() * 2304, ShopCatalog.saleValue(product.item(), 2304));
        }
    }

}

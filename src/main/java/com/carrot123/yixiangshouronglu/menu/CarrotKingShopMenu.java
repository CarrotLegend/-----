package com.carrot123.yixiangshouronglu.menu;

import com.carrot123.yixiangshouronglu.entity.LegendaryCarrotKingEntity;
import com.carrot123.yixiangshouronglu.network.ShopNetwork;
import com.carrot123.yixiangshouronglu.registry.ModMenus;
import com.carrot123.yixiangshouronglu.shop.ShopCapability;
import com.carrot123.yixiangshouronglu.shop.ShopCatalog;
import com.carrot123.yixiangshouronglu.shop.ShopInventory;
import com.carrot123.yixiangshouronglu.shop.ShopTransactions;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public final class CarrotKingShopMenu extends AbstractContainerMenu {
    public static final int WELCOME = 0;
    public static final int BUY = 1;
    public static final int SELL = 2;
    private final Inventory inventory;
    private final int entityId;
    private final UUID entityUuid;
    private List<ItemStack> observedInventory = List.of();
    private long observedWallet = -1;
    private int observedPage = -1;
    public int page;
    public long revision = -1;
    public String status = "ok";
    public List<ShopCatalog.Offer> offers = List.of();
    public List<ShopCatalog.Offer> sellOffers = List.of();

    public CarrotKingShopMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readVarInt(), buffer.readUUID(), buffer.readBoolean());
    }
    public CarrotKingShopMenu(int id, Inventory inventory, int entityId, UUID entityUuid, boolean welcome) {
        super(ModMenus.CARROT_KING_SHOP.get(), id);
        this.inventory = inventory; this.entityId = entityId; this.entityUuid = entityUuid; page = welcome ? WELCOME : BUY;
        if (!inventory.player.level().isClientSide) { offers = ShopCatalog.offers(); sellOffers = ShopCatalog.sellOffers(); }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) { addPlayerSlot(inventory, col + row * 9 + 9, 90 + col * 18, 166 + row * 18); }
        }
        for (int col = 0; col < 9; col++) { addPlayerSlot(inventory, col, 90 + col * 18, 224); }
    }
    private void addPlayerSlot(Inventory inventory, int slot, int x, int y) {
        addSlot(new Slot(inventory, slot, x, y) {
            @Override public boolean isActive() { return page != WELCOME; }
            @Override public boolean mayPlace(ItemStack stack) { return page != WELCOME; }
            @Override public boolean mayPickup(Player player) { return page != WELCOME; }
        });
    }
    @Override public boolean stillValid(Player player) {
        var entity = player.level().getEntity(entityId);
        return inventory.player == player && entity instanceof LegendaryCarrotKingEntity && entity.isAlive()
                && !entity.isRemoved() && entity.getUUID().equals(entityUuid) && entity.distanceToSqr(player) <= 64;
    }
    public void syncState(String result) {
        if (!(inventory.player instanceof ServerPlayer player)) { return; }
        status = result;
        updateState(player, true);
    }
    private void updateState(ServerPlayer player, boolean force) {
        var wallet = ShopCapability.get(player);
        var currentInventory = ShopInventory.snapshot(inventory);
        boolean changed = observedWallet != wallet.revision() || observedPage != page || !ShopInventory.matches(observedInventory, currentInventory);
        if (changed) {
            revision++;
            observedWallet = wallet.revision(); observedPage = page; observedInventory = currentInventory;
        }
        if (force || changed) { ShopNetwork.sendState(player, this); }
    }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (inventory.player instanceof ServerPlayer player) { updateState(player, false); }
    }
    public void act(ServerPlayer player, ShopNetwork.Action request) {
        if (player.containerMenu != this || inventory.player != player || request.window() != containerId || !stillValid(player)) { syncState("invalid_session"); return; }
        updateState(player, false);
        var wallet = ShopCapability.get(player);
        String validity = ShopTransactions.validate(request.revision(), revision, request.walletRevision(), wallet.revision());
        if (!validity.equals("ok")) { syncState(validity); return; }
        if (!getCarried().isEmpty()) { syncState("cursor_not_empty"); return; }
        try {
            String result;
            if (page == WELCOME) {
                if (request.action() != 0) { syncState("invalid"); return; }
                page = BUY; result = "ok";
            } else if (request.action() == 1 || request.action() == 2) {
                page = request.action() == 1 ? BUY : SELL; result = "ok";
            } else if (request.action() == 3 && page == BUY) {
                result = purchase(player, request.product(), request.walletRevision());
            } else if ((request.action() == 4 || request.action() == 5) && page == SELL) {
                result = sell(player, request.product(), request.action() == 5, request.walletRevision());
            } else { result = "invalid"; }
            super.broadcastChanges();
            syncState(result);
        } catch (RuntimeException exception) {
            LogUtils.getLogger().error("Carrot King transaction failed", exception);
            super.broadcastChanges();
            syncState("trade_failed");
        }
    }
    private String purchase(ServerPlayer player, int index, long walletRevision) {
        if (index < 0 || index >= offers.size()) { return "invalid_product"; }
        var offer = offers.get(index);
        ItemStack product = offer.stack();
        if (product.isEmpty()) { return "unavailable"; }
        if (!ShopCapability.get(player).canPay(offer.price())) { return "insufficient_coins"; }
        var destination = ShopInventory.insert(ShopInventory.snapshot(inventory), product);
        if (destination.isEmpty()) { return "inventory_full"; }
        return ShopTransactions.commit(ShopCapability.get(player), walletRevision, offer.price(), true,
                () -> ShopInventory.snapshot(inventory), snapshot -> ShopInventory.restore(inventory, snapshot),
                () -> { ShopInventory.restore(inventory, destination.get()); inventory.setChanged(); return true; });
    }
    private String sell(ServerPlayer player, int index, boolean all, long walletRevision) {
        if (index < 0 || index >= ShopCatalog.SELL_PRODUCTS.size()) { return "invalid_product"; }
        var product = ShopCatalog.SELL_PRODUCTS.get(index);
        var plan = ShopInventory.remove(ShopInventory.snapshot(inventory), product.item(), all);
        if (plan.isEmpty()) { return "no_item"; }
        long total = ShopCatalog.saleValue(product.item(), plan.get().count());
        return ShopTransactions.commit(ShopCapability.get(player), walletRevision, total, false,
                () -> ShopInventory.snapshot(inventory), snapshot -> ShopInventory.restore(inventory, snapshot),
                () -> { ShopInventory.restore(inventory, plan.get().stacks()); inventory.setChanged(); return true; });
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (page == WELCOME || index < 0 || index >= slots.size()) { return ItemStack.EMPTY; }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) { return ItemStack.EMPTY; }
        ItemStack stack = slot.getItem(); ItemStack original = stack.copy();
        if (index < 27) {
            if (!moveItemStackTo(stack, 27, 36, false)) { return ItemStack.EMPTY; }
        } else if (!moveItemStackTo(stack, 0, 27, false)) { return ItemStack.EMPTY; }
        if (stack.isEmpty()) { slot.set(ItemStack.EMPTY); } else { slot.setChanged(); }
        slot.onTake(player, stack);
        return original;
    }
}

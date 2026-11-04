package com.carrot123.yixiangshouronglu.menu;

import com.carrot123.yixiangshouronglu.gene.*;
import com.carrot123.yixiangshouronglu.network.GeneNetwork;
import com.carrot123.yixiangshouronglu.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class GeneModificationMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Inventory inventory;
    private int selectedSlot = -1;
    private ItemStack selected = ItemStack.EMPTY;
    public String status = "ok";
    public GeneCapacityUpgrades.Preview capacityUpgrade;
    public GeneModificationMenu(int id, Inventory inventory, FriendlyByteBuf buffer) { this(id, inventory, buffer.readBlockPos()); }
    public GeneModificationMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.GENE_MODIFICATION_CHAMBER.get(), id); this.pos = pos; this.inventory = inventory;
        for (int row = 0; row < 3; row++) { for (int col = 0; col < 9; col++) { addSlot(new Slot(inventory, col + row * 9 + 9, 108 + col * 18, 158 + row * 18)); } }
        for (int col = 0; col < 9; col++) { addSlot(new Slot(inventory, col, 108 + col * 18, 216)); }
    }
    public PlayerGenes genes() { return GeneCapability.get(inventory.player); }
    public void act(ServerPlayer player, GeneNetwork.Action message) {
        if (inventory.player != player || !stillValid(player) || !getCarried().isEmpty()) { GeneNetwork.sync(player, "cursor_not_empty"); return; }
        if (!player.getCapability(GeneCapability.TYPE).isPresent()) { GeneNetwork.sync(player, "capacity_upgrade_failed"); return; }
        if (message.revision() != genes().revision()) { GeneNetwork.sync(player, message.action() == 5 ? "capacity_upgrade_stale" : "stale"); return; }
        if (message.action() == 5) {
            String result = GeneService.upgradeCapacity(player, message.revision());
            if (!result.equals("capacity_upgrade_success")) { GeneNetwork.sync(player, result); }
            broadcastChanges();
            return;
        }
        if (message.action() == 0) {
            selectedSlot = message.inventorySlot();
            selected = selectedSlot >= 0 && selectedSlot < 36 ? inventory.getItem(selectedSlot).copy() : ItemStack.EMPTY;
            return;
        }
        String result = GeneService.move(player, message.action(), message.inventorySlot() == selectedSlot ? selectedSlot : -1, message.bodySlot(), message.revision(), selected);
        if (result.equals("ok")) { selected = ItemStack.EMPTY; selectedSlot = -1; }
        if (!result.equals("ok")) { GeneNetwork.sync(player, result); }
        broadcastChanges();
    }
    @Override public boolean stillValid(Player player) { return inventory.player == player && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64 && player.level().hasChunkAt(pos) && player.level().getBlockState(pos).is(ModBlocks.GENE_MODIFICATION_CHAMBER.get()); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) { return ItemStack.EMPTY; }
        Slot slot = slots.get(index); if (!slot.hasItem()) { return ItemStack.EMPTY; }
        ItemStack stack = slot.getItem(); ItemStack original = stack.copy();
        if (!(index < 27 ? moveItemStackTo(stack, 27, 36, false) : moveItemStackTo(stack, 0, 27, false))) { return ItemStack.EMPTY; }
        if (stack.isEmpty()) { slot.set(ItemStack.EMPTY); } else { slot.setChanged(); }
        slot.onTake(player, stack); return original;
    }
}

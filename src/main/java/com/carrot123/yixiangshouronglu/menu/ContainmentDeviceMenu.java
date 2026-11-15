package com.carrot123.yixiangshouronglu.menu;

import com.carrot123.yixiangshouronglu.item.BiologicalResearchItem;
import com.carrot123.yixiangshouronglu.item.ContainmentDeviceItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import com.carrot123.yixiangshouronglu.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ContainmentDeviceMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final int heldSlot;
    private final ItemStack device;

    public ContainmentDeviceMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readVarInt(), null);
    }

    public ContainmentDeviceMenu(int id, Inventory inventory, int heldSlot, ItemStack device) {
        super(ModMenus.CONTAINMENT_DEVICE.get(), id);
        this.inventory = inventory;
        this.heldSlot = heldSlot;
        this.device = device;
        SimpleContainer result = new SimpleContainer(1);
        if (device != null) {
            result.setItem(0, ContainmentDeviceItem.stored(device));
            result.addListener(changed -> {
                if (stillValid(inventory.player)) {
                    ContainmentDeviceItem.store(device, changed.getItem(0));
                    inventory.setChanged();
                }
            });
        }
        addSlot(new Slot(result, 0, 80, 52) {
            @Override public boolean mayPlace(ItemStack stack) {
                return BiologicalResearchItem.entityId(stack, true) != null;
            }

            @Override public int getMaxStackSize() { return 1; }
        });
        for (int index = 0; index < 36; index++) {
            int playerSlot = (index + 9) % 36;
            int y = index < 27 ? 108 + index / 9 * 18 : 166;
            addSlot(new Slot(inventory, playerSlot, 8 + index % 9 * 18, y) {
                @Override public boolean mayPlace(ItemStack stack) { return playerSlot != heldSlot; }
                @Override public boolean mayPickup(Player player) { return playerSlot != heldSlot; }
            });
        }
    }

    @Override public boolean stillValid(Player player) {
        ItemStack current = inventory.getItem(heldSlot);
        return player == inventory.player && player.isAlive() && inventory.selected == heldSlot
                && current.is(ModItems.CONTAINMENT_DEVICE.get()) && (device == null || current == device);
    }

    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (stillValid(player) && type != ClickType.CLONE && (type != ClickType.SWAP || button != heldSlot)
                && (slot < 0 || (slot < slots.size() && getSlot(slot).mayPickup(player)))) {
            super.clicked(slot, button, type, player);
        }
    }

    @Override public boolean canDragTo(Slot slot) { return slot.getContainerSlot() != heldSlot || slot.container != inventory; }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = getSlot(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack original = stack.copy();
        if (!(index == 0 ? moveItemStackTo(stack, 1, slots.size(), true) : moveItemStackTo(stack, 0, 1, false))) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }
}

package com.carrot123.yixiangshouronglu.menu;

import com.carrot123.yixiangshouronglu.item.BiologicalContainmentDeviceItem;
import com.carrot123.yixiangshouronglu.item.ResearchResultItem;
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

public final class BiologicalContainmentDeviceMenu extends AbstractContainerMenu {
    public static final int RESULT_X = 80;
    public static final int RESULT_Y = 34;
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 80;

    private final Inventory owner;
    private final int selected;
    private final ItemStack device;
    private final int lockedIndex;

    public BiologicalContainmentDeviceMenu(int id, Inventory owner, FriendlyByteBuf buffer) {
        this(id, owner, buffer.readVarInt(), null);
    }

    public BiologicalContainmentDeviceMenu(int id, Inventory owner, int selected, ItemStack device) {
        super(ModMenus.BIOLOGICAL_CONTAINMENT_DEVICE.get(), id);
        this.owner = owner;
        this.selected = selected;
        this.device = device;
        lockedIndex = 28 + selected;

        SimpleContainer contents = new SimpleContainer(1);
        if (device != null) {
            contents.setItem(0, BiologicalContainmentDeviceItem.storedResult(device));
            contents.addListener(container -> {
                if (stillValid(owner.player)) {
                    BiologicalContainmentDeviceItem.storeResult(device, container.getItem(0));
                    owner.setChanged();
                }
            });
        }
        addSlot(new Slot(contents, 0, RESULT_X, RESULT_Y) {
            @Override public int getMaxStackSize() { return 1; }

            @Override public boolean mayPlace(ItemStack stack) {
                return ResearchResultItem.sourceType(stack) != null;
            }
        });
        for (int index = 0; index < 27; index++) {
            addSlot(new Slot(owner, index + 9, INVENTORY_X + index % 9 * 18, INVENTORY_Y + index / 9 * 18));
        }
        for (int index = 0; index < 9; index++) {
            boolean locked = index == selected;
            addSlot(new Slot(owner, index, INVENTORY_X + index * 18, INVENTORY_Y + 58) {
                @Override public boolean mayPlace(ItemStack stack) { return !locked; }
                @Override public boolean mayPickup(Player player) { return !locked; }
            });
        }
    }

    @Override public boolean stillValid(Player player) {
        if (player != owner.player || !player.isAlive() || owner.selected != selected
                || selected < 0 || selected >= 9) {
            return false;
        }
        ItemStack held = owner.getItem(selected);
        return held.is(ModItems.BIOLOGICAL_CONTAINMENT_DEVICE.get()) && (device == null || held == device);
    }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (!stillValid(player) || index == lockedIndex || (type == ClickType.SWAP && button == selected)) {
            return;
        }
        if (index == -999 || (index >= 0 && index < slots.size())) {
            super.clicked(index, button, type, player);
        }
    }

    @Override public boolean canDragTo(Slot slot) { return slot.index != lockedIndex && super.canDragTo(slot); }

    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.index != lockedIndex && super.canTakeItemForPickAll(stack, slot);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size() || index == lockedIndex) {
            return ItemStack.EMPTY;
        }
        Slot slot = getSlot(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = stack.copy();
        boolean moved = index == 0 ? moveItemStackTo(stack, 1, slots.size(), true)
                : moveItemStackTo(stack, 0, 1, false);
        if (!moved) {
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

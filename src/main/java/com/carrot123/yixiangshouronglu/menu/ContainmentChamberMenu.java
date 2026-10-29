package com.carrot123.yixiangshouronglu.menu;

import com.carrot123.yixiangshouronglu.block.entity.ContainmentChamberBlockEntity;
import com.carrot123.yixiangshouronglu.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ContainmentChamberMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    private final String entityId;

    public ContainmentChamberMenu(int windowId, Inventory inventory, FriendlyByteBuf buffer) {
        this(windowId, inventory, findContainer(inventory, buffer.readBlockPos()), new SimpleContainerData(2), buffer.readUtf(256));
    }

    public ContainmentChamberMenu(int windowId, Inventory inventory, ContainmentChamberBlockEntity chamber) {
        this(windowId, inventory, chamber, chamber.getProductionData(), chamber.getEntityId());
    }

    private ContainmentChamberMenu(int windowId, Inventory inventory, Container container, ContainerData data, String entityId) {
        super(ModMenus.CONTAINMENT_CHAMBER.get(), windowId);
        checkContainerSize(container, 2);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.data = data;
        this.entityId = entityId;
        container.startOpen(inventory.player);

        for (int index = 0; index < 2; index++) {
            addSlot(new Slot(container, index, 70 + index * 18, 45) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 110 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 168));
        }
        addDataSlots(data);
    }

    private static Container findContainer(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof ContainmentChamberBlockEntity chamber ? chamber : new SimpleContainer(2);
    }

    public String getEntityId() {
        return entityId;
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getInterval() {
        return data.get(1);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (slotIndex < 2) {
            if (!moveItemStackTo(stack, 2, 38, true)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex < 29) {
            if (!moveItemStackTo(stack, 29, 38, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 2, 29, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}

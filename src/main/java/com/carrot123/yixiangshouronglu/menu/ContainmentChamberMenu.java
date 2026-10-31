package com.carrot123.yixiangshouronglu.menu;

import com.carrot123.yixiangshouronglu.block.entity.ContainmentChamberBlockEntity;
import com.carrot123.yixiangshouronglu.production.ChamberProduction;
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
import org.jetbrains.annotations.Nullable;

public final class ContainmentChamberMenu extends AbstractContainerMenu {
    public static final int PAGE_PRODUCTION = 0;
    public static final int PAGE_EXTRACTION = 1;

    private final Container container;
    private final ContainerData data;

    @Nullable
    private final ContainmentChamberBlockEntity chamber;

    private int page = PAGE_PRODUCTION;

    public ContainmentChamberMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, findContainer(inventory, buffer.readBlockPos()), new SimpleContainerData(4));
    }

    public ContainmentChamberMenu(
            int id,
            Inventory inventory,
            ContainmentChamberBlockEntity chamber
    ) {
        this(id, inventory, chamber, chamber.getProductionData());
    }

    private ContainmentChamberMenu(
            int id,
            Inventory inventory,
            Container container,
            ContainerData data
    ) {
        super(ModMenus.CONTAINMENT_CHAMBER.get(), id);

        checkContainerSize(container, 4);
        checkContainerDataCount(data, 4);

        this.container = container;
        this.data = data;
        this.chamber = container instanceof ContainmentChamberBlockEntity entity ? entity : null;

        container.startOpen(inventory.player);

        addSlot(new Slot(container, 0, 70, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return page == PAGE_PRODUCTION;
            }

            @Override
            public boolean isActive() {
                return page == PAGE_PRODUCTION;
            }
        });

        addSlot(new Slot(container, 1, 88, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return page == PAGE_PRODUCTION;
            }

            @Override
            public boolean isActive() {
                return page == PAGE_PRODUCTION;
            }
        });

        addSlot(new Slot(container, 2, 51, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return page == PAGE_EXTRACTION && container.canPlaceItem(2, stack);
            }

            @Override
            public boolean mayPickup(Player player) {
                return page == PAGE_EXTRACTION;
            }

            @Override
            public boolean isActive() {
                return page == PAGE_EXTRACTION;
            }
        });

        addSlot(new Slot(container, 3, 111, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return page == PAGE_EXTRACTION;
            }

            @Override
            public boolean isActive() {
                return page == PAGE_EXTRACTION;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        inventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        110 + row * 18
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 168));
        }

        addDataSlots(data);
    }

    private static Container findContainer(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos)
                instanceof ContainmentChamberBlockEntity chamber) {
            return chamber;
        }

        return new SimpleContainer(4);
    }

    public int getPage() {
        return page;
    }

    public String getEntityId() {
        return chamber == null ? "" : chamber.getEntityId();
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getInterval() {
        return data.get(1);
    }

    public int getProductionProgress() {
        return data.get(0);
    }

    public int getProductionInterval() {
        return data.get(1);
    }

    public int getExtractStoredMb() {
        return data.get(2);
    }

    public int getExtractionProgress() {
        return data.get(3);
    }

    @Nullable
    public ChamberProduction.Recipe getProductionRecipe() {
        return chamber == null
                ? null
                : ChamberProduction.forEntity(chamber.getEntityId());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != PAGE_PRODUCTION && id != PAGE_EXTRACTION) {
            return false;
        }

        page = id;
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return chamber == null || chamber.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(index);

        if (!slot.hasItem() || !slot.isActive()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < 4) {
            if (!moveItemStackTo(stack, 4, 40, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean movedToInput = page == PAGE_EXTRACTION
                    && container.canPlaceItem(2, stack)
                    && moveItemStackTo(stack, 2, 3, false);

            if (!movedToInput) {
                if (index < 31) {
                    if (!moveItemStackTo(stack, 31, 40, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!moveItemStackTo(stack, 4, 31, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
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

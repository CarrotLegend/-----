package com.carrot123.yixiangshouronglu.menu;

import com.carrot123.yixiangshouronglu.block.entity.ContainmentChamberBlockEntity;
import com.carrot123.yixiangshouronglu.production.ChamberProduction;
import com.carrot123.yixiangshouronglu.registry.ModMenus;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import static com.carrot123.yixiangshouronglu.block.entity.ContainmentChamberBlockEntity.*;

public final class ContainmentChamberMenu extends AbstractContainerMenu {
    public static final int PAGE_PRODUCTION = 0;
    public static final int PAGE_EXTRACTION = 1;
    public static final int PAGE_UPGRADE = 2;
    private static final int HOTBAR_START = SLOT_COUNT + 27;
    private static final int MENU_END = HOTBAR_START + 9;
    private static final int[] OUTPUT_X = {70, 88, 106, 52, 124};

    private final Container container;
    private final ContainerData data;

    @Nullable
    private final ContainmentChamberBlockEntity chamber;

    private final DataSlot page = DataSlot.standalone();

    public ContainmentChamberMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, new SimpleContainer(SLOT_COUNT), new SimpleContainerData(DATA_COUNT),
                findChamber(inventory, buffer.readBlockPos()));
    }

    public ContainmentChamberMenu(
            int id,
            Inventory inventory,
            ContainmentChamberBlockEntity chamber
    ) {
        this(id, inventory, chamber, chamber.getProductionData(), chamber);
    }

    private ContainmentChamberMenu(
            int id,
            Inventory inventory,
            Container container,
            ContainerData data,
            @Nullable ContainmentChamberBlockEntity chamber
    ) {
        super(ModMenus.CONTAINMENT_CHAMBER.get(), id);

        checkContainerSize(container, SLOT_COUNT);
        checkContainerDataCount(data, DATA_COUNT);

        this.container = container;
        this.data = data;
        this.chamber = chamber;

        container.startOpen(inventory.player);

        for (int index = 0; index < MAX_OUTPUTS; index++) {
            addSlot(new ChamberSlot(index, OUTPUT_X[index], PAGE_PRODUCTION));
        }
        addSlot(new ChamberSlot(EXTRACTION_INPUT, 51, PAGE_EXTRACTION));
        addSlot(new ChamberSlot(EXTRACTION_OUTPUT, 111, PAGE_EXTRACTION));
        for (int index = 0; index < UPGRADE_COUNT; index++) {
            addSlot(new ChamberSlot(FIRST_UPGRADE + index, 62 + index * 18, PAGE_UPGRADE));
        }

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
        addDataSlot(page);
    }

    @Nullable
    private static ContainmentChamberBlockEntity findChamber(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos) instanceof ContainmentChamberBlockEntity entity
                ? entity : null;
    }

    public int getPage() {
        return page.get();
    }

    public int getUnlockedOutputs() {
        return Math.max(BASE_OUTPUTS, Math.min(MAX_OUTPUTS, data.get(4)));
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

    public int getExtractionInterval() {
        int interval = data.get(5);
        return interval > 0 ? interval : EXTRACT_TIME;
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
        if (id < PAGE_PRODUCTION || id > PAGE_UPGRADE || !stillValid(player)) {
            return false;
        }
        page.set(id);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().isClientSide || (chamber != null && chamber.stillValid(player));
    }

    @Override
    public void clicked(int index, int button, ClickType type, Player player) {
        if (index >= 0 && (index >= slots.size() || !slots.get(index).isActive()
                || (type == ClickType.CLONE && !slots.get(index).mayPickup(player)))) {
            return;
        }
        super.clicked(index, button, type, player);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.isActive();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.isActive();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.isActive() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        boolean movingUpgrade = index >= FIRST_UPGRADE && index < SLOT_COUNT;
        ItemStack stack = movingUpgrade ? slot.getItem().copy() : slot.getItem();
        ItemStack original = stack.copy();
        if (index < SLOT_COUNT) {
            if (!moveItemStackTo(stack, SLOT_COUNT, MENU_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveToChamber(stack)) {
            boolean moved = index < HOTBAR_START
                    ? moveItemStackTo(stack, HOTBAR_START, MENU_END, false)
                    : moveItemStackTo(stack, SLOT_COUNT, HOTBAR_START, false);
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        if (movingUpgrade || stack.isEmpty()) {
            slot.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    private boolean moveToChamber(ItemStack stack) {
        if (getPage() == PAGE_EXTRACTION && getSlot(EXTRACTION_INPUT).mayPlace(stack)) {
            return moveItemStackTo(stack, EXTRACTION_INPUT, EXTRACTION_INPUT + 1, false);
        }
        if (getPage() == PAGE_UPGRADE && isUpgrade(stack)) {
            for (int index = FIRST_UPGRADE; index < SLOT_COUNT; index++) {
                if (!getSlot(index).hasItem() && getSlot(index).mayPlace(stack)) {
                    return moveItemStackTo(stack, index, index + 1, false);
                }
            }
        }
        return false;
    }

    private final class ChamberSlot extends Slot {
        private final int slot;
        private final int slotPage;

        private ChamberSlot(int slot, int x, int slotPage) {
            super(ContainmentChamberMenu.this.container, slot, x, 45);
            this.slot = slot;
            this.slotPage = slotPage;
        }

        @Override
        public boolean isActive() {
            return getPage() == slotPage && (slotPage != PAGE_PRODUCTION || slot < getUnlockedOutputs());
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!isActive()) {
                return false;
            }
            if (slot >= FIRST_UPGRADE) {
                return isUpgrade(stack);
            }
            return slot == EXTRACTION_INPUT && acceptsExtraction(getEntityId(), stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            if (!isActive()) {
                return false;
            }
            if (slot < FIRST_UPGRADE) {
                return true;
            }
            if (container instanceof ContainmentChamberBlockEntity entity) {
                return entity.canChangeUpgrade(slot, ItemStack.EMPTY);
            }
            return !getItem().is(ModItems.CAPACITY_UPGRADE.get())
                    || container.getItem(getUnlockedOutputs() - 1).isEmpty();
        }

        @Override
        public int getMaxStackSize() {
            return slot >= FIRST_UPGRADE ? 1 : super.getMaxStackSize();
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}

package com.carrot123.yixiangshouronglu.menu;

import com.carrot123.yixiangshouronglu.block.entity.GeneSynthesizerBlockEntity;
import com.carrot123.yixiangshouronglu.registry.*;
import com.carrot123.yixiangshouronglu.item.ResearchResultItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;

public final class GeneSynthesizerMenu extends AbstractContainerMenu {
    public static final int RESEARCH_PAGE = 0;
    public static final int CHIMERA_PAGE = 1;
    public static final int RESEARCH_INPUT_X = 45;
    public static final int RESEARCH_OUTPUT_X = 205;
    public static final int RESEARCH_Y = 54;
    private static final int CHIMERA_START = 36;
    private static final int RESEARCH_START = CHIMERA_START + 5;
    private final DataSlot page = DataSlot.standalone();
    private final BlockPos pos;
    private final Inventory playerInventory;
    private final GeneSynthesizerBlockEntity machine;
    private final ContainerData data;
    public GeneSynthesizerMenu(int id, Inventory inventory, FriendlyByteBuf buffer) { this(id, inventory, buffer.readBlockPos(), null); }
    public GeneSynthesizerMenu(int id, Inventory inventory, GeneSynthesizerBlockEntity machine) { this(id, inventory, machine.getBlockPos(), machine); }
    private GeneSynthesizerMenu(int id, Inventory inventory, BlockPos pos, GeneSynthesizerBlockEntity machine) {
        super(ModMenus.GENE_SYNTHESIZER.get(), id); this.pos = pos; playerInventory = inventory; this.machine = machine;
        data = machine == null ? new SimpleContainerData(3) : new ContainerData() {
            public int get(int index) { return switch (index) { case 0 -> machine.chimeraProgress(); case 1 -> machine.chimeraMatched() ? 1 : 0; case 2 -> machine.researchProgress(); default -> 0; }; }
            public void set(int index, int value) {}
            public int getCount() { return 3; }
        };
        addInventory(inventory, 51, 121);
        IItemHandler chimera = machine == null ? new ItemStackHandler(5) : machine.chimeraInventory;
        for (int slot = 0; slot < 5; slot++) {
            addSlot(new WorkSlot(chimera, slot, chimeraX(slot), chimeraY(slot), CHIMERA_PAGE, slot < 4));
        }
        IItemHandler research = machine == null ? new ItemStackHandler(2) : machine.researchInventory;
        addSlot(new WorkSlot(research, 0, RESEARCH_INPUT_X, RESEARCH_Y, RESEARCH_PAGE, true));
        addSlot(new WorkSlot(research, 1, RESEARCH_OUTPUT_X, RESEARCH_Y, RESEARCH_PAGE, false));
        addDataSlots(data);
        addDataSlot(page);
    }
    private void addInventory(Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) { for (int col = 0; col < 9; col++) { addSlot(new Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18)); } }
        for (int col = 0; col < 9; col++) { addSlot(new Slot(inventory, col, x + col * 18, y + 58)); }
    }
    public static int chimeraX(int slot) { return slot == 4 ? 125 : 44 + slot % 2 * 162; }
    public static int chimeraY(int slot) { return slot == 4 ? 55 : 27 + slot / 2 * 56; }
    public int chimeraProgress() { return data.get(0); }
    public boolean chimeraMatched() { return data.get(1) != 0; }

    public int researchProgress() { return data.get(2); }
    public int page() { return page.get(); }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (machine == null || !stillValid(player) || (button != RESEARCH_PAGE && button != CHIMERA_PAGE)) {
            return false;
        }
        resetQuickCraft();
        page.set(button);
        broadcastChanges();
        return true;
    }

    private final class WorkSlot extends SlotItemHandler {
        private final boolean input;

        private final int slotPage;

        private WorkSlot(IItemHandler handler, int index, int x, int y, int slotPage, boolean input) {
            super(handler, index, x, y);
            this.input = input;
            this.slotPage = slotPage;
        }

        @Override public void setChanged() {
            super.setChanged();
            if (machine != null) {
                machine.setChanged();
            }
        }

        @Override public boolean isActive() { return page() == slotPage; }

        @Override public boolean mayPlace(ItemStack stack) {
            return input && isActive() && (slotPage != RESEARCH_PAGE || ResearchResultItem.canResearch(stack))
                    && super.mayPlace(stack);
        }

        @Override public boolean mayPickup(Player player) { return isActive() && super.mayPickup(player); }
    }

    @Override public boolean stillValid(Player player) { return playerInventory.player == player && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64 && player.level().hasChunkAt(pos) && player.level().getBlockState(pos).is(ModBlocks.GENE_SYNTHESIZER.get()) && (machine == null || player.level().getBlockEntity(pos) == machine); }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (stillValid(player) && (slot == -999 || slot >= 0 && slot < slots.size() && getSlot(slot).isActive())) {
            super.clicked(slot, button, type, player);
        }
    }

    @Override public boolean canDragTo(Slot slot) { return slot.isActive() && super.canDragTo(slot); }

    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.isActive() && super.canTakeItemForPickAll(stack, slot);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size() || !getSlot(index).isActive()) { return ItemStack.EMPTY; }
        Slot slot = slots.get(index); if (!slot.hasItem()) { return ItemStack.EMPTY; }
        ItemStack stack = slot.getItem(); ItemStack original = stack.copy();
        if (index >= CHIMERA_START) {
            if (!moveItemStackTo(stack, 0, CHIMERA_START, true)) { return ItemStack.EMPTY; }
        } else {
            int start = page() == RESEARCH_PAGE ? RESEARCH_START : CHIMERA_START;
            int end = page() == RESEARCH_PAGE ? RESEARCH_START + 1 : CHIMERA_START + 4;
            if (!moveItemStackTo(stack, start, end, false)) { return ItemStack.EMPTY; }
        }
        if (stack.isEmpty()) { slot.set(ItemStack.EMPTY); } else { slot.setChanged(); }
        slot.onTake(player, stack); return original;
    }
}

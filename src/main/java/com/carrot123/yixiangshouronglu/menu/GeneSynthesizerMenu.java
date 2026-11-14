package com.carrot123.yixiangshouronglu.menu;

import com.carrot123.yixiangshouronglu.block.entity.GeneSynthesizerBlockEntity;
import com.carrot123.yixiangshouronglu.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;

public final class GeneSynthesizerMenu extends AbstractContainerMenu {
    public static final int PAGE_GENE = 0;
    public static final int PAGE_CHIMERA = 1;
    public static final int PAGE_BUTTON = 2;
    private static final int CHIMERA_START = 41;
    private final DataSlot page = DataSlot.standalone();
    private final BlockPos pos;
    private final Inventory playerInventory;
    private final GeneSynthesizerBlockEntity machine;
    private final ContainerData data;
    public GeneSynthesizerMenu(int id, Inventory inventory, FriendlyByteBuf buffer) { this(id, inventory, buffer.readBlockPos(), null); }
    public GeneSynthesizerMenu(int id, Inventory inventory, GeneSynthesizerBlockEntity machine) { this(id, inventory, machine.getBlockPos(), machine); }
    private GeneSynthesizerMenu(int id, Inventory inventory, BlockPos pos, GeneSynthesizerBlockEntity machine) {
        super(ModMenus.GENE_SYNTHESIZER.get(), id); this.pos = pos; playerInventory = inventory; this.machine = machine;
        IItemHandler handler = machine == null ? new ItemStackHandler(5) : machine.inventory;
        data = machine == null ? new SimpleContainerData(2) : new ContainerData() {
            public int get(int index) { return index == 0 ? machine.chimeraProgress() : machine.chimeraMatched() ? 1 : 0; }
            public void set(int index, int value) {}
            public int getCount() { return 2; }
        };
        for (int slot = 0; slot < 4; slot++) { addSlot(new PageSlot(handler, slot, 22 + (slot % 2) * 22, 48 + (slot / 2) * 22, PAGE_GENE)); }
        addSlot(new PageSlot(handler, 4, 142, 59, PAGE_GENE));
        addInventory(inventory, 51, 121);
        IItemHandler chimera = machine == null ? new ItemStackHandler(5) : machine.chimeraInventory;
        for (int slot = 0; slot < 5; slot++) {
            addSlot(new PageSlot(chimera, slot, chimeraX(slot), chimeraY(slot), PAGE_CHIMERA));
        }
        addDataSlots(data);
        addDataSlot(page);
    }
    private void addInventory(Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) { for (int col = 0; col < 9; col++) { addSlot(new Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18)); } }
        for (int col = 0; col < 9; col++) { addSlot(new Slot(inventory, col, x + col * 18, y + 58)); }
    }
    public static int chimeraX(int slot) { return slot == 4 ? 125 : 44 + slot % 2 * 162; }
    public static int chimeraY(int slot) { return slot == 4 ? 55 : 27 + slot / 2 * 56; }
    public int page() { return page.get(); }
    public int chimeraProgress() { return data.get(0); }
    public boolean chimeraMatched() { return data.get(1) != 0; }

    private final class PageSlot extends SlotItemHandler {
        private final int slotPage;
        private final boolean input;

        private PageSlot(IItemHandler handler, int index, int x, int y, int slotPage) {
            super(handler, index, x, y);
            this.slotPage = slotPage;
            input = slotPage == PAGE_CHIMERA && index < 4;
        }

        @Override public void setChanged() {
            super.setChanged();
            if (machine != null) {
                machine.setChanged();
            }
        }

        @Override public boolean isActive() { return page() == slotPage; }
        @Override public boolean mayPlace(ItemStack stack) { return input && !stack.is(ModItems.GENE_MODULE.get()) && isActive() && super.mayPlace(stack); }
        @Override public boolean mayPickup(Player player) { return isActive() && super.mayPickup(player); }
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (machine == null || !stillValid(player) || button < PAGE_BUTTON || button > PAGE_BUTTON + PAGE_CHIMERA) {
            return false;
        }
        resetQuickCraft();
        page.set(button - PAGE_BUTTON);
        broadcastChanges();
        return true;
    }
    @Override public boolean stillValid(Player player) { return playerInventory.player == player && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64 && player.level().hasChunkAt(pos) && player.level().getBlockState(pos).is(ModBlocks.GENE_SYNTHESIZER.get()) && (machine == null || player.level().getBlockEntity(pos) == machine); }
    @Override public void clicked(int slot, int button, ClickType type, Player player) { if (stillValid(player) && (slot < 0 || slot < slots.size() && getSlot(slot).isActive())) { super.clicked(slot, button, type, player); } }
    @Override public boolean canDragTo(Slot slot) { return slot.isActive() && super.canDragTo(slot); }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.isActive() && super.canTakeItemForPickAll(stack, slot);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size() || !getSlot(index).isActive()) { return ItemStack.EMPTY; }
        Slot slot = slots.get(index); if (!slot.hasItem()) { return ItemStack.EMPTY; }
        ItemStack stack = slot.getItem(); ItemStack original = stack.copy();
        if (index < 5 || index >= CHIMERA_START) {
            if (!moveItemStackTo(stack, 5, CHIMERA_START, true)) { return ItemStack.EMPTY; }
        } else {
            if (page() != PAGE_CHIMERA || stack.is(ModItems.GENE_MODULE.get())) { return ItemStack.EMPTY; }
            int start = CHIMERA_START;
            if (!moveItemStackTo(stack, start, start + 4, false)) { return ItemStack.EMPTY; }
        }
        if (stack.isEmpty()) { slot.set(ItemStack.EMPTY); } else { slot.setChanged(); }
        slot.onTake(player, stack); return original;
    }
}

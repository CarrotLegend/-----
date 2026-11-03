package com.carrot123.yixiangshouronglu.menu;

import com.carrot123.yixiangshouronglu.block.entity.GeneSynthesizerBlockEntity;
import com.carrot123.yixiangshouronglu.gene.GeneTypes;
import com.carrot123.yixiangshouronglu.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;

public final class GeneSynthesizerMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Inventory playerInventory;
    private final GeneSynthesizerBlockEntity machine;
    private final ContainerData data;
    private final SimpleContainer preview = new SimpleContainer(1);
    public GeneSynthesizerMenu(int id, Inventory inventory, FriendlyByteBuf buffer) { this(id, inventory, buffer.readBlockPos(), null); }
    public GeneSynthesizerMenu(int id, Inventory inventory, GeneSynthesizerBlockEntity machine) { this(id, inventory, machine.getBlockPos(), machine); }
    private GeneSynthesizerMenu(int id, Inventory inventory, BlockPos pos, GeneSynthesizerBlockEntity machine) {
        super(ModMenus.GENE_SYNTHESIZER.get(), id); this.pos = pos; playerInventory = inventory; this.machine = machine;
        IItemHandler handler = machine == null ? new ItemStackHandler(5) : machine.inventory;
        data = machine == null ? new SimpleContainerData(5) : new ContainerData() {
            public int get(int index) { return switch (index) { case 0 -> machine.progress() & 65535; case 1 -> machine.progress() >>> 16; case 2 -> machine.duration() & 65535; case 3 -> machine.duration() >>> 16; case 4 -> machine.mode().ordinal(); default -> 0; }; }
            public void set(int index, int value) {}
            public int getCount() { return 5; }
        };
        for (int slot = 0; slot < 4; slot++) { addSlot(new SlotItemHandler(handler, slot, 22 + (slot % 2) * 22, 48 + (slot / 2) * 22)); }
        addSlot(new SlotItemHandler(handler, 4, 142, 59) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        addSlot(new Slot(preview, 0, -10000, -10000) {
            @Override public boolean isActive() { return false; }
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
        });
        addInventory(inventory, 51, 121); addDataSlots(data);
    }
    private void addInventory(Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) { for (int col = 0; col < 9; col++) { addSlot(new Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18)); } }
        for (int col = 0; col < 9; col++) { addSlot(new Slot(inventory, col, x + col * 18, y + 58)); }
    }
    public int progress() { return data.get(0) & 65535 | data.get(1) << 16; }
    public int duration() { return data.get(2) & 65535 | data.get(3) << 16; }
    public GeneTypes.Mode mode() { return data.get(4) == 1 ? GeneTypes.Mode.UPGRADE : GeneTypes.Mode.SYNTHESIS; }
    public ItemStack preview() { return preview.getItem(0); }
    @Override public void broadcastChanges() { if (machine != null) { preview.setItem(0, machine.preview()); } super.broadcastChanges(); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (machine == null || !stillValid(player) || button < 0 || button > 1) { return false; }
        machine.mode(GeneTypes.Mode.values()[button]); broadcastChanges(); return true;
    }
    @Override public boolean stillValid(Player player) { return playerInventory.player == player && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64 && player.level().hasChunkAt(pos) && player.level().getBlockState(pos).is(ModBlocks.GENE_SYNTHESIZER.get()); }
    @Override public void clicked(int slot, int button, ClickType type, Player player) { if (slot != 5) { super.clicked(slot, button, type, player); } }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || index == 5) { return ItemStack.EMPTY; }
        Slot slot = slots.get(index); if (!slot.hasItem()) { return ItemStack.EMPTY; }
        ItemStack stack = slot.getItem(); ItemStack original = stack.copy();
        if (index < 5) { if (!moveItemStackTo(stack, 6, slots.size(), true)) { return ItemStack.EMPTY; } }
        else if (!moveItemStackTo(stack, 0, 4, false)) { return ItemStack.EMPTY; }
        if (stack.isEmpty()) { slot.set(ItemStack.EMPTY); } else { slot.setChanged(); }
        slot.onTake(player, stack); return original;
    }
}

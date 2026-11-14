package com.carrot123.yixiangshouronglu.menu;

import com.carrot123.yixiangshouronglu.block.entity.GeneModificationChamberBlockEntity;
import com.carrot123.yixiangshouronglu.registry.ModBlocks;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import com.carrot123.yixiangshouronglu.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class GeneModificationMenu extends AbstractContainerMenu {
    public static final int GENES = 0;
    public static final int LIMBS = 1;
    public static final int GENE_SLOTS = 8;
    public static final int INVENTORY_X = 51;
    public static final int INVENTORY_Y = 106;
    private static final int SIZE = GeneModificationChamberBlockEntity.SIZE;
    private final BlockPos pos;
    private final Player owner;
    private final GeneModificationChamberBlockEntity chamber;
    private final DataSlot page = DataSlot.standalone();

    public GeneModificationMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), null);
    }

    public GeneModificationMenu(int id, Inventory inventory, GeneModificationChamberBlockEntity chamber) {
        this(id, inventory, chamber.getBlockPos(), chamber);
    }

    private GeneModificationMenu(int id, Inventory inventory, BlockPos pos, GeneModificationChamberBlockEntity chamber) {
        super(ModMenus.GENE_MODIFICATION_CHAMBER.get(), id);
        this.pos = pos;
        this.chamber = chamber;
        owner = inventory.player;
        SimpleContainer contents = chamber == null ? new SimpleContainer(SIZE) : chamber.inventory();
        for (int index = 0; index < SIZE; index++) {
            int slotPage = index < GENE_SLOTS ? GENES : LIMBS;
            addSlot(new Slot(contents, index, slotX(index), slotY(index)) {
                @Override public boolean isActive() { return page() == slotPage; }
                @Override public boolean mayPickup(Player player) { return isActive(); }
                @Override public boolean mayPlace(ItemStack stack) {
                    return isActive() && !stack.is(ModItems.GENE_MODULE.get());
                }
            });
        }
        for (int index = 0; index < 27; index++) {
            addSlot(new Slot(inventory, index + 9, INVENTORY_X + index % 9 * 18, INVENTORY_Y + index / 9 * 18));
        }
        for (int index = 0; index < 9; index++) {
            addSlot(new Slot(inventory, index, INVENTORY_X + index * 18, INVENTORY_Y + 58));
        }
        addDataSlot(page);
    }

    public static int slotX(int index) {
        return index < GENE_SLOTS ? 31 + index * 27 : 62 + (index - GENE_SLOTS) % 2 * 108;
    }

    public static int slotY(int index) {
        return index < GENE_SLOTS ? 33 + index % 2 * 39 : 34 + (index - GENE_SLOTS) / 2 * 31;
    }

    public int page() { return page.get(); }

    @Override public boolean stillValid(Player player) {
        return player == owner && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64
                && player.level().hasChunkAt(pos) && player.level().getBlockState(pos).is(ModBlocks.GENE_MODIFICATION_CHAMBER.get())
                && (chamber == null || player.level().getBlockEntity(pos) == chamber);
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (chamber == null || !stillValid(player) || (button != GENES && button != LIMBS)) {
            return false;
        }
        resetQuickCraft();
        page.set(button);
        broadcastChanges();
        return true;
    }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (stillValid(player) && (index < 0 || index < slots.size() && getSlot(index).isActive())) {
            super.clicked(index, button, type, player);
        }
    }

    @Override public boolean canDragTo(Slot slot) { return slot.isActive() && super.canDragTo(slot); }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.isActive() && super.canTakeItemForPickAll(stack, slot);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size() || !getSlot(index).isActive()) {
            return ItemStack.EMPTY;
        }
        Slot slot = getSlot(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = stack.copy();
        if (index < SIZE) {
            if (!moveItemStackTo(stack, SIZE, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (stack.is(ModItems.GENE_MODULE.get())
                    || !moveItemStackTo(stack, page() == GENES ? 0 : GENE_SLOTS, page() == GENES ? GENE_SLOTS : SIZE, false)) {
                return ItemStack.EMPTY;
            }
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

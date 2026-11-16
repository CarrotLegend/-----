package com.carrot123.yixiangshouronglu.menu;

import com.carrot123.yixiangshouronglu.block.entity.GeneModificationChamberBlockEntity;
import com.carrot123.yixiangshouronglu.registry.ModBlocks;
import com.carrot123.yixiangshouronglu.registry.ModMenus;
import com.carrot123.yixiangshouronglu.item.GeneFragmentItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class GeneModificationMenu extends AbstractContainerMenu {
    public static final int PANEL_WIDTH = 396;
    public static final int PANEL_HEIGHT = 272;
    public static final int INVENTORY_X = (PANEL_WIDTH - 9 * 18) / 2 + 1;
    public static final int INVENTORY_Y = 194;
    private static final int SIZE = GeneModificationChamberBlockEntity.SIZE;
    private final BlockPos pos;
    private final Player owner;
    private final GeneModificationChamberBlockEntity chamber;

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
        SimpleContainer contents = chamber == null ? new SimpleContainer(SIZE) {
            @Override public boolean canPlaceItem(int slot, ItemStack stack) {
                return GeneFragmentItem.sourceType(stack) != null;
            }
        } : chamber.inventory();
        for (int index = 0; index < SIZE; index++) {
            addSlot(new Slot(contents, index, slotX(index), slotY(index)) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(getContainerSlot(), stack);
                }
            });
        }
        for (int index = 0; index < 27; index++) {
            addSlot(new Slot(inventory, index + 9, INVENTORY_X + index % 9 * 18, INVENTORY_Y + index / 9 * 18));
        }
        for (int index = 0; index < 9; index++) {
            addSlot(new Slot(inventory, index, INVENTORY_X + index * 18, INVENTORY_Y + 58));
        }
    }

    public static final int REGION_COUNT = 6;

    public static int firstSlot(int region) {
        return region == 0 ? 0 : 1 + (region - 1) * 4;
    }

    public static int region(int index) {
        return index == 0 ? 0 : 1 + (index - 1) / 4;
    }

    public static int slotX(int index) {
        return 162 + (index - firstSlot(region(index))) * 18;
    }

    public static int slotY(int index) {
        return 32 + region(index) * 24;
    }

    @Override public boolean stillValid(Player player) {
        return player == owner && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64
                && player.level().hasChunkAt(pos) && player.level().getBlockState(pos).is(ModBlocks.GENE_MODIFICATION_CHAMBER.get())
                && (chamber == null || player.level().getBlockEntity(pos) == chamber);
    }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (stillValid(player) && (index == -999 || (index >= 0 && index < slots.size()))) {
            super.clicked(index, button, type, player);
        }
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = getSlot(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = stack.copy();
        boolean moved = index < SIZE ? moveItemStackTo(stack, SIZE, slots.size(), true)
                : moveItemStackTo(stack, 0, SIZE, false);
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

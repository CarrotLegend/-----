package com.carrot123.yixiangshouronglu.block.entity;

import com.carrot123.yixiangshouronglu.menu.GeneModificationMenu;
import com.carrot123.yixiangshouronglu.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class GeneModificationChamberBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SIZE = 12;

    private final SimpleContainer inventory = new SimpleContainer(SIZE) {
        @Override public void setChanged() {
            super.setChanged();
            GeneModificationChamberBlockEntity.this.setChanged();
        }
    };

    public GeneModificationChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENE_MODIFICATION_CHAMBER.get(), pos, state);
    }

    public SimpleContainer inventory() {
        return inventory;
    }

    public void dropInventory() {
        if (level != null && !level.isClientSide) {
            Containers.dropContents(level, worldPosition, inventory);
            inventory.clearContent();
        }
    }

    @Override protected void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < SIZE; i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(data, items);
    }

    @Override public void load(CompoundTag data) {
        super.load(data);
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(data, items);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, items.get(i));
        }
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.yixiangshouronglu.gene_modification_chamber");
    }

    @Override public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new GeneModificationMenu(id, playerInventory, this);
    }
}

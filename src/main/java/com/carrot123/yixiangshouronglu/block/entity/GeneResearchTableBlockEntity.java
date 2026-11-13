package com.carrot123.yixiangshouronglu.block.entity;

import com.carrot123.yixiangshouronglu.menu.GeneResearchTableMenu;
import com.carrot123.yixiangshouronglu.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class GeneResearchTableBlockEntity extends BlockEntity implements MenuProvider {
    private final SimpleContainer items = new SimpleContainer(2) {
        @Override public void setChanged() {
            super.setChanged();
            GeneResearchTableBlockEntity.this.setChanged();
        }
    };

    public GeneResearchTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENE_RESEARCH_TABLE.get(), pos, state);
    }

    public SimpleContainer items() {
        return items;
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.yixiangshouronglu.gene_research_table");
    }

    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GeneResearchTableMenu(id, inventory, this);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("ResearchInput", items.getItem(0).save(new CompoundTag()));
        tag.put("SynthesisOutput", items.getItem(1).save(new CompoundTag()));
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        items.setItem(0, ItemStack.of(tag.getCompound("ResearchInput")));
        items.setItem(1, ItemStack.of(tag.getCompound("SynthesisOutput")));
    }
}

package com.carrot123.yixiangshouronglu.block.entity;

import com.carrot123.yixiangshouronglu.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ContainmentChamberBlockEntity extends BlockEntity {
    public ContainmentChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTAINMENT_CHAMBER.get(), pos, state);
    }
}

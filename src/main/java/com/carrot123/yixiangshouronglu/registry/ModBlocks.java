package com.carrot123.yixiangshouronglu.registry;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.block.ContainmentChamberBlock;
import com.carrot123.yixiangshouronglu.block.IncubatingDragonEggBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AnomalousContainmentChronicles.MOD_ID);
    public static final RegistryObject<Block> CONTAINMENT_CHAMBER = BLOCKS.register("containment_chamber", () -> new ContainmentChamberBlock(BlockBehaviour.Properties.of().strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> INCUBATING_DRAGON_EGG = BLOCKS.register("incubating_dragon_egg", () -> new IncubatingDragonEggBlock(BlockBehaviour.Properties.of().strength(3.0F, 9.0F).sound(SoundType.STONE).noOcclusion()));

    private ModBlocks() {
    }
}

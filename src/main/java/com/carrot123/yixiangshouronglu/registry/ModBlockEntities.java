package com.carrot123.yixiangshouronglu.registry;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.block.entity.ContainmentChamberBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AnomalousContainmentChronicles.MOD_ID);
    public static final RegistryObject<BlockEntityType<ContainmentChamberBlockEntity>> CONTAINMENT_CHAMBER = BLOCK_ENTITIES.register("containment_chamber", () -> BlockEntityType.Builder.of(ContainmentChamberBlockEntity::new, ModBlocks.CONTAINMENT_CHAMBER.get()).build(null));

    private ModBlockEntities() {
    }
}

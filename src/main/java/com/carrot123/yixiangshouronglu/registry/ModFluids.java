package com.carrot123.yixiangshouronglu.registry;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.fluid.EternalSpringFluidType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, AnomalousContainmentChronicles.MOD_ID);

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, AnomalousContainmentChronicles.MOD_ID);

    public static final RegistryObject<FluidType> ETERNAL_SPRING_TYPE = FLUID_TYPES.register(
            "eternal_spring",
            () -> new EternalSpringFluidType(
                    FluidType.Properties.create()
                            .descriptionId("fluid_type.yixiangshouronglu.eternal_spring")
                            .density(1000)
                            .viscosity(1000)
                            .temperature(300)
                            .canSwim(true)
                            .canDrown(true)
                            .canPushEntity(true)
                            .canExtinguish(true)
                            .canHydrate(true)
                            .canConvertToSource(true)
                            .supportsBoating(true)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
            )
    );

    private static final ForgeFlowingFluid.Properties PROPERTIES = new ForgeFlowingFluid.Properties(
            ETERNAL_SPRING_TYPE,
            () -> ModFluids.ETERNAL_SPRING.get(),
            () -> ModFluids.FLOWING_ETERNAL_SPRING.get()
    )
            .bucket(() -> ModItems.ETERNAL_SPRING_BUCKET.get())
            .block(() -> ModBlocks.ETERNAL_SPRING_BLOCK.get())
            .tickRate(5)
            .slopeFindDistance(4)
            .levelDecreasePerBlock(1)
            .explosionResistance(100.0F);

    public static final RegistryObject<ForgeFlowingFluid.Source> ETERNAL_SPRING = FLUIDS.register(
            "eternal_spring",
            () -> new ForgeFlowingFluid.Source(PROPERTIES)
    );

    public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_ETERNAL_SPRING = FLUIDS.register(
            "flowing_eternal_spring",
            () -> new ForgeFlowingFluid.Flowing(PROPERTIES)
    );

    private ModFluids() {
    }
}

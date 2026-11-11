package com.carrot123.yixiangshouronglu.registry;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.worldgen.EternalSpringFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, AnomalousContainmentChronicles.MOD_ID);
    public static final RegistryObject<Feature<?>> ETERNAL_SPRING =
            FEATURES.register("eternal_spring", EternalSpringFeature::new);

    private ModFeatures() {
    }
}

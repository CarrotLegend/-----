package com.carrot123.yixiangshouronglu.registry;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, AnomalousContainmentChronicles.MOD_ID);
    public static final RegistryObject<MobEffect> CHILL = EFFECTS.register("chill", ChillEffect::new);

    public static final RegistryObject<MobEffect> SEARING_BURN = EFFECTS.register("searing_burn",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0xE65C21) {});

    private ModEffects() {}

    private static final class ChillEffect extends MobEffect {
        private ChillEffect() {
            super(MobEffectCategory.HARMFUL, 0x79CBE9);
        }
    }
}

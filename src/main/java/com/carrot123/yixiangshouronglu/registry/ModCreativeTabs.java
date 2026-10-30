
package com.carrot123.yixiangshouronglu.registry;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    AnomalousContainmentChronicles.MOD_ID
            );

    public static final RegistryObject<CreativeModeTab> MAIN =
            CREATIVE_TABS.register("main",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.yixiangshouronglu.main"))
                            .icon(() -> new ItemStack(ModItems.RESONANCE_CORE.get()))
                            .displayItems((parameters, output) -> {
                                output.accept(ModItems.SOUL.get());
                                output.accept(ModItems.RESONANCE_CORE.get());
                                output.accept(ModItems.WENDIGO_EYE.get());
                                output.accept(ModItems.TENACIOUS_SHIELD_FRAGMENT.get());
                                output.accept(ModItems.TENACIOUS_SHIELD_SPAWN_EGG.get());
                                output.accept(ModItems.EXEMPTION_SHIELD.get());
                                output.accept(ModItems.WENDIGO_SIGHT.get());
                                output.accept(ModItems.BLAZE_CAGE.get());
                                output.accept(ModItems.ENDER_DRAGON_WINGS.get());
                                output.accept(ModItems.REFINED_DRAGON_MEAL.get());
                                output.accept(ModItems.SAPHYRA_GEM.get());
                            })
                            .build()
            );

    private ModCreativeTabs() {
    }
}

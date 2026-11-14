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
                            .icon(() -> new ItemStack(ModItems.CONTAINMENT_CHAMBER.get()))
                            .displayItems((parameters, output) -> {
                                output.accept(ModItems.SOUL.get());
                                output.accept(ModItems.LEGENDARY_CARROT_KING_SPAWN_EGG.get());
                                output.accept(ModItems.LEGENDARY_CARROT_KING_SUMMONER.get());
                                output.accept(ModItems.PANACEA.get());
                                output.accept(ModItems.SOUL_GUIDING_LANTERN.get());
                                output.accept(ModItems.FINAL_KNELL.get());
                                output.accept(ModItems.MOB_CONTAINER.get());
                                output.accept(ModItems.CAPACITY_UPGRADE.get());
                                output.accept(ModItems.SPEED_UPGRADE.get());
                                output.accept(ModItems.EXTRACTION_UPGRADE.get());
                                output.accept(ModItems.WENDIGO_EYE.get());
                                output.accept(ModItems.TENACIOUS_SHIELD_FRAGMENT.get());
                                output.accept(ModItems.TENACIOUS_SHIELD_SPAWN_EGG.get());
                                output.accept(ModItems.SHADOW_DEMON_SPAWN_EGG.get());
                                output.accept(ModItems.SHADOW_FRAGMENT.get());
                                output.accept(ModItems.EXEMPTION_SHIELD.get());
                                output.accept(ModItems.WENDIGO_SIGHT.get());
                                output.accept(ModItems.BLAZE_CAGE.get());
                                output.accept(ModItems.ENDER_DRAGON_WINGS.get());
                                output.accept(ModItems.DRAGON_NEST_TELEPORTER.get());
                                output.accept(ModItems.REFINED_DRAGON_MEAL.get());
                                output.accept(ModItems.SAPHYRA_GEM.get());
                                output.accept(ModItems.ECHO_CAGE.get());
                                output.accept(ModItems.ZOMBIE_BRAIN.get());
                                output.accept(ModItems.ROTTEN_BRAIN.get());
                                output.accept(ModItems.BONE_CAGE.get());
                                output.accept(ModItems.AMETHYST_CRAB_LURE.get());
                                output.accept(ModItems.SEDATIVE_MEAT.get());
                                output.accept(ModItems.EXTRACTOR.get());
                                output.accept(ModItems.GENE_SYNTHESIZER.get());
                                output.accept(ModItems.GENE_RESEARCH_TABLE.get());
                                output.accept(ModItems.GENE_MODIFIER_HEALTH.get());
                                output.accept(ModItems.GENE_MODIFIER_ATTACK.get());
                                output.accept(ModItems.GENE_MODIFIER_ARMOR.get());
                                output.accept(ModItems.GENE_MODIFIER_ARMOR_SHRED.get());
                                output.accept(ModItems.GENE_MODIFIER_FROST.get());
                                output.accept(ModItems.GENE_MODIFICATION_CHAMBER.get());
                                output.accept(ModItems.ETERNAL_SPRING_BUCKET.get());
                                output.accept(ModItems.BOTTLED_ETERNAL_SPRING.get());
                                output.accept(ModItems.MADNESS_MASK.get());
                            })
                            .build()
            );

    private ModCreativeTabs() {
    }
}

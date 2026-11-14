package com.carrot123.yixiangshouronglu;

import com.carrot123.yixiangshouronglu.gene.research.GeneResearchRegistry;
import com.carrot123.yixiangshouronglu.gene.research.PlayerGeneResearch;
import com.carrot123.yixiangshouronglu.network.GeneResearchNetwork;

import com.carrot123.yixiangshouronglu.network.ShopNetwork;
import com.carrot123.yixiangshouronglu.network.LanternNetwork;
import com.carrot123.yixiangshouronglu.shop.PlayerShopData;
import com.carrot123.yixiangshouronglu.gene.PlayerGenes;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import com.carrot123.yixiangshouronglu.registry.ModGeneRecipes;
import com.carrot123.yixiangshouronglu.registry.ModBlockEntities;
import com.carrot123.yixiangshouronglu.registry.ModBlocks;
import com.carrot123.yixiangshouronglu.registry.ModCreativeTabs;
import com.carrot123.yixiangshouronglu.registry.ModEntities;
import com.carrot123.yixiangshouronglu.registry.ModFluids;
import com.carrot123.yixiangshouronglu.registry.ModFeatures;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import com.carrot123.yixiangshouronglu.registry.ModMenus;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AnomalousContainmentChronicles.MOD_ID)
public final class AnomalousContainmentChronicles {
    public static final String MOD_ID = "yixiangshouronglu";

    public AnomalousContainmentChronicles(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModCreativeTabs.CREATIVE_TABS.register(modBus);
        ModGeneRecipes.TYPES.register(modBus);
        ModGeneRecipes.SERIALIZERS.register(modBus);
        modBus.addListener((RegisterCapabilitiesEvent event) -> event.register(PlayerGenes.class));
        modBus.addListener((RegisterCapabilitiesEvent event) -> event.register(PlayerGeneResearch.class));
        modBus.addListener((RegisterCapabilitiesEvent event) -> event.register(PlayerShopData.class));
        ShopNetwork.register();
        LanternNetwork.register();
        GeneResearchNetwork.register();
        GeneResearchRegistry.registerDefaults();
        ModFluids.FLUID_TYPES.register(modBus);
        ModFluids.FLUIDS.register(modBus);
        ModFeatures.FEATURES.register(modBus);
    }
}

package com.carrot123.yixiangshouronglu;

import com.carrot123.yixiangshouronglu.network.GeneNetwork;
import com.carrot123.yixiangshouronglu.network.ShopNetwork;
import com.carrot123.yixiangshouronglu.network.LanternNetwork;
import com.carrot123.yixiangshouronglu.shop.PlayerShopData;
import com.carrot123.yixiangshouronglu.gene.PlayerGenes;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import com.carrot123.yixiangshouronglu.registry.ModGeneRecipes;
import com.carrot123.yixiangshouronglu.gene.RegenerativeCells;
import com.carrot123.yixiangshouronglu.gene.AdditionalGenes;
import com.carrot123.yixiangshouronglu.gene.ExtendedGenes;
import com.carrot123.yixiangshouronglu.gene.ElementalGenes;
import com.carrot123.yixiangshouronglu.gene.DragonGenes;
import com.carrot123.yixiangshouronglu.gene.BossGenes;
import com.carrot123.yixiangshouronglu.gene.HighTierBossGenes;
import com.carrot123.yixiangshouronglu.gene.AscendantGenes;
import com.carrot123.yixiangshouronglu.gene.GeneSkillConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
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
        modBus.addListener((RegisterCapabilitiesEvent event) -> event.register(PlayerShopData.class));
        ShopNetwork.register();
        LanternNetwork.register();
        GeneNetwork.register();
        RegenerativeCells.register();
        AdditionalGenes.register();
        ExtendedGenes.register();
        ElementalGenes.register();
        DragonGenes.register();
        BossGenes.register();
        HighTierBossGenes.register();
        AscendantGenes.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, GeneSkillConfig.SPEC);
        ModFluids.FLUID_TYPES.register(modBus);
        ModFluids.FLUIDS.register(modBus);
        ModFeatures.FEATURES.register(modBus);
    }
}

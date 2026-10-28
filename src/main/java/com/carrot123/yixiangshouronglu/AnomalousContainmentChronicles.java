package com.carrot123.yixiangshouronglu;

import com.carrot123.yixiangshouronglu.combat.AllDamageEvents;
import com.carrot123.yixiangshouronglu.registry.ModAttributes;
import com.carrot123.yixiangshouronglu.registry.ModBlockEntities;
import com.carrot123.yixiangshouronglu.registry.ModBlocks;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import com.carrot123.yixiangshouronglu.registry.ModMenus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AnomalousContainmentChronicles.MOD_ID)
public final class AnomalousContainmentChronicles {
    public static final String MOD_ID = "yixiangshouronglu";

    public AnomalousContainmentChronicles(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModAttributes.ATTRIBUTES.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        modBus.addListener(ModAttributes::addPlayerAttribute);
        MinecraftForge.EVENT_BUS.register(AllDamageEvents.class);
    }
}

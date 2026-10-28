package com.carrot123.yixiangshouronglu;

import com.carrot123.yixiangshouronglu.combat.AllDamageEvents;
import com.carrot123.yixiangshouronglu.registry.ModAttributes;
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
        modBus.addListener(ModAttributes::addPlayerAttribute);
        MinecraftForge.EVENT_BUS.register(AllDamageEvents.class);
    }
}

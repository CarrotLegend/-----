package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.registry.ModBlockEntities;
import com.carrot123.yixiangshouronglu.registry.ModEntities;
import com.carrot123.yixiangshouronglu.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.DragonFireballRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
        modid = AnomalousContainmentChronicles.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ClientScreens {
    private ClientScreens() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GENE_DRAGON_FIREBALL.get(), DragonFireballRenderer::new);
        event.registerEntityRenderer(ModEntities.GENE_DRAGON_BREATH_CLOUD.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.GENE_STORM_LIGHTNING.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.GENE_DRAGON_BREATH_BEAM.get(), GeneDragonBreathRenderer::new);
        event.registerEntityRenderer(ModEntities.LEGENDARY_CARROT_KING.get(), LegendaryCarrotKingRenderer::new);
        event.registerBlockEntityRenderer(
                ModBlockEntities.CONTAINMENT_CHAMBER.get(),
                ContainmentChamberRenderer::new
        );

        event.registerEntityRenderer(
                ModEntities.TENACIOUS_SHIELD.get(),
                TenaciousShieldRenderer::new
        );

        event.registerEntityRenderer(
                ModEntities.YOUNG_ENDER_DRAGON.get(),
                YoungEnderDragonRenderer::new
        );

        event.registerEntityRenderer(
                ModEntities.SHADOW_DEMON.get(),
                ShadowDemonRenderer::new
        );
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.CARROT_KING_SHOP.get(), CarrotKingShopScreen::new);
            MenuScreens.register(ModMenus.GENE_SYNTHESIZER.get(), GeneSynthesizerScreen::new);
            MenuScreens.register(ModMenus.GENE_MODIFICATION_CHAMBER.get(), GeneModificationScreen::new);
            MenuScreens.register(
                    ModMenus.CONTAINMENT_CHAMBER.get(),
                    ContainmentChamberScreen::new
            );
        });
    }
}

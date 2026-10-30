package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.entity.YoungEnderDragonEntity;
import com.carrot123.yixiangshouronglu.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class YoungEnderDragonRenderer
        extends MobRenderer<YoungEnderDragonEntity, YoungEnderDragonModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/entity/enderdragon/dragon.png"
            );

    public YoungEnderDragonRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new YoungEnderDragonModel(
                        context.bakeLayer(YoungEnderDragonModel.LAYER)
                ),
                0.35F
        );
    }

    @Override
    protected void scale(
            YoungEnderDragonEntity entity,
            PoseStack pose,
            float partialTick
    ) {
        pose.scale(0.75F, 0.75F, 0.75F);
    }

    @Override
    public ResourceLocation getTextureLocation(YoungEnderDragonEntity entity) {
        return TEXTURE;
    }

    @Mod.EventBusSubscriber(
            modid = AnomalousContainmentChronicles.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT
    )
    public static final class Registration {
        @SubscribeEvent
        public static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(
                    ModEntities.YOUNG_ENDER_DRAGON.get(),
                    YoungEnderDragonRenderer::new
            );
        }

        @SubscribeEvent
        public static void registerLayers(
                EntityRenderersEvent.RegisterLayerDefinitions event
        ) {
            event.registerLayerDefinition(
                    YoungEnderDragonModel.LAYER,
                    YoungEnderDragonModel::createBodyLayer
            );
        }
    }
}

package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.AmethystCrabLureBlock;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class AmethystCrabLureItemModel extends GeoModel<AmethystCrabLureBlock> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            AnomalousContainmentChronicles.MOD_ID,
            "geo/amethyst_crab_lure.geo.json"
    );

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            AnomalousContainmentChronicles.MOD_ID,
            "textures/block/amethyst_crab_lure.png"
    );

    private static final ResourceLocation ANIMATION = new ResourceLocation(
            AnomalousContainmentChronicles.MOD_ID,
            "animations/amethyst_crab_lure.animation.json"
    );

    @Override
    public ResourceLocation getModelResource(AmethystCrabLureBlock animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AmethystCrabLureBlock animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(AmethystCrabLureBlock animatable) {
        return ANIMATION;
    }
}

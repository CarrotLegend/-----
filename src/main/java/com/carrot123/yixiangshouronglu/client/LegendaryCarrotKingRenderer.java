package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.entity.LegendaryCarrotKingEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class LegendaryCarrotKingRenderer extends HumanoidMobRenderer<LegendaryCarrotKingEntity, PlayerModel<LegendaryCarrotKingEntity>> {
    private static final ResourceLocation SKIN = new ResourceLocation("yixiangshouronglu", "textures/entity/legendary_carrot_king.png");
    public LegendaryCarrotKingRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
    }
    @Override public ResourceLocation getTextureLocation(LegendaryCarrotKingEntity entity) { return SKIN; }
}

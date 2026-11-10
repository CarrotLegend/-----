package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.entity.LegendaryCarrotKingEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class LegendaryCarrotKingRenderer extends HumanoidMobRenderer<LegendaryCarrotKingEntity, PlayerModel<LegendaryCarrotKingEntity>> {
    static final ResourceLocation SKIN = new ResourceLocation("yixiangshouronglu", "textures/entity/legendary_carrot_king.png");
    public LegendaryCarrotKingRenderer(EntityRendererProvider.Context context) {
        super(context, createModel(context.getModelSet()), 0.5F);
    }
    static PlayerModel<LegendaryCarrotKingEntity> createModel(EntityModelSet models) {
        return new PlayerModel<>(models.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }
    @Override public ResourceLocation getTextureLocation(LegendaryCarrotKingEntity entity) { return SKIN; }
}

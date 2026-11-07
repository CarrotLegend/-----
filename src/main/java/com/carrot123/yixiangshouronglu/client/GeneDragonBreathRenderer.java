package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.entity.GeneDragonBreathBeam;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public final class GeneDragonBreathRenderer extends EntityRenderer<GeneDragonBreathBeam> {
    public GeneDragonBreathRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public void render(GeneDragonBreathBeam beam, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        IafBreathVisuals.render(beam, partialTick, pose, buffers);
    }

    @Override
    public ResourceLocation getTextureLocation(GeneDragonBreathBeam beam) { return TextureAtlas.LOCATION_PARTICLES; }
}

package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.block.entity.AmethystCrabLureBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class AmethystCrabLureRenderer extends GeoBlockRenderer<AmethystCrabLureBlockEntity> {
    public AmethystCrabLureRenderer(BlockEntityRendererProvider.Context context) {
        super(new AmethystCrabLureModel());
    }

    @Override
    public void preRender(
            PoseStack poseStack,
            AmethystCrabLureBlockEntity animatable,
            BakedGeoModel model,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        if (!isReRender) {
            AmethystCrabLureParticles.ensure(animatable);
        }

        super.preRender(
                poseStack,
                animatable,
                model,
                bufferSource,
                buffer,
                isReRender,
                partialTick,
                packedLight,
                packedOverlay,
                red,
                green,
                blue,
                alpha
        );
    }

    @Override
    public boolean shouldRenderOffScreen(AmethystCrabLureBlockEntity blockEntity) {
        return true;
    }
}

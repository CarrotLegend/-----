package com.carrot123.yixiangshouronglu.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

public final class EternalSpringFluidClient {
    private static final ResourceLocation STILL = new ResourceLocation("minecraft", "block/water_still");
    private static final ResourceLocation FLOWING = new ResourceLocation("minecraft", "block/water_flow");
    private static final ResourceLocation OVERLAY = new ResourceLocation("minecraft", "block/water_overlay");
    private static final ResourceLocation UNDERWATER = new ResourceLocation("minecraft", "textures/misc/underwater.png");

    private EternalSpringFluidClient() {
    }

    public static IClientFluidTypeExtensions create() {
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return OVERLAY;
            }

            @Override
            public ResourceLocation getRenderOverlayTexture(Minecraft minecraft) {
                return UNDERWATER;
            }

            @Override
            public int getTintColor() {
                return 0xFF3F76E4;
            }

            @Override
            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                return 0xFF000000 | BiomeColors.getAverageWaterColor(getter, pos);
            }
        };
    }
}

package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.entity.YoungEnderDragonEntity;
import com.carrot123.yixiangshouronglu.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

public final class YoungEnderDragonRenderer extends EntityRenderer<YoungEnderDragonEntity> {
    private static final float DRAGON_SCALE = 0.0625F;

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/entity/enderdragon/dragon.png"
            );

    private final EnderDragonRenderer vanillaRenderer;
    private final Map<YoungEnderDragonEntity, EnderDragon> previews =
            new WeakHashMap<>();

    public YoungEnderDragonRenderer(EntityRendererProvider.Context context) {
        super(context);
        vanillaRenderer = new EnderDragonRenderer(context);
        shadowRadius = 0.4F;
    }

    @Override
    public void render(
            YoungEnderDragonEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight
    ) {
        EnderDragon dragon = previews.computeIfAbsent(
                entity,
                current -> new EnderDragon(
                        EntityType.ENDER_DRAGON,
                        current.level()
                )
        );

        float yaw = Mth.rotLerp(
                partialTick,
                entity.yRotO,
                entity.getYRot()
        );

        dragon.setPos(
                entity.getX(),
                entity.getY(),
                entity.getZ()
        );

        dragon.yRotO = yaw;
        dragon.setYRot(yaw);

        dragon.xRotO = entity.xRotO;
        dragon.setXRot(entity.getXRot());

        dragon.tickCount = entity.tickCount;

        float animationTime =
                (entity.tickCount + partialTick) * 0.06F;

        dragon.oFlapTime = animationTime - 0.06F;
        dragon.flapTime = animationTime;

        dragon.posPointer = 0;

        for (double[] position : dragon.positions) {
            position[0] = yaw;
            position[1] = entity.getY();
            position[2] = entity.getZ();
        }

        poseStack.pushPose();

        poseStack.translate(0.0D, 0.45D, 0.0D);

        poseStack.scale(
                DRAGON_SCALE,
                DRAGON_SCALE,
                DRAGON_SCALE
        );

        vanillaRenderer.render(
                dragon,
                yaw,
                partialTick,
                poseStack,
                buffers,
                packedLight
        );

        poseStack.popPose();

        super.render(
                entity,
                entityYaw,
                partialTick,
                poseStack,
                buffers,
                packedLight
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            YoungEnderDragonEntity entity
    ) {
        return TEXTURE;
    }

    @Mod.EventBusSubscriber(
            modid = AnomalousContainmentChronicles.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT
    )
    public static final class Registration {
        @SubscribeEvent
        public static void register(
                EntityRenderersEvent.RegisterRenderers event
        ) {
            event.registerEntityRenderer(
                    ModEntities.YOUNG_ENDER_DRAGON.get(),
                    YoungEnderDragonRenderer::new
            );
        }
    }
}

package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.entity.TenaciousShieldEntity;
import com.carrot123.yixiangshouronglu.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class TenaciousShieldRenderer extends EntityRenderer<TenaciousShieldEntity> {
    private static final ItemStack EYE = new ItemStack(Items.ENDER_EYE);
    private static final ItemStack SHIELD = new ItemStack(Items.SHIELD);
    private final ItemRenderer itemRenderer;

    public TenaciousShieldRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
        shadowRadius = 0.35F;
    }

    @Override
    public void render(TenaciousShieldEntity entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0D, 1.05D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));

        pose.pushPose();
        pose.scale(1.15F, 1.15F, 1.15F);
        itemRenderer.renderStatic(EYE, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();

        int shields = entity.getShields();
        float orbit = (entity.tickCount + partialTick) * 2.2F;
        for (int i = 0; i < shields; i++) {
            double angle = Math.toRadians(orbit + i * 90.0F);
            pose.pushPose();
            pose.translate(Math.cos(angle) * 0.9D, 0.1D + Math.sin((entity.tickCount + partialTick) * 0.12F + i) * 0.08D, Math.sin(angle) * 0.9D);
            pose.mulPose(Axis.YP.rotationDegrees(-orbit - i * 90.0F + 90.0F));
            pose.scale(0.68F, 0.68F, 0.68F);
            itemRenderer.renderStatic(SHIELD, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId() + i + 1);
            pose.popPose();
        }
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(TenaciousShieldEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.TENACIOUS_SHIELD.get(), TenaciousShieldRenderer::new);
        }
    }
}

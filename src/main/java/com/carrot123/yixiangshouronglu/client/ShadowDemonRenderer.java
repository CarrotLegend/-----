package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.entity.ShadowDemonEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class ShadowDemonRenderer extends MobRenderer<ShadowDemonEntity, ShadowDemonRenderer.ShadowPlayerModel> {
    private final ShadowPlayerModel wideModel;
    private final ShadowPlayerModel slimModel;
    private final Map<ShadowDemonEntity, SkinView> skins = new WeakHashMap<>();

    public ShadowDemonRenderer(EntityRendererProvider.Context context) {
        super(context, new ShadowPlayerModel(context.getModelSet().bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        wideModel = model;
        slimModel = new ShadowPlayerModel(context.getModelSet().bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(ShadowDemonEntity demon, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        SkinView view = skin(demon);
        model = view.slim ? slimModel : wideModel;
        super.render(demon, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowDemonEntity demon) {
        return skin(demon).texture;
    }

    private SkinView skin(ShadowDemonEntity demon) {
        UUID uuid = demon.getAppearanceUuid().orElse(null);
        String value = demon.getSkinValue();
        String signature = demon.getSkinSignature();
        SkinView old = skins.get(demon);
        if (old != null && old.matches(uuid, value, signature)) {
            return old;
        }

        ResourceLocation fallback = uuid == null ? DefaultPlayerSkin.getDefaultSkin()
                : DefaultPlayerSkin.getDefaultSkin(uuid);
        boolean fallbackSlim = value.isEmpty() && uuid != null
                ? "slim".equals(DefaultPlayerSkin.getSkinModelName(uuid)) : demon.isSlimModel();
        SkinView view = new SkinView(uuid, value, signature, fallback, fallbackSlim);
        skins.put(demon, view);
        if (uuid != null) {
            GameProfile profile = new GameProfile(uuid, demon.getAppearanceName().isEmpty()
                    ? "ShadowDemon" : demon.getAppearanceName());
            if (!value.isEmpty()) {
                profile.getProperties().put("textures", signature.isEmpty()
                        ? new Property("textures", value) : new Property("textures", value, signature));
            }
            Minecraft.getInstance().getSkinManager().registerSkins(profile, (type, location, texture) -> {
                if (type == MinecraftProfileTexture.Type.SKIN) {
                    view.texture = location;
                    view.slim = "slim".equals(texture.getMetadata("model"));
                }
            }, false);
        }
        return view;
    }

    private static final class SkinView {
        private final UUID uuid;
        private final String value;
        private final String signature;
        private ResourceLocation texture;
        private boolean slim;

        private SkinView(UUID uuid, String value, String signature, ResourceLocation texture, boolean slim) {
            this.uuid = uuid;
            this.value = value;
            this.signature = signature;
            this.texture = texture;
            this.slim = slim;
        }

        private boolean matches(UUID otherUuid, String otherValue, String otherSignature) {
            return java.util.Objects.equals(uuid, otherUuid) && value.equals(otherValue)
                    && signature.equals(otherSignature);
        }
    }

    public static final class ShadowPlayerModel extends PlayerModel<ShadowDemonEntity> {
        private ShadowPlayerModel(ModelPart root, boolean slim) {
            super(root, slim);
        }

        @Override
        public void setupAnim(ShadowDemonEntity demon, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
            super.setupAnim(demon, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            float lift = Math.min(1.0F, demon.getCastTicks() / 8.0F);
            switch (demon.getCastSkill()) {
                case 1 -> {
                    rightArm.xRot = -1.4F * lift;
                    leftArm.xRot = -1.0F * lift;
                    leftArm.yRot = 0.7F * lift;
                }
                case 2 -> {
                    rightArm.xRot = -1.8F * lift;
                    rightArm.zRot = 0.5F * lift;
                    leftArm.xRot = -0.5F * lift;
                }
                case 3 -> {
                    rightArm.xRot = -2.3F * lift;
                    rightArm.yRot = -0.5F * lift;
                    leftArm.xRot = -0.8F * lift;
                }
                default -> {
                }
            }
            rightSleeve.copyFrom(rightArm);
            leftSleeve.copyFrom(leftArm);
        }
    }
}

package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.block.ContainmentChamberBlock;
import com.carrot123.yixiangshouronglu.block.entity.ContainmentChamberBlockEntity;
import com.carrot123.yixiangshouronglu.registry.ModBlockEntities;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ContainmentChamberRenderer implements BlockEntityRenderer<ContainmentChamberBlockEntity> {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final ResourceLocation TEXTURE = new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "textures/block/containment_chamber.png");
    private static final ResourceLocation GEOMETRY = new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "models/block/containment_chamber_geometry.json");
    private final List<Cube> cubes = new ArrayList<>();
    private final java.util.Map<ContainmentChamberBlockEntity, Preview> previews = new java.util.WeakHashMap<>();

    private static final class Preview {
        private String entityId = "";
        private net.minecraft.world.entity.Entity entity;
    }

    public ContainmentChamberRenderer(BlockEntityRendererProvider.Context context) {
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(GEOMETRY);
            if (resource.isPresent()) {
                try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonArray elements = root.getAsJsonArray("elements");
                    for (int index = 0; index < elements.size() && index < 17; index++) {
                        cubes.add(new Cube(elements.get(index).getAsJsonObject()));
                    }
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load containment chamber geometry", exception);
        }
    }

    @Override
    public void render(ContainmentChamberBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (blockEntity.getBlockState().getValue(ContainmentChamberBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        Direction facing = blockEntity.getBlockState().getValue(ContainmentChamberBlock.FACING);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        for (Cube cube : cubes) {
            cube.render(poseStack, consumer, packedLight, packedOverlay);
        }
        poseStack.popPose();
        renderSoul(blockEntity, partialTick, poseStack, buffers, packedLight);
    }

    private void renderSoul(ContainmentChamberBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        String id = blockEntity.getEntityId();
        Preview preview = previews.computeIfAbsent(blockEntity, unused -> new Preview());
        if (id.isBlank()) {
            preview.entityId = "";
            preview.entity = null;
            return;
        }
        if (!id.equals(preview.entityId)) {
            preview.entityId = id;
            preview.entity = null;
            net.minecraft.resources.ResourceLocation entityId = net.minecraft.resources.ResourceLocation.tryParse(id);
            if (entityId != null && net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.containsKey(entityId)) {
                net.minecraft.world.entity.EntityType<?> type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(entityId);
                if (type != null) {
                    preview.entity = type.create(blockEntity.getLevel());
                }
            }
        }
        if (preview.entity == null) {
            return;
        }
        float width = Math.max(0.2F, preview.entity.getBbWidth());
        float height = Math.max(0.2F, preview.entity.getBbHeight());
        float scale = Math.min(2.5F, Math.min(0.82F / width, 0.86F / height));
        if (com.carrot123.yixiangshouronglu.resonance.ResonanceRules.isAntlersWendigo(id)) {
            scale = Math.min(scale, 0.14F);
        }
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.5D - height * scale * 0.5D, 0.5D);
        poseStack.scale(scale, scale, scale);
        try {
            Minecraft.getInstance().getEntityRenderDispatcher().render(preview.entity, 0.0D, 0.0D, 0.0D, 0.0F, partialTick, poseStack, buffers, packedLight);
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to display contained entity {}", id, exception);
            preview.entity = null;
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(ContainmentChamberBlockEntity blockEntity) {
        return true;
    }

    private static float[] array(JsonArray arr) {
        float[] values = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            values[i] = arr.get(i).getAsFloat() / 16.0F;
        }
        return values;
    }

    private static final class Cube {
        private final float[] from;
        private final float[] to;
        private final float[] origin;
        private final float xAngle;
        private final float yAngle;
        private final float zAngle;
        private final float[][] uv = new float[6][];

        Cube(JsonObject json) {
            from = array(json.getAsJsonArray("from"));
            to = array(json.getAsJsonArray("to"));
            JsonObject rotation = json.has("rotation") ? json.getAsJsonObject("rotation") : new JsonObject();
            origin = rotation.has("origin") ? array(rotation.getAsJsonArray("origin")) : new float[] {0, 0, 0};
            float xa = rotation.has("x") ? rotation.get("x").getAsFloat() : 0;
            float ya = rotation.has("y") ? rotation.get("y").getAsFloat() : 0;
            float za = rotation.has("z") ? rotation.get("z").getAsFloat() : 0;
            if (rotation.has("angle")) {
                float angle = rotation.get("angle").getAsFloat();
                String axis = GsonHelper.getAsString(rotation, "axis", "y");
                if (axis.equals("x")) xa += angle;
                if (axis.equals("y")) ya += angle;
                if (axis.equals("z")) za += angle;
            }
            xAngle = xa;
            yAngle = ya;
            zAngle = za;
            JsonObject faces = json.getAsJsonObject("faces");
            String[] names = {"north", "south", "west", "east", "up", "down"};
            for (int i = 0; i < names.length; i++) {
                if (faces.has(names[i])) {
                    uv[i] = array(faces.getAsJsonObject(names[i]).getAsJsonArray("uv"));
                }
            }
        }

        void render(PoseStack pose, VertexConsumer consumer, int light, int overlay) {
            pose.pushPose();
            pose.translate(origin[0], origin[1], origin[2]);
            pose.mulPose(Axis.ZP.rotationDegrees(zAngle));
            pose.mulPose(Axis.YP.rotationDegrees(yAngle));
            pose.mulPose(Axis.XP.rotationDegrees(xAngle));
            pose.translate(-origin[0], -origin[1], -origin[2]);
            float x0 = Math.min(from[0], to[0]);
            float x1 = Math.max(from[0], to[0]);
            float y0 = Math.min(from[1], to[1]);
            float y1 = Math.max(from[1], to[1]);
            float z0 = Math.min(from[2], to[2]);
            float z1 = Math.max(from[2], to[2]);
            draw(pose, consumer, light, overlay, 0, new float[][] {{x1,y0,z0},{x0,y0,z0},{x0,y1,z0},{x1,y1,z0}}, 0,0,-1);
            draw(pose, consumer, light, overlay, 1, new float[][] {{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}}, 0,0,1);
            draw(pose, consumer, light, overlay, 2, new float[][] {{x0,y0,z0},{x0,y0,z1},{x0,y1,z1},{x0,y1,z0}}, -1,0,0);
            draw(pose, consumer, light, overlay, 3, new float[][] {{x1,y0,z1},{x1,y0,z0},{x1,y1,z0},{x1,y1,z1}}, 1,0,0);
            draw(pose, consumer, light, overlay, 4, new float[][] {{x0,y1,z0},{x0,y1,z1},{x1,y1,z1},{x1,y1,z0}}, 0,1,0);
            draw(pose, consumer, light, overlay, 5, new float[][] {{x0,y0,z1},{x0,y0,z0},{x1,y0,z0},{x1,y0,z1}}, 0,-1,0);
            pose.popPose();
        }

        void draw(PoseStack pose, VertexConsumer consumer, int light, int overlay, int face, float[][] vertices, float nx, float ny, float nz) {
            if (uv[face] == null) {
                return;
            }
            PoseStack.Pose current = pose.last();
            float u0 = uv[face][0];
            float v0 = uv[face][1];
            float u1 = uv[face][2];
            float v1 = uv[face][3];
            float[][] tex = {{u0,v1},{u1,v1},{u1,v0},{u0,v0}};
            for (int i = 0; i < 4; i++) {
                float[] p = vertices[i];
                consumer.vertex(current.pose(), p[0], p[1], p[2])
                        .color(255, 255, 255, 255)
                        .uv(tex[i][0], tex[i][1])
                        .overlayCoords(overlay)
                        .uv2(light)
                        .normal(current.normal(), nx, ny, nz)
                        .endVertex();
            }
        }
    }

    @Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.CONTAINMENT_CHAMBER.get(), ContainmentChamberRenderer::new);
        }
    }
}

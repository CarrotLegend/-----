package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.entity.YoungEnderDragonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public final class YoungEnderDragonModel extends EntityModel<YoungEnderDragonEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(
                    AnomalousContainmentChronicles.MOD_ID,
                    "young_ender_dragon"
            ),
            "main"
    );

    private final ModelPart root;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart head;
    private final ModelPart tail;

    public YoungEnderDragonModel(ModelPart root) {
        this.root = root;
        leftWing = root.getChild("left_wing");
        rightWing = root.getChild("right_wing");
        head = root.getChild("head");
        tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -3.0F, -5.0F, 8.0F, 6.0F, 10.0F)
                        .texOffs(42, 0)
                        .addBox(-2.0F, -5.0F, 0.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 14.0F, 1.0F)
        );

        PartDefinition head = root.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 20)
                        .addBox(-3.0F, -3.0F, -4.0F, 6.0F, 5.0F, 6.0F)
                        .texOffs(32, 20)
                        .addBox(-2.0F, -1.0F, -7.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 12.0F, -5.0F)
        );

        head.addOrReplaceChild(
                "horn_left",
                CubeListBuilder.create()
                        .texOffs(0, 34)
                        .addBox(0.0F, -3.0F, -1.0F, 1.0F, 3.0F, 2.0F),
                PartPose.offset(1.8F, -3.0F, 0.0F)
        );

        head.addOrReplaceChild(
                "horn_right",
                CubeListBuilder.create()
                        .texOffs(0, 34)
                        .addBox(-1.0F, -3.0F, -1.0F, 1.0F, 3.0F, 2.0F),
                PartPose.offset(-1.8F, -3.0F, 0.0F)
        );

        root.addOrReplaceChild(
                "left_wing",
                CubeListBuilder.create()
                        .texOffs(0, 42)
                        .addBox(0.0F, -0.5F, -3.0F, 6.0F, 1.0F, 8.0F),
                PartPose.offset(3.5F, 12.5F, 0.0F)
        );

        root.addOrReplaceChild(
                "right_wing",
                CubeListBuilder.create()
                        .texOffs(0, 42)
                        .mirror()
                        .addBox(-6.0F, -0.5F, -3.0F, 6.0F, 1.0F, 8.0F)
                        .mirror(false),
                PartPose.offset(-3.5F, 12.5F, 0.0F)
        );

        root.addOrReplaceChild(
                "tail",
                CubeListBuilder.create()
                        .texOffs(0, 54)
                        .addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 8.0F),
                PartPose.offset(0.0F, 15.0F, 5.0F)
        );

        root.addOrReplaceChild(
                "leg_left",
                CubeListBuilder.create()
                        .texOffs(42, 34)
                        .addBox(-1.5F, 0.0F, -2.0F, 3.0F, 4.0F, 4.0F),
                PartPose.offset(2.5F, 17.0F, 3.0F)
        );

        root.addOrReplaceChild(
                "leg_right",
                CubeListBuilder.create()
                        .texOffs(42, 34)
                        .addBox(-1.5F, 0.0F, -2.0F, 3.0F, 4.0F, 4.0F),
                PartPose.offset(-2.5F, 17.0F, 3.0F)
        );

        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(
            YoungEnderDragonEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        float flap =
                (float) Math.sin(ageInTicks * 0.65F) * 0.38F;

        leftWing.zRot = flap;
        rightWing.zRot = -flap;

        head.yRot =
                netHeadYaw * ((float) Math.PI / 180.0F) * 0.35F;

        head.xRot =
                headPitch * ((float) Math.PI / 180.0F) * 0.35F;

        tail.yRot =
                (float) Math.sin(ageInTicks * 0.2F) * 0.20F;
    }

    @Override
    public void renderToBuffer(
            PoseStack pose,
            VertexConsumer consumer,
            int light,
            int overlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        root.render(
                pose,
                consumer,
                light,
                overlay,
                red,
                green,
                blue,
                alpha
        );
    }
}

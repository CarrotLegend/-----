package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.block.ContainmentChamberBlock;
import com.carrot123.yixiangshouronglu.block.entity.ContainmentChamberBlockEntity;
import com.carrot123.yixiangshouronglu.menu.ResonanceMenu;
import com.carrot123.yixiangshouronglu.registry.ModBlocks;
import com.carrot123.yixiangshouronglu.resonance.ResonanceRules;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class ResonanceScreen extends AbstractContainerScreen<ResonanceMenu> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CARD_WIDTH = 112;
    private static final int CARD_HEIGHT = 164;
    private static final int CARD_GAP = 12;
    private static final int VISIBLE_CARDS = 3;

    private static final BlockState LOWER = ModBlocks.CONTAINMENT_CHAMBER.get()
            .defaultBlockState()
            .setValue(ContainmentChamberBlock.HALF, DoubleBlockHalf.LOWER);

    private static final BlockState UPPER = LOWER
            .setValue(ContainmentChamberBlock.HALF, DoubleBlockHalf.UPPER);

    private static final ContainmentChamberBlockEntity PREVIEW =
            new ContainmentChamberBlockEntity(BlockPos.ZERO, LOWER);

    private final Map<String, Entity> previewEntities = new HashMap<>();
    private final Set<String> unavailableEntities = new HashSet<>();

    private ClientLevel previewLevel;
    private int offset = 0;

    public ResonanceScreen(ResonanceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 396;
        imageHeight = 210;
        inventoryLabelY = 10000;
        titleLabelY = 10;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xED10141F);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + 1, 0xFFBCA06C);
        graphics.fill(x + 1, y + imageHeight - 2, x + imageWidth - 1, y + imageHeight - 1, 0xFFBCA06C);

        int start = leftPos + 18;

        for (int index = 0; index < VISIBLE_CARDS; index++) {
            int cardIndex = index + offset;

            if (cardIndex >= menu.entries().size()) {
                continue;
            }

            int cardX = start + index * (CARD_WIDTH + CARD_GAP);

            renderCard(
                    graphics,
                    menu.entries().get(cardIndex),
                    cardX,
                    topPos + 30,
                    partialTick
            );
        }

        if (menu.entries().isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("screen.yixiangshouronglu.no_links"),
                    leftPos + imageWidth / 2,
                    topPos + 104,
                    0xD1C8BB
            );
        }

        graphics.drawString(
                font,
                Component.literal("◀"),
                leftPos + 7,
                topPos + 100,
                offset > 0 ? 0xFFF4D589 : 0xFF626772,
                false
        );

        graphics.drawString(
                font,
                Component.literal("▶"),
                leftPos + imageWidth - 16,
                topPos + 100,
                offset + VISIBLE_CARDS < menu.entries().size() ? 0xFFF4D589 : 0xFF626772,
                false
        );

        graphics.drawCenteredString(
                font,
                Component.translatable("screen.yixiangshouronglu.scroll"),
                leftPos + imageWidth / 2,
                topPos + 197,
                0xFF898C99
        );
    }

    private void renderCard(
            GuiGraphics graphics,
            ResonanceMenu.Entry entry,
            int x,
            int y,
            float partialTick
    ) {
        graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0xFF2B2634);
        graphics.fill(x + 2, y + 2, x + CARD_WIDTH - 2, y + CARD_HEIGHT - 2, 0xFF161D2B);
        graphics.fill(x + 4, y + 4, x + CARD_WIDTH - 4, y + 6, 0xFFBDA268);

        renderPreview(
                graphics,
                entry.entityId(),
                x + CARD_WIDTH / 2,
                y + 55,
                partialTick
        );

        Component name = entry.entityName();

        graphics.drawCenteredString(
                font,
                font.plainSubstrByWidth(name.getString(), CARD_WIDTH - 12),
                x + CARD_WIDTH / 2,
                y + 104,
                0xFFF3DCAF
        );

        var bonus = entry.entityId().isEmpty()
                ? null
                : ResonanceRules.fromEntity(entry.entityId());

        Component description = bonus == null
                ? Component.translatable("screen.yixiangshouronglu.no_bonus")
                : Component.translatable(bonus.translationKey());

        var wrapped = font.split(description, CARD_WIDTH - 12);

        for (int i = 0; i < Math.min(3, wrapped.size()); i++) {
            graphics.drawCenteredString(
                    font,
                    wrapped.get(i),
                    x + CARD_WIDTH / 2,
                    y + 120 + i * 11,
                    0xFFBBC2D0
            );
        }

        graphics.drawCenteredString(
                font,
                Component.literal(
                        entry.pos().getX() + "," +
                        entry.pos().getY() + "," +
                        entry.pos().getZ()
                ),
                x + CARD_WIDTH / 2,
                y + 153,
                0xFF81879A
        );
    }

    private void renderPreview(
            GuiGraphics graphics,
            String entityId,
            int centerX,
            int centerY,
            float partialTick
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        BlockEntityRenderer<ContainmentChamberBlockEntity> renderer =
                minecraft.getBlockEntityRenderDispatcher().getRenderer(PREVIEW);

        if (renderer == null) {
            return;
        }

        graphics.flush();

        PoseStack pose = graphics.pose();
        pose.pushPose();

        pose.translate(centerX, centerY, 200.0D);
        pose.scale(29.0F, -29.0F, 29.0F);

        pose.mulPose(Axis.XP.rotationDegrees(20.0F));
        pose.mulPose(Axis.YP.rotationDegrees(
                (System.currentTimeMillis() % 12000L) * 0.03F
        ));

        pose.translate(-0.5D, -1.0D, -0.5D);

        MultiBufferSource.BufferSource source =
                minecraft.renderBuffers().bufferSource();

        RenderSystem.enableDepthTest();

        renderer.render(
                PREVIEW,
                partialTick,
                pose,
                source,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY
        );

        pose.pushPose();
        pose.translate(0.0D, 1.0D, 0.0D);

        minecraft.getBlockRenderer().renderSingleBlock(
                UPPER,
                pose,
                source,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY
        );

        pose.popPose();

        renderContainedEntity(
                minecraft,
                entityId,
                pose,
                source
        );

        source.endBatch();

        pose.popPose();
        graphics.flush();
    }

    private void renderContainedEntity(
            Minecraft minecraft,
            String entityId,
            PoseStack pose,
            MultiBufferSource buffers
    ) {
        if (entityId == null || entityId.isBlank()) {
            return;
        }

        ClientLevel level = minecraft.level;

        if (level == null) {
            return;
        }

        if (previewLevel != level) {
            previewLevel = level;
            previewEntities.clear();
            unavailableEntities.clear();
        }

        if (unavailableEntities.contains(entityId)) {
            return;
        }

        Entity entity = previewEntities.get(entityId);

        if (entity == null) {
            ResourceLocation location = ResourceLocation.tryParse(entityId);

            if (location == null || !ForgeRegistries.ENTITY_TYPES.containsKey(location)) {
                unavailableEntities.add(entityId);
                return;
            }

            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(location);

            if (type == null) {
                unavailableEntities.add(entityId);
                return;
            }

            try {
                entity = type.create(level);
            } catch (RuntimeException exception) {
                LOGGER.warn("Failed to create resonance preview entity {}", entityId, exception);
                unavailableEntities.add(entityId);
                return;
            }

            if (entity == null) {
                unavailableEntities.add(entityId);
                return;
            }

            previewEntities.put(entityId, entity);
        }

        stabilizeEntity(entity);

        float width = Math.max(0.2F, entity.getBbWidth());
        float height = Math.max(0.2F, entity.getBbHeight());

        float scale = Math.min(
                2.5F,
                Math.min(0.82F / width, 0.86F / height)
        );
        if (ResonanceRules.isAntlersWendigo(entityId)) {
            scale = Math.min(scale, 0.14F);
        }

        pose.pushPose();

        pose.translate(
                0.5D,
                1.5D - height * scale * 0.5D,
                0.5D
        );

        pose.scale(scale, scale, scale);

        try {
            minecraft.getEntityRenderDispatcher().render(
                    entity,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0F,
                    0.0F,
                    pose,
                    buffers,
                    LightTexture.FULL_BRIGHT
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to render resonance preview entity {}", entityId, exception);
            previewEntities.remove(entityId);
            unavailableEntities.add(entityId);
        }

        pose.popPose();
    }

    private static void stabilizeEntity(Entity entity) {
        entity.tickCount = 0;
        entity.setYRot(0.0F);
        entity.yRotO = 0.0F;
        entity.setXRot(0.0F);
        entity.xRotO = 0.0F;

        if (entity instanceof LivingEntity living) {
            living.yBodyRot = 0.0F;
            living.yBodyRotO = 0.0F;
            living.yHeadRot = 0.0F;
            living.yHeadRotO = 0.0F;
        }
    }

    @Override
    protected void renderLabels(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        graphics.drawCenteredString(
                font,
                title,
                imageWidth / 2,
                12,
                0xFFEBD29B
        );
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (delta != 0) {
            scroll(delta < 0 ? 1 : -1);
            return true;
        }

        return super.mouseScrolled(x, y, delta);
    }

    @Override
    public boolean mouseDragged(
            double x,
            double y,
            int button,
            double dragX,
            double dragY
    ) {
        if (button == 0 && Math.abs(dragX) > 4) {
            scroll(dragX < 0 ? 1 : -1);
            return true;
        }

        return super.mouseDragged(x, y, button, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && y >= topPos + 80 && y <= topPos + 126) {
            if (x > leftPos && x < leftPos + 22) {
                scroll(-1);
                return true;
            }

            if (x > leftPos + imageWidth - 22 && x < leftPos + imageWidth) {
                scroll(1);
                return true;
            }
        }

        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
            scroll(key == GLFW.GLFW_KEY_LEFT ? -1 : 1);
            return true;
        }

        return super.keyPressed(key, scanCode, modifiers);
    }

    private void scroll(int amount) {
        offset = Math.max(
                0,
                Math.min(
                        offset + amount,
                        Math.max(0, menu.entries().size() - VISIBLE_CARDS)
                )
        );
    }

    @Override
    public void removed() {
        previewEntities.clear();
        unavailableEntities.clear();
        previewLevel = null;
        super.removed();
    }
}

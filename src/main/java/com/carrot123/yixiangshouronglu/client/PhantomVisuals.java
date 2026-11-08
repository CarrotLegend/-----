package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.network.GeneNetwork;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, value = Dist.CLIENT)
public final class PhantomVisuals {
    private static final List<Echo> ECHOES = new ArrayList<>();
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new BufferBuilder(256));
    private static final int GRAY_COLOR = 0x383C40;
    private static final int MAX_ALPHA = 120;
    private static final int MAX_LAYERS = 32;
    private static final IGuiOverlay OVERLAY = (gui, graphics, partialTick, width, height) -> {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.options.hideGui || client.player.isSpectator()
                || !active()) { return; }
        fog(graphics, width, height);
    };
    private static SnapshotRenderer normal;
    private static SnapshotRenderer slim;
    private static long session;
    private static long expires;
    private static boolean active;
    private static ResourceLocation dimension;

    private record Echo(SnapshotPlayer player, ClientLevel level, long started) {}

    private PhantomVisuals() {}

    public static void apply(GeneNetwork.PhantomStatus message) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null || !message.owner().equals(client.player.getUUID())
                || !message.dimension().equals(client.level.dimension().location()) || message.session() < session) { return; }
        long next = client.level.getGameTime() + message.remaining();
        expires = active && message.session() == session ? Math.min(expires, next) : next;
        active = message.remaining() > 0;
        session = message.session();
        dimension = message.dimension();
    }

    public static void echo(GeneNetwork.PhantomEcho message) {
        var client = Minecraft.getInstance();
        if (client.level == null || !client.level.dimension().location().equals(message.dimension())) { return; }
        if (ECHOES.size() >= 128) { ECHOES.remove(0); }
        ECHOES.add(new Echo(new SnapshotPlayer(client.level, message), client.level, client.level.getGameTime()));
    }

    private static boolean active() {
        var client = Minecraft.getInstance();
        return active && client.player != null && client.player.isAlive() && client.level != null
                && client.level.dimension().location().equals(dimension) && client.level.getGameTime() < expires;
    }

    private static void fog(GuiGraphics graphics, int width, int height) {
        if (width <= 0 || height <= 0) { return; }
        int horizontalThickness = Math.max(1, Math.min(width / 2, Math.max(10, width / 9)));
        int verticalThickness = Math.max(1, Math.min(height / 2, Math.max(10, height / 6)));
        int layers = Math.max(1, Math.min(MAX_LAYERS, Math.min(horizontalThickness, verticalThickness)));
        for (int i = 0; i < layers; i++) {
            float progress = (float) i / layers;
            float fade = 1.0F - progress;
            int alpha = Math.round(MAX_ALPHA * fade * fade);
            if (alpha <= 0) { continue; }
            int color = (alpha << 24) | GRAY_COLOR;
            int x0 = i * horizontalThickness / layers;
            int x1 = (i + 1) * horizontalThickness / layers;
            int y0 = i * verticalThickness / layers;
            int y1 = (i + 1) * verticalThickness / layers;
            graphics.fill(x0, 0, x1, height, color);
            graphics.fill(width - x1, 0, width - x0, height, color);
            graphics.fill(0, y0, width, y1, color);
            graphics.fill(0, height - y1, width, height - y0, color);
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) { return; }
        var client = Minecraft.getInstance();
        if (client.level == null) { clear(); return; }
        if (!active()) { active = false; }
        ECHOES.removeIf(echo -> echo.level() != client.level || client.level.getGameTime() >= echo.started() + 20);
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || normal == null || slim == null) { return; }
        var client = Minecraft.getInstance();
        if (client.level == null) { return; }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        for (Echo echo : List.copyOf(ECHOES)) {
            if (echo.level() != client.level || !client.level.hasChunkAt(echo.player().blockPosition())) { continue; }
            float age = client.level.getGameTime() - echo.started() + event.getPartialTick();
            if (age < 0 || age >= 20) { continue; }
            SnapshotPlayer player = echo.player();
            if (player.getEyePosition().distanceToSqr(camera) < 0.36) { continue; }
            SnapshotRenderer renderer = player.getModelName().equals("slim") ? slim : normal;
            float alpha = 0.55F * (1 - age / 20);
            Map<RenderType, RenderType> types = renderer.translucentTypes(player);
            MultiBufferSource tinted = type -> new Tint(BUFFERS.getBuffer(types.getOrDefault(type, type)), alpha);
            Vec3 offset = renderer.getRenderOffset(player, 0);
            pose.pushPose();
            try {
                pose.translate(player.getX() - camera.x + offset.x, player.getY() - camera.y + offset.y, player.getZ() - camera.z + offset.z);
                renderer.render(player, player.getYRot(), 0, pose, tinted, renderer.getPackedLightCoords(player, 0));
            } finally { pose.popPose(); }
        }
        BUFFERS.endBatch();
    }

    private static final class SnapshotPlayer extends RemotePlayer {
        private final ResourceLocation skin;
        private final ResourceLocation cape;
        private final ResourceLocation elytra;
        private final String model;
        private final int parts;

        SnapshotPlayer(ClientLevel level, GeneNetwork.PhantomEcho packet) {
            super(level, new GameProfile(packet.owner(), packet.name()));
            var original = level.getPlayerByUUID(packet.owner());
            var clientPlayer = original instanceof AbstractClientPlayer player ? player : null;
            skin = clientPlayer == null ? DefaultPlayerSkin.getDefaultSkin(packet.owner()) : clientPlayer.getSkinTextureLocation();
            model = clientPlayer == null ? DefaultPlayerSkin.getSkinModelName(packet.owner()) : clientPlayer.getModelName();
            cape = clientPlayer == null ? null : clientPlayer.getCloakTextureLocation();
            elytra = clientPlayer == null ? null : clientPlayer.getElytraTextureLocation();
            parts = packet.modelParts();
            setPos(packet.x(), packet.y(), packet.z());
            xo = packet.x(); yo = packet.y(); zo = packet.z();
            setYRot(packet.yaw()); yRotO = packet.yaw();
            setXRot(packet.pitch()); xRotO = packet.pitch();
            yBodyRot = yBodyRotO = packet.bodyYaw(); yHeadRot = yHeadRotO = packet.headYaw();
            attackAnim = oAttackAnim = packet.attack();
            setPose(packet.pose()); setShiftKeyDown(packet.pose() == Pose.CROUCHING); setMainArm(packet.arm());
            for (var slot : EquipmentSlot.values()) { setItemSlot(slot, packet.equipment().get(slot.ordinal()).copy()); }
        }

        @Override public ResourceLocation getSkinTextureLocation() { return skin; }
        @Override public String getModelName() { return model; }
        @Override public ResourceLocation getCloakTextureLocation() { return cape; }
        @Override public ResourceLocation getElytraTextureLocation() { return elytra; }
        @Override public boolean isModelPartShown(PlayerModelPart part) { return (parts & part.getMask()) != 0; }
    }

    private static final class SnapshotRenderer extends PlayerRenderer {
        SnapshotRenderer(EntityRendererProvider.Context context, boolean slim) { super(context, slim); }
        @Override protected boolean shouldShowName(AbstractClientPlayer player) { return false; }

        Map<RenderType, RenderType> translucentTypes(SnapshotPlayer player) {
            Map<RenderType, RenderType> result = new HashMap<>();
            texture(result, player.getSkinTextureLocation());
            if (player.getCloakTextureLocation() != null) { texture(result, player.getCloakTextureLocation()); }
            if (player.getElytraTextureLocation() != null) { texture(result, player.getElytraTextureLocation()); }
            texture(result, new ResourceLocation("minecraft", "textures/entity/elytra.png"));
            for (var layer : layers) {
                if (layer instanceof HumanoidArmorLayer<?, ?, ?> armor) {
                    for (var slot : EquipmentSlot.values()) {
                        var stack = player.getItemBySlot(slot);
                        if (stack.getItem() instanceof ArmorItem) {
                            texture(result, armor.getArmorResource(player, stack, slot, null));
                            texture(result, armor.getArmorResource(player, stack, slot, "overlay"));
                        }
                    }
                }
            }
            var items = RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS);
            result.put(Sheets.cutoutBlockSheet(), items);
            result.put(Sheets.translucentItemSheet(), items);
            return result;
        }

        private void texture(Map<RenderType, RenderType> map, ResourceLocation texture) {
            var target = RenderType.entityTranslucent(texture);
            map.put(RenderType.entitySolid(texture), target);
            map.put(RenderType.entityCutoutNoCull(texture), target);
            map.put(RenderType.armorCutoutNoCull(texture), target);
            map.put(RenderType.entityTranslucentCull(texture), target);
        }
    }

    private static final class Tint implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alpha;
        Tint(VertexConsumer delegate, float alpha) { this.delegate = delegate; this.alpha = alpha; }
        @Override public VertexConsumer vertex(double x, double y, double z) { delegate.vertex(x, y, z); return this; }
        @Override public VertexConsumer color(int red, int green, int blue, int alpha) {
            delegate.color((int) (red * 0.35), (int) (green * 0.35), (int) (blue * 0.35), (int) (alpha * this.alpha)); return this;
        }
        @Override public VertexConsumer uv(float u, float v) { delegate.uv(u, v); return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { delegate.overlayCoords(u, v); return this; }
        @Override public VertexConsumer uv2(int u, int v) { delegate.uv2(u, v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }
        @Override public void endVertex() { delegate.endVertex(); }
        @Override public void defaultColor(int red, int green, int blue, int alpha) {
            delegate.defaultColor((int) (red * 0.35), (int) (green * 0.35), (int) (blue * 0.35), (int) (alpha * this.alpha));
        }
        @Override public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
    }

    private static void clear() { ECHOES.clear(); active = false; expires = 0; session = 0; dimension = null; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) { if (event.getLevel().isClientSide()) { clear(); } }

    @Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void overlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("phantom_fog", OVERLAY);
        }
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers event) {
            normal = new SnapshotRenderer(event.getContext(), false);
            slim = new SnapshotRenderer(event.getContext(), true);
            ECHOES.clear();
        }
    }
}

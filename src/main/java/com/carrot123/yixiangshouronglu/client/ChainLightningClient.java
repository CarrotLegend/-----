package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, value = Dist.CLIENT)
public final class ChainLightningClient {
    private static final String PARTICLE_PACKAGE = "com.github.alexthe666.iceandfire.client.particle.";
    private static final int LIFESPAN = 4;
    private static ClientLevel renderLevel;
    private static Object renderer;
    private static long lastArcTick;
    private static boolean rendererFailed;

    private ChainLightningClient() {}

    public static void show(Vec3 start, Vec3 end) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        var api = OptionalIceAndFire.API;
        if (!rendererFailed && api != null) {
            try {
                if (renderer == null || renderLevel != level || level.getGameTime() > lastArcTick + LIFESPAN) {
                    renderer = api.renderer().newInstance();
                    renderLevel = level;
                }
                Object bolt = api.bolt().newInstance(api.electricity(), start, end, 15);
                api.size().invoke(bolt, 0.05F);
                api.lifespan().invoke(bolt, LIFESPAN);
                api.spawn().invoke(bolt, api.noDelay());
                api.update().invoke(renderer, new Object(), bolt, Minecraft.getInstance().getFrameTime());
                lastArcTick = level.getGameTime();
                return;
            } catch (ReflectiveOperationException | LinkageError exception) {
                disableRenderer(exception);
            }
        }
        int steps = Math.max(1, Math.min(32, (int) (start.distanceTo(end) * 3)));
        for (int step = 1; step <= steps; step++) {
            var point = start.lerp(end, (double) step / steps);
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 0, 0, 0);
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || renderer == null) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        if (minecraft.level != renderLevel || renderLevel.getGameTime() > lastArcTick + LIFESPAN) {
            renderer = null;
            renderLevel = null;
            return;
        }
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        var buffers = minecraft.renderBuffers().bufferSource();
        pose.pushPose();
        try {
            pose.translate(-camera.x, -camera.y, -camera.z);
            OptionalIceAndFire.API.render().invoke(renderer, event.getPartialTick(), pose, buffers);
        } catch (ReflectiveOperationException | LinkageError exception) {
            disableRenderer(exception);
        } finally {
            pose.popPose();
            buffers.endBatch(RenderType.lightning());
        }
    }

    private static void disableRenderer(Throwable exception) {
        rendererFailed = true;
        renderer = null;
        renderLevel = null;
        LogUtils.getLogger().warn("Could not render Ice and Fire chain lightning", exception);
    }

    private static final class OptionalIceAndFire {
        private static final Api API = load();

        private static Api load() {
            if (!ModList.get().isLoaded("iceandfire")) {
                return null;
            }
            try {
                var bolt = Class.forName(PARTICLE_PACKAGE + "LightningBoltData");
                var info = Class.forName(PARTICLE_PACKAGE + "LightningBoltData$BoltRenderInfo");
                var spawn = Class.forName(PARTICLE_PACKAGE + "LightningBoltData$SpawnFunction");
                var render = Class.forName(PARTICLE_PACKAGE + "LightningRender");
                return new Api(bolt.getConstructor(info, Vec3.class, Vec3.class, int.class),
                        render.getConstructor(), info.getField("ELECTRICITY").get(null),
                        spawn.getField("NO_DELAY").get(null), bolt.getMethod("size", float.class),
                        bolt.getMethod("lifespan", int.class), bolt.getMethod("spawn", spawn),
                        render.getMethod("update", Object.class, bolt, float.class),
                        render.getMethod("render", float.class, PoseStack.class, MultiBufferSource.class));
            } catch (ReflectiveOperationException | LinkageError exception) {
                LogUtils.getLogger().warn("Ice and Fire lightning renderer is unavailable", exception);
                return null;
            }
        }
    }

    private record Api(Constructor<?> bolt, Constructor<?> renderer, Object electricity, Object noDelay,
                       Method size, Method lifespan, Method spawn, Method update, Method render) {}
}

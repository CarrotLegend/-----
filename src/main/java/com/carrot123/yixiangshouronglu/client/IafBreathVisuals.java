package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.entity.GeneDragonBreathBeam;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, value = Dist.CLIENT)
public final class IafBreathVisuals {
    private static final Map<UUID, View> VIEWS = new HashMap<>();
    private static boolean attempted;
    private static Api api;

    private static final class Api {
        final Constructor<?> flame;
        final Constructor<?> frost;
        final Constructor<?> lightning;
        final Constructor<?> bolt;
        final Method size;
        final Method lifespan;
        final Method spawn;
        final Method update;
        final Method render;
        final Object electricity;
        final Object noDelay;

        Api() throws ReflectiveOperationException {
            String prefix = "com.github.alexthe666.iceandfire.client.particle.";
            Class<?>[] particleArguments = {ClientLevel.class, double.class, double.class, double.class,
                    double.class, double.class, double.class, float.class};
            flame = Class.forName(prefix + "ParticleDragonFlame").getConstructor(particleArguments);
            frost = Class.forName(prefix + "ParticleDragonFrost").getConstructor(particleArguments);
            Class<?> renderClass = Class.forName(prefix + "LightningRender");
            Class<?> boltClass = Class.forName(prefix + "LightningBoltData");
            Class<?> infoClass = Class.forName(prefix + "LightningBoltData$BoltRenderInfo");
            Class<?> spawnClass = Class.forName(prefix + "LightningBoltData$SpawnFunction");
            lightning = renderClass.getConstructor();
            bolt = boltClass.getConstructor(infoClass, Vec3.class, Vec3.class, int.class);
            electricity = infoClass.getField("ELECTRICITY").get(null);
            noDelay = spawnClass.getField("NO_DELAY").get(null);
            size = boltClass.getMethod("size", float.class);
            lifespan = boltClass.getMethod("lifespan", int.class);
            spawn = boltClass.getMethod("spawn", spawnClass);
            update = renderClass.getMethod("update", Object.class, boltClass, float.class);
            render = renderClass.getMethod("render", float.class, PoseStack.class, MultiBufferSource.class);
        }
    }

    private static final class View {
        final GeneDragonBreathBeam beam;
        final List<Particle> particles = new ArrayList<>();
        final Object[] lightning = new Object[4];
        int[] chain = new int[0];

        View(GeneDragonBreathBeam beam) { this.beam = beam; }

        void clear() {
            particles.forEach(Particle::remove);
            particles.clear();
            java.util.Arrays.fill(lightning, null);
        }
    }

    private IafBreathVisuals() {}

    private static Api api() {
        if (!attempted) {
            attempted = true;
            if (ModList.get().isLoaded("iceandfire")) {
                try { api = new Api(); }
                catch (ReflectiveOperationException | LinkageError exception) { failed(exception); }
            }
        }
        return api;
    }

    private static void failed(Throwable exception) {
        LogUtils.getLogger().error("Unable to use Ice and Fire dragon breath visuals", exception);
        api = null;
        clear();
    }

    private static View view(GeneDragonBreathBeam beam) {
        return VIEWS.computeIfAbsent(beam.getUUID(), ignored -> new View(beam));
    }

    private static Vec3 start(GeneDragonBreathBeam beam, float partialTick) {
        var owner = beam.owner();
        return owner == null ? beam.getPosition(partialTick) : owner.getEyePosition(partialTick);
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide && event.getEntity() instanceof GeneDragonBreathBeam beam && api() != null) { view(beam); }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        var client = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || client.isPaused()) { return; }
        if (client.level == null) { clear(); return; }
        Api bridge = api();
        if (bridge == null) { return; }
        var iterator = VIEWS.values().iterator();
        try {
            while (iterator.hasNext()) {
                View view = iterator.next();
                var beam = view.beam;
                if (beam.isRemoved() || beam.level() != client.level || beam.owner() != null && !beam.owner().isAlive()) {
                    view.clear(); iterator.remove(); continue;
                }
                view.particles.removeIf(particle -> !particle.isAlive());
                if (beam.kind() == GeneDragonBreathBeam.Kind.LIGHTNING || beam.tickCount % 2 != 0) { continue; }
                Vec3 start = start(beam, 1);
                Vec3 direction = beam.direction();
                Vec3 velocity = direction.normalize().scale(0.12);
                Constructor<?> factory = beam.kind() == GeneDragonBreathBeam.Kind.FIRE ? bridge.flame : bridge.frost;
                for (double distance = 0; distance < direction.length(); distance += 0.75) {
                    Vec3 point = start.add(direction.normalize().scale(distance));
                    Particle particle = (Particle) factory.newInstance(client.level, point.x, point.y, point.z,
                            velocity.x, velocity.y, velocity.z, 0.5F);
                    client.particleEngine.add(particle);
                    view.particles.add(particle);
                }
            }
        } catch (ReflectiveOperationException | LinkageError exception) { failed(exception); }
    }

    public static void render(GeneDragonBreathBeam beam, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        if (beam.kind() != GeneDragonBreathBeam.Kind.LIGHTNING) { return; }
        Api bridge = api();
        if (bridge == null) { return; }
        View view = view(beam);
        Vec3 start = start(beam, partialTick);
        Vec3 position = beam.getPosition(partialTick);
        pose.pushPose();
        try {
            pose.translate(-position.x, -position.y, -position.z);
            renderBolt(bridge, view, 0, start, start.add(beam.direction()), partialTick, pose, buffers);
            int[] chain = beam.chain();
            if (!java.util.Arrays.equals(view.chain, chain)) {
                for (int index = 1; index < view.lightning.length; index++) { view.lightning[index] = null; }
                view.chain = chain;
            }
            for (int index = 1; index < chain.length && index < view.lightning.length; index++) {
                var from = beam.level().getEntity(chain[index - 1]);
                var to = beam.level().getEntity(chain[index]);
                if (from instanceof LivingEntity && to instanceof LivingEntity) {
                    renderBolt(bridge, view, index, from.getPosition(partialTick).add(0, from.getBbHeight() / 2, 0),
                            to.getPosition(partialTick).add(0, to.getBbHeight() / 2, 0), partialTick, pose, buffers);
                }
            }
        } catch (ReflectiveOperationException | LinkageError exception) { failed(exception); }
        finally { pose.popPose(); }
    }

    private static void renderBolt(Api bridge, View view, int index, Vec3 start, Vec3 end, float partialTick,
                                   PoseStack pose, MultiBufferSource buffers) throws ReflectiveOperationException {
        if (view.lightning[index] == null) { view.lightning[index] = bridge.lightning.newInstance(); }
        Object bolt = bridge.bolt.newInstance(bridge.electricity, start, end, 15);
        bridge.size.invoke(bolt, 0.05F);
        bridge.lifespan.invoke(bolt, 4);
        bridge.spawn.invoke(bolt, bridge.noDelay);
        bridge.update.invoke(view.lightning[index], view.beam.getUUID(), bolt, partialTick);
        bridge.render.invoke(view.lightning[index], partialTick, pose, buffers);
    }

    private static void clear() { VIEWS.values().forEach(View::clear); VIEWS.clear(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) { if (event.getLevel().isClientSide()) { clear(); } }
}

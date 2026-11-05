package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.gene.BlazingDomain;
import com.carrot123.yixiangshouronglu.network.GeneNetwork;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, value = Dist.CLIENT)
public final class BlazingDomainVisuals {
    private static final Map<UUID, Visual> ACTIVE = new HashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();
    private static final class Visual {
        final GeneNetwork.DomainState state;
        final List<Particle> particles = new ArrayList<>();
        int remaining;
        int missing;
        Visual(GeneNetwork.DomainState state) { this.state = state; remaining = state.remaining(); }
        void clear() { particles.forEach(Particle::remove); particles.clear(); }
    }
    private BlazingDomainVisuals() {}
    public static void apply(GeneNetwork.DomainState state) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(state.dimension())) { return; }
        Visual old = ACTIVE.get(state.owner());
        if (old != null && old.state.session() > state.session()) { return; }
        if (old != null) { old.clear(); ACTIVE.remove(state.owner()); }
        if (state.remaining() > 0) { ACTIVE.put(state.owner(), new Visual(state)); }
    }
    public static <T extends ParticleOptions> T heat(ParticleType<T> type, int entity, float radius, float angle, float speed, int y) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeUtf("mutantmore:heat");
            for (float value : new float[]{radius, angle, speed, entity, 1, 0, y, 0, 0, 0}) { buffer.writeFloat(value); }
            return type.getDeserializer().fromNetwork(type, buffer);
        } finally { buffer.release(); }
    }
    private static void emit(Visual visual, Entity owner, ParticleType<?> type) {
        for (int pass = 0; pass < 4; pass++) {
            for (int y = -(int) BlazingDomain.RANGE_Y; y < BlazingDomain.RANGE_Y + owner.getBbHeight(); y++) {
                if ((y == 0 || RANDOM.nextInt(1 + Math.abs(y) / 2) == 0) && RANDOM.nextBoolean()) {
                    var options = heat(type, owner.getId(), (float) BlazingDomain.RANGE, RANDOM.nextFloat() * (float) (2 * Math.PI), 0.001F + RANDOM.nextFloat() * 0.05F, y);
                    Particle particle = Minecraft.getInstance().particleEngine.createParticle(options, owner.blockPosition().getX(), owner.blockPosition().getY() + y, owner.blockPosition().getZ(), 0, 0, 0);
                    if (particle != null) { visual.particles.add(particle); }
                }
            }
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var client = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || client.isPaused()) { return; }
        if (client.level == null) { clear(); return; }
        var type = ForgeRegistries.PARTICLE_TYPES.getValue(BlazingDomain.HEAT_PARTICLE);
        var iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            Visual visual = iterator.next();
            Entity owner = client.level.getEntity(visual.state.entity());
            if (type == null || --visual.remaining < 0 || !client.level.dimension().location().equals(visual.state.dimension())
                    || owner != null && (!owner.getUUID().equals(visual.state.owner()) || !owner.isAlive() || owner.isRemoved())) {
                visual.clear(); iterator.remove(); continue;
            }
            if (owner == null) {
                visual.clear();
                if (++visual.missing > 20) { iterator.remove(); }
                continue;
            }
            visual.missing = 0;
            visual.particles.removeIf(particle -> !particle.isAlive());
            emit(visual, owner, type);
        }
    }
    private static void clear() { ACTIVE.values().forEach(Visual::clear); ACTIVE.clear(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) { if (event.getLevel().isClientSide()) { clear(); } }
}

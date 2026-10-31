package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.block.entity.AmethystCrabLureBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class AmethystCrabLureParticles {
    private static final ResourceLocation CORE = new ResourceLocation(
            AnomalousContainmentChronicles.MOD_ID,
            "amethyst_crab_pedestal_core"
    );

    private static final ResourceLocation AURA = new ResourceLocation(
            AnomalousContainmentChronicles.MOD_ID,
            "amethyst_crab_pedestal_aura"
    );

    private static final Map<AmethystCrabLureBlockEntity, ParticlePair> ACTIVE = new HashMap<>();

    private record ParticlePair(ParticleEmitter core, ParticleEmitter aura) {
    }

    private AmethystCrabLureParticles() {
    }

    public static void ensure(AmethystCrabLureBlockEntity blockEntity) {
        if (!(blockEntity.getLevel() instanceof ClientLevel level) || blockEntity.isRemoved()) {
            return;
        }

        MolangParticleEngine engine = MolangParticleEngine.INSTANCE;

        if (!engine.id2Emitter().containsKey(CORE) || !engine.id2Emitter().containsKey(AURA)) {
            return;
        }

        ParticlePair existing = ACTIVE.get(blockEntity);

        if (existing != null
                && engine.getEmitter(existing.core().id) == existing.core()
                && engine.getEmitter(existing.aura().id) == existing.aura()) {
            return;
        }

        if (existing != null) {
            remove(existing);
        }

        double x = blockEntity.getBlockPos().getX() + 0.5D;
        double y = blockEntity.getBlockPos().getY() + 0.8D;
        double z = blockEntity.getBlockPos().getZ() + 0.5D;

        ParticleEmitter core = new ParticleEmitter(level, new Vec3(x, y, z), CORE);
        ParticleEmitter aura = new ParticleEmitter(level, new Vec3(x, y, z), AURA);

        core.hideOutline = true;
        aura.hideOutline = true;

        engine.addEmitter(core);
        engine.addEmitter(aura);

        ACTIVE.put(blockEntity, new ParticlePair(core, aura));
    }

    public static void clientTick() {
        ClientLevel level = Minecraft.getInstance().level;

        Iterator<Map.Entry<AmethystCrabLureBlockEntity, ParticlePair>> iterator =
                ACTIVE.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<AmethystCrabLureBlockEntity, ParticlePair> entry = iterator.next();
            AmethystCrabLureBlockEntity blockEntity = entry.getKey();

            if (level == null
                    || blockEntity.isRemoved()
                    || blockEntity.getLevel() != level
                    || !level.hasChunkAt(blockEntity.getBlockPos())
                    || level.getBlockEntity(blockEntity.getBlockPos()) != blockEntity) {
                remove(entry.getValue());
                iterator.remove();
            }
        }
    }

    private static void remove(ParticlePair pair) {
        MolangParticleEngine engine = MolangParticleEngine.INSTANCE;

        removeEmitter(engine, pair.core());
        removeEmitter(engine, pair.aura());
    }

    private static void removeEmitter(
            MolangParticleEngine engine,
            ParticleEmitter emitter
    ) {
        if (engine.getEmitter(emitter.id) == emitter) {
            engine.removeEmitter(emitter.id, false);
        }
    }
}

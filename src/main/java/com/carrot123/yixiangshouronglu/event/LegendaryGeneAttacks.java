package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.gene.BiologicalGene;
import com.carrot123.yixiangshouronglu.gene.PlayerGeneModules;
import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

final class LegendaryGeneAttacks {
    private static final ResourceLocation ANCHOR_SOUND = new ResourceLocation("legendary_monsters", "anchor_slam");
    private static final int HURT_IMMUNITY_THRESHOLD = 10;
    private static final DustParticleOptions SUNLIGHT = new DustParticleOptions(new Vector3f(1.0F, 0.78F, 0.15F), 1.15F);
    private static final Map<ServerPlayer, List<Hit>> PENDING = Collections.synchronizedMap(new WeakHashMap<>());

    private LegendaryGeneAttacks() {}

    static void onHit(ServerPlayer player, LivingEntity target, PlayerGeneModules modules, long now, boolean melee) {
        if (modules.count(BiologicalGene.SKY_STORM_CORE) > 0 && modules.skyHit(target.getUUID(), now)) {
            schedule(player, target, Strike.SKY, now, BiologicalGene.SKY_BOLT_DAMAGE);
        }
        if (modules.count(BiologicalGene.ANNIHILATION_REACTOR_CORE) > 0 && modules.startAnnihilationCooldown(now)) {
            long delay = delayUntilHurtWindowEnds(target);
            schedule(player, target, Strike.ANNIHILATION, now + delay,
                    target.getHealth() * BiologicalGene.ANNIHILATION_HEALTH_PERCENT / 100.0F);
        }
        if (melee && modules.count(BiologicalGene.ANCHOR_MUSCLE_FIBERS) > 0
                && player.getRandom().nextInt(100) < BiologicalGene.ANCHOR_CHANCE_PERCENT) {
            schedule(player, target, Strike.ANCHOR, now, BiologicalGene.ANCHOR_DAMAGE);
        }
    }

    private static void schedule(ServerPlayer player, LivingEntity target, Strike strike, long dueAt, float damage) {
        schedule(player, target, strike, dueAt, damage, null);
    }

    static void onMowzieHit(ServerPlayer player, LivingEntity target, PlayerGeneModules modules,
                            long now, boolean melee, LivingDamageEvent mainHit) {
        if (!player.isAlive()) {
            return;
        }
        boolean cold = melee && modules.count(BiologicalGene.EXTREME_COLD_BREATH) > 0
                && player.getRandom().nextInt(100) < BiologicalGene.EXTREME_COLD_CHANCE_PERCENT;
        boolean solar = modules.count(BiologicalGene.SOLAR_RETINA) > 0
                && player.getRandom().nextInt(100) < BiologicalGene.SOLAR_BEAM_CHANCE_PERCENT;
        if (!cold && !solar) {
            return;
        }
        long dueAt = now + delayUntilHurtWindowEnds(target);
        if (cold) {
            schedule(player, target, Strike.EXTREME_COLD, dueAt, 0, mainHit);
        }
        if (solar) {
            schedule(player, target, Strike.SOLAR, dueAt + (cold ? HURT_IMMUNITY_THRESHOLD + 1 : 0),
                    BiologicalGene.SOLAR_BEAM_DAMAGE, mainHit);
        }
    }

    private static int delayUntilHurtWindowEnds(LivingEntity target) {
        return Math.max(1, target.invulnerableTime - HURT_IMMUNITY_THRESHOLD + 1);
    }

    private static void schedule(ServerPlayer player, LivingEntity target, Strike strike, long dueAt,
                                 float damage, LivingDamageEvent mainHit) {
        PENDING.computeIfAbsent(player, ignored -> new ArrayList<>())
                .add(new Hit(target.getUUID(), strike, dueAt, damage, mainHit));
    }

    static void discard(ServerPlayer player) {
        PENDING.remove(player);
    }

    static void discardUnequipped(ServerPlayer player, PlayerGeneModules modules) {
        var hits = PENDING.get(player);
        if (hits == null) {
            return;
        }
        hits.removeIf(hit -> modules.count(hit.strike().gene) == 0);
        if (hits.isEmpty()) {
            discard(player);
        }
    }

    static void tick(ServerPlayer player, PlayerGeneModules modules, long now) {
        List<Hit> hits = PENDING.get(player);
        if (hits == null) {
            return;
        }
        if (!player.isAlive()) {
            discard(player);
            return;
        }
        var ready = hits.stream().filter(hit -> hit.dueAt() <= now).toList();
        hits.removeIf(hit -> hit.dueAt() <= now);
        if (hits.isEmpty()) {
            discard(player);
        }
        for (Hit hit : ready) {
            if (modules.count(hit.strike().gene) > 0
                    && player.serverLevel().getEntity(hit.target()) instanceof LivingEntity target && target.isAlive()) {
                execute(player, target, hit);
            }
        }
    }

    private static void execute(ServerPlayer player, LivingEntity target, Hit hit) {
        float damage = hit.damage();
        var mainHit = hit.mainHit();
        if (mainHit != null) {
            float amount = mainHit.getAmount();
            if (mainHit.isCanceled() || !(amount > 0) || !Float.isFinite(amount)) {
                return;
            }
            if (hit.strike() == Strike.EXTREME_COLD) {
                damage = amount * (BiologicalGene.EXTREME_COLD_DAMAGE_PERCENT / 100.0F);
            }
        }
        var level = player.serverLevel();
        if (hit.strike() == Strike.SKY) {
            var lightning = EntityType.LIGHTNING_BOLT.create(level);
            if (lightning != null) {
                lightning.setVisualOnly(true);
                lightning.moveTo(target.getX(), target.getBoundingBox().maxY, target.getZ());
                level.getServer().getPlayerList().broadcast(null, lightning.getX(), lightning.getY(), lightning.getZ(),
                        512, level.dimension(), lightning.getAddEntityPacket());
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BiologicalGene.SKY_SLOW_TICKS), player);
        } else if (hit.strike() == Strike.ANCHOR) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BiologicalGene.ANCHOR_SLOW_TICKS, 1), player);
            var sound = ForgeRegistries.SOUND_EVENTS.getValue(ANCHOR_SOUND);
            if (sound != null) {
                level.playSound(null, target.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
            } else {
                LogUtils.getLogger().warn("Missing Legendary Monsters sound {}", ANCHOR_SOUND);
            }
        } else if (hit.strike() == Strike.EXTREME_COLD) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(), 12, target.getBbWidth() * 0.4, target.getBbHeight() * 0.3,
                    target.getBbWidth() * 0.4, 0.015);
        } else if (hit.strike() == Strike.SOLAR) {
            var origin = player.getEyePosition();
            var end = target.position().add(0, target.getBbHeight() * 0.5, 0);
            int steps = Math.min(48, Math.max(1, (int) Math.ceil(origin.distanceTo(end) * 4)));
            for (int step = 0; step <= steps; step++) {
                var point = origin.lerp(end, (double) step / steps);
                level.sendParticles(SUNLIGHT, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            }
        }
        var damageType = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(hit.strike().damage);
        target.hurt(new DamageSource(damageType, player), damage);
    }

    private enum Strike {
        SKY(BiologicalGene.SKY_STORM_CORE, "sky_storm_strike"),
        ANNIHILATION(BiologicalGene.ANNIHILATION_REACTOR_CORE, "annihilation_strike"),
        ANCHOR(BiologicalGene.ANCHOR_MUSCLE_FIBERS, "anchor_strike"),
        EXTREME_COLD(BiologicalGene.EXTREME_COLD_BREATH, "extreme_cold_breath"),
        SOLAR(BiologicalGene.SOLAR_RETINA, "solar_beam");

        private final BiologicalGene gene;
        private final ResourceKey<DamageType> damage;

        Strike(BiologicalGene gene, String damage) {
            this.gene = gene;
            this.damage = ResourceKey.create(Registries.DAMAGE_TYPE,
                    new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, damage));
        }
    }

    private record Hit(UUID target, Strike strike, long dueAt, float damage, LivingDamageEvent mainHit) {}
}

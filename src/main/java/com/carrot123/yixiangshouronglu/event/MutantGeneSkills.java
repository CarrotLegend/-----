package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.gene.PlayerGeneModules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class MutantGeneSkills {
    private static final String BOW_ARROW = "yixiangshouronglu:bow_arrow";

    private record BowRelease(long tick, ItemStack bow) {}

    private static final Map<ServerPlayer, BowRelease> RELEASES = new WeakHashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void bowReleased(ArrowLooseEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getBow().getItem() instanceof BowItem
                && event.getCharge() >= 0 && event.hasAmmo()) {
            RELEASES.put(player, new BowRelease(player.level().getGameTime(), event.getBow()));
        }
    }

    @SubscribeEvent
    public static void arrowLaunched(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && !event.loadedFromDisk()
                && event.getEntity() instanceof AbstractArrow arrow && !(arrow instanceof ThrownTrident)
                && !arrow.shotFromCrossbow() && arrow.getOwner() instanceof ServerPlayer shooter
                && shooter.getUseItem().getItem() instanceof BowItem) {
            var release = RELEASES.get(shooter);
            if (release != null && release.tick() == shooter.level().getGameTime() && release.bow() == shooter.getUseItem()) {
                arrow.getPersistentData().putBoolean(BOW_ARROW, true);
            }
        }
    }

    private static final Map<ServerPlayer, MobEffectInstance> REGENERATION = new WeakHashMap<>();
    private static final ThreadLocal<LivingEntity> BURST_TARGET = new ThreadLocal<>();

    static void regeneration(ServerPlayer player, boolean equipped) {
        var current = player.getEffect(MobEffects.REGENERATION);
        if (!equipped || !player.isAlive()) {
            var supplied = REGENERATION.remove(player);
            if (supplied != null && supplied == current) {
                player.removeEffect(MobEffects.REGENERATION);
            }
        } else if (current == null) {
            var supplied = new MobEffectInstance(MobEffects.REGENERATION, 50, 0, true, false);
            if (player.addEffect(supplied)) {
                REGENERATION.put(player, supplied);
            }
        }
    }

    @SubscribeEvent
    public static void externalRegeneration(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getEffectInstance().getEffect() == MobEffects.REGENERATION) {
            REGENERATION.remove(player);
        }
    }

    static void burstDamage(LivingEntity target, DamageSource source, float damage) {
        var previous = BURST_TARGET.get();
        BURST_TARGET.set(target);
        try {
            target.hurt(source, damage);
        } finally {
            if (previous == null) {
                BURST_TARGET.remove();
            } else {
                BURST_TARGET.set(previous);
            }
        }
    }

    @SubscribeEvent
    public static void burstKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() == BURST_TARGET.get()) {
            event.setCanceled(true);
        }
    }

    static boolean bowArrow(Entity direct) {
        return direct instanceof AbstractArrow arrow && !arrow.shotFromCrossbow()
                && arrow.getPersistentData().getBoolean(BOW_ARROW);
    }

    static boolean attackable(ServerPlayer player, LivingEntity target) {
        return target != player && target.isAlive() && !target.isRemoved() && !target.isSpectator()
                && target.level() == player.level() && player.canAttack(target) && !player.isAlliedTo(target)
                && (!(target instanceof OwnableEntity pet) || !player.getUUID().equals(pet.getOwnerUUID()))
                && (!(target instanceof Player other) || player.canHarmPlayer(other) && !other.getAbilities().invulnerable);
    }

    static boolean enemy(ServerPlayer player, LivingEntity target) {
        return attackable(player, target) && (target instanceof Enemy || target instanceof Player
                || target instanceof Mob mob && mob.getTarget() == player || player.getLastHurtByMob() == target);
    }

    static void leap(ServerPlayer player, PlayerGeneModules modules, long now) {
        var level = player.serverLevel();
        var eye = player.getEyePosition();
        var end = player.pick(16, 1, false).getLocation();
        var hit = ProjectileUtil.getEntityHitResult(player, eye, end,
                player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1),
                entity -> entity instanceof LivingEntity living && attackable(player, living) && living.isPickable(),
                eye.distanceToSqr(end));
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target) || player.distanceToSqr(target) > 256) {
            return;
        }
        Vec3 behind = Vec3.directionFromRotation(0, target.yBodyRot).reverse();
        for (double offset : new double[]{1.75, 2, 1.5}) {
            var base = target.position().add(behind.scale(offset));
            for (int dy : new int[]{0, 1, -1}) {
                var pos = BlockPos.containing(base.x, Math.floor(base.y) + dy, base.z);
                var dest = new Vec3(base.x, pos.getY(), base.z);
                var box = player.getBoundingBox().move(dest.subtract(player.position()));
                if (!level.hasChunksAt(BlockPos.containing(box.minX, box.minY - 1, box.minZ),
                        BlockPos.containing(box.maxX, box.maxY, box.maxZ)) || !level.getWorldBorder().isWithinBounds(box)
                        || level.isOutsideBuildHeight(pos) || !level.noCollision(player, box)
                        || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                        || BlockPos.betweenClosedStream(box.inflate(0, 0.01, 0)).anyMatch(block -> {
                            var state = level.getBlockState(block);
                            return !state.getFluidState().isEmpty() || state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK)
                                    || state.is(Blocks.CACTUS) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                                    || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.SWEET_BERRY_BUSH);
                        })) {
                    continue;
                }
                player.stopRiding();
                player.connection.teleport(dest.x, dest.y, dest.z, target.yBodyRot, player.getXRot());
                player.fallDistance = 0;
                modules.empowerLeap(now);
                level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1, 1);
                level.sendParticles(ParticleTypes.PORTAL, dest.x, dest.y + 1, dest.z, 16, 0.3, 0.5, 0.3, 0.1);
                return;
            }
        }
    }
}

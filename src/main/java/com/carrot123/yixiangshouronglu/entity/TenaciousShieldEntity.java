package com.carrot123.yixiangshouronglu.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class TenaciousShieldEntity extends Monster {
    private static final EntityDataAccessor<Integer> SHIELDS =
            SynchedEntityData.defineId(TenaciousShieldEntity.class, EntityDataSerializers.INT);

    private static final int SHIELD_BREAK_COOLDOWN = 40;
    private static final int FIREBALL_INTERVAL = 80;
    private static final float CONTACT_DAMAGE = 5.0F;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.yixiangshouronglu.tenacious_shield"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_20
    );

    private int shieldBreakCooldownTicks;
    private int fireballCooldownTicks = FIREBALL_INTERVAL;

    public TenaciousShieldEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 60;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 160.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SHIELDS, 4);
    }

    public int getShields() {
        return entityData.get(SHIELDS);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1D, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (getShields() > 0) {
            if (level().isClientSide) {
                return false;
            }

            if (!(source.getEntity() instanceof Player attacker)
                    || source.getDirectEntity() != attacker
                    || !(attacker.getMainHandItem().getItem() instanceof AxeItem)) {
                return false;
            }

            if (shieldBreakCooldownTicks > 0) {
                return false;
            }

            int remaining = getShields() - 1;

            entityData.set(SHIELDS, remaining);
            shieldBreakCooldownTicks = SHIELD_BREAK_COOLDOWN;

            level().playSound(
                    null,
                    blockPosition(),
                    SoundEvents.SHIELD_BREAK,
                    SoundSource.HOSTILE,
                    1.0F,
                    0.85F + random.nextFloat() * 0.3F
            );

            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        getX(),
                        getY() + 1.2D,
                        getZ(),
                        24,
                        0.7D,
                        0.5D,
                        0.7D,
                        0.08D
                );
            }

            updateBossBar();
            return true;
        }

        return super.hurt(source, amount);
    }

    private void updateBossBar() {
        if (getShields() > 0) {
            bossEvent.setProgress(getShields() / 4.0F);
        } else {
            bossEvent.setProgress(Math.max(0.0F, getHealth() / getMaxHealth()));
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (level().isClientSide) {
            return;
        }

        if (shieldBreakCooldownTicks > 0) {
            shieldBreakCooldownTicks--;
        }

        if (fireballCooldownTicks > 0) {
            fireballCooldownTicks--;
        }

        if (tickCount % 20 == 0) {
            dealContactDamage();
        }

        if (fireballCooldownTicks <= 0) {
            LivingEntity target = getTarget();

            if (target != null
                    && target.isAlive()
                    && distanceToSqr(target) <= 1024.0D
                    && getSensing().hasLineOfSight(target)) {
                shootFireball(target);
                fireballCooldownTicks = FIREBALL_INTERVAL;
            }
        }

        updateBossBar();
    }

    private void dealContactDamage() {
        for (Player player : level().getEntitiesOfClass(
                Player.class,
                getBoundingBox().inflate(0.25D),
                player -> player.isAlive()
                        && !player.isSpectator()
                        && !player.isCreative()
        )) {
            player.hurt(damageSources().mobAttack(this), CONTACT_DAMAGE);
        }
    }

    private void shootFireball(LivingEntity target) {
        double startY = getY(0.55D);

        Vec3 direction = new Vec3(
                target.getX() - getX(),
                target.getY(0.5D) - startY,
                target.getZ() - getZ()
        ).normalize();

        if (direction.lengthSqr() < 0.0001D) {
            return;
        }

        LargeFireball fireball = new LargeFireball(
                level(),
                this,
                direction.x,
                direction.y,
                direction.z,
                1
        );

        fireball.setPos(
                getX() + direction.x * 1.25D,
                startY + direction.y * 1.25D,
                getZ() + direction.z * 1.25D
        );

        level().addFreshEntity(fireball);

        level().playSound(
                null,
                getX(),
                startY,
                getZ(),
                SoundEvents.GHAST_SHOOT,
                SoundSource.HOSTILE,
                1.0F,
                0.9F + random.nextFloat() * 0.2F
        );

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.FLAME,
                    fireball.getX(),
                    fireball.getY(),
                    fireball.getZ(),
                    12,
                    0.2D,
                    0.2D,
                    0.2D,
                    0.04D
            );
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RemainingShields", getShields());
        tag.putInt("ShieldBreakCooldown", shieldBreakCooldownTicks);
        tag.putInt("FireballCooldown", fireballCooldownTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("RemainingShields", 3)) {
            entityData.set(
                    SHIELDS,
                    Math.max(0, Math.min(4, tag.getInt("RemainingShields")))
            );
        }

        shieldBreakCooldownTicks = tag.contains("ShieldBreakCooldown", 3)
                ? Math.max(0, Math.min(SHIELD_BREAK_COOLDOWN, tag.getInt("ShieldBreakCooldown")))
                : 0;

        fireballCooldownTicks = tag.contains("FireballCooldown", 3)
                ? Math.max(0, Math.min(FIREBALL_INTERVAL, tag.getInt("FireballCooldown")))
                : FIREBALL_INTERVAL;

        bossEvent.setName(getDisplayName());
        updateBossBar();
    }
}

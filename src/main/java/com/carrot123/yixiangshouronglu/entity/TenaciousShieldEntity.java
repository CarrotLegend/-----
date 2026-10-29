package com.carrot123.yixiangshouronglu.entity;

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
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.item.AxeItem;

public final class TenaciousShieldEntity extends Monster {
    private static final EntityDataAccessor<Integer> SHIELDS = SynchedEntityData.defineId(TenaciousShieldEntity.class, EntityDataSerializers.INT);
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.yixiangshouronglu.tenacious_shield"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_20);

    public TenaciousShieldEntity(EntityType<? extends Monster> type, net.minecraft.world.level.Level level) {
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
            int remaining = getShields() - 1;
            entityData.set(SHIELDS, remaining);
            level().playSound(null, blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 1.0F, 0.85F + random.nextFloat() * 0.3F);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, getX(), getY() + 1.2D, getZ(), 24, 0.7D, 0.5D, 0.7D, 0.08D);
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
        if (!level().isClientSide) {
            updateBossBar();
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
    public void setCustomName(net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RemainingShields", getShields());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("RemainingShields", 3)) {
            entityData.set(SHIELDS, Math.max(0, Math.min(4, tag.getInt("RemainingShields"))));
        }
        bossEvent.setName(getDisplayName());
        updateBossBar();
    }
}

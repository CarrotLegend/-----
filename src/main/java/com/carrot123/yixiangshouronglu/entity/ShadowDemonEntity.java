package com.carrot123.yixiangshouronglu.entity;

import com.carrot123.yixiangshouronglu.entity.appearance.ShadowDemonAppearance;
import com.github.L_Ender.cataclysm.config.CMConfig;
import com.github.L_Ender.cataclysm.entity.projectile.Ignis_Fireball_Entity;
import com.github.L_Ender.cataclysm.entity.projectile.Lightning_Spear_Entity;
import com.github.L_Ender.cataclysm.entity.projectile.Phantom_Arrow_Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ShadowDemonEntity extends Monster {
    private static final double MAX_HEALTH = 3100.0D;
    private static final double ATTACK_DAMAGE = 6.0D;
    private static final double MOVEMENT_SPEED = 0.30D;
    private static final double FOLLOW_RANGE = 32.0D;
    private static final int CAST_DURATION = 16;

    private static final EntityDataAccessor<Integer> CAST_SKILL =
            SynchedEntityData.defineId(ShadowDemonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CAST_TICKS =
            SynchedEntityData.defineId(ShadowDemonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> APPEARANCE_UUID =
            SynchedEntityData.defineId(ShadowDemonEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> APPEARANCE_NAME =
            SynchedEntityData.defineId(ShadowDemonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_VALUE =
            SynchedEntityData.defineId(ShadowDemonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_SIGNATURE =
            SynchedEntityData.defineId(ShadowDemonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SLIM_MODEL =
            SynchedEntityData.defineId(ShadowDemonEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.yixiangshouronglu.shadow_demon"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_20
    );

    private int meleeSwingCount;
    private int nextSkill;
    private boolean skillPending;
    private UUID castTarget;
    private boolean appearanceResolved;
    private boolean appearanceRequested;

    public ShadowDemonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 60;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CAST_SKILL, 0);
        entityData.define(CAST_TICKS, 0);
        entityData.define(APPEARANCE_UUID, Optional.empty());
        entityData.define(APPEARANCE_NAME, "");
        entityData.define(SKIN_VALUE, "");
        entityData.define(SKIN_SIGNATURE, "");
        entityData.define(SLIM_MODEL, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CastingGoal());
        goalSelector.addGoal(2, new ShadowMeleeGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public int getCastSkill() {
        return entityData.get(CAST_SKILL);
    }

    public int getCastTicks() {
        return entityData.get(CAST_TICKS);
    }

    public Optional<UUID> getAppearanceUuid() {
        return entityData.get(APPEARANCE_UUID);
    }

    public String getAppearanceName() {
        return entityData.get(APPEARANCE_NAME);
    }

    public String getSkinValue() {
        return entityData.get(SKIN_VALUE);
    }

    public String getSkinSignature() {
        return entityData.get(SKIN_SIGNATURE);
    }

    public boolean isSlimModel() {
        return entityData.get(SLIM_MODEL);
    }

    public void setAppearance(UUID uuid, String name, String skinValue, String skinSignature, boolean slim) {
        if (level().isClientSide || appearanceResolved) {
            return;
        }

        entityData.set(APPEARANCE_UUID, Optional.ofNullable(uuid));
        entityData.set(APPEARANCE_NAME, name);
        entityData.set(SKIN_VALUE, skinValue);
        entityData.set(SKIN_SIGNATURE, skinSignature);
        entityData.set(SLIM_MODEL, slim);
        appearanceResolved = true;
        appearanceRequested = false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!appearanceResolved && !appearanceRequested) {
            appearanceRequested = true;
            ShadowDemonAppearance.resolve(serverLevel, this);
        }

        if (getCastSkill() == 0 && skillPending) {
            LivingEntity target = getTarget();
            if (isValidTarget(target)) {
                beginCast(target);
            }
        }

        if (getCastSkill() != 0) {
            tickCast(serverLevel);
        }

        bossEvent.setProgress(Math.max(0.0F, getHealth() / getMaxHealth()));
    }

    private boolean isValidTarget(LivingEntity target) {
        return target != null && target.isAlive() && target.level() == level();
    }

    private void onMeleeSwing() {
        meleeSwingCount++;
        if (meleeSwingCount >= 3) {
            meleeSwingCount = 0;
            skillPending = true;
        }
    }

    private void beginCast(LivingEntity target) {
        castTarget = target.getUUID();
        skillPending = false;
        getNavigation().stop();
        entityData.set(CAST_TICKS, 0);
        entityData.set(CAST_SKILL, nextSkill + 1);
    }

    private void tickCast(ServerLevel serverLevel) {
        Entity entity = castTarget == null ? null : serverLevel.getEntity(castTarget);
        if (!(entity instanceof LivingEntity target) || !isValidTarget(target)) {
            cancelCast();
            return;
        }

        getNavigation().stop();
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        int ticks = entityData.get(CAST_TICKS) + 1;
        entityData.set(CAST_TICKS, ticks);
        if (ticks < CAST_DURATION) {
            return;
        }

        if (releaseSkill(serverLevel, target)) {
            nextSkill = (nextSkill + 1) % 3;
            castTarget = null;
            entityData.set(CAST_SKILL, 0);
            entityData.set(CAST_TICKS, 0);
        } else {
            cancelCast();
        }
    }

    private void cancelCast() {
        castTarget = null;
        skillPending = true;
        entityData.set(CAST_SKILL, 0);
        entityData.set(CAST_TICKS, 0);
    }

    private boolean releaseSkill(ServerLevel serverLevel, LivingEntity target) {
        Vec3 origin = new Vec3(getX(), getEyeY() - 0.2D, getZ());
        Vec3 direction = target.getEyePosition().subtract(origin).normalize();
        if (direction.lengthSqr() < 0.0001D) {
            return false;
        }

        return switch (getCastSkill()) {
            case 1 -> shootArrows(serverLevel, target, origin, direction);
            case 2 -> shootFireballs(serverLevel, origin, direction);
            case 3 -> shootSpear(serverLevel, origin, direction);
            default -> false;
        };
    }

    private boolean shootArrows(ServerLevel serverLevel, LivingEntity target, Vec3 origin, Vec3 direction) {
        List<Entity> projectiles = new ArrayList<>();
        for (int index = -1; index <= 1; index++) {
            Vec3 shot = direction.yRot((float) Math.toRadians(index * 8.0D));
            Phantom_Arrow_Entity arrow = new Phantom_Arrow_Entity(serverLevel, this, target);
            arrow.setPos(origin.add(shot.scale(0.8D)));
            arrow.setBaseDamage(CMConfig.MaledictusPhantomArrowbasedamage);
            arrow.shoot(shot.x, shot.y, shot.z, 1.8F, 1.0F);
            projectiles.add(arrow);
        }
        return addProjectiles(serverLevel, projectiles);
    }

    private boolean shootFireballs(ServerLevel serverLevel, Vec3 origin, Vec3 direction) {
        List<Entity> projectiles = new ArrayList<>();
        for (int index = -1; index <= 1; index++) {
            Vec3 shot = direction.yRot((float) Math.toRadians(index * 12.0D));
            Ignis_Fireball_Entity fireball = new Ignis_Fireball_Entity(serverLevel, this);
            fireball.setPos(origin.add(shot.scale(0.9D)));
            fireball.shoot(shot.x, shot.y, shot.z, 0.25F, 0.0F);
            fireball.setUp(0);
            fireball.setFired(true);
            projectiles.add(fireball);
        }
        return addProjectiles(serverLevel, projectiles);
    }

    private boolean shootSpear(ServerLevel serverLevel, Vec3 origin, Vec3 direction) {
        Lightning_Spear_Entity spear = new Lightning_Spear_Entity(
                this, direction, serverLevel, (float) CMConfig.ScyllaSpearDamage
        );
        spear.accelerationPower = 0.2D;
        spear.setPos(origin.add(direction.scale(0.9D)));
        spear.setAreaDamage((float) CMConfig.ScyllaLightningAreaDamage);
        spear.setHpDamage((float) CMConfig.ScyllaLightningStormHpDamage);
        spear.setAreaRadius(2.0F);
        return serverLevel.addFreshEntity(spear);
    }

    private boolean addProjectiles(ServerLevel serverLevel, List<Entity> projectiles) {
        List<Entity> added = new ArrayList<>();
        for (Entity projectile : projectiles) {
            if (!serverLevel.addFreshEntity(projectile)) {
                added.forEach(Entity::discard);
                return false;
            }
            added.add(projectile);
        }
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity direct = source.getDirectEntity();
        if (source.getEntity() == this && (direct instanceof Phantom_Arrow_Entity
                || direct instanceof Ignis_Fireball_Entity
                || direct instanceof Lightning_Spear_Entity)) {
            return false;
        }
        return super.hurt(source, amount);
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
    public void remove(RemovalReason reason) {
        bossEvent.removeAllPlayers();
        castTarget = null;
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ShadowMeleeSwings", meleeSwingCount);
        tag.putInt("ShadowNextSkill", nextSkill);
        tag.putBoolean("ShadowSkillPending", skillPending);
        tag.putInt("ShadowCastSkill", getCastSkill());
        tag.putInt("ShadowCastTicks", getCastTicks());
        if (castTarget != null) {
            tag.putUUID("ShadowCastTarget", castTarget);
        }
        tag.putBoolean("ShadowAppearanceResolved", appearanceResolved);
        getAppearanceUuid().ifPresent(uuid -> tag.putUUID("ShadowAppearanceUuid", uuid));
        tag.putString("ShadowAppearanceName", getAppearanceName());
        tag.putString("ShadowSkinValue", getSkinValue());
        tag.putString("ShadowSkinSignature", getSkinSignature());
        tag.putBoolean("ShadowSlimModel", isSlimModel());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        meleeSwingCount = Math.max(0, Math.min(2, tag.getInt("ShadowMeleeSwings")));
        nextSkill = Math.max(0, Math.min(2, tag.getInt("ShadowNextSkill")));
        skillPending = tag.getBoolean("ShadowSkillPending");
        int castSkill = Math.max(0, Math.min(3, tag.getInt("ShadowCastSkill")));
        castTarget = tag.hasUUID("ShadowCastTarget") ? tag.getUUID("ShadowCastTarget") : null;
        if (castSkill != 0 && castTarget == null) {
            castSkill = 0;
            skillPending = true;
        }
        entityData.set(CAST_SKILL, castSkill);
        entityData.set(CAST_TICKS, Math.max(0, Math.min(CAST_DURATION, tag.getInt("ShadowCastTicks"))));
        appearanceResolved = tag.getBoolean("ShadowAppearanceResolved");
        appearanceRequested = false;
        entityData.set(APPEARANCE_UUID, tag.hasUUID("ShadowAppearanceUuid")
                ? Optional.of(tag.getUUID("ShadowAppearanceUuid")) : Optional.empty());
        entityData.set(APPEARANCE_NAME, tag.getString("ShadowAppearanceName"));
        entityData.set(SKIN_VALUE, tag.getString("ShadowSkinValue"));
        entityData.set(SKIN_SIGNATURE, tag.getString("ShadowSkinSignature"));
        entityData.set(SLIM_MODEL, tag.getBoolean("ShadowSlimModel"));
        bossEvent.setName(getDisplayName());
        bossEvent.setProgress(Math.max(0.0F, getHealth() / getMaxHealth()));
    }

    private final class ShadowMeleeGoal extends MeleeAttackGoal {
        private ShadowMeleeGoal() {
            super(ShadowDemonEntity.this, 1.1D, false);
        }

        @Override
        public boolean canUse() {
            return getCastSkill() == 0 && !skillPending && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return getCastSkill() == 0 && !skillPending && super.canContinueToUse();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target, double distanceSqr) {
            if (getCastSkill() != 0 || skillPending) {
                return;
            }
            boolean swinging = distanceSqr <= getAttackReachSqr(target) && isTimeToAttack();
            super.checkAndPerformAttack(target, distanceSqr);
            if (swinging) {
                onMeleeSwing();
            }
        }
    }

    private final class CastingGoal extends Goal {
        private CastingGoal() {
            setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getCastSkill() != 0;
        }

        @Override
        public boolean canContinueToUse() {
            return getCastSkill() != 0;
        }

        @Override
        public void tick() {
            getNavigation().stop();
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        }
    }
}

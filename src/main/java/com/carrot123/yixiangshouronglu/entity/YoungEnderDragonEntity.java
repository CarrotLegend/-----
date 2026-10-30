package com.carrot123.yixiangshouronglu.entity;

import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Locale;

public final class YoungEnderDragonEntity extends TamableAnimal {
    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(
                    YoungEnderDragonEntity.class,
                    EntityDataSerializers.INT
            );

    public enum Mode {
        ROAM,
        FOLLOW,
        STAY
    }

    public YoungEnderDragonEntity(
            EntityType<? extends TamableAnimal> type,
            Level level
    ) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 12, true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FLYING_SPEED, 0.36D)
                .add(Attributes.FOLLOW_RANGE, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(MODE, Mode.FOLLOW.ordinal());
    }

    public Mode getMode() {
        int value = entityData.get(MODE);

        return Mode.values()[
                Math.max(0, Math.min(Mode.values().length - 1, value))
        ];
    }

    public void setMode(Mode mode) {
        entityData.set(MODE, mode.ordinal());
        setOrderedToSit(mode == Mode.STAY);
        getNavigation().stop();
        setTarget(null);

        if (mode == Mode.STAY) {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));

        goalSelector.addGoal(2, new FollowOwnerGoal(
                this,
                1.1D,
                4.0F,
                2.0F,
                true
        ) {
            @Override
            public boolean canUse() {
                return getMode() == Mode.FOLLOW && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return getMode() == Mode.FOLLOW && super.canContinueToUse();
            }
        });

        goalSelector.addGoal(3, new FireballGoal());
        goalSelector.addGoal(5, new RandomFlightGoal());
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this,
                Monster.class,
                true
        ) {
            @Override
            public boolean canUse() {
                return getMode() != Mode.STAY && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return getMode() != Mode.STAY && super.canContinueToUse();
            }
        });
    }

    @Override
    public void aiStep() {
        super.aiStep();
        setNoGravity(true);

        if (getMode() == Mode.STAY) {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(
            ServerLevel level,
            AgeableMob mate
    ) {
        return null;
    }

    @Override
    public InteractionResult mobInteract(
            Player player,
            InteractionHand hand
    ) {
        ItemStack held = player.getItemInHand(hand);

        if (isTame() && held.is(Items.NETHER_STAR)) {
            if (getOwnerUUID() == null
                    || !getOwnerUUID().equals(player.getUUID())) {
                return InteractionResult.FAIL;
            }

            if (!level().isClientSide) {
                ItemStack soul = SoulItem.create(
                        ModItems.SOUL.get(),
                        ForgeRegistries.ENTITY_TYPES.getKey(getType())
                );

                if (!player.getInventory().add(soul)) {
                    player.drop(soul, false);
                }

                discard();
            }

            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (isTame()
                && held.isEmpty()
                && getOwnerUUID() != null
                && getOwnerUUID().equals(player.getUUID())) {
            if (!level().isClientSide) {
                Mode next = switch (getMode()) {
                    case ROAM -> Mode.FOLLOW;
                    case FOLLOW -> Mode.STAY;
                    case STAY -> Mode.ROAM;
                };

                setMode(next);

                player.displayClientMessage(
                        Component.translatable(
                                "message.yixiangshouronglu.young_ender_dragon."
                                        + next.name().toLowerCase(Locale.ROOT)
                        ),
                        true
                );
            }

            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DragonMode", getMode().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        int savedMode = tag.contains("DragonMode", 3)
                ? tag.getInt("DragonMode")
                : Mode.FOLLOW.ordinal();

        setMode(
                Mode.values()[
                        Math.max(
                                0,
                                Math.min(Mode.values().length - 1, savedMode)
                        )
                ]
        );
    }

    private final class RandomFlightGoal extends Goal {
        @Override
        public boolean canUse() {
            return getMode() == Mode.ROAM
                    && getNavigation().isDone()
                    && getRandom().nextInt(28) == 0;
        }

        @Override
        public void start() {
            double x = getX()
                    + (getRandom().nextDouble() - 0.5D) * 12.0D;

            double y = Math.max(
                    level().getMinBuildHeight() + 1,
                    Math.min(
                            level().getMaxBuildHeight() - 2,
                            getY()
                                    + (getRandom().nextDouble() - 0.5D) * 6.0D
                    )
            );

            double z = getZ()
                    + (getRandom().nextDouble() - 0.5D) * 12.0D;

            getNavigation().moveTo(x, y, z, 0.9D);
        }
    }

    private final class FireballGoal extends Goal {
        private int cooldown;

        private FireballGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getMode() != Mode.STAY
                    && getTarget() instanceof Monster target
                    && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            if (!(getTarget() instanceof Monster target)) {
                return;
            }

            getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (cooldown > 0) {
                cooldown--;
            }

            if (cooldown > 0
                    || distanceToSqr(target) > 256.0D
                    || !getSensing().hasLineOfSight(target)) {
                return;
            }

            Vec3 direction = new Vec3(
                    target.getX() - getX(),
                    target.getEyeY() - getEyeY(),
                    target.getZ() - getZ()
            ).normalize();

            if (direction.lengthSqr() <= 0.0001D) {
                return;
            }

            SmallFireball fireball = new SmallFireball(
                    level(),
                    YoungEnderDragonEntity.this,
                    direction.x,
                    direction.y,
                    direction.z
            );

            fireball.setPos(
                    getX() + direction.x * 0.6D,
                    getEyeY() + direction.y * 0.6D,
                    getZ() + direction.z * 0.6D
            );

            level().addFreshEntity(fireball);
            cooldown = 60;
        }
    }
}

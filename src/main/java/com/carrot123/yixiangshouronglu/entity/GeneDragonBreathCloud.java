package com.carrot123.yixiangshouronglu.entity;

import com.carrot123.yixiangshouronglu.gene.BlazingDomain;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public final class GeneDragonBreathCloud extends AreaEffectCloud {
    public GeneDragonBreathCloud(EntityType<? extends GeneDragonBreathCloud> type, Level level) {
        super(type, level);
        setParticle(ParticleTypes.DRAGON_BREATH);
        setRadius(3);
        setDuration(600);
        setRadiusPerTick((7 - getRadius()) / getDuration());
    }

    @Override
    public void tick() {
        if (!level().isClientSide && !GeneDragonFireball.validOwner(getOwner(), level())) {
            discard();
            return;
        }
        super.tick();
        if (level().isClientSide || isRemoved() || tickCount < getWaitTime()
                || (tickCount - getWaitTime()) % 20 != 0 || !(getOwner() instanceof ServerPlayer owner)) { return; }
        double radiusSquared = getRadius() * getRadius();
        var source = damageSources().indirectMagic(this, owner);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox(),
                candidate -> BlazingDomain.canTarget(owner, candidate))) {
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            if (dx * dx + dz * dz <= radiusSquared) { target.hurt(source, 8); }
        }
    }
}

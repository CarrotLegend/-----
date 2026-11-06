package com.carrot123.yixiangshouronglu.entity;

import com.carrot123.yixiangshouronglu.gene.BlazingDomain;
import com.carrot123.yixiangshouronglu.registry.ModEntities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class GeneDragonFireball extends DragonFireball {
    private boolean impacted;

    public GeneDragonFireball(EntityType<? extends GeneDragonFireball> type, Level level) {
        super(type, level);
    }

    public static String fire(ServerPlayer player) {
        Vec3 direction = player.getLookAngle();
        if (!validOwner(player, player.level()) || !Double.isFinite(direction.lengthSqr()) || direction.lengthSqr() < 1.0E-6) {
            return "invalid_state";
        }
        GeneDragonFireball fireball = new GeneDragonFireball(ModEntities.GENE_DRAGON_FIREBALL.get(), player.level());
        fireball.setOwner(player);
        Vec3 start = player.getEyePosition();
        fireball.moveTo(start.x, start.y, start.z, player.getYRot(), player.getXRot());
        Vec3 acceleration = direction.normalize().scale(0.1);
        fireball.xPower = acceleration.x;
        fireball.yPower = acceleration.y;
        fireball.zPower = acceleration.z;
        return player.serverLevel().addFreshEntity(fireball) ? "ok" : "spawn_failed";
    }

    static boolean validOwner(Entity entity, Level level) {
        return entity instanceof ServerPlayer player && player.isAlive() && !player.isRemoved()
                && !player.hasDisconnected() && !player.isSpectator() && player.level() == level;
    }

    @Override
    public void tick() {
        if (!level().isClientSide && !validOwner(getOwner(), level())) {
            discard();
            return;
        }
        super.tick();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!super.canHitEntity(entity)) { return false; }
        return level().isClientSide || getOwner() instanceof ServerPlayer owner
                && entity instanceof LivingEntity target && BlazingDomain.canTarget(owner, target);
    }

    @Override
    protected void onHit(HitResult hit) {
        if (level().isClientSide || isRemoved() || impacted || hit.getType() == HitResult.Type.MISS) { return; }
        if (!(getOwner() instanceof ServerPlayer owner) || !validOwner(owner, level())) {
            discard();
            return;
        }
        LivingEntity target = null;
        if (hit instanceof EntityHitResult entityHit) {
            if (!(entityHit.getEntity() instanceof LivingEntity living) || !BlazingDomain.canTarget(owner, living)) { return; }
            target = living;
        }
        impacted = true;
        if (target != null) { target.hurt(damageSources().indirectMagic(this, owner), 20); }
        GeneDragonBreathCloud cloud = new GeneDragonBreathCloud(ModEntities.GENE_DRAGON_BREATH_CLOUD.get(), level());
        cloud.setOwner(owner);
        cloud.setPos(hit.getLocation());
        level().addFreshEntity(cloud);
        level().levelEvent(2006, blockPosition(), isSilent() ? -1 : 1);
        discard();
    }
}

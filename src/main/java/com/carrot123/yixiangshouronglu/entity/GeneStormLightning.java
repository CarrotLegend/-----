package com.carrot123.yixiangshouronglu.entity;

import com.carrot123.yixiangshouronglu.gene.BlazingDomain;
import com.carrot123.yixiangshouronglu.gene.BossGenes;
import com.carrot123.yixiangshouronglu.gene.GeneEffects;
import com.carrot123.yixiangshouronglu.registry.ModEntities;
import com.github.L_Ender.cataclysm.config.CMConfig;
import com.github.L_Ender.cataclysm.entity.effect.Lightning_Storm_Entity;
import com.github.L_Ender.cataclysm.util.CMDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class GeneStormLightning extends Lightning_Storm_Entity {
    public GeneStormLightning(EntityType<? extends GeneStormLightning> type, Level level) {
        super(type, level);
    }

    public static void strike(ServerPlayer player, LivingEntity target) {
        if (target.level() != player.level()) { return; }
        GeneStormLightning lightning = new GeneStormLightning(ModEntities.GENE_STORM_LIGHTNING.get(), player.level());
        lightning.setCaster(player);
        lightning.setDelay(-9);
        lightning.setSize(2);
        lightning.setDamage((float) CMConfig.ScyllaLightningStormDamage);
        lightning.setHpDamage((float) CMConfig.ScyllaLightningStormHpDamage);
        lightning.moveTo(target.getX(), target.getY(), target.getZ(), player.getYRot(), 0);
        player.serverLevel().addFreshEntity(lightning);
    }

    @Override
    public void tick() {
        if (!level().isClientSide && (!(getCaster() instanceof ServerPlayer player)
                || !GeneDragonFireball.validOwner(player, level()) || player.isChangingDimension()
                || !GeneEffects.installed(player).containsKey(BossGenes.STORM_NERVES))) {
            discard();
            return;
        }
        super.tick();
    }

    @Override
    public void damageEntityLivingBaseNearby(double padding) {
        if (level().isClientSide || !(getCaster() instanceof ServerPlayer player)) { return; }
        AABB bounds = getBoundingBox().inflate(padding);
        bounds = new AABB(bounds.minX, getY() - 0.1, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, bounds)) {
            if (BlazingDomain.canTarget(player, target) && !target.isInvulnerable()
                    && !player.isAlliedTo(target) && !target.isAlliedTo(player)) {
                target.hurt(CMDamageTypes.causeLightningDamage(this, player), getDamage() + target.getMaxHealth() * getHpDamage());
            }
        }
    }
}

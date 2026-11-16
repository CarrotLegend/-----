package com.carrot123.yixiangshouronglu.entity;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.gene.BiologicalGene;
import com.carrot123.yixiangshouronglu.gene.PlayerGeneModules;
import com.carrot123.yixiangshouronglu.registry.ModEntities;
import com.github.L_Ender.cataclysm.entity.effect.Lightning_Storm_Entity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class GeneThunderboltEntity extends Lightning_Storm_Entity {
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "gene_thunderbolt"));
    private UUID targetId;
    private UUID playerId;
    private boolean resolved;

    public GeneThunderboltEntity(EntityType<? extends GeneThunderboltEntity> type, Level level) {
        super(type, level);
    }

    public static void summon(ServerPlayer player, LivingEntity target) {
        var bolt = new GeneThunderboltEntity(ModEntities.GENE_THUNDERBOLT.get(), player.serverLevel());
        bolt.targetId = target.getUUID();
        bolt.playerId = player.getUUID();
        bolt.setSize(1.0F);
        bolt.setPos(target.getX(), target.getY(), target.getZ());
        player.serverLevel().addFreshEntity(bolt);
    }

    @Override
    public void tick() {
        if (!resolved && level() instanceof ServerLevel level && targetId != null
                && level.getEntity(targetId) instanceof LivingEntity target && target.isAlive()) {
            setPos(target.getX(), target.getY(), target.getZ());
        }
        super.tick();
    }

    @Override
    public void damageEntityLivingBaseNearby(double radius) {
        if (resolved || !(level() instanceof ServerLevel level) || targetId == null || playerId == null) {
            return;
        }
        resolved = true;
        var player = level.getServer().getPlayerList().getPlayer(playerId);
        if (player == null || !player.isAlive() || player.serverLevel() != level
                || PlayerGeneModules.get(player).count(BiologicalGene.THUNDER_CORE) == 0
                || !(level.getEntity(targetId) instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }
        var type = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE);
        target.hurt(new DamageSource(type, this, player), BiologicalGene.BOLT_DAMAGE);
    }
}

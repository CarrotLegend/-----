package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class GeneDamageEvents {
    public static final TagKey<DamageType> MAGIC = TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("yixiangshouronglu", "magic"));
    private GeneDamageEvents() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        if (PhantomState.isFollowUp(event.getSource())) { return; }
        if (event.isCanceled() || event.getAmount() <= 0 || event.getEntity().level().isClientSide) { return; }
        ServerPlayer attacker = BossGenes.attacker(event.getSource());
        if (attacker != null) { event.setAmount((float) (event.getAmount() * BossGenes.outgoingMultiplier(attacker)
                * AscendantGenes.outgoingMultiplier(event, attacker))); }
        if (!(event.getEntity() instanceof ServerPlayer player)) { return; }
        double remaining = BossGenes.incomingMultiplier(player);
        if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            for (var module : GeneEffects.installed(player).values()) {
                var definition = GeneModuleItem.definition(module);
                if (definition.effect() != null) { remaining *= 1 - Math.max(0, Math.min(1, definition.effect().explosionReduction(GeneModuleItem.read(module).quality()))); }
            }
        }
        if (event.getSource().is(MAGIC) && GeneEffects.categories(GeneEffects.installed(player)).contains(GeneCategories.ELEMENTAL)) { remaining *= 0.15; }
        event.setAmount((float) (event.getAmount() * remaining));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rangedDamage(LivingDamageEvent event) {
        if (PhantomState.isFollowUp(event.getSource())) { return; }
        if (event.isCanceled() || event.getAmount() <= 0 || event.getEntity().level().isClientSide) { return; }
        var source = event.getSource();
        ServerPlayer attacker = BossGenes.attacker(source);
        if (attacker != null && isRanged(source)) {
            int tier = BossGenes.tier(GeneEffects.installed(attacker), HighTierBossGenes.WITHERED_MECHANICAL_BRAIN);
            if (tier > 0) { event.setAmount((float) (event.getAmount() * (1 + 0.15 * tier))); }
        }
        AscendantGenes.finalDamage(event);
    }

    static boolean isRanged(DamageSource source) {
        return !source.is(DamageTypes.PLAYER_ATTACK)
                && (source.getDirectEntity() instanceof Projectile || source.is(DamageTypeTags.IS_PROJECTILE));
    }
}

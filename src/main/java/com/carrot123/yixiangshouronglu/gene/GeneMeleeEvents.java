package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Queue;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class GeneMeleeEvents {
    private record Hit(ServerPlayer player, LivingEntity target, LivingDamageEvent event) {}
    private static final Queue<Hit> HITS = new ArrayDeque<>();

    static final class BonusFreezeDamage extends DamageSource {
        BonusFreezeDamage(Holder<DamageType> type, ServerPlayer player) { super(type, player); }
        @Override public boolean is(TagKey<DamageType> tag) {
            return tag == DamageTypeTags.BYPASSES_COOLDOWN || super.is(tag);
        }
    }

    private GeneMeleeEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damaged(LivingDamageEvent event) {
        DamageSource source = event.getSource();
        if (event.getAmount() <= 0 || !source.is(DamageTypes.PLAYER_ATTACK)
                || !(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player
                || event.getEntity() == player) { return; }
        var installed = GeneEffects.installed(player);
        if (installed.containsKey(ExtendedGenes.VINE_MUSCLES) || installed.containsKey(ExtendedGenes.FROZEN_BONE_MARROW) || installed.containsKey(ElementalGenes.WITHERED_RIBS)) {
            HITS.add(new Hit(player, event.getEntity(), event));
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) { return; }
        Hit hit;
        while ((hit = HITS.poll()) != null) {
            ServerPlayer player = hit.player();
            LivingEntity target = hit.target();
            if (hit.event().isCanceled() || hit.event().getAmount() <= 0) { continue; }
            if (!player.isAlive() || player.hasDisconnected() || !target.isAlive() || target.isRemoved() || target.level() != player.level()) { continue; }
            var installed = GeneEffects.installed(player);
            var vine = installed.get(ExtendedGenes.VINE_MUSCLES);
            if (vine != null && player.getRandom().nextDouble() < ExtendedGenes.vineChance(GeneModuleItem.read(vine).quality())) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0), player);
            }
            var ribs = installed.get(ElementalGenes.WITHERED_RIBS);
            if (ribs != null && player.getRandom().nextDouble() < ElementalGenes.witherChance(GeneModuleItem.read(ribs).quality())) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), player);
            }
            var marrow = installed.get(ExtendedGenes.FROZEN_BONE_MARROW);
            if (marrow != null) {
                DamageSource freeze = new BonusFreezeDamage(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.FREEZE), player);
                target.hurt(freeze, ExtendedGenes.freezeDamage(GeneModuleItem.read(marrow).quality()));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) { return; }
        var effect = event.getEffectInstance().getEffect();
        if (effect == MobEffects.LEVITATION && GeneEffects.installed(player).containsKey(DragonGenes.VOID_CARAPACE)) {
            event.setResult(Event.Result.DENY);
        }
        if ((effect == MobEffects.HUNGER || effect == MobEffects.POISON)
                && GeneEffects.installed(player).containsKey(ExtendedGenes.HEART_OF_DUST)) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { HITS.clear(); }
}

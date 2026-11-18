package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.gene.BiologicalGene;
import com.carrot123.yixiangshouronglu.registry.ModEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class SearingBurnEvents {
    private static final String STATE = "yixiangshouronglu:searing_burn";
    private static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(STATE));

    static void apply(ServerPlayer player, LivingEntity target, long now) {
        var burn = ModEffects.SEARING_BURN.get();
        var data = target.getPersistentData();
        if (!target.hasEffect(burn) || !data.getCompound(STATE).hasUUID("Source")) {
            var state = new CompoundTag();
            state.putUUID("Source", player.getUUID());
            state.putLong("NextDamageAt", now + 20);
            data.put(STATE, state);
        }
        ChillEvents.forceApply(target, new MobEffectInstance(burn, BiologicalGene.SEARING_TICKS), player);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        var target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level) || !target.getPersistentData().contains(STATE)) {
            return;
        }
        if (!target.isAlive() || !target.hasEffect(ModEffects.SEARING_BURN.get())) {
            target.getPersistentData().remove(STATE);
            return;
        }
        var state = target.getPersistentData().getCompound(STATE);
        long now = level.getServer().overworld().getGameTime();
        if (now < state.getLong("NextDamageAt")) {
            return;
        }
        state.putLong("NextDamageAt", now + 20);
        var player = state.hasUUID("Source") ? level.getServer().getPlayerList().getPlayer(state.getUUID("Source")) : null;
        if (player != null) {
            var type = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE);
            target.hurt(new DamageSource(type, player), Math.min(target.getMaxHealth() * 0.01F, 100.0F));
        }
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        event.getEntity().getPersistentData().remove(STATE);
    }
}

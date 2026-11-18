package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.gene.BiologicalGene;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class BlazingMusclesEvents {
    private static final String STATE = "yixiangshouronglu:blazing_muscles";
    private static final int TICKS_PER_SECOND = 20;
    private static final ResourceKey<DamageType> FIRE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(STATE));
    private static final ResourceKey<DamageType> FIRE_IMMUNE = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "blazing_muscles_immune"));

    static void ignite(ServerPlayer player, LivingEntity target, long now) {
        var data = target.getPersistentData();
        var state = data.getCompound(STATE);
        if (now >= state.getLong("EndsAt")) {
            state.putLong("NextDamageAt", now + TICKS_PER_SECOND);
        }
        state.putUUID("Source", player.getUUID());
        state.putLong("EndsAt", now + BiologicalGene.BLAZING_MUSCLES_BURN_SECONDS * TICKS_PER_SECOND);
        data.put(STATE, state);
        if (!target.fireImmune()) {
            target.setSecondsOnFire(BiologicalGene.BLAZING_MUSCLES_BURN_SECONDS);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void attributeFireDamage(LivingAttackEvent event) {
        var target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level) || !target.isAlive() || target.fireImmune()
                || !event.getSource().is(DamageTypes.ON_FIRE) || event.getSource().getEntity() != null
                || !target.getPersistentData().contains(STATE)) {
            return;
        }
        var state = target.getPersistentData().getCompound(STATE);
        if (time(level) >= state.getLong("EndsAt")) {
            return;
        }
        var player = source(level, state);
        if (player != null) {
            event.setCanceled(true);
            hurt(level, target, player, FIRE);
        }
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        var target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level) || !target.getPersistentData().contains(STATE)) {
            return;
        }
        var data = target.getPersistentData();
        var state = data.getCompound(STATE);
        long now = time(level);
        boolean immune = target.fireImmune();
        if (!target.isAlive() || now > state.getLong("EndsAt")
                || (immune ? target.isInWaterRainOrBubble() : target.getRemainingFireTicks() <= 0)) {
            data.remove(STATE);
            return;
        }
        if (!immune || now < state.getLong("NextDamageAt")) {
            return;
        }
        state.putLong("NextDamageAt", now + TICKS_PER_SECOND);
        var player = source(level, state);
        if (player != null && !target.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            hurt(level, target, player, FIRE_IMMUNE);
            level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() / 2,
                    target.getZ(), 6, 0.25, 0.35, 0.25, 0.01);
        }
    }

    private static ServerPlayer source(ServerLevel level, CompoundTag state) {
        var player = state.hasUUID("Source") ? level.getServer().getPlayerList().getPlayer(state.getUUID("Source")) : null;
        return player != null && player.level() == level ? player : null;
    }

    private static long time(ServerLevel level) {
        return level.getServer().overworld().getGameTime();
    }

    private static void hurt(ServerLevel level, LivingEntity target, ServerPlayer player, ResourceKey<DamageType> damage) {
        var type = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(damage);
        target.hurt(new DamageSource(type, player), BiologicalGene.BLAZING_MUSCLES_BURN_DAMAGE);
    }
}

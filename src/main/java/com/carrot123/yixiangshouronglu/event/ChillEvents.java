package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.compat.mixin.LivingEntityEffectAccess;
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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class ChillEvents {
    private static final String STATE = AnomalousContainmentChronicles.MOD_ID + ":chill";
    private static final UUID SLOW = UUID.fromString("d465d159-4713-4fb2-92e6-8a44f1e3109f");
    private static final UUID WEAKNESS = UUID.fromString("dc971769-4de9-48ea-94d4-5d79831e68a6");
    private static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "chill"));

    private ChillEvents() {}

    static void apply(LivingEntity target, ServerPlayer source) {
        if (!(target.level() instanceof ServerLevel level) || !target.isAlive()) {
            return;
        }
        var chill = ModEffects.CHILL.get();
        var previous = target.getEffect(chill);
        int amplifier = previous == null ? 0 : Math.min(BiologicalGene.CHILL_STACKS - 1, previous.getAmplifier() + 1);
        var next = new MobEffectInstance(chill, BiologicalGene.CHILL_TICKS, amplifier);
        var state = target.getPersistentData().getCompound(STATE);
        if (previous == null || !state.hasUUID("Source")) {
            state = new CompoundTag();
            state.putUUID("Source", source.getUUID());
            state.putLong("NextDamageAt", time(level) + 20);
        }
        target.getPersistentData().put(STATE, state);
        target.forceAddEffect(next, source);
        if (target.getEffect(chill) != next) {
            var access = (LivingEntityEffectAccess) target;
            if (target.getActiveEffectsMap().put(chill, next) == null) {
                access.acc$onEffectAdded(next, source);
            } else {
                access.acc$onEffectUpdated(next, true, source);
            }
        }
        refreshModifiers(target, next);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void allowChill(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == ModEffects.CHILL.get()) {
            event.setResult(event.getEntity().isAlive() ? Event.Result.ALLOW : Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        var target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        var persistent = target.getPersistentData();
        var chill = target.getEffect(ModEffects.CHILL.get());
        if (chill == null || !target.isAlive()) {
            if (persistent.contains(STATE)) {
                clear(target);
            }
            return;
        }
        var state = persistent.getCompound(STATE);
        long now = time(level);
        if (!persistent.contains(STATE)) {
            state.putLong("NextDamageAt", now + 20);
            persistent.put(STATE, state);
        }
        refreshModifiers(target, chill);
        if (now >= state.getLong("NextDamageAt")) {
            state.putLong("NextDamageAt", now + 20);
            var source = state.hasUUID("Source") ? level.getServer().getPlayerList().getPlayer(state.getUUID("Source")) : null;
            var type = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE);
            int stacks = Math.min(BiologicalGene.CHILL_STACKS, chill.getAmplifier() + 1);
            target.hurt(new DamageSource(type, source), stacks * BiologicalGene.CHILL_DAMAGE);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        var target = event.getEntity();
        if (target.level() instanceof ServerLevel && target.getPersistentData().contains(STATE)) {
            clear(target);
        }
    }

    private static long time(ServerLevel level) {
        return level.getServer().overworld().getGameTime();
    }

    private static void refreshModifiers(LivingEntity target, MobEffectInstance chill) {
        supplement(target, MobEffects.MOVEMENT_SLOWDOWN, Attributes.MOVEMENT_SPEED, SLOW,
                Math.min(BiologicalGene.CHILL_STACKS - 1, chill.getAmplifier()));
        supplement(target, MobEffects.WEAKNESS, Attributes.ATTACK_DAMAGE, WEAKNESS, 0);
    }

    private static void supplement(LivingEntity target, MobEffect vanilla, Attribute attribute, UUID id, int amplifier) {
        var instance = target.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        var template = vanilla.getAttributeModifiers().get(attribute);
        var vanillaModifier = instance.getModifier(template.getId());
        double original = vanillaModifier == null ? 0 : vanillaModifier.getAmount();
        double desired = vanilla.getAttributeModifierValue(amplifier, template);
        double extra = Math.min(0, desired - original);
        if (template.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL && extra < 0 && original > -1) {
            extra /= 1 + original;
        }
        var existing = instance.getModifier(id);
        if (existing != null && existing.getAmount() == extra) {
            return;
        }
        if (existing != null) {
            instance.removeModifier(id);
        }
        if (extra < 0) {
            instance.addTransientModifier(new AttributeModifier(id, STATE, extra, template.getOperation()));
        }
    }

    private static void clear(LivingEntity target) {
        target.getPersistentData().remove(STATE);
        var speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SLOW);
        }
        var attack = target.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.removeModifier(WEAKNESS);
        }
    }
}

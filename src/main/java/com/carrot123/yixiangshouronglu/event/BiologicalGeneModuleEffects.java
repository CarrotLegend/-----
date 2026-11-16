package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.entity.GeneThunderboltEntity;
import com.carrot123.yixiangshouronglu.gene.BiologicalGene;
import com.carrot123.yixiangshouronglu.gene.PlayerGeneModules;
import com.github.L_Ender.cataclysm.init.ModEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class BiologicalGeneModuleEffects {
    public static final ResourceKey<DamageType> LAVA_SHOCK = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "lava_shock"));
    private static final TagKey<DamageType> RANGED_ATTACK = TagKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "ranged_attack"));
    private static final TagKey<DamageType> MAGIC = TagKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation("forge", "is_magic"));
    private static final ResourceLocation CRIT_CHANCE = new ResourceLocation("attributeslib", "crit_chance");
    private static final ResourceLocation COLD_DAMAGE = new ResourceLocation("attributeslib", "cold_damage");

    private BiologicalGeneModuleEffects() {}

    public static void refresh(ServerPlayer player) {
        var modules = PlayerGeneModules.get(player);
        modules.recount();
        for (BiologicalGene gene : BiologicalGene.values()) {
            int count = modules.count(gene);
            update(player, gene, Attributes.ARMOR, gene.armor, count);
            update(player, gene, Attributes.MAX_HEALTH, gene.health, count);
            update(player, gene, Attributes.ATTACK_DAMAGE, gene.attack, count);
        }
        var war = BiologicalGene.WAR_INSTINCT;
        update(player, war, ForgeRegistries.ATTRIBUTES.getValue(CRIT_CHANCE),
                BiologicalGene.CRIT_CHANCE_POINTS / 100.0, modules.count(war));
        var thunder = BiologicalGene.THUNDER_CORE;
        update(player, thunder, ForgeRegistries.ATTRIBUTES.getValue(COLD_DAMAGE),
                BiologicalGene.COLD_DAMAGE, modules.count(thunder));
        var abyss = BiologicalGene.ABYSSAL_CIRCULATION;
        updatePercent(player, abyss, Attributes.MOVEMENT_SPEED, BiologicalGene.MOVE_SPEED_PERCENT, modules.count(abyss));
        updatePercent(player, abyss, ForgeMod.SWIM_SPEED.get(), BiologicalGene.SWIM_SPEED_PERCENT, modules.count(abyss));
        var cursed = BiologicalGene.CURSED_MUSCLE_FIBERS;
        updatePercent(player, cursed, Attributes.ATTACK_SPEED, BiologicalGene.ATTACK_SPEED_PERCENT, modules.count(cursed));
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void update(ServerPlayer player, BiologicalGene gene, Attribute attribute, double points, int count) {
        if (points != 0) {
            update(player, gene, attribute, points * count, AttributeModifier.Operation.ADDITION);
        }
    }

    private static void updatePercent(ServerPlayer player, BiologicalGene gene, Attribute attribute, int percent, int count) {
        update(player, gene, attribute, percent * count / 100.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static void update(ServerPlayer player, BiologicalGene gene, Attribute attribute, double amount,
                               AttributeModifier.Operation operation) {
        if (attribute == null) {
            return;
        }
        var instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        String key = AnomalousContainmentChronicles.MOD_ID + ":gene_modules/" + gene.id + "/" + attribute.getDescriptionId();
        UUID uuid = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        var current = instance.getModifier(uuid);
        if (current != null && current.getAmount() == amount
                && current.getOperation() == operation) {
            return;
        }
        if (current != null) {
            instance.removeModifier(uuid);
        }
        if (amount > 0) {
            instance.addPermanentModifier(new AttributeModifier(uuid, key, amount, operation));
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        restore(event);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        restore(event);
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        restore(event);
    }

    private static void restore(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        var modules = PlayerGeneModules.get(player);
        modules.rageStacks(gameTime(player));
        if (modules.tickWaterHealing(player.isAlive() && player.isInWater())
                && player.getHealth() < player.getMaxHealth()) {
            player.heal(BiologicalGene.WATER_HEAL);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        var target = event.getEntity();
        var source = event.getSource();
        if (target.level().isClientSide || !Float.isFinite(event.getAmount()) || event.getAmount() <= 0) {
            return;
        }
        ServerPlayer attacker = source.getEntity() instanceof ServerPlayer player ? player : null;
        if (attacker == null && source.getEntity() == null && source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof ServerPlayer owner) {
            attacker = owner;
        }
        if (attacker != null && attacker != target) {
            applyOutgoing(event, attacker, target, source);
        }
        if (target instanceof ServerPlayer player) {
            applyIncoming(event, player, source);
        }
    }

    private static void applyOutgoing(LivingDamageEvent event, ServerPlayer player, LivingEntity target, DamageSource source) {
        if (source.is(LAVA_SHOCK) || source.is(GeneThunderboltEntity.DAMAGE)) {
            return;
        }
        var modules = PlayerGeneModules.get(player);
        long now = gameTime(player);
        boolean ranged = source.getDirectEntity() instanceof Projectile
                || source.is(DamageTypeTags.IS_PROJECTILE) || source.is(RANGED_ATTACK);
        boolean secondary = source.typeHolder().unwrapKey().map(key -> {
            ResourceLocation id = key.location();
            return id.getNamespace().equals("attributeslib")
                    || id.getNamespace().equals(AnomalousContainmentChronicles.MOD_ID);
        }).orElse(false);
        boolean melee = !ranged && source.getDirectEntity() == player && isDirectAttack(source);
        boolean active = !secondary && (melee || ranged);
        int soulfire = modules.count(BiologicalGene.SOULFIRE_REACTOR_CORE);
        int bonus = soulfire * (BiologicalGene.SOULFIRE_DAMAGE_PERCENT
                + (target.isOnFire() ? BiologicalGene.BURNING_DAMAGE_PERCENT : 0));
        bonus += modules.rageStacks(now) * BiologicalGene.RAGE_DAMAGE_PERCENT;
        if (active && ranged) {
            bonus += modules.count(BiologicalGene.WAR_INSTINCT) * BiologicalGene.RANGED_DAMAGE_PERCENT;
        }
        if (bonus > 0) {
            event.setAmount(event.getAmount() * (1 + bonus / 100.0F));
        }
        if (!active) {
            return;
        }
        if (soulfire > 0 && roll(player, BiologicalGene.BRAND_CHANCE_PERCENT)) {
            target.addEffect(new MobEffectInstance(ModEffect.EFFECTBLAZING_BRAND.get(), BiologicalGene.BRAND_TICKS), player);
        }
        if (modules.count(BiologicalGene.DESERT_MUSCLE_REINFORCEMENT) > 0 && roll(player, BiologicalGene.STUN_CHANCE_PERCENT)) {
            target.addEffect(new MobEffectInstance(ModEffect.EFFECTSTUN.get(), BiologicalGene.STUN_TICKS), player);
        }
        if (melee && modules.count(BiologicalGene.CURSED_MUSCLE_FIBERS) > 0) {
            modules.addRage(now);
        }
        if (modules.count(BiologicalGene.THUNDER_CORE) > 0 && roll(player, BiologicalGene.BOLT_CHANCE_PERCENT)) {
            GeneThunderboltEntity.summon(player, target);
        }
    }

    private static boolean isDirectAttack(DamageSource source) {
        return !source.is(DamageTypes.THORNS) && !source.is(DamageTypeTags.IS_PROJECTILE)
                && !source.is(DamageTypeTags.IS_EXPLOSION) && !source.is(DamageTypeTags.IS_FIRE)
                && !source.is(DamageTypeTags.IS_FALL) && !source.is(MAGIC)
                && !source.is(DamageTypes.MAGIC) && !source.is(DamageTypes.INDIRECT_MAGIC);
    }

    private static boolean roll(ServerPlayer player, int chance) {
        return player.getRandom().nextInt(100) < chance;
    }

    private static long gameTime(ServerPlayer player) {
        return player.serverLevel().getServer().overworld().getGameTime();
    }

    private static void applyIncoming(LivingDamageEvent event, ServerPlayer player, DamageSource source) {
        var modules = PlayerGeneModules.get(player);
        if (modules.count(BiologicalGene.SPINAL_VOIDIZATION) > 0
                && player.getRandom().nextInt(100) < BiologicalGene.VOID_CHANCE_PERCENT) {
            event.setAmount(event.getAmount() * (1 - BiologicalGene.VOID_REDUCTION_PERCENT / 100.0F));
        }
        if (modules.count(BiologicalGene.MOLTEN_SKELETON) == 0
                || !(source.getDirectEntity() instanceof LivingEntity attacker)
                || source.getEntity() != attacker || attacker == player || !attacker.isAlive()
                || source.is(LAVA_SHOCK) || source.is(GeneThunderboltEntity.DAMAGE)
                || source.is(RANGED_ATTACK) || !isDirectAttack(source)
                || !modules.startShockCooldown(gameTime(player))) {
            return;
        }
        var level = player.serverLevel();
        var shock = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(LAVA_SHOCK), player);
        attacker.hurt(shock, BiologicalGene.SHOCK_DAMAGE);
        attacker.setSecondsOnFire(BiologicalGene.SHOCK_FIRE_SECONDS);
        level.sendParticles(ParticleTypes.LAVA, attacker.getX(), attacker.getY() + attacker.getBbHeight() / 2,
                attacker.getZ(), 8, 0.25, 0.3, 0.25, 0.08);
        level.playSound(null, attacker.blockPosition(), SoundEvents.LAVA_POP, SoundSource.PLAYERS, 1.0F, 0.8F);
    }
}

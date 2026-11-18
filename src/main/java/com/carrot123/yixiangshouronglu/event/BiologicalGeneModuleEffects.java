package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.entity.GeneThunderboltEntity;
import com.carrot123.yixiangshouronglu.gene.BiologicalGene;
import com.carrot123.yixiangshouronglu.gene.PlayerGeneModules;
import com.carrot123.yixiangshouronglu.registry.ModEffects;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.Event;
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
    private static final ResourceLocation DODGE_CHANCE = new ResourceLocation("attributeslib", "dodge_chance");
    private static final ResourceLocation LIFE_STEAL = new ResourceLocation("attributeslib", "life_steal");
    private static final TagKey<DamageType> MODULE_EXTRA = TagKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "legendary_gene_extra"));

    private BiologicalGeneModuleEffects() {}

    public static void refresh(ServerPlayer player) {
        var modules = PlayerGeneModules.getIfPresent(player);
        if (modules == null) {
            return;
        }
        modules.recount();
        MutantGeneSkills.regeneration(player, modules.count(BiologicalGene.UNDYING_BODY) > 0);
        LegendaryGeneAttacks.discardUnequipped(player, modules);
        refreshFallenForm(player, modules);
        refreshLavaDigestion(player, modules.count(BiologicalGene.LAVA_DIGESTION) > 0);
        for (BiologicalGene gene : BiologicalGene.values()) {
            int count = modules.count(gene);
            update(player, gene, Attributes.ARMOR, gene.armor, count);
            update(player, gene, Attributes.MAX_HEALTH, gene.health, count);
            update(player, gene, Attributes.ATTACK_DAMAGE, gene.attack, count);
        }
        var plant = BiologicalGene.PLANT_MUSCLE_FIBERS;
        update(player, plant, ForgeMod.ENTITY_REACH.get(), 1, modules.count(plant));
        var arachnid = BiologicalGene.ARACHNID_APONEUROSIS;
        int arachnidCount = modules.count(arachnid);
        updatePercent(player, arachnid, Attributes.MOVEMENT_SPEED, 15, arachnidCount);
        update(player, arachnid, GeneJumpEvents.JUMP_HEIGHT.get(), arachnidCount * 0.25,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        if (modules.count(BiologicalGene.QUICKSAND_METABOLISM) > 0) {
            player.removeEffect(MobEffects.HUNGER);
        }
        var war = BiologicalGene.WAR_INSTINCT;
        update(player, war, ForgeRegistries.ATTRIBUTES.getValue(CRIT_CHANCE),
                BiologicalGene.CRIT_CHANCE_POINTS / 100.0, modules.count(war));
        var sandstorm = BiologicalGene.SANDSTORM_VISION;
        update(player, sandstorm, ForgeRegistries.ATTRIBUTES.getValue(CRIT_CHANCE),
                BiologicalGene.SANDSTORM_CRIT_PERCENT / 100.0, modules.count(sandstorm));
        var solar = BiologicalGene.SOLAR_RETINA;
        update(player, solar, ForgeRegistries.ATTRIBUTES.getValue(CRIT_CHANCE),
                BiologicalGene.SOLAR_CRIT_PERCENT / 100.0, modules.count(solar));
        var spine = BiologicalGene.ANCIENT_DRAGON_SPINE;
        update(player, spine, Attributes.KNOCKBACK_RESISTANCE,
                BiologicalGene.ANCIENT_KNOCKBACK_RESISTANCE_PERCENT / 100.0, modules.count(spine));
        var chorus = BiologicalGene.CHORUS_TRANSLOCATION;
        updatePercent(player, chorus, Attributes.MOVEMENT_SPEED, BiologicalGene.CHORUS_SPEED_PERCENT, modules.count(chorus));
        update(player, chorus, ForgeRegistries.ATTRIBUTES.getValue(DODGE_CHANCE),
                BiologicalGene.CHORUS_DODGE_PERCENT / 100.0, modules.count(chorus));
        var ender = BiologicalGene.ENDER_SYNAPSE;
        updatePercent(player, ender, Attributes.MOVEMENT_SPEED, 10, modules.count(ender));
        update(player, ender, ForgeRegistries.ATTRIBUTES.getValue(DODGE_CHANCE), 0.05, modules.count(ender));
        var frost = BiologicalGene.FROST_CORE_RETINA;
        update(player, frost, ForgeRegistries.ATTRIBUTES.getValue(CRIT_CHANCE), 0.10, modules.count(frost));
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
        update(player, gene, attribute, amount, operation, true);
    }

    private static void update(ServerPlayer player, BiologicalGene gene, Attribute attribute, double amount,
                               AttributeModifier.Operation operation, boolean permanent) {
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
            var modifier = new AttributeModifier(uuid, key, amount, operation);
            if (permanent) {
                instance.addPermanentModifier(modifier);
            } else {
                instance.addTransientModifier(modifier);
            }
        }
    }

    private static void refreshFallenForm(ServerPlayer player, PlayerGeneModules modules) {
        double amount = modules.fallenForm(gameTime(player)) ? BiologicalGene.FALLEN_BONUS_PERCENT / 100.0 : 0;
        var gene = BiologicalGene.FALLEN_COMBAT_NERVES;
        update(player, gene, Attributes.ATTACK_SPEED, amount, AttributeModifier.Operation.MULTIPLY_TOTAL, false);
        update(player, gene, ForgeRegistries.ATTRIBUTES.getValue(LIFE_STEAL), amount, AttributeModifier.Operation.ADDITION, false);
    }

    private static void refreshLavaDigestion(ServerPlayer player, boolean equipped) {
        double amount = equipped && player.isAlive() && player.isOnFire()
                ? BiologicalGene.LAVA_ATTACK_PERCENT / 100.0 : 0;
        update(player, BiologicalGene.LAVA_DIGESTION, Attributes.ATTACK_DAMAGE,
                amount, AttributeModifier.Operation.MULTIPLY_TOTAL, false);
    }

    public static void activateSkill(ServerPlayer player) {
        if (!player.isAlive() || player.isRemoved()
                || player.hasDisconnected() || player.isSpectator()) {
            return;
        }
        var modules = PlayerGeneModules.getIfPresent(player);
        if (modules != null && modules.count(BiologicalGene.ENDER_SYNAPSE) > 0) {
            MutantGeneSkills.leap(player, modules, gameTime(player));
            return;
        }
        if (modules == null || modules.count(BiologicalGene.FALLEN_COMBAT_NERVES) == 0
                || !modules.startFallenForm(gameTime(player))) {
            return;
        }
        refreshFallenForm(player, modules);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
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
        if (event.getEntity() instanceof ServerPlayer player) {
            var modules = PlayerGeneModules.getIfPresent(player);
            if (modules != null) {
                modules.clearLeap();
                modules.clearSkyCombo();
            }
            LegendaryGeneAttacks.discard(player);
        }
        restore(event);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearLegendaryCombat(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearLegendaryCombat(player);
        }
    }

    private static void clearLegendaryCombat(ServerPlayer player) {
        var modules = PlayerGeneModules.getIfPresent(player);
        if (modules != null) {
            modules.clearSkyCombo();
            modules.clearLeap();
            modules.endFallenForm();
            refreshFallenForm(player, modules);
            refreshLavaDigestion(player, false);
        }
        LegendaryGeneAttacks.discard(player);
    }

    private static void restore(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || !player.isAlive()
                || player.isRemoved()
                || player.hasDisconnected()) {
            return;
        }
        var modules = PlayerGeneModules.getIfPresent(player);
        if (modules == null) {
            return;
        }
        long now = gameTime(player);
        modules.rageStacks(now);
        MutantGeneSkills.regeneration(player, modules.count(BiologicalGene.UNDYING_BODY) > 0);
        modules.settleLeap(now);
        if (modules.count(BiologicalGene.LAVA_DIGESTION) > 0) {
            refreshLavaDigestion(player, true);
        }
        if (modules.expireFallenForm(now)) {
            refreshFallenForm(player, modules);
        }
        LegendaryGeneAttacks.tick(player, modules, now);
        if (modules.tickWaterHealing(player.isAlive() && player.isInWater())
                && player.getHealth() < player.getMaxHealth()) {
            player.heal(BiologicalGene.WATER_HEAL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0) {
            return;
        }
        var modules = PlayerGeneModules.getIfPresent(player);
        if (modules == null) {
            return;
        }
        var source = event.getSource();
        if ((modules.count(BiologicalGene.ANCIENT_DRAGON_SPINE) > 0 && source.is(DamageTypes.FALL))
                || (modules.count(BiologicalGene.LAVA_DIGESTION) > 0
                && (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LAVA) || source.is(DamageTypes.HOT_FLOOR)))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void combatNerves(LivingAttackEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()
                || player.isRemoved() || !Float.isFinite(event.getAmount()) || event.getAmount() <= 0) {
            return;
        }
        var modules = PlayerGeneModules.getIfPresent(player);
        if (modules == null || modules.count(BiologicalGene.COMBAT_NERVES) == 0) {
            return;
        }
        var source = event.getSource();
        if (source.is(MODULE_EXTRA) || source.is(LAVA_SHOCK) || source.is(GeneThunderboltEntity.DAMAGE)
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        var direct = source.getDirectEntity();
        var owner = source.getEntity() == null && direct instanceof Projectile projectile
                ? projectile.getOwner() : source.getEntity();
        boolean melee = direct == owner && isDirectAttack(source);
        boolean projectileHit = direct instanceof Projectile || source.is(DamageTypeTags.IS_PROJECTILE) || source.is(RANGED_ATTACK);
        if ((melee || projectileHit) && owner instanceof LivingEntity attacker
                && isHostileAttack(player, attacker) && roll(player, 12)) {
            event.setCanceled(true);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1, 1);
        }
    }

    private static boolean isHostileAttack(ServerPlayer target, LivingEntity attacker) {
        if (attacker instanceof Player player) {
            return player != target && player.isAlive() && !player.isRemoved() && !player.isSpectator()
                    && player.level() == target.level() && !player.isAlliedTo(target) && player.canHarmPlayer(target);
        }
        return MutantGeneSkills.enemy(target, attacker);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preventHunger(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == MobEffects.HUNGER && event.getEntity() instanceof ServerPlayer player) {
            var modules = PlayerGeneModules.getIfPresent(player);
            if (modules != null && modules.count(BiologicalGene.QUICKSAND_METABOLISM) > 0) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    @SubscribeEvent
    public static void beforePlayerAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var modules = PlayerGeneModules.getIfPresent(player);
        if (modules != null && modules.count(BiologicalGene.LAVA_DIGESTION) > 0) {
            refreshLavaDigestion(player, true);
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
        if (source.is(LAVA_SHOCK) || source.is(GeneThunderboltEntity.DAMAGE) || source.is(MODULE_EXTRA)) {
            return;
        }
        var modules = PlayerGeneModules.getIfPresent(player);
        if (modules == null) {
            return;
        }
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
        bonus += modules.count(BiologicalGene.SKY_STORM_CORE) * BiologicalGene.SKY_DAMAGE_PERCENT;
        bonus += modules.count(BiologicalGene.ANNIHILATION_REACTOR_CORE) * BiologicalGene.ANNIHILATION_DAMAGE_PERCENT;
        if (modules.fallenForm(now)) {
            bonus += BiologicalGene.FALLEN_BONUS_PERCENT;
        }
        if (active && ranged) {
            bonus += modules.count(BiologicalGene.WAR_INSTINCT) * BiologicalGene.RANGED_DAMAGE_PERCENT;
            if (MutantGeneSkills.bowArrow(source.getDirectEntity())) {
                bonus += modules.count(BiologicalGene.PRECISION_MUSCLE_FIBERS) * 20;
            }
        }
        if (bonus > 0) {
            event.setAmount(event.getAmount() * (1 + bonus / 100.0F));
        }
        if (active && player.isAlive() && MutantGeneSkills.attackable(player, target)) {
            if (modules.count(BiologicalGene.FROZEN_CIRCULATORY_TISSUE) > 0 && target.hasEffect(ModEffects.CHILL.get())) {
                event.setAmount(event.getAmount() * 1.45F);
            }
            if (modules.leapActive(now)) {
                event.setAmount(event.getAmount() * 2);
                modules.reserveLeap(() -> !event.isCanceled() && event.getAmount() > 0);
            }
            LegendaryGeneAttacks.onElementalHit(player, target, modules, now, event);
        }
        if (!active) {
            return;
        }
        if (!source.is(DamageTypes.ON_FIRE) && !source.is(DamageTypes.IN_FIRE) && !source.is(DamageTypes.WITHER)
                && !source.is(DamageTypes.MAGIC) && !source.is(DamageTypes.INDIRECT_MAGIC)) {
            applyGlandularAndWither(player, target, modules, melee);
            LegendaryGeneAttacks.onMowzieHit(player, target, modules, now, melee, event);
        }
        LegendaryGeneAttacks.onHit(player, target, modules, now, melee);
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

    private static void applyGlandularAndWither(ServerPlayer player, LivingEntity target, PlayerGeneModules modules, boolean melee) {
        if (modules.count(BiologicalGene.GLANDULAR_REGENERATION) > 0) {
            float missing = player.getMaxHealth() - player.getHealth();
            if (player.isAlive() && missing > 0 && target.hasEffect(MobEffects.POISON)) {
                player.heal(Math.min(missing, BiologicalGene.GLANDULAR_HEAL));
            }
            if (melee && roll(player, BiologicalGene.GLANDULAR_POISON_CHANCE_PERCENT)) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, BiologicalGene.GLANDULAR_POISON_TICKS), player);
            }
        }
        if (modules.count(BiologicalGene.ABOMINATION_SPINAL_CORD) > 0) {
            var existing = target.getEffect(MobEffects.WITHER);
            if (existing == null || existing.getAmplifier() <= BiologicalGene.ABOMINATION_WITHER_AMPLIFIER) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER,
                        BiologicalGene.ABOMINATION_WITHER_TICKS, BiologicalGene.ABOMINATION_WITHER_AMPLIFIER), player);
            }
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
        var modules = PlayerGeneModules.getIfPresent(player);
        if (modules == null) {
            return;
        }
        if (modules.count(BiologicalGene.SPINAL_VOIDIZATION) > 0
                && player.getRandom().nextInt(100) < BiologicalGene.VOID_CHANCE_PERCENT) {
            event.setAmount(event.getAmount() * (1 - BiologicalGene.VOID_REDUCTION_PERCENT / 100.0F));
        }
        if (!source.is(MODULE_EXTRA) && !source.is(LAVA_SHOCK) && !source.is(GeneThunderboltEntity.DAMAGE)
                && modules.count(BiologicalGene.PRIMORDIAL_ICE_CRYSTALLIZATION) > 0) {
            var direct = source.getDirectEntity();
            var owner = source.getEntity() == null && direct instanceof Projectile projectile
                    ? projectile.getOwner() : source.getEntity();
            boolean directHit = direct == owner && isDirectAttack(source);
            boolean rangedHit = direct instanceof Projectile || source.is(DamageTypeTags.IS_PROJECTILE) || source.is(RANGED_ATTACK);
            if ((directHit || rangedHit) && owner instanceof LivingEntity attacker && attacker != player
                    && attacker.isAlive() && attacker.level() == player.level()
                    && roll(player, BiologicalGene.CHILL_CHANCE_PERCENT)) {
                ChillEvents.apply(attacker, player);
            }
        }
        if (!source.is(MODULE_EXTRA) && !source.is(LAVA_SHOCK) && !source.is(GeneThunderboltEntity.DAMAGE)
                && modules.count(BiologicalGene.EXPLOSIVE_GLAND) > 0 && roll(player, 15)) {
            LegendaryGeneAttacks.burst(player, event, gameTime(player));
        }
        if (modules.count(BiologicalGene.MOLTEN_SKELETON) == 0
                || !(source.getDirectEntity() instanceof LivingEntity attacker)
                || source.getEntity() != attacker || attacker == player || !attacker.isAlive()
                || source.is(LAVA_SHOCK) || source.is(GeneThunderboltEntity.DAMAGE) || source.is(MODULE_EXTRA)
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

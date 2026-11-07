package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.entity.GeneDragonBreathBeam;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class HighTierBossGenes {
    public static final ResourceLocation FIRE_DRAGON_GLAND = id("fire_dragon_gland");
    public static final ResourceLocation ICE_DRAGON_GLAND = id("ice_dragon_gland");
    public static final ResourceLocation LIGHTNING_DRAGON_GLAND = id("lightning_dragon_gland");
    public static final ResourceLocation INFERNAL_CORE = id("infernal_core");
    public static final ResourceLocation DESERT_MARROW = id("desert_marrow");
    public static final ResourceLocation WITHERED_MECHANICAL_BRAIN = id("withered_mechanical_brain");
    public static final ResourceLocation SCULK_VOCAL_CORDS = id("sculk_vocal_cords");
    public static final ResourceLocation UNDYING_ADAPTIVE_CELLS = id("undying_adaptive_cells");
    public static final List<ResourceLocation> MODULES = List.of(FIRE_DRAGON_GLAND, ICE_DRAGON_GLAND, LIGHTNING_DRAGON_GLAND,
            INFERNAL_CORE, DESERT_MARROW, WITHERED_MECHANICAL_BRAIN, SCULK_VOCAL_CORDS, UNDYING_ADAPTIVE_CELLS);
    public static final ResourceLocation SONIC_SHRIEK = id("sonic_shriek");
    private static final ResourceLocation ATTACK = new ResourceLocation("minecraft", "generic.attack_damage");
    private static final ResourceLocation ARMOR_SHRED = new ResourceLocation("attributeslib", "armor_shred");
    private static final ResourceLocation DRAW_SPEED = new ResourceLocation("attributeslib", "draw_speed");
    private static final List<RegenHit> REGEN_HITS = new ArrayList<>();

    private record RegenHit(LivingDamageEvent event, ServerPlayer player, Level level) {}

    public static final class SkillDamage extends DamageSource {
        private SkillDamage(ServerPlayer player, Entity direct, ResourceKey<DamageType> type) {
            super(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), direct, player);
        }
    }

    private HighTierBossGenes() {}

    private static ResourceLocation id(String path) {
        return new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, path);
    }

    public static void register() {
        for (var kind : GeneDragonBreathBeam.Kind.values()) {
            register(kind.gene(), GeneTypes.Strength.WAW, GeneTypes.Region.CARDIOPULMONARY, Set.of(GeneCategories.DRAGON), quality -> List.of(
                    Component.translatable("gene.yixiangshouronglu." + kind.gene().getPath() + ".description", 4 + 2 * quality.ordinal()),
                    Component.translatable("gene.yixiangshouronglu.dragon_gland.consumption", Component.translatable(kind.bloodNameKey()))));
            GeneSkills.register(new GeneSkills.Skill(kind.skill(), kind.gene(), kind.skillNameKey(), () -> 0,
                    player -> GeneDragonBreathBeam.start(player, kind), false));
        }
        register(INFERNAL_CORE, GeneTypes.Strength.WAW, GeneTypes.Region.CARDIOPULMONARY, Set.of(GeneCategories.ELEMENTAL), quality ->
                List.of(Component.translatable("gene.yixiangshouronglu.effect.attack", 4 * (quality.ordinal() + 1))));
        register(DESERT_MARROW, GeneTypes.Strength.WAW, GeneTypes.Region.SKELETAL, Set.of(GeneCategories.UNDEAD), quality ->
                List.of(Component.translatable("gene.yixiangshouronglu.desert_marrow.description", 10 + 8 * quality.ordinal())));
        register(WITHERED_MECHANICAL_BRAIN, GeneTypes.Strength.ALEPH, GeneTypes.Region.NERVOUS, Set.of(GeneCategories.UNDEAD), quality ->
                List.of(Component.translatable("gene.yixiangshouronglu.withered_mechanical_brain.description", 15 * (quality.ordinal() + 1)),
                        Component.translatable("gene.yixiangshouronglu.withered_mechanical_brain.draw_speed", 10 * (quality.ordinal() + 1))));
        register(SCULK_VOCAL_CORDS, GeneTypes.Strength.WAW, GeneTypes.Region.NERVOUS, Set.of(), quality -> List.of(
                Component.translatable("gene.yixiangshouronglu.sculk_vocal_cords.description", 12 + 6 * quality.ordinal()),
                Component.translatable("gene.yixiangshouronglu.sculk_vocal_cords.cooldown", 45)));
        register(UNDYING_ADAPTIVE_CELLS, GeneTypes.Strength.ALEPH, GeneTypes.Region.MUSCLE, Set.of(), quality -> List.of(
                Component.translatable("gene.yixiangshouronglu.wither_core.life_steal", 10 * (quality.ordinal() + 1)),
                Component.translatable("gene.yixiangshouronglu.wither_core.overheal", Math.round(overheal(quality) * 100)),
                Component.translatable("gene.yixiangshouronglu.undying_adaptive_cells.description", 4 * (quality.ordinal() + 1))));
        GeneSkills.register(new GeneSkills.Skill(SONIC_SHRIEK, SCULK_VOCAL_CORDS, "gene.yixiangshouronglu.skill.sonic_shriek", () -> 900,
                HighTierBossGenes::sonicShriek));
    }

    private static void register(ResourceLocation id, GeneTypes.Strength strength, GeneTypes.Region region,
                                 Set<ResourceLocation> categories, Function<GeneTypes.Quality, List<Component>> descriptions) {
        String name = "gene.yixiangshouronglu." + id.getPath();
        GeneDefinitions.register(new GeneDefinition(id, name, name + ".description", strength, region, 6, Set.of(), categories,
                new GeneDefinition.Effect() {
                    @Override public List<Component> descriptions(GeneTypes.Quality quality) { return descriptions.apply(quality); }
                    @Override public void removed(ServerPlayer player, ItemStack module) { GeneDragonBreathBeam.stop(player, id); }
                }));
    }

    public static double overheal(GeneTypes.Quality quality) {
        return switch (quality) {
            case ALPHA -> 0.10;
            case BETA -> 0.25;
            case GAMMA -> 0.50;
        };
    }

    public static void refresh(ServerPlayer player, Map<ResourceLocation, ItemStack> installed) {
        int core = BossGenes.tier(installed, INFERNAL_CORE);
        int marrow = BossGenes.tier(installed, DESERT_MARROW);
        int cells = BossGenes.tier(installed, UNDYING_ADAPTIVE_CELLS);
        int brain = BossGenes.tier(installed, WITHERED_MECHANICAL_BRAIN);
        apply(player, INFERNAL_CORE, ATTACK, 4 * core);
        apply(player, DESERT_MARROW, ARMOR_SHRED, marrow == 0 ? 0 : (2 + 8 * marrow) / 100.0);
        apply(player, WITHERED_MECHANICAL_BRAIN, DRAW_SPEED, 0.10 * brain);
        apply(player, UNDYING_ADAPTIVE_CELLS, DragonGenes.LIFE_STEAL, 0.10 * cells);
        apply(player, UNDYING_ADAPTIVE_CELLS, RegenerativeCells.OVERHEAL,
                cells == 0 ? 0 : overheal(GeneModuleItem.read(installed.get(UNDYING_ADAPTIVE_CELLS)).quality()));
        GeneDragonBreathBeam.validate(player);
    }

    private static void apply(ServerPlayer player, ResourceLocation gene, ResourceLocation attribute, double amount) {
        GeneEffects.reconcile(player, attribute, GeneDefinitions.get(gene).effectId(attribute.toString()), amount, AttributeModifier.Operation.ADDITION);
    }

    public static DamageSource source(ServerPlayer player, Entity direct, ResourceKey<DamageType> type) {
        return new SkillDamage(player, direct, type);
    }

    public static boolean canTarget(ServerPlayer player, LivingEntity target) {
        return BlazingDomain.canTarget(player, target) && !target.isInvulnerable()
                && (target instanceof ServerPlayer || !player.isAlliedTo(target) && !target.isAlliedTo(player));
    }

    public static Vec3 loadedEnd(Level level, Vec3 start, Vec3 end) {
        if (!level.hasChunkAt(BlockPos.containing(start))) { return start; }
        BlockPos unloaded = BlockGetter.traverseBlocks(start, end, level,
                (world, pos) -> world.hasChunkAt(pos) ? null : pos.immutable(), world -> null);
        if (unloaded == null) { return end; }
        Vec3 entry = new AABB(unloaded).clip(start, end).orElse(start);
        if (entry.distanceToSqr(start) < 1.0E-6) { return start; }
        return entry.subtract(end.subtract(start).normalize().scale(0.001));
    }

    public static Vec3 clippedEnd(ServerPlayer player, Vec3 start, Vec3 end) {
        Vec3 loaded = loadedEnd(player.level(), start, end);
        return player.level().clip(new ClipContext(start, loaded, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
    }

    public static boolean unobstructed(ServerPlayer player, Vec3 start, Vec3 end) {
        return clippedEnd(player, start, end).distanceToSqr(end) < 1.0E-6;
    }

    public static List<LivingEntity> targets(ServerPlayer player, Vec3 end, double radius, boolean blocked) {
        Vec3 start = player.getEyePosition();
        List<LivingEntity> result = new ArrayList<>();
        for (var target : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(radius),
                candidate -> canTarget(player, candidate))) {
            var bounds = target.getBoundingBox().inflate(radius);
            var hit = bounds.contains(start) ? java.util.Optional.of(start) : bounds.clip(start, end);
            if (hit.isPresent() && (!blocked || unobstructed(player, start, hit.get()))) { result.add(target); }
        }
        result.sort(Comparator.comparingDouble((LivingEntity target) -> target.getBoundingBox().inflate(radius)
                .clip(start, end).orElse(start).distanceToSqr(start)).thenComparingInt(Entity::getId));
        return result;
    }

    private static String sonicShriek(ServerPlayer player) {
        ItemStack module = GeneEffects.installed(player).get(SCULK_VOCAL_CORDS);
        if (module == null) { return "not_installed"; }
        Vec3 start = player.getEyePosition();
        Vec3 end = loadedEnd(player.level(), start, start.add(player.getLookAngle().scale(15)));
        if (start.distanceToSqr(end) < 1.0E-6) { return "invalid_state"; }
        DamageSource source = source(player, player, DamageTypes.SONIC_BOOM);
        float damage = 12 + 6 * GeneModuleItem.read(module).quality().ordinal();
        for (LivingEntity target : targets(player, end, 1, false)) {
            if (target.hurt(source, damage)) { target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0), player); }
        }
        Vec3 direction = end.subtract(start).normalize();
        for (double distance = 0; distance <= start.distanceTo(end); distance++) {
            Vec3 point = start.add(direction.scale(distance));
            player.serverLevel().sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3, 1);
        return "ok";
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damaged(LivingDamageEvent event) {
        if (PhantomState.isFollowUp(event.getSource())) { return; }
        if (!event.isCanceled() && event.getAmount() > 0 && event.getEntity() instanceof ServerPlayer player
                && GeneEffects.installed(player).containsKey(UNDYING_ADAPTIVE_CELLS)) {
            REGEN_HITS.add(new RegenHit(event, player, player.level()));
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || REGEN_HITS.isEmpty()) { return; }
        List<RegenHit> hits = List.copyOf(REGEN_HITS);
        REGEN_HITS.clear();
        for (var hit : hits) {
            ServerPlayer player = hit.player();
            if (hit.event().isCanceled() || hit.event().getAmount() <= 0 || !player.isAlive() || player.isRemoved()
                    || player.hasDisconnected() || player.level() != hit.level()) { continue; }
            ItemStack module = GeneEffects.installed(player).get(UNDYING_ADAPTIVE_CELLS);
            if (module == null || player.getRandom().nextInt(100) >= 4 * (GeneModuleItem.read(module).quality().ordinal() + 1)) { continue; }
            var current = player.getEffect(MobEffects.REGENERATION);
            if (current == null || current.getAmplifier() <= 3) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 3));
            }
        }
    }

    private static void clear(ServerPlayer player) {
        REGEN_HITS.removeIf(hit -> hit.player() == player);
        GeneDragonBreathBeam.stop(player);
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { clear(player); }
    }

    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { clear(player); }
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        REGEN_HITS.clear();
        GeneDragonBreathBeam.stopAll();
    }
}

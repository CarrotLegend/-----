package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.entity.GeneStormLightning;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class BossGenes {
    public static final ResourceLocation STORM_NERVES = id("storm_nerves");
    public static final ResourceLocation LEVIATHAN_GILLS = id("leviathan_gills");
    public static final ResourceLocation VOID_SPINE = id("void_spine");
    public static final ResourceLocation SAPHYRA_MARROW = id("saphyra_marrow");
    public static final ResourceLocation TENEBRITE_MUSCLES = id("tenebrite_muscles");
    public static final ResourceLocation ACTIVE_INK_SAC = id("active_ink_sac");
    private static final ResourceLocation MOVEMENT = new ResourceLocation("minecraft", "generic.movement_speed");
    private static final ResourceLocation DODGE = new ResourceLocation("attributeslib", "dodge_chance");
    private static final ResourceLocation INKED = new ResourceLocation("theinkarena", "inked");
    private static final String MARROW_READY_AT = "SaphyraMarrowReadyAt";
    private static final List<PendingHit> HITS = new ArrayList<>();

    private record PendingHit(LivingDamageEvent event, ServerPlayer attacker, Level level,
                              boolean resistance, boolean storm, boolean ink) {}

    private BossGenes() {}

    private static ResourceLocation id(String path) {
        return new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, path);
    }

    public static void register() {
        register(STORM_NERVES, GeneTypes.Region.NERVOUS, 4, Set.of(GeneCategories.ELEMENTAL), quality ->
                List.of(description(STORM_NERVES, 10)));
        register(LEVIATHAN_GILLS, GeneTypes.Region.CARDIOPULMONARY, 4, Set.of(), quality -> List.of(
                Component.translatable("gene.yixiangshouronglu.leviathan_gills.swim", 15 + 10 * quality.ordinal()),
                description(LEVIATHAN_GILLS, 15 * (quality.ordinal() + 1))));
        register(VOID_SPINE, GeneTypes.Region.SKELETAL, 4, Set.of(), quality -> List.of(
                description(VOID_SPINE, 8 + 4 * quality.ordinal()),
                Component.translatable("gene.yixiangshouronglu.void_spine.resistance", 10, 5)));
        register(SAPHYRA_MARROW, GeneTypes.Region.NERVOUS, 8, Set.of(), quality ->
                List.of(description(SAPHYRA_MARROW, marrowCooldown(quality) / 20)));
        register(TENEBRITE_MUSCLES, GeneTypes.Region.MUSCLE, 7, Set.of(), quality -> List.of(
                description(TENEBRITE_MUSCLES, 10 * (quality.ordinal() + 1)),
                Component.translatable("gene.yixiangshouronglu.vine_muscles.speed", 10)));
        register(ACTIVE_INK_SAC, GeneTypes.Region.MUSCLE, 2, Set.of(), quality -> List.of(
                Component.translatable("gene.yixiangshouronglu.active_ink_sac.dodge", 2 * (quality.ordinal() + 1)),
                description(ACTIVE_INK_SAC, 5, 6)));
    }

    private static Component description(ResourceLocation gene, Object... arguments) {
        return Component.translatable("gene.yixiangshouronglu." + gene.getPath() + ".description", arguments);
    }

    private static void register(ResourceLocation id, GeneTypes.Region region, int cost, Set<ResourceLocation> categories,
                                 Function<GeneTypes.Quality, List<Component>> descriptions) {
        String name = "gene.yixiangshouronglu." + id.getPath();
        GeneDefinitions.register(new GeneDefinition(id, name, name + ".description", GeneTypes.Strength.HE,
                region, cost, Set.of(), categories, new GeneDefinition.Effect() {
                    @Override public List<Component> descriptions(GeneTypes.Quality quality) { return descriptions.apply(quality); }
                }));
    }

    public static void refresh(ServerPlayer player, Map<ResourceLocation, ItemStack> installed) {
        int gills = tier(installed, LEVIATHAN_GILLS);
        int muscles = tier(installed, TENEBRITE_MUSCLES);
        int ink = tier(installed, ACTIVE_INK_SAC);
        apply(player, LEVIATHAN_GILLS, ForgeRegistries.ATTRIBUTES.getKey(ForgeMod.SWIM_SPEED.get()),
                gills == 0 ? 0 : (5 + 10 * gills) / 100.0, AttributeModifier.Operation.MULTIPLY_BASE);
        apply(player, TENEBRITE_MUSCLES, MOVEMENT, muscles == 0 ? 0 : 0.10, AttributeModifier.Operation.MULTIPLY_BASE);
        apply(player, ACTIVE_INK_SAC, DODGE, 0.02 * ink, AttributeModifier.Operation.ADDITION);
    }

    private static void apply(ServerPlayer player, ResourceLocation gene, ResourceLocation attribute, double amount,
                              AttributeModifier.Operation operation) {
        GeneEffects.reconcile(player, attribute, GeneDefinitions.get(gene).effectId(attribute.toString()), amount, operation);
    }

    static int tier(Map<ResourceLocation, ItemStack> installed, ResourceLocation gene) {
        ItemStack stack = installed.get(gene);
        return stack == null ? 0 : GeneModuleItem.read(stack).quality().ordinal() + 1;
    }

    public static int marrowCooldown(GeneTypes.Quality quality) {
        return 4800 - 1200 * quality.ordinal();
    }

    static ServerPlayer attacker(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player) { return player; }
        if (source.getEntity() == null && source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof ServerPlayer player) { return player; }
        return null;
    }

    static double outgoingMultiplier(ServerPlayer player) {
        var installed = GeneEffects.installed(player);
        double multiplier = 1 + 0.10 * tier(installed, TENEBRITE_MUSCLES);
        if (player.isInWater()) { multiplier *= 1 + 0.15 * tier(installed, LEVIATHAN_GILLS); }
        return multiplier;
    }

    static double incomingMultiplier(ServerPlayer player) {
        int spine = tier(GeneEffects.installed(player), VOID_SPINE);
        return spine == 0 ? 1 : 1 - (4 + 4 * spine) / 100.0;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damaged(LivingDamageEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0 || event.getEntity().level().isClientSide) { return; }
        ServerPlayer attacker = attacker(event.getSource());
        boolean resistance = event.getEntity() instanceof ServerPlayer victim && GeneEffects.installed(victim).containsKey(VOID_SPINE);
        var installed = attacker == null ? Map.<ResourceLocation, ItemStack>of() : GeneEffects.installed(attacker);
        boolean secondary = event.getSource().getDirectEntity() instanceof GeneStormLightning
                || event.getSource() instanceof HighTierBossGenes.SkillDamage
                || event.getSource() instanceof GeneMeleeEvents.BonusFreezeDamage;
        boolean storm = !secondary && installed.containsKey(STORM_NERVES);
        boolean ink = installed.containsKey(ACTIVE_INK_SAC);
        if (resistance || storm || ink) {
            HITS.add(new PendingHit(event, attacker, event.getEntity().level(), resistance, storm, ink));
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || HITS.isEmpty()) { return; }
        List<PendingHit> hits = List.copyOf(HITS);
        HITS.clear();
        for (PendingHit hit : hits) {
            var damage = hit.event();
            LivingEntity target = damage.getEntity();
            if (damage.isCanceled() || damage.getAmount() <= 0 || target.level() != hit.level()) { continue; }
            if (hit.resistance() && target instanceof ServerPlayer victim && valid(victim, hit.level())
                    && GeneEffects.installed(victim).containsKey(VOID_SPINE) && victim.getRandom().nextInt(100) < 10) {
                var current = victim.getEffect(MobEffects.DAMAGE_RESISTANCE);
                if (current == null || current.getAmplifier() == 0) {
                    victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
                }
            }
            ServerPlayer player = hit.attacker();
            if (!valid(player, hit.level()) || target == player || target.isRemoved()) { continue; }
            var installed = GeneEffects.installed(player);
            if (hit.storm() && installed.containsKey(STORM_NERVES) && player.getRandom().nextInt(100) < 10) {
                GeneStormLightning.strike(player, target);
            }
            if (hit.ink() && target.isAlive() && installed.containsKey(ACTIVE_INK_SAC) && player.getRandom().nextInt(100) < 5
                    && ForgeRegistries.MOB_EFFECTS.containsKey(INKED)) {
                var inked = ForgeRegistries.MOB_EFFECTS.getValue(INKED);
                if (inked != null) { target.addEffect(new MobEffectInstance(inked, 120, 0), player); }
            }
        }
    }

    private static boolean valid(ServerPlayer player, Level level) {
        return player != null && player.isAlive() && !player.isRemoved() && !player.hasDisconnected()
                && !player.isSpectator() && player.level() == level && !player.isChangingDimension();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void dying(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) { return; }
        ItemStack module = GeneEffects.installed(player).get(SAPHYRA_MARROW);
        if (module == null || player.isRemoved() || player.hasDisconnected()) { return; }
        var runtime = GeneCapability.get(player).effectRuntime();
        long now = player.server.overworld().getGameTime();
        if (now < runtime.getLong(MARROW_READY_AT)) { return; }
        runtime.putLong(MARROW_READY_AT, now + marrowCooldown(GeneModuleItem.read(module).quality()));
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
    }

    private static void clear(ServerPlayer player) {
        HITS.removeIf(hit -> hit.attacker() == player || hit.event().getEntity() == player);
        for (var level : player.server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof GeneStormLightning lightning && lightning.getCaster() == player) { lightning.discard(); }
            }
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { clear(player); }
    }

    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { clear(player); }
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { HITS.clear(); }
}

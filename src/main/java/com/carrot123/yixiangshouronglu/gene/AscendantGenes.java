package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import com.carrot123.yixiangshouronglu.network.GeneNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class AscendantGenes {
    public static final ResourceLocation IGNIS_HEART = id("ignis_heart");
    public static final ResourceLocation CURSED_WING_BONES = id("cursed_wing_bones");
    public static final ResourceLocation PHANTOM_NERVES = id("phantom_nerves");
    public static final List<ResourceLocation> MODULES = List.of(IGNIS_HEART, CURSED_WING_BONES, PHANTOM_NERVES);
    private static final ResourceLocation BRAND = new ResourceLocation("cataclysm", "blazing_brand");
    private static final Map<UUID, PrimaryAttack> ATTACKS = new HashMap<>();
    private static final Map<UUID, Rage> RAGE = new HashMap<>();
    private static final List<Hit> HITS = new ArrayList<>();
    private static final Set<LivingHurtEvent> HURT = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<LivingDamageEvent> FINAL = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<LivingDamageEvent> DISCARDED_RAGE = Collections.newSetFromMap(new IdentityHashMap<>());

    private static final class PrimaryAttack {
        final AttackEntityEvent event;
        final long tick;
        final Level level;
        boolean claimed;
        PrimaryAttack(AttackEntityEvent event, ServerPlayer player) {
            this.event = event; tick = now(player); level = player.level();
        }
    }

    private record Rage(ServerPlayer player, int stacks, long expires) {}
    private record Hit(LivingDamageEvent event, Level level, ServerPlayer attacker, boolean brand, boolean rage,
                       PhantomState.State followUp, ServerPlayer defended, PhantomState.State defense,
                       GeneNetwork.PhantomEcho echo) {}

    private AscendantGenes() {}
    private static ResourceLocation id(String path) { return new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, path); }
    static long now(ServerPlayer player) { return player.server.overworld().getGameTime(); }

    public static void register() {
        register(IGNIS_HEART, GeneTypes.Region.CARDIOPULMONARY, 7, Set.of(GeneCategories.ELEMENTAL), quality -> List.of(
                Component.translatable("gene.yixiangshouronglu.ignis_heart.brand", 25 * (quality.ordinal() + 1)),
                Component.translatable("gene.yixiangshouronglu.ignis_heart.damage", 15 + 10 * quality.ordinal())));
        register(CURSED_WING_BONES, GeneTypes.Region.SKELETAL, 6, Set.of(GeneCategories.UNDEAD), quality -> List.of(
                Component.translatable("gene.yixiangshouronglu.cursed_wing_bones.description", 5 + 2 * quality.ordinal(), 5, 6)));
        register(PHANTOM_NERVES, GeneTypes.Region.NERVOUS, 8, Set.of(), quality -> List.of(
                Component.translatable("gene.yixiangshouronglu.phantom_nerves.dodge", 5 * (quality.ordinal() + 1)),
                Component.translatable("gene.yixiangshouronglu.phantom_nerves.state", 30),
                Component.translatable("gene.yixiangshouronglu.phantom_nerves.defense", 25, 50),
                Component.translatable("gene.yixiangshouronglu.phantom_nerves.pursuit", 25, 50),
                Component.translatable("gene.yixiangshouronglu.phantom_nerves.cooldown", 240)));
        GeneSkills.register(new GeneSkills.Skill(PhantomState.SKILL, PHANTOM_NERVES,
                "gene.yixiangshouronglu.skill.phantom_state", () -> 4800, PhantomState::start));
    }

    private static void register(ResourceLocation id, GeneTypes.Region region, int cost, Set<ResourceLocation> categories,
                                 Function<GeneTypes.Quality, List<Component>> descriptions) {
        String name = "gene.yixiangshouronglu." + id.getPath();
        GeneDefinitions.register(new GeneDefinition(id, name, name + ".description", GeneTypes.Strength.ALEPH, region, cost,
                Set.of(), categories, new GeneDefinition.Effect() {
                    @Override public List<Component> descriptions(GeneTypes.Quality quality) { return descriptions.apply(quality); }
                    @Override public void removed(ServerPlayer player, ItemStack module) {
                        if (id.equals(CURSED_WING_BONES)) { clearRage(player); }
                        if (id.equals(PHANTOM_NERVES)) { PhantomState.stop(player); }
                    }
                }));
    }

    public static void refresh(ServerPlayer player, Map<ResourceLocation, ItemStack> installed) {
        int tier = BossGenes.tier(installed, PHANTOM_NERVES);
        GeneEffects.reconcile(player, AdditionalGenes.DODGE_CHANCE, GeneDefinitions.get(PHANTOM_NERVES).effectId("dodge"),
                0.05 * tier, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION);
        if (!installed.containsKey(CURSED_WING_BONES)) { clearRage(player); }
        if (tier == 0) { PhantomState.stop(player); }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void attack(AttackEntityEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player) {
            ATTACKS.put(player.getUUID(), new PrimaryAttack(event, player));
        }
    }

    private static PrimaryAttack primary(ServerPlayer player, LivingEntity target, DamageSource source) {
        PrimaryAttack attack = ATTACKS.get(player.getUUID());
        return attack != null && !attack.claimed && !attack.event.isCanceled() && attack.tick == now(player)
                && attack.level == player.level() && attack.event.getTarget() == target
                && source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == player ? attack : null;
    }

    private static int stacks(ServerPlayer player) {
        Rage rage = RAGE.get(player.getUUID());
        if (rage == null) { return 0; }
        if (rage.player() != player || now(player) >= rage.expires()) { RAGE.remove(player.getUUID()); return 0; }
        return rage.stacks();
    }

    private static int pendingStacks(ServerPlayer player) {
        int stacks = stacks(player);
        for (Hit hit : HITS) {
            if (hit.rage() && hit.attacker() == player && hit.level() == player.level() && !DISCARDED_RAGE.contains(hit.event())
                    && !hit.event().isCanceled() && hit.event().getAmount() > 0) { stacks = Math.min(5, stacks + 1); }
        }
        return stacks;
    }

    private static void clearRage(ServerPlayer player) {
        RAGE.remove(player.getUUID());
        for (Hit hit : HITS) { if (hit.rage() && hit.attacker() == player) { DISCARDED_RAGE.add(hit.event()); } }
    }

    static double outgoingMultiplier(LivingHurtEvent event, ServerPlayer player) {
        if (event.getEntity() == player || !HURT.add(event)) { return 1; }
        var installed = GeneEffects.installed(player);
        int ignis = BossGenes.tier(installed, IGNIS_HEART);
        double multiplier = ignis == 0 ? 1 : 1 + (5 + 10 * ignis) / 100.0;
        int bones = BossGenes.tier(installed, CURSED_WING_BONES);
        if (bones > 0 && primary(player, event.getEntity(), event.getSource()) != null) {
            multiplier *= 1 + pendingStacks(player) * (3 + 2 * bones) / 100.0;
        }
        return multiplier;
    }

    static void finalDamage(LivingDamageEvent event) {
        if (!FINAL.add(event)) { return; }
        ServerPlayer defended = event.getEntity() instanceof ServerPlayer player ? player : null;
        PhantomState.State defense = defended == null ? null : PhantomState.current(defended);
        GeneNetwork.PhantomEcho echo = null;
        if (defense != null && defended.getRandom().nextInt(100) < 25) {
            event.setAmount(event.getAmount() * 0.5F);
            echo = GeneNetwork.PhantomEcho.capture(defended);
        }
        ServerPlayer attacker = BossGenes.attacker(event.getSource());
        boolean brand = false;
        boolean rage = false;
        PhantomState.State followUp = null;
        if (attacker != null && attacker != event.getEntity()) {
            var installed = GeneEffects.installed(attacker);
            PrimaryAttack primary = primary(attacker, event.getEntity(), event.getSource());
            rage = primary != null && installed.containsKey(CURSED_WING_BONES);
            if (primary != null) { primary.claimed = true; }
            boolean secondary = event.getSource() instanceof GeneMeleeEvents.BonusFreezeDamage
                    || event.getSource().getDirectEntity() instanceof com.carrot123.yixiangshouronglu.entity.GeneStormLightning;
            brand = !secondary && installed.containsKey(IGNIS_HEART);
            if (primary != null || GeneDamageEvents.isRanged(event.getSource())) { followUp = PhantomState.current(attacker); }
        }
        if (brand || rage || followUp != null || echo != null) {
            HITS.add(new Hit(event, event.getEntity().level(), attacker, brand, rage, followUp, defended, defense, echo));
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) { return; }
        List<Hit> hits = List.copyOf(HITS);
        HITS.clear();
        ATTACKS.clear();
        for (Hit hit : hits) {
            var damage = hit.event();
            LivingEntity target = damage.getEntity();
            if (damage.isCanceled() || damage.getAmount() <= 0 || target.level() != hit.level()) { continue; }
            if (hit.echo() != null && PhantomState.current(hit.defended()) == hit.defense()) { GeneNetwork.phantomEcho(hit.defended(), hit.echo()); }
            ServerPlayer player = hit.attacker();
            if (!valid(player, hit.level())) { continue; }
            var installed = GeneEffects.installed(player);
            if (hit.rage() && !DISCARDED_RAGE.contains(damage) && installed.containsKey(CURSED_WING_BONES)) {
                RAGE.put(player.getUUID(), new Rage(player, Math.min(5, stacks(player) + 1), now(player) + 120));
            }
            if (target.isRemoved()) { continue; }
            int tier = BossGenes.tier(installed, IGNIS_HEART);
            if (hit.brand() && tier > 0 && HighTierBossGenes.canTarget(player, target) && player.getRandom().nextInt(100) < 25 * tier) { brand(player, target); }
            if (hit.followUp() != null && PhantomState.current(player) == hit.followUp()
                    && HighTierBossGenes.canTarget(player, target) && player.getRandom().nextInt(100) < 25) {
                target.hurt(PhantomState.followUpSource(player), damage.getAmount() * 0.5F);
            }
        }
        RAGE.values().removeIf(rage -> !valid(rage.player(), rage.player().level()) || now(rage.player()) >= rage.expires());
        HURT.clear();
        FINAL.clear();
        DISCARDED_RAGE.clear();
    }

    private static void brand(ServerPlayer player, LivingEntity target) {
        if (!ForgeRegistries.MOB_EFFECTS.containsKey(BRAND)) { return; }
        var effect = ForgeRegistries.MOB_EFFECTS.getValue(BRAND);
        if (effect == null) { return; }
        var current = target.getEffect(effect);
        if (current == null || current.getAmplifier() == 0 && !current.isInfiniteDuration() && current.getDuration() < 120) {
            target.addEffect(new MobEffectInstance(effect, 120, 0), player);
        }
    }

    static boolean valid(ServerPlayer player, Level level) {
        return player != null && player.isAlive() && !player.isRemoved() && !player.hasDisconnected()
                && !player.isSpectator() && !player.isChangingDimension() && player.level() == level;
    }

    private static void clear(ServerPlayer player) {
        ATTACKS.remove(player.getUUID());
        clearRage(player);
        HITS.removeIf(hit -> hit.attacker() == player || hit.defended() == player);
        PhantomState.stop(player);
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { if (event.getEntity() instanceof ServerPlayer player) { clear(player); } }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) { clear(player); } }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { if (event.getEntity() instanceof ServerPlayer player) { clear(player); } }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { if (event.getEntity() instanceof ServerPlayer player) { clear(player); } }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        ATTACKS.clear(); RAGE.clear(); HITS.clear(); HURT.clear(); FINAL.clear(); DISCARDED_RAGE.clear(); PhantomState.clear();
    }
}

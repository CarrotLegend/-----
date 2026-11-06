package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.entity.GeneDragonFireball;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class DragonGenes {
    public static final ResourceLocation WITHER_CORE = id("wither_core");
    public static final ResourceLocation ENDER_DRAGON_BRAIN = id("ender_dragon_brain");
    public static final ResourceLocation BERSERKER_TENDONS = id("berserker_tendons");
    public static final ResourceLocation VOID_CARAPACE = id("void_carapace");
    public static final ResourceLocation DRAGON_BREATH = id("dragon_breath");
    private static final ResourceLocation ATTACK = new ResourceLocation("minecraft", "generic.attack_damage");
    private static final ResourceLocation HEALTH = new ResourceLocation("minecraft", "generic.max_health");
    private static final ResourceLocation ARMOR = new ResourceLocation("minecraft", "generic.armor");
    public static final ResourceLocation LIFE_STEAL = new ResourceLocation("attributeslib", "life_steal");
    private static final Set<LivingDamageEvent> BERSERKER_HITS = new LinkedHashSet<>();

    private DragonGenes() {}

    private static ResourceLocation id(String path) {
        return new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, path);
    }

    public static int berserkerChance(GeneTypes.Quality quality) {
        return switch (quality) {
            case ALPHA -> 13;
            case BETA -> 36;
            case GAMMA -> 39;
        };
    }

    public static void register() {
        GeneCategories.register(new GeneCategories.Category(GeneCategories.DRAGON, "gene.yixiangshouronglu.dragon.name", List.of(
                new GeneCategories.Bonus(HEALTH, 40, "gene.yixiangshouronglu.effect.health"),
                new GeneCategories.Bonus(ATTACK, 8, "gene.yixiangshouronglu.effect.attack")), false, List.of(), 1));
        register(WITHER_CORE, GeneTypes.Region.CARDIOPULMONARY, 6, GeneCategories.UNDEAD, quality -> {
            int tier = quality.ordinal() + 1;
            return List.of(Component.translatable("gene.yixiangshouronglu.effect.attack", 2 * tier),
                    Component.translatable("gene.yixiangshouronglu.wither_core.life_steal", 10 * tier),
                    Component.translatable("gene.yixiangshouronglu.wither_core.overheal", 5 * tier));
        });
        register(ENDER_DRAGON_BRAIN, GeneTypes.Region.NERVOUS, 8, GeneCategories.DRAGON, quality -> List.of(
                Component.translatable("gene.yixiangshouronglu.effect.health", 15 * (quality.ordinal() + 1)),
                Component.translatable("gene.yixiangshouronglu.ender_dragon_brain.description")));
        register(BERSERKER_TENDONS, GeneTypes.Region.MUSCLE, 5, GeneCategories.MUTANT, quality -> List.of(
                Component.translatable("gene.yixiangshouronglu.effect.attack", 3 * (quality.ordinal() + 1)),
                Component.translatable("gene.yixiangshouronglu.berserker_tendons.description", berserkerChance(quality))));
        register(VOID_CARAPACE, GeneTypes.Region.SKELETAL, 4, GeneCategories.MUTANT, quality -> List.of(
                Component.translatable("gene.yixiangshouronglu.effect.armor", 3 + 2 * quality.ordinal()),
                Component.translatable("gene.yixiangshouronglu.void_carapace.description")));
        GeneSkills.register(new GeneSkills.Skill(DRAGON_BREATH, ENDER_DRAGON_BRAIN, "gene.yixiangshouronglu.skill.dragon_breath",
                () -> GeneSkillConfig.DRAGON_BREATH_COOLDOWN.get(), GeneDragonFireball::fire));
    }

    private static void register(ResourceLocation id, GeneTypes.Region region, int cost, ResourceLocation category,
                                 Function<GeneTypes.Quality, List<Component>> descriptions) {
        String name = "gene.yixiangshouronglu." + id.getPath();
        GeneDefinitions.register(new GeneDefinition(id, name, name + ".description", GeneTypes.Strength.TETH, region, cost,
                Set.of(), Set.of(category), new GeneDefinition.Effect() {
                    @Override public List<Component> descriptions(GeneTypes.Quality quality) { return descriptions.apply(quality); }
                }));
    }

    public static void refresh(ServerPlayer player, Map<ResourceLocation, ItemStack> installed) {
        int core = tier(installed, WITHER_CORE);
        int brain = tier(installed, ENDER_DRAGON_BRAIN);
        int tendons = tier(installed, BERSERKER_TENDONS);
        int carapace = tier(installed, VOID_CARAPACE);
        apply(player, WITHER_CORE, ATTACK, 2 * core);
        apply(player, WITHER_CORE, LIFE_STEAL, 0.10 * core);
        apply(player, WITHER_CORE, RegenerativeCells.OVERHEAL, 0.05 * core);
        apply(player, ENDER_DRAGON_BRAIN, HEALTH, 15 * brain);
        apply(player, BERSERKER_TENDONS, ATTACK, 3 * tendons);
        apply(player, VOID_CARAPACE, ARMOR, carapace == 0 ? 0 : 1 + 2 * carapace);
        if (carapace > 0) { player.removeEffect(MobEffects.LEVITATION); }
    }

    private static int tier(Map<ResourceLocation, ItemStack> installed, ResourceLocation gene) {
        ItemStack stack = installed.get(gene);
        return stack == null ? 0 : GeneModuleItem.read(stack).quality().ordinal() + 1;
    }

    private static void apply(ServerPlayer player, ResourceLocation gene, ResourceLocation attribute, double amount) {
        GeneEffects.reconcile(player, attribute, GeneDefinitions.get(gene).effectId(attribute.toString()), amount, AttributeModifier.Operation.ADDITION);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damaged(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0 && !event.isCanceled()
                && !(event.getSource() instanceof GeneMeleeEvents.BonusFreezeDamage)
                && GeneEffects.installed(player).containsKey(BERSERKER_TENDONS)) {
            BERSERKER_HITS.add(event);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || BERSERKER_HITS.isEmpty()) { return; }
        List<LivingDamageEvent> hits = List.copyOf(BERSERKER_HITS);
        BERSERKER_HITS.clear();
        for (var hit : hits) {
            ServerPlayer player = (ServerPlayer) hit.getEntity();
            if (hit.isCanceled() || hit.getAmount() <= 0 || !player.isAlive() || player.hasDisconnected()) { continue; }
            ItemStack module = GeneEffects.installed(player).get(BERSERKER_TENDONS);
            if (module == null || player.getRandom().nextInt(100) >= berserkerChance(GeneModuleItem.read(module).quality())) { continue; }
            var current = player.getEffect(MobEffects.DAMAGE_BOOST);
            if (current == null || current.getAmplifier() < 1) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, GeneSkillConfig.BERSERKER_STRENGTH_DURATION.get(), 1));
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { BERSERKER_HITS.clear(); }
}

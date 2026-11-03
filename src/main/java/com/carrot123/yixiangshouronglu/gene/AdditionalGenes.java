package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.util.List;
import java.util.Set;

public final class AdditionalGenes {
    public static final ResourceLocation SKELETAL_RETINA = id("skeletal_retina");
    public static final ResourceLocation BLAST_SKELETON = id("blast_skeleton");
    public static final ResourceLocation ENDER_SYNAPSE = id("ender_synapse");
    public static final ResourceLocation ARROW_DAMAGE = new ResourceLocation("attributeslib", "arrow_damage");
    public static final ResourceLocation DODGE_CHANCE = new ResourceLocation("attributeslib", "dodge_chance");
    private AdditionalGenes() {}
    private static ResourceLocation id(String path) { return new ResourceLocation("yixiangshouronglu", path); }
    public static double arrow(GeneTypes.Quality quality) { return switch (quality) { case ALPHA -> 0.10; case BETA -> 0.15; case GAMMA -> 0.25; }; }
    public static double dodge(GeneTypes.Quality quality) { return (quality.ordinal() + 1) * 0.02; }
    public static double explosionReduction(GeneTypes.Quality quality) { return (quality.ordinal() + 1) * 0.25; }
    public static void register() {
        register(SKELETAL_RETINA, GeneTypes.Region.VISION, 3, Set.of(GeneCategories.MUTANT, GeneCategories.UNDEAD), new GeneDefinition.Effect() {
            @Override public List<Component> descriptions(GeneTypes.Quality quality) { return List.of(Component.translatable("gene.yixiangshouronglu.skeletal_retina.arrow", Math.round(arrow(quality) * 100))); }
        });
        register(BLAST_SKELETON, GeneTypes.Region.SKELETAL, 3, Set.of(GeneCategories.MUTANT), new GeneDefinition.Effect() {
            @Override public List<Component> descriptions(GeneTypes.Quality quality) { return List.of(Component.translatable("gene.yixiangshouronglu.blast_skeleton.reduction", Math.round(explosionReduction(quality) * 100))); }
            @Override public double explosionReduction(GeneTypes.Quality quality) { return AdditionalGenes.explosionReduction(quality); }
        });
        register(ENDER_SYNAPSE, GeneTypes.Region.NERVOUS, 5, Set.of(GeneCategories.MUTANT), new GeneDefinition.Effect() {
            @Override public List<Component> descriptions(GeneTypes.Quality quality) { return List.of(Component.translatable("gene.yixiangshouronglu.ender_synapse.dodge", Math.round(dodge(quality) * 100)), Component.translatable("gene.yixiangshouronglu.ender_synapse.teleport")); }
        });
        GeneSkills.register(new GeneSkills.Skill(GeneSkills.ENDER_TELEPORT, ENDER_SYNAPSE, "gene.yixiangshouronglu.skill.ender_teleport", () -> GeneSkillConfig.TELEPORT_COOLDOWN.get(), EnderTeleport::execute));
    }
    private static void register(ResourceLocation id, GeneTypes.Region region, int cost, Set<ResourceLocation> categories, GeneDefinition.Effect effect) {
        GeneDefinitions.register(new GeneDefinition(id, "gene.yixiangshouronglu." + id.getPath(), "gene.yixiangshouronglu." + id.getPath() + ".description", GeneTypes.Strength.ZAYIN, region, cost, Set.of(), categories, effect));
    }
    public static void refresh(ServerPlayer player, Map<ResourceLocation, ItemStack> installed) {
        reconcile(player, installed, SKELETAL_RETINA, ARROW_DAMAGE, AttributeModifier.Operation.MULTIPLY_BASE, AdditionalGenes::arrow);
        reconcile(player, installed, ENDER_SYNAPSE, DODGE_CHANCE, AttributeModifier.Operation.ADDITION, AdditionalGenes::dodge);
    }
    private static void reconcile(ServerPlayer player, Map<ResourceLocation, ItemStack> installed, ResourceLocation gene, ResourceLocation attribute, AttributeModifier.Operation operation, ToDoubleFunction<GeneTypes.Quality> amount) {
        var module = installed.get(gene);
        GeneEffects.reconcile(player, attribute, GeneDefinitions.get(gene).effectId(attribute.toString()), module == null ? 0 : amount.applyAsDouble(GeneModuleItem.read(module).quality()), operation);
    }
}

package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ElementalGenes {
    public static final ResourceLocation BLAZING_NERVES = new ResourceLocation("yixiangshouronglu", "blazing_nerves");
    public static final ResourceLocation WITHERED_RIBS = new ResourceLocation("yixiangshouronglu", "withered_ribs");
    public static final ResourceLocation BLAZING_DOMAIN = new ResourceLocation("yixiangshouronglu", "blazing_domain");
    private static final ResourceLocation ARMOR = new ResourceLocation("minecraft", "generic.armor");
    private ElementalGenes() {}
    public static double armor(GeneTypes.Quality quality) { return 3 + quality.ordinal() * 2; }
    public static double witherChance(GeneTypes.Quality quality) { return (quality.ordinal() + 1) * 0.05; }
    public static void register() {
        GeneCategories.register(new GeneCategories.Category(GeneCategories.ELEMENTAL, "gene.yixiangshouronglu.elemental.name", List.of(), false,
                List.of("gene.yixiangshouronglu.elemental.magic")));
        GeneDefinitions.register(new GeneDefinition(BLAZING_NERVES, "gene.yixiangshouronglu.blazing_nerves", "gene.yixiangshouronglu.blazing_nerves.description",
                GeneTypes.Strength.TETH, GeneTypes.Region.NERVOUS, 8, Set.of(), Set.of(GeneCategories.ELEMENTAL, GeneCategories.MUTANT), new GeneDefinition.Effect() {
                    @Override public List<Component> descriptions(GeneTypes.Quality quality) {
                        return List.of(Component.translatable("gene.yixiangshouronglu.blazing_nerves.description"));
                    }
                    @Override public void removed(ServerPlayer player, ItemStack module) { BlazingDomain.stop(player); }
                }));
        GeneDefinitions.register(new GeneDefinition(WITHERED_RIBS, "gene.yixiangshouronglu.withered_ribs", "gene.yixiangshouronglu.withered_ribs.description",
                GeneTypes.Strength.TETH, GeneTypes.Region.SKELETAL, 6, Set.of(), Set.of(GeneCategories.MUTANT, GeneCategories.UNDEAD), new GeneDefinition.Effect() {
                    @Override public List<Component> descriptions(GeneTypes.Quality quality) {
                        return List.of(Component.translatable("gene.yixiangshouronglu.effect.armor", (int) armor(quality)),
                                Component.translatable("gene.yixiangshouronglu.withered_ribs.description", Math.round(witherChance(quality) * 100)));
                    }
                }));
        GeneSkills.register(new GeneSkills.Skill(BLAZING_DOMAIN, BLAZING_NERVES, "gene.yixiangshouronglu.skill.blazing_domain", () -> BlazingDomain.COOLDOWN, BlazingDomain::start));
    }
    public static void refresh(ServerPlayer player, Map<ResourceLocation, ItemStack> installed) {
        var module = installed.get(WITHERED_RIBS);
        GeneEffects.reconcile(player, ARMOR, GeneDefinitions.get(WITHERED_RIBS).effectId(ARMOR.toString()),
                module == null ? 0 : armor(GeneModuleItem.read(module).quality()), AttributeModifier.Operation.ADDITION);
        if (!installed.containsKey(BLAZING_NERVES)) { BlazingDomain.stop(player); }
    }
}

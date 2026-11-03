package com.carrot123.yixiangshouronglu.gene;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Set;

public final class RegenerativeCells implements GeneDefinition.Effect {
    public static final ResourceLocation ID = new ResourceLocation("yixiangshouronglu", "regenerative_cells");
    public static final ResourceLocation OVERHEAL = new ResourceLocation("attributeslib", "overheal");
    public static int interval(GeneTypes.Quality quality) { return switch (quality) { case ALPHA -> 40; case BETA -> 30; case GAMMA -> 20; }; }
    public static double overheal(GeneTypes.Quality quality) { return (quality.ordinal() + 1) * 0.05D; }
    public static void register() {
        GeneCategories.register(new GeneCategories.Category(GeneCategories.MUTANT, "gene.yixiangshouronglu.category.mutant", List.of(
                new GeneCategories.Bonus(new ResourceLocation("minecraft", "generic.max_health"), 20, "gene.yixiangshouronglu.mutant.health"),
                new GeneCategories.Bonus(new ResourceLocation("minecraft", "generic.attack_damage"), 4, "gene.yixiangshouronglu.mutant.attack")), false));
        GeneCategories.register(new GeneCategories.Category(GeneCategories.UNDEAD, "gene.yixiangshouronglu.category.undead", List.of(
                new GeneCategories.Bonus(new ResourceLocation("minecraft", "generic.max_health"), 5, "gene.yixiangshouronglu.undead.health")), true));
        GeneDefinitions.register(new GeneDefinition(ID, "gene.yixiangshouronglu.regenerative_cells", "gene.yixiangshouronglu.regenerative_cells.description",
                GeneTypes.Strength.ZAYIN, GeneTypes.Region.CARDIOPULMONARY, 4, Set.of(), Set.of(GeneCategories.MUTANT, GeneCategories.UNDEAD), new RegenerativeCells()));
    }
    @Override public List<Component> descriptions(GeneTypes.Quality quality) {
        String seconds = java.math.BigDecimal.valueOf(interval(quality)).divide(java.math.BigDecimal.valueOf(20)).stripTrailingZeros().toPlainString();
        return List.of(Component.translatable("gene.yixiangshouronglu.regenerative_cells.healing", seconds),
                Component.translatable("gene.yixiangshouronglu.regenerative_cells.overheal", (int) Math.round(overheal(quality) * 100)));
    }
    @Override public void installed(ServerPlayer player, ItemStack module) { GeneCapability.get(player).effectRuntime().remove("RegenerativeCells"); }
    @Override public void removed(ServerPlayer player, ItemStack module) { GeneCapability.get(player).effectRuntime().remove("RegenerativeCells"); }
}

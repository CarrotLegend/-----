package com.carrot123.yixiangshouronglu.gene;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExtendedGenesTest {
    @BeforeAll static void register() { ExtendedGenes.register(); }

    @Test void definitionsKeepRegionsCapacityAndIndependentCategories() {
        var shell = GeneDefinitions.get(ExtendedGenes.CLAWDIAN_EXOSKELETON);
        assertEquals(GeneTypes.Strength.ZAYIN, shell.strength());
        assertEquals(GeneTypes.Region.SKELETAL, shell.region());
        assertEquals(2, shell.cost());
        assertTrue(shell.categories().isEmpty());
        var dust = GeneDefinitions.get(ExtendedGenes.HEART_OF_DUST);
        var vine = GeneDefinitions.get(ExtendedGenes.VINE_MUSCLES);
        var marrow = GeneDefinitions.get(ExtendedGenes.FROZEN_BONE_MARROW);
        assertEquals(GeneTypes.Region.CARDIOPULMONARY, dust.region());
        assertEquals(GeneTypes.Region.MUSCLE, vine.region());
        assertEquals(GeneTypes.Region.SKELETAL, marrow.region());
        assertEquals(List.of(5, 4, 6), List.of(dust.cost(), vine.cost(), marrow.cost()));
        for (var definition : List.of(dust, vine, marrow)) {
            assertEquals(GeneTypes.Strength.TETH, definition.strength());
            assertEquals(Set.of(GeneCategories.MUTANT, GeneCategories.UNDEAD), definition.categories());
        }
        var counts = GeneCategories.counts(List.of(shell, dust, vine, marrow));
        assertEquals(3, counts.get(GeneCategories.MUTANT));
        assertEquals(3, counts.get(GeneCategories.UNDEAD));
        assertTrue(GeneCategories.active(counts).isEmpty());
    }

    @Test void allQualityValuesMatchTheDesign() {
        double[][] expected = {{2, 5, .05, .10, 3, 4}, {4, 15, .10, .20, 5, 8}, {6, 25, .15, .30, 7, 12}};
        for (var quality : GeneTypes.Quality.values()) {
            double[] values = {ExtendedGenes.exoskeletonArmor(quality), ExtendedGenes.dustHealth(quality), ExtendedGenes.vineSpeed(quality),
                    ExtendedGenes.vineChance(quality), ExtendedGenes.marrowArmor(quality), ExtendedGenes.freezeDamage(quality)};
            assertArrayEquals(expected[quality.ordinal()], values, 1e-8);
        }
    }

    @Test void eachCategoryProducesOneLineWithAllItsBonuses() {
        var category = new GeneCategories.Category(GeneCategories.MUTANT, "test.category", List.of(
                new GeneCategories.Bonus(ExtendedGenes.CLAWDIAN_EXOSKELETON, 20, "test.health"),
                new GeneCategories.Bonus(ExtendedGenes.HEART_OF_DUST, 4, "test.attack")), true);
        var lines = category.descriptions();
        assertEquals(1, lines.size());
        var requirement = (TranslatableContents) lines.get(0).getContents();
        assertEquals(4, requirement.getArgs()[1]);
        var summary = (TranslatableContents) ((Component) requirement.getArgs()[0]).getContents();
        var combined = (Component) summary.getArgs()[1];
        assertEquals(5, combined.getSiblings().size());
        assertEquals("test.health", ((TranslatableContents) combined.getSiblings().get(0).getContents()).getKey());
        assertEquals("test.attack", ((TranslatableContents) combined.getSiblings().get(2).getContents()).getKey());
        assertEquals("gene.yixiangshouronglu.undead.regeneration", ((TranslatableContents) combined.getSiblings().get(4).getContents()).getKey());
    }
}

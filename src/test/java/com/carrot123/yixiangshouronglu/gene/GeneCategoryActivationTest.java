package com.carrot123.yixiangshouronglu.gene;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GeneCategoryActivationTest {
    private GeneDefinition gene(int index, Set<ResourceLocation> categories) {
        return new GeneDefinition(new ResourceLocation("test", "g" + index), "test.name", "test.desc", GeneTypes.Strength.ZAYIN,
                GeneTypes.Region.values()[index % GeneTypes.Region.values().length], 1, Set.of(), categories, null);
    }
    @Test void countsAcrossRegionsActivateOnlyAtFour() {
        List<GeneDefinition> installed = new ArrayList<>();
        for (int count = 0; count <= 5; count++) {
            assertEquals(count >= 4, GeneCategories.active(GeneCategories.counts(installed)).contains(GeneCategories.MUTANT));
            if (count < 5) { installed.add(gene(count, Set.of(GeneCategories.MUTANT))); }
        }
        installed.subList(3, installed.size()).clear();
        assertFalse(GeneCategories.active(GeneCategories.counts(installed)).contains(GeneCategories.MUTANT));
    }
    @Test void dualCategoriesAndDuplicateIdsAreIndependent() {
        var both = Set.of(GeneCategories.MUTANT, GeneCategories.UNDEAD);
        var first = gene(0, both);
        List<GeneDefinition> installed = Arrays.asList(first, gene(1, both), gene(2, both), gene(3, Set.of(GeneCategories.MUTANT)), first, null);
        var counts = GeneCategories.counts(installed);
        assertEquals(4, counts.get(GeneCategories.MUTANT));
        assertEquals(3, counts.get(GeneCategories.UNDEAD));
        assertEquals(Set.of(GeneCategories.MUTANT), GeneCategories.active(counts));
    }
}

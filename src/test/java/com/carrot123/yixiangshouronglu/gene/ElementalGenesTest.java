package com.carrot123.yixiangshouronglu.gene;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ElementalGenesTest {
    @Test void qualityValuesAndTiming() {
        for (var quality : GeneTypes.Quality.values()) {
            assertEquals(3 + quality.ordinal() * 2, ElementalGenes.armor(quality));
            assertEquals((quality.ordinal() + 1) * 0.05, ElementalGenes.witherChance(quality), 0.000001);
        }
        assertEquals(600, BlazingDomain.DURATION); assertEquals(2400, BlazingDomain.COOLDOWN);
        assertEquals(30, BlazingDomain.DURATION / BlazingDomain.INTERVAL);
    }
    @Test void distinctCategoryThresholds() {
        var definitions = new ArrayList<GeneDefinition>();
        for (int count = 1; count <= 5; count++) {
            definitions.add(new GeneDefinition(new ResourceLocation("acc_test", "element_" + count), "name", "description", GeneTypes.Strength.TETH,
                    GeneTypes.Region.MUSCLE, 1, Set.of(), Set.of(GeneCategories.ELEMENTAL, GeneCategories.MUTANT), null));
            var counts = GeneCategories.counts(definitions);
            assertEquals(count >= 4, GeneCategories.active(counts).contains(GeneCategories.ELEMENTAL));
            assertEquals(count, counts.get(GeneCategories.MUTANT));
        }
        definitions.add(definitions.get(0)); assertEquals(5, GeneCategories.counts(definitions).get(GeneCategories.ELEMENTAL));
        definitions.subList(3, definitions.size()).clear(); assertFalse(GeneCategories.active(GeneCategories.counts(definitions)).contains(GeneCategories.ELEMENTAL));
    }
}

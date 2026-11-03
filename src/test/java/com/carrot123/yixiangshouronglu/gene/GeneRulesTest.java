package com.carrot123.yixiangshouronglu.gene;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class GeneRulesTest {
    private final ResourceLocation a = new ResourceLocation("test", "a");
    private final ResourceLocation b = new ResourceLocation("test", "b");
    private GeneRules.Entry entry(ResourceLocation id, int cost) { return new GeneRules.Entry(id, GeneTypes.Region.MUSCLE, cost, Set.of(), true); }
    @Test void regionsAndQualityRemainIndependent() {
        assertEquals(10, java.util.Arrays.stream(GeneTypes.Region.values()).mapToInt(region -> region.slots).sum());
        assertEquals(GeneTypes.Quality.BETA, GeneTypes.Quality.ALPHA.next());
        assertEquals(GeneTypes.Quality.GAMMA, GeneTypes.Quality.BETA.next());
        assertNull(GeneTypes.Quality.GAMMA.next());
        assertEquals("region_mismatch", GeneRules.validate(entry(a, 3), GeneTypes.Region.VISION, List.of(), 20));
    }
    @Test void conflictsAreSymmetricAndDuplicatesReject() {
        assertEquals("duplicate", GeneRules.validate(entry(a, 3), GeneTypes.Region.MUSCLE, List.of(entry(a, 3)), 20));
        var conflicts = new GeneRules.Entry(b, GeneTypes.Region.MUSCLE, 3, Set.of(a), true);
        assertEquals("conflict", GeneRules.validate(entry(a, 3), GeneTypes.Region.MUSCLE, List.of(conflicts), 20));
        assertEquals("conflict", GeneRules.validate(conflicts, GeneTypes.Region.MUSCLE, List.of(entry(a, 3)), 20));
    }
    @Test void capacityAndUnknownsCannotBypassChecks() {
        assertEquals("ok", GeneRules.validate(entry(a, 13), GeneTypes.Region.MUSCLE, List.of(entry(b, 7)), 20));
        assertEquals("capacity_exceeded", GeneRules.validate(entry(a, 14), GeneTypes.Region.MUSCLE, List.of(entry(b, 7)), 20));
        assertEquals("unknown_capacity", GeneRules.validate(entry(a, 1), GeneTypes.Region.MUSCLE, List.of(new GeneRules.Entry(null, GeneTypes.Region.MUSCLE, 0, Set.of(), false)), 44));
        assertEquals("capacity_exceeded", GeneRules.validate(entry(a, Integer.MAX_VALUE), GeneTypes.Region.MUSCLE, List.of(entry(b, 10)), 44));
    }
    @Test void extremeLimitsAndRejectionBoundaries() {
        assertEquals(List.of(20,25,30,35,40), java.util.Arrays.stream(GeneTypes.Capacity.values()).map(level -> level.limit).toList());
        assertEquals(GeneTypes.Rejection.NONE, GeneTypes.rejection(40,40));
        assertEquals(GeneTypes.Rejection.MILD, GeneTypes.rejection(41,40));
        assertEquals(GeneTypes.Rejection.MILD, GeneTypes.rejection(42,40));
        assertEquals(GeneTypes.Rejection.SEVERE, GeneTypes.rejection(43,40));
        assertEquals(GeneTypes.Rejection.SEVERE, GeneTypes.rejection(44,40));
        assertEquals("ok", GeneRules.validate(entry(a, 24), GeneTypes.Region.MUSCLE, List.of(entry(b, 20)), 44));
        assertEquals("capacity_exceeded", GeneRules.validate(entry(a, 25), GeneTypes.Region.MUSCLE, List.of(entry(b, 20)), 44));
    }
}

package com.carrot123.yixiangshouronglu.gene;

import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Set;

public final class GeneRules {
    private GeneRules() {}
    public record Entry(ResourceLocation id, GeneTypes.Region region, int cost, Set<ResourceLocation> conflicts, boolean known) {}
    public static String validate(Entry candidate, GeneTypes.Region target, List<Entry> existing, int limit) {
        if (candidate == null || !candidate.known()) { return "invalid"; }
        if (candidate.region() != target) { return "region_mismatch"; }
        long total = candidate.cost();
        for (Entry entry : existing) {
            if (!entry.known()) { return "unknown_capacity"; }
            if (entry.id().equals(candidate.id())) { return "duplicate"; }
            if (entry.conflicts().contains(candidate.id()) || candidate.conflicts().contains(entry.id())) { return "conflict"; }
            total += entry.cost();
        }
        return total > limit ? "capacity_exceeded" : "ok";
    }
}

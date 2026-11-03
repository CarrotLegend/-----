package com.carrot123.yixiangshouronglu.gene;

import net.minecraft.resources.ResourceLocation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collection;

public final class GeneDefinitions {
    private static final Map<ResourceLocation, GeneDefinition> DEFINITIONS = new LinkedHashMap<>();
    private GeneDefinitions() {}
    public static synchronized void register(GeneDefinition definition) {
        if (DEFINITIONS.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalArgumentException("Duplicate gene " + definition.id());
        }
    }
    public static GeneDefinition get(ResourceLocation id) { return DEFINITIONS.get(id); }
    public static Collection<GeneDefinition> values() { return java.util.List.copyOf(DEFINITIONS.values()); }
}

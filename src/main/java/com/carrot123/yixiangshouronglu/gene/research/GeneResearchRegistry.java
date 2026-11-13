package com.carrot123.yixiangshouronglu.gene.research;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GeneResearchRegistry {
    private static final Map<ResourceLocation, GeneResearchDefinition> DEFINITIONS = new LinkedHashMap<>();
    public static final GeneResearchDefinition HEALTH = define("health", 20, 40, "minecraft:generic.max_health", 5, 20,
            extracts(4), item("minecraft:rotten_flesh", 4), item("minecraft:bone", 4), item("minecraft:gold_ingot", 1));
    public static final GeneResearchDefinition ATTACK = define("attack", 15, 30, "minecraft:generic.attack_damage", 1, 10,
            extracts(4), item("minecraft:iron_ingot", 4), item("minecraft:diamond", 1));
    public static final GeneResearchDefinition ARMOR = define("armor", 15, 30, "minecraft:generic.armor", 1, 6,
            extracts(4), item("minecraft:iron_ingot", 8), item("minecraft:netherite_scrap", 1));
    public static final GeneResearchDefinition ARMOR_SHRED = define("armor_shred", 5, 15, "attributeslib:armor_shred", 0.05, 4,
            extracts(8), item("minecraft:iron_sword", 1), item("minecraft:ender_pearl", 4));
    public static final GeneResearchDefinition FROST = define("frost", 5, 5, "attributeslib:cold_damage", 5, 4,
            extracts(16), item("minecraft:netherite_scrap", 1), item("mutantmore:mutant_frozen_zombie_spikes", 1));

    private GeneResearchRegistry() {}

    public static void registerDefaults() {
        List.of(HEALTH, ATTACK, ARMOR, ARMOR_SHRED, FROST).forEach(GeneResearchRegistry::register);
    }

    public static void register(GeneResearchDefinition definition) {
        if (DEFINITIONS.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalArgumentException("Duplicate research definition: " + definition.id());
        }
    }

    public static GeneResearchDefinition get(ResourceLocation id) {
        return DEFINITIONS.get(id);
    }

    public static List<GeneResearchDefinition> values() {
        return List.copyOf(DEFINITIONS.values());
    }

    private static GeneResearchDefinition define(String name, int min, int max, String attribute, double amount, int limit,
                                                 GeneResearchDefinition.Material... materials) {
        return new GeneResearchDefinition(new ResourceLocation("yixiangshouronglu", "gene_modifier_" + name), min, max,
                new ResourceLocation(attribute), amount, limit, List.of(materials));
    }

    private static GeneResearchDefinition.Material item(String id, int count) {
        return new GeneResearchDefinition.Material(new ResourceLocation(id), count, false, null);
    }

    private static GeneResearchDefinition.Material extracts(int count) {
        return new GeneResearchDefinition.Material(new ResourceLocation("yixiangshouronglu", "containment_extract"), count, true, null);
    }
}

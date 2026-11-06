package com.carrot123.yixiangshouronglu.gene;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class GeneCategories {
    public record Bonus(ResourceLocation attribute, double amount, String descriptionKey) {}
    public record Category(ResourceLocation id, String nameKey, List<Bonus> bonuses, boolean regeneration, List<String> extraDescriptions, int threshold) {
        public Category {
            if (threshold < 1) { throw new IllegalArgumentException("Category threshold must be positive"); }
            bonuses = List.copyOf(bonuses); extraDescriptions = List.copyOf(extraDescriptions); }
        public Category(ResourceLocation id, String nameKey, List<Bonus> bonuses, boolean regeneration) { this(id, nameKey, bonuses, regeneration, List.of()); }
        public Category(ResourceLocation id, String nameKey, List<Bonus> bonuses, boolean regeneration, List<String> extraDescriptions) {
            this(id, nameKey, bonuses, regeneration, extraDescriptions, ACTIVATION_COUNT);
        }
        public UUID modifierId(ResourceLocation attribute) { return UUID.nameUUIDFromBytes(("yixiangshouronglu:gene_category:" + id + ":" + attribute).getBytes(StandardCharsets.UTF_8)); }
        public List<Component> descriptions() {
            List<Component> result = new ArrayList<>();
            for (Bonus bonus : bonuses) { result.add(Component.translatable(bonus.descriptionKey(), (int) bonus.amount())); }
            if (regeneration) { result.add(Component.translatable("gene.yixiangshouronglu.undead.regeneration")); }
            extraDescriptions.forEach(key -> result.add(Component.translatable(key)));
            if (result.isEmpty()) { return List.of(); }
            var combined = Component.empty();
            for (Component line : result) {
                if (!combined.getSiblings().isEmpty()) { combined.append(Component.translatable("gene.yixiangshouronglu.effect.separator")); }
                combined.append(line);
            }
            return List.of(Component.translatable("gene.yixiangshouronglu.category_requirement",
                    Component.translatable("gene.yixiangshouronglu.category_effects", Component.translatable(nameKey), combined), threshold));
        }
    }
    public static final int ACTIVATION_COUNT = 4;
    public static Map<ResourceLocation, Integer> counts(Iterable<GeneDefinition> definitions) {
        Map<ResourceLocation, Integer> result = new HashMap<>(); Set<ResourceLocation> seen = new HashSet<>();
        for (GeneDefinition definition : definitions) {
            if (definition == null || !seen.add(definition.id())) { continue; }
            for (ResourceLocation category : definition.categories()) { result.merge(category, 1, Integer::sum); }
        }
        return Map.copyOf(result);
    }
    public static Set<ResourceLocation> active(Map<ResourceLocation, Integer> counts) {
        Set<ResourceLocation> result = new HashSet<>();
        counts.forEach((id, count) -> { if (count >= threshold(id)) { result.add(id); } });
        return Set.copyOf(result);
    }
    private static final Map<ResourceLocation, Category> CATEGORIES = new LinkedHashMap<>();
    public static final ResourceLocation MUTANT = new ResourceLocation("yixiangshouronglu", "mutant");
    public static final ResourceLocation ELEMENTAL = new ResourceLocation("yixiangshouronglu", "elemental");
    public static final ResourceLocation UNDEAD = new ResourceLocation("yixiangshouronglu", "undead");
    public static final ResourceLocation DRAGON = new ResourceLocation("yixiangshouronglu", "dragon");
    public static int threshold(ResourceLocation id) {
        Category category = get(id);
        return category == null ? ACTIVATION_COUNT : category.threshold();
    }
    private GeneCategories() {}
    public static void register(Category category) { if (CATEGORIES.putIfAbsent(category.id(), category) != null) { throw new IllegalArgumentException("Duplicate gene category " + category.id()); } }
    public static Category get(ResourceLocation id) { return CATEGORIES.get(id); }
    public static Collection<Category> values() { return List.copyOf(CATEGORIES.values()); }
}

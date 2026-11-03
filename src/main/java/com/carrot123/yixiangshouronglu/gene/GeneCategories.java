package com.carrot123.yixiangshouronglu.gene;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class GeneCategories {
    public record Bonus(ResourceLocation attribute, double amount, String descriptionKey) {}
    public record Category(ResourceLocation id, String nameKey, List<Bonus> bonuses, boolean regeneration) {
        public Category { bonuses = List.copyOf(bonuses); }
        public UUID modifierId(ResourceLocation attribute) { return UUID.nameUUIDFromBytes(("yixiangshouronglu:gene_category:" + id + ":" + attribute).getBytes(StandardCharsets.UTF_8)); }
        public List<Component> descriptions() {
            List<Component> result = new ArrayList<>();
            for (Bonus bonus : bonuses) { result.add(Component.translatable(bonus.descriptionKey(), (int) bonus.amount())); }
            if (regeneration) { result.add(Component.translatable("gene.yixiangshouronglu.undead.regeneration")); }
            return result;
        }
    }
    private static final Map<ResourceLocation, Category> CATEGORIES = new LinkedHashMap<>();
    public static final ResourceLocation MUTANT = new ResourceLocation("yixiangshouronglu", "mutant");
    public static final ResourceLocation UNDEAD = new ResourceLocation("yixiangshouronglu", "undead");
    private GeneCategories() {}
    public static void register(Category category) { if (CATEGORIES.putIfAbsent(category.id(), category) != null) { throw new IllegalArgumentException("Duplicate gene category " + category.id()); } }
    public static Category get(ResourceLocation id) { return CATEGORIES.get(id); }
    public static Collection<Category> values() { return List.copyOf(CATEGORIES.values()); }
}

package com.carrot123.yixiangshouronglu.gene;

import net.minecraft.network.chat.Component;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public record GeneDefinition(ResourceLocation id, String nameKey, String descriptionKey,
                             GeneTypes.Strength strength, GeneTypes.Region region, int cost,
                             Set<ResourceLocation> conflicts, Set<ResourceLocation> categories, Effect effect) {
    public GeneDefinition {
        java.util.Objects.requireNonNull(id);
        java.util.Objects.requireNonNull(nameKey);
        java.util.Objects.requireNonNull(descriptionKey);
        java.util.Objects.requireNonNull(strength);
        java.util.Objects.requireNonNull(region);
        if (cost <= 0) { throw new IllegalArgumentException("Gene cost must be positive"); }
        conflicts = Set.copyOf(conflicts);
        categories = Set.copyOf(categories);
    }
    public GeneDefinition(ResourceLocation id, String nameKey, String descriptionKey, GeneTypes.Strength strength, GeneTypes.Region region, int cost, Set<ResourceLocation> conflicts, Effect effect) {
        this(id, nameKey, descriptionKey, strength, region, cost, conflicts, Set.of(), effect);
    }
    public interface Effect {
        default double explosionReduction(GeneTypes.Quality quality) { return 0; }
        default List<Component> descriptions(GeneTypes.Quality quality) { return List.of(); }
        default GeneDisplay display(GeneTypes.Quality quality, net.minecraft.nbt.CompoundTag runtime) { return GeneDisplay.EMPTY; }
        default void installed(ServerPlayer player, ItemStack module) {}
        default void removed(ServerPlayer player, ItemStack module) {}
        default void restored(ServerPlayer player, ItemStack module) {}
        default void refreshed(ServerPlayer player, ItemStack module) {}
    }
    public UUID effectId(String purpose) {
        return UUID.nameUUIDFromBytes(("yixiangshouronglu:gene:" + id + ":" + purpose).getBytes(StandardCharsets.UTF_8));
    }
}

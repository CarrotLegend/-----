package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

public final class ExtendedGenes {
    public static final ResourceLocation CLAWDIAN_EXOSKELETON = id("clawdian_exoskeleton");
    public static final ResourceLocation HEART_OF_DUST = id("heart_of_dust");
    public static final ResourceLocation VINE_MUSCLES = id("vine_muscles");
    public static final ResourceLocation FROZEN_BONE_MARROW = id("frozen_bone_marrow");
    private static final ResourceLocation ARMOR = new ResourceLocation("minecraft", "generic.armor");
    private static final ResourceLocation HEALTH = new ResourceLocation("minecraft", "generic.max_health");
    private static final ResourceLocation SPEED = new ResourceLocation("minecraft", "generic.movement_speed");

    private ExtendedGenes() {}
    private static ResourceLocation id(String path) { return new ResourceLocation("yixiangshouronglu", path); }
    public static double exoskeletonArmor(GeneTypes.Quality quality) { return 2 + quality.ordinal() * 2; }
    public static double dustHealth(GeneTypes.Quality quality) { return 5 + quality.ordinal() * 10; }
    public static double vineSpeed(GeneTypes.Quality quality) { return (quality.ordinal() + 1) * 0.05; }
    public static double vineChance(GeneTypes.Quality quality) { return (quality.ordinal() + 1) * 0.10; }
    public static double marrowArmor(GeneTypes.Quality quality) { return 3 + quality.ordinal() * 2; }
    public static float freezeDamage(GeneTypes.Quality quality) { return (quality.ordinal() + 1) * 4; }

    public static void register() {
        register(CLAWDIAN_EXOSKELETON, GeneTypes.Strength.ZAYIN, GeneTypes.Region.SKELETAL, 2, Set.of(), quality -> List.of(
                Component.translatable("gene.yixiangshouronglu.effect.armor", (int) exoskeletonArmor(quality))));
        Set<ResourceLocation> categories = Set.of(GeneCategories.MUTANT, GeneCategories.UNDEAD);
        register(HEART_OF_DUST, GeneTypes.Strength.TETH, GeneTypes.Region.CARDIOPULMONARY, 5, categories, quality -> List.of(
                Component.translatable("gene.yixiangshouronglu.effect.health", (int) dustHealth(quality)),
                Component.translatable("gene.yixiangshouronglu.heart_of_dust.immunity")));
        register(VINE_MUSCLES, GeneTypes.Strength.TETH, GeneTypes.Region.MUSCLE, 4, categories, quality -> List.of(
                Component.translatable("gene.yixiangshouronglu.vine_muscles.speed", Math.round(vineSpeed(quality) * 100)),
                Component.translatable("gene.yixiangshouronglu.vine_muscles.slow", Math.round(vineChance(quality) * 100))));
        register(FROZEN_BONE_MARROW, GeneTypes.Strength.TETH, GeneTypes.Region.SKELETAL, 6, categories, quality -> List.of(
                Component.translatable("gene.yixiangshouronglu.effect.armor", (int) marrowArmor(quality)),
                Component.translatable("gene.yixiangshouronglu.frozen_bone_marrow.freeze", (int) freezeDamage(quality))));
    }

    private static void register(ResourceLocation id, GeneTypes.Strength strength, GeneTypes.Region region, int cost,
                                 Set<ResourceLocation> categories, java.util.function.Function<GeneTypes.Quality, List<Component>> descriptions) {
        GeneDefinitions.register(new GeneDefinition(id, "gene.yixiangshouronglu." + id.getPath(), "gene.yixiangshouronglu." + id.getPath() + ".description",
                strength, region, cost, Set.of(), categories, new GeneDefinition.Effect() {
                    @Override public List<Component> descriptions(GeneTypes.Quality quality) { return descriptions.apply(quality); }
                }));
    }

    public static void refresh(ServerPlayer player, Map<ResourceLocation, ItemStack> installed) {
        reconcile(player, installed, CLAWDIAN_EXOSKELETON, ARMOR, AttributeModifier.Operation.ADDITION, ExtendedGenes::exoskeletonArmor);
        reconcile(player, installed, HEART_OF_DUST, HEALTH, AttributeModifier.Operation.ADDITION, ExtendedGenes::dustHealth);
        reconcile(player, installed, VINE_MUSCLES, SPEED, AttributeModifier.Operation.MULTIPLY_BASE, ExtendedGenes::vineSpeed);
        reconcile(player, installed, FROZEN_BONE_MARROW, ARMOR, AttributeModifier.Operation.ADDITION, ExtendedGenes::marrowArmor);
        if (installed.containsKey(HEART_OF_DUST)) {
            player.removeEffect(MobEffects.HUNGER);
            player.removeEffect(MobEffects.POISON);
        }
    }

    private static void reconcile(ServerPlayer player, Map<ResourceLocation, ItemStack> installed, ResourceLocation gene,
                                  ResourceLocation attribute, AttributeModifier.Operation operation, ToDoubleFunction<GeneTypes.Quality> amount) {
        ItemStack module = installed.get(gene);
        GeneEffects.reconcile(player, attribute, GeneDefinitions.get(gene).effectId(attribute.toString()),
                module == null ? 0 : amount.applyAsDouble(GeneModuleItem.read(module).quality()), operation);
    }
}

package com.carrot123.yixiangshouronglu.resonance;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

public final class ResonanceRules {
    public record Bonus(Attribute attribute, AttributeModifier.Operation operation, double amount, String translationKey) {
    }

    private ResonanceRules() {
    }

    public static Bonus fromEntity(String id) {
        return switch (id) {
            case "mutantmonsters:mutant_zombie" -> new Bonus(Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION, 8.0D, "bonus.yixiangshouronglu.mutant_zombie");
            case "mutantmonsters:mutant_skeleton" -> new Bonus(Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION, 4.0D, "bonus.yixiangshouronglu.mutant_skeleton");
            case "mutantmonsters:mutant_creeper" -> new Bonus(Attributes.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADDITION, 0.30D, "bonus.yixiangshouronglu.mutant_creeper");
            case "mutantmonsters:mutant_enderman" -> new Bonus(Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL, 0.15D, "bonus.yixiangshouronglu.mutant_enderman");
            default -> fallback(id);
        };
    }

    private static Bonus fallback(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        EntityType<?> type = location == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(location);
        if (type == null) {
            return null;
        }
        MobCategory category = type.getCategory();
        if (category == MobCategory.MONSTER) {
            return new Bonus(Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION, 1.0D, "bonus.yixiangshouronglu.monster");
        }
        if (category == MobCategory.CREATURE || category == MobCategory.AMBIENT || category == MobCategory.UNDERGROUND_WATER_CREATURE) {
            return new Bonus(Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION, 2.0D, "bonus.yixiangshouronglu.creature");
        }
        return new Bonus(Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL, 0.05D, "bonus.yixiangshouronglu.other");
    }
}

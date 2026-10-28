package com.carrot123.yixiangshouronglu.resonance;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class ResonanceRules {
    private static final String ZOMBIE = "mutantmonsters:mutant_zombie";
    private static final String SKELETON = "mutantmonsters:mutant_skeleton";
    private static final String CREEPER = "mutantmonsters:mutant_creeper";
    private static final String ENDERMAN = "mutantmonsters:mutant_enderman";

    public record Bonus(Attribute attribute, AttributeModifier.Operation operation, double amount, String translationKey) {
    }

    private ResonanceRules() {
    }

    public static boolean grantsRegeneration(String entityId) {
        return ZOMBIE.equals(entityId);
    }

    public static Bonus fromEntity(String entityId) {
        List<Bonus> bonuses = allBonusesForEntity(entityId);
        return bonuses.isEmpty() ? null : bonuses.get(0);
    }

    public static List<Bonus> allBonusesForEntity(String entityId) {
        List<Bonus> bonuses = new ArrayList<>();

        switch (entityId) {
            case ZOMBIE -> add(
                    bonuses,
                    Attributes.MAX_HEALTH,
                    AttributeModifier.Operation.ADDITION,
                    10.0D,
                    "bonus.yixiangshouronglu.mutant_zombie"
            );

            case SKELETON -> {
                addTaa(bonuses, "bullet_gundamage", 0.04D, "bonus.yixiangshouronglu.mutant_skeleton");
                addTaa(bonuses, "rounds_per_minute", 0.02D, "bonus.yixiangshouronglu.mutant_skeleton");
            }

            case CREEPER -> {
                addTaa(bonuses, "bullet_gundamage_launcher", 0.05D, "bonus.yixiangshouronglu.mutant_creeper");
                addTaa(bonuses, "headshot_multiplier", 0.02D, "bonus.yixiangshouronglu.mutant_creeper");
                add(
                        bonuses,
                        Attributes.MAX_HEALTH,
                        AttributeModifier.Operation.ADDITION,
                        5.0D,
                        "bonus.yixiangshouronglu.mutant_creeper"
                );
            }

            case ENDERMAN -> {
                add(
                        bonuses,
                        Attributes.MOVEMENT_SPEED,
                        AttributeModifier.Operation.MULTIPLY_TOTAL,
                        0.10D,
                        "bonus.yixiangshouronglu.mutant_enderman"
                );
                addTaa(bonuses, "move_speed", 0.10D, "bonus.yixiangshouronglu.mutant_enderman");
            }

            default -> {
            }
        }

        return bonuses;
    }

    private static void addTaa(List<Bonus> bonuses, String path, double amount, String translationKey) {
        ResourceLocation id = new ResourceLocation("taa", path);

        if (ForgeRegistries.ATTRIBUTES.containsKey(id)) {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(id);

            if (attribute != null) {
                add(
                        bonuses,
                        attribute,
                        AttributeModifier.Operation.MULTIPLY_TOTAL,
                        amount,
                        translationKey
                );
            }
        }
    }

    private static void add(List<Bonus> bonuses, Attribute attribute, AttributeModifier.Operation operation, double amount, String translationKey) {
        bonuses.add(new Bonus(attribute, operation, amount, translationKey));
    }
}

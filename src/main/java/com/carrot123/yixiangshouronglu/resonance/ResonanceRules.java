package com.carrot123.yixiangshouronglu.resonance;

import com.carrot123.yixiangshouronglu.registry.ModAttributes;
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
    private static final String WITHER = "minecraft:wither";
    private static final String CRAB = "cataclysm:amethyst_crab";
    private static final String CLAWDIAN = "cataclysm:clawdian";
    private static final String SCYLLA = "cataclysm:scylla";
    private static final String LEVIATHAN = "cataclysm:the_leviathan";
    private static final String ENDER_GUARDIAN = "cataclysm:ender_guardian";
    private static final String TENACIOUS_SHIELD = "yixiangshouronglu:tenacious_shield";
    private static final String YOUNG_ENDER_DRAGON = "yixiangshouronglu:young_ender_dragon";

    private static final String JUNGLE_ZOMBIE = "mutantmore:mutant_jungle_zombie";
    private static final String FROZEN_ZOMBIE = "mutantmore:mutant_frozen_zombie";
    private static final String HUSK = "mutantmore:mutant_husk";
    private static final String HOGLIN = "mutantmore:mutant_hoglin";
    private static final String WITHER_SKELETON = "mutantmore:mutant_wither_skeleton";
    private static final String MUTANT_BLAZE = "mutantmore:mutant_blaze";

    private static final String FIRE_DRAGON = "iceandfire:fire_dragon";
    private static final String ICE_DRAGON = "iceandfire:ice_dragon";
    private static final String LIGHTNING_DRAGON = "iceandfire:lightning_dragon";

    public record Bonus(
            Attribute attribute,
            AttributeModifier.Operation operation,
            double amount,
            String translationKey
    ) {
    }

    private ResonanceRules() {
    }

    public static boolean isAntlersWendigo(String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId);

        return id != null
                && id.getNamespace().equals("antlers")
                && id.getPath().contains("wendigo");
    }

    public static boolean isMutantMoreZombie(String entityId) {
        return JUNGLE_ZOMBIE.equals(entityId)
                || FROZEN_ZOMBIE.equals(entityId)
                || HUSK.equals(entityId);
    }

    public static boolean grantsRegeneration(String entityId) {
        return ZOMBIE.equals(entityId)
                || isMutantMoreZombie(entityId)
                || HOGLIN.equals(entityId);
    }

    public static boolean grantsResistance(String entityId) {
        return TENACIOUS_SHIELD.equals(entityId);
    }

    public static boolean grantsBlazeProtection(String entityId) {
        return MUTANT_BLAZE.equals(entityId);
    }

    public static Bonus fromEntity(String entityId) {
        List<Bonus> bonuses = allBonusesForEntity(entityId);
        return bonuses.isEmpty() ? null : bonuses.get(0);
    }

    public static List<Bonus> allBonusesForEntity(String entityId) {
        List<Bonus> bonuses = new ArrayList<>();

        if (isAntlersWendigo(entityId)) {
            add(bonuses, Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION,
                    10.0D, "bonus.yixiangshouronglu.wendigo");
            add(bonuses, Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION,
                    2.0D, "bonus.yixiangshouronglu.wendigo");
            add(bonuses, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL,
                    0.10D, "bonus.yixiangshouronglu.wendigo");
            return bonuses;
        }

        if (isMutantMoreZombie(entityId) || HOGLIN.equals(entityId)) {
            String key = HOGLIN.equals(entityId)
                    ? "bonus.yixiangshouronglu.mutant_hoglin"
                    : "bonus.yixiangshouronglu.mutantmore_zombie";

            add(bonuses, Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION,
                    10.0D, key);
            add(bonuses, Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION,
                    2.0D, key);
            return bonuses;
        }

        switch (entityId) {
            case ZOMBIE ->
                    add(bonuses, Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION,
                            10.0D, "bonus.yixiangshouronglu.mutant_zombie");

            case SKELETON -> {
                addTaa(bonuses, "bullet_gundamage", 0.04D,
                        "bonus.yixiangshouronglu.mutant_skeleton");
                addTaa(bonuses, "rounds_per_minute", 0.02D,
                        "bonus.yixiangshouronglu.mutant_skeleton");
            }

            case CREEPER -> {
                addTaa(bonuses, "bullet_gundamage_launcher", 0.05D,
                        "bonus.yixiangshouronglu.mutant_creeper");
                addTaa(bonuses, "headshot_multiplier", 0.02D,
                        "bonus.yixiangshouronglu.mutant_creeper");
                add(bonuses, Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION,
                        5.0D, "bonus.yixiangshouronglu.mutant_creeper");
            }

            case ENDERMAN -> {
                add(bonuses, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL,
                        0.10D, "bonus.yixiangshouronglu.mutant_enderman");
                addTaa(bonuses, "move_speed", 0.10D,
                        "bonus.yixiangshouronglu.mutant_enderman");
            }

            case WITHER -> {
                add(bonuses, Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION,
                        5.0D, "bonus.yixiangshouronglu.wither");
                add(bonuses, ModAttributes.ALL_DAMAGE.get(), AttributeModifier.Operation.MULTIPLY_TOTAL,
                        0.06D, "bonus.yixiangshouronglu.wither");
            }

            case CRAB -> {
                add(bonuses, Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION,
                        4.0D, "bonus.yixiangshouronglu.amethyst_crab");
                addTaa(bonuses, "weight", -0.05D,
                        "bonus.yixiangshouronglu.amethyst_crab");
            }

            case CLAWDIAN ->
                    add(bonuses, ModAttributes.ALL_DAMAGE.get(), AttributeModifier.Operation.MULTIPLY_TOTAL,
                            0.02D, "bonus.yixiangshouronglu.clawdian");

            case SCYLLA -> {
                add(bonuses, Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION,
                        5.0D, "bonus.yixiangshouronglu.scylla");
                addTaa(bonuses, "bullet_gundamage_sniper", 0.10D,
                        "bonus.yixiangshouronglu.scylla");
                addTaa(bonuses, "bullet_gundamage_launcher", 0.10D,
                        "bonus.yixiangshouronglu.scylla");
            }

            case LEVIATHAN -> {
                add(bonuses, Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION,
                        5.0D, "bonus.yixiangshouronglu.the_leviathan");
                addTaa(bonuses, "bullet_gundamage_smg", 0.10D,
                        "bonus.yixiangshouronglu.the_leviathan");
                addTaa(bonuses, "bullet_gundamage_shotgun", 0.10D,
                        "bonus.yixiangshouronglu.the_leviathan");
            }

            case ENDER_GUARDIAN -> {
                add(bonuses, Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION,
                        5.0D, "bonus.yixiangshouronglu.ender_guardian");
                addTaa(bonuses, "bullet_gundamage_rifle", 0.10D,
                        "bonus.yixiangshouronglu.ender_guardian");
                addTaa(bonuses, "bullet_gundamage_pistol", 0.10D,
                        "bonus.yixiangshouronglu.ender_guardian");
            }

            case FIRE_DRAGON, ICE_DRAGON, LIGHTNING_DRAGON -> {
                add(bonuses, Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION,
                        10.0D, "bonus.yixiangshouronglu.iceandfire_dragon");
                addTaa(bonuses, "bullet_gundamage", 0.02D,
                        "bonus.yixiangshouronglu.iceandfire_dragon");
                addTaa(bonuses, "bullet_gundamage_rifle", 0.04D,
                        "bonus.yixiangshouronglu.iceandfire_dragon");
            }

            case TENACIOUS_SHIELD -> {
                add(bonuses, Attributes.ARMOR, AttributeModifier.Operation.ADDITION,
                        4.0D, "bonus.yixiangshouronglu.tenacious_shield");
                add(bonuses, Attributes.ARMOR_TOUGHNESS, AttributeModifier.Operation.ADDITION,
                        2.0D, "bonus.yixiangshouronglu.tenacious_shield");
            }

            case WITHER_SKELETON ->
                    add(bonuses, ModAttributes.ALL_DAMAGE.get(), AttributeModifier.Operation.MULTIPLY_TOTAL,
                            0.04D, "bonus.yixiangshouronglu.mutantmore_wither_skeleton");

            case YOUNG_ENDER_DRAGON -> {
                add(bonuses, Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION,
                        10.0D, "bonus.yixiangshouronglu.young_ender_dragon");
                add(bonuses, ModAttributes.ALL_DAMAGE.get(), AttributeModifier.Operation.MULTIPLY_TOTAL,
                        0.05D, "bonus.yixiangshouronglu.young_ender_dragon");
            }

            default -> {
            }
        }

        return bonuses;
    }

    private static void addTaa(
            List<Bonus> bonuses,
            String path,
            double amount,
            String translationKey
    ) {
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

    private static void add(
            List<Bonus> bonuses,
            Attribute attribute,
            AttributeModifier.Operation operation,
            double amount,
            String translationKey
    ) {
        bonuses.add(new Bonus(
                attribute,
                operation,
                amount,
                translationKey
        ));
    }
}

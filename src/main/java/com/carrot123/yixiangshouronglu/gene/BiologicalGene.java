package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.menu.GeneModificationMenu;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public enum BiologicalGene {
    COMMON("common", null, 0, 20, 0, 2, 1),
    SPINAL_VOIDIZATION("spinal_voidization", "cataclysm:ender_guardian", 1, 4, 2, 20, 4),
    MOLTEN_SKELETON("molten_skeleton", "cataclysm:netherite_monstrosity", 1, 4, 6, 0, 0),
    SOULFIRE_REACTOR_CORE("soulfire_reactor_core", "cataclysm:ignis", 13, 16, 0, 0, 0),
    WAR_INSTINCT("war_instinct", "cataclysm:the_harbinger", 17, 20, 0, 0, 0),
    ABYSSAL_CIRCULATION("abyssal_circulation", "cataclysm:the_leviathan", 9, 12, 0, 20, 0),
    DESERT_MUSCLE_REINFORCEMENT("desert_muscle_reinforcement", "cataclysm:ancient_remnant", 5, 8, 0, 0, 8),
    CURSED_MUSCLE_FIBERS("cursed_muscle_fibers", "cataclysm:maledictus", 5, 8, 0, 0, 0),
    THUNDER_CORE("thunder_core", "cataclysm:scylla", 13, 16, 0, 0, 5),
    SKY_STORM_CORE("sky_storm_core", "legendary_monsters:cloud_golem", 13, 16, 0, 0, 0),
    FALLEN_COMBAT_NERVES("fallen_combat_nerves", "legendary_monsters:posessed_paladin", 0, 0, 0, 0, 0),
    ANNIHILATION_REACTOR_CORE("annihilation_reactor_core", "legendary_monsters:the_obliterator", 13, 16, 0, 0, 0),
    ANCHOR_MUSCLE_FIBERS("anchor_muscle_fibers", "legendary_monsters:ancient_guardian", 5, 8, 0, 0, 4),
    PRIMORDIAL_ICE_CRYSTALLIZATION("primordial_ice_crystallization", "legendary_monsters:frostbitten_golem", 1, 4, 0, 0, 0),
    GLANDULAR_REGENERATION("glandular_regeneration", "legendary_monsters:overgrown_colossus", 9, 12, 0, 8, 0),
    SANDSTORM_VISION("sandstorm_vision", "legendary_monsters:dune_sentinel", 17, 20, 0, 0, 0),
    ANCIENT_DRAGON_SPINE("ancient_dragon_spine", "legendary_monsters:skeletosaurus", 1, 4, 0, 0, 0),
    LAVA_DIGESTION("lava_digestion", "legendary_monsters:lava_eater", 9, 12, 0, 0, 6),
    ABOMINATION_SPINAL_CORD("abomination_spinal_cord", "legendary_monsters:withered_abomination", 9, 12, 0, 0, 0),
    CHORUS_TRANSLOCATION("chorus_translocation", "legendary_monsters:endersent", 5, 8, 0, 0, 0),
    CARAPACE_OSSIFICATION("carapace_ossification", "legendary_monsters:shulker_mimic", 1, 4, 8, 0, 0),
    THOUSAND_FORGED_MUSCLE("thousand_forged_muscle", "mowziesmobs:ferrous_wroughtnaut", 5, 8, 8, 0, 8),
    EXTREME_COLD_BREATH("extreme_cold_breath", "mowziesmobs:frostmaw", 13, 16, 0, 0, 0),
    SOLAR_RETINA("solar_retina", "mowziesmobs:umvuthi", 17, 20, 0, 0, 0),
    INCINERATION_GLAND("incineration_gland", "iceandfire:fire_dragon", 13, 16, 0, 0, 0),
    FREEZING_GLAND("freezing_gland", "iceandfire:ice_dragon", 13, 16, 0, 0, 0),
    LIGHTNING_CHAIN_GLAND("lightning_chain_gland", "iceandfire:lightning_dragon", 13, 16, 0, 0, 0),
    UNDYING_BODY("undying_body", "mutantmonsters:mutant_zombie", 9, 12, 0, 10, 0),
    PRECISION_MUSCLE_FIBERS("precision_muscle_fibers", "mutantmonsters:mutant_skeleton", 5, 8, 0, 0, 0),
    EXPLOSIVE_GLAND("explosive_gland", "mutantmonsters:mutant_creeper", 13, 16, 0, 0, 0),
    ENDER_SYNAPSE("ender_synapse", "mutantmonsters:mutant_enderman", 0, 0, 0, 0, 0),
    FROST_CORE_RETINA("frost_core_retina", "mutantmonsters:mutant_snow_golem", 17, 20, 0, 0, 0),
    COMBAT_NERVES("combat_nerves", "mutantmore:mutant_wither_skeleton", 17, 20, 0, 0, 0),
    BLAZING_MUSCLES("blazing_muscles", "mutantmore:mutant_blaze", 5, 8, 0, 0, 6),
    DIMENSIONAL_CALCIFIED_CARAPACE("dimensional_calcified_carapace", "mutantmore:mutant_shulker", 1, 4, 8, 20, 0),
    QUICKSAND_METABOLISM("quicksand_metabolism", "mutantmore:mutant_husk", 9, 12, 0, 10, 0),
    HOGLIN_MUSCLE_BUNDLES("hoglin_muscle_bundles", "mutantmore:mutant_hoglin", 5, 8, 0, 20, 6),
    FROZEN_CIRCULATORY_TISSUE("frozen_circulatory_tissue", "mutantmore:mutant_frozen_zombie", 9, 12, 0, 8, 0),
    PLANT_MUSCLE_FIBERS("plant_muscle_fibers", "mutantmore:mutant_jungle_zombie", 5, 8, 0, 0, 5),
    ARACHNID_APONEUROSIS("arachnid_aponeurosis", "mutantmonsters:spider_pig", 5, 8, 0, 0, 0);

    public static final int BLAZING_MUSCLES_BURN_SECONDS = 4;
    public static final int ENDER_TELEPORT_COOLDOWN = 600;
    public static final float BLAZING_MUSCLES_BURN_DAMAGE = 1.0F;

    public static final int SEARING_TICKS = 100;

    public static final int VOID_CHANCE_PERCENT = 8;
    public static final int VOID_REDUCTION_PERCENT = 20;
    public static final int SHOCK_DAMAGE = 12;
    public static final int SHOCK_FIRE_SECONDS = 2;
    public static final int SHOCK_COOLDOWN = 100;
    public static final int SOULFIRE_DAMAGE_PERCENT = 35;
    public static final int BURNING_DAMAGE_PERCENT = 20;
    public static final int BRAND_CHANCE_PERCENT = 25;
    public static final int BRAND_TICKS = 100;
    public static final int RANGED_DAMAGE_PERCENT = 24;
    public static final int CRIT_CHANCE_POINTS = 12;
    public static final int MOVE_SPEED_PERCENT = 10;
    public static final int SWIM_SPEED_PERCENT = 50;
    public static final int WATER_HEAL = 1;
    public static final int WATER_HEAL_TICKS = 20;
    public static final int STUN_CHANCE_PERCENT = 8;
    public static final int STUN_TICKS = 60;
    public static final int ATTACK_SPEED_PERCENT = 12;
    public static final int RAGE_DAMAGE_PERCENT = 15;
    public static final int MAX_RAGE = 3;
    public static final int RAGE_TICKS = 120;
    public static final int BOLT_CHANCE_PERCENT = 15;
    public static final int BOLT_DAMAGE = 9;
    public static final int COLD_DAMAGE = 5;
    public static final int EXTREME_COLD_CHANCE_PERCENT = 17;
    public static final int EXTREME_COLD_DAMAGE_PERCENT = 50;
    public static final int SOLAR_CRIT_PERCENT = 8;
    public static final int SOLAR_BEAM_CHANCE_PERCENT = 10;
    public static final int SOLAR_BEAM_DAMAGE = 12;
    public static final int GLANDULAR_POISON_CHANCE_PERCENT = 20;
    public static final int GLANDULAR_POISON_TICKS = 100;
    public static final int GLANDULAR_HEAL = 2;
    public static final int SANDSTORM_CRIT_PERCENT = 20;
    public static final int ANCIENT_KNOCKBACK_RESISTANCE_PERCENT = 35;
    public static final int LAVA_ATTACK_PERCENT = 20;
    public static final int ABOMINATION_WITHER_TICKS = 100;
    public static final int ABOMINATION_WITHER_AMPLIFIER = 1;
    public static final int CHORUS_SPEED_PERCENT = 20;
    public static final int CHORUS_DODGE_PERCENT = 10;
    public static final int SKY_DAMAGE_PERCENT = 35;
    public static final int SKY_HITS = 5;
    public static final int SKY_BOLT_DAMAGE = 12;
    public static final int SKY_SLOW_TICKS = 40;
    public static final int SKY_COOLDOWN = 240;
    public static final int FALLEN_BONUS_PERCENT = 25;
    public static final int FALLEN_TICKS = 600;
    public static final int FALLEN_COOLDOWN = 4800;
    public static final int ANNIHILATION_DAMAGE_PERCENT = 16;
    public static final int ANNIHILATION_HEALTH_PERCENT = 4;
    public static final int ANNIHILATION_COOLDOWN = 160;
    public static final int ANCHOR_CHANCE_PERCENT = 15;
    public static final int ANCHOR_DAMAGE = 8;
    public static final int ANCHOR_SLOW_TICKS = 60;
    public static final int CHILL_CHANCE_PERCENT = 15;
    public static final int CHILL_TICKS = 100;
    public static final int CHILL_STACKS = 3;
    public static final int CHILL_DAMAGE = 2;

    public final String id;
    public final int armor;
    public final int health;
    public final int attack;
    private final ResourceLocation source;
    private final int firstSlot;
    private final int lastSlot;

    BiologicalGene(String id, String source, int firstSlot, int lastSlot, int armor, int health, int attack) {
        this.id = id;
        this.source = source == null ? null : new ResourceLocation(source);
        this.firstSlot = firstSlot;
        this.lastSlot = lastSlot;
        this.armor = armor;
        this.health = health;
        this.attack = attack;
    }

    public static BiologicalGene fromSource(ResourceLocation source) {
        for (BiologicalGene gene : values()) {
            if (source.equals(gene.source)) {
                return gene;
            }
        }
        return COMMON;
    }

    public ResourceLocation displaySource() {
        return source == null ? new ResourceLocation("minecraft", "zombie") : source;
    }

    public boolean allowsSlot(int slot) {
        return slot >= firstSlot && slot <= lastSlot;
    }

    @Nullable
    public String installationRegionKey() {
        int finalSlot = PlayerGeneModules.SIZE - 1;
        if (firstSlot == 0 && lastSlot == finalSlot) {
            return "tooltip.yixiangshouronglu.biological_gene_module.any_region";
        }
        if (firstSlot < 0 || lastSlot < firstSlot || lastSlot > finalSlot) {
            return null;
        }
        int region = GeneModificationMenu.region(firstSlot);
        if (region != GeneModificationMenu.region(lastSlot)) {
            return null;
        }
        return switch (region) {
            case 0 -> "screen.yixiangshouronglu.gene_modification.neural";
            case 1 -> "screen.yixiangshouronglu.gene_modification.bone";
            case 2 -> "screen.yixiangshouronglu.gene_modification.muscle";
            case 3 -> "screen.yixiangshouronglu.gene_modification.metabolism";
            case 4 -> "screen.yixiangshouronglu.gene_modification.energy";
            case 5 -> "screen.yixiangshouronglu.gene_modification.senses";
            default -> null;
        };
    }
}

package com.carrot123.yixiangshouronglu.gene;

import net.minecraft.resources.ResourceLocation;

public enum BiologicalGene {
    COMMON("common", null, 0, 20, 0, 2, 1),
    SPINAL_VOIDIZATION("spinal_voidization", "cataclysm:ender_guardian", 1, 4, 2, 20, 4),
    MOLTEN_SKELETON("molten_skeleton", "cataclysm:netherite_monstrosity", 1, 4, 6, 0, 0),
    SOULFIRE_REACTOR_CORE("soulfire_reactor_core", "cataclysm:ignis", 13, 16, 0, 0, 0),
    WAR_INSTINCT("war_instinct", "cataclysm:the_harbinger", 17, 20, 0, 0, 0),
    ABYSSAL_CIRCULATION("abyssal_circulation", "cataclysm:the_leviathan", 9, 12, 0, 20, 0),
    DESERT_MUSCLE_REINFORCEMENT("desert_muscle_reinforcement", "cataclysm:ancient_remnant", 5, 8, 0, 0, 8),
    CURSED_MUSCLE_FIBERS("cursed_muscle_fibers", "cataclysm:maledictus", 5, 8, 0, 0, 0),
    THUNDER_CORE("thunder_core", "cataclysm:scylla", 13, 16, 0, 0, 5);

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
}

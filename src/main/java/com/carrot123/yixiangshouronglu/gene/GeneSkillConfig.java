package com.carrot123.yixiangshouronglu.gene;

import net.minecraftforge.common.ForgeConfigSpec;

public final class GeneSkillConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue TELEPORT_RANGE;
    public static final ForgeConfigSpec.IntValue TELEPORT_COOLDOWN;
    public static final ForgeConfigSpec.IntValue DRAGON_BREATH_COOLDOWN;
    public static final ForgeConfigSpec.IntValue BERSERKER_STRENGTH_DURATION;
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("geneSkills");
        TELEPORT_RANGE = builder.defineInRange("teleportRange", 32, 1, 128);
        TELEPORT_COOLDOWN = builder.defineInRange("teleportCooldownTicks", 100, 1, 72000);
        DRAGON_BREATH_COOLDOWN = builder.defineInRange("dragonBreathCooldownTicks", 100, 1, 72000);
        BERSERKER_STRENGTH_DURATION = builder.defineInRange("berserkerStrengthDurationTicks", 200, 1, 72000);
        builder.pop(); SPEC = builder.build();
    }
    private GeneSkillConfig() {}
}

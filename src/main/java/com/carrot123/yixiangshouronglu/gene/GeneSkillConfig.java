package com.carrot123.yixiangshouronglu.gene;

import net.minecraftforge.common.ForgeConfigSpec;

public final class GeneSkillConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue TELEPORT_RANGE;
    public static final ForgeConfigSpec.IntValue TELEPORT_COOLDOWN;
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("geneSkills");
        TELEPORT_RANGE = builder.defineInRange("teleportRange", 32, 1, 128);
        TELEPORT_COOLDOWN = builder.defineInRange("teleportCooldownTicks", 100, 1, 72000);
        builder.pop(); SPEC = builder.build();
    }
    private GeneSkillConfig() {}
}

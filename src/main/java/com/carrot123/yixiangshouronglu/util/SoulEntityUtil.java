package com.carrot123.yixiangshouronglu.util;

import net.minecraft.resources.ResourceLocation;

public final class SoulEntityUtil {
    private SoulEntityUtil() {
    }

    public static boolean isAntlersWendigo(String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        return id != null
                && id.getNamespace().equals("antlers")
                && id.getPath().contains("wendigo");
    }

    public static boolean isMutantMoreZombie(String entityId) {
        return "mutantmore:mutant_jungle_zombie".equals(entityId)
                || "mutantmore:mutant_frozen_zombie".equals(entityId)
                || "mutantmore:mutant_husk".equals(entityId);
    }
}

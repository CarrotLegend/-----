package com.carrot123.yixiangshouronglu.gene;

import java.util.List;

public record GeneDisplay(List<AttributeLine> attributes, List<String> activeSkills, List<String> passiveSkills,
                          int cooldownTicks, List<String> conditions) {
    public record AttributeLine(String translationKey, double value) {}
    public static final GeneDisplay EMPTY = new GeneDisplay(List.of(), List.of(), List.of(), 0, List.of());
    public GeneDisplay {
        attributes = List.copyOf(attributes); activeSkills = List.copyOf(activeSkills);
        passiveSkills = List.copyOf(passiveSkills); conditions = List.copyOf(conditions);
        cooldownTicks = Math.max(0, cooldownTicks);
    }
}

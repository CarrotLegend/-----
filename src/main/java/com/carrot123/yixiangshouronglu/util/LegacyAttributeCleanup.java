package com.carrot123.yixiangshouronglu.util;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class LegacyAttributeCleanup {
    private LegacyAttributeCleanup() {
    }

    public static boolean removeFrom(AttributeInstance instance) {
        boolean removed = false;
        for (AttributeModifier.Operation operation : AttributeModifier.Operation.values()) {
            UUID id = UUID.nameUUIDFromBytes(("yixiangshouronglu:resonance_core:"
                    + instance.getAttribute().getDescriptionId() + ":" + operation.name())
                    .getBytes(StandardCharsets.UTF_8));
            if (instance.getModifier(id) != null) {
                instance.removeModifier(id);
                removed = true;
            }
        }
        return removed;
    }
}

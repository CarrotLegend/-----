package com.carrot123.yixiangshouronglu.util;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LegacyAttributeCleanupTest {
    private AttributeInstance health() {
        return new AttributeInstance(new RangedAttribute("attribute.name.generic.max_health", 20, 1, 1024), instance -> {});
    }

    @Test
    void removesLegacyModifierAndPreservesOtherSources() {
        AttributeInstance instance = health();
        UUID foreign = UUID.fromString("22222222-2222-2222-2222-222222222222");
        instance.setBaseValue(30);
        instance.addPermanentModifier(new AttributeModifier(UUID.fromString("915f17b6-545b-3c53-91c6-df9721a3faa4"), "ACC Resonance Core", 25, AttributeModifier.Operation.ADDITION));
        instance.addPermanentModifier(new AttributeModifier(foreign, "other source", 7, AttributeModifier.Operation.ADDITION));
        AttributeInstance restored = instance;
        assertTrue(LegacyAttributeCleanup.removeFrom(restored));
        assertEquals(30, restored.getBaseValue());
        assertEquals(37, restored.getValue());
        assertNotNull(restored.getModifier(foreign));
        assertFalse(LegacyAttributeCleanup.removeFrom(restored));
    }

    @Test
    void removesEveryLegacyOperationButDoesNotMatchByName() {
        AttributeInstance instance = health();
        String[] ids = {"915f17b6-545b-3c53-91c6-df9721a3faa4", "dbba8349-678f-349b-8247-d51d173db84a", "5a5aa284-5bec-39c1-bea2-c48fec09793f"};
        for (var operation : AttributeModifier.Operation.values()) {
            instance.addTransientModifier(new AttributeModifier(UUID.fromString(ids[operation.toValue()]), "legacy", 0.1, operation));
        }
        UUID foreign = UUID.fromString("33333333-3333-3333-3333-333333333333");
        instance.addPermanentModifier(new AttributeModifier(foreign, "ACC Resonance Core", 3, AttributeModifier.Operation.ADDITION));
        assertTrue(LegacyAttributeCleanup.removeFrom(instance));
        assertEquals(23, instance.getValue());
        assertNotNull(instance.getModifier(foreign));
    }
}

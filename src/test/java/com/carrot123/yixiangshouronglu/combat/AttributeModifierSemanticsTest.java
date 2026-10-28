package com.carrot123.yixiangshouronglu.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.junit.jupiter.api.Test;

class AttributeModifierSemanticsTest {
    @Test
    void standardMultiplyBaseModifierIncreasesDamageByItsPercentage() {
        AttributeInstance attribute = new AttributeInstance(
                new RangedAttribute("test.all_damage", 1.0D, 0.0D, 32767.0D),
                ignored -> {
                }
        );
        attribute.addTransientModifier(new AttributeModifier(
                UUID.fromString("83bf9df3-654c-41aa-960e-829862a01f41"),
                "test bonus",
                0.25D,
                AttributeModifier.Operation.MULTIPLY_BASE
        ));

        assertEquals(1.25D, attribute.getValue());
        assertEquals(10.0F, AllDamageMath.apply(8.0F, attribute.getValue()));
    }
}

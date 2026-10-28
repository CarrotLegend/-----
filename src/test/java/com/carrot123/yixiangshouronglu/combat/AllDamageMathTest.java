package com.carrot123.yixiangshouronglu.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class AllDamageMathTest {
    @Test
    void neutralMultiplierLeavesDamageUnchanged() {
        assertEquals(8.0F, AllDamageMath.apply(8.0F, 1.0D));
    }

    @Test
    void percentageModifiersUseTheAttributeFinalValue() {
        assertEquals(10.0F, AllDamageMath.apply(8.0F, 1.25D));
        assertEquals(16.0F, AllDamageMath.apply(8.0F, 2.0D));
    }

    @Test
    void invalidInputsDoNotIntroduceDamage() {
        assertEquals(8.0F, AllDamageMath.apply(8.0F, Double.NaN));
        assertEquals(8.0F, AllDamageMath.apply(8.0F, -1.0D));
        assertEquals(-2.0F, AllDamageMath.apply(-2.0F, 2.0D));
    }

    @Test
    void largeResultsAreClamped() {
        assertEquals(Float.MAX_VALUE, AllDamageMath.apply(Float.MAX_VALUE, 32767.0D));
    }
}

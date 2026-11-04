package com.carrot123.yixiangshouronglu.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeneSkillKeyLatchTest {
    @Test void heldRepeatsDoNotFireUntilRelease() {
        var latch = new GeneSkillKeyLatch();
        assertTrue(latch.press(true, true, true));
        for (int tick = 0; tick < 100; tick++) { assertFalse(latch.press(true, true, true)); }
        assertFalse(latch.press(true, false, true));
        assertTrue(latch.press(true, true, true));
    }
    @Test void tapBetweenTicksIsNotLost() {
        var latch = new GeneSkillKeyLatch();
        assertTrue(latch.press(true, false, true));
        assertFalse(latch.press(false, false, true));
        assertTrue(latch.press(true, false, true));
    }
    @Test void blockedScreensAndFocusConsumePresses() {
        var latch = new GeneSkillKeyLatch();
        assertFalse(latch.press(true, true, false));
        assertFalse(latch.press(true, true, true));
        assertFalse(latch.press(false, false, true));
        assertTrue(latch.press(true, true, true));
        assertFalse(latch.press(true, false, false));
        assertFalse(latch.press(false, false, true));
    }
}

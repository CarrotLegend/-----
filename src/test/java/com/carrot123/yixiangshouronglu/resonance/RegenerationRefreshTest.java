package com.carrot123.yixiangshouronglu.resonance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegenerationRefreshTest {
    @Test
    void preservesEveryVanillaHealingTickAcrossAllRefreshPhases() {
        for (int phase = 0; phase < 20; phase++) {
            int remaining = 45;
            int heals = 0;
            for (int tick = 1; tick <= 10000; tick++) {
                boolean expectedHealing = (45 - tick + 1) % 25 == 0;
                assertEquals(expectedHealing, remaining % 25 == 0,
                        "phase=" + phase + ", tick=" + tick);
                if (remaining % 25 == 0) {
                    heals++;
                }
                remaining--;
                if ((tick + phase) % 20 == 0) {
                    int refresh = RegenerationRefresh.nextDuration(1, 1, remaining);
                    if (refresh > 0) {
                        remaining = refresh;
                    }
                }
                assertTrue(remaining > 0 && remaining <= 45);
            }
            assertEquals(400, heals);
        }
    }

    @Test
    void initiallyAppliesRegenerationTwoAndUpgradesWeakerEffects() {
        assertEquals(45, RegenerationRefresh.nextDuration(1, -1, 0));
        assertEquals(45, RegenerationRefresh.nextDuration(1, 0, 1200));
    }

    @Test
    void extendsOnlyShortFiniteEffectsByOneHealingPeriod() {
        assertEquals(0, RegenerationRefresh.nextDuration(1, 1, 25));
        assertEquals(0, RegenerationRefresh.nextDuration(1, 1, 21));
        assertEquals(45, RegenerationRefresh.nextDuration(1, 1, 20));
        assertEquals(26, RegenerationRefresh.nextDuration(1, 1, 1));
        assertEquals(0, RegenerationRefresh.nextDuration(1, 1, -1));
        assertEquals(0, RegenerationRefresh.nextDuration(1, 3, 1));
    }

    @Test
    void keepsRegenerationFourRefreshBehavior() {
        assertEquals(0, RegenerationRefresh.nextDuration(3, 3, 26));
        assertEquals(45, RegenerationRefresh.nextDuration(3, 3, 25));
        assertEquals(45, RegenerationRefresh.nextDuration(3, 1, 1200));
        assertEquals(0, RegenerationRefresh.nextDuration(3, 3, -1));
    }

    @Test
    void expiresWithinFortyFiveTicksWhenResonanceStops() {
        for (int remaining = 1; remaining <= 45; remaining++) {
            int duration = RegenerationRefresh.nextDuration(1, 1, remaining);
            if (duration == 0) {
                duration = remaining;
            }
            int ticks = 0;
            while (duration > 0) {
                duration--;
                ticks++;
            }
            assertTrue(ticks <= 45);
        }
    }
}

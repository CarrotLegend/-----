package com.carrot123.yixiangshouronglu.gene;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RegenerativeTimingTest {
    @Test void qualitiesUseExplicitIntervalsAndAdditivePercentages() {
        assertEquals(List.of(40,30,20), java.util.Arrays.stream(GeneTypes.Quality.values()).map(RegenerativeCells::interval).toList());
        assertEquals(0.05, RegenerativeCells.overheal(GeneTypes.Quality.ALPHA), 0.000001);
        assertEquals(0.10, RegenerativeCells.overheal(GeneTypes.Quality.BETA), 0.000001);
        assertEquals(0.15, RegenerativeCells.overheal(GeneTypes.Quality.GAMMA), 0.000001);
    }
    @Test void periodicRenewalKeepsVanillaHealingPhase() {
        int duration=45; int heals=0; int previous=0;
        for(int tick=1;tick<=1000;tick++) {
            if(duration%25==0) { heals++; if(previous!=0) assertEquals(25,tick-previous); previous=tick; }
            duration--;
            int renewed=GeneRegeneration.renewedDuration(duration);
            if(renewed>0) { assertEquals(duration%25,renewed%25); duration=renewed; }
            assertTrue(duration>0 && duration<=45);
        }
        assertEquals(40,heals);
    }
    @Test void renewalDoesNotRewriteLongInfiniteOrExpiredEffects() {
        for(int duration:new int[]{-1,0,21,25,45,200}) assertEquals(0,GeneRegeneration.renewedDuration(duration));
        assertEquals(45,GeneRegeneration.renewedDuration(20));
        assertEquals(26,GeneRegeneration.renewedDuration(1));
    }
}

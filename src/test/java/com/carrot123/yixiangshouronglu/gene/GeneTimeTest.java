package com.carrot123.yixiangshouronglu.gene;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeneTimeTest {
    @Test void elapsedSecondsRoundDownAndRemainNonnegative() {
        assertEquals("0", GeneTime.seconds(-1));
        assertEquals("0", GeneTime.seconds(0));
        assertEquals("0", GeneTime.seconds(19));
        assertEquals("1", GeneTime.seconds(20));
        assertEquals("2", GeneTime.seconds(45));
        assertEquals("10", GeneTime.seconds(200));
        assertEquals("107374182", GeneTime.seconds(Integer.MAX_VALUE));
    }
    @Test void totalSecondsRoundUpWithoutOverflow() {
        assertEquals("0", GeneTime.totalSeconds(-1));
        assertEquals("0", GeneTime.totalSeconds(0));
        assertEquals("1", GeneTime.totalSeconds(1));
        assertEquals("1", GeneTime.totalSeconds(20));
        assertEquals("2", GeneTime.totalSeconds(21));
        assertEquals("10", GeneTime.totalSeconds(200));
        assertEquals("107374183", GeneTime.totalSeconds(Integer.MAX_VALUE));
    }
}

package com.carrot123.yixiangshouronglu.gene;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeneTimeTest {
    @Test void secondsAreBoundedAndDoNotDisplayTickUnits() {
        assertEquals("0", GeneTime.seconds(0));
        assertEquals("0", GeneTime.seconds(-1));
        assertEquals("0.05", GeneTime.seconds(1));
        assertEquals("1.5", GeneTime.seconds(30));
        assertEquals("10", GeneTime.seconds(200));
        assertEquals("107374182.35", GeneTime.seconds(Integer.MAX_VALUE));
    }
}

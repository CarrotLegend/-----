package com.carrot123.yixiangshouronglu.gene;

import java.math.BigDecimal;

public final class GeneTime {
    private GeneTime() {}
    public static String seconds(int ticks) { return BigDecimal.valueOf(Math.max(0, ticks)).divide(BigDecimal.valueOf(20)).stripTrailingZeros().toPlainString(); }
}

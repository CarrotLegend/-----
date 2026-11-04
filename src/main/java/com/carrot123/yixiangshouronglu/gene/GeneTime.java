package com.carrot123.yixiangshouronglu.gene;

public final class GeneTime {
    private GeneTime() {}
    public static String seconds(int ticks) { return Integer.toString(Math.max(0, ticks) / 20); }
    public static String totalSeconds(int ticks) { return Long.toString((Math.max(0L, ticks) + 19) / 20); }
}

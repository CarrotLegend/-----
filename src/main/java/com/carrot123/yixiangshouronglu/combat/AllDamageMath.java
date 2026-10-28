package com.carrot123.yixiangshouronglu.combat;

public final class AllDamageMath {
    private AllDamageMath() {
    }

    public static float apply(float amount, double multiplier) {
        if (!Float.isFinite(amount) || amount <= 0.0F
                || !Double.isFinite(multiplier) || multiplier < 0.0D) {
            return amount;
        }
        double result = (double) amount * multiplier;
        if (result >= Float.MAX_VALUE) {
            return Float.MAX_VALUE;
        }
        return (float) result;
    }
}

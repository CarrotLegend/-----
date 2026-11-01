package com.carrot123.yixiangshouronglu.resonance;

final class RegenerationRefresh {
    private RegenerationRefresh() {
    }

    static int nextDuration(int amplifier, int currentAmplifier, int remaining) {
        if (currentAmplifier < amplifier) {
            return 45;
        }
        if (currentAmplifier > amplifier || remaining == -1) {
            return 0;
        }
        if (amplifier == 1) {
            return remaining <= 20 ? remaining + 25 : 0;
        }
        return remaining <= 25 ? 45 : 0;
    }
}

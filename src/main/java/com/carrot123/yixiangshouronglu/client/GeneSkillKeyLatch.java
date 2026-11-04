package com.carrot123.yixiangshouronglu.client;

public final class GeneSkillKeyLatch {
    private boolean held;
    public boolean press(boolean clicked, boolean down, boolean allowed) {
        boolean fresh = clicked && !held;
        held = down;
        return fresh && allowed;
    }
}

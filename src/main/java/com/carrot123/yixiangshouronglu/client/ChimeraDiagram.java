package com.carrot123.yixiangshouronglu.client;

import net.minecraft.client.gui.GuiGraphics;

public final class ChimeraDiagram {
    private ChimeraDiagram() {}

    public static void line(GuiGraphics graphics, int x, int y, int endX, int endY, int progress, int duration) {
        int steps = Math.max(Math.abs(endX - x), Math.abs(endY - y));
        int fill = Math.max(0, Math.min(steps, steps * progress / duration));
        for (int step = 0; step <= steps; step++) {
            int px = x + (endX - x) * step / steps;
            int py = y + (endY - y) * step / steps;
            graphics.fill(px, py, px + 2, py + 2, step < fill ? 0xFF60CCD4 : 0xFF263B47);
        }
    }

    public static boolean onProgressLines(int x, int y) {
        int dx = Math.abs(x - 133);
        int dy = Math.abs(y - 63);
        return dx + dy > 18 && dx < 72 && Math.abs(dy * 81 - dx * 28) <= 324;
    }

    public static void diamond(GuiGraphics graphics, int x, int y) {
        for (int row = -17; row <= 17; row++) {
            int half = 17 - Math.abs(row);
            graphics.fill(x - half, y + row, x + half + 1, y + row + 1, 0xFF4B9CA7);
            if (half > 1) {
                graphics.fill(x - half + 1, y + row, x + half, y + row + 1, 0xFF080E15);
            }
        }
    }
}

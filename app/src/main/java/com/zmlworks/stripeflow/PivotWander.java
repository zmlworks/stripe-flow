package com.zmlworks.stripeflow;

/**
 * Smooth, deterministic wander of the rotation pivot, kept on screen.
 * A sum of slow incommensurate sines; no jumps from frame to frame.
 */
final class PivotWander {
    private PivotWander() {}

    static final float MARGIN = 0.08f;
    private static final float[] PX = {41f, 63f, 89f};
    private static final float[] PY = {47f, 71f, 97f};
    private static final float[] AX = {0.50f, 0.32f, 0.18f};
    private static final float[] AY = {0.50f, 0.32f, 0.18f};

    /** Writes the pivot (x, y) at time {@code t} seconds into {@code out[0], out[1]}. */
    static void position(float t, float width, float height, long seed, float[] out) {
        float nx = SlowNoise.wave(t, seed, PX, AX, 0x51);
        float ny = SlowNoise.wave(t, seed, PY, AY, 0xA3);
        float mx = Math.max(8f, width * MARGIN);
        float my = Math.max(8f, height * MARGIN);
        float rw = width - 2f * mx, rh = height - 2f * my;
        if (rw < 0f) rw = 0f;
        if (rh < 0f) rh = 0f;
        out[0] = mx + rw * (0.5f + 0.5f * nx);
        out[1] = my + rh * (0.5f + 0.5f * ny);
    }
}

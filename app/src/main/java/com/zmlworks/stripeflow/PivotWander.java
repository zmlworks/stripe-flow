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
        float nx = wave(t, seed, PX, AX, 0x51);
        float ny = wave(t, seed, PY, AY, 0xA3);
        float mx = Math.max(8f, width * MARGIN);
        float my = Math.max(8f, height * MARGIN);
        float rw = width - 2f * mx, rh = height - 2f * my;
        if (rw < 0f) rw = 0f;
        if (rh < 0f) rh = 0f;
        out[0] = mx + rw * (0.5f + 0.5f * nx);
        out[1] = my + rh * (0.5f + 0.5f * ny);
    }

    /** Value in [-1, 1]. */
    static float wave(float t, long seed, float[] periods, float[] amps, int tag) {
        float s = 0f;
        for (int i = 0; i < periods.length; i++) {
            float phase = phaseOf(seed, tag, i);
            s += amps[i] * (float) Math.sin(2.0 * Math.PI * t / periods[i] + phase);
        }
        if (s < -1f) return -1f;
        if (s > 1f) return 1f;
        return s;
    }

    private static float phaseOf(long seed, int tag, int i) {
        long x = seed ^ ((long) tag << 32) ^ (i * 0x9E3779B97F4A7C15L);
        x ^= (x >>> 30);
        x *= 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 27);
        x *= 0x94D049BB133111EBL;
        x ^= (x >>> 31);
        return (float) ((x & 0xFFFFFFL) / (double) 0x1000000L * 2.0 * Math.PI);
    }
}

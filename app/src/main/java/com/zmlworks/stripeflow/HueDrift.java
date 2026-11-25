package com.zmlworks.stripeflow;

/**
 * Slow, unwrapped hue wander for colour mode. A constant rate carries the
 * hue around the wheel; a small sum of slow sines adds irregularity.
 * At rate 0 the starting hue (from the seed) stays fixed.
 */
final class HueDrift {
    private HueDrift() {}

    /** Degrees per second. 2 °/s is one full cycle in 3 minutes. */
    static final float RATE_MIN = 0f, RATE_MAX = 36f, RATE_DEFAULT = 2f;
    static final float WOBBLE_DEG = 30f;
    static final float[] T = {59f, 83f, 113f};
    static final float[] A = {0.50f, 0.32f, 0.18f};
    private static final int TAG_H0 = 0xC01;
    private static final int TAG_W = 0xC10;

    static float startHue(long seed) {
        return (float) (SlowNoise.phaseOf(seed, TAG_H0, 0) * (180.0 / Math.PI));
    }

    /** Unwrapped hue in degrees at time {@code t} seconds. */
    static float hueDeg(float t, long seed, float rateDegPerSec) {
        float h0 = startHue(seed);
        if (rateDegPerSec <= 0f) return h0;
        if (t < 0f) t = 0f;
        float wobble = WOBBLE_DEG * SlowNoise.wave(t, seed, T, A, TAG_W);
        return h0 + rateDegPerSec * t + wobble;
    }
}

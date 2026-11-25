package com.zmlworks.stripeflow;

/**
 * Slow random drift for the "auto" mode: flow speed in [-20, 20] px/s and
 * rotation speed in [-5, 5] °/s. A monotone odd map is applied to the
 * sine-sum so the value passes quickly through zero and lingers toward
 * the ends of the range. The map's slope at zero is finite, so the
 * reversal stays smooth. Offset and angle are integrated in StripeView.
 */
final class SmoothDrift {
    private SmoothDrift() {}

    static final float FLOW_MAX = 20f;
    static final float ROT_MAX = 5f;

    /**
     * Shape parameter of {@code tanh(k u) / tanh(k)}.
     * k = 2.5 gives a finite slope of about 2.53 at 0, a clearly thinner
     * near-zero density, and more time near the ends of the range.
     */
    static final float SHAPE_K = 2.5f;
    private static final float SHAPE_DEN = (float) Math.tanh(SHAPE_K);

    /** Dominant periods, in seconds, for flow speed. */
    static final float[] FLOW_T = {37f, 53f, 79f};
    static final float[] FLOW_A = {0.50f, 0.32f, 0.18f};
    /** Dominant periods, in seconds, for rotation speed. */
    static final float[] ROT_T = {43f, 67f, 97f};
    static final float[] ROT_A = {0.50f, 0.32f, 0.18f};

    private static final int TAG_FLOW = 0xF10;
    private static final int TAG_ROT = 0xF20;

    /** Odd, monotone map of u in [-1, 1] onto [-1, 1], finite slope at 0. */
    static float shape(float u) {
        if (u <= -1f) return -1f;
        if (u >= 1f) return 1f;
        return (float) Math.tanh(SHAPE_K * u) / SHAPE_DEN;
    }

    /** Unshaped sine-sum in [-1, 1], used to compare time spent near zero. */
    static float rawFlowUnit(float t, long seed) {
        return SlowNoise.wave(t, seed, FLOW_T, FLOW_A, TAG_FLOW);
    }

    static float rawRotUnit(float t, long seed) {
        return SlowNoise.wave(t, seed, ROT_T, ROT_A, TAG_ROT);
    }

    static float flowSpeed(float t, long seed) {
        return FLOW_MAX * shape(rawFlowUnit(t, seed));
    }

    static float rotSpeed(float t, long seed) {
        return ROT_MAX * shape(rawRotUnit(t, seed));
    }

    /** Slope of {@link #shape} at 0: k / tanh(k). */
    static float shapeSlopeAtZero() {
        return SHAPE_K / SHAPE_DEN;
    }
}

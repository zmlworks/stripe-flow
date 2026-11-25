package com.zmlworks.stripeflow;

/**
 * Slow random drift for the "auto" mode: flow speed in [-20, 20] px/s and
 * rotation speed in [-5, 5] °/s. Periods are tens of seconds, and the sign
 * passes through zero smoothly, so direction reverses without a jump.
 * Offset and angle must be taken from the integrals, not speed * t.
 */
final class SmoothDrift {
    private SmoothDrift() {}

    static final int WIDTH = 8;
    static final float FLOW_MAX = 20f;
    static final float ROT_MAX = 5f;

    /** Dominant periods, in seconds, for flow speed. */
    static final float[] FLOW_T = {37f, 53f, 79f};
    static final float[] FLOW_A = {0.50f, 0.32f, 0.18f};
    /** Dominant periods, in seconds, for rotation speed. */
    static final float[] ROT_T = {43f, 67f, 97f};
    static final float[] ROT_A = {0.50f, 0.32f, 0.18f};

    private static final int TAG_FLOW = 0xF10;
    private static final int TAG_ROT = 0xF20;

    static float flowSpeed(float t, long seed) {
        return FLOW_MAX * SlowNoise.wave(t, seed, FLOW_T, FLOW_A, TAG_FLOW);
    }

    static float rotSpeed(float t, long seed) {
        return ROT_MAX * SlowNoise.wave(t, seed, ROT_T, ROT_A, TAG_ROT);
    }

    /** ∫ flowSpeed dτ from 0 to t, in pixels along the normal. */
    static float flowIntegral(float t, long seed) {
        return FLOW_MAX * SlowNoise.integral(t, seed, FLOW_T, FLOW_A, TAG_FLOW);
    }

    /** ∫ rotSpeed dτ from 0 to t, in degrees (clockwise positive on screen). */
    static float rotIntegral(float t, long seed) {
        return ROT_MAX * SlowNoise.integral(t, seed, ROT_T, ROT_A, TAG_ROT);
    }
}

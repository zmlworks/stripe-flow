package com.zmlworks.stripeflow;

/**
 * Smooth, deterministic noise used for the wandering pivot and for the
 * auto-drift speeds. A sum of slow incommensurate sines; values and their
 * time integrals are closed form, so there are no jumps from frame to frame.
 */
final class SlowNoise {
    private SlowNoise() {}

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

    /** Integral of {@link #wave} from 0 to {@code t}. */
    static float integral(float t, long seed, float[] periods, float[] amps, int tag) {
        double s = 0;
        for (int i = 0; i < periods.length; i++) {
            double w = 2.0 * Math.PI / periods[i];
            double phase = phaseOf(seed, tag, i);
            s += amps[i] * (-Math.cos(w * t + phase) + Math.cos(phase)) / w;
        }
        return (float) s;
    }

    static float phaseOf(long seed, int tag, int i) {
        long x = seed ^ ((long) tag << 32) ^ (i * 0x9E3779B97F4A7C15L);
        x ^= (x >>> 30);
        x *= 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 27);
        x *= 0x94D049BB133111EBL;
        x ^= (x >>> 31);
        return (float) ((x & 0xFFFFFFL) / (double) 0x1000000L * 2.0 * Math.PI);
    }
}

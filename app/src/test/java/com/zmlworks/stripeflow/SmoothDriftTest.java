package com.zmlworks.stripeflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Bounds, smoothness, shaping, and near-zero time of the auto-drift speeds. */
public class SmoothDriftTest {
    private static final long[] SEEDS = {1L, 7L, 42L, 99L, 123456789L};

    /** Analytical |d(wave)/dt| bound: Σ a_i · 2π / T_i. */
    private static float maxAbsDeriv(float scale, float[] periods, float[] amps) {
        float s = 0f;
        for (int i = 0; i < periods.length; i++) {
            s += amps[i] * (float) (2.0 * Math.PI / periods[i]);
        }
        return scale * s;
    }

    @Test public void shapeIsOddMonotoneAndHasFiniteSlopeAtZero() {
        assertEquals(0f, SmoothDrift.shape(0f), 0f);
        assertEquals(1f, SmoothDrift.shape(1f), 0f);
        assertEquals(-1f, SmoothDrift.shape(-1f), 0f);
        for (int i = 1; i < 50; i++) {
            float u = i / 50f;
            float v = SmoothDrift.shape(u);
            assertEquals(-v, SmoothDrift.shape(-u), 1e-6f);
            assertTrue(v > SmoothDrift.shape(u - 0.02f));
            assertTrue(v >= u); // pushed outward from zero
        }
        float eps = 1e-3f;
        float slope = SmoothDrift.shape(eps) / eps;
        assertEquals(SmoothDrift.shapeSlopeAtZero(), slope, 0.01f);
        assertTrue("slope at 0 should be finite and > 1, was " + slope,
                slope > 2f && slope < 4f);
    }

    @Test public void flowAndRotationStayInsideTheDocumentedRanges() {
        float flowMax = 0f, rotMax = 0f;
        boolean flowPos = false, flowNeg = false, rotPos = false, rotNeg = false;
        for (long seed : SEEDS) {
            for (int i = 0; i <= 8000; i++) {
                float t = i * 0.125f;
                float f = SmoothDrift.flowSpeed(t, seed);
                float r = SmoothDrift.rotSpeed(t, seed);
                assertTrue("flow " + f + " t=" + t + " seed=" + seed,
                        f >= -SmoothDrift.FLOW_MAX - 1e-5f && f <= SmoothDrift.FLOW_MAX + 1e-5f);
                assertTrue("rot " + r + " t=" + t + " seed=" + seed,
                        r >= -SmoothDrift.ROT_MAX - 1e-5f && r <= SmoothDrift.ROT_MAX + 1e-5f);
                float af = Math.abs(f), ar = Math.abs(r);
                if (af > flowMax) flowMax = af;
                if (ar > rotMax) rotMax = ar;
                if (f > 1f) flowPos = true;
                if (f < -1f) flowNeg = true;
                if (r > 0.3f) rotPos = true;
                if (r < -0.3f) rotNeg = true;
            }
        }
        assertTrue("flow max " + flowMax, flowMax > 15f);
        assertTrue("rot max " + rotMax, rotMax > 3.5f);
        assertTrue(flowPos && flowNeg && rotPos && rotNeg);
    }

    @Test public void ratesOfChangeStayWithinTheShapedSineSumBound() {
        float flowBound = SmoothDrift.shapeSlopeAtZero()
                * maxAbsDeriv(SmoothDrift.FLOW_MAX, SmoothDrift.FLOW_T, SmoothDrift.FLOW_A);
        float rotBound = SmoothDrift.shapeSlopeAtZero()
                * maxAbsDeriv(SmoothDrift.ROT_MAX, SmoothDrift.ROT_T, SmoothDrift.ROT_A);
        float dt = 1f / 60f;
        float flowStep = 0f, rotStep = 0f;
        for (long seed : SEEDS) {
            float pf = SmoothDrift.flowSpeed(0f, seed);
            float pr = SmoothDrift.rotSpeed(0f, seed);
            for (int i = 1; i <= 60 * 180; i++) {
                float t = i * dt;
                float f = SmoothDrift.flowSpeed(t, seed);
                float r = SmoothDrift.rotSpeed(t, seed);
                float df = Math.abs(f - pf) / dt;
                float dr = Math.abs(r - pr) / dt;
                if (df > flowStep) flowStep = df;
                if (dr > rotStep) rotStep = dr;
                pf = f;
                pr = r;
            }
        }
        assertTrue("flow rate " + flowStep + " bound " + flowBound, flowStep <= flowBound + 0.1f);
        assertTrue("rot rate " + rotStep + " bound " + rotBound, rotStep <= rotBound + 0.05f);
        assertTrue("flow bound should stay modest, was " + flowBound, flowBound < 10f);
        assertTrue("rot bound should stay modest, was " + rotBound, rotBound < 3f);
    }

    /**
     * Fraction of samples with |v| &lt; 20% of the range, before and after
     * shaping. The shaped density near zero must be clearly thinner.
     */
    @Test public void nearZeroTimeIsClearlyThinnerAfterShaping() {
        int rawFlow = 0, shapedFlow = 0, rawRot = 0, shapedRot = 0, n = 0;
        for (long seed : SEEDS) {
            for (int i = 0; i <= 8000; i++) {
                float t = i * 0.125f;
                float uf = SmoothDrift.rawFlowUnit(t, seed);
                float ur = SmoothDrift.rawRotUnit(t, seed);
                if (Math.abs(uf) < 0.2f) rawFlow++;
                if (Math.abs(SmoothDrift.shape(uf)) < 0.2f) shapedFlow++;
                if (Math.abs(ur) < 0.2f) rawRot++;
                if (Math.abs(SmoothDrift.shape(ur)) < 0.2f) shapedRot++;
                n++;
            }
        }
        float beforeFlow = rawFlow / (float) n;
        float afterFlow = shapedFlow / (float) n;
        float beforeRot = rawRot / (float) n;
        float afterRot = shapedRot / (float) n;
        System.out.println("near-zero |v|<20% of max: flow before=" + beforeFlow
                + " after=" + afterFlow + " rot before=" + beforeRot + " after=" + afterRot);
        assertTrue("flow before " + beforeFlow, beforeFlow > 0.28f && beforeFlow < 0.36f);
        assertTrue("flow after " + afterFlow, afterFlow > 0.10f && afterFlow < 0.16f);
        assertTrue("flow after should be clearly thinner", afterFlow < 0.5f * beforeFlow);
        assertTrue("rot before " + beforeRot, beforeRot > 0.28f && beforeRot < 0.36f);
        assertTrue("rot after " + afterRot, afterRot > 0.10f && afterRot < 0.16f);
        assertTrue("rot after should be clearly thinner", afterRot < 0.5f * beforeRot);
    }
}

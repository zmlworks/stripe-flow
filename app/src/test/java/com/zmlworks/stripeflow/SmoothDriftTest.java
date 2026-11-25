package com.zmlworks.stripeflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Bounds, smoothness and closed-form integrals of the auto-drift speeds. */
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
        // The ranges should actually be used, including both signs.
        assertTrue("flow max " + flowMax, flowMax > 15f);
        assertTrue("rot max " + rotMax, rotMax > 3.5f);
        assertTrue(flowPos && flowNeg && rotPos && rotNeg);
    }

    @Test public void ratesOfChangeStayWithinTheSineSumBound() {
        float flowBound = maxAbsDeriv(SmoothDrift.FLOW_MAX, SmoothDrift.FLOW_T, SmoothDrift.FLOW_A);
        float rotBound = maxAbsDeriv(SmoothDrift.ROT_MAX, SmoothDrift.ROT_T, SmoothDrift.ROT_A);
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
        // Finite differences sit slightly below the analytical max.
        assertTrue("flow rate " + flowStep + " bound " + flowBound, flowStep <= flowBound + 0.05f);
        assertTrue("rot rate " + rotStep + " bound " + rotBound, rotStep <= rotBound + 0.02f);
        assertTrue("flow bound should be a few px/s², was " + flowBound, flowBound < 4f);
        assertTrue("rot bound should be under 1 °/s², was " + rotBound, rotBound < 1f);
    }

    @Test public void closedFormIntegralsMatchATrapezoidSum() {
        float dt = 1f / 120f;
        for (long seed : SEEDS) {
            float flowAcc = 0f, rotAcc = 0f;
            float pf = SmoothDrift.flowSpeed(0f, seed);
            float pr = SmoothDrift.rotSpeed(0f, seed);
            for (int i = 1; i <= 120 * 200; i++) {
                float t = i * dt;
                float f = SmoothDrift.flowSpeed(t, seed);
                float r = SmoothDrift.rotSpeed(t, seed);
                flowAcc += 0.5f * (pf + f) * dt;
                rotAcc += 0.5f * (pr + r) * dt;
                pf = f;
                pr = r;
                if (i % 2400 == 0) {
                    assertEquals("flow integral t=" + t + " seed=" + seed,
                            SmoothDrift.flowIntegral(t, seed), flowAcc, 0.05f);
                    assertEquals("rot integral t=" + t + " seed=" + seed,
                            SmoothDrift.rotIntegral(t, seed), rotAcc, 0.02f);
                }
            }
        }
    }

    @Test public void integralsAreContinuousAndStartAtZero() {
        for (long seed : SEEDS) {
            assertEquals(0f, SmoothDrift.flowIntegral(0f, seed), 0f);
            assertEquals(0f, SmoothDrift.rotIntegral(0f, seed), 0f);
            float pf = 0f, pr = 0f;
            float dt = 1f / 60f;
            for (int i = 1; i <= 60 * 60; i++) {
                float t = i * dt;
                float f = SmoothDrift.flowIntegral(t, seed);
                float r = SmoothDrift.rotIntegral(t, seed);
                assertTrue(Math.abs(f - pf) <= SmoothDrift.FLOW_MAX * dt + 0.01f);
                assertTrue(Math.abs(r - pr) <= SmoothDrift.ROT_MAX * dt + 0.01f);
                pf = f;
                pr = r;
            }
        }
    }
}

package com.zmlworks.stripeflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScalesTest {

    @Test public void defaultSpeedSitsOnTheSliderAndRoundTrips() {
        int p = Scales.progressFromSpeed(3);
        assertEquals(3, Scales.speedFromProgress(p));
        assertTrue("default 3 should not sit on the first 5% of the bar", p > Scales.STEPS / 20);
        assertTrue(p < Scales.STEPS / 2);
    }

    @Test public void speedEndsAndMonotonic() {
        assertEquals(0, Scales.speedFromProgress(0));
        assertEquals(Prefs.SPEED_MAX, Scales.speedFromProgress(Scales.STEPS));
        int prev = -1;
        for (int p = 0; p <= Scales.STEPS; p += 10) {
            int v = Scales.speedFromProgress(p);
            assertTrue(v >= prev);
            prev = v;
        }
    }

    @Test public void defaultRotationRoundTripsClosely() {
        int p = Scales.progressFromRot(Prefs.ROT_DEFAULT);
        float back = Scales.rotFromProgress(p);
        assertEquals(Prefs.ROT_DEFAULT, back, 0.05f);
        assertTrue(p > 0);
        assertTrue(p < Scales.STEPS / 2);
    }

    @Test public void rotEndsAndLowEndIsFineGrained() {
        assertEquals(0f, Scales.rotFromProgress(0), 0f);
        assertEquals(Prefs.ROT_MAX, Scales.rotFromProgress(Scales.STEPS), 0.01f);
        // First 20% of the slider stays under 1 °/s so slow motion is settable.
        assertTrue(Scales.rotFromProgress(Scales.STEPS / 5) < 1f);
    }

    @Test public void distScaleIsLogarithmicAndDefaultsToOne() {
        assertEquals(Prefs.DIST_MIN, Scales.distFromProgress(0), 1e-5f);
        assertEquals(Prefs.DIST_MAX, Scales.distFromProgress(Scales.STEPS), 1e-4f);
        assertEquals(1f, Scales.distFromProgress(Scales.progressFromDist(1f)), 0.02f);
        int mid = Scales.STEPS / 2;
        float midV = Scales.distFromProgress(mid);
        // Geometric mid of 0.1 and 5 is sqrt(0.5) ≈ 0.707, not the arithmetic mid 2.55.
        assertTrue("log mid " + midV, midV > 0.5f && midV < 1.0f);
        assertEquals(Scales.progressFromDist(0.1f), 0);
    }

    @Test public void hueRateCubeIncludesZeroAndDefault() {
        assertEquals(0f, Scales.hueRateFromProgress(0), 0f);
        assertEquals(Prefs.HUE_RATE_MAX, Scales.hueRateFromProgress(Scales.STEPS), 0.01f);
        float back = Scales.hueRateFromProgress(Scales.progressFromHueRate(Prefs.HUE_RATE_DEFAULT));
        assertEquals(Prefs.HUE_RATE_DEFAULT, back, 0.05f);
        assertTrue(Scales.progressFromHueRate(Prefs.HUE_RATE_DEFAULT) < Scales.STEPS / 2);
    }

    @Test public void aaEdgeIsLinearAndRoundTrips() {
        assertEquals(Prefs.AA_EDGE_MIN, Scales.aaEdgeFromProgress(0), 1e-5f);
        assertEquals(Prefs.AA_EDGE_MAX, Scales.aaEdgeFromProgress(Scales.STEPS), 1e-5f);
        assertEquals(1f, Scales.aaEdgeFromProgress(Scales.progressFromAaEdge(1f)), 1e-3f);
        assertEquals(0.3f, Scales.aaEdgeFromProgress(Scales.progressFromAaEdge(0.3f)), 1e-3f);
        assertEquals(0.1f, Scales.aaEdgeFromProgress(Scales.progressFromAaEdge(0.1f)), 1e-3f);
    }
}

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
}

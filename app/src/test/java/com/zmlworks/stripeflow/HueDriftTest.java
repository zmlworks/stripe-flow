package com.zmlworks.stripeflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HueDriftTest {
    private static final long[] SEEDS = {1L, 7L, 42L, 99L, 123456789L};

    @Test public void zeroRateStaysOnTheStartingHue() {
        for (long seed : SEEDS) {
            float h0 = HueDrift.startHue(seed);
            assertTrue(h0 >= 0f && h0 < 360f);
            for (int i = 0; i <= 600; i++) {
                assertEquals(h0, HueDrift.hueDeg(i * 0.25f, seed, 0f), 0f);
            }
        }
    }

    @Test public void unwrappedHueMovesSmoothlyAndCoversTheWheel() {
        float dt = 1f / 60f;
        float waveBound = 0f;
        for (int i = 0; i < HueDrift.T.length; i++) {
            waveBound += HueDrift.A[i] * (float) (2.0 * Math.PI / HueDrift.T[i]);
        }
        float rate = HueDrift.RATE_DEFAULT;
        float maxDh = rate + HueDrift.WOBBLE_DEG * waveBound + 0.05f;
        for (long seed : SEEDS) {
            float prev = HueDrift.hueDeg(0f, seed, rate);
            float min = prev, max = prev;
            for (int i = 1; i <= 60 * 240; i++) {
                float t = i * dt;
                float h = HueDrift.hueDeg(t, seed, rate);
                float dh = Math.abs(h - prev) / dt;
                assertTrue("jump " + dh + " at t=" + t + " seed=" + seed, dh <= maxDh);
                if (h < min) min = h;
                if (h > max) max = h;
                prev = h;
            }
            // A 4-minute run at 2 °/s plus a 30° wobble covers more than a full turn.
            assertTrue("span " + (max - min) + " seed=" + seed, max - min > 360f);
        }
    }

    @Test public void startHueIsDeterministic() {
        assertEquals(HueDrift.startHue(7L), HueDrift.startHue(7L), 0f);
        assertTrue(HueDrift.startHue(7L) != HueDrift.startHue(8L)
                || HueDrift.startHue(1L) != HueDrift.startHue(99L));
    }

    @Test public void suppliedStartHueIsUsedAndStaysPutWhenRateIsZero() {
        float h0 = 120f;
        for (long seed : SEEDS) {
            assertEquals(h0, HueDrift.hueDeg(0f, seed, 0f, h0), 0f);
            assertEquals(h0, HueDrift.hueDeg(30f, seed, 0f, h0), 0f);
            assertTrue(HueDrift.hueDeg(30f, seed, 2f, h0) > h0 + 40f);
        }
    }
}

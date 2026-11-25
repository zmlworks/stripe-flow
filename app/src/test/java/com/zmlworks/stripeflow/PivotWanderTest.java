package com.zmlworks.stripeflow;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PivotWanderTest {

    @Test public void staysOnScreenWithMargin() {
        float w = 800, h = 1200;
        float mx = Math.max(8f, w * PivotWander.MARGIN);
        float my = Math.max(8f, h * PivotWander.MARGIN);
        float[] p = new float[2];
        for (int i = 0; i <= 400; i++) {
            PivotWander.position(i * 0.25f, w, h, 42L, p);
            assertTrue("x=" + p[0], p[0] >= mx - 0.01f && p[0] <= w - mx + 0.01f);
            assertTrue("y=" + p[1], p[1] >= my - 0.01f && p[1] <= h - my + 0.01f);
        }
    }

    @Test public void movesSmoothlyAndDoesNotStayFixed() {
        float[] a = new float[2], b = new float[2];
        float maxStep = 0f, travel = 0f;
        PivotWander.position(0f, 800, 1200, 7L, a);
        for (int i = 1; i <= 200; i++) {
            float t = i * (1f / 60f);
            PivotWander.position(t, 800, 1200, 7L, b);
            float dx = b[0] - a[0], dy = b[1] - a[1];
            float step = (float) Math.hypot(dx, dy);
            if (step > maxStep) maxStep = step;
            travel += step;
            a[0] = b[0]; a[1] = b[1];
        }
        // At 60 Hz the pivot should crawl, not jump tens of pixels, but it must move.
        assertTrue("max step " + maxStep, maxStep < 20f);
        assertTrue("travel " + travel, travel > 30f);
    }

    @Test public void sameSeedAndTimeAreDeterministic() {
        float[] a = new float[2], b = new float[2];
        PivotWander.position(3.25f, 500, 700, 99L, a);
        PivotWander.position(3.25f, 500, 700, 99L, b);
        assertTrue(a[0] == b[0] && a[1] == b[1]);
    }
}

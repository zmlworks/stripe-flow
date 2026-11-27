package com.zmlworks.stripeflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Color;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class StripePatternTest {
    static final float ANG0 = 0f;
    static final float ANG45 = StripePattern.BASE_ANGLE_RAD;
    static final float ANG90 = (float) (Math.PI / 2.0);

    @Test public void periodIsTheSumOfTheTwoWidths() {
        assertEquals(2, StripePattern.period(1, 1));
        assertEquals(4, StripePattern.period(2, 2));
        assertEquals(20, StripePattern.period(10, 10));
        assertEquals(128, StripePattern.period(64, 64));
        assertEquals(2, StripePattern.period(0, 0));
        assertEquals(8, StripePattern.period(3, 5));
        assertEquals(9, StripePattern.period(8, 1));
    }

    @Test public void verticalStripesAtAngleZero() {
        // Normal along +x, pivot at origin: colour depends only on x + offset.
        assertEquals(Color.BLACK, StripePattern.color(0, 0, 0, 0, ANG0, 0f, 1, 1));
        assertEquals(Color.WHITE, StripePattern.color(1, 0, 0, 0, ANG0, 0f, 1, 1));
        assertEquals(Color.BLACK, StripePattern.color(2, 5, 0, 0, ANG0, 0f, 1, 1));
        assertEquals(Color.BLACK, StripePattern.color(0, 9, 0, 0, ANG0, 0f, 1, 1));
        assertEquals(Color.BLACK, StripePattern.color(0, 0, 0, 0, ANG0, 0f, 4, 4));
        assertEquals(Color.BLACK, StripePattern.color(3, 0, 0, 0, ANG0, 0f, 4, 4));
        assertEquals(Color.WHITE, StripePattern.color(4, 0, 0, 0, ANG0, 0f, 4, 4));
        assertEquals(Color.WHITE, StripePattern.color(7, 0, 0, 0, ANG0, 0f, 4, 4));
        assertEquals(Color.BLACK, StripePattern.color(8, 0, 0, 0, ANG0, 0f, 4, 4));
    }

    @Test public void horizontalStripesAtAngle90() {
        // Independent of x, changes along y (width 8 so the band is unmistakable).
        assertEquals(StripePattern.color(0, 0, 0, 0, ANG90, 0f, 8, 8),
                StripePattern.color(40, 0, 0, 0, ANG90, 0f, 8, 8));
        assertEquals(Color.BLACK, StripePattern.color(0, 0, 0, 0, ANG90, 0f, 8, 8));
        assertEquals(Color.BLACK, StripePattern.color(0, 7, 0, 0, ANG90, 0f, 8, 8));
        assertEquals(Color.WHITE, StripePattern.color(0, 8, 0, 0, ANG90, 0f, 8, 8));
        assertEquals(Color.WHITE, StripePattern.color(15, 8, 0, 0, ANG90, 0f, 8, 8));
    }

    @Test public void onlyBlackAndWhiteAtManyAngles() {
        float[] angles = {0f, ANG45, ANG90, 1.1f, 2.2f, (float) Math.PI, 4f};
        for (int w : new int[]{1, 2, 8, 64}) {
            for (float a : angles) {
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        int c = StripePattern.color(x, y, 7.5f, 6f, a, 0.3f, w, w);
                        assertTrue(c == Color.BLACK || c == Color.WHITE);
                    }
                }
            }
        }
    }

    @Test public void colourAtThePivotDependsOnlyOnOffset() {
        int a = StripePattern.color(10, 20, 10, 20, ANG45, 0.4f, 3, 3);
        int b = StripePattern.color(10, 20, 10, 20, ANG0, 0.4f, 3, 3);
        int c = StripePattern.color(10, 20, 10, 20, 2.7f, 0.4f, 3, 3);
        assertEquals(a, b);
        assertEquals(a, c);
        int flipped = StripePattern.color(10, 20, 10, 20, ANG45, 0.4f + 3f, 3, 3);
        assertTrue(a != flipped);
    }

    @Test public void anglePlusTwoPiAndOffsetPlusPeriodAreIdentical() {
        Bitmap a = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        Bitmap b = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        StripePattern.render(a, 5, 5, 16, 16, 0.8f, 1.2f);
        StripePattern.render(b, 5, 5, 16, 16, 0.8f + (float) (2 * Math.PI), 1.2f + StripePattern.period(5, 5));
        assertSamePixels(a, b);
    }

    @Test public void integerOffsetShiftsVerticalPatternByOne() {
        assertEquals(
                StripePattern.color(0, 0, 0, 0, ANG0, 1f, 1, 1),
                StripePattern.color(1, 0, 0, 0, ANG0, 0f, 1, 1));
        assertEquals(
                StripePattern.color(3, 8, 0, 0, ANG0, 1f, 1, 1),
                StripePattern.color(4, 8, 0, 0, ANG0, 0f, 1, 1));
    }

    @Test public void phaseIsContinuousWhenTheAngleChanges() {
        // At a point 20 px from the pivot, a 0.001 rad step changes phase by
        // about 0.02 px, never a wrap of the period.
        float px = 50, py = 40, x = 70, y = 40;
        float prev = StripePattern.phase(x, y, px, py, 0f, 3f);
        for (int i = 1; i <= 200; i++) {
            float ang = i * 0.001f;
            float ph = StripePattern.phase(x, y, px, py, ang, 3f);
            assertTrue("jump at " + ang + ": " + prev + " -> " + ph,
                    Math.abs(ph - prev) < 0.1f);
            prev = ph;
        }
    }

    @Test public void pivotCompensationLeavesTheFieldUnchangedWhenAngleIsFixed() {
        int w = 4;
        float ang = ANG45, off = 1.3f;
        float p1x = 20, p1y = 30, p2x = 55, p2y = 12;
        float off2 = off + StripePattern.pivotCompDelta(p1x, p1y, p2x, p2y, ang);
        Bitmap a = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888);
        Bitmap b = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888);
        StripePattern.render(a, w, w, p1x, p1y, ang, off);
        StripePattern.render(b, w, w, p2x, p2y, ang, off2);
        assertSamePixels(a, b);
    }

    @Test public void compensationKeepsThePivotPixelStableAcrossRotation() {
        // Integrate a short motion: angle changes and the pivot wanders.
        // The colour at the *current* pivot must follow only the flow offset.
        int width = 6;
        float speed = 5f;
        float[] p = new float[2], prev = new float[2];
        float comp = 0f;
        boolean have = false;
        for (int i = 0; i <= 120; i++) {
            float t = i / 60f;
            float ang = ANG45 + (float) Math.toRadians(20) * t;
            PivotWander.position(t, 200, 200, 11L, p);
            if (!have) {
                prev[0] = p[0]; prev[1] = p[1];
                have = true;
            } else {
                comp += StripePattern.pivotCompDelta(prev[0], prev[1], p[0], p[1], ang);
                prev[0] = p[0]; prev[1] = p[1];
            }
            float off = speed * t + comp;
            int atPivot = StripePattern.color(p[0], p[1], p[0], p[1], ang, off, width, width);
            int fromFlow = StripePattern.colorFromPhase(off, width, width);
            assertEquals("t=" + t, fromFlow, atPivot);
        }
    }

    @Test public void tileMatchesOneDimensionalBands() {
        for (int w = 1; w <= 16; w++) {
            Bitmap tile = StripePattern.tile(w, w);
            assertEquals(Bitmap.DENSITY_NONE, tile.getDensity());
            assertEquals(StripePattern.period(w, w), tile.getWidth());
            for (int y = 0; y < tile.getHeight(); y++) {
                for (int x = 0; x < tile.getWidth(); x++) {
                    assertEquals(StripePattern.colorFromPhase(x, w, w), tile.getPixel(x, y));
                }
            }
        }
    }

    @Test public void unequalWidthsHaveTheExpectedBands() {
        // Along +x at angle 0: [0, b) black, [b, a+b) white, then it repeats.
        int a = 3, b = 5;
        assertEquals(Color.BLACK, StripePattern.color(0, 0, 0, 0, ANG0, 0f, a, b));
        assertEquals(Color.BLACK, StripePattern.color(4, 10, 0, 0, ANG0, 0f, a, b));
        assertEquals(Color.WHITE, StripePattern.color(5, 0, 0, 0, ANG0, 0f, a, b));
        assertEquals(Color.WHITE, StripePattern.color(7, 0, 0, 0, ANG0, 0f, a, b));
        assertEquals(Color.BLACK, StripePattern.color(8, 0, 0, 0, ANG0, 0f, a, b));
        Bitmap tile = StripePattern.tile(a, b);
        assertEquals(8, tile.getWidth());
        assertEquals(Color.BLACK, tile.getPixel(0, 3));
        assertEquals(Color.BLACK, tile.getPixel(4, 3));
        assertEquals(Color.WHITE, tile.getPixel(5, 3));
        assertEquals(Color.WHITE, tile.getPixel(7, 3));
        // Adding the period to the offset leaves the field unchanged.
        assertEquals(
                StripePattern.color(11, 2, 0, 0, ANG0, 0.4f, a, b),
                StripePattern.color(11, 2, 0, 0, ANG0, 0.4f + StripePattern.period(a, b), a, b));
    }

    @Test public void flowOffsetWrapsAndRespectsDirection() {
        assertEquals(0f, StripePattern.flowOffset(0L, 3f, -1, 1, 1), 0f);
        assertEquals(1f, StripePattern.flowOffset(1_000_000_000L, 3f, -1, 1, 1), 1e-5f);
        long sixth = 1_000_000_000L / 6;
        float a = StripePattern.flowOffset(sixth, 3f, 1, 1, 1);
        float b = StripePattern.flowOffset(sixth, 3f, -1, 1, 1);
        assertEquals(0.5f, a, 1e-5f);
        assertEquals(1.5f, b, 1e-5f);
    }

    private static void assertSamePixels(Bitmap a, Bitmap b) {
        int n = a.getWidth() * a.getHeight();
        int[] pa = new int[n], pb = new int[n];
        a.getPixels(pa, 0, a.getWidth(), 0, 0, a.getWidth(), a.getHeight());
        b.getPixels(pb, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());
        for (int i = 0; i < n; i++) assertEquals("i=" + i, pa[i], pb[i]);
    }

    @Test public void complementaryHuesAreExactly180ApartAndPure() {
        float[] hsv = new float[3], hsvC = new float[3];
        float[] samples = {0f, 17f, 60f, 120f, 179.5f, 200f, 300f, 359.2f, -40f, 720f};
        for (float h : samples) {
            int c0 = StripePattern.colorAtHue(h, hsv);
            int c1 = StripePattern.colorAtHue(h + 180f, hsvC);
            android.graphics.Color.colorToHSV(c0, hsv);
            android.graphics.Color.colorToHSV(c1, hsvC);
            assertEquals(1f, hsv[1], 0.001f);
            assertEquals(1f, hsv[2], 0.001f);
            assertEquals(1f, hsvC[1], 0.001f);
            assertEquals(1f, hsvC[2], 0.001f);
            float d = Math.abs(StripePattern.wrapHue(hsv[0]) - StripePattern.wrapHue(hsvC[0]));
            d = Math.min(d, 360f - d);
            assertEquals("h=" + h, 180f, d, 0.5f);
            assertEquals(StripePattern.wrapHue(h + 180f), StripePattern.complementHue(h), 0.001f);
        }
    }
}

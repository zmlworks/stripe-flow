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

    @Test public void hardTileStaysBlackAndWhite() {
        Bitmap tile = StripePattern.tile(3, 5);
        assertEquals(8, tile.getWidth());
        assertEquals(8, tile.getHeight());
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int c = tile.getPixel(x, y);
                assertTrue(c == Color.BLACK || c == Color.WHITE);
                assertEquals(StripePattern.colorFromPhase(x, 3, 5), c);
            }
        }
    }

    @Test public void aaTileHasAOnePixelRampAndSeamlessRepeat() {
        int a = 3, b = 5;
        float period = 8f;
        float half = StripePattern.aaHalf(a, b);
        assertEquals(0.5f, half, 0f);
        assertEquals(0.5f, StripePattern.coverage(0f, a, b, half), 1e-5f);
        assertEquals(0.5f, StripePattern.coverage(b, a, b, half), 1e-5f);
        assertEquals(0.5f, StripePattern.coverage(period, a, b, half), 1e-5f);
        assertEquals(0f, StripePattern.coverage(2.5f, a, b, half), 1e-5f);
        assertEquals(1f, StripePattern.coverage(6.5f, a, b, half), 1e-5f);
        // One period away is identical, including the wrap seam.
        assertEquals(StripePattern.coverage(-0.2f, a, b, half),
                StripePattern.coverage(period - 0.2f, a, b, half), 1e-5f);
        Bitmap tile = StripePattern.tile(a, b, true);
        assertEquals(Math.round(period), tile.getWidth());
        assertEquals(StripePattern.AA_TILE_HEIGHT, tile.getHeight());
        int tw = tile.getWidth();
        int first = tile.getPixel(0, 0), last = tile.getPixel(tw - 1, 0);
        // The wrap boundary sits at 0 / period. The first and last texels are
        // the two halves of the 1 px ramp, so they are complementary greys.
        float c0 = (first & 0xff) / 255f, c1 = (last & 0xff) / 255f;
        assertEquals(c0 + c1, 1f, 4f / 255f);
        assertTrue("first=" + c0, c0 > 0f && c0 < 0.5f);
        assertTrue("last=" + c1, c1 > 0.5f && c1 < 1f);
        int greys = 0;
        for (int x = 0; x < tw; x++) {
            assertEquals(tile.getPixel(x, 0), tile.getPixel(x, 1));
            int c = tile.getPixel(x, 0);
            if (c != Color.BLACK && c != Color.WHITE) greys++;
        }
        // Two boundaries, 1–2 intermediate texels each.
        assertTrue("grey texels=" + greys, greys >= 2 && greys <= 8);
    }

    @Test public void aaTileAllowsFractionalWidths() {
        float a = 3.25f, b = 5.5f;
        float period = StripePattern.periodF(a, b);
        assertEquals(8.75f, period, 0f);
        Bitmap tile = StripePattern.tile(a, b, true);
        assertEquals(Math.round(period), tile.getWidth());
        float half = StripePattern.aaHalf(a, b);
        assertEquals(0.5f, StripePattern.coverage(0f, a, b, half), 1e-5f);
        assertEquals(0.5f, StripePattern.coverage(b, a, b, half), 1e-5f);
        assertEquals(0f, StripePattern.coverage(b / 2f, a, b, half), 1e-5f);
        int tw = tile.getWidth();
        int first = tile.getPixel(0, 0), last = tile.getPixel(tw - 1, 0);
        float c0 = (first & 0xff) / 255f, c1 = (last & 0xff) / 255f;
        assertEquals(c0 + c1, 1f, 4f / 255f);
    }

    @Test public void mixColorsLerpsFromBToA() {
        assertEquals(Color.BLACK, StripePattern.mixColors(Color.BLACK, Color.WHITE, 0xFF000000));
        assertEquals(Color.WHITE, StripePattern.mixColors(Color.BLACK, Color.WHITE, 0xFFFFFFFF));
        int mid = StripePattern.mixColors(Color.BLACK, Color.WHITE, 0xFF808080);
        int g = mid & 0xff;
        assertEquals(g, (mid >> 8) & 0xff);
        assertTrue(g > 120 && g < 140);
    }


    @Test public void perceivedMixOfEqualBlackAndWhiteIsAboutBcbcbc() {
        int mix = StripePattern.perceivedMix(Color.WHITE, Color.BLACK, 1, 1);
        int g = mix & 0xff;
        assertEquals(g, (mix >> 8) & 0xff);
        assertEquals(g, (mix >> 16) & 0xff);
        assertEquals(0xFFBCBCBC, mix);
        assertEquals(mix, StripePattern.perceivedMix(Color.WHITE, Color.BLACK, 8, 8));
        // Unequal widths lean toward the wider band, still in linear light.
        int light = StripePattern.perceivedMix(Color.WHITE, Color.BLACK, 3, 1);
        int dark = StripePattern.perceivedMix(Color.WHITE, Color.BLACK, 1, 3);
        assertTrue((light & 0xff) > g);
        assertTrue((dark & 0xff) < g);
        assertTrue((light & 0xff) > (dark & 0xff));
    }

    @Test public void perceivedMixUsesLinearLightPerChannel() {
        // A mid sRGB grey mixed equally with black in linear light is not
        // #404040 (the naive 8-bit average).
        int mid = 0xFF808080;
        int mixed = StripePattern.perceivedMix(mid, Color.BLACK, 1, 1);
        int g = mixed & 0xff;
        assertTrue("linear mix of #808080 and black should differ from 64, got " + g, g != 0x40);
        assertTrue(g > 0 && g < 0x80);
        assertEquals(g, StripePattern.perceivedMix(Color.BLACK, mid, 1, 1) & 0xff);
        // Complementary pair at equal width should sit on the mid grey of the
        // two linear colours (not required to be #BCBCBC, which is B/W only).
        int a = StripePattern.colorAtHue(120f);
        int b = StripePattern.colorAtHue(300f);
        int m = StripePattern.perceivedMix(a, b, 5, 5);
        float lr = 0.5f * (StripePattern.srgbToLinear(((a >> 16) & 0xff) / 255f)
                + StripePattern.srgbToLinear(((b >> 16) & 0xff) / 255f));
        int expectR = Math.round(StripePattern.linearToSrgb(lr) * 255f);
        assertEquals(expectR, (m >> 16) & 0xff);
    }

    @Test public void hueDeltaTakesTheShorterArc() {
        assertEquals(0f, StripePattern.hueDelta(10f, 10f), 0f);
        assertEquals(1f, StripePattern.hueDelta(10f, 11f), 1e-5f);
        assertEquals(2f, StripePattern.hueDelta(1f, 359f), 1e-5f);
        assertEquals(0f, StripePattern.hueDelta(0f, 360f), 1e-5f);
    }


    @Test public void colorAtHsvYieldsGreenAndMagenta() {
        assertEquals(0xFF00FF00, StripePattern.colorAtHsv(120f, 1f, 1f));
        assertEquals(0xFFFF00FF, StripePattern.colorAtHsv(300f, 1f, 1f));
        assertEquals(0xFF000000, StripePattern.colorAtHsv(0f, 0f, 0f));
        assertEquals(0xFFFFFFFF, StripePattern.colorAtHsv(0f, 0f, 1f));
    }

    @Test public void defaultAaRampGreysAOnePixelWhiteTexel() {
        Bitmap tile = StripePattern.tile(1, 8, true);
        // White band is the texel at x=8. The 1 px symmetric ramp eats both
        // edges, so the box-filtered texel is mid-grey, not 255.
        int g = tile.getPixel(8, 0) & 0xff;
        assertTrue("default 1 px white texel should be grey, got " + g, g < 250);
        assertTrue("expected around 191, got " + g, g > 160);
    }

    @Test public void whitePriorityKeepsAOnePixelWhiteTexelAt255() {
        Bitmap tile = StripePattern.tile(1, 8, true, 1f, true);
        assertEquals(0xFFFFFFFF, tile.getPixel(8, 0));
        assertEquals(0xFFFFFFFF, tile.getPixel(8, 1));
        // Black core, far from both edges, stays black.
        assertEquals(0xFF000000, tile.getPixel(4, 0));
        // The texel just inside black at the white boundary is intermediate.
        int left = tile.getPixel(7, 0) & 0xff;
        assertTrue("black-side ramp " + left, left > 0 && left < 255);
    }

    @Test public void narrowerSymmetricRampRaisesTheOnePixelWhitePeak() {
        int wide = StripePattern.tile(1, 8, true, 1f, false).getPixel(8, 0) & 0xff;
        int mid = StripePattern.tile(1, 8, true, 0.3f, false).getPixel(8, 0) & 0xff;
        int narrow = StripePattern.tile(1, 8, true, 0.1f, false).getPixel(8, 0) & 0xff;
        assertTrue("0.3 px " + mid + " should exceed 1.0 px " + wide, mid > wide);
        assertTrue("0.1 px " + narrow + " should be at least 0.3 px " + mid, narrow >= mid);
        // 4 box-filter samples can miss a 0.1 px edge, so the 1 px white texel
        // may already be 255. A 0.3 px edge is still usually a little under.
        assertTrue("narrow should be near white, got " + narrow, narrow >= 240);
    }

    @Test public void whitePriorityCoverageIsOneAcrossTheWholeWhiteBand() {
        int a = 1, b = 8;
        for (float x = b; x < b + a; x += 0.05f) {
            assertEquals("x=" + x, 1f, StripePattern.coverage(x, a, b, 1f, true), 1e-5f);
        }
        assertEquals(1f, StripePattern.coverage(b, a, b, 1f, true), 1e-5f);
        assertEquals(0f, StripePattern.coverage(b / 2f, a, b, 1f, true), 1e-5f);
        // Just inside black, the 1 px ramp is almost white.
        float near = StripePattern.coverage(b - 0.05f, a, b, 1f, true);
        assertTrue("near white edge " + near, near > 0.9f && near < 1f);
    }

    @Test public void twoPixelWhiteHasAPureTexelWhenTheRampIsNarrowOrBiased() {
        // Default 1 px symmetric ramp on a 2 px white band leaves no 255 texel
        // (each 1 px texel includes a 0.5 px ramp).
        int def0 = StripePattern.tile(2, 8, true, 1f, false).getPixel(8, 0) & 0xff;
        int def1 = StripePattern.tile(2, 8, true, 1f, false).getPixel(9, 0) & 0xff;
        assertTrue("default 2 px whites are grey: " + def0 + "," + def1,
                def0 < 255 && def1 < 255);
        Bitmap prior = StripePattern.tile(2, 8, true, 1f, true);
        assertEquals(0xFFFFFFFF, prior.getPixel(8, 0));
        assertEquals(0xFFFFFFFF, prior.getPixel(9, 0));
        Bitmap narrow = StripePattern.tile(2, 8, true, 0.3f, false);
        int n0 = narrow.getPixel(8, 0) & 0xff, n1 = narrow.getPixel(9, 0) & 0xff;
        assertTrue("narrow 2 px peaks " + n0 + "," + n1, n0 > def0 && n1 > def1);
    }
}

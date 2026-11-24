package com.zmlworks.stripeflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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

    @Test public void periodIsTwiceTheWidth() {
        assertEquals(2, StripePattern.period(1));
        assertEquals(4, StripePattern.period(2));
        assertEquals(10, StripePattern.period(5));
        assertEquals(20, StripePattern.period(10));
        assertEquals(2, StripePattern.period(0)); // below 1 is treated as 1
    }

    @Test public void width1AtOffset0IsTheCheckerboard() {
        assertEquals(Color.BLACK, StripePattern.color(0, 0, 1, 0f));
        assertEquals(Color.WHITE, StripePattern.color(1, 0, 1, 0f));
        assertEquals(Color.WHITE, StripePattern.color(0, 1, 1, 0f));
        assertEquals(Color.BLACK, StripePattern.color(1, 1, 1, 0f));
    }

    @Test public void width2BandsAlongXPlusY() {
        // floor((x+y)/2) even -> black, odd -> white
        assertEquals(Color.BLACK, StripePattern.color(0, 0, 2, 0f));
        assertEquals(Color.BLACK, StripePattern.color(1, 0, 2, 0f));
        assertEquals(Color.WHITE, StripePattern.color(2, 0, 2, 0f));
        assertEquals(Color.WHITE, StripePattern.color(3, 0, 2, 0f));
        assertEquals(Color.BLACK, StripePattern.color(0, 1, 2, 0f));
        assertEquals(Color.WHITE, StripePattern.color(1, 1, 2, 0f));
    }

    @Test public void width5BandsAlongXPlusY() {
        assertEquals(Color.BLACK, StripePattern.color(0, 0, 5, 0f));
        assertEquals(Color.BLACK, StripePattern.color(4, 0, 5, 0f));
        assertEquals(Color.WHITE, StripePattern.color(5, 0, 5, 0f));
        assertEquals(Color.WHITE, StripePattern.color(9, 0, 5, 0f));
        assertEquals(Color.BLACK, StripePattern.color(10, 0, 5, 0f));
    }

    @Test public void onlyBlackAndWhite() {
        for (int w = 1; w <= 10; w++) {
            for (int o = 0; o < StripePattern.period(w); o++) {
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        int c = StripePattern.color(x, y, w, o);
                        assertTrue(c == Color.BLACK || c == Color.WHITE);
                    }
                }
            }
        }
    }

    @Test public void integerOffsetShiftsByOneAlongXPlusY() {
        assertEquals(StripePattern.color(0, 0, 1, 1f), StripePattern.color(1, 0, 1, 0f));
        assertEquals(StripePattern.color(3, 5, 1, 1f), StripePattern.color(4, 5, 1, 0f));
        assertEquals(StripePattern.color(2, 1, 2, 2f), StripePattern.color(4, 1, 2, 0f));
    }

    @Test public void offsetWrapsAndRespectsDirection() {
        // 3 px/s, width 1, period 2: after 2/3 s the shift is 2 px == 0
        assertEquals(0f, StripePattern.offset(0L, 3f, -1, 1), 0f);
        long twoThirds = 2_000_000_000L / 3;
        assertEquals(0f, StripePattern.offset(twoThirds, 3f, -1, 1), 1e-5f);
        // one second at 3 px/s, direction -1: raw = -3, wrapped into [0, 2) is 1
        assertEquals(1f, StripePattern.offset(1_000_000_000L, 3f, -1, 1), 1e-5f);
        // opposite direction: raw = +3, wrapped is 1 as well (3 % 2 == 1), but
        // after 1/3 s they differ: -1 vs +1 -> 5/3 vs 1, wait 1.0 vs 1.0? 
        // +1 px vs -1 px: +1 wraps to 1, -1 wraps to 1. Same after 1/3 s at 3 px/s
        // because period is 2 and they moved to opposite phases that coincide
        // at half-period. After 1/6 s (0.5 px) they differ.
        long sixth = 1_000_000_000L / 6;
        float a = StripePattern.offset(sixth, 3f, 1, 1);
        float b = StripePattern.offset(sixth, 3f, -1, 1);
        assertEquals(0.5f, a, 1e-5f);
        assertEquals(1.5f, b, 1e-5f);
        assertNotEquals(a, b, 0.1f);
    }

    @Test public void tileMatchesColorAtOffset0() {
        for (int w = 1; w <= 10; w++) {
            Bitmap tile = StripePattern.tile(w);
            assertEquals(Bitmap.DENSITY_NONE, tile.getDensity());
            assertEquals(StripePattern.period(w), tile.getWidth());
            for (int y = 0; y < tile.getHeight(); y++) {
                for (int x = 0; x < tile.getWidth(); x++) {
                    assertEquals(StripePattern.color(x, y, w, 0f), tile.getPixel(x, y));
                }
            }
        }
    }

    @Test public void renderFillsOnlyBlackAndWhiteAndMoves() {
        Bitmap a = Bitmap.createBitmap(16, 12, Bitmap.Config.ARGB_8888);
        Bitmap b = Bitmap.createBitmap(16, 12, Bitmap.Config.ARGB_8888);
        StripePattern.render(a, 1, 0f);
        StripePattern.render(b, 1, 1f);
        int changed = 0;
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 16; x++) {
                int ca = a.getPixel(x, y), cb = b.getPixel(x, y);
                assertTrue(ca == Color.BLACK || ca == Color.WHITE);
                assertTrue(cb == Color.BLACK || cb == Color.WHITE);
                if (ca != cb) changed++;
                // shift of 1 along x+y equals sampling (x+1, y) at offset 0
                if (x + 1 < 16) assertEquals(a.getPixel(x + 1, y), b.getPixel(x, y));
            }
        }
        assertEquals(16 * 12, changed); // every pixel flips when offset moves by 1 at width 1
    }

    @Test public void onePeriodLaterTheImageIsIdentical() {
        Bitmap a = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        Bitmap b = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        StripePattern.render(a, 1, 0.3f);
        StripePattern.render(b, 1, 0.3f + StripePattern.period(1));
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                assertEquals(a.getPixel(x, y), b.getPixel(x, y));
            }
        }
    }
}

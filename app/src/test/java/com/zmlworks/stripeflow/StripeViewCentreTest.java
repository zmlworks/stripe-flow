package com.zmlworks.stripeflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/**
 * The centre of a black/white field must stay pure #FFFFFF / #000000, whether
 * the edge vignette and anti-aliasing are on or off. The vignette may only
 * tint pixels near the display edge.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class StripeViewCentreTest {

    @Test public void blackWhiteCentreIsPureWithAndWithoutVignetteAndAa() {
        int s = 256, blur = 48, margin = blur + 4;
        boolean[] onoff = {false, true};
        for (boolean aa : onoff) {
            for (boolean vig : onoff) {
                Bitmap bmp = renderVertical(s, 8, 8, aa, vig ? blur : 0, 32);
                // Vertical period 16: x in [0,8) black, [8,16) white, repeating.
                for (int x = 4; x < s; x += 16) {
                    if (x < margin || x >= s - margin) continue;
                    int c = bmp.getPixel(x, s / 2);
                    assertEquals("black aa=" + aa + " vig=" + vig + " x=" + x
                            + " got #" + Integer.toHexString(c), Color.BLACK, c);
                }
                for (int x = 12; x < s; x += 16) {
                    if (x < margin || x >= s - margin) continue;
                    int c = bmp.getPixel(x, s / 2);
                    assertEquals("white aa=" + aa + " vig=" + vig + " x=" + x
                            + " got #" + Integer.toHexString(c), Color.WHITE, c);
                }
            }
        }
    }

    @Test public void diagonalInteriorStaysPureWhenVignetteIsOnAndAaIsOff() {
        int s = 256, blur = 48;
        StripeView v = view(s, 8, 8, false, blur, 40);
        v.setState(StripePattern.BASE_ANGLE_RAD, s / 2f, s / 2f, 12f);
        Bitmap bmp = draw(v);
        int m = blur + 8;
        int[] px = new int[(s - 2 * m) * (s - 2 * m)];
        bmp.getPixels(px, 0, s - 2 * m, m, m, s - 2 * m, s - 2 * m);
        int black = 0, white = 0;
        for (int p : px) {
            assertTrue("interior #" + Integer.toHexString(p),
                    p == Color.BLACK || p == Color.WHITE);
            if (p == Color.BLACK) black++;
            else white++;
        }
        assertTrue("expected both bands, black=" + black + " white=" + white,
                black > 100 && white > 100);
    }

    @Test public void specifiedColoursDoNotDriftAndDriveTheVignette() {
        int s = 128, blur = 32;
        StripeView v = view(s, 8, 8, false, blur, 0);
        v.configure(8, 8, 0f, Prefs.DIR_DOWN_RIGHT, false, 0f, Prefs.ROT_CW, false, 1L, false,
                StripePattern.SCHEME_COLOR, 1f, 2f, false, blur);
        v.applyColorStyle(true, false, 0f, 120f, 1f, 1f, 300f, 1f, 1f);
        v.measure(View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, s, s);
        v.setState(0f, 0f, 0f, 0f);
        Bitmap a = draw(v);
        int green = StripePattern.colorAtHsv(120f, 1f, 1f);
        int magenta = StripePattern.colorAtHsv(300f, 1f, 1f);
        assertEquals(green, 0xFF00FF00);
        assertEquals(magenta, 0xFFFF00FF);
        // x=4 and x=12 sit inside the 32 px fade; sample a period past the fade.
        int xB = 4 + 16 * 3, xA = 12 + 16 * 3;
        assertEquals(magenta, a.getPixel(xB, s / 2));
        assertEquals(green, a.getPixel(xA, s / 2));
        int mix = StripePattern.perceivedMix(green, magenta, 8, 8);
        assertEquals(mix, v.fadeColor());
        v.setElapsedNanos(3_000_000_000L);
        Bitmap b = draw(v);
        assertEquals(magenta, b.getPixel(xB, s / 2));
        assertEquals(green, b.getPixel(xA, s / 2));
        assertEquals(mix, v.fadeColor());
    }

    private static Bitmap renderVertical(int s, int wa, int wb, boolean aa, int blur, int radius) {
        StripeView v = view(s, wa, wb, aa, blur, radius);
        v.setState(0f, 0f, 0f, 0f);
        return draw(v);
    }

    private static StripeView view(int s, int wa, int wb, boolean aa, int blur, int radius) {
        StripeView v = new StripeView(Robolectric.buildActivity(SettingsActivity.class).setup().get());
        v.configure(wa, wb, 0f, Prefs.DIR_DOWN_RIGHT, false, 0f, Prefs.ROT_CW, false, 1L, false,
                StripePattern.SCHEME_BW, 1f, 0f, aa, blur);
        v.setCornerRadii(radius, radius, radius, radius);
        v.measure(View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, s, s);
        return v;
    }

    private static Bitmap draw(StripeView v) {
        Bitmap bmp = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        v.draw(new Canvas(bmp));
        return bmp;
    }
}

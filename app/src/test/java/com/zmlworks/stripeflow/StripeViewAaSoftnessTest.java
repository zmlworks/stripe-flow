package com.zmlworks.stripeflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class StripeViewAaSoftnessTest {

    @Test public void axisAlignedOnePixelWhiteStays255WithWhitePriority() {
        Bitmap bmp = render(0f, 1, 8, true, 1f, true);
        int mid = bmp.getHeight() / 2;
        // Phase 0, vertical: [0,8) black, [8,9) white.
        assertEquals(Color.WHITE, bmp.getPixel(8, mid));
        assertEquals(Color.BLACK, bmp.getPixel(4, mid));
        int left = bmp.getPixel(7, mid) & 0xff;
        assertTrue("ramp into black " + left, left > 0 && left < 255);
    }

    @Test public void axisAlignedOnePixelWhiteIsGreyWithDefaultRamp() {
        Bitmap bmp = render(0f, 1, 8, true, 1f, false);
        int g = bmp.getPixel(8, bmp.getHeight() / 2) & 0xff;
        assertTrue("default AA 1 px white is grey, got " + g, g < 250 && g > 160);
    }

    @Test public void axisAlignedTwoPixelWhiteStays255WithWhitePriority() {
        Bitmap bmp = render(0f, 2, 8, true, 1f, true);
        int mid = bmp.getHeight() / 2;
        assertEquals(Color.WHITE, bmp.getPixel(8, mid));
        assertEquals(Color.WHITE, bmp.getPixel(9, mid));
    }

    @Test public void rotatedOnePixelWhitePeakIsHighWithWhitePriorityButBilinearSoftensSomePixels() {
        int s = 128;
        Bitmap bmp = renderAt(s, StripePattern.BASE_ANGLE_RAD, s / 2f, s / 2f, 8.5f,
                1, 8, true, 1f, true);
        int max = 0, under = 0, white = 0, n = s * s;
        int[] px = new int[n];
        bmp.getPixels(px, 0, s, 0, 0, s, s);
        for (int c : px) {
            int g = c & 0xff;
            if (g > max) max = g;
            if (g == 255) white++;
            else if (g > 0 && g < 255) under++;
        }
        // Axis-aligned stays 255. At 45°, bilinear mixes the 1 px white texel
        // with its neighbours, so not every white-band pixel is 255, but the
        // peak should still reach, or come very close to, full white.
        assertTrue("45° peak " + max, max >= 250);
        assertTrue("some intermediate samples " + under, under > 0);
        // Honest bound: a 1 px white at 45° may have few exact 255s.
        assertTrue("peak recorded, whites=" + white + " max=" + max, max > 240);
    }

    @Test public void compareAaRampWidthsAtOneAndTwoPixelWhite() throws Exception {
        int[] widths = {1, 2};
        // off, 1.0, 0.3, 1.0 white-priority
        String[] labels = {"オフ", "1.0 px", "0.3 px", "白優先"};
        boolean[] aa = {false, true, true, true};
        float[] edge = {1f, 1f, 0.3f, 1f};
        boolean[] prior = {false, false, false, true};
        int cellW = 88, cellH = 96, zoom = 8, pad = 12, labelH = 36, rowLabelW = 52;
        int cols = labels.length, rows = widths.length;
        int w = rowLabelW + cols * (cellW + pad) + pad;
        int h = 28 + rows * (labelH + cellH + pad) + pad;
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.eraseColor(Ui.BG);
        Canvas c = new Canvas(out);
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(Ui.TEXT);
        text.setTextSize(18);
        text.setTypeface(Typeface.SANS_SERIF);
        Paint hi = new Paint(text);
        hi.setColor(Ui.HI);
        for (int col = 0; col < cols; col++) {
            float x = rowLabelW + col * (cellW + pad) + pad;
            c.drawText(labels[col], x, 24, hi);
        }
        for (int row = 0; row < rows; row++) {
            int wa = widths[row];
            float y0 = 28 + row * (labelH + cellH + pad);
            c.drawText(wa + " px", 8, y0 + labelH + cellH / 2f, text);
            for (int col = 0; col < cols; col++) {
                Bitmap src = render(0f, wa, 8, aa[col], edge[col], prior[col]);
                // Crop a few periods around the first white band and scale up.
                int cx = 4, cy = 8, cw = 16, ch = 12;
                Bitmap crop = Bitmap.createBitmap(src, cx, cy, cw, ch);
                Bitmap zoomed = Bitmap.createScaledBitmap(crop, cw * zoom, ch * zoom, false);
                float x = rowLabelW + col * (cellW + pad) + pad;
                float y = y0 + labelH;
                c.drawBitmap(zoomed, x, y, null);
                // Record the white-band peak in this crop for the report.
                int peak = peakWhite(src, wa);
                if (!aa[col] || prior[col]) {
                    assertEquals("row wa=" + wa + " col=" + labels[col] + " peak=" + peak,
                            255, peak);
                } else if (edge[col] >= 0.99f) {
                    assertTrue("default ramp should grey thin white, peak=" + peak, peak < 250);
                }
            }
        }
        java.io.File dir = new java.io.File("build/reports/screens");
        if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException("mkdir");
        java.io.File f = new java.io.File(dir, "aa-ramp-compare.png");
        try (java.io.FileOutputStream o = new java.io.FileOutputStream(f)) {
            if (!out.compress(Bitmap.CompressFormat.PNG, 100, o)) throw new java.io.IOException("compress");
        }
    }

    private static int peakWhite(Bitmap bmp, int widthA) {
        // Vertical, offset 0: first white texel is at x = widthB = 8.
        int mid = bmp.getHeight() / 2;
        int peak = 0;
        for (int x = 8; x < 8 + widthA; x++) {
            int g = bmp.getPixel(x, mid) & 0xff;
            if (g > peak) peak = g;
        }
        return peak;
    }

    private static Bitmap render(float angle, int wa, int wb, boolean aa, float edge, boolean prior) {
        int s = 64;
        return renderAt(s, angle, 0f, 0f, 0f, wa, wb, aa, edge, prior);
    }

    private static Bitmap renderAt(int s, float angle, float px, float py, float offset,
                                   int wa, int wb, boolean aa, float edge, boolean prior) {
        StripeView v = new StripeView(Robolectric.buildActivity(SettingsActivity.class).setup().get());
        v.configure(wa, wb, 0f, Prefs.DIR_DOWN_RIGHT, false, 0f, Prefs.ROT_CW, false, 1L, false,
                StripePattern.SCHEME_BW, 1f, 0f, aa, 0, edge, prior);
        v.measure(View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, s, s);
        v.setState(angle, px, py, offset);
        Bitmap bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        v.draw(new Canvas(bmp));
        return bmp;
    }
}

package com.zmlworks.stripeflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/** Renders the settings and stripe screens to PNG under build/reports/screens/. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w360dp-h720dp-port-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ScreenshotsTest {
    static final int W = 1080, H = 2160;
    static final File DIR = new File("build/reports/screens");

    @Test public void settingsScreen() throws Exception {
        Activity a = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        Bitmap bmp = drawActivity(a);
        // A few pixels should be the dark background, and some the accent/text.
        assertNotEquals(0, countColor(bmp, Ui.BG));
        assertTrue(countColor(bmp, Ui.TEXT) > 100);
        save(bmp, "settings.png");
    }

    @Test public void stripeScreenIs1pxBlackAndWhiteAndMoves() throws Exception {
        StripeView v = new StripeView(Robolectric.buildActivity(SettingsActivity.class).setup().get());
        v.apply(1, 3f, Prefs.DIR_DOWN_RIGHT);
        v.measure(View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(H, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, W, H);

        v.setOffset(0f);
        Bitmap a = drawView(v);
        assertOnlyBlackAndWhite(a);
        assertEquals(Color.BLACK, a.getPixel(0, 0));
        assertEquals(Color.WHITE, a.getPixel(1, 0));
        assertEquals(Color.WHITE, a.getPixel(0, 1));
        assertEquals(Color.BLACK, a.getPixel(1, 1));
        save(a, "stripes-0.png");
        save(crop(a, 0, 0, 64, 64), "stripes-0-detail.png");

        v.setOffset(1f);
        Bitmap b = drawView(v);
        assertOnlyBlackAndWhite(b);
        assertEquals(Color.WHITE, b.getPixel(0, 0));
        assertEquals(Color.BLACK, b.getPixel(1, 0));
        // Every pixel flipped, and equals the pattern shifted by 1 along x.
        int[] pa = new int[W * H], pb = new int[W * H];
        a.getPixels(pa, 0, W, 0, 0, W, H);
        b.getPixels(pb, 0, W, 0, 0, W, H);
        int changed = 0;
        for (int y = 0; y < H; y++) {
            int row = y * W;
            for (int x = 0; x < W; x++) {
                if (pa[row + x] != pb[row + x]) changed++;
                if (x + 1 < W && pa[row + x + 1] != pb[row + x]) {
                    throw new AssertionError("shift mismatch at " + x + "," + y);
                }
            }
        }
        assertEquals(W * H, changed);
        save(b, "stripes-1.png");
        save(crop(b, 0, 0, 64, 64), "stripes-1-detail.png");

        // After one period the frame matches the first.
        v.setOffset(StripePattern.period(1));
        Bitmap c = drawView(v);
        assertEquals(a.getPixel(0, 0), c.getPixel(0, 0));
        assertEquals(a.getPixel(10, 7), c.getPixel(10, 7));
    }

    @Test public void stripeScreenWidth2() throws Exception {
        StripeView v = new StripeView(Robolectric.buildActivity(SettingsActivity.class).setup().get());
        v.apply(2, 3f, Prefs.DIR_DOWN_RIGHT);
        v.measure(View.MeasureSpec.makeMeasureSpec(128, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(128, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, 128, 128);
        v.setOffset(0f);
        Bitmap bmp = drawView(v);
        assertOnlyBlackAndWhite(bmp);
        assertEquals(Color.BLACK, bmp.getPixel(0, 0));
        assertEquals(Color.BLACK, bmp.getPixel(1, 0));
        assertEquals(Color.WHITE, bmp.getPixel(2, 0));
        assertEquals(Color.WHITE, bmp.getPixel(3, 0));
        save(bmp, "stripes-width2.png");
    }

    static Bitmap drawActivity(Activity a) {
        View root = a.getWindow().getDecorView();
        root.measure(View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(H, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, W, H);
        return drawView(root);
    }

    static Bitmap drawView(View v) {
        Bitmap bmp = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        v.draw(new Canvas(bmp));
        return bmp;
    }

    static Bitmap crop(Bitmap src, int x, int y, int w, int h) {
        return Bitmap.createBitmap(src, x, y, w, h);
    }

    static void assertOnlyBlackAndWhite(Bitmap bmp) {
        int n = bmp.getWidth() * bmp.getHeight();
        int[] px = new int[n];
        bmp.getPixels(px, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
        for (int c : px) {
            assertTrue("unexpected pixel " + Integer.toHexString(c), c == Color.BLACK || c == Color.WHITE);
        }
    }

    static int countColor(Bitmap bmp, int color) {
        int n = 0;
        int[] px = new int[bmp.getWidth() * bmp.getHeight()];
        bmp.getPixels(px, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
        for (int c : px) if (c == color) n++;
        return n;
    }

    static void save(Bitmap bmp, String name) throws IOException {
        if (!DIR.exists() && !DIR.mkdirs()) throw new IOException("mkdir " + DIR);
        File f = new File(DIR, name);
        try (FileOutputStream o = new FileOutputStream(f)) {
            if (!bmp.compress(Bitmap.CompressFormat.PNG, 100, o)) throw new IOException("compress " + name);
        }
    }
}

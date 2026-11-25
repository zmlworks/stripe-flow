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
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/** Renders the settings and stripe screens to PNG under build/reports/screens/. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w360dp-h800dp-port-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ScreenshotsTest {
    static final int W = 1080, H = 3000;
    static final File DIR = new File("build/reports/screens");

    @Test public void settingsScreen() throws Exception {
        android.content.Context app = RuntimeEnvironment.getApplication();
        Prefs.sp(app).edit().clear().commit();
        Activity a = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        Bitmap bmp = drawActivity(a);
        assertNotEquals(0, countColor(bmp, Ui.BG));
        assertTrue(countColor(bmp, Ui.TEXT) > 100);
        save(bmp, "settings.png");
    }

    @Test public void settingsScreenWithRotation() throws Exception {
        android.content.Context app = RuntimeEnvironment.getApplication();
        Prefs.sp(app).edit().clear().commit();
        Prefs.setRotate(app, true);
        Prefs.setWander(app, true);
        Prefs.setRotateSpeed(app, 0.5f);
        Prefs.setWidth(app, 8);
        Activity a = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        Bitmap bmp = drawActivity(a);
        assertTrue(countColor(bmp, Ui.TEXT) > 100);
        save(bmp, "settings-rotate.png");
        Prefs.sp(app).edit().clear().commit();
    }

    @Test public void settingsScreenAutoDrift() throws Exception {
        android.content.Context app = RuntimeEnvironment.getApplication();
        Prefs.sp(app).edit().clear().commit();
        Prefs.setAutoDrift(app, true);
        Activity a = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        Bitmap bmp = drawActivity(a);
        assertTrue(countColor(bmp, Ui.TEXT) > 100);
        save(bmp, "settings-auto.png");
        Prefs.sp(app).edit().clear().commit();
    }

    @Test public void stripeViewMatchesPointSamplingAndRotatesWithoutJumping() throws Exception {
        StripeView v = new StripeView(Robolectric.buildActivity(SettingsActivity.class).setup().get());
        v.configure(8, 0f, Prefs.DIR_DOWN_RIGHT, false, 0f, Prefs.ROT_CW, false, 1L, false);
        int s = 256;
        v.measure(View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, s, s);

        float cx = s / 2f, cy = s / 2f;
        float a0 = StripePattern.BASE_ANGLE_RAD;
        v.setState(a0, cx, cy, 0f);
        Bitmap view0 = drawView(v);
        Bitmap ref0 = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        StripePattern.render(ref0, 8, cx, cy, a0, 0f);
        assertOnlyBlackAndWhite(view0);
        // Shader and point sampling should agree on a solid 8 px field at 45°.
        assertEquals(ref0.getPixel(s / 2, s / 2), view0.getPixel(s / 2, s / 2));
        save(view0, "stripes-45-w8.png");
        save(crop(view0, 0, 0, 64, 64), "stripes-45-w8-detail.png");

        float a1 = a0 + (float) Math.toRadians(20);
        v.setState(a1, cx, cy, 0f);
        Bitmap view1 = drawView(v);
        assertOnlyBlackAndWhite(view1);
        assertEquals(
                StripePattern.color(cx, cy, cx, cy, a1, 0f, 8),
                view1.getPixel(s / 2, s / 2));
        save(view1, "stripes-65-w8.png");
        save(crop(view1, 0, 0, 64, 64), "stripes-65-w8-detail.png");

        v.setState(0f, 0f, 0f, 0f);
        Bitmap vert = drawView(v);
        assertOnlyBlackAndWhite(vert);
        assertEquals(Color.BLACK, vert.getPixel(0, 0));
        assertEquals(Color.BLACK, vert.getPixel(7, 10));
        assertEquals(Color.WHITE, vert.getPixel(8, 0));
        save(vert, "stripes-0-w8.png");
    }

    @Test public void rotationFrames() throws Exception {
        File frameDir = new File(DIR, "rot-frames");
        if (!frameDir.exists() && !frameDir.mkdirs()) throw new IOException("mkdir frames");
        int s = 240, width = 8, frames = 45;
        float cx = s / 2f, cy = s / 2f;
        float start = StripePattern.BASE_ANGLE_RAD;
        Bitmap bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < frames; i++) {
            float ang = start + (float) Math.toRadians(20) * (i / 15f);
            StripePattern.render(bmp, width, cx, cy, ang, 0f);
            assertOnlyBlackAndWhite(bmp);
            // Pivot pixel stays the same colour when only the angle changes.
            assertEquals(StripePattern.colorFromPhase(0f, width), bmp.getPixel(s / 2, s / 2));
            save(bmp, String.format(Locale.US, "rot-frames/frame-%03d.png", i));
        }
    }

    /** Time-lapse of auto drift: 90 s of motion in 45 frames, integrated at 30 Hz. */
    @Test public void autoDriftFrames() throws Exception {
        File frameDir = new File(DIR, "auto-frames");
        if (!frameDir.exists() && !frameDir.mkdirs()) throw new IOException("mkdir frames");
        int s = 320;
        long seed = 7L;
        StripeView v = new StripeView(Robolectric.buildActivity(SettingsActivity.class).setup().get());
        v.configure(8, 0f, Prefs.DIR_DOWN_RIGHT, true, 0f, Prefs.ROT_CW, true, seed, true);
        v.measure(View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, s, s);
        int frames = 45;
        float span = 90f;
        int hz = 30;
        int steps = Math.round(span * hz);
        Bitmap first = null, last = null;
        int i = 0;
        v.setElapsedNanos(0L);
        for (int k = 0; k < frames; k++) {
            int target = Math.round(k * steps / (float) (frames - 1));
            while (i < target) {
                i++;
                v.setElapsedNanos((long) (i / (float) hz * 1_000_000_000f));
            }
            Bitmap bmp = drawView(v);
            assertOnlyBlackAndWhite(bmp);
            if (k == 0) first = bmp;
            if (k == frames - 1) last = bmp;
            save(bmp, String.format(Locale.US, "auto-frames/frame-%03d.png", k));
        }
        if (first == null || last == null) throw new AssertionError("missing end frames");
        int changed = 0;
        int[] a = new int[s * s], b = new int[s * s];
        first.getPixels(a, 0, s, 0, 0, s, s);
        last.getPixels(b, 0, s, 0, 0, s, s);
        for (int j = 0; j < a.length; j++) if (a[j] != b[j]) changed++;
        assertTrue("pattern should move over 90 s; changed=" + changed, changed > 1000);
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
        File parent = f.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("mkdir " + parent);
        try (FileOutputStream o = new FileOutputStream(f)) {
            if (!bmp.compress(Bitmap.CompressFormat.PNG, 100, o)) throw new IOException("compress " + name);
        }
    }
}

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
    static final int W = 1080, H = 4000;
    static final File DIR = new File("build/reports/screens");

    @Test public void settingsScreen() throws Exception {
        android.content.Context app = RuntimeEnvironment.getApplication();
        Prefs.sp(app).edit().clear().commit();
        Prefs.setWidthA(app, 3);
        Prefs.setWidthB(app, 8);
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
        Prefs.setWidthA(app, 8);
        Prefs.setWidthB(app, 8);
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

    @Test public void settingsScreenColorMode() throws Exception {
        android.content.Context app = RuntimeEnvironment.getApplication();
        Prefs.sp(app).edit().clear().commit();
        Prefs.setColorScheme(app, Prefs.COLOR_MODE);
        Prefs.setHueRate(app, Prefs.HUE_RATE_DEFAULT);
        Activity a = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        Bitmap bmp = drawActivity(a);
        assertTrue(countColor(bmp, Ui.TEXT) > 100);
        save(bmp, "settings-color.png");
        Prefs.sp(app).edit().clear().commit();
    }

    @Test public void settingsScreenAaAndEdgeBlur() throws Exception {
        android.content.Context app = RuntimeEnvironment.getApplication();
        Prefs.sp(app).edit().clear().commit();
        Prefs.setWidthA(app, 3);
        Prefs.setWidthB(app, 8);
        Prefs.setAntiAlias(app, true);
        Prefs.setEdgeBlur(app, true);
        Prefs.setEdgeBlurWidth(app, 64);
        Activity a = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        Bitmap bmp = drawActivity(a);
        assertTrue(countColor(bmp, Ui.TEXT) > 100);
        save(bmp, "settings-aa-blur.png");
        Prefs.sp(app).edit().clear().commit();
    }

    @Test public void stripeViewMatchesPointSamplingAndRotatesWithoutJumping() throws Exception {
        StripeView v = new StripeView(Robolectric.buildActivity(SettingsActivity.class).setup().get());
        v.configure(8, 8, 0f, Prefs.DIR_DOWN_RIGHT, false, 0f, Prefs.ROT_CW, false, 1L, false, StripePattern.SCHEME_BW);
        int s = 256;
        v.measure(View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, s, s);

        float cx = s / 2f, cy = s / 2f;
        float a0 = StripePattern.BASE_ANGLE_RAD;
        v.setState(a0, cx, cy, 0f);
        Bitmap view0 = drawView(v);
        Bitmap ref0 = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        StripePattern.render(ref0, 8, 8, cx, cy, a0, 0f);
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
                StripePattern.color(cx, cy, cx, cy, a1, 0f, 8, 8),
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
            StripePattern.render(bmp, width, width, cx, cy, ang, 0f);
            assertOnlyBlackAndWhite(bmp);
            // Pivot pixel stays the same colour when only the angle changes.
            assertEquals(StripePattern.colorFromPhase(0f, width, width), bmp.getPixel(s / 2, s / 2));
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
        v.configure(8, 8, 0f, Prefs.DIR_DOWN_RIGHT, true, 0f, Prefs.ROT_CW, true, seed, true, StripePattern.SCHEME_BW);
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

    @Test public void colorModeStripeFrame() throws Exception {
        int s = 256;
        long seed = 7L;
        StripeView v = new StripeView(Robolectric.buildActivity(SettingsActivity.class).setup().get());
        v.configure(8, 8, 0f, Prefs.DIR_DOWN_RIGHT, false, 0f, Prefs.ROT_CW, false, seed, false,
                StripePattern.SCHEME_COLOR, 1f, 0f);
        v.measure(View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, s, s);
        v.setElapsedNanos(0L);
        Bitmap bmp = drawView(v);
        int[] px = new int[s * s];
        bmp.getPixels(px, 0, s, 0, 0, s, s);
        int c0 = 0, c1 = 0, n0 = 0, n1 = 0;
        for (int c : px) {
            if (n0 == 0) { c0 = c; n0++; }
            else if (c == c0) n0++;
            else if (n1 == 0) { c1 = c; n1++; }
            else if (c == c1) n1++;
            else throw new AssertionError("third colour " + Integer.toHexString(c));
        }
        assertTrue(n0 > 1000 && n1 > 1000);
        float[] h0 = new float[3], h1 = new float[3];
        android.graphics.Color.colorToHSV(c0, h0);
        android.graphics.Color.colorToHSV(c1, h1);
        assertEquals(1f, h0[1], 0.01f);
        assertEquals(1f, h0[2], 0.01f);
        assertEquals(1f, h1[1], 0.01f);
        assertEquals(1f, h1[2], 0.01f);
        float d = Math.abs(h0[0] - h1[0]);
        d = Math.min(d, 360f - d);
        assertEquals(180f, d, 0.5f);
        save(bmp, "stripes-45-w8-color.png");
        save(crop(bmp, 0, 0, 64, 64), "stripes-45-w8-color-detail.png");
    }

    /** Time-lapse of a full hue cycle at the default 2 °/s (180 s in 45 frames). */
    @Test public void colorDriftFrames() throws Exception {
        File frameDir = new File(DIR, "color-frames");
        if (!frameDir.exists() && !frameDir.mkdirs()) throw new IOException("mkdir frames");
        int s = 320;
        long seed = 7L;
        float rate = HueDrift.RATE_DEFAULT;
        float span = 360f / rate;
        StripeView v = new StripeView(Robolectric.buildActivity(SettingsActivity.class).setup().get());
        v.configure(8, 8, 0f, Prefs.DIR_DOWN_RIGHT, false, 0f, Prefs.ROT_CW, false, seed, false,
                StripePattern.SCHEME_COLOR, 1f, rate);
        v.measure(View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, s, s);
        int frames = 45;
        int hz = 30;
        int steps = Math.round(span * hz);
        int i = 0;
        v.setElapsedNanos(0L);
        Integer first = null, last = null;
        for (int k = 0; k < frames; k++) {
            int target = Math.round(k * steps / (float) (frames - 1));
            while (i < target) {
                i++;
                v.setElapsedNanos((long) (i / (float) hz * 1_000_000_000f));
            }
            Bitmap bmp = drawView(v);
            int mid = bmp.getPixel(s / 2, s / 2);
            if (k == 0) first = mid;
            if (k == frames - 1) last = mid;
            save(bmp, String.format(Locale.US, "color-frames/frame-%03d.png", k));
        }
        if (first == null || last == null) throw new AssertionError("missing frames");
        // After a full unwrapped cycle the pair has moved; the mid pixel may
        // coincide, so compare a handful of pixels.
        v.setElapsedNanos(0L);
        Bitmap a = drawView(v);
        v.setElapsedNanos((long) (span * 1_000_000_000f));
        Bitmap b = drawView(v);
        int changed = 0;
        int[] pa = new int[s * s], pb = new int[s * s];
        a.getPixels(pa, 0, s, 0, 0, s, s);
        b.getPixels(pb, 0, s, 0, 0, s, s);
        for (int j = 0; j < pa.length; j++) if (pa[j] != pb[j]) changed++;
        assertTrue("colour should wander over a cycle; changed=" + changed, changed > 1000);
    }

    @Test public void compareOffAaAndEdgeBlur() throws Exception {
        int s = 256;
        Bitmap off = stripeShot(s, false, 0);
        Bitmap aa = stripeShot(s, true, 0);
        Bitmap blur = stripeShot(s, false, 48);
        assertOnlyBlackAndWhite(off);
        // AA softens the 45° edges: there should be grey in-between pixels.
        int greys = 0, n = s * s;
        int[] px = new int[n];
        aa.getPixels(px, 0, s, 0, 0, s, s);
        for (int c : px) {
            int r = (c >> 16) & 0xff, g = (c >> 8) & 0xff, b = c & 0xff;
            if (!(c == Color.BLACK || c == Color.WHITE) && r == g && g == b) greys++;
        }
        assertTrue("AA should introduce grey edge pixels, got " + greys, greys > 50);
        int trans = 0;
        boolean wasLight = ((aa.getPixel(0, s / 2) >> 16) & 0xff) >= 128;
        for (int x = 1; x < s; x++) {
            boolean light = ((aa.getPixel(x, s / 2) >> 16) & 0xff) >= 128;
            if (light != wasLight) { trans++; wasLight = light; }
        }
        // Off shot has about 22 px per period along x at 45°, so ~11 crossings.
        assertTrue("AA period looks wrong, transitions=" + trans, trans >= 18 && trans <= 28);
        // Edge fade leaves the centre as a hard stripe and darkens the border.
        int mid = blur.getPixel(s / 2, s / 2);
        assertTrue(mid == Color.BLACK || mid == Color.WHITE);
        int edge = blur.getPixel(0, s / 2);
        assertTrue(((edge >> 16) & 0xff) < 80);
        Bitmap row = Bitmap.createBitmap(s * 3, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(row);
        c.drawBitmap(off, 0, 0, null);
        c.drawBitmap(aa, s, 0, null);
        c.drawBitmap(blur, s * 2, 0, null);
        save(row, "compare-off-aa-blur.png");
        save(crop(off, 80, 80, 96, 96), "compare-off-detail.png");
        save(crop(aa, 80, 80, 96, 96), "compare-aa-detail.png");
        save(crop(blur, 0, 0, 96, 96), "compare-blur-corner.png");
    }

    static Bitmap stripeShot(int s, boolean aa, int edgeBlur) {
        StripeView v = new StripeView(Robolectric.buildActivity(SettingsActivity.class).setup().get());
        v.configure(8, 8, 0f, Prefs.DIR_DOWN_RIGHT, false, 0f, Prefs.ROT_CW, false, 1L, false,
                StripePattern.SCHEME_BW, 1f, 0f, aa, edgeBlur);
        v.measure(View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, s, s);
        v.setState(StripePattern.BASE_ANGLE_RAD, s / 2f, s / 2f, 0.25f);
        return drawView(v);
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

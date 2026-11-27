package com.zmlworks.stripeflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class EdgeFadeTest {

    @Test public void alphaIsOpaqueAtTheBorderAndClearInside() {
        int w = 200, h = 100;
        float r = 20f;
        assertEquals(1f, EdgeFade.alpha(0f, 50f, w, h, r), 1e-5f);
        assertEquals(1f, EdgeFade.alpha(100f, 0f, w, h, r), 1e-5f);
        assertEquals(0f, EdgeFade.alpha(100f, 50f, w, h, r), 1e-5f);
        assertEquals(0f, EdgeFade.alpha(20f, 50f, w, h, r), 1e-5f);
        float mid = EdgeFade.alpha(10f, 50f, w, h, r);
        assertTrue("half-radius should be between, got " + mid, mid > 0.4f && mid < 0.6f);
        assertEquals(0f, EdgeFade.alpha(100f, 50f, w, h, 0f), 0f);
    }

    @Test public void radiusIsClampedToHalfTheShorterSide() {
        assertEquals(0, EdgeFade.clampedRadius(16, 0, 100));
        assertEquals(0, EdgeFade.clampedRadius(-4, 200, 100));
        assertEquals(50, EdgeFade.clampedRadius(80, 200, 100));
        assertEquals(40, EdgeFade.clampedRadius(40, 200, 100));
    }

    @Test public void rampAndCornerShareTheSameFalloff() {
        int r = 16;
        Bitmap ramp = EdgeFade.makeRamp(r);
        Bitmap corner = EdgeFade.makeCorner(r);
        assertEquals(1, ramp.getWidth());
        assertEquals(r, ramp.getHeight());
        assertEquals(r, corner.getWidth());
        int outer = (ramp.getPixel(0, 0) >>> 24) & 0xff;
        int inner = (ramp.getPixel(0, r - 1) >>> 24) & 0xff;
        assertTrue(outer > 200);
        assertTrue(inner < 40);
        // The first row of the corner is on the top edge, so every pixel
        // matches the first ramp sample (distance 0.5 px).
        int expected = ramp.getPixel(0, 0);
        for (int x = 0; x < r; x++) {
            assertEquals("x=" + x, expected, corner.getPixel(x, 0));
            assertEquals(corner.getPixel(x, 0), corner.getPixel(0, x));
        }
        int centre = (corner.getPixel(r - 1, r - 1) >>> 24) & 0xff;
        assertTrue(centre < 40);
    }

    @Test public void overlayLeavesTheCentreUntouchedAndDarkensTheEdges() {
        int s = 120, r = 24;
        Bitmap bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(Color.WHITE);
        EdgeFade fade = new EdgeFade();
        fade.ensure(s, s, r);
        Canvas c = new Canvas(bmp);
        fade.draw(c);
        int mid = bmp.getPixel(s / 2, s / 2);
        int edge = bmp.getPixel(0, s / 2);
        int corner = bmp.getPixel(0, 0);
        assertEquals(Color.WHITE, mid);
        int edgeA = (edge >>> 24) & 0xff;
        // Drawn as premultiplied-ish SRC_OVER black; the RGB of the edge
        // should be darker than white.
        int edgeR = (edge >> 16) & 0xff;
        int cornerR = (corner >> 16) & 0xff;
        assertTrue("edge should darken, rgb=" + edgeR, edgeR < 40);
        assertTrue("corner should darken, rgb=" + cornerR, cornerR < 40);
        assertTrue(edgeA > 0 || edgeR < 255);
    }
}

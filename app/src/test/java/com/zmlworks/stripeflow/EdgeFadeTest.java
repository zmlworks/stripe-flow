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

    @Test public void squareCornersMatchTheStraightEdgeDistance() {
        int w = 200, h = 100, blur = 20;
        assertEquals(1f, EdgeFade.alpha(0f, 50f, w, h, blur, 0, 0, 0, 0), 1e-5f);
        assertEquals(0f, EdgeFade.alpha(100f, 50f, w, h, blur, 0, 0, 0, 0), 1e-5f);
        assertEquals(0f, EdgeFade.alpha(20f, 50f, w, h, blur, 0, 0, 0, 0), 1e-5f);
        float mid = EdgeFade.alpha(10f, 50f, w, h, blur, 0, 0, 0, 0);
        assertTrue("half-blur should be between, got " + mid, mid > 0.4f && mid < 0.6f);
        assertEquals(EdgeFade.distToRoundedEdge(10f, 50f, w, h, 0, 0, 0, 0), 10f, 1e-5f);
    }

    @Test public void roundedCornerIsOpaqueOutsideTheQuarterCircle() {
        int w = 200, h = 200, r = 40, blur = 20;
        // The far cut-off of the top-left corner is outside the screen.
        float outside = EdgeFade.distToRoundedEdge(0.5f, 0.5f, w, h, r, r, r, r);
        assertTrue("outside should be negative, d=" + outside, outside < 0f);
        assertEquals(1f, EdgeFade.alpha(0.5f, 0.5f, w, h, blur, r, r, r, r), 1e-5f);
        // Mid-arc of the quarter circle (45°): on the physical edge.
        float on = r - r / (float) Math.sqrt(2.0);
        float dEdge = EdgeFade.distToRoundedEdge(on, on, w, h, r, r, r, r);
        assertEquals(0f, dEdge, 0.02f);
        assertEquals(1f, EdgeFade.alpha(on, on, w, h, blur, r, r, r, r), 1e-4f);
        // Inward along 45° by the blur width: almost clear.
        float inward = on + blur / (float) Math.sqrt(2.0);
        float dIn = EdgeFade.distToRoundedEdge(inward, inward, w, h, r, r, r, r);
        assertEquals(blur, dIn, 0.05f);
        assertEquals(0f, EdgeFade.alpha(inward, inward, w, h, blur, r, r, r, r), 1e-4f);
        // Along the top straight edge, past the corner radius, still a linear inset.
        assertEquals(8f, EdgeFade.distToRoundedEdge(r + 10f, 8f, w, h, r, r, r, r), 1e-5f);
    }

    @Test public void blurAndCornerRadiiAreClamped() {
        assertEquals(0, EdgeFade.clampBlur(16, 0, 100));
        assertEquals(50, EdgeFade.clampBlur(80, 200, 100));
        assertEquals(0, EdgeFade.clampCorner(-4, 200, 100));
        assertEquals(50, EdgeFade.clampCorner(80, 200, 100));
        assertEquals(40, EdgeFade.clampCorner(40, 200, 100));
    }

    @Test public void maskCoversTheRoundedCornerAndLeavesTheCentreClear() {
        int s = 160, r = 32, blur = 24;
        int mix = StripePattern.perceivedMix(Color.WHITE, Color.BLACK, 8, 8);
        Bitmap bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(Color.WHITE);
        EdgeFade fade = new EdgeFade();
        fade.ensure(s, s, blur, r, r, r, r);
        fade.setColor(mix);
        fade.draw(new Canvas(bmp));
        assertEquals(Color.WHITE, bmp.getPixel(s / 2, s / 2) | 0);
        // The very corner is fully the mix colour (no stripe peeking).
        int corner = bmp.getPixel(0, 0);
        assertTrue(Math.abs(((mix >> 16) & 0xff) - ((corner >> 16) & 0xff)) <= 2);
        assertTrue(Math.abs(((mix >> 8) & 0xff) - ((corner >> 8) & 0xff)) <= 2);
        // Mid top edge is also close to the mix (almost opaque at 0.5 px).
        int edge = bmp.getPixel(s / 2, 0);
        assertTrue(Math.abs(((mix >> 16) & 0xff) - ((edge >> 16) & 0xff)) <= 8);
    }
}

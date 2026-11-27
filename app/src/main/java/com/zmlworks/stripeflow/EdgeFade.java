package com.zmlworks.stripeflow;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

/**
 * A soft fade from the stripe field to the perceived average colour, following
 * the rounded corners of the display. The mask (alpha only) is rebuilt when
 * the size, blur width or corner radii change. Recolouring is a color filter,
 * so a drifting hue can update the overlay without touching the mask.
 */
final class EdgeFade {
    private int width, height, blur, rtl, rtr, rbr, rbl;
    private int color = 0;
    private final Paint paint = new Paint(0);
    private Bitmap mask;

    EdgeFade() {
        paint.setDither(false);
        paint.setFilterBitmap(false);
        paint.setAntiAlias(false);
    }

    /**
     * Signed distance to the rounded-rect edge: positive inward, zero on the
     * physical edge, negative outside (in the cut-off corner).
     */
    static float distToRoundedEdge(float x, float y, int width, int height,
                                   int rtl, int rtr, int rbr, int rbl) {
        if (width <= 0 || height <= 0) return 0f;
        if (x < rtl && y < rtl && rtl > 0) {
            return rtl - (float) Math.hypot(rtl - x, rtl - y);
        }
        if (x > width - rtr && y < rtr && rtr > 0) {
            return rtr - (float) Math.hypot(x - (width - rtr), rtr - y);
        }
        if (x > width - rbr && y > height - rbr && rbr > 0) {
            return rbr - (float) Math.hypot(x - (width - rbr), y - (height - rbr));
        }
        if (x < rbl && y > height - rbl && rbl > 0) {
            return rbl - (float) Math.hypot(rbl - x, y - (height - rbl));
        }
        return Math.min(Math.min(x, y), Math.min(width - x, height - y));
    }

    /** Opacity of the overlay: 1 on or outside the physical edge, 0 past {@code blur}. */
    static float alpha(float x, float y, int width, int height, float blur,
                       int rtl, int rtr, int rbr, int rbl) {
        if (blur <= 0f) return 0f;
        float d = distToRoundedEdge(x, y, width, height, rtl, rtr, rbr, rbl);
        if (d <= 0f) return 1f;
        return alphaAtDist(d, blur);
    }

    static float alphaAtDist(float dist, float radius) {
        if (radius <= 0f) return 0f;
        if (dist >= radius) return 0f;
        if (dist <= 0f) return 1f;
        float u = dist / radius;
        float s = u * u * (3f - 2f * u);
        return 1f - s;
    }

    static int clampBlur(int blur, int width, int height) {
        if (blur <= 0 || width <= 0 || height <= 0) return 0;
        return Math.min(blur, Math.min(width, height) / 2);
    }

    static int clampCorner(int radius, int width, int height) {
        if (radius <= 0 || width <= 0 || height <= 0) return 0;
        return Math.min(radius, Math.min(width, height) / 2);
    }

    void ensure(int width, int height, int blur, int rtl, int rtr, int rbr, int rbl) {
        int b = clampBlur(blur, width, height);
        int tl = clampCorner(rtl, width, height);
        int tr = clampCorner(rtr, width, height);
        int br = clampCorner(rbr, width, height);
        int bl = clampCorner(rbl, width, height);
        if (this.width == width && this.height == height && this.blur == b
                && this.rtl == tl && this.rtr == tr && this.rbr == br && this.rbl == bl
                && mask != null) {
            return;
        }
        this.width = width;
        this.height = height;
        this.blur = b;
        this.rtl = tl;
        this.rtr = tr;
        this.rbr = br;
        this.rbl = bl;
        if (mask != null) {
            mask.recycle();
            mask = null;
        }
        if (b <= 0 || width <= 0 || height <= 0) return;
        mask = makeMask(width, height, b, tl, tr, br, bl);
    }

    static Bitmap makeMask(int width, int height, int blur, int rtl, int rtr, int rbr, int rbl) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setDensity(Bitmap.DENSITY_NONE);
        int[] px = new int[width];
        for (int y = 0; y < height; y++) {
            boolean any = false;
            for (int x = 0; x < width; x++) {
                float a = alpha(x + 0.5f, y + 0.5f, width, height, blur, rtl, rtr, rbr, rbl);
                if (a <= 0f) {
                    px[x] = 0;
                } else {
                    any = true;
                    int ai = Math.round(a * 255f);
                    px[x] = (ai << 24) | 0x00FFFFFF;
                }
            }
            if (any || y == 0) bmp.setPixels(px, 0, width, 0, y, width, 1);
        }
        return bmp;
    }

    void setColor(int rgb) {
        int c = 0xFF000000 | rgb;
        if (c == color && paint.getColorFilter() != null) return;
        color = c;
        paint.setColorFilter(new PorterDuffColorFilter(c, PorterDuff.Mode.SRC_IN));
    }

    int color() { return color; }

    void draw(Canvas c) {
        if (mask == null) return;
        if (paint.getColorFilter() == null) setColor(color == 0 ? 0xFFBCBCBC : color);
        c.drawBitmap(mask, 0, 0, paint);
    }
}

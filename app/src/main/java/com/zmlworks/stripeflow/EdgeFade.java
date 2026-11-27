package com.zmlworks.stripeflow;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * A soft fade from the stripe field to the perceived average colour, following
 * the rounded corners of the display. Only the four edge bands and four
 * corner tiles are drawn; the centre is left untouched, so a colour filter
 * cannot tint the stripes. The alpha tiles are rebuilt when the size, blur
 * width or corner radii change. Recolouring is {@link Paint#setColor}.
 */
final class EdgeFade {
    private int width, height, blur, rtl, rtr, rbr, rbl;
    private int color = 0;
    private final Paint paint = new Paint(0);
    private final Rect srcV = new Rect();
    private final Rect srcH = new Rect();
    private final Rect dst = new Rect();
    /** 1 x blur, y = 0 is the outer (opaque) end. */
    private Bitmap vRamp;
    /** blur x 1, x = 0 is the outer (opaque) end. */
    private Bitmap hRamp;
    private Bitmap tl, tr, br, bl;

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

    /**
     * Alpha in a top-left corner tile of size {@code radius + blur}. The tile
     * covers the rounded pocket and the adjoining straight-edge fade so the
     * side bands can start after {@code radius + blur} without a gap.
     */
    static float cornerAlpha(float x, float y, int radius, float blur) {
        if (blur <= 0f) return 0f;
        float d;
        if (radius > 0 && x < radius && y < radius) {
            d = radius - (float) Math.hypot(radius - x, radius - y);
        } else {
            d = Math.min(x, y);
        }
        if (d <= 0f) return 1f;
        return alphaAtDist(d, blur);
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
        int ctl = clampCorner(rtl, width, height);
        int ctr = clampCorner(rtr, width, height);
        int cbr = clampCorner(rbr, width, height);
        int cbl = clampCorner(rbl, width, height);
        if (this.width == width && this.height == height && this.blur == b
                && this.rtl == ctl && this.rtr == ctr && this.rbr == cbr && this.rbl == cbl
                && vRamp != null) {
            return;
        }
        this.width = width;
        this.height = height;
        this.blur = b;
        this.rtl = ctl;
        this.rtr = ctr;
        this.rbr = cbr;
        this.rbl = cbl;
        recycleTiles();
        if (b <= 0 || width <= 0 || height <= 0) return;
        vRamp = makeVRamp(b);
        hRamp = makeHRamp(b);
        tl = makeCorner(ctl, b);
        tr = makeCorner(ctr, b);
        br = makeCorner(cbr, b);
        bl = makeCorner(cbl, b);
    }

    private void recycleTiles() {
        vRamp = recycle(vRamp);
        hRamp = recycle(hRamp);
        tl = recycle(tl);
        tr = recycle(tr);
        br = recycle(br);
        bl = recycle(bl);
    }

    private static Bitmap recycle(Bitmap b) {
        if (b != null) b.recycle();
        return null;
    }

    static Bitmap makeVRamp(int blur) {
        Bitmap bmp = Bitmap.createBitmap(1, blur, Bitmap.Config.ALPHA_8);
        bmp.setDensity(Bitmap.DENSITY_NONE);
        for (int y = 0; y < blur; y++) {
            float a = alphaAtDist(y + 0.5f, blur);
            bmp.setPixel(0, y, Math.round(a * 255f) << 24);
        }
        return bmp;
    }

    static Bitmap makeHRamp(int blur) {
        Bitmap bmp = Bitmap.createBitmap(blur, 1, Bitmap.Config.ALPHA_8);
        bmp.setDensity(Bitmap.DENSITY_NONE);
        for (int x = 0; x < blur; x++) {
            float a = alphaAtDist(x + 0.5f, blur);
            bmp.setPixel(x, 0, Math.round(a * 255f) << 24);
        }
        return bmp;
    }

    static Bitmap makeCorner(int radius, int blur) {
        int s = radius + blur;
        if (s <= 0) return null;
        Bitmap bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ALPHA_8);
        bmp.setDensity(Bitmap.DENSITY_NONE);
        int[] px = new int[s];
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float a = cornerAlpha(x + 0.5f, y + 0.5f, radius, blur);
                px[x] = Math.round(a * 255f) << 24;
            }
            bmp.setPixels(px, 0, s, 0, y, s, 1);
        }
        return bmp;
    }

    void setColor(int rgb) {
        int c = 0xFF000000 | rgb;
        if (c == color && paint.getColor() == c && paint.getColorFilter() == null) return;
        color = c;
        paint.setColorFilter(null);
        paint.setColor(c);
    }

    int color() { return color; }

    /** Texels in the edge tiles (for tests). Much smaller than a full-screen mask. */
    int overlayTexels() {
        int n = 0;
        if (vRamp != null) n += vRamp.getWidth() * vRamp.getHeight();
        if (hRamp != null) n += hRamp.getWidth() * hRamp.getHeight();
        if (tl != null) n += tl.getWidth() * tl.getHeight();
        if (tr != null) n += tr.getWidth() * tr.getHeight();
        if (br != null) n += br.getWidth() * br.getHeight();
        if (bl != null) n += bl.getWidth() * bl.getHeight();
        return n;
    }

    void draw(Canvas c) {
        if (vRamp == null || hRamp == null || blur <= 0) return;
        if (paint.getColorFilter() != null || color == 0 || paint.getColor() != color) {
            setColor(color == 0 ? 0xFFBCBCBC : color);
        }
        int b = blur;
        int midX = width / 2;
        int midY = height / 2;

        int topL = rtl + b, topR = width - rtr - b;
        if (topR > topL) {
            srcV.set(0, 0, 1, b);
            dst.set(topL, 0, topR, b);
            c.drawBitmap(vRamp, srcV, dst, paint);
        }
        int botL = rbl + b, botR = width - rbr - b;
        if (botR > botL) {
            c.save();
            c.translate(0, height);
            c.scale(1f, -1f);
            srcV.set(0, 0, 1, b);
            dst.set(botL, 0, botR, b);
            c.drawBitmap(vRamp, srcV, dst, paint);
            c.restore();
        }
        int leftT = rtl + b, leftB = height - rbl - b;
        if (leftB > leftT) {
            srcH.set(0, 0, b, 1);
            dst.set(0, leftT, b, leftB);
            c.drawBitmap(hRamp, srcH, dst, paint);
        }
        int rightT = rtr + b, rightB = height - rbr - b;
        if (rightB > rightT) {
            c.save();
            c.translate(width, 0);
            c.scale(-1f, 1f);
            srcH.set(0, 0, b, 1);
            dst.set(0, rightT, b, rightB);
            c.drawBitmap(hRamp, srcH, dst, paint);
            c.restore();
        }
        drawCorner(c, tl, 0, 0, 1, 1, 0, 0, midX, midY);
        drawCorner(c, tr, width, 0, -1, 1, midX, 0, width, midY);
        drawCorner(c, br, width, height, -1, -1, midX, midY, width, height);
        drawCorner(c, bl, 0, height, 1, -1, 0, midY, midX, height);
    }

    private void drawCorner(Canvas c, Bitmap tile, int ox, int oy, int sx, int sy,
                            int clipL, int clipT, int clipR, int clipB) {
        if (tile == null) return;
        c.save();
        c.clipRect(clipL, clipT, clipR, clipB);
        c.translate(ox, oy);
        c.scale(sx, sy);
        c.drawBitmap(tile, 0, 0, paint);
        c.restore();
    }
}

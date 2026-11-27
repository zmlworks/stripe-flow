package com.zmlworks.stripeflow;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * A soft fade to black at the four edges of the stripe view (a vignette of
 * the display area, not of the stripe bands). A side ramp and a corner tile
 * are built when the radius or size changes; each frame draws four side
 * strips and four corners. The same smoothstep is used on sides and corners
 * so they meet without a seam or a darker overlap.
 */
final class EdgeFade {
    private int width, height, radius;
    private final Paint paint = new Paint(0);
    private final Matrix mat = new Matrix();
    private final Rect srcRamp = new Rect();
    private final RectF dstRamp = new RectF();
    private Bitmap ramp;
    private Bitmap corner;

    EdgeFade() {
        paint.setDither(false);
        paint.setFilterBitmap(false);
        paint.setAntiAlias(false);
    }

    /** Distance to the nearest outer edge; 0 on the border, larger inward. */
    static float distToEdge(float x, float y, int width, int height) {
        return Math.min(Math.min(x, y), Math.min(width - x, height - y));
    }

    /**
     * Opacity of the black overlay: 1 at the outer edge, 0 once the distance
     * is at least {@code radius}. A smoothstep keeps the falloff soft.
     */
    static float alpha(float x, float y, int width, int height, float radius) {
        return alphaAtDist(distToEdge(x, y, width, height), radius);
    }

    static float alphaAtDist(float dist, float radius) {
        if (radius <= 0f) return 0f;
        if (dist >= radius) return 0f;
        if (dist <= 0f) return 1f;
        float u = dist / radius;
        float s = u * u * (3f - 2f * u);
        return 1f - s;
    }

    static int clampedRadius(int radius, int width, int height) {
        if (radius <= 0 || width <= 0 || height <= 0) return 0;
        int max = Math.min(width, height) / 2;
        return Math.min(radius, max);
    }

    void ensure(int width, int height, int radius) {
        int r = clampedRadius(radius, width, height);
        if (this.width == width && this.height == height && this.radius == r && ramp != null) return;
        this.width = width;
        this.height = height;
        this.radius = r;
        recycle(ramp);
        recycle(corner);
        ramp = null;
        corner = null;
        if (r <= 0) return;
        ramp = makeRamp(r);
        corner = makeCorner(r);
        srcRamp.set(0, 0, 1, r);
    }

    static Bitmap makeRamp(int r) {
        // A vertical 1×r strip: y = 0 is the outer edge (opaque black).
        Bitmap bmp = Bitmap.createBitmap(1, r, Bitmap.Config.ARGB_8888);
        bmp.setDensity(Bitmap.DENSITY_NONE);
        int[] px = new int[r];
        for (int y = 0; y < r; y++) {
            float a = alphaAtDist(y + 0.5f, r);
            px[y] = Math.round(a * 255f) << 24;
        }
        bmp.setPixels(px, 0, 1, 0, 0, 1, r);
        return bmp;
    }

    static Bitmap makeCorner(int r) {
        Bitmap bmp = Bitmap.createBitmap(r, r, Bitmap.Config.ARGB_8888);
        bmp.setDensity(Bitmap.DENSITY_NONE);
        int[] px = new int[r * r];
        for (int y = 0; y < r; y++) {
            for (int x = 0; x < r; x++) {
                float d = Math.min(x + 0.5f, y + 0.5f);
                float a = alphaAtDist(d, r);
                px[y * r + x] = Math.round(a * 255f) << 24;
            }
        }
        bmp.setPixels(px, 0, r, 0, 0, r, r);
        return bmp;
    }

    void draw(Canvas c) {
        int r = radius, w = width, h = height;
        if (r <= 0 || ramp == null || corner == null) return;
        int midR = w - r, midB = h - r;
        if (midR > r) {
            dstRamp.set(r, 0, midR, r);
            c.drawBitmap(ramp, srcRamp, dstRamp, paint);
            c.save();
            c.translate(r, h);
            c.scale(1f, -1f);
            dstRamp.set(0, 0, midR - r, r);
            c.drawBitmap(ramp, srcRamp, dstRamp, paint);
            c.restore();
        }
        if (midB > r) {
            c.save();
            c.translate(0, midB);
            c.rotate(-90f);
            dstRamp.set(0, 0, midB - r, r);
            c.drawBitmap(ramp, srcRamp, dstRamp, paint);
            c.restore();
            c.save();
            c.translate(w, r);
            c.rotate(90f);
            dstRamp.set(0, 0, midB - r, r);
            c.drawBitmap(ramp, srcRamp, dstRamp, paint);
            c.restore();
        }
        c.drawBitmap(corner, 0, 0, paint);
        drawMirrored(c, corner, -1f, 1f, w, 0);
        drawMirrored(c, corner, -1f, -1f, w, h);
        drawMirrored(c, corner, 1f, -1f, 0, h);
    }

    private void drawMirrored(Canvas c, Bitmap bmp, float sx, float sy, float tx, float ty) {
        mat.setScale(sx, sy);
        mat.postTranslate(tx, ty);
        c.drawBitmap(bmp, mat, paint);
    }

    private static void recycle(Bitmap b) {
        if (b != null) b.recycle();
    }
}

package com.zmlworks.stripeflow;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * 45-degree 1-device-pixel (or 2) black/white diagonal stripes.
 * Colour of pixel (x, y) is decided by floor((x + y + offset) / width) modulo 2,
 * point-sampled so every pixel is pure black or pure white.
 */
final class StripePattern {
    private StripePattern() {}

    static final int BLACK = Color.BLACK;
    static final int WHITE = Color.WHITE;

    /** Period of the pattern along the x+y axis, in physical pixels. */
    static int period(int width) {
        return 2 * (width == 2 ? 2 : 1);
    }

    /**
     * Pattern shift after {@code elapsedNanos}, wrapped to [0, period).
     * {@code direction} is +1 or -1. Speed is in physical pixels per second
     * along the x+y axis.
     */
    static float offset(long elapsedNanos, float speedPxPerSec, int direction, int width) {
        int p = period(width);
        float raw = (elapsedNanos / 1_000_000_000f) * speedPxPerSec * (direction >= 0 ? 1f : -1f);
        float wrapped = raw % p;
        if (wrapped < 0f) wrapped += p;
        return wrapped;
    }

    /** Pure black or white at integer pixel (x, y) for a given sub-pixel offset. */
    static int color(int x, int y, int width, float offset) {
        int w = width == 2 ? 2 : 1;
        int band = (int) Math.floor((x + y + offset) / (double) w);
        return (band & 1) == 0 ? BLACK : WHITE;
    }

    /** A repeating tile of the pattern at offset 0. Density is none, so 1 bitmap pixel = 1 device pixel. */
    static Bitmap tile(int width) {
        int w = width == 2 ? 2 : 1;
        int n = period(w);
        Bitmap b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        b.setDensity(Bitmap.DENSITY_NONE);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                b.setPixel(x, y, color(x, y, w, 0f));
            }
        }
        return b;
    }

    /** Fill {@code dest} by point-sampling the pattern. Used by tests. */
    static void render(Bitmap dest, int width, float offset) {
        int w = dest.getWidth(), h = dest.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                dest.setPixel(x, y, color(x, y, width, offset));
            }
        }
    }
}

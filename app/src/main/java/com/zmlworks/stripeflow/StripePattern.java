package com.zmlworks.stripeflow;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Black/white diagonal stripes of a chosen device-pixel width, measured along
 * the unit normal (cos θ, sin θ). The flow is the same axis, so it stays
 * perpendicular to the stripes as the angle changes.
 * Colour is point-sampled: every pixel is pure black or pure white.
 * The repeating period along the normal is 2 * width, so the loop is seamless.
 */
final class StripePattern {
    private StripePattern() {}

    static final int BLACK = Color.BLACK;
    static final int WHITE = Color.WHITE;
    /** Starting angle: stripes like ＼, normal toward the bottom-right (y down). */
    static final float BASE_ANGLE_RAD = (float) (Math.PI / 4.0);

    static int normWidth(int width) {
        return width < 1 ? 1 : width;
    }

    static int period(int width) {
        return 2 * normWidth(width);
    }

    static float wrap(float value, float period) {
        if (period <= 0f) return 0f;
        float w = value % period;
        if (w < 0f) w += period;
        return w;
    }

    /** Flow offset after {@code elapsedNanos}, wrapped to [0, period). */
    static float flowOffset(long elapsedNanos, float speedPxPerSec, int direction, int width) {
        float raw = (elapsedNanos / 1_000_000_000f) * speedPxPerSec * (direction >= 0 ? 1f : -1f);
        return wrap(raw, period(width));
    }

    static float phase(float x, float y, float px, float py, float angleRad, float offset) {
        double c = Math.cos(angleRad), s = Math.sin(angleRad);
        return (float) ((x - px) * c + (y - py) * s + offset);
    }

    static int colorFromPhase(float phase, int width) {
        int w = normWidth(width);
        // A sub-pixel bias absorbs -eps from sin/cos of 90°, which would
        // otherwise floor a nearly-zero phase (e.g. x * cos(90°)) to -1.
        int band = (int) Math.floor((phase + 1e-3) / (double) w);
        return (band & 1) == 0 ? BLACK : WHITE;
    }

    static int color(float x, float y, float px, float py, float angleRad, float offset, int width) {
        return colorFromPhase(phase(x, y, px, py, angleRad, offset), width);
    }

    /**
     * How much to add to the flow offset when the pivot moves, so the field
     * does not translate (only later rotation is around the new pivot).
     */
    static float pivotCompDelta(float oldPx, float oldPy, float newPx, float newPy, float angleRad) {
        double c = Math.cos(angleRad), s = Math.sin(angleRad);
        return (float) ((newPx - oldPx) * c + (newPy - oldPy) * s);
    }

    /**
     * A tile of vertical bands (constant in y). A shader matrix then rotates
     * it so the bands are perpendicular to {@code angleRad}. Density is none,
     * so 1 bitmap pixel = 1 device pixel along the normal.
     */
    static Bitmap tile(int width) {
        int w = normWidth(width);
        int n = period(w);
        Bitmap b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        b.setDensity(Bitmap.DENSITY_NONE);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                b.setPixel(x, y, colorFromPhase(x, w));
            }
        }
        return b;
    }

    /** Fill {@code dest} by point-sampling. Used by tests and previews. */
    static void render(Bitmap dest, int width, float px, float py, float angleRad, float offset) {
        int w = dest.getWidth(), h = dest.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                dest.setPixel(x, y, color(x, y, px, py, angleRad, offset, width));
            }
        }
    }
}

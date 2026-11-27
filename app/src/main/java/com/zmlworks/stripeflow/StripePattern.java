package com.zmlworks.stripeflow;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Diagonal stripes of a chosen device-pixel width, measured along
 * the unit normal (cos θ, sin θ). The flow is the same axis, so it stays
 * perpendicular to the stripes as the angle changes.
 * Colour is point-sampled: every pixel is one of the two scheme colours,
 * unless anti-aliasing is on, in which case a linear ramp (default 1 px,
 * optionally narrower, and optionally biased into the black band) is
 * box-filtered into a 1:1 tile.
 * The white (colour A) and black (colour B) bands may have different widths.
 * The repeating period along the normal is widthA + widthB.
 */
final class StripePattern {
    private StripePattern() {}

    static final int BLACK = Color.BLACK;
    static final int WHITE = Color.WHITE;
    static final int SCHEME_BW = 0, SCHEME_COLOR = 1;
    /** Starting angle: stripes like ＼, normal toward the bottom-right (y down). */
    static final float BASE_ANGLE_RAD = (float) (Math.PI / 4.0);
    /** Sub-samples per texel when box-filtering an anti-aliased tile. */
    static final int AA_SCALE = 4;
    static final int AA_TILE_HEIGHT = 2;

    static int normWidth(int width) {
        return width < 1 ? 1 : width;
    }

    static float normWidthF(float width) {
        return width < 1f ? 1f : width;
    }

    static int period(int widthA, int widthB) {
        return normWidth(widthA) + normWidth(widthB);
    }

    static float periodF(float widthA, float widthB) {
        return normWidthF(widthA) + normWidthF(widthB);
    }

    static float wrap(float value, float period) {
        if (period <= 0f) return 0f;
        float w = value % period;
        if (w < 0f) w += period;
        return w;
    }

    /** Flow offset after {@code elapsedNanos}, wrapped to [0, period). */
    static float flowOffset(long elapsedNanos, float speedPxPerSec, int direction, int widthA, int widthB) {
        float raw = (elapsedNanos / 1_000_000_000f) * speedPxPerSec * (direction >= 0 ? 1f : -1f);
        return wrap(raw, period(widthA, widthB));
    }

    static float phase(float x, float y, float px, float py, float angleRad, float offset) {
        double c = Math.cos(angleRad), s = Math.sin(angleRad);
        return (float) ((x - px) * c + (y - py) * s + offset);
    }

    static float wrapHue(float hueDeg) {
        float h = hueDeg % 360f;
        if (h < 0f) h += 360f;
        return h;
    }

    static float complementHue(float hueDeg) {
        return wrapHue(hueDeg + 180f);
    }

    /** Pure HSV colour (S = V = 1) at {@code hueDeg}. Reuses {@code hsv} of length 3. */
    static int colorAtHue(float hueDeg, float[] hsv) {
        hsv[0] = wrapHue(hueDeg);
        hsv[1] = 1f;
        hsv[2] = 1f;
        return Color.HSVToColor(hsv);
    }

    static int colorAtHue(float hueDeg) {
        return colorAtHue(hueDeg, new float[3]);
    }

    /** HSV colour at the given hue (degrees), saturation and value in 0..1. */
    static int colorAtHsv(float hueDeg, float sat, float value, float[] hsv) {
        hsv[0] = wrapHue(hueDeg);
        hsv[1] = Prefs.clampF(sat, 0f, 1f);
        hsv[2] = Prefs.clampF(value, 0f, 1f);
        return android.graphics.Color.HSVToColor(hsv);
    }

    static int colorAtHsv(float hueDeg, float sat, float value) {
        return colorAtHsv(hueDeg, sat, value, new float[3]);
    }

    static int colorFromPhase(float phase, int widthA, int widthB) {
        int a = normWidth(widthA), b = normWidth(widthB);
        // A sub-pixel bias absorbs -eps from sin/cos of 90°, which would
        // otherwise wrap a nearly-zero phase (e.g. x * cos(90°)) to the
        // previous period. Within one period, [0, b) is black (colour B)
        // and [b, a+b) is white (colour A).
        float w = wrap(phase + 1e-3f, a + b);
        return w < b ? BLACK : WHITE;
    }

    static int color(float x, float y, float px, float py, float angleRad, float offset,
                     int widthA, int widthB) {
        return colorFromPhase(phase(x, y, px, py, angleRad, offset), widthA, widthB);
    }

    /**
     * Half-width in device pixels of a symmetric anti-aliased linear ramp
     * (0.5 on each side when the edge is 1 px), clamped so a 1 px band still
     * reaches full colour at its centre in the continuous coverage.
     */
    static float aaHalf(float widthA, float widthB) {
        return aaHalf(widthA, widthB, Prefs.AA_EDGE_DEFAULT);
    }

    static float aaHalf(float widthA, float widthB, float edgePx) {
        float edge = Prefs.clampF(edgePx, Prefs.AA_EDGE_MIN, Prefs.AA_EDGE_MAX);
        return Math.min(0.5f * edge, 0.5f * Math.min(normWidthF(widthA), normWidthF(widthB)));
    }

    /**
     * Coverage of colour A (white) at {@code phase}, with a linear ramp of
     * half-width {@code half} around each boundary. 0 is colour B (black).
     */
    static float coverage(float phase, float widthA, float widthB, float half) {
        float a = normWidthF(widthA), b = normWidthF(widthB);
        float p = a + b;
        if (half <= 0f) return wrap(phase + 1e-3f, p) < b ? 0f : 1f;
        float w = wrap(phase, p);
        boolean inBlack = w < b;
        float dist = inBlack ? Math.min(w, b - w) : Math.min(w - b, p - w);
        if (dist >= half) return inBlack ? 0f : 1f;
        float u = dist / half;
        return inBlack ? 0.5f * (1f - u) : 0.5f * (1f + u);
    }

    /**
     * Coverage with a chosen edge width. When {@code whitePriority} is false
     * this is the symmetric ramp of {@link #aaHalf(float, float, float)}.
     * When it is true, colour A stays at full coverage across its whole
     * width and the ramp of width {@code edgePx} sits only in colour B.
     */
    static float coverage(float phase, float widthA, float widthB, float edgePx,
                          boolean whitePriority) {
        if (!whitePriority) return coverage(phase, widthA, widthB, aaHalf(widthA, widthB, edgePx));
        float a = normWidthF(widthA), b = normWidthF(widthB);
        float p = a + b;
        float ramp = Prefs.clampF(edgePx, Prefs.AA_EDGE_MIN, Prefs.AA_EDGE_MAX);
        float w = wrap(phase, p);
        if (w >= b) return 1f;
        if (ramp <= 0f) return 0f;
        float fromLeft = w;
        float fromRight = b - w;
        return Math.max(rampCov(fromLeft, ramp), rampCov(fromRight, ramp));
    }

    static float rampCov(float dist, float ramp) {
        if (dist >= ramp) return 0f;
        if (dist <= 0f) return 1f;
        return 1f - dist / ramp;
    }

    static int gray(float coverage) {
        int g = Math.round(Math.max(0f, Math.min(1f, coverage)) * 255f);
        return 0xFF000000 | (g << 16) | (g << 8) | g;
    }

    /** Lerp from {@code colorB} (coverage 0) to {@code colorA} (coverage 1). */
    static int mixColors(int colorB, int colorA, int maskArgb) {
        int g = maskArgb & 0xff;
        if (g <= 0) return colorB;
        if (g >= 255) return colorA;
        return lerpArgb(colorB, colorA, g / 255f);
    }

    /** sRGB component 0..1 to linear light. */
    static float srgbToLinear(float c) {
        if (c <= 0f) return 0f;
        if (c >= 1f) return 1f;
        return c <= 0.04045f ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4);
    }

    /** Linear light 0..1 to sRGB component. */
    static float linearToSrgb(float c) {
        if (c <= 0f) return 0f;
        if (c >= 1f) return 1f;
        return c <= 0.0031308f ? 12.92f * c
                : (float) (1.055 * Math.pow(c, 1.0 / 2.4) - 0.055);
    }

    static float hueDelta(float a, float b) {
        float d = Math.abs(wrapHue(a) - wrapHue(b));
        return Math.min(d, 360f - d);
    }

    /**
     * Perceived average of colour A and colour B, weighted by their band
     * widths, mixed in linear light and written back to sRGB. Equal widths
     * of black and white yield about {@code #BCBCBC}.
     */
    static int perceivedMix(int colorA, int colorB, float widthA, float widthB) {
        float a = widthA > 0f ? widthA : 0f;
        float b = widthB > 0f ? widthB : 0f;
        float p = a + b;
        if (p <= 0f) { a = 1f; b = 1f; p = 2f; }
        float wa = a / p, wb = b / p;
        int r = chan(colorA, 16, colorB, wa, wb);
        int g = chan(colorA, 8, colorB, wa, wb);
        int bl = chan(colorA, 0, colorB, wa, wb);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static int chan(int cA, int shift, int cB, float wa, float wb) {
        float la = srgbToLinear(((cA >> shift) & 0xff) / 255f);
        float lb = srgbToLinear(((cB >> shift) & 0xff) / 255f);
        return Math.round(linearToSrgb(wa * la + wb * lb) * 255f);
    }

    static int lerpArgb(int c0, int c1, float t) {
        if (t <= 0f) return c0;
        if (t >= 1f) return c1;
        int a0 = (c0 >>> 24) & 0xff, r0 = (c0 >>> 16) & 0xff, g0 = (c0 >>> 8) & 0xff, b0 = c0 & 0xff;
        int a1 = (c1 >>> 24) & 0xff, r1 = (c1 >>> 16) & 0xff, g1 = (c1 >>> 8) & 0xff, b1 = c1 & 0xff;
        int a = Math.round(a0 + t * (a1 - a0));
        int r = Math.round(r0 + t * (r1 - r0));
        int g = Math.round(g0 + t * (g1 - g0));
        int b = Math.round(b0 + t * (b1 - b0));
        return (a << 24) | (r << 16) | (g << 8) | b;
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
    static Bitmap tile(int widthA, int widthB) {
        int a = normWidth(widthA), b = normWidth(widthB);
        int n = period(a, b);
        Bitmap bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        bmp.setDensity(Bitmap.DENSITY_NONE);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                bmp.setPixel(x, y, colorFromPhase(x, a, b));
            }
        }
        return bmp;
    }

    /**
     * When {@code aa} is false, the hard 1:1 tile. When it is true, a 1-D
     * tile (two rows, 1 texel = 1 device pixel) whose coverage is box-filtered
     * from the chosen boundary ramp. Bilinear sampling then keeps rotation
     * and sub-pixel flow smooth, and REPEAT seams stay periodic.
     */
    static Bitmap tile(int widthA, int widthB, boolean aa) {
        return tile(widthA, widthB, aa, Prefs.AA_EDGE_DEFAULT, false);
    }

    static Bitmap tile(int widthA, int widthB, boolean aa, float edgePx, boolean whitePriority) {
        if (!aa) return tile(widthA, widthB);
        return aaTile(normWidth(widthA), normWidth(widthB), edgePx, whitePriority);
    }

    static Bitmap tile(float widthA, float widthB, boolean aa) {
        return tile(widthA, widthB, aa, Prefs.AA_EDGE_DEFAULT, false);
    }

    static Bitmap tile(float widthA, float widthB, boolean aa, float edgePx, boolean whitePriority) {
        if (!aa && widthA == (int) widthA && widthB == (int) widthB) {
            return tile((int) widthA, (int) widthB);
        }
        return aaTile(normWidthF(widthA), normWidthF(widthB), edgePx, whitePriority);
    }

    private static Bitmap aaTile(float widthA, float widthB, float edgePx, boolean whitePriority) {
        float period = periodF(widthA, widthB);
        // 1 texel = 1 device pixel. Fractional widths round the period to an
        // integer texel count and map the true period onto that width, so
        // REPEAT is exactly one cycle.
        int tw = Math.max(1, Math.round(period));
        int th = AA_TILE_HEIGHT;
        Bitmap bmp = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888);
        bmp.setDensity(Bitmap.DENSITY_NONE);
        int[] row = new int[tw];
        for (int x = 0; x < tw; x++) {
            float x0 = x * period / tw;
            float step = period / tw / AA_SCALE;
            float acc = 0f;
            for (int i = 0; i < AA_SCALE; i++) {
                acc += coverage(x0 + (i + 0.5f) * step, widthA, widthB, edgePx, whitePriority);
            }
            row[x] = gray(acc / AA_SCALE);
        }
        for (int y = 0; y < th; y++) {
            bmp.setPixels(row, 0, tw, 0, y, tw, 1);
        }
        return bmp;
    }

    /** Fill {@code dest} by point-sampling. Used by tests and previews. */
    static void render(Bitmap dest, int widthA, int widthB, float px, float py, float angleRad, float offset) {
        int w = dest.getWidth(), h = dest.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                dest.setPixel(x, y, color(x, y, px, py, angleRad, offset, widthA, widthB));
            }
        }
    }
}

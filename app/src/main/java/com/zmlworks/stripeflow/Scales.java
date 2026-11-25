package com.zmlworks.stripeflow;

/**
 * Non-linear slider maps so the low end of wide ranges stays usable.
 * Flow speed uses a square curve (0..300 px/s). Rotation speed uses a cube
 * (0..90 °/s). Distribution scale is logarithmic (0.1×..5×). Hue rate uses
 * a cube (0..36 °/s) so the slow end stays usable and 0 is included.
 */
final class Scales {
    private Scales() {}

    static final int STEPS = 1000;

    static int speedFromProgress(int progress) {
        float t = progress / (float) STEPS;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return Math.round(Prefs.SPEED_MAX * t * t);
    }

    static int progressFromSpeed(int speed) {
        speed = Prefs.clamp(speed, Prefs.SPEED_MIN, Prefs.SPEED_MAX);
        return Math.round(STEPS * (float) Math.sqrt(speed / (float) Prefs.SPEED_MAX));
    }

    static float rotFromProgress(int progress) {
        float t = progress / (float) STEPS;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return Prefs.ROT_MAX * t * t * t;
    }

    static int progressFromRot(float degPerSec) {
        degPerSec = Prefs.clampF(degPerSec, Prefs.ROT_MIN, Prefs.ROT_MAX);
        return Math.round(STEPS * (float) Math.cbrt(degPerSec / Prefs.ROT_MAX));
    }

    static float distFromProgress(int progress) {
        float t = progress / (float) STEPS;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return Prefs.DIST_MIN * (float) Math.pow(Prefs.DIST_MAX / Prefs.DIST_MIN, t);
    }

    static int progressFromDist(float scale) {
        scale = Prefs.clampF(scale, Prefs.DIST_MIN, Prefs.DIST_MAX);
        double u = Math.log(scale / Prefs.DIST_MIN) / Math.log(Prefs.DIST_MAX / Prefs.DIST_MIN);
        return Math.round(STEPS * (float) u);
    }

    static float hueRateFromProgress(int progress) {
        float t = progress / (float) STEPS;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return Prefs.HUE_RATE_MAX * t * t * t;
    }

    static int progressFromHueRate(float degPerSec) {
        degPerSec = Prefs.clampF(degPerSec, Prefs.HUE_RATE_MIN, Prefs.HUE_RATE_MAX);
        return Math.round(STEPS * (float) Math.cbrt(degPerSec / Prefs.HUE_RATE_MAX));
    }
}

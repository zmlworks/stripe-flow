package com.zmlworks.stripeflow;

/**
 * Non-linear slider maps so the low end of wide ranges stays usable.
 * Flow speed uses a square curve (0..300 px/s). Rotation speed uses a cube
 * (0..90 °/s).
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
}

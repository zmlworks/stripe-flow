package com.zmlworks.stripeflow;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.WindowManager;

/** Settings in SharedPreferences "settings". Invalid stored values are clamped. */
final class Prefs {
    private Prefs() {}

    static final String SPEED = "speed_px_s";
    static final String WIDTH = "stripe_width";
    static final String DIR = "direction";
    static final String SYS_BRIGHT = "use_system_brightness";
    static final String BRIGHT = "brightness";
    static final String ROTATE = "rotate";
    static final String ROT_SPEED = "rotate_speed_deg_s";
    static final String ROT_DIR = "rotate_dir";
    static final String WANDER = "wander_pivot";

    static final int SPEED_MIN = 0, SPEED_MAX = 300, SPEED_DEFAULT = 3;
    static final int WIDTH_MIN = 1, WIDTH_MAX = 64, WIDTH_DEFAULT = 1;
    /** +1 = against the normal, -1 = along the normal (down-right at the starting 45°). */
    static final int DIR_DOWN_RIGHT = -1, DIR_UP_LEFT = 1, DIR_DEFAULT = DIR_DOWN_RIGHT;
    static final float BRIGHT_DEFAULT = 1f;
    static final float ROT_MIN = 0f, ROT_MAX = 90f, ROT_DEFAULT = 0.5f;
    /** +1 = clockwise on screen (y down), -1 = counterclockwise. */
    static final int ROT_CW = 1, ROT_CCW = -1, ROT_DIR_DEFAULT = ROT_CW;

    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    static int speed(Context c) {
        return clamp(sp(c).getInt(SPEED, SPEED_DEFAULT), SPEED_MIN, SPEED_MAX);
    }

    static void setSpeed(Context c, int v) {
        sp(c).edit().putInt(SPEED, clamp(v, SPEED_MIN, SPEED_MAX)).apply();
    }

    /** Stripe thickness in physical pixels along the normal: 1..64. Older 1..10 stay valid. */
    static int width(Context c) {
        return clamp(sp(c).getInt(WIDTH, WIDTH_DEFAULT), WIDTH_MIN, WIDTH_MAX);
    }

    static void setWidth(Context c, int v) {
        sp(c).edit().putInt(WIDTH, clamp(v, WIDTH_MIN, WIDTH_MAX)).apply();
    }

    static int direction(Context c) {
        int d = sp(c).getInt(DIR, DIR_DEFAULT);
        return d >= 0 ? DIR_UP_LEFT : DIR_DOWN_RIGHT;
    }

    static void setDirection(Context c, int v) {
        sp(c).edit().putInt(DIR, v >= 0 ? DIR_UP_LEFT : DIR_DOWN_RIGHT).apply();
    }

    static boolean useSystemBrightness(Context c) {
        return sp(c).getBoolean(SYS_BRIGHT, true);
    }

    static void setUseSystemBrightness(Context c, boolean v) {
        sp(c).edit().putBoolean(SYS_BRIGHT, v).apply();
    }

    /** 0..1 when a custom brightness is set. Unused while useSystemBrightness is true. */
    static float brightness(Context c) {
        return clampF(sp(c).getFloat(BRIGHT, BRIGHT_DEFAULT), 0f, 1f);
    }

    static void setBrightness(Context c, float v) {
        sp(c).edit().putFloat(BRIGHT, clampF(v, 0f, 1f)).apply();
    }

    /** Value for WindowManager.LayoutParams.screenBrightness on the stripe screen only. */
    static float windowBrightness(Context c) {
        if (useSystemBrightness(c)) return WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        return brightness(c);
    }

    static boolean rotate(Context c) {
        return sp(c).getBoolean(ROTATE, false);
    }

    static void setRotate(Context c, boolean v) {
        sp(c).edit().putBoolean(ROTATE, v).apply();
    }

    static float rotateSpeed(Context c) {
        return clampF(sp(c).getFloat(ROT_SPEED, ROT_DEFAULT), ROT_MIN, ROT_MAX);
    }

    static void setRotateSpeed(Context c, float v) {
        sp(c).edit().putFloat(ROT_SPEED, clampF(v, ROT_MIN, ROT_MAX)).apply();
    }

    static int rotateDir(Context c) {
        int d = sp(c).getInt(ROT_DIR, ROT_DIR_DEFAULT);
        return d < 0 ? ROT_CCW : ROT_CW;
    }

    static void setRotateDir(Context c, int v) {
        sp(c).edit().putInt(ROT_DIR, v < 0 ? ROT_CCW : ROT_CW).apply();
    }

    static boolean wander(Context c) {
        return sp(c).getBoolean(WANDER, false);
    }

    static void setWander(Context c, boolean v) {
        sp(c).edit().putBoolean(WANDER, v).apply();
    }

    static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    static float clampF(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

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

    static final int SPEED_MIN = 1, SPEED_MAX = 30, SPEED_DEFAULT = 3;
    static final int WIDTH_MIN = 1, WIDTH_MAX = 10, WIDTH_DEFAULT = 1;
    /** +1 = toward top-left, -1 = toward bottom-right (the default). */
    static final int DIR_DOWN_RIGHT = -1, DIR_UP_LEFT = 1, DIR_DEFAULT = DIR_DOWN_RIGHT;
    static final float BRIGHT_DEFAULT = 1f;

    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    static int speed(Context c) {
        return clamp(sp(c).getInt(SPEED, SPEED_DEFAULT), SPEED_MIN, SPEED_MAX);
    }

    static void setSpeed(Context c, int v) {
        sp(c).edit().putInt(SPEED, clamp(v, SPEED_MIN, SPEED_MAX)).apply();
    }

    /** Stripe thickness in physical pixels: 1..10. Stored 1 or 2 from older versions stay valid. */
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
        float b = sp(c).getFloat(BRIGHT, BRIGHT_DEFAULT);
        if (b < 0f) return 0f;
        if (b > 1f) return 1f;
        return b;
    }

    static void setBrightness(Context c, float v) {
        if (v < 0f) v = 0f;
        if (v > 1f) v = 1f;
        sp(c).edit().putFloat(BRIGHT, v).apply();
    }

    /** Value for WindowManager.LayoutParams.screenBrightness on the stripe screen only. */
    static float windowBrightness(Context c) {
        if (useSystemBrightness(c)) return WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        return brightness(c);
    }

    static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

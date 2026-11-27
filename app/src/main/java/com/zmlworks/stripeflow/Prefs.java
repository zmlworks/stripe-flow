package com.zmlworks.stripeflow;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.WindowManager;

/** Settings in SharedPreferences "settings". Invalid stored values are clamped. */
final class Prefs {
    private Prefs() {}

    static final String SPEED = "speed_px_s";
    static final String WIDTH_A = "stripe_width_a";
    static final String WIDTH_B = "stripe_width_b";
    static final String DIR = "direction";
    static final String SYS_BRIGHT = "use_system_brightness";
    static final String BRIGHT = "brightness";
    static final String ROTATE = "rotate";
    static final String ROT_SPEED = "rotate_speed_deg_s";
    static final String ROT_DIR = "rotate_dir";
    static final String WANDER = "wander_pivot";
    static final String AUTO = "auto_drift";
    static final String AUTO_WIDTH_A = "auto_stripe_width_a";
    static final String AUTO_WIDTH_B = "auto_stripe_width_b";
    static final String COLOR = "color_scheme";
    static final String DIST = "dist_scale";
    static final String HUE_RATE = "hue_rate_deg_s";

    static final int SPEED_MIN = 0, SPEED_MAX = 300, SPEED_DEFAULT = 3;
    static final int WIDTH_MIN = 1, WIDTH_MAX = 64, WIDTH_DEFAULT = 1, AUTO_WIDTH_DEFAULT = 8;
    /** +1 = against the normal, -1 = along the normal (down-right at the starting 45°). */
    static final int DIR_DOWN_RIGHT = -1, DIR_UP_LEFT = 1, DIR_DEFAULT = DIR_DOWN_RIGHT;
    static final int COLOR_BW = StripePattern.SCHEME_BW, COLOR_MODE = StripePattern.SCHEME_COLOR, COLOR_DEFAULT = COLOR_BW;
    static final float DIST_MIN = 0.1f, DIST_MAX = 5f, DIST_DEFAULT = 1f;
    static final float HUE_RATE_MIN = HueDrift.RATE_MIN, HUE_RATE_MAX = HueDrift.RATE_MAX, HUE_RATE_DEFAULT = HueDrift.RATE_DEFAULT;
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

    /** White (colour A) band thickness in physical pixels along the normal: 1..64. */
    static int widthA(Context c) {
        return clamp(sp(c).getInt(WIDTH_A, WIDTH_DEFAULT), WIDTH_MIN, WIDTH_MAX);
    }

    static void setWidthA(Context c, int v) {
        sp(c).edit().putInt(WIDTH_A, clamp(v, WIDTH_MIN, WIDTH_MAX)).apply();
    }

    /** Black (colour B) band thickness in physical pixels along the normal: 1..64. */
    static int widthB(Context c) {
        return clamp(sp(c).getInt(WIDTH_B, WIDTH_DEFAULT), WIDTH_MIN, WIDTH_MAX);
    }

    static void setWidthB(Context c, int v) {
        sp(c).edit().putInt(WIDTH_B, clamp(v, WIDTH_MIN, WIDTH_MAX)).apply();
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

    static boolean autoDrift(Context c) {
        return sp(c).getBoolean(AUTO, false);
    }

    static void setAutoDrift(Context c, boolean v) {
        sp(c).edit().putBoolean(AUTO, v).apply();
    }

    /** Auto-drift white (colour A) width. Default 8; independent of the manual widths. */
    static int autoWidthA(Context c) {
        return clamp(sp(c).getInt(AUTO_WIDTH_A, AUTO_WIDTH_DEFAULT), WIDTH_MIN, WIDTH_MAX);
    }

    static void setAutoWidthA(Context c, int v) {
        sp(c).edit().putInt(AUTO_WIDTH_A, clamp(v, WIDTH_MIN, WIDTH_MAX)).apply();
    }

    /** Auto-drift black (colour B) width. Default 8; independent of the manual widths. */
    static int autoWidthB(Context c) {
        return clamp(sp(c).getInt(AUTO_WIDTH_B, AUTO_WIDTH_DEFAULT), WIDTH_MIN, WIDTH_MAX);
    }

    static void setAutoWidthB(Context c, int v) {
        sp(c).edit().putInt(AUTO_WIDTH_B, clamp(v, WIDTH_MIN, WIDTH_MAX)).apply();
    }

    static int colorScheme(Context c) {
        int v = sp(c).getInt(COLOR, COLOR_DEFAULT);
        return v == COLOR_MODE ? COLOR_MODE : COLOR_BW;
    }

    static void setColorScheme(Context c, int v) {
        sp(c).edit().putInt(COLOR, v == COLOR_MODE ? COLOR_MODE : COLOR_BW).apply();
    }

    static float distScale(Context c) {
        return clampF(sp(c).getFloat(DIST, DIST_DEFAULT), DIST_MIN, DIST_MAX);
    }

    static void setDistScale(Context c, float v) {
        sp(c).edit().putFloat(DIST, clampF(v, DIST_MIN, DIST_MAX)).apply();
    }

    static float hueRate(Context c) {
        return clampF(sp(c).getFloat(HUE_RATE, HUE_RATE_DEFAULT), HUE_RATE_MIN, HUE_RATE_MAX);
    }

    static void setHueRate(Context c, float v) {
        sp(c).edit().putFloat(HUE_RATE, clampF(v, HUE_RATE_MIN, HUE_RATE_MAX)).apply();
    }

    static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    static float clampF(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

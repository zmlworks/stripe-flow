package com.zmlworks.stripeflow;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

/**
 * Immersive full-screen stripes. When tap-to-blackout is off, a tap
 * returns to settings. When it is on, a first tap blacks the screen out
 * (animation paused, the phone may time out and lock as usual) and a
 * second tap, or the back gesture or button, returns to settings.
 * Unlocking the phone leaves the blackout in place.
 */
public final class StripeActivity extends Activity {
    private StripeView view;
    private boolean blackout;
    private boolean tapBlackout;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams lp = w.getAttributes();
        lp.screenBrightness = Prefs.windowBrightness(this);
        if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        w.setAttributes(lp);
        tapBlackout = Prefs.tapBlackout(this);
        view = new StripeView(this);
        setContentView(view);
        view.requestApplyInsets();
        hideSystemBars();
        applyStripeWindowFlags();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @Override protected void onResume() {
        super.onResume();
        hideSystemBars();
        applyStripeWindowFlags();
        if (view != null) view.requestApplyInsets();
        if (view != null && !blackout) view.setRunning(true);
    }

    @Override protected void onPause() {
        if (view != null) view.setRunning(false);
        super.onPause();
    }

    @Override public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (!tapBlackout || blackout) finish();
            else enterBlackout();
            return true;
        }
        return super.onTouchEvent(ev);
    }

    @Override public void onBackPressed() {
        finish();
    }

    boolean blackout() { return blackout; }

    private void enterBlackout() {
        if (blackout) return;
        blackout = true;
        if (view != null) view.setBlackout(true);
        applyStripeWindowFlags();
    }

    /**
     * In blackout, drop FLAG_KEEP_SCREEN_ON and any brightness override so the
     * phone's normal timeout and lock apply. Restored while the stripes run.
     */
    private void applyStripeWindowFlags() {
        Window w = getWindow();
        if (w == null) return;
        if (blackout) {
            w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (view != null) view.setKeepScreenOn(false);
        } else {
            w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (view != null) view.setKeepScreenOn(true);
        }
        WindowManager.LayoutParams lp = w.getAttributes();
        lp.screenBrightness = blackout
                ? WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                : Prefs.windowBrightness(this);
        w.setAttributes(lp);
    }

    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        Window w = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
            WindowInsetsController c = w.getDecorView() != null ? w.getInsetsController() : null;
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }
}

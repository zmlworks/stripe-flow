package com.zmlworks.stripeflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.pm.ActivityInfo;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class StripeActivityTest {

    @Test public void firstTapBlacksOutWithoutFinishingAndClearsKeepScreenOn() {
        ActivityController<StripeActivity> c = Robolectric.buildActivity(StripeActivity.class).setup();
        StripeActivity a = c.get();
        assertFalse(a.isFinishing());
        assertFalse(a.blackout());
        assertTrue(keepScreenOn(a));
        a.dispatchTouchEvent(down(12f, 20f));
        assertFalse(a.isFinishing());
        assertTrue(a.blackout());
        assertFalse(keepScreenOn(a));
        assertEquals(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE,
                a.getWindow().getAttributes().screenBrightness, 0f);
        StripeView sv = contentView(a);
        assertTrue(sv.blackout());
        assertFalse(sv.running());
    }

    @Test public void secondTapResumesStripesAndRestoresKeepScreenOn() {
        ActivityController<StripeActivity> c = Robolectric.buildActivity(StripeActivity.class).setup();
        StripeActivity a = c.get();
        a.dispatchTouchEvent(down(12f, 20f));
        assertTrue(a.blackout());
        a.dispatchTouchEvent(down(40f, 40f));
        assertFalse(a.isFinishing());
        assertFalse(a.blackout());
        assertTrue(keepScreenOn(a));
        StripeView sv = contentView(a);
        assertFalse(sv.blackout());
        assertTrue(sv.running());
    }

    @Test public void backFromBlackoutReturnsToSettings() {
        ActivityController<StripeActivity> c = Robolectric.buildActivity(StripeActivity.class).setup();
        StripeActivity a = c.get();
        a.dispatchTouchEvent(down(12f, 20f));
        assertTrue(a.blackout());
        a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
        assertTrue(a.isFinishing());
    }

    @Test public void pauseAndResumeInBlackoutDoNotRestartTheStripesOrKeepTheScreenOn() {
        ActivityController<StripeActivity> c = Robolectric.buildActivity(StripeActivity.class).setup();
        StripeActivity a = c.get();
        a.dispatchTouchEvent(down(12f, 20f));
        c.pause();
        assertTrue(a.blackout());
        assertFalse(contentView(a).running());
        c.resume();
        assertTrue(a.blackout());
        assertFalse(a.isFinishing());
        assertFalse(keepScreenOn(a));
        assertFalse(contentView(a).running());
        assertTrue(contentView(a).blackout());
    }

    @Test public void stripeScreenLocksOrientationAndSettingsDoesNot() {
        StripeActivity stripe = Robolectric.buildActivity(StripeActivity.class).setup().get();
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED,
                stripe.getRequestedOrientation());
        SettingsActivity settings = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        org.junit.Assert.assertNotEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED,
                settings.getRequestedOrientation());
    }

    @Test public void windowUsesCustomBrightnessUntilBlackout() {
        android.content.Context app = RuntimeEnvironment.getApplication();
        Prefs.sp(app).edit().clear().commit();
        Prefs.setUseSystemBrightness(app, false);
        Prefs.setBrightness(app, 0.25f);
        ActivityController<StripeActivity> c = Robolectric.buildActivity(StripeActivity.class).setup();
        StripeActivity a = c.get();
        assertEquals(0.25f, a.getWindow().getAttributes().screenBrightness, 0.0001f);
        a.dispatchTouchEvent(down(1f, 1f));
        assertEquals(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE,
                a.getWindow().getAttributes().screenBrightness, 0f);
        a.dispatchTouchEvent(down(1f, 1f));
        assertEquals(0.25f, a.getWindow().getAttributes().screenBrightness, 0.0001f);
        c.pause().destroy();

        Prefs.setUseSystemBrightness(app, true);
        ActivityController<StripeActivity> c2 = Robolectric.buildActivity(StripeActivity.class).setup();
        assertEquals(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE,
                c2.get().getWindow().getAttributes().screenBrightness, 0.0001f);
    }

    private static MotionEvent down(float x, float y) {
        return MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0);
    }

    private static boolean keepScreenOn(StripeActivity a) {
        int flags = a.getWindow().getAttributes().flags;
        return (flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0;
    }

    private static StripeView contentView(StripeActivity a) {
        android.view.View inner = a.findViewById(android.R.id.content);
        if (inner instanceof StripeView) return (StripeView) inner;
        if (inner instanceof android.view.ViewGroup) {
            android.view.View child = ((android.view.ViewGroup) inner).getChildAt(0);
            if (child instanceof StripeView) return (StripeView) child;
        }
        throw new AssertionError("StripeView not found in " + inner);
    }
}

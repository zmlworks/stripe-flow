package com.zmlworks.stripeflow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.MotionEvent;
import android.view.WindowManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class StripeActivityTest {

    @Test public void touchFinishesAndReturnsToCaller() {
        ActivityController<StripeActivity> c = Robolectric.buildActivity(StripeActivity.class).setup();
        StripeActivity a = c.get();
        assertFalse(a.isFinishing());
        a.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 12f, 20f, 0));
        assertTrue(a.isFinishing());
    }

    @Test public void windowUsesCustomBrightness() {
        android.content.Context app = RuntimeEnvironment.getApplication();
        Prefs.sp(app).edit().clear().commit();
        Prefs.setUseSystemBrightness(app, false);
        Prefs.setBrightness(app, 0.25f);
        ActivityController<StripeActivity> c = Robolectric.buildActivity(StripeActivity.class).setup();
        assertEqualsBright(0.25f, c.get().getWindow().getAttributes().screenBrightness);
        c.pause().destroy();

        Prefs.setUseSystemBrightness(app, true);
        ActivityController<StripeActivity> c2 = Robolectric.buildActivity(StripeActivity.class).setup();
        assertEqualsBright(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE,
                c2.get().getWindow().getAttributes().screenBrightness);
    }

    private static void assertEqualsBright(float expected, float got) {
        org.junit.Assert.assertEquals(expected, got, 0.0001f);
    }
}

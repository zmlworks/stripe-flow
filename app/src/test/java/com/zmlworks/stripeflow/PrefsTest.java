package com.zmlworks.stripeflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.WindowManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class PrefsTest {
    private Context c;

    @Before public void setUp() {
        c = RuntimeEnvironment.getApplication();
        Prefs.sp(c).edit().clear().commit();
    }

    @Test public void defaults() {
        assertEquals(3, Prefs.speed(c));
        assertEquals(1, Prefs.width(c));
        assertEquals(Prefs.DIR_DOWN_RIGHT, Prefs.direction(c));
        assertTrue(Prefs.useSystemBrightness(c));
        assertEquals(1f, Prefs.brightness(c), 0f);
        assertEquals(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE, Prefs.windowBrightness(c), 0f);
    }

    @Test public void roundTrip() {
        Prefs.setSpeed(c, 7);
        Prefs.setWidth(c, 7);
        Prefs.setDirection(c, Prefs.DIR_UP_LEFT);
        Prefs.setUseSystemBrightness(c, false);
        Prefs.setBrightness(c, 0.4f);
        assertEquals(7, Prefs.speed(c));
        assertEquals(7, Prefs.width(c));
        assertEquals(Prefs.DIR_UP_LEFT, Prefs.direction(c));
        assertFalse(Prefs.useSystemBrightness(c));
        assertEquals(0.4f, Prefs.brightness(c), 0.0001f);
        assertEquals(0.4f, Prefs.windowBrightness(c), 0.0001f);
    }

    @Test public void clampsInvalidValues() {
        Prefs.setSpeed(c, 0);
        assertEquals(Prefs.SPEED_MIN, Prefs.speed(c));
        Prefs.setSpeed(c, 99);
        assertEquals(Prefs.SPEED_MAX, Prefs.speed(c));
        Prefs.setWidth(c, 0);
        assertEquals(Prefs.WIDTH_MIN, Prefs.width(c));
        Prefs.setWidth(c, 99);
        assertEquals(Prefs.WIDTH_MAX, Prefs.width(c));
        // Values stored by v0.1.0 (1 or 2) remain valid with no migration.
        Prefs.sp(c).edit().putInt(Prefs.WIDTH, 2).commit();
        assertEquals(2, Prefs.width(c));
        Prefs.setBrightness(c, -0.5f);
        assertEquals(0f, Prefs.brightness(c), 0f);
        Prefs.setBrightness(c, 1.5f);
        assertEquals(1f, Prefs.brightness(c), 0f);
        // raw garbage in the file is also clamped
        Prefs.sp(c).edit().putInt(Prefs.SPEED, -3).putInt(Prefs.WIDTH, 0).putFloat(Prefs.BRIGHT, 9f).commit();
        assertEquals(Prefs.SPEED_MIN, Prefs.speed(c));
        assertEquals(1, Prefs.width(c));
        assertEquals(1f, Prefs.brightness(c), 0f);
    }
}

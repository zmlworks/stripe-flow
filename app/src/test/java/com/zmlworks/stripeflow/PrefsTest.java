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
        assertFalse(Prefs.rotate(c));
        assertEquals(0.5f, Prefs.rotateSpeed(c), 0.0001f);
        assertEquals(Prefs.ROT_CW, Prefs.rotateDir(c));
        assertFalse(Prefs.wander(c));
        assertFalse(Prefs.autoDrift(c));
    }

    @Test public void roundTrip() {
        Prefs.setSpeed(c, 7);
        Prefs.setWidth(c, 40);
        Prefs.setDirection(c, Prefs.DIR_UP_LEFT);
        Prefs.setUseSystemBrightness(c, false);
        Prefs.setBrightness(c, 0.4f);
        Prefs.setRotate(c, true);
        Prefs.setRotateSpeed(c, 12.5f);
        Prefs.setRotateDir(c, Prefs.ROT_CCW);
        Prefs.setWander(c, true);
        Prefs.setAutoDrift(c, true);
        assertEquals(7, Prefs.speed(c));
        assertEquals(40, Prefs.width(c));
        assertEquals(Prefs.DIR_UP_LEFT, Prefs.direction(c));
        assertFalse(Prefs.useSystemBrightness(c));
        assertEquals(0.4f, Prefs.brightness(c), 0.0001f);
        assertTrue(Prefs.rotate(c));
        assertEquals(12.5f, Prefs.rotateSpeed(c), 0.0001f);
        assertEquals(Prefs.ROT_CCW, Prefs.rotateDir(c));
        assertTrue(Prefs.wander(c));
        assertTrue(Prefs.autoDrift(c));
    }

    @Test public void clampsInvalidValuesAndKeepsOldWidths() {
        Prefs.setSpeed(c, -1);
        assertEquals(Prefs.SPEED_MIN, Prefs.speed(c));
        Prefs.setSpeed(c, 0);
        assertEquals(0, Prefs.speed(c));
        Prefs.setSpeed(c, 999);
        assertEquals(Prefs.SPEED_MAX, Prefs.speed(c));
        Prefs.setWidth(c, 0);
        assertEquals(Prefs.WIDTH_MIN, Prefs.width(c));
        Prefs.setWidth(c, 99);
        assertEquals(Prefs.WIDTH_MAX, Prefs.width(c));
        // Values stored by v0.1.x (1, 2 or up to 10) remain valid with no migration.
        Prefs.sp(c).edit().putInt(Prefs.WIDTH, 2).commit();
        assertEquals(2, Prefs.width(c));
        Prefs.sp(c).edit().putInt(Prefs.WIDTH, 10).commit();
        assertEquals(10, Prefs.width(c));
        Prefs.setBrightness(c, -0.5f);
        assertEquals(0f, Prefs.brightness(c), 0f);
        Prefs.setBrightness(c, 1.5f);
        assertEquals(1f, Prefs.brightness(c), 0f);
        Prefs.setRotateSpeed(c, -3f);
        assertEquals(0f, Prefs.rotateSpeed(c), 0f);
        Prefs.setRotateSpeed(c, 180f);
        assertEquals(90f, Prefs.rotateSpeed(c), 0f);
        Prefs.sp(c).edit().putInt(Prefs.SPEED, -3).putInt(Prefs.WIDTH, 0).putFloat(Prefs.BRIGHT, 9f).commit();
        assertEquals(Prefs.SPEED_MIN, Prefs.speed(c));
        assertEquals(1, Prefs.width(c));
        assertEquals(1f, Prefs.brightness(c), 0f);
    }

    @Test public void autoDriftDoesNotOverwriteWidth() {
        Prefs.setWidth(c, 12);
        Prefs.setAutoDrift(c, true);
        assertEquals(12, Prefs.width(c));
        assertTrue(Prefs.autoDrift(c));
        Prefs.setAutoDrift(c, false);
        assertEquals(12, Prefs.width(c));
        assertFalse(Prefs.autoDrift(c));
    }
}

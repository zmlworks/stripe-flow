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
        assertEquals(1, Prefs.widthA(c));
        assertEquals(1, Prefs.widthB(c));
        assertEquals(Prefs.DIR_DOWN_RIGHT, Prefs.direction(c));
        assertTrue(Prefs.useSystemBrightness(c));
        assertEquals(1f, Prefs.brightness(c), 0f);
        assertEquals(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE, Prefs.windowBrightness(c), 0f);
        assertFalse(Prefs.rotate(c));
        assertEquals(0.5f, Prefs.rotateSpeed(c), 0.0001f);
        assertEquals(Prefs.ROT_CW, Prefs.rotateDir(c));
        assertFalse(Prefs.wander(c));
        assertFalse(Prefs.autoDrift(c));
        assertEquals(8, Prefs.autoWidthA(c));
        assertEquals(8, Prefs.autoWidthB(c));
        assertEquals(Prefs.COLOR_BW, Prefs.colorScheme(c));
        assertEquals(1f, Prefs.distScale(c), 0f);
        assertEquals(2f, Prefs.hueRate(c), 0f);
        assertFalse(Prefs.antiAlias(c));
        assertFalse(Prefs.edgeBlur(c));
        assertEquals(64, Prefs.edgeBlurWidth(c));
    }

    @Test public void roundTrip() {
        Prefs.setSpeed(c, 7);
        Prefs.setWidthA(c, 40);
        Prefs.setWidthB(c, 12);
        Prefs.setDirection(c, Prefs.DIR_UP_LEFT);
        Prefs.setUseSystemBrightness(c, false);
        Prefs.setBrightness(c, 0.4f);
        Prefs.setRotate(c, true);
        Prefs.setRotateSpeed(c, 12.5f);
        Prefs.setRotateDir(c, Prefs.ROT_CCW);
        Prefs.setWander(c, true);
        Prefs.setAutoDrift(c, true);
        Prefs.setAutoWidthA(c, 24);
        Prefs.setAutoWidthB(c, 16);
        Prefs.setColorScheme(c, Prefs.COLOR_MODE);
        Prefs.setDistScale(c, 2.5f);
        Prefs.setHueRate(c, 0f);
        Prefs.setAntiAlias(c, true);
        Prefs.setEdgeBlur(c, true);
        Prefs.setEdgeBlurWidth(c, 48);
        assertEquals(7, Prefs.speed(c));
        assertEquals(40, Prefs.widthA(c));
        assertEquals(12, Prefs.widthB(c));
        assertEquals(Prefs.DIR_UP_LEFT, Prefs.direction(c));
        assertFalse(Prefs.useSystemBrightness(c));
        assertEquals(0.4f, Prefs.brightness(c), 0.0001f);
        assertTrue(Prefs.rotate(c));
        assertEquals(12.5f, Prefs.rotateSpeed(c), 0.0001f);
        assertEquals(Prefs.ROT_CCW, Prefs.rotateDir(c));
        assertTrue(Prefs.wander(c));
        assertTrue(Prefs.autoDrift(c));
        assertEquals(24, Prefs.autoWidthA(c));
        assertEquals(16, Prefs.autoWidthB(c));
        assertEquals(Prefs.COLOR_MODE, Prefs.colorScheme(c));
        assertEquals(2.5f, Prefs.distScale(c), 0.0001f);
        assertEquals(0f, Prefs.hueRate(c), 0f);
        assertTrue(Prefs.antiAlias(c));
        assertTrue(Prefs.edgeBlur(c));
        assertEquals(48, Prefs.edgeBlurWidth(c));
    }

    @Test public void clampsInvalidValues() {
        Prefs.setSpeed(c, -1);
        assertEquals(Prefs.SPEED_MIN, Prefs.speed(c));
        Prefs.setSpeed(c, 0);
        assertEquals(0, Prefs.speed(c));
        Prefs.setSpeed(c, 999);
        assertEquals(Prefs.SPEED_MAX, Prefs.speed(c));
        Prefs.setWidthA(c, 0);
        assertEquals(Prefs.WIDTH_MIN, Prefs.widthA(c));
        Prefs.setWidthA(c, 99);
        assertEquals(Prefs.WIDTH_MAX, Prefs.widthA(c));
        Prefs.setWidthB(c, 0);
        assertEquals(Prefs.WIDTH_MIN, Prefs.widthB(c));
        Prefs.setWidthB(c, 99);
        assertEquals(Prefs.WIDTH_MAX, Prefs.widthB(c));
        Prefs.setBrightness(c, -0.5f);
        assertEquals(0f, Prefs.brightness(c), 0f);
        Prefs.setBrightness(c, 1.5f);
        assertEquals(1f, Prefs.brightness(c), 0f);
        Prefs.setRotateSpeed(c, -3f);
        assertEquals(0f, Prefs.rotateSpeed(c), 0f);
        Prefs.setRotateSpeed(c, 180f);
        assertEquals(90f, Prefs.rotateSpeed(c), 0f);
        Prefs.setAutoWidthA(c, 0);
        assertEquals(Prefs.WIDTH_MIN, Prefs.autoWidthA(c));
        Prefs.setAutoWidthA(c, 99);
        assertEquals(Prefs.WIDTH_MAX, Prefs.autoWidthA(c));
        Prefs.setAutoWidthB(c, 0);
        assertEquals(Prefs.WIDTH_MIN, Prefs.autoWidthB(c));
        Prefs.setAutoWidthB(c, 99);
        assertEquals(Prefs.WIDTH_MAX, Prefs.autoWidthB(c));
        Prefs.setDistScale(c, 0.01f);
        assertEquals(Prefs.DIST_MIN, Prefs.distScale(c), 0f);
        Prefs.setDistScale(c, 9f);
        assertEquals(Prefs.DIST_MAX, Prefs.distScale(c), 0f);
        Prefs.setHueRate(c, -1f);
        assertEquals(0f, Prefs.hueRate(c), 0f);
        Prefs.setHueRate(c, 99f);
        assertEquals(Prefs.HUE_RATE_MAX, Prefs.hueRate(c), 0f);
        Prefs.setEdgeBlurWidth(c, -4);
        assertEquals(Prefs.EDGE_BLUR_MIN, Prefs.edgeBlurWidth(c));
        Prefs.setEdgeBlurWidth(c, 999);
        assertEquals(Prefs.EDGE_BLUR_MAX, Prefs.edgeBlurWidth(c));
        Prefs.sp(c).edit().putInt(Prefs.SPEED, -3)
                .putInt(Prefs.WIDTH_A, 0).putInt(Prefs.WIDTH_B, 0)
                .putFloat(Prefs.BRIGHT, 9f).commit();
        assertEquals(Prefs.SPEED_MIN, Prefs.speed(c));
        assertEquals(1, Prefs.widthA(c));
        assertEquals(1, Prefs.widthB(c));
        assertEquals(1f, Prefs.brightness(c), 0f);
    }

    @Test public void autoDriftDoesNotOverwriteWidth() {
        Prefs.setWidthA(c, 12);
        Prefs.setWidthB(c, 4);
        Prefs.setAutoDrift(c, true);
        assertEquals(12, Prefs.widthA(c));
        assertEquals(4, Prefs.widthB(c));
        assertEquals(8, Prefs.autoWidthA(c));
        assertEquals(8, Prefs.autoWidthB(c));
        Prefs.setAutoWidthA(c, 16);
        Prefs.setAutoWidthB(c, 20);
        assertEquals(12, Prefs.widthA(c));
        assertEquals(4, Prefs.widthB(c));
        assertEquals(16, Prefs.autoWidthA(c));
        assertEquals(20, Prefs.autoWidthB(c));
        Prefs.setAutoDrift(c, false);
        assertEquals(12, Prefs.widthA(c));
        assertEquals(4, Prefs.widthB(c));
        assertEquals(16, Prefs.autoWidthA(c));
        assertEquals(20, Prefs.autoWidthB(c));
        assertFalse(Prefs.autoDrift(c));
    }

    @Test public void unknownColorSchemeFallsBackToBlackWhite() {
        Prefs.sp(c).edit().putInt(Prefs.COLOR, 9).commit();
        assertEquals(Prefs.COLOR_BW, Prefs.colorScheme(c));
        assertEquals(1f, Prefs.distScale(c), 0f);
        assertEquals(2f, Prefs.hueRate(c), 0f);
    }
}

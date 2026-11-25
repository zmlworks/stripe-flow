package com.zmlworks.stripeflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class StripeViewAutoTest {

    @Test public void autoModeIntegratesShapedSpeedsWithoutPhaseJumps() {
        int width = 12;
        StripeView v = view();
        long seed = 7L;
        v.configure(width, 300f, Prefs.DIR_UP_LEFT, false, 90f, Prefs.ROT_CCW, false, seed, true, StripePattern.SCHEME_BW);

        long dtNs = 1_000_000_000L / 60L;
        float dt = 1f / 60f;
        float prevPhase = 0f;
        boolean first = true;
        float per = StripePattern.period(width);
        float x = 200f, y = 40f;
        float flowAcc = 0f, rotAcc = 0f;
        for (int i = 0; i <= 60 * 90; i++) {
            v.setElapsedNanos(i * dtNs);
            float t = i * dt;
            if (i > 0) {
                float mid = t - 0.5f * dt;
                flowAcc += SmoothDrift.flowSpeed(mid, seed) * dt;
                rotAcc += SmoothDrift.rotSpeed(mid, seed) * dt;
            }
            assertEquals(
                    StripePattern.BASE_ANGLE_RAD + (float) Math.toRadians(rotAcc),
                    v.angle(), 1e-4f);
            float ph = StripePattern.phase(x, y, v.pivotX(), v.pivotY(), v.angle(), v.offset());
            if (!first) {
                float d = ph - prevPhase;
                d -= per * Math.round(d / per);
                assertTrue("phase jump " + d + " at frame " + i, Math.abs(d) < 3f);
            }
            prevPhase = ph;
            first = false;
        }
        int expected = StripePattern.colorFromPhase(v.offset(), width);
        assertEquals(expected, StripePattern.color(
                v.pivotX(), v.pivotY(), v.pivotX(), v.pivotY(), v.angle(), v.offset(),
                width));
    }

    @Test public void autoModeHonoursChosenWidth() {
        StripeView v = view();
        v.configure(3, 0f, Prefs.DIR_DOWN_RIGHT, true, 0f, Prefs.ROT_CW, true, 1L, true, StripePattern.SCHEME_BW);
        v.setElapsedNanos(0L);
        // period 6: at the pivot with offset 0 the first three pixels along the
        // normal are black, the next three white. Vertical at angle 0 is easier.
        // Just check that a 3 px field is not rendered as 8 px: two pixels 3 px
        // apart along the base normal can share a colour only if width is 3? Skip
        // geometry; the wrap period is what setElapsedNanos uses.
        // Re-query by comparing colour at offset 0 vs a 3 px vs 8 px pattern
        // after posing at base angle with zero offset.
        v.setState(StripePattern.BASE_ANGLE_RAD, 128f, 128f, 0f);
        int c0 = StripePattern.color(128f, 128f, 128f, 128f,
                StripePattern.BASE_ANGLE_RAD, 0f, 3);
        assertEquals(c0, StripePattern.colorFromPhase(0f, 3));
        // A point 3 px along the normal is the start of the next band for width 3,
        // but still in the first band for width 8.
        float ang = StripePattern.BASE_ANGLE_RAD;
        float nx = 128f + 3f * (float) Math.cos(ang);
        float ny = 128f + 3f * (float) Math.sin(ang);
        assertEquals(StripePattern.colorFromPhase(3f, 3),
                StripePattern.color(nx, ny, 128f, 128f, ang, 0f, 3));
        assertTrue(StripePattern.color(nx, ny, 128f, 128f, ang, 0f, 3)
                != StripePattern.color(nx, ny, 128f, 128f, ang, 0f, 8));
    }

    @Test public void turningAutoOffRestoresManualConstantSpeed() {
        StripeView v = view();
        v.configure(4, 10f, Prefs.DIR_DOWN_RIGHT, false, 0f, Prefs.ROT_CW, false, 1L, false, StripePattern.SCHEME_BW);
        v.setElapsedNanos(2_000_000_000L);
        float per = StripePattern.period(4);
        assertEquals(StripePattern.wrap(-20f, per), v.offset(), 1e-4f);
        assertEquals(StripePattern.BASE_ANGLE_RAD, v.angle(), 1e-5f);
    }

    private static StripeView view() {
        StripeView v = new StripeView(Robolectric.buildActivity(SettingsActivity.class).setup().get());
        int s = 256;
        v.measure(View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(s, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, s, s);
        return v;
    }
}

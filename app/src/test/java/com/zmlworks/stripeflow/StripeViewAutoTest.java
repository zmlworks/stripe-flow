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

    @Test public void autoModeUsesFixedWidthAndIntegratedSpeeds() {
        StripeView v = view();
        long seed = 7L;
        v.configure(1, 300f, Prefs.DIR_UP_LEFT, false, 90f, Prefs.ROT_CCW, false, seed, true);

        long dtNs = 1_000_000_000L / 60L;
        float dt = 1f / 60f;
        float prevPhase = 0f;
        boolean first = true;
        float per = StripePattern.period(SmoothDrift.WIDTH);
        // A pixel well away from the centre, so both rotation and flow show up.
        float x = 200f, y = 40f;
        for (int i = 0; i <= 60 * 90; i++) {
            long nanos = i * dtNs;
            v.setElapsedNanos(nanos);
            float t = i * dt;
            assertEquals(
                    StripePattern.BASE_ANGLE_RAD
                            + (float) Math.toRadians(SmoothDrift.rotIntegral(t, seed)),
                    v.angle(), 1e-5f);
            float ph = StripePattern.phase(x, y, v.pivotX(), v.pivotY(), v.angle(), v.offset());
            if (!first) {
                float d = ph - prevPhase;
                d -= per * Math.round(d / per);
                // Rotation of a ~200 px lever at 5 °/s plus 20 px/s of flow, with a little
                // room for pivot motion, is still well under a few pixels per frame.
                assertTrue("phase jump " + d + " at frame " + i, Math.abs(d) < 3f);
            }
            prevPhase = ph;
            first = false;
        }
        // Colour at the current pivot depends only on the flow integral plus compensation.
        int expected = StripePattern.colorFromPhase(v.offset(), SmoothDrift.WIDTH);
        assertEquals(expected, StripePattern.color(
                v.pivotX(), v.pivotY(), v.pivotX(), v.pivotY(), v.angle(), v.offset(),
                SmoothDrift.WIDTH));
    }

    @Test public void turningAutoOffRestoresManualConstantSpeed() {
        StripeView v = view();
        v.configure(4, 10f, Prefs.DIR_DOWN_RIGHT, false, 0f, Prefs.ROT_CW, false, 1L, false);
        v.setElapsedNanos(2_000_000_000L);
        // direction down-right is -1, so offset = -10 * 2 = -20, wrapped into [0, 8).
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

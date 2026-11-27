package com.zmlworks.stripeflow;

import android.content.Context;
import android.os.Build;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.Choreographer;
import android.view.View;

/**
 * Full-screen stripe field. A 1-D repeating tile is drawn with a BitmapShader
 * whose local matrix encodes the current angle, pivot and flow offset.
 * One shader texel is one device pixel along the normal.
 */
public final class StripeView extends View implements Choreographer.FrameCallback {
    private final Paint paint = new Paint(0);
    private final Matrix matrix = new Matrix();
    private final float[] mat = new float[9];
    private final float[] pivot = new float[2];
    private final float[] prevPivot = new float[2];
    private Bitmap tile;
    private BitmapShader shader;
    private int widthA = 1, widthB = 1;
    private float speed = Prefs.SPEED_DEFAULT;
    private int direction = Prefs.DIR_DEFAULT;
    private boolean rotate;
    private float rotDegPerSec = Prefs.ROT_DEFAULT;
    private int rotDir = Prefs.ROT_DIR_DEFAULT;
    private boolean wander;
    private boolean auto;
    private int colorScheme = StripePattern.SCHEME_BW;
    private float distScale = Prefs.DIST_DEFAULT;
    private float hueRate = Prefs.HUE_RATE_DEFAULT;
    private int[] maskPx, colorPx;
    private final float[] hsv = new float[3];
    private float lastHue = Float.NaN;
    private long wanderSeed = 1L;
    private long startNanos;
    private boolean running;
    private boolean havePivot;
    private float pivotComp;
    private float offset;
    private float angle = StripePattern.BASE_ANGLE_RAD;
    private float autoT;
    private float autoFlowOff;
    private float autoAng = StripePattern.BASE_ANGLE_RAD;
    private boolean autoInited;
    private static final float AUTO_DT = 1f / 60f;

    public StripeView(Context c) {
        super(c);
        paint.setAntiAlias(false);
        paint.setDither(false);
        paint.setFilterBitmap(false);
        applyPrefs(c);
    }

    void applyPrefs(Context c) {
        boolean auto = Prefs.autoDrift(c);
        if (auto) {
            configure(Prefs.autoWidthA(c), Prefs.autoWidthB(c), 0f, Prefs.DIR_DEFAULT,
                    true, 0f, Prefs.ROT_CW, true, System.nanoTime(), true,
                    Prefs.colorScheme(c), Prefs.distScale(c), Prefs.hueRate(c));
        } else {
            configure(Prefs.widthA(c), Prefs.widthB(c), Prefs.speed(c), Prefs.direction(c),
                    Prefs.rotate(c), Prefs.rotateSpeed(c), Prefs.rotateDir(c),
                    Prefs.wander(c), System.nanoTime(), false,
                    Prefs.colorScheme(c), Prefs.DIST_DEFAULT, Prefs.hueRate(c));
        }
    }

    void configure(int widthA, int widthB, float speedPxPerSec, int direction,
                   boolean rotate, float rotDegPerSec, int rotDir,
                   boolean wander, long wanderSeed, boolean auto, int colorScheme) {
        configure(widthA, widthB, speedPxPerSec, direction, rotate, rotDegPerSec, rotDir,
                wander, wanderSeed, auto, colorScheme, Prefs.DIST_DEFAULT, Prefs.HUE_RATE_DEFAULT);
    }

    void configure(int widthA, int widthB, float speedPxPerSec, int direction,
                   boolean rotate, float rotDegPerSec, int rotDir,
                   boolean wander, long wanderSeed, boolean auto, int colorScheme,
                   float distScale, float hueRate) {
        this.auto = auto;
        this.colorScheme = colorScheme == StripePattern.SCHEME_COLOR
                ? StripePattern.SCHEME_COLOR : StripePattern.SCHEME_BW;
        this.distScale = Prefs.clampF(distScale, Prefs.DIST_MIN, Prefs.DIST_MAX);
        this.hueRate = Prefs.clampF(hueRate, Prefs.HUE_RATE_MIN, Prefs.HUE_RATE_MAX);
        this.widthA = Prefs.clamp(widthA, Prefs.WIDTH_MIN, Prefs.WIDTH_MAX);
        this.widthB = Prefs.clamp(widthB, Prefs.WIDTH_MIN, Prefs.WIDTH_MAX);
        this.speed = speedPxPerSec;
        this.direction = direction >= 0 ? 1 : -1;
        this.rotate = rotate || auto;
        this.rotDegPerSec = Prefs.clampF(rotDegPerSec, Prefs.ROT_MIN, Prefs.ROT_MAX);
        this.rotDir = rotDir < 0 ? Prefs.ROT_CCW : Prefs.ROT_CW;
        this.wander = wander || auto;
        this.wanderSeed = wanderSeed;
        if (tile != null) tile.recycle();
        tile = StripePattern.tile(this.widthA, this.widthB);
        int n = tile.getWidth() * tile.getHeight();
        if (maskPx == null || maskPx.length != n) {
            maskPx = new int[n];
            colorPx = new int[n];
        }
        tile.getPixels(maskPx, 0, tile.getWidth(), 0, 0, tile.getWidth(), tile.getHeight());
        shader = new BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        paint.setShader(shader);
        havePivot = false;
        pivotComp = 0f;
        lastHue = Float.NaN;
        resetAutoMotion();
        applyHueAt(0f);
        applyPose(this.angle, Float.NaN, Float.NaN, this.offset);
    }

    /**
     * Pose the pattern directly (tests and still frames). Skips time integration
     * and pivot compensation.
     */
    void setState(float angleRad, float px, float py, float offsetPx) {
        this.angle = angleRad;
        this.offset = StripePattern.wrap(offsetPx, StripePattern.period(widthA, widthB));
        this.pivot[0] = px;
        this.pivot[1] = py;
        applyMatrix(angleRad, px, py, this.offset);
        invalidate();
    }

    /**
     * Advance the animation to {@code elapsedNanos} after start. Used by the
     * frame callback and by tests that want the integrated motion.
     */
    void setElapsedNanos(long elapsedNanos) {
        float t = elapsedNanos / 1_000_000_000f;
        if (t < 0f) t = 0f;
        int vw = Math.max(1, getWidth()), vh = Math.max(1, getHeight());
        float ang;
        float flowOff;
        if (auto) {
            advanceAuto(t);
            ang = autoAng;
            flowOff = autoFlowOff;
            PivotWander.position(t, vw, vh, wanderSeed, pivot);
        } else {
            ang = StripePattern.BASE_ANGLE_RAD;
            if (rotate) {
                ang += rotDir * (float) Math.toRadians(rotDegPerSec) * t;
            }
            flowOff = (direction >= 0 ? 1f : -1f) * speed * t;
            if (rotate && wander) {
                PivotWander.position(t, vw, vh, wanderSeed, pivot);
            } else {
                pivot[0] = vw / 2f;
                pivot[1] = vh / 2f;
            }
        }
        if (!havePivot) {
            prevPivot[0] = pivot[0];
            prevPivot[1] = pivot[1];
            havePivot = true;
        } else {
            pivotComp += StripePattern.pivotCompDelta(
                    prevPivot[0], prevPivot[1], pivot[0], pivot[1], ang);
            prevPivot[0] = pivot[0];
            prevPivot[1] = pivot[1];
        }
        float off = StripePattern.wrap(flowOff + pivotComp, StripePattern.period(widthA, widthB));
        this.angle = ang;
        this.offset = off;
        applyHueAt(t);
        applyMatrix(ang, pivot[0], pivot[1], off);
        invalidate();
    }

    float offset() { return offset; }
    float angle() { return angle; }
    float pivotX() { return pivot[0]; }
    float pivotY() { return pivot[1]; }
    float hue() { return lastHue; }
    int colorScheme() { return colorScheme; }

    private void applyHueAt(float t) {
        if (colorScheme != StripePattern.SCHEME_COLOR) return;
        applyHue(HueDrift.hueDeg(t, wanderSeed, hueRate));
    }

    /** Recolour the existing tile in place. No allocation after the first configure. */
    private void applyHue(float hueDeg) {
        if (colorScheme != StripePattern.SCHEME_COLOR) return;
        if (maskPx == null || tile == null) return;
        if (!Float.isNaN(lastHue) && hueDeg == lastHue) return;
        lastHue = hueDeg;
        int c0 = StripePattern.colorAtHue(hueDeg, hsv);
        int c1 = StripePattern.colorAtHue(hueDeg + 180f, hsv);
        for (int i = 0; i < maskPx.length; i++) {
            colorPx[i] = (maskPx[i] == StripePattern.WHITE) ? c0 : c1;
        }
        tile.setPixels(colorPx, 0, tile.getWidth(), 0, 0, tile.getWidth(), tile.getHeight());
    }


    private void resetAutoMotion() {
        autoT = 0f;
        autoFlowOff = 0f;
        autoAng = StripePattern.BASE_ANGLE_RAD;
        autoInited = true;
    }

    /** Mid-point Euler from the last time up to {@code t}, in 1/60 s steps. */
    private void advanceAuto(float t) {
        if (!autoInited || t < autoT) resetAutoMotion();
        while (t - autoT > AUTO_DT) stepAuto(AUTO_DT);
        float rem = t - autoT;
        if (rem > 0f) stepAuto(rem);
    }

    private void stepAuto(float dt) {
        float mid = autoT + 0.5f * dt;
        autoFlowOff += SmoothDrift.flowSpeed(mid, wanderSeed) * distScale * dt;
        autoAng += (float) Math.toRadians(SmoothDrift.rotSpeed(mid, wanderSeed) * distScale) * dt;
        autoT += dt;
    }

    private void applyPose(float angleRad, float px, float py, float offsetPx) {
        int vw = getWidth(), vh = getHeight();
        if (Float.isNaN(px)) px = (vw > 0 ? vw : 1) / 2f;
        if (Float.isNaN(py)) py = (vh > 0 ? vh : 1) / 2f;
        this.angle = angleRad;
        this.offset = StripePattern.wrap(offsetPx, StripePattern.period(widthA, widthB));
        this.pivot[0] = px;
        this.pivot[1] = py;
        applyMatrix(this.angle, px, py, this.offset);
        invalidate();
    }

    private void applyMatrix(float angleRad, float px, float py, float offsetPx) {
        float c = (float) Math.cos(angleRad), s = (float) Math.sin(angleRad);
        // sx = c*(x-px) + s*(y-py) + offset
        mat[0] = c;  mat[1] = s;  mat[2] = offsetPx - c * px - s * py;
        mat[3] = -s; mat[4] = c;  mat[5] = s * px - c * py;
        mat[6] = 0f; mat[7] = 0f; mat[8] = 1f;
        matrix.setValues(mat);
        if (shader != null) shader.setLocalMatrix(matrix);
    }

    void setRunning(boolean run) {
        if (run == running) return;
        running = run;
        if (run) {
            startNanos = System.nanoTime();
            havePivot = false;
            pivotComp = 0f;
            resetAutoMotion();
            if (liveFrames()) Choreographer.getInstance().postFrameCallback(this);
        } else {
            Choreographer.getInstance().removeFrameCallback(this);
        }
    }

    /**
     * Robolectric drains posted Choreographer callbacks until the queue is empty,
     * so a self-posting frame loop would never return from Activity setup.
     * Tests drive the pattern with {@link #setState} or {@link #setElapsedNanos}.
     */
    private static boolean liveFrames() {
        return !"robolectric".equals(Build.FINGERPRINT);
    }

    @Override public void doFrame(long frameTimeNanos) {
        if (!running) return;
        setElapsedNanos(frameTimeNanos - startNanos);
        if (running && liveFrames()) Choreographer.getInstance().postFrameCallback(this);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        havePivot = false;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setRunning(true);
    }

    @Override protected void onDetachedFromWindow() {
        setRunning(false);
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas c) {
        c.drawPaint(paint);
    }
}

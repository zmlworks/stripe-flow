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
 * Full-screen 45-degree stripe field. A tiny repeating tile is drawn with a
 * BitmapShader and translated each frame. One shader texel is one device pixel.
 */
public final class StripeView extends View implements Choreographer.FrameCallback {
    private final Paint paint = new Paint(0);
    private final Matrix matrix = new Matrix();
    private Bitmap tile;
    private BitmapShader shader;
    private int stripeWidth = 1;
    private float speed = Prefs.SPEED_DEFAULT;
    private int direction = Prefs.DIR_DEFAULT;
    private long startNanos;
    private boolean running;
    private float offset;

    public StripeView(Context c) {
        super(c);
        paint.setAntiAlias(false);
        paint.setDither(false);
        paint.setFilterBitmap(false);
        apply(Prefs.width(c), Prefs.speed(c), Prefs.direction(c));
    }

    void apply(int width, float speedPxPerSec, int direction) {
        this.stripeWidth = width == 2 ? 2 : 1;
        this.speed = speedPxPerSec;
        this.direction = direction >= 0 ? 1 : -1;
        if (tile != null) tile.recycle();
        tile = StripePattern.tile(this.stripeWidth);
        shader = new BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        paint.setShader(shader);
        setOffset(offset);
    }

    /** Visible for tests: set the pattern shift in physical pixels and redraw. */
    void setOffset(float offsetPx) {
        int p = StripePattern.period(stripeWidth);
        float o = offsetPx % p;
        if (o < 0f) o += p;
        this.offset = o;
        matrix.setTranslate(-o, 0f);
        if (shader != null) shader.setLocalMatrix(matrix);
        invalidate();
    }

    float offset() { return offset; }

    void setRunning(boolean run) {
        if (run == running) return;
        running = run;
        if (run) {
            startNanos = System.nanoTime();
            if (liveFrames()) Choreographer.getInstance().postFrameCallback(this);
        } else {
            Choreographer.getInstance().removeFrameCallback(this);
        }
    }

    /**
     * Robolectric drains posted Choreographer callbacks until the queue is empty,
     * so a self-posting frame loop would never return from Activity setup.
     * Tests drive the pattern with {@link #setOffset(float)}.
     */
    private static boolean liveFrames() {
        return !"robolectric".equals(Build.FINGERPRINT);
    }

    @Override public void doFrame(long frameTimeNanos) {
        if (!running) return;
        setOffset(StripePattern.offset(frameTimeNanos - startNanos, speed, direction, stripeWidth));
        if (running && liveFrames()) Choreographer.getInstance().postFrameCallback(this);
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

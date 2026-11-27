package com.zmlworks.stripeflow;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

/** Launch screen: remember settings, then open the full-screen stripes. */
public final class SettingsActivity extends Activity {
    private TextView speedValue, widthAValue, widthBValue, brightValue, rotSpeedValue, autoHint, distValue, hueRateValue, edgeBlurValue, startHueValue;
    private SeekBar speedBar, widthABar, widthBBar, brightBar, rotSpeedBar, distBar, hueRateBar, edgeBlurBar, startHueBar;
    private CheckBox systemBright, rotate, wander, autoDrift, antiAlias, edgeBlur, startHueSet;
    private LinearLayout rotPanel, manualPanel, autoPanel, colorPanel, edgeBlurPanel, autoColorPanel, specColorPanel, startHuePanel;
    private View colorASwatch, colorBSwatch;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        int pad = Ui.dp(this, 16);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(pad, pad, pad, pad);

        TextView title = text(getString(R.string.app_name), 22, Ui.TEXT);
        col.addView(title);
        TextView sub = text(getString(R.string.subtitle), 14, Ui.DIM);
        sub.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 16));
        col.addView(sub);

        heading(col, R.string.brightness);
        systemBright = new CheckBox(this);
        systemBright.setText(R.string.brightness_system);
        systemBright.setTextColor(Ui.TEXT);
        systemBright.setChecked(Prefs.useSystemBrightness(this));
        col.addView(systemBright);

        brightValue = text("", 14, Ui.DIM);
        col.addView(brightValue);
        brightBar = seek(100, Math.round(Prefs.brightness(this) * 100f));
        col.addView(brightBar, lp());
        systemBright.setOnCheckedChangeListener((v, on) -> {
            Prefs.setUseSystemBrightness(this, on);
            refreshBrightness();
        });
        brightBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) Prefs.setBrightness(SettingsActivity.this, p / 100f);
                refreshBrightness();
            }
        });

        autoDrift = new CheckBox(this);
        autoDrift.setText(R.string.auto_drift);
        autoDrift.setTextColor(Ui.TEXT);
        autoDrift.setChecked(Prefs.autoDrift(this));
        LinearLayout.LayoutParams autoLp = lp();
        autoLp.topMargin = Ui.dp(this, 16);
        col.addView(autoDrift, autoLp);

        autoPanel = new LinearLayout(this);
        autoPanel.setOrientation(LinearLayout.VERTICAL);
        heading(autoPanel, R.string.dist_scale);
        distValue = text("", 14, Ui.DIM);
        autoPanel.addView(distValue);
        distBar = seek(Scales.STEPS, Scales.progressFromDist(Prefs.distScale(this)));
        autoPanel.addView(distBar, lp());
        distBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                float v = Scales.distFromProgress(p);
                if (fromUser) Prefs.setDistScale(SettingsActivity.this, v);
                refreshDist(v);
            }
        });
        autoHint = text("", 13, Ui.DIM);
        autoHint.setPadding(0, Ui.dp(this, 8), 0, 0);
        autoPanel.addView(autoHint);
        col.addView(autoPanel, lp());
        refreshDist(Prefs.distScale(this));

        LinearLayout widthPanel = new LinearLayout(this);
        widthPanel.setOrientation(LinearLayout.VERTICAL);
        heading(widthPanel, R.string.width_a);
        widthAValue = text("", 14, Ui.DIM);
        widthPanel.addView(widthAValue);
        widthABar = seek(Prefs.WIDTH_MAX - Prefs.WIDTH_MIN, 0);
        widthPanel.addView(widthABar, lp());
        widthABar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int v = p + Prefs.WIDTH_MIN;
                if (fromUser) {
                    if (autoDrift.isChecked()) Prefs.setAutoWidthA(SettingsActivity.this, v);
                    else Prefs.setWidthA(SettingsActivity.this, v);
                }
                widthAValue.setText(getString(R.string.width_value, v));
            }
        });
        heading(widthPanel, R.string.width_b);
        widthBValue = text("", 14, Ui.DIM);
        widthPanel.addView(widthBValue);
        widthBBar = seek(Prefs.WIDTH_MAX - Prefs.WIDTH_MIN, 0);
        widthPanel.addView(widthBBar, lp());
        widthBBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int v = p + Prefs.WIDTH_MIN;
                if (fromUser) {
                    if (autoDrift.isChecked()) Prefs.setAutoWidthB(SettingsActivity.this, v);
                    else Prefs.setWidthB(SettingsActivity.this, v);
                }
                widthBValue.setText(getString(R.string.width_value, v));
            }
        });
        col.addView(widthPanel, lp());

        antiAlias = new CheckBox(this);
        antiAlias.setText(R.string.anti_alias);
        antiAlias.setTextColor(Ui.TEXT);
        antiAlias.setChecked(Prefs.antiAlias(this));
        LinearLayout.LayoutParams aaLp = lp();
        aaLp.topMargin = Ui.dp(this, 16);
        col.addView(antiAlias, aaLp);
        antiAlias.setOnCheckedChangeListener((v, on) -> Prefs.setAntiAlias(this, on));

        edgeBlur = new CheckBox(this);
        edgeBlur.setText(R.string.edge_blur);
        edgeBlur.setTextColor(Ui.TEXT);
        edgeBlur.setChecked(Prefs.edgeBlur(this));
        LinearLayout.LayoutParams ebLp = lp();
        ebLp.topMargin = Ui.dp(this, 12);
        col.addView(edgeBlur, ebLp);
        TextView edgeBlurHint = text(getString(R.string.edge_blur_hint), 13, Ui.DIM);
        edgeBlurHint.setPadding(0, Ui.dp(this, 4), 0, 0);
        col.addView(edgeBlurHint);
        edgeBlurPanel = new LinearLayout(this);
        edgeBlurPanel.setOrientation(LinearLayout.VERTICAL);
        heading(edgeBlurPanel, R.string.edge_blur_width);
        edgeBlurValue = text("", 14, Ui.DIM);
        edgeBlurPanel.addView(edgeBlurValue);
        edgeBlurBar = seek(Prefs.EDGE_BLUR_MAX - Prefs.EDGE_BLUR_MIN, 0);
        edgeBlurPanel.addView(edgeBlurBar, lp());
        edgeBlurBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int v = p + Prefs.EDGE_BLUR_MIN;
                if (fromUser) Prefs.setEdgeBlurWidth(SettingsActivity.this, v);
                edgeBlurValue.setText(getString(R.string.edge_blur_value, v));
            }
        });
        int ebw = Prefs.edgeBlurWidth(this);
        edgeBlurBar.setProgress(ebw - Prefs.EDGE_BLUR_MIN);
        edgeBlurValue.setText(getString(R.string.edge_blur_value, ebw));
        col.addView(edgeBlurPanel, lp());
        edgeBlur.setOnCheckedChangeListener((v, on) -> {
            Prefs.setEdgeBlur(this, on);
            refreshEdgeBlur();
        });

        heading(col, R.string.color);
        col.addView(radios(
                new String[]{getString(R.string.color_bw), getString(R.string.color_mode)},
                new int[]{Prefs.COLOR_BW, Prefs.COLOR_MODE},
                Prefs.colorScheme(this),
                v -> {
                    Prefs.setColorScheme(this, v);
                    refreshColor();
                }));
        colorPanel = new LinearLayout(this);
        colorPanel.setOrientation(LinearLayout.VERTICAL);
        heading(colorPanel, R.string.color_how);
        colorPanel.addView(radios(
                new String[]{getString(R.string.color_auto), getString(R.string.color_specified)},
                new int[]{0, 1},
                Prefs.colorSpecified(this) ? 1 : 0,
                v -> {
                    Prefs.setColorSpecified(this, v == 1);
                    refreshColorStyle();
                }));

        autoColorPanel = new LinearLayout(this);
        autoColorPanel.setOrientation(LinearLayout.VERTICAL);
        startHueSet = new CheckBox(this);
        startHueSet.setText(R.string.hue_start_set);
        startHueSet.setTextColor(Ui.TEXT);
        startHueSet.setChecked(Prefs.startHueSet(this));
        autoColorPanel.addView(startHueSet);
        startHuePanel = new LinearLayout(this);
        startHuePanel.setOrientation(LinearLayout.VERTICAL);
        heading(startHuePanel, R.string.hue);
        startHueValue = text("", 14, Ui.DIM);
        startHuePanel.addView(startHueValue);
        startHueBar = seek(360, hueProgress(Prefs.startHue(this)));
        startHuePanel.addView(startHueBar, lp());
        startHueBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) Prefs.setStartHue(SettingsActivity.this, p);
                startHueValue.setText(getString(R.string.hue_value, p == 360 ? 0 : p));
            }
        });
        startHueValue.setText(getString(R.string.hue_value, hueProgress(Prefs.startHue(this))));
        autoColorPanel.addView(startHuePanel, lp());
        startHueSet.setOnCheckedChangeListener((v, on) -> {
            Prefs.setStartHueSet(this, on);
            refreshStartHue();
        });
        heading(autoColorPanel, R.string.hue_rate);
        hueRateValue = text("", 14, Ui.DIM);
        autoColorPanel.addView(hueRateValue);
        hueRateBar = seek(Scales.STEPS, Scales.progressFromHueRate(Prefs.hueRate(this)));
        autoColorPanel.addView(hueRateBar, lp());
        hueRateBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                float v = Scales.hueRateFromProgress(p);
                if (fromUser) Prefs.setHueRate(SettingsActivity.this, v);
                hueRateValue.setText(hueRateLabel(v));
            }
        });
        hueRateValue.setText(hueRateLabel(Prefs.hueRate(this)));
        colorPanel.addView(autoColorPanel, lp());

        specColorPanel = new LinearLayout(this);
        specColorPanel.setOrientation(LinearLayout.VERTICAL);
        colorASwatch = headingWithSwatch(specColorPanel, R.string.color_a, Prefs.colorA(this));
        addHueBar(specColorPanel, Prefs.colorAH(this), v -> Prefs.setColorAH(this, v));
        addSatValBar(specColorPanel, R.string.saturation, Prefs.colorAS(this), v -> Prefs.setColorAS(this, v));
        addSatValBar(specColorPanel, R.string.value_bright, Prefs.colorAV(this), v -> Prefs.setColorAV(this, v));
        colorBSwatch = headingWithSwatch(specColorPanel, R.string.color_b, Prefs.colorB(this));
        addHueBar(specColorPanel, Prefs.colorBH(this), v -> Prefs.setColorBH(this, v));
        addSatValBar(specColorPanel, R.string.saturation, Prefs.colorBS(this), v -> Prefs.setColorBS(this, v));
        addSatValBar(specColorPanel, R.string.value_bright, Prefs.colorBV(this), v -> Prefs.setColorBV(this, v));
        colorPanel.addView(specColorPanel, lp());
        col.addView(colorPanel, lp());

        manualPanel = new LinearLayout(this);
        manualPanel.setOrientation(LinearLayout.VERTICAL);
        col.addView(manualPanel, lp());

        heading(manualPanel, R.string.speed);
        speedValue = text("", 14, Ui.DIM);
        manualPanel.addView(speedValue);
        speedBar = seek(Scales.STEPS, Scales.progressFromSpeed(Prefs.speed(this)));
        manualPanel.addView(speedBar, lp());
        speedBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int v = Scales.speedFromProgress(p);
                if (fromUser) Prefs.setSpeed(SettingsActivity.this, v);
                speedValue.setText(getString(R.string.speed_value, v));
            }
        });
        speedValue.setText(getString(R.string.speed_value, Prefs.speed(this)));

        heading(manualPanel, R.string.direction);
        manualPanel.addView(radios(
                new String[]{getString(R.string.dir_down_right), getString(R.string.dir_up_left)},
                new int[]{Prefs.DIR_DOWN_RIGHT, Prefs.DIR_UP_LEFT},
                Prefs.direction(this),
                v -> Prefs.setDirection(this, v)));
        TextView dirHint = text(getString(R.string.dir_hint), 13, Ui.DIM);
        dirHint.setPadding(0, Ui.dp(this, 4), 0, 0);
        manualPanel.addView(dirHint);

        rotate = new CheckBox(this);
        rotate.setText(R.string.rotate);
        rotate.setTextColor(Ui.TEXT);
        rotate.setChecked(Prefs.rotate(this));
        LinearLayout.LayoutParams rotLp = lp();
        rotLp.topMargin = Ui.dp(this, 16);
        manualPanel.addView(rotate, rotLp);

        rotPanel = new LinearLayout(this);
        rotPanel.setOrientation(LinearLayout.VERTICAL);
        heading(rotPanel, R.string.rotate_speed);
        rotSpeedValue = text("", 14, Ui.DIM);
        rotPanel.addView(rotSpeedValue);
        rotSpeedBar = seek(Scales.STEPS, Scales.progressFromRot(Prefs.rotateSpeed(this)));
        rotPanel.addView(rotSpeedBar, lp());
        rotSpeedBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                float v = Scales.rotFromProgress(p);
                if (fromUser) Prefs.setRotateSpeed(SettingsActivity.this, v);
                rotSpeedValue.setText(rotLabel(v));
            }
        });
        rotSpeedValue.setText(rotLabel(Prefs.rotateSpeed(this)));

        heading(rotPanel, R.string.rotate_dir);
        rotPanel.addView(radios(
                new String[]{getString(R.string.rotate_cw), getString(R.string.rotate_ccw)},
                new int[]{Prefs.ROT_CW, Prefs.ROT_CCW},
                Prefs.rotateDir(this),
                v -> Prefs.setRotateDir(this, v)));

        wander = new CheckBox(this);
        wander.setText(R.string.wander);
        wander.setTextColor(Ui.TEXT);
        wander.setChecked(Prefs.wander(this));
        LinearLayout.LayoutParams wandLp = lp();
        wandLp.topMargin = Ui.dp(this, 12);
        rotPanel.addView(wander, wandLp);
        wander.setOnCheckedChangeListener((v, on) -> Prefs.setWander(this, on));
        TextView wanderHint = text(getString(R.string.wander_hint), 13, Ui.DIM);
        wanderHint.setPadding(0, Ui.dp(this, 4), 0, 0);
        rotPanel.addView(wanderHint);
        manualPanel.addView(rotPanel, lp());

        rotate.setOnCheckedChangeListener((v, on) -> {
            Prefs.setRotate(this, on);
            refreshRotate();
        });
        autoDrift.setOnCheckedChangeListener((v, on) -> {
            Prefs.setAutoDrift(this, on);
            refreshAuto();
        });

        Button start = new Button(this);
        start.setText(R.string.start);
        start.setOnClickListener(v -> startActivity(new Intent(this, StripeActivity.class)));
        LinearLayout.LayoutParams startLp = lp();
        startLp.topMargin = Ui.dp(this, 24);
        col.addView(start, startLp);

        TextView hint = text(getString(R.string.touch_hint), 13, Ui.DIM);
        hint.setPadding(0, Ui.dp(this, 12), 0, 0);
        col.addView(hint);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.BG);
        sv.addView(col);
        setContentView(sv);
        Ui.edgeToEdge(this, sv);
        refreshBrightness();
        refreshRotate();
        refreshAuto();
        refreshColor();
        refreshEdgeBlur();
    }

    private void refreshBrightness() {
        boolean sys = systemBright.isChecked();
        brightBar.setEnabled(!sys);
        brightValue.setText(sys ? "" : getString(R.string.brightness_value, brightBar.getProgress()));
    }

    private void refreshRotate() {
        rotPanel.setVisibility(rotate.isChecked() ? View.VISIBLE : View.GONE);
    }

    private void refreshAuto() {
        boolean on = autoDrift.isChecked();
        autoPanel.setVisibility(on ? View.VISIBLE : View.GONE);
        manualPanel.setVisibility(on ? View.GONE : View.VISIBLE);
        bindWidth();
    }

    private void refreshColor() {
        boolean on = Prefs.colorScheme(this) == Prefs.COLOR_MODE;
        colorPanel.setVisibility(on ? View.VISIBLE : View.GONE);
        if (on) refreshColorStyle();
    }

    private void refreshColorStyle() {
        boolean spec = Prefs.colorSpecified(this);
        autoColorPanel.setVisibility(spec ? View.GONE : View.VISIBLE);
        specColorPanel.setVisibility(spec ? View.VISIBLE : View.GONE);
        if (spec) refreshSpecSwatches();
        else refreshStartHue();
    }

    private void refreshStartHue() {
        boolean on = Prefs.startHueSet(this);
        startHuePanel.setVisibility(on ? View.VISIBLE : View.GONE);
    }

    private void refreshSpecSwatches() {
        colorASwatch.setBackgroundColor(Prefs.colorA(this));
        colorBSwatch.setBackgroundColor(Prefs.colorB(this));
    }

    private void refreshEdgeBlur() {
        edgeBlurPanel.setVisibility(edgeBlur.isChecked() ? View.VISIBLE : View.GONE);
    }

    private void refreshDist(float scale) {
        distValue.setText(getString(R.string.dist_scale_value, formatNum(scale)));
        float flow = SmoothDrift.FLOW_MAX * scale;
        float rot = SmoothDrift.ROT_MAX * scale;
        autoHint.setText(getString(R.string.auto_hint,
                signed(flow), formatNum(flow), signed(rot), formatNum(rot)));
    }

    private static String signed(float v) {
        return "-" + formatNum(v);
    }

    private String hueRateLabel(float degPerSec) {
        if (degPerSec <= 0f) return getString(R.string.hue_rate_fixed);
        float period = 360f / degPerSec;
        if (period >= 120f) {
            return getString(R.string.hue_rate_cycle_min, formatNum(period / 60f));
        }
        return getString(R.string.hue_rate_cycle_sec, formatNum(period));
    }

    private void bindWidth() {
        boolean auto = autoDrift.isChecked();
        int a = auto ? Prefs.autoWidthA(this) : Prefs.widthA(this);
        int b = auto ? Prefs.autoWidthB(this) : Prefs.widthB(this);
        widthABar.setProgress(a - Prefs.WIDTH_MIN);
        widthBBar.setProgress(b - Prefs.WIDTH_MIN);
        widthAValue.setText(getString(R.string.width_value, a));
        widthBValue.setText(getString(R.string.width_value, b));
    }

    private String rotLabel(float degPerSec) {
        if (degPerSec <= 0f) return getString(R.string.rotate_speed_sec, "0");
        if (degPerSec < 1f) {
            return getString(R.string.rotate_speed_min, formatNum(degPerSec * 60f));
        }
        return getString(R.string.rotate_speed_sec, formatNum(degPerSec));
    }

    private static String formatNum(float v) {
        if (Math.abs(v - Math.round(v)) < 0.05f) return String.format(Locale.US, "%d", Math.round(v));
        return String.format(Locale.US, "%.1f", v);
    }

    private TextView text(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private static int hueProgress(float hue) {
        int p = Math.round(StripePattern.wrapHue(hue));
        return p == 360 ? 0 : p;
    }

    private interface FloatChoice { void pick(float v); }

    private void addHueBar(LinearLayout col, float current, FloatChoice onPick) {
        heading(col, R.string.hue);
        TextView value = text(getString(R.string.hue_value, hueProgress(current)), 14, Ui.DIM);
        col.addView(value);
        SeekBar bar = seek(360, hueProgress(current));
        col.addView(bar, lp());
        bar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int deg = p == 360 ? 0 : p;
                if (fromUser) onPick.pick(deg);
                value.setText(getString(R.string.hue_value, deg));
                refreshSpecSwatches();
            }
        });
    }

    private void addSatValBar(LinearLayout col, int label, float current, FloatChoice onPick) {
        heading(col, label);
        int pct = Math.round(Prefs.clampF(current, 0f, 1f) * 100f);
        TextView value = text(getString(R.string.percent_value, pct), 14, Ui.DIM);
        col.addView(value);
        SeekBar bar = seek(100, pct);
        col.addView(bar, lp());
        bar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) onPick.pick(p / 100f);
                value.setText(getString(R.string.percent_value, p));
                refreshSpecSwatches();
            }
        });
    }

    private View headingWithSwatch(LinearLayout col, int res, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 4));
        TextView t = text(getString(res), 14, Ui.HI);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(t, tp);
        View sw = new View(this);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 24));
        sw.setBackgroundColor(color);
        row.addView(sw, sp);
        col.addView(row, lp());
        return sw;
    }

    private void heading(LinearLayout col, int res) {
        TextView t = text(getString(res), 14, Ui.HI);
        t.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 4));
        col.addView(t);
    }

    private SeekBar seek(int max, int progress) {
        SeekBar s = new SeekBar(this);
        s.setMax(max);
        s.setProgress(progress);
        return s;
    }

    private RadioGroup radios(String[] labels, int[] values, int current, IntChoice onPick) {
        RadioGroup g = new RadioGroup(this);
        g.setOrientation(RadioGroup.HORIZONTAL);
        int[] ids = new int[labels.length];
        for (int i = 0; i < labels.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(labels[i]);
            rb.setTextColor(Ui.TEXT);
            ids[i] = View.generateViewId();
            rb.setId(ids[i]);
            g.addView(rb);
            if (values[i] == current) rb.setChecked(true);
        }
        g.setOnCheckedChangeListener((gr, id) -> {
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] == id) { onPick.pick(values[i]); break; }
            }
        });
        return g;
    }

    private static LinearLayout.LayoutParams lp() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private interface IntChoice { void pick(int v); }

    private abstract static class SimpleSeek implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar s) {}
        @Override public void onStopTrackingTouch(SeekBar s) {}
    }
}

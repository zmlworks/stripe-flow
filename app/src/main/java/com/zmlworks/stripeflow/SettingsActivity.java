package com.zmlworks.stripeflow;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
    private TextView speedValue, widthAValue, widthBValue, brightValue, rotSpeedValue, autoHint, distValue, hueRateValue;
    private SeekBar speedBar, widthABar, widthBBar, brightBar, rotSpeedBar, distBar, hueRateBar;
    private CheckBox systemBright, rotate, wander, autoDrift;
    private LinearLayout rotPanel, manualPanel, autoPanel, colorPanel;

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
        heading(colorPanel, R.string.hue_rate);
        hueRateValue = text("", 14, Ui.DIM);
        colorPanel.addView(hueRateValue);
        hueRateBar = seek(Scales.STEPS, Scales.progressFromHueRate(Prefs.hueRate(this)));
        colorPanel.addView(hueRateBar, lp());
        hueRateBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                float v = Scales.hueRateFromProgress(p);
                if (fromUser) Prefs.setHueRate(SettingsActivity.this, v);
                hueRateValue.setText(hueRateLabel(v));
            }
        });
        hueRateValue.setText(hueRateLabel(Prefs.hueRate(this)));
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

package com.zmlworks.stripeflow;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/** Launch screen: remember settings, then open the full-screen stripes. */
public final class SettingsActivity extends Activity {
    private TextView speedValue, widthValue, brightValue;
    private SeekBar speedBar, widthBar, brightBar;
    private CheckBox systemBright;

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

        heading(col, R.string.speed);
        speedValue = text("", 14, Ui.DIM);
        col.addView(speedValue);
        speedBar = seek(Prefs.SPEED_MAX - Prefs.SPEED_MIN, Prefs.speed(this) - Prefs.SPEED_MIN);
        col.addView(speedBar, lp());
        speedBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int v = p + Prefs.SPEED_MIN;
                if (fromUser) Prefs.setSpeed(SettingsActivity.this, v);
                speedValue.setText(getString(R.string.speed_value, v));
            }
        });
        speedValue.setText(getString(R.string.speed_value, Prefs.speed(this)));

        heading(col, R.string.stripe_width);
        widthValue = text("", 14, Ui.DIM);
        col.addView(widthValue);
        widthBar = seek(Prefs.WIDTH_MAX - Prefs.WIDTH_MIN, Prefs.width(this) - Prefs.WIDTH_MIN);
        col.addView(widthBar, lp());
        widthBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int v = p + Prefs.WIDTH_MIN;
                if (fromUser) Prefs.setWidth(SettingsActivity.this, v);
                widthValue.setText(getString(R.string.width_value, v));
            }
        });
        widthValue.setText(getString(R.string.width_value, Prefs.width(this)));

        heading(col, R.string.direction);
        col.addView(radios(
                new String[]{getString(R.string.dir_down_right), getString(R.string.dir_up_left)},
                new int[]{Prefs.DIR_DOWN_RIGHT, Prefs.DIR_UP_LEFT},
                Prefs.direction(this),
                v -> Prefs.setDirection(this, v)));

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
    }

    private void refreshBrightness() {
        boolean sys = systemBright.isChecked();
        brightBar.setEnabled(!sys);
        brightValue.setText(sys ? "" : getString(R.string.brightness_value, brightBar.getProgress()));
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
        for (int i = 0; i < labels.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(labels[i]);
            rb.setTextColor(Ui.TEXT);
            rb.setId(i + 1);
            g.addView(rb);
            if (values[i] == current) rb.setChecked(true);
        }
        g.setOnCheckedChangeListener((gr, id) -> {
            int i = id - 1;
            if (i >= 0 && i < values.length) onPick.pick(values[i]);
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

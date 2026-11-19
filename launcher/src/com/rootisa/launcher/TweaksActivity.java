package com.rootisa.launcher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * TWEAKS — customization of the RooTISA shell (accent, columns, labels, icon
 * shape, hidden apps, default-home). No root/system tweaks: an unprivileged app
 * can only restyle its own home surface.
 */
public class TweaksActivity extends Activity {

    private float density;
    private Prefs prefs;
    private LinearLayout swatches;

    private int dp(float v) { return Math.round(v * density); }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        density = getResources().getDisplayMetrics().density;
        prefs = new Prefs(this);
        setContentView(build());
    }

    private View build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundDrawable(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF0A1022, 0xFF0E1630, 0xFF140B24}));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(14), dp(16), dp(14), dp(20));

        TextView title = new TextView(this);
        title.setText("TWEAKS");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        col.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Оформление оболочки RooTISA");
        sub.setTextColor(0xCCB9CBE8);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        sub.setPadding(0, dp(2), 0, dp(10));
        col.addView(sub);

        // --- Appearance ---
        col.addView(label("Вид"));
        LinearLayout look = panel();

        look.addView(rowLabel("Акцентный цвет"));
        swatches = new LinearLayout(this);
        swatches.setOrientation(LinearLayout.HORIZONTAL);
        swatches.setPadding(0, dp(4), 0, dp(10));
        buildSwatches();
        look.addView(swatches);

        final Button colsBtn = ghostButton("Колонок: " + prefs.columns());
        colsBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                int c = prefs.columns() >= 4 ? 3 : prefs.columns() + 1; // 3 -> 4 -> 3
                prefs.setColumns(c);
                colsBtn.setText("Колонок: " + c);
            }
        });
        look.addView(colsBtn);

        final Button labelsBtn = ghostButton("Подписи: " + onOff(prefs.labels()));
        labelsBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                prefs.setLabels(!prefs.labels());
                labelsBtn.setText("Подписи: " + onOff(prefs.labels()));
            }
        });
        look.addView(labelsBtn);

        final Button shapeBtn = ghostButton("Иконки: " + (prefs.squircle() ? "squircle" : "оригинал"));
        shapeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                prefs.setSquircle(!prefs.squircle());
                shapeBtn.setText("Иконки: " + (prefs.squircle() ? "squircle" : "оригинал"));
            }
        });
        look.addView(shapeBtn);
        col.addView(look);

        // --- Apps ---
        col.addView(label("Приложения"));
        LinearLayout appsP = panel();
        Button unhide = ghostButton("Показать скрытые приложения");
        unhide.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                prefs.clearHidden();
                Toast.makeText(TweaksActivity.this, "Скрытые возвращены", Toast.LENGTH_SHORT).show();
            }
        });
        appsP.addView(unhide);
        TextView hint = new TextView(this);
        hint.setText("Долгий тап по иконке на главном — переименовать, сменить иконку или скрыть.");
        hint.setTextColor(0xB0C7D4EC);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        hint.setPadding(dp(4), dp(8), dp(4), dp(2));
        appsP.addView(hint);
        col.addView(appsP);

        // --- Shell ---
        col.addView(label("Оболочка"));
        LinearLayout shell = panel();
        Button home = ghostButton("Назначить лаунчером по умолчанию");
        home.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openHome(); }
        });
        shell.addView(home);
        TextView shint = new TextView(this);
        shint.setText("Телефон будет запускать RooTISA вместо стокового экрана.");
        shint.setTextColor(0xB0C7D4EC);
        shint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        shint.setPadding(dp(4), dp(8), dp(4), dp(2));
        shell.addView(shint);
        col.addView(shell);

        TextView ver = new TextView(this);
        ver.setText("RooTISA " + versionName());
        ver.setTextColor(0x80ACC0E0);
        ver.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        ver.setPadding(dp(2), dp(14), 0, 0);
        col.addView(ver);

        scroll.addView(col);
        return scroll;
    }

    private void buildSwatches() {
        swatches.removeAllViews();
        int current = prefs.accent();
        for (int i = 0; i < Prefs.ACCENTS.length; i++) {
            final int color = Prefs.ACCENTS[i];
            View sw = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(color);
            if (color == current) g.setStroke(dp(2), 0xFFFFFFFF);
            sw.setBackgroundDrawable(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(28), dp(28));
            lp.rightMargin = dp(8);
            sw.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    prefs.setAccent(color);
                    buildSwatches();
                }
            });
            swatches.addView(sw, lp);
        }
    }

    private String onOff(boolean b) { return b ? "вкл" : "выкл"; }

    // ---- UI helpers --------------------------------------------------------

    private TextView label(String t) {
        TextView x = new TextView(this);
        x.setText(t.toUpperCase());
        x.setTextColor(0x805BA8FF);
        x.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        x.setPadding(dp(2), dp(12), 0, dp(4));
        return x;
    }

    private TextView rowLabel(String t) {
        TextView x = new TextView(this);
        x.setText(t);
        x.setTextColor(0xFFE6ECF7);
        x.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        return x;
    }

    private LinearLayout panel() {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x22FFFFFF);
        g.setCornerRadius(dp(14));
        g.setStroke(dp(1), 0x33FFFFFF);
        p.setBackgroundDrawable(g);
        p.setPadding(dp(12), dp(10), dp(12), dp(10));
        return p;
    }

    private Button ghostButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x33FFFFFF);
        g.setCornerRadius(dp(10));
        g.setStroke(dp(1), 0x405BA8FF);
        b.setBackgroundDrawable(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        b.setLayoutParams(lp);
        return b;
    }

    private String versionName() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (Exception e) { return ""; }
    }

    private void openHome() {
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (Exception ignored) {
                Toast.makeText(this, "Настройки → Приложения по умолчанию", Toast.LENGTH_LONG).show();
            }
        }
    }
}

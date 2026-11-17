package com.rootisa.launcher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * RooTISA settings: self-update ("Обновить систему") that pulls a newer signed
 * APK from GitHub and launches the installer, plus a shortcut to make RooTISA
 * the default home app ("вместо андроида").
 */
public class SettingsActivity extends Activity {

    // Update channel. Point this at a stable branch or a Releases asset when ready.
    private static final String UPDATE_BRANCH = "claude/vibrant-sagan-ev4hhh";
    private static final String UPDATE_BASE =
            "https://raw.githubusercontent.com/kl1ms1/RooTISA/" + UPDATE_BRANCH + "/dist/";
    private static final String VERSION_URL = UPDATE_BASE + "version.txt";
    private static final String APK_URL = UPDATE_BASE + "RooTISA.apk";
    private static final String APK_NAME = "update.apk";

    private float density;
    private TextView status;
    private Button updateBtn;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private int dp(float v) { return Math.round(v * density); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;

        ScrollView scroll = new ScrollView(this);
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF0A1022, 0xFF0E1630, 0xFF140B24});
        scroll.setBackgroundDrawable(bg);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(14), dp(16), dp(14), dp(16));

        TextView title = new TextView(this);
        title.setText("Настройки");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        col.addView(title);

        TextView ver = new TextView(this);
        ver.setText("Версия: " + versionName() + " (" + versionCode() + ")");
        ver.setTextColor(0xCCB9CBE8);
        ver.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        ver.setPadding(0, dp(2), 0, dp(14));
        col.addView(ver);

        // --- System update panel ---
        col.addView(panelLabel("Система"));
        LinearLayout updPanel = glassColumn();
        updateBtn = glassButton("Обновить систему");
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startUpdate(); }
        });
        updPanel.addView(updateBtn);

        status = new TextView(this);
        status.setText("Берёт новый образ с GitHub и ставит поверх текущего.");
        status.setTextColor(0xB0C7D4EC);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        status.setPadding(dp(4), dp(8), dp(4), dp(2));
        updPanel.addView(status);
        col.addView(updPanel);

        // --- Launcher panel ("instead of Android") ---
        col.addView(panelLabel("Оболочка"));
        LinearLayout homePanel = glassColumn();
        Button homeBtn = glassButton("Назначить лаунчером по умолчанию");
        homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openHomeSettings(); }
        });
        homePanel.addView(homeBtn);

        TextView homeHint = new TextView(this);
        homeHint.setText("После этого телефон всегда запускает RooTISA вместо стокового экрана.");
        homeHint.setTextColor(0xB0C7D4EC);
        homeHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        homeHint.setPadding(dp(4), dp(8), dp(4), dp(2));
        homePanel.addView(homeHint);
        col.addView(homePanel);

        scroll.addView(col);
        setContentView(scroll);
    }

    // ---- UI helpers --------------------------------------------------------

    private TextView panelLabel(String text) {
        TextView t = new TextView(this);
        t.setText(text.toUpperCase());
        t.setTextColor(0x805BA8FF);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        t.setPadding(dp(2), dp(10), 0, dp(4));
        return t;
    }

    private LinearLayout glassColumn() {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x22FFFFFF);
        g.setCornerRadius(dp(14));
        g.setStroke(dp(1), 0x33FFFFFF);
        p.setBackgroundDrawable(g);
        p.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setLayoutParams(lp);
        return p;
    }

    private Button glassButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x33FFFFFF);
        g.setCornerRadius(dp(10));
        g.setStroke(dp(1), 0x405BA8FF);
        b.setBackgroundDrawable(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        b.setLayoutParams(lp);
        return b;
    }

    private void setStatus(final String text) {
        ui.post(new Runnable() { @Override public void run() { status.setText(text); } });
    }

    private void enableBtn(final boolean on) {
        ui.post(new Runnable() { @Override public void run() { updateBtn.setEnabled(on); } });
    }

    // ---- Version info ------------------------------------------------------

    private int versionCode() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (Exception e) { return 0; }
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) { return "?"; }
    }

    // ---- Update flow -------------------------------------------------------

    private void startUpdate() {
        enableBtn(false);
        setStatus("Проверяю обновление…");
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    int remote = fetchRemoteVersion();
                    int local = versionCode();
                    if (remote <= local) {
                        setStatus("У вас последняя версия (" + local + ").");
                        enableBtn(true);
                        return;
                    }
                    setStatus("Загрузка версии " + remote + "…");
                    File apk = download(APK_URL);
                    setStatus("Загружено. Запуск установки…");
                    launchInstall(apk);
                    enableBtn(true);
                } catch (Exception e) {
                    setStatus("Ошибка обновления: " + e.getMessage());
                    enableBtn(true);
                }
            }
        }).start();
    }

    private int fetchRemoteVersion() throws Exception {
        HttpURLConnection c = open(VERSION_URL);
        InputStream in = c.getInputStream();
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = in.read()) != -1 && sb.length() < 16) sb.append((char) ch);
        in.close();
        c.disconnect();
        return Integer.parseInt(sb.toString().trim());
    }

    private File download(String url) throws Exception {
        HttpURLConnection c = open(url);
        File out = new File(getCacheDir(), APK_NAME);
        InputStream in = c.getInputStream();
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[8192];
        int n;
        long total = 0;
        int len = c.getContentLength();
        while ((n = in.read(buf)) != -1) {
            fos.write(buf, 0, n);
            total += n;
            if (len > 0) setStatus("Загрузка… " + (total * 100 / len) + "%");
        }
        fos.flush();
        fos.close();
        in.close();
        c.disconnect();
        return out;
    }

    private HttpURLConnection open(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setInstanceFollowRedirects(true);
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setRequestProperty("User-Agent", "RooTISA-Updater");
        c.connect();
        if (c.getResponseCode() / 100 != 2) {
            throw new Exception("HTTP " + c.getResponseCode());
        }
        return c;
    }

    private void launchInstall(File apk) {
        // On Android 8+ the app needs "install unknown apps" permission; send the
        // user to grant it first if it isn't set, then they re-tap.
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            setStatus("Разреши установку из этого источника и нажми «Обновить» снова.");
            try {
                Intent perm = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName()));
                perm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(perm);
            } catch (Exception ignored) {}
            return;
        }
        Uri uri = ApkProvider.uriFor(APK_NAME);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, "application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (Exception e) {
            ui.post(new Runnable() { @Override public void run() {
                Toast.makeText(SettingsActivity.this, "Нет установщика пакетов",
                        Toast.LENGTH_SHORT).show();
            }});
        }
    }

    private void openHomeSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            // Fallback: generic settings.
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (Exception ignored) {
                Toast.makeText(this, "Открой Настройки → Приложения по умолчанию",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}

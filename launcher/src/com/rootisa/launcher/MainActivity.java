package com.rootisa.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RooTISA home — iOS-like side-paging launcher, squircle icons, per-app tweaks.
 * Pure framework, no AndroidX, no background services (stays cool and light).
 */
public class MainActivity extends Activity {

    private static final int REQ_ICON = 1001;

    private float density;
    private Prefs prefs;
    private TextView clockView;
    private LinearLayout dots;
    private PagedHome paged;

    private final List<AppItem> apps = new ArrayList<AppItem>();
    private final Map<String, Drawable> iconCache = new HashMap<String, Drawable>();
    private String pendingIconPkg; // app awaiting a chosen image

    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private final Handler clock = new Handler() {
        @Override public void handleMessage(Message m) {
            if (clockView != null) clockView.setText(timeFmt.format(new Date()));
            sendEmptyMessageDelayed(0, 15000);
        }
    };

    private int dp(float v) { return Math.round(v * density); }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        density = getResources().getDisplayMetrics().density;
        prefs = new Prefs(this);
        rebuild();
    }

    @Override
    protected void onResume() {
        super.onResume();
        clock.removeMessages(0);
        clock.sendEmptyMessage(0);
        rebuild(); // reflect installs / hides / tweaks cheaply
    }

    @Override
    protected void onPause() {
        super.onPause();
        clock.removeMessages(0);
    }

    @Override
    public void onBackPressed() {
        if (paged != null) paged.smoothScrollTo(0, 0);
    }

    // ---- Build the whole home surface --------------------------------------

    private void rebuild() {
        loadApps();
        setContentView(buildUi());
    }

    private View buildUi() {
        int accent = prefs.accent();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundDrawable(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x660A1022, 0x33140B24})); // scrim so wallpaper shows through

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);

        // Top bar: clock + gear.
        FrameLayout bar = new FrameLayout(this);
        bar.setPadding(dp(14), dp(8), dp(10), dp(6));

        clockView = new TextView(this);
        clockView.setTextColor(Color.WHITE);
        clockView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        clockView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        clockView.setText(timeFmt.format(new Date()));
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
        bar.addView(clockView, clp);

        TextView gear = new TextView(this);
        gear.setText("⚙");
        gear.setTextColor(0xDDFFFFFF);
        gear.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        gear.setPadding(dp(10), dp(4), dp(8), dp(4));
        gear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TweaksActivity.class));
            }
        });
        FrameLayout.LayoutParams glp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        glp.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
        bar.addView(gear, glp);
        col.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Paged app grid.
        paged = new PagedHome(this);
        LinearLayout pagesRow = new LinearLayout(this);
        pagesRow.setOrientation(LinearLayout.HORIZONTAL);
        paged.addView(pagesRow, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        col.addView(paged, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Page dots.
        dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER);
        dots.setPadding(0, dp(6), 0, dp(10));
        col.addView(dots, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(col);

        // Fill pages once we know the real width.
        final LinearLayout pagesRowF = pagesRow;
        final int accentF = accent;
        paged.post(new Runnable() {
            @Override public void run() {
                int w = paged.getWidth();
                int h = paged.getHeight();
                if (w <= 0) w = getResources().getDisplayMetrics().widthPixels;
                if (h <= 0) h = getResources().getDisplayMetrics().heightPixels;
                fillPages(pagesRowF, w, h, accentF);
            }
        });
        return root;
    }

    private void fillPages(LinearLayout pagesRow, int pageW, int pageH, int accent) {
        pagesRow.removeAllViews();
        int cols = prefs.columns();
        int cell = pageW / cols;
        int iconPx = (int) (cell * 0.58f);
        int rows = Math.max(3, (pageH - dp(8)) / (iconPx + dp(prefs.labels() ? 26 : 10)));
        int perPage = cols * rows;
        if (perPage <= 0) perPage = cols * 4;

        int total = apps.size();
        int pages = Math.max(1, (total + perPage - 1) / perPage);
        buildDots(pages, accent);

        for (int pg = 0; pg < pages; pg++) {
            LinearLayout page = new LinearLayout(this);
            page.setOrientation(LinearLayout.VERTICAL);
            page.setLayoutParams(new LinearLayout.LayoutParams(pageW, LinearLayout.LayoutParams.MATCH_PARENT));
            page.setPadding(dp(6), dp(4), dp(6), dp(4));

            for (int r = 0; r < rows; r++) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
                for (int cc = 0; cc < cols; cc++) {
                    int idx = pg * perPage + r * cols + cc;
                    View tile = (idx < total) ? tile(apps.get(idx), iconPx) : new View(this);
                    LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                            ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                    row.addView(tile, tlp);
                }
                page.addView(row, rlp);
            }
            pagesRow.addView(page);
        }

        paged.setOnPageChange(new PagedHome.OnPageChange() {
            @Override public void onPage(int page, int count) { highlightDot(page); }
        });
    }

    private View tile(final AppItem app, int iconPx) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setPadding(dp(2), dp(6), dp(2), dp(4));

        ImageView iv = new ImageView(this);
        iv.setImageDrawable(iconFor(app, iconPx));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(iconPx, iconPx);
        cell.addView(iv, ip);

        if (prefs.labels()) {
            TextView tv = new TextView(this);
            tv.setText(app.displayName());
            tv.setTextColor(0xFFFFFFFF);
            tv.setShadowLayer(3f, 0, 1, 0xAA000000);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
            tv.setGravity(Gravity.CENTER);
            tv.setMaxLines(1);
            tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tp.topMargin = dp(4);
            cell.addView(tv, tp);
        }

        cell.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { launch(app); }
        });
        cell.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) { customize(app); return true; }
        });
        return cell;
    }

    // ---- Icons -------------------------------------------------------------

    private Drawable iconFor(AppItem app, int size) {
        boolean sq = prefs.squircle();
        String custom = prefs.iconPath(app.packageName);
        String key = app.packageName + "|" + size + "|" + sq + "|" + (custom == null ? "" : custom);
        Drawable d = iconCache.get(key);
        if (d != null) return d;

        if (custom != null) {
            d = IconTools.fromFile(getResources(), custom, size, sq);
        }
        if (d == null) {
            Drawable base = app.icon;
            if (base == null) {
                try {
                    base = getPackageManager().getActivityIcon(
                            new ComponentName(app.packageName, app.activityName));
                } catch (Exception e) {
                    try { base = getPackageManager().getApplicationIcon(app.packageName); }
                    catch (Exception ignored) {}
                }
                app.icon = base;
            }
            d = sq ? IconTools.squircle(getResources(), base, size) : base;
        }
        if (d != null) iconCache.put(key, d);
        return d;
    }

    // ---- Page dots ---------------------------------------------------------

    private void buildDots(int count, int accent) {
        dots.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(i == 0 ? accent : 0x55FFFFFF);
            dot.setBackgroundDrawable(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(6), dp(6));
            lp.leftMargin = dp(3); lp.rightMargin = dp(3);
            dots.addView(dot, lp);
        }
        dots.setTag(accent);
    }

    private void highlightDot(int active) {
        int accent = dots.getTag() == null ? prefs.accent() : (Integer) dots.getTag();
        for (int i = 0; i < dots.getChildCount(); i++) {
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(i == active ? accent : 0x55FFFFFF);
            dots.getChildAt(i).setBackgroundDrawable(g);
        }
    }

    // ---- App list ----------------------------------------------------------

    private void loadApps() {
        apps.clear();
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> found = pm.queryIntentActivities(main, 0);
        String self = getPackageName();
        for (int i = 0; i < found.size(); i++) {
            ResolveInfo ri = found.get(i);
            String pkg = ri.activityInfo.packageName;
            if (self.equals(pkg)) continue;
            if (prefs.isHidden(pkg)) continue;
            String label = ri.loadLabel(pm).toString();
            AppItem it = new AppItem(label, pkg, ri.activityInfo.name);
            it.custom = prefs.name(pkg, null);
            apps.add(it);
        }
        Collections.sort(apps, new Comparator<AppItem>() {
            @Override public int compare(AppItem a, AppItem b) {
                return a.displayName().compareToIgnoreCase(b.displayName());
            }
        });
    }

    private void launch(AppItem app) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setComponent(new ComponentName(app.packageName, app.activityName));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        } catch (Exception e) {
            Intent fb = getPackageManager().getLaunchIntentForPackage(app.packageName);
            if (fb != null) { fb.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(fb); }
            else Toast.makeText(this, "Не удалось открыть", Toast.LENGTH_SHORT).show();
        }
    }

    // ---- Per-app customize -------------------------------------------------

    private void customize(final AppItem app) {
        final String[] items = new String[]{"Переименовать", "Сменить иконку",
                "Сбросить иконку", "Скрыть", "О приложении"};
        new AlertDialog.Builder(this)
                .setTitle(app.displayName())
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        if (which == 0) rename(app);
                        else if (which == 1) pickIcon(app);
                        else if (which == 2) { prefs.setIconPath(app.packageName, null);
                            iconCache.clear(); rebuild(); }
                        else if (which == 3) { prefs.setHidden(app.packageName, true); rebuild(); }
                        else openAppInfo(app);
                    }
                })
                .show();
    }

    private void rename(final AppItem app) {
        final EditText in = new EditText(this);
        in.setText(app.displayName());
        new AlertDialog.Builder(this)
                .setTitle("Название")
                .setView(in)
                .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String v = in.getText().toString().trim();
                        prefs.setName(app.packageName, v.length() == 0 ? app.label : v);
                        rebuild();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void pickIcon(AppItem app) {
        pendingIconPkg = app.packageName;
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(Intent.createChooser(i, "Выбери картинку"), REQ_ICON);
        } catch (Exception e) {
            Toast.makeText(this, "Нет галереи", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_ICON && res == RESULT_OK && data != null && pendingIconPkg != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String path = saveIcon(uri, pendingIconPkg);
                if (path != null) {
                    prefs.setIconPath(pendingIconPkg, path);
                    iconCache.clear();
                    Toast.makeText(this, "Иконка обновлена", Toast.LENGTH_SHORT).show();
                }
            }
            pendingIconPkg = null;
        }
    }

    private String saveIcon(Uri uri, String pkg) {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(in);
            if (bmp == null) return null;
            int s = 144;
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, s, s, true);
            File dir = new File(getFilesDir(), "icons");
            dir.mkdirs();
            File f = new File(dir, pkg.replaceAll("[^a-zA-Z0-9]", "_") + "_" + System.currentTimeMillis() + ".png");
            out = new FileOutputStream(f);
            scaled.compress(Bitmap.CompressFormat.PNG, 100, out);
            return f.getAbsolutePath();
        } catch (Throwable t) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
        }
    }

    private void openAppInfo(AppItem app) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + app.packageName));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) {}
    }
}

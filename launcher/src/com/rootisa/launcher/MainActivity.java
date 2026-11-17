package com.rootisa.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RooTISA — a tiny, low-RAM home launcher tuned for the XS11 (MTK6580, 240x432).
 * Pure framework APIs, no AndroidX. UI is built in code to keep the APK minimal.
 */
public class MainActivity extends Activity {

    private float density;
    private TextView clockView;
    private TextView dateView;
    private GridView grid;
    private AppAdapter adapter;
    private final List<AppItem> apps = new ArrayList<AppItem>();

    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("EEE, d MMM", Locale.getDefault());

    // Lightweight minute-ticking clock without extra threads.
    private final Handler clockHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            updateClock();
            sendEmptyMessageDelayed(0, 15000); // 15s is plenty; cheap on battery
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        setContentView(buildUi());
        loadApps();
        adapter = new AppAdapter();
        grid.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateClock();
        clockHandler.removeMessages(0);
        clockHandler.sendEmptyMessage(0);
        // Refresh the app list cheaply so installs/uninstalls show up.
        reloadAppsIfChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        clockHandler.removeMessages(0);
    }

    // Home launchers should swallow BACK so it never "exits" to a black screen.
    @Override
    public void onBackPressed() {
        if (grid != null) grid.smoothScrollToPosition(0);
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    // ---- UI ----------------------------------------------------------------

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundDrawable(backgroundGradient());

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(8), dp(10), dp(8), dp(4));

        // Header "glass" panel: clock + date + brand.
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundDrawable(glassPanel(dp(14)));
        header.setPadding(dp(12), dp(8), dp(12), dp(8));

        clockView = new TextView(this);
        clockView.setTextColor(Color.WHITE);
        clockView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        clockView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

        dateView = new TextView(this);
        dateView.setTextColor(0xCCB9CBE8);
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

        TextView brand = new TextView(this);
        brand.setText("RooTISA");
        brand.setTextColor(0x805BA8FF);
        brand.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);

        header.addView(clockView);
        header.addView(dateView);
        header.addView(brand);

        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.bottomMargin = dp(8);
        col.addView(header, hp);

        // App grid.
        grid = new GridView(this);
        grid.setNumColumns(3);
        grid.setVerticalSpacing(dp(6));
        grid.setHorizontalSpacing(dp(6));
        grid.setPadding(dp(2), dp(4), dp(2), dp(8));
        grid.setClipToPadding(false);
        grid.setSelector(new GradientDrawable()); // transparent selector, no ugly orange flash
        grid.setVerticalScrollBarEnabled(false);
        grid.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launch(apps.get(position));
            }
        });
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                openAppInfo(apps.get(position));
                return true;
            }
        });

        col.addView(grid);
        root.addView(col);

        // Settings (gear) button, pinned top-right.
        TextView gear = new TextView(this);
        gear.setText("⚙");
        gear.setTextColor(0xDDDCE6FA);
        gear.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        gear.setPadding(dp(12), dp(10), dp(14), dp(10));
        gear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gp.gravity = Gravity.TOP | Gravity.RIGHT;
        root.addView(gear, gp);

        return root;
    }

    private GradientDrawable backgroundGradient() {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF0A1022, 0xFF0E1630, 0xFF140B24});
        return g;
    }

    private GradientDrawable glassPanel(int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x22FFFFFF);            // faux-frosted fill
        g.setCornerRadius(radius);
        g.setStroke(dp(1), 0x33FFFFFF);    // subtle highlight edge
        return g;
    }

    private void updateClock() {
        Date now = new Date();
        if (clockView != null) clockView.setText(timeFmt.format(now));
        if (dateView != null) dateView.setText(dateFmt.format(now));
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
            if (self.equals(pkg)) continue; // hide ourselves
            String label = ri.loadLabel(pm).toString();
            apps.add(new AppItem(label, pkg, ri.activityInfo.name));
        }
        Collections.sort(apps, new Comparator<AppItem>() {
            @Override public int compare(AppItem a, AppItem b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
    }

    private int lastCount = -1;

    private void reloadAppsIfChanged() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        int count = pm.queryIntentActivities(main, 0).size();
        if (count != lastCount) {
            lastCount = count;
            loadApps();
            if (adapter != null) adapter.notifyDataSetChanged();
        }
    }

    private void launch(AppItem app) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setComponent(new ComponentName(app.packageName, app.activityName));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        } catch (Exception e) {
            // Fallback to the package's default launch intent.
            Intent fb = getPackageManager().getLaunchIntentForPackage(app.packageName);
            if (fb != null) {
                fb.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(fb);
            } else {
                Toast.makeText(this, "Не удалось открыть", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openAppInfo(AppItem app) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + app.packageName));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, app.label, Toast.LENGTH_SHORT).show();
        }
    }

    // ---- Adapter -----------------------------------------------------------

    private class AppAdapter extends BaseAdapter {
        @Override public int getCount() { return apps.size(); }
        @Override public Object getItem(int position) { return apps.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout cell;
            ImageView iv;
            TextView tv;
            if (convertView == null) {
                cell = new LinearLayout(MainActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                cell.setPadding(dp(2), dp(6), dp(2), dp(6));

                FrameLayout tile = new FrameLayout(MainActivity.this);
                tile.setBackgroundDrawable(glassPanel(dp(12)));
                int tileSize = dp(52);
                FrameLayout.LayoutParams tilp = new FrameLayout.LayoutParams(tileSize, tileSize);

                iv = new ImageView(MainActivity.this);
                int icon = dp(34);
                FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(icon, icon);
                ip.gravity = Gravity.CENTER;
                tile.addView(iv, ip);

                tv = new TextView(MainActivity.this);
                tv.setTextColor(0xFFE6ECF7);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
                tv.setGravity(Gravity.CENTER);
                tv.setMaxLines(1);
                tv.setEllipsize(android.text.TextUtils.TruncateAt.END);

                LinearLayout.LayoutParams tvp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tvp.topMargin = dp(4);

                cell.addView(tile, new LinearLayout.LayoutParams(tileSize, tileSize));
                cell.addView(tv, tvp);
                cell.setTag(new View[]{iv, tv});
            } else {
                cell = (LinearLayout) convertView;
                View[] holder = (View[]) cell.getTag();
                iv = (ImageView) holder[0];
                tv = (TextView) holder[1];
            }

            AppItem app = apps.get(position);
            if (app.icon == null) {
                try {
                    app.icon = getPackageManager().getActivityIcon(
                            new ComponentName(app.packageName, app.activityName));
                } catch (Exception e) {
                    try { app.icon = getPackageManager().getApplicationIcon(app.packageName); }
                    catch (Exception ignored) {}
                }
            }
            iv.setImageDrawable(app.icon);
            tv.setText(app.label);
            return cell;
        }
    }
}

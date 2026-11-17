package com.rootisa.launcher;

import android.graphics.drawable.Drawable;

/** One launchable app. Icon is cached here so we never re-query the PackageManager per scroll. */
public class AppItem {
    public final String label;
    public final String packageName;
    public final String activityName;
    public Drawable icon; // loaded lazily, then cached

    public AppItem(String label, String packageName, String activityName) {
        this.label = label;
        this.packageName = packageName;
        this.activityName = activityName;
    }
}

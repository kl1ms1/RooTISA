package com.rootisa.launcher;

import android.graphics.drawable.Drawable;

/** One launchable app, plus optional user overrides (custom name/icon). */
public class AppItem {
    public final String label;        // original label
    public final String packageName;
    public final String activityName;
    public String custom;             // user-set name, or null
    public Drawable icon;             // cached base icon

    public AppItem(String label, String packageName, String activityName) {
        this.label = label;
        this.packageName = packageName;
        this.activityName = activityName;
    }

    public String displayName() {
        return (custom != null && custom.length() > 0) ? custom : label;
    }
}

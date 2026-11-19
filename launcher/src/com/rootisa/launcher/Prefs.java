package com.rootisa.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** Thin typed wrapper over SharedPreferences for launcher + per-app tweaks. */
public class Prefs {

    public static final int[] ACCENTS = new int[]{
            0xFF5BA8FF, 0xFF34C759, 0xFFFF375F, 0xFFFF9F0A, 0xFFBF5AF2, 0xFF64D2FF
    };

    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getSharedPreferences("rootisa", Context.MODE_PRIVATE);
    }

    // --- Global look ---
    public int accent()            { return sp.getInt("accent", ACCENTS[0]); }
    public void setAccent(int v)   { sp.edit().putInt("accent", v).apply(); }

    public int columns()           { return sp.getInt("columns", 3); }
    public void setColumns(int v)  { sp.edit().putInt("columns", v).apply(); }

    public boolean labels()        { return sp.getBoolean("labels", true); }
    public void setLabels(boolean v){ sp.edit().putBoolean("labels", v).apply(); }

    public boolean squircle()      { return sp.getBoolean("squircle", true); }
    public void setSquircle(boolean v){ sp.edit().putBoolean("squircle", v).apply(); }

    // --- Per-app overrides (keyed by package name) ---
    public String name(String pkg, String def) {
        return sp.getString("name/" + pkg, def);
    }
    public void setName(String pkg, String v) {
        sp.edit().putString("name/" + pkg, v).apply();
    }

    public String iconPath(String pkg) {
        return sp.getString("icon/" + pkg, null);
    }
    public void setIconPath(String pkg, String path) {
        sp.edit().putString("icon/" + pkg, path).apply();
    }

    public boolean isHidden(String pkg) {
        return hidden().contains(pkg);
    }
    public void setHidden(String pkg, boolean hide) {
        Set<String> s = new HashSet<String>(hidden());
        if (hide) s.add(pkg); else s.remove(pkg);
        sp.edit().putStringSet("hidden", s).apply();
    }
    public Set<String> hidden() {
        return sp.getStringSet("hidden", new HashSet<String>());
    }
    public void clearHidden() {
        sp.edit().putStringSet("hidden", new HashSet<String>()).apply();
    }
}

package com.fasdeq.airtok;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.util.HashMap;
import java.util.Map;


















public final class Launcher {

    private Launcher() {}

    public static final String KEY = "icon";

    
    public static final String DEFAULT = "";

    
    static final String CLASSIC = "classic";

    public static String[] all() {
        String[] out = new String[Shots.KEYS.length + 1];
        out[0] = DEFAULT;
        int at = 1;
        
        for (String key : Shots.KEYS) if (CLASSIC.equals(key)) out[at++] = key;
        for (String key : Shots.KEYS) if (!CLASSIC.equals(key)) out[at++] = key;
        return out;
    }

    
    static final String[] LEGACY = {"materialyou", "material3"};

    
    private static final String[] REDRAWN = {"airtokrialyou", "airtokrial3"};

    private static final String KEY_LEGACY = "icons_legacy";

    static boolean legacy(String which) {
        for (String old : LEGACY) if (old.equals(which)) return true;
        return false;
    }

    
    static String[] shown() {
        java.util.List<String> out = new java.util.ArrayList<String>();
        for (String which : all()) {
            boolean redrawn = false;
            for (String fresh : REDRAWN) if (fresh.equals(which)) redrawn = true;
            if (redrawn) continue;
            int old = -1;
            for (int i = 0; i < LEGACY.length; i++) if (LEGACY[i].equals(which)) old = i;
            if (old < 0) out.add(which);
            else if (indexOf(REDRAWN[old]) >= 0) out.add(REDRAWN[old]);
        }
        return out.toArray(new String[0]);
    }

    




    static void settle(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(AirTok.PREFS, Context.MODE_PRIVATE);
            if (prefs.contains(KEY_LEGACY)) return;
            android.content.pm.PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            boolean updated = info.lastUpdateTime - info.firstInstallTime > 60000L;
            prefs.edit().putBoolean(KEY_LEGACY, updated || legacy(prefs.getString(KEY, DEFAULT))).apply();
        } catch (Throwable error) {
            Diary.note("icon legacy: " + error);
        }
    }

    static boolean legacyOffered() {
        SharedPreferences prefs = prefs();
        return legacy(chosen()) || (prefs != null && prefs.getBoolean(KEY_LEGACY, false));
    }

    public static String nameOf(String which) {
        int at = indexOf(which);
        return at < 0 ? Text.ICON_DEFAULT : Local.of(Shots.LABELS[at]);
    }

    public static String chosen() {
        SharedPreferences prefs = prefs();
        String which = prefs == null ? DEFAULT : prefs.getString(KEY, DEFAULT);
        return indexOf(which) < 0 ? DEFAULT : which;
    }

    










    public static void route(android.content.Intent intent) {
        try {
            ComponentName wanted = intent == null ? null : intent.getComponent();
            if (wanted == null || !Shots.DEFAULT.equals(wanted.getClassName())) return;
            int at = indexOf(chosen());
            if (at < 0 || at >= Shots.COMPONENTS.length) return;
            intent.setComponent(new ComponentName(wanted.getPackageName(), Shots.COMPONENTS[at]));
        } catch (Throwable ignored) {
            
        }
    }

    






    public static void choose(Context context, String which) {
        if (context == null) return;
        if (indexOf(which) < 0) which = DEFAULT;
        try {
            PackageManager packages = context.getPackageManager();
            String self = context.getPackageName();

            for (int i = 0; i < Shots.COMPONENTS.length; i++) {
                boolean on = Shots.KEYS[i].equals(which);
                set(packages, self, Shots.COMPONENTS[i], on);
            }
            set(packages, self, Shots.DEFAULT, DEFAULT.equals(which));

            SharedPreferences prefs = prefs();
            if (prefs != null) prefs.edit().putString(KEY, which).apply();
            Diary.note("icon: " + (DEFAULT.equals(which) ? "back to TikTok's" : which));
        } catch (Throwable error) {
            Diary.note("icon: " + error);
        }
    }

    private static void set(PackageManager packages, String self,
                            String component, boolean on) {
        packages.setComponentEnabledSetting(
                new ComponentName(self, component),
                on ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                   : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    
    public static Bitmap preview(String which) {
        int at = indexOf(which);
        String[] png = at < 0 ? Shots.DEFAULT_PNG : Shots.PNG[at];
        if (which == null) return null;
        synchronized (drawn) {
            Bitmap known = drawn.get(which);
            if (known != null) return known;
            try {
                StringBuilder whole = new StringBuilder();
                for (String piece : png) whole.append(piece);
                byte[] raw = Base64.decode(whole.toString(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                if (bitmap != null) drawn.put(which, bitmap);
                return bitmap;
            } catch (Throwable error) {
                Diary.note("icon: " + error);
                return null;
            }
        }
    }

    private static final Map<String, Bitmap> drawn = new HashMap<String, Bitmap>();

    private static int indexOf(String which) {
        if (which == null) return -1;
        for (int i = 0; i < Shots.KEYS.length; i++) {
            if (Shots.KEYS[i].equals(which)) return i;
        }
        return -1;
    }

    private static SharedPreferences prefs() {
        Context context = AirTok.context();
        if (context == null) return null;
        return context.getSharedPreferences(AirTok.PREFS, Context.MODE_PRIVATE);
    }
}

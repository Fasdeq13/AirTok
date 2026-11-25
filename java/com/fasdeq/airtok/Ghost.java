package com.fasdeq.airtok;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public final class Ghost {

    public static final String KEY = "ghost";
    public static final String ONLINE = "ghost_online", READ = "ghost_read", TYPING = "ghost_typing",
            STORIES = "ghost_stories", PROFILES = "ghost_profiles";

    private Ghost() {}

    private static SharedPreferences prefs() {
        Context context = AirTok.context();
        return context == null ? null : context.getSharedPreferences(AirTok.PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled() {
        SharedPreferences prefs = prefs();
        return prefs != null && prefs.getBoolean(KEY, false);
    }

    public static void setEnabled(boolean value) {
        SharedPreferences prefs = prefs();
        if (prefs != null) prefs.edit().putBoolean(KEY, value).apply();
        Flags.forget();
    }

    public static boolean isTypingHidden() {
        return isEnabled();
    }

    public static boolean on(String key) {
        return isEnabled();
    }

    public static void set(String key, boolean value) {
        setEnabled(value);
    }

    public static void recordActivity() {}

    public static boolean isTemporarilyActive() {
        return false;
    }

    static void start() {
        if (isEnabled()) Flags.forget();
    }

    static void resumed(Activity activity) {
        if (isEnabled()) Flags.forget();
    }

    static void paused(Activity activity) {}

    static void apply() {
        Flags.forget();
    }

    static String report() {
        return isEnabled() ? "включён" : "выключен";
    }
}

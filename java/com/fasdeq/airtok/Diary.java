package com.fasdeq.airtok;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public final class Diary {

    private Diary() {}

    private static final String KEY = "diary";

    private static final String FILE = "diary_airtok";
    private static final int KEEP = 40;
    private static final int KEEP_DEV = 400;

    private static final List<String> LINES = new ArrayList<String>();
    private static boolean loaded;


    private static boolean dirty;
    private static long savedAt;
    private static final long REST = 3000;


    private static final SimpleDateFormat CLOCK =
            new SimpleDateFormat("HH:mm:ss", Locale.US);

    public static void note(String line) {
        DevLink.heard(line);
        if (flooding(line)) return;
        if (Version.TEST || Version.LITE) android.util.Log.i("MargyT", "diary: " + line);
        String stamped = stamp() + "  " + line;
        synchronized (LINES) {
            load();
            String last = LINES.isEmpty() ? "" : LINES.get(LINES.size() - 1);
            if (last.length() > 10 && last.substring(10).equals(stamped.substring(10))) return;
            LINES.add(stamped);
            int keep = DevMode.isOn() ? KEEP_DEV : KEEP;
            while (LINES.size() > keep) LINES.remove(0);
            dirty = true;
            long now = android.os.SystemClock.uptimeMillis();
            if (now - savedAt >= REST) {
                savedAt = now;
                save();
            }
        }
    }

    private static final java.util.HashMap<String, long[]> SAID = new java.util.HashMap<String, long[]>();


    private static boolean flooding(String line) {
        if (line == null) return true;
        String kind = line.length() > 80 ? line.substring(0, 80) : line;
        kind = kind.replaceAll("[0-9]+", "#");
        long now = android.os.SystemClock.uptimeMillis();
        synchronized (SAID) {
            long[] count = SAID.get(kind);
            if (count == null) {
                if (SAID.size() > 300) SAID.clear();
                SAID.put(kind, new long[] {now, 1});
                return false;
            }
            if (now - count[0] > 60000L) {
                count[0] = now;
                count[1] = 1;
                return false;
            }
            return ++count[1] > 6;
        }
    }


    public static void detail(String line) {
        if (DevMode.isOn()) note("· " + line);
    }

    public static List<String> lines() {
        synchronized (LINES) {
            load();
            if (dirty) save();
            return new ArrayList<String>(LINES);
        }
    }

    public static void clear() {
        synchronized (LINES) {
            LINES.clear();
            loaded = true;
            save();
        }
    }



    private static void load() {
        if (loaded) return;
        SharedPreferences prefs = prefs();
        if (prefs == null) return;
        loaded = true;
        String stored = prefs.getString(KEY, "");
        if (stored.length() == 0) {


            try {
                SharedPreferences settings = AirTok.context()
                        .getSharedPreferences(AirTok.PREFS, Context.MODE_PRIVATE);
                if (settings.contains(KEY)) {
                    stored = settings.getString(KEY, "");
                    settings.edit().remove(KEY).apply();
                }
            } catch (Throwable ignored) { }
        }
        if (stored.length() > 0) LINES.addAll(Arrays.asList(stored.split("\n")));
    }

    private static void save() {
        dirty = false;
        SharedPreferences prefs = prefs();
        if (prefs == null) return;
        StringBuilder out = new StringBuilder();
        for (String line : LINES) {
            if (out.length() > 0) out.append('\n');
            out.append(line);
        }
        prefs.edit().putString(KEY, out.toString()).apply();
    }

    private static SharedPreferences prefs() {
        try {
            Context context = AirTok.context();
            if (context == null) return null;
            return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String stamp() {
        synchronized (CLOCK) {
            return CLOCK.format(new Date());
        }
    }
}

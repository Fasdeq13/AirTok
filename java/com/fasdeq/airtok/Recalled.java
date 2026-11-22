package com.fasdeq.airtok;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;


public final class Recalled {

    private Recalled() {}

    static final String KEY = "keep_recalled";
    private static final String STORE = "airtok_recalled";
    private static final int MOST = 4000;
    private static final float FADED = 0.68f;
    private static final int FADED_ROW = 0x7e0c0381, WATCHED_ROW = 0x7e0c0383;

    private static volatile Boolean on;
    private static Map<String, Long> kept;
    private static final Map<View, String> rows = new WeakHashMap<>();
    private static Method recall;

    static boolean enabled() {

        return true;
    }

    static void setEnabled(boolean value) {
        on = value;
        Context context = AirTok.context();
        if (context != null) context.getSharedPreferences(AirTok.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply();
    }

    private static SharedPreferences store() {
        Context context = AirTok.context();
        return context == null ? null : context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
    }

    private static synchronized Map<String, Long> kept() {
        if (kept != null) return kept;
        Map<String, Long> out = new HashMap<>();
        SharedPreferences store = store();
        if (store != null) for (Map.Entry<String, ?> one : store.getAll().entrySet())
            if (one.getValue() instanceof Long) out.put(one.getKey(), (Long) one.getValue());
        kept = out;
        return out;
    }


    public static void arrived(Object domain, Object message, String recaller, Object after) throws Throwable {
        try {
            if (enabled() && holdBack(message, recaller)) return;
        } catch (Throwable error) {
            Diary.note("recalled: " + error);
        }
        original(domain, message, recaller, after);
    }

    private static boolean holdBack(Object message, String recaller) {

        String own = Account.id();
        if (own == null || own.length() == 0 || own.equals(recaller)) return false;
        if (own.equals(String.valueOf(NativeRead.number(NativeRead.get(message, "getSender"))))) return false;
        String uuid = NativeRead.string(NativeRead.get(message, "getUuid"));

        if (uuid.length() == 0 || NativeRead.string(NativeRead.get(message, "getContent")).length() == 0) return false;
        remember(uuid);
        return true;
    }

    private static void remember(String uuid) {
        long now = System.currentTimeMillis();
        Map<String, Long> known = kept();
        synchronized (Recalled.class) {
            known.put(uuid, now);
            SharedPreferences store = store();
            if (store != null) {
                SharedPreferences.Editor edit = store.edit().putLong(uuid, now);
                if (known.size() > MOST) {
                    List<Map.Entry<String, Long>> old = new ArrayList<>(known.entrySet());
                    java.util.Collections.sort(old, (a, b) -> Long.compare(a.getValue(), b.getValue()));
                    for (int i = 0; i < old.size() - MOST; i++) { known.remove(old.get(i).getKey()); edit.remove(old.get(i).getKey()); }
                }
                edit.apply();
            }
        }
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            List<View> showing = new ArrayList<>();
            synchronized (rows) {
                for (Map.Entry<View, String> row : rows.entrySet()) if (uuid.equals(row.getValue()) && row.getKey() != null) showing.add(row.getKey());
            }
            for (View row : showing) fade(row);
        });
    }


    private static void fade(final View row) {
        row.setTag(FADED_ROW, Boolean.TRUE);
        row.setAlpha(FADED);
        if (row.getTag(WATCHED_ROW) != null) return;
        row.setTag(WATCHED_ROW, Boolean.TRUE);
        row.getViewTreeObserver().addOnPreDrawListener(() -> {
            if (row.getTag(FADED_ROW) != null && row.getAlpha() != FADED) row.setAlpha(FADED);
            return true;
        });
    }

    private static void original(Object domain, Object message, String recaller, Object after) throws Throwable {
        Method known = recall;
        if (known == null) {
            for (Method one : domain.getClass().getMethods()) {
                if (one.getName().equals("LLJJIJI") && one.getParameterTypes().length == 3) { known = one; break; }
            }
            if (known == null) throw new NoSuchMethodException("recall on " + domain.getClass());
            known.setAccessible(true);
            recall = known;
        }
        try {
            known.invoke(domain, message, recaller, after);
        } catch (java.lang.reflect.InvocationTargetException error) {
            throw error.getCause() == null ? error : error.getCause();
        }
    }


    static void bound(View row, Object message) {
        try {
            String uuid = NativeRead.string(NativeRead.get(message, "getUuid"));
            synchronized (rows) { rows.put(row, uuid); }
            if (kept().containsKey(uuid)) {
                fade(row);
            } else if (row.getTag(FADED_ROW) != null) {

                row.setTag(FADED_ROW, null);
                row.setAlpha(1f);
            }
        } catch (Throwable error) {
            Diary.note("recalled row: " + error);
        }
    }

    static int count() { return kept().size(); }
}

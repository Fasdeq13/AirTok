package com.fasdeq.airtok;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

final class FlagGuard {

    private FlagGuard() {}

    private static final String FILE = "flagguard_margyt";
    private static final String STARTS = "starts";
    private static final int TOO_MANY = 3;
    private static final long LASTED = 8000L;

    private static volatile SharedPreferences counting;
    private static volatile int countedFrom;
    private static volatile boolean counted;

    private static volatile int answer;

    static boolean allows() {
        int known = answer;
        if (known != 0) return known == 2;
        Context context = AirTok.context();
        if (context == null) return false;
        try {
            final SharedPreferences prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
            int starts = prefs.getInt(STARTS, 0);
            if (starts >= TOO_MANY) {
                answer = 1;
                Diary.note("flags: held back, TikTok did not stay up with them");
                return false;
            }
            if (context.getPackageName().equals(processName())) {
                counting = prefs;
                countedFrom = starts;

                final Thread.UncaughtExceptionHandler before = Thread.getDefaultUncaughtExceptionHandler();
                Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
                    count();
                    if (before != null) before.uncaughtException(thread, error);
                });
            }
            answer = 2;
            return true;
        } catch (Throwable error) {
            return false;
        }
    }

    static void shown() {
        final SharedPreferences prefs = counting;
        if (prefs == null || counted) return;
        count();
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> prefs.edit().putInt(STARTS, 0).apply(), LASTED);
    }

    private static synchronized void count() {
        SharedPreferences prefs = counting;
        if (prefs == null || counted) return;
        counted = true;

        try { prefs.edit().putInt(STARTS, countedFrom + 1).commit(); } catch (Throwable ignored) { }
    }

    static boolean heldBack() {
        return answer == 1;
    }

    static void again() {
        answer = 2;
        Context context = AirTok.context();
        if (context == null) return;
        try {
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putInt(STARTS, 0).apply();
        } catch (Throwable ignored) { }
    }

    private static String processName() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 28) return android.app.Application.getProcessName();
            return (String) Class.forName("android.app.ActivityThread")
                    .getMethod("currentProcessName").invoke(null);
        } catch (Throwable error) {
            return null;
        }
    }
}

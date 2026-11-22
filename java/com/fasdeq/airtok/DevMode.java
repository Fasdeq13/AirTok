package com.fasdeq.airtok;

import android.content.Context;

final class DevMode {
    private DevMode() {}

    static final String KEY = "developer";
    private static final int NEEDED = 5;
    private static final long GAP = 1000L;

    private static volatile int state;
    private static int taps;
    private static long lastTap;

    static boolean isOn() {
        int known = state;
        if (known != 0) return known == 2;
        Context context = AirTok.context();
        if (context == null) return false;
        boolean on = false;
        try {
            on = context.getSharedPreferences("margyt", Context.MODE_PRIVATE)
                    .getBoolean(KEY, false);
        } catch (Throwable ignored) { }
        state = on ? 2 : 1;
        return on;
    }

    static void set(boolean on) {
        state = on ? 2 : 1;
        Flags.forget();
        Context context = AirTok.context();
        if (context == null) return;
        try {
            context.getSharedPreferences("margyt", Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY, on).apply();
        } catch (Throwable ignored) { }
    }

    static synchronized boolean tapped() {
        long now = android.os.SystemClock.uptimeMillis();
        taps = now - lastTap > GAP ? 1 : taps + 1;
        lastTap = now;
        if (taps < NEEDED) return false;
        taps = 0;
        return true;
    }
}

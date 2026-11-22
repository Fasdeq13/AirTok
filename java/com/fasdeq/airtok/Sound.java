package com.fasdeq.airtok;

import android.content.SharedPreferences;

import com.ss.android.ugc.aweme.music.model.Music;


public final class Sound {

    private Sound() {}

    static void reloadSettings(){cached=null;}
    public static final String KEY = "sound_available";


    private static int spoke;
    private static boolean told;

    private static void counted() {
        if (told || ++spoke < 50) return;
        told = true;
        Diary.note("sound: told the app " + spoke + " muted tracks are fine");
    }


    private static final int AVAILABLE = 1;
    private static final int NOT_MUTED = 0;

    private static volatile Boolean cached;

    public static boolean isEnabled() {

        return true;
    }

    public static void setEnabled(boolean enabled) {
        cached = enabled;
        SharedPreferences prefs = prefs();
        if (prefs != null) prefs.edit().putBoolean(KEY, enabled).apply();
    }

    private static SharedPreferences prefs() {
        android.content.Context context = AirTok.context();
        if (context == null) return null;
        return context.getSharedPreferences(AirTok.PREFS, android.content.Context.MODE_PRIVATE);
    }



    public static boolean available(Music music) {
        if (music == null) return false;
        return isEnabled() || music.available();
    }

    public static int getMusicStatus(Music music) {
        if (music == null) return 0;
        if (!isEnabled()) return music.getMusicStatus();
        if (music.getMusicStatus() != AVAILABLE) counted();
        return AVAILABLE;
    }

    public static boolean isMuteShare(Music music) {
        if (music == null) return false;
        return !isEnabled() && music.isMuteShare();
    }

    public static int getMuteType(Music music) {
        if (music == null) return NOT_MUTED;
        return isEnabled() ? NOT_MUTED : music.getMuteType();
    }
}

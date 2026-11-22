package com.fasdeq.airtok;

import android.app.Activity;
import android.view.View;
import android.widget.EditText;

import com.fasdeq.airtok.textanim.Main;


final class TextAnim {

    private TextAnim() {}

    private static Main main;

    static boolean on() {
        return false;
    }

    private static Main main() {
        return null;
    }

    static void resumed(Activity activity) {
    }

    static void destroyed(Activity activity) {
        if (main == null) return;
        try { main.onActivityDestroyed(activity); } catch (Throwable ignored) {}
    }


    static void set(boolean on) {
        TypingAnim.set(TypingAnim.ON, on);
        Main.enabled = on;
        if (main == null) return;

        try { if (on) main.onStart(); else main.onStop(); } catch (Throwable ignored) {}
    }

    static boolean openSettings(Activity activity) {
        try {
            return main().onOpenSettings(activity);
        } catch (Throwable error) {
            Diary.note("text animation settings: " + error);
            return false;
        }
    }
}

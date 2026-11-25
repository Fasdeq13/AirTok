package com.fasdeq.airtok;

import android.app.Activity;
import android.view.View;
import android.widget.EditText;

import com.fasdeq.airtok.textanim.Main;








final class TextAnim {

    private TextAnim() {}

    private static Main main;

    static boolean on() {
        return !Version.LITE && TypingAnim.on(TypingAnim.ON);
    }

    private static Main main() {
        if (main == null) {
            main = new Main();
            main.onStart();
        }
        return main;
    }

    static void resumed(Activity activity) {
        if (!on()) return;
        try {
            Main plugin = main();
            Main.enabled = true;
            plugin.onActivityResumed(activity);
            View focused = activity.getCurrentFocus();
            if (focused instanceof EditText) plugin.onView(focused);
        } catch (Throwable error) {
            Diary.note("text animation: " + error);
        }
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

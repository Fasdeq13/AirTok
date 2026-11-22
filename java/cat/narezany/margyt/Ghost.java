package cat.narezany.margyt;

import android.app.Activity;

public final class Ghost {

    static final String KEY = "ghost";
    static final String ONLINE = "ghost_online", READ = "ghost_read", TYPING = "ghost_typing",
            STORIES = "ghost_stories", PROFILES = "ghost_profiles";
    static final String LONG_COMMENTS = "comments_long";

    private Ghost() {}

    static boolean on(String key) {
        return false;
    }

    static void set(String key, boolean value) {
    }

    static void start() {
        try {
            LongComments.apply(true);
        } catch (Throwable ignored) {}
    }

    static void resumed(Activity activity) {
    }

    static void paused(Activity activity) {
    }

    static void apply() {
        try {
            LongComments.apply(true);
        } catch (Throwable ignored) {}
    }

    static String report() {
        return "отключен";
    }
}

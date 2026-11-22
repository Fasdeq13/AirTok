package com.fasdeq.airtok;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

public final class ExpressiveDesign {

    private ExpressiveDesign() {}

    public static final String KEY_CHATS = "expressive_chats";
    public static final String KEY_TT_SETTINGS = "expressive_tt_settings";

    public static boolean isChatsEnabled() {
        return false;
    }

    public static void setChatsEnabled(boolean on) {}

    public static boolean isTtSettingsEnabled() {
        return false;
    }

    public static void setTtSettingsEnabled(boolean on) {}

    public static void watch(final Activity activity) {}

    public static void applyTtSettings(final Activity activity) {}

    public static void applyChatBubble(ViewGroup root, Object message, boolean incoming) {}

    public static void styleInboxRow(View row, int accent, boolean dark, float density) {}

    public static void applyChat(final Activity activity) {}
}

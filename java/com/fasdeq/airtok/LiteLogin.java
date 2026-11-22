package com.fasdeq.airtok;

import android.app.Activity;
import android.content.Intent;

public final class LiteLogin {

    private LiteLogin() {}

    private static final String PROVIDER = "provider_id";

    static void created(Activity screen) {
        try {
            if (screen == null) return;
            Intent opened = screen.getIntent();
            if (opened == null) return;
            String provider = opened.getStringExtra(PROVIDER);
            if ("google".equals(provider) || "google_onetap".equals(provider)) {
                opened.putExtra(PROVIDER, "google_web");
            }
        } catch (Throwable broken) {
            Diary.note("web sign-in: " + broken);
        }
    }
}

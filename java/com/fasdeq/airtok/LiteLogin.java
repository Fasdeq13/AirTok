package com.fasdeq.airtok;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;






public final class LiteLogin {

    private LiteLogin() {}

    
    private static final String SCREEN = "com.ss.android.ugc.aweme.mini_lobby.internal.LobbyInvisibleActivity";
    private static final String PROVIDER = "provider_id";

    





    static void created(Activity screen) {
        try {
            if (!Version.LITE || screen == null || !SCREEN.equals(screen.getClass().getName())) return;
            Intent opened = screen.getIntent();
            if (opened == null) return;
            String provider = opened.getStringExtra(PROVIDER);
            android.util.Log.i("AirTok", "sign-in: asked for " + provider);
            
            if (!"google".equals(provider) && !"google_web".equals(provider)) return;
            if (opened.getIntExtra("action_type", 1) != 1) return;
            opened.putExtra(PROVIDER, "airtok_none");
            Toast.makeText(screen.getApplicationContext(), Text.LITE_GOOGLE, Toast.LENGTH_LONG).show();
        } catch (Throwable broken) {
            Diary.note("lite sign-in: " + broken);
        }
    }
}

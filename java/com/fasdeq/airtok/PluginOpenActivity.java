package com.fasdeq.airtok;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public final class PluginOpenActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            Uri file = getIntent() == null ? null : getIntent().getData();
            if (file != null) {
                Intent open = new Intent(this, SettingsActivity.class);

                open.setData(file);
                open.putExtra(SettingsActivity.OPEN_PLUGIN, true);
                open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(open);
            }
        } catch (Throwable error) {
            Diary.note("plugin file: " + error);
        }
        finish();
    }
}

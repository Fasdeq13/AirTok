package cat.narezany.margyt;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;

public final class MargyProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context == null) return true;
        Margy.attach(context);
        try { CrashReports.start(context); } catch(Throwable ignored) {}
        if (!Version.LITE) SocialLibrary.start();

        if (!Version.LITE) Ghost.start();
        Diary.note("start-up hook ran");
        try {
            Context application = context.getApplicationContext();
            if (application instanceof Application) {
                ((Application) application).registerActivityLifecycleCallbacks(new SettingsRow());
                Watch.start((Application) application);
                Lag.watch();
                Diary.note("watching for the settings screen");
            } else {
                Diary.note("no application yet: " + application);
            }
        } catch (Throwable error) {

            Diary.note("hook failed: " + error);
        }
        try {
            Launcher.settle(context);
        } catch (Throwable error) {
            Diary.note("icons: " + error);
        }
        try {
            Badges.start(context);
        } catch (Throwable error) {
            Diary.note("badges failed to start: " + error);
        }
        try {
            if (!Version.LITE) Streaks.start(context);
        } catch (Throwable error) {
            Diary.note("streaks failed to start: " + error);
        }
        try {
            DevLink.start(context);
        } catch (Throwable error) {
            Diary.note("devlink failed to start: " + error);
        }
        try {
            Plugins.startAll(context);
        } catch (Throwable error) {
            Diary.note("plugins failed to start: " + error);
        }
        return true;
    }

    public static Uri share(Context context, File file) {
        try {
            if (!file.isFile()) return null;
            return Uri.parse("content://" + context.getPackageName() + ".margyt/"
                    + file.getName());
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        try {
            Context context = getContext();
            if (context == null) return null;
            String name = uri.getLastPathSegment();

            if (name == null || name.contains("/") || name.contains("..")) return null;
            File file = new File(new File(context.getFilesDir(), "margyt"), name);
            if (!file.isFile()) return null;
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (Throwable error) {
            Diary.note("share: " + error);
            return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        return 0;
    }
}

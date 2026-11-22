package com.fasdeq.airtok;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.File;
import java.util.Locale;


public final class Updater {

    private Updater() {}

    private static final String SOURCE = "";

    public static final String KEY_REMIND = "update_remind";


    private static final String KEY_GOT = "update_got";

    private static final long EVERY = 5 * 60 * 1000L;

    private static volatile String latest;
    private static volatile String where;
    private static volatile String notes;

    private static volatile String title;


    private static volatile boolean refused;
    private static volatile boolean started;



    public static synchronized void start(final Context context) {
        return;
    }


    public static void check(final Context context, final boolean byHand) {
        if (byHand) Screen.say("Обновления отключены");
    }


    public static boolean newer() {
        return false;
    }

    public static String latest() {
        return latest;
    }


    static int compare(String a, String b) {
        if (a == null) return -1;
        if (b == null) return 1;
        String[] left = a.split("\\."), right = b.split("\\.");
        int most = Math.max(left.length, right.length);
        for (int i = 0; i < most; i++) {
            int one = number(left, i), two = number(right, i);
            if (one != two) return one < two ? -1 : 1;
        }
        return 0;
    }

    private static int number(String[] parts, int at) {
        if (at >= parts.length) return 0;
        try {
            return Integer.parseInt(parts[at].trim());
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static String localised(JSONObject root, String field) {
        String language = Locale.getDefault().getLanguage();
        String translated = root.optString(field + "_" + language, "");
        return translated.length() > 0 ? translated : root.optString(field, "");
    }



    public static boolean remind(Context context) {
        try {
            return prefs(context).getBoolean(KEY_REMIND, true);
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static void setRemind(Context context, boolean on) {
        try {
            prefs(context).edit().putBoolean(KEY_REMIND, on).apply();
        } catch (Throwable ignored) {
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(AirTok.PREFS, Context.MODE_PRIVATE);
    }

    public static void offer(final Context context) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                android.app.Activity activity = Screen.now();
                if (activity == null || activity.isFinishing()) return;



                final boolean store = storeInstalled(activity);
                String named = title == null || title.length() == 0 ? "" : " «" + title + "»";
                String message = Text.UPDATE_THERE_IS + " " + latest + named + ". " + Text.UPDATE_IN_STORE
                        + (store ? "" : " " + Text.UPDATE_GET_STORE)
                        + (notes == null || notes.length() == 0 ? "" : "\n\n" + notes);
                Popup.askUpdate(activity, Text.UPDATE, message,
                        store ? Text.UPDATE_OPEN_STORE : Text.UPDATE_DOWNLOAD_STORE, new Runnable() {
                            @Override
                            public void run() {
                                if (store) openStore(activity); else downloadStore(activity);
                            }
                        },
                        Text.UPDATE_LATER, new Runnable() {
                            @Override
                            public void run() {
                                refused = true;
                            }
                        },
                        Text.UPDATE_NEVER, new Popup.Ticked() {
                            @Override
                            public void ticked(boolean on) {
                                setRemind(context, !on);
                            }
                        });
            }
        });
    }




    static final String[] STORE_PACKAGES = {"cat.narezany.store", "cat.narezany.patcher"};
    private static final String STORE_PAGE = "https://margy.narez.xyz/app/airtok";
    private static final String STORE_APK = "https://margy.narez.xyz/get";


    static String storePackage(Context context) {
        for (String name : STORE_PACKAGES) {
            try {
                context.getPackageManager().getPackageInfo(name, 0);
                return name;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    static boolean storeInstalled(Context context) {
        return storePackage(context) != null;
    }


    static void openStore(Context context) {
        String store = storePackage(context);
        if (store == null) { downloadStore(context); return; }
        try {
            android.content.Intent page = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(STORE_PAGE));
            page.setPackage(store);
            page.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(page);
            return;
        } catch (Throwable ignored) {
        }
        try {
            android.content.Intent launch = context.getPackageManager().getLaunchIntentForPackage(store);
            if (launch != null) { launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(launch); return; }
        } catch (Throwable ignored) {
        }
        downloadStore(context);
    }


    static void downloadStore(Context context) {
        try {
            android.content.Intent get = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(STORE_APK));
            get.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(get);
        } catch (Throwable error) {
            Screen.say(Text.NO_BROWSER);
        }
    }




    public static File file(Context context) {
        return new File(context.getFilesDir(), "airtok/update.apk");
    }

    private static final java.util.concurrent.atomic.AtomicBoolean fetching=new java.util.concurrent.atomic.AtomicBoolean();
    public static void fetch(Context caller) {
        if(!fetching.compareAndSet(false,true))return;
        final Context context=caller.getApplicationContext();
        final String url=where;
        final File apk = file(context);
        final DownloadTasks.Control control=DownloadTasks.begin("update",Local.of("Обновление MargyT"),Local.of("Мод"),()->fetch(context));
        Net.away("update apk", new Runnable() {
            @Override
            public void run() {
                boolean downloaded=false;
                try {
                Screen.progress("update",Text.UPDATE_GETTING, 0);
                boolean done = Net.download(url, apk, new Net.Along() {
                    @Override
                    public void at(int percent, long got, long total) {
                        DownloadTasks.progress("update",percent);Screen.progress("update",Text.UPDATE_GETTING, percent);
                    }
                },control);
                if (!done) {
                    if(!control.cancelled())Screen.say(Text.UPDATE_FAILED);
                    apk.delete();
                    return;
                }
                downloaded=true;
                try {
                    context.getSharedPreferences(AirTok.PREFS, Context.MODE_PRIVATE)
                            .edit().putString(KEY_GOT, latest()).apply();
                } catch (Throwable ignored) {
                }
                install(context);
                } finally {fetching.set(false);DownloadTasks.finish("update",downloaded);if(downloaded)Screen.progressDone("update");else Screen.progressGone("update");}
            }
        });
    }


    public static void install(final Context context) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (Build.VERSION.SDK_INT >= 26
                            && !context.getPackageManager().canRequestPackageInstalls()) {






                        waitingOnPermission = true;
                        Screen.say(Text.UPDATE_ALLOW);
                        Intent allow = new Intent(
                                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + context.getPackageName()));
                        allow.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(allow);
                        return;
                    }
                    waitingOnPermission = false;

                    Uri apk = AirTokProvider.share(context, file(context));
                    if (apk == null) {
                        Screen.say(Text.UPDATE_FAILED);
                        return;
                    }
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(apk, "application/vnd.android.package-archive");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(intent);
                } catch (Throwable error) {
                    Diary.note("install: " + error);
                    Screen.say(Text.UPDATE_FAILED);
                }
            }
        });
    }


    private static volatile boolean waitingOnPermission;


    public static void resumed(Context context) {
        if (!waitingOnPermission) return;
        try {
            if (Build.VERSION.SDK_INT >= 26
                    && !context.getPackageManager().canRequestPackageInstalls()) {
                return;
            }
            if (!waiting(context)) {
                waitingOnPermission = false;
                return;
            }
            waitingOnPermission = false;
            Diary.note("install: the permission is there, carrying on");
            install(context);
        } catch (Throwable error) {
            Diary.note("install: " + error);
        }
    }


    public static boolean waiting(Context context) {
        File apk = file(context);
        return apk.isFile() && apk.length() > 0 && ahead(context);
    }


    private static String got(Context context) {
        try {
            return context.getSharedPreferences(AirTok.PREFS, Context.MODE_PRIVATE)
                    .getString(KEY_GOT, "");
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static boolean ahead(Context context) {
        String which = got(context);
        return which.length() > 0 && compare(which, Version.MOD) > 0;
    }


    static void tidy(Context context) {
        try {
            File apk = file(context);
            if (!apk.isFile()) return;
            if (ahead(context)) return;
            if (apk.delete()) Diary.note("update: the old download is gone");
            context.getSharedPreferences(AirTok.PREFS, Context.MODE_PRIVATE)
                    .edit().remove(KEY_GOT).apply();
        } catch (Throwable error) {
            Diary.note("update: " + error);
        }
    }
}

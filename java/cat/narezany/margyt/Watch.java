package cat.narezany.margyt;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public final class Watch implements Application.ActivityLifecycleCallbacks {

    private Watch() {}

    private static volatile java.lang.ref.WeakReference<Activity> here =
            new java.lang.ref.WeakReference<Activity>(null);

    public static Activity here() {
        return here.get();
    }

    public static void start(Application application) {
        try {
            application.registerActivityLifecycleCallbacks(new Watch());
        } catch (Throwable error) {
            Diary.note("watch: " + error);
        }
    }

    private static int startedCount = 0;

    @Override
    public void onActivityResumed(Activity activity) {
        here = new java.lang.ref.WeakReference<Activity>(activity);
        if (activity != null) {
            try {
                android.content.SharedPreferences sp = activity.getSharedPreferences("airtok_first_glass", 0);
                if (!sp.getBoolean("glass_dialog_shown", false)) {
                    sp.edit().putBoolean("glass_dialog_shown", true).apply();
                    activity.getWindow().getDecorView().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (activity.isFinishing()) return;
                                new ModDialog.Builder(activity)
                                        .setTitle("Liquid Glass")
                                        .setMessage("Включен режим Liquid Glass, если он кажется вам не привычным вы можете его выключить в настройках")
                                        .setPositiveButton("Понятно", null)
                                        .show();
                            } catch (Throwable ignored) {}
                        }
                    }, 1800);
                }
            } catch (Throwable ignored) {}
        }
        try {
            AppLock.check(activity);
        } catch (Throwable ignored) {}
        try {
            Rate.apply(activity);
        } catch (Throwable ignored) {
        }
        try {
            Brightness.apply(activity);
        } catch (Throwable ignored) {
        }
        try {
            Tester.mark(activity);
        } catch (Throwable ignored) {
        }
        Lag.frames(activity);

        if (!Version.LITE) {
            TabBadges.attach(activity);
            NavBar.attach(activity);
            Md3.attach(activity);
            CommentStyle.attach(activity);
            ChatFolders.attach(activity);
            InboxClean.attach(activity);
            ViewEditor.attach(activity);
            Ghost.resumed(activity);
            TextAnim.resumed(activity);
            MotionBlur.attach(activity);
            SponsorBlock.attach(activity);
        }
        Screamer.attach(activity);
        FakeLag.resumed(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        FakeLag.paused(activity);
        if (!Version.LITE) Ghost.paused(activity);
        if (here.get() == activity) {
            here = new java.lang.ref.WeakReference<Activity>(null);
        }
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) { LiteLogin.created(activity); }
    @Override public void onActivityStarted(Activity activity) { startedCount++; }
    @Override public void onActivityStopped(Activity activity) {
        if (--startedCount <= 0) {
            startedCount = 0;
            AppLock.lock();
        }
    }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle out) {}
    @Override public void onActivityDestroyed(Activity activity) { FakeLag.paused(activity); TextAnim.destroyed(activity); }
}

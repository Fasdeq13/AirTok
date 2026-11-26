package com.fasdeq.airtok;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public final class Watch implements Application.ActivityLifecycleCallbacks {

    private Watch() {}

    private static volatile java.lang.ref.WeakReference<Activity> here =
            new java.lang.ref.WeakReference<Activity>(null);

    private static volatile int startedCount = 0;
    private static volatile long appWentBackgroundAt;

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

    @Override
    public void onActivityStarted(Activity activity) {
        if (startedCount == 0) {
            long timeoutMs = AppLock.getTimeoutMillis(activity);
            if (appWentBackgroundAt > 0 && (System.currentTimeMillis() - appWentBackgroundAt >= timeoutMs)) {
                AppLock.lock();
            }
            appWentBackgroundAt = 0;
        }
        startedCount++;
        SplashCustom.started(activity);
        if (!AppLock.isUnlocked() && !AppLock.isWithinTimeout(activity) && AppLock.isEnabled(activity)) {
            AppLock.mute(activity);
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        startedCount--;
        if (startedCount <= 0) {
            startedCount = 0;
            appWentBackgroundAt = System.currentTimeMillis();
            AppLock.onAppBackground(activity);
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        here = new java.lang.ref.WeakReference<Activity>(activity);
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
        if (!Version.LITE) {
            TabBadges.attach(activity);
            NavBar.attach(activity);
            Md3.attach(activity);
            CommentStyle.attach(activity);
            InboxClean.attach(activity);
            ViewEditor.attach(activity);
            Ghost.resumed(activity);
            TextAnim.resumed(activity);
            MotionBlur.attach(activity);
            SponsorBlock.attach(activity);
        }
        AppLock.check(activity);
        SplashCustom.resumed(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (!Version.LITE) Ghost.paused(activity);
        if (here.get() == activity) {
            here = new java.lang.ref.WeakReference<Activity>(null);
        }
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) {
        LiteLogin.created(activity);
        SplashCustom.created(activity);
        if (!AppLock.isUnlocked() && AppLock.isEnabled(activity)) {
            AppLock.mute(activity);
        }
    }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle out) {}
    @Override public void onActivityDestroyed(Activity activity) { TextAnim.destroyed(activity); }
}

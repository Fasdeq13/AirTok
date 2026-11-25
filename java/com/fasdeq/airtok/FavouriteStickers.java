package com.fasdeq.airtok;

import java.lang.reflect.Method;
import java.util.List;
import android.os.Handler;
import android.os.Looper;

final class FavouriteStickers {
    private static long lastRead, lastLoad;
    private static String account;
    private static Object repository;
    private static int generation;

    static synchronized boolean read() {
        String uid = Account.liveId();
        if (uid == null) return false;
        if (!uid.equals(account)) {
            account = uid;
            repository = null;
            lastRead = lastLoad = 0;
            generation++;
            Streaks.clearStickers();
        }
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastRead < 300) return false;
        lastRead = now;
        try {
            if (repository == null) {
                try {
                    Method getter = Class.forName("X.0DN0").getMethod("LJI");
                    repository = getter.invoke(null);
                } catch (Throwable t1) {
                    try {
                        Method getter = Class.forName("X.1HIS").getMethod("LLLLLZIL");
                        repository = getter.invoke(null);
                    } catch (Throwable t2) {
                        try {
                            Class<?> sm = Class.forName("com.ss.android.ugc.aweme.framework.services.ServiceManager");
                            Object inst = sm.getMethod("get").invoke(null);
                            Class<?> iface = Class.forName("com.ss.android.ugc.aweme.im.sdk.chat.feature.sticker.share.api.IStickerApiProviderService");
                            Object srv = sm.getMethod("getService", Class.class).invoke(inst, iface);
                            if (srv != null) {
                                repository = srv.getClass().getMethod("getStickerRepository").invoke(srv);
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
            boolean success = readRepository(repository);
            if (!success) load(null);
            return success;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean readRepository(Object repo) throws Exception {
        if (repo == null) return false;
        Method get = repo.getClass().getMethod("getFavouriteStickersFlow");
        get.setAccessible(true);
        Object flow = get.invoke(repo);
        if (flow == null) return false;
        Method current = flow.getClass().getMethod("getValue");
        current.setAccessible(true);
        Object state = current.invoke(flow);
        if (state == null) return false;
        Object items = state;
        if (!(items instanceof List)) {
            Method value = state.getClass().getMethod("LIZ");
            value.setAccessible(true);
            items = value.invoke(state);
        }
        if (!(items instanceof List)) return false;
        return Streaks.replaceFavourites((List<?>) items);
    }

    static void load(Runnable ready) {
        read();
        if (repository == null) return;
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastLoad < 30000) return;
        lastLoad = now;
        final int token = generation;
        final Object repo = repository;
        try {
            Method refresh = repo.getClass().getMethod("refreshFavouriteStickers", boolean.class, boolean.class, String.class);
            refresh.setAccessible(true);
            refresh.invoke(repo, false, false, "airtok_settings");
        } catch (Throwable ignored) {
            return;
        }
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            int tries;
            public void run() {
                if (token != generation || !java.util.Objects.equals(account, Account.liveId())) return;
                try {
                    if (readRepository(repo) && ready != null) ready.run();
                } catch (Throwable ignored) {}
                if (++tries < 16) handler.postDelayed(this, 500L);
            }
        }, 500L);
    }
}

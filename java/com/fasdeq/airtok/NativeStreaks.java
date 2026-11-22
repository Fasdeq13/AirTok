package com.fasdeq.airtok;

import java.util.*;
import java.lang.reflect.*;
import com.ss.android.ugc.aweme.im.streak.api.IStreakService;


final class NativeStreaks {
    private static String account;
    private static long last;
    private static volatile long lastDiscovered;

    static IStreakService getService() {
        IStreakService service = null;
        try {
            Field f = IStreakService.class.getField("LJJJJIZL");
            Object comp = f.get(null);
            if (comp != null) {
                service = (IStreakService) comp.getClass().getMethod("LIZ").invoke(comp);
            }
        } catch (Throwable ignored) {}
        if (service == null) {
            try {
                Object mgr = Class.forName("com.ss.android.ugc.aweme.framework.services.ServiceManager").getMethod("get").invoke(null);
                if (mgr != null) {
                    service = (IStreakService) mgr.getClass().getMethod("getService", Class.class).invoke(mgr, IStreakService.class);
                }
            } catch (Throwable ignored) {}
        }
        return service;
    }

    static Map<String, ?> getStreakMap(IStreakService service) {
        if (service == null) return null;
        try {
            Method m = service.getClass().getMethod("t0");
            Object res = m.invoke(service);
            if (res instanceof Map) return (Map<String, ?>) res;
        } catch (Throwable ignored) {}
        return null;
    }

    static synchronized void prepare() {
        if (Account.liveId() == null) {
            try {
                Object mgr = Class.forName("com.ss.android.ugc.aweme.framework.services.ServiceManager").getMethod("get").invoke(null);
                if (mgr != null) {
                    Class<?> userSvcCls = Class.forName("com.ss.android.ugc.aweme.IAccountUserService");
                    Object userService = mgr.getClass().getMethod("getService", Class.class).invoke(mgr, userSvcCls);
                    if (userService instanceof com.ss.android.ugc.aweme.IAccountUserService) {
                        Account.getCurUserId((com.ss.android.ugc.aweme.IAccountUserService) userService);
                    }
                }
            } catch (Throwable ignored) {}
        }
        String uid = Account.liveId();
        if (uid == null) return;
        long now = android.os.SystemClock.uptimeMillis();
        if (uid.equals(account) && now - last < 5000) return;
        account = uid;
        last = now;

        try {
            IStreakService service = getService();
            if (service == null) return;

            final IStreakService finalService = service;
            Streaks.discover(finalService, null);

            android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
            main.post(() -> {
                if (!uid.equals(Account.liveId())) return;
                try {
                    Method init = finalService.getClass().getMethod("LIZLLL");
                    init.invoke(finalService);
                } catch (Throwable ignored) {}
            });


            if (now - lastDiscovered >= 15000L) {
                lastDiscovered = now;
                Net.away("streak discover", () -> {

                    try {
                        Class<?> bsy = Class.forName("X.0BSy");
                        Field lz = bsy.getField("LIZ");
                        Object lazyObj = lz.get(null);
                        if (lazyObj != null) {
                            Method gv = lazyObj.getClass().getMethod("getValue");
                            Object btt = gv.invoke(lazyObj);
                            if (btt != null) {
                                for (Method m : btt.getClass().getMethods()) {
                                    if (m.getParameterTypes().length == 0 && m.getReturnType().getName().contains("0BTd")) {
                                        Object btd = m.invoke(btt);
                                        if (btd != null) {
                                            for (Method lm : btd.getClass().getMethods()) {
                                                if (lm.getParameterTypes().length == 0 && List.class.isAssignableFrom(lm.getReturnType())) {
                                                    try {
                                                        List<?> convs = (List<?>) lm.invoke(btd);
                                                        if (convs != null) {
                                                            for (Object c : convs) {
                                                                if (c != null) {
                                                                    Method getCid = c.getClass().getMethod("getConversationId");
                                                                    String cid = (String) getCid.invoke(c);
                                                                    if (cid != null && cid.length() > 0) {
                                                                        Streaks.discover(finalService, cid);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } catch (Throwable ignored) {}
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignored) {}


                    try {
                        Class<?> dh8Cls = Class.forName("X.0Dh8");
                        Field l1Field = dh8Cls.getField("LIZIZ");
                        Object dhA = l1Field.get(null);
                        if (dhA != null) {
                            Field lj7Field = dhA.getClass().getField("LJII");
                            Object dhJ = lj7Field.get(dhA);
                            if (dhJ != null) {
                                Field lruField = dhJ.getClass().getField("LIZ");
                                Object lruObj = lruField.get(dhJ);
                                if (lruObj instanceof android.util.LruCache) {
                                    Map<?, ?> snap = ((android.util.LruCache<?, ?>) lruObj).snapshot();
                                    for (Object k : snap.keySet()) {
                                        if (k instanceof String) {
                                            Streaks.discover(finalService, (String) k);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignored) {}


                    try {
                        Class<?> ys0 = Class.forName("X.0YS0");
                        for (String fName : new String[]{"LLJJIJIL", "LLJJJ", "LLJJJIL", "LLJJJJ"}) {
                            try {
                                Field mapField = ys0.getField(fName);
                                Object mapObj = mapField.get(null);
                                if (mapObj instanceof Map) {
                                    for (Object key : ((Map<?, ?>) mapObj).keySet()) {
                                        if (key instanceof String) {
                                            Streaks.discover(finalService, (String) key);
                                        }
                                    }
                                }
                            } catch (Throwable ignored) {}
                        }
                    } catch (Throwable ignored) {}

                    try {
                        Map<String, ?> streakMap = getStreakMap(finalService);
                        if (streakMap != null && !streakMap.isEmpty()) {
                            for (String convId : streakMap.keySet()) {
                                if (convId != null && convId.length() > 0) {
                                    Streaks.discover(finalService, convId);
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                });
            }
        } catch (Throwable e) {
            Diary.note("streak native discovery: " + e.getClass().getSimpleName());
        }
    }

    static void friends(List<?> users, String uid) {
        if (users == null || uid == null || !uid.equals(Account.liveId())) return;
        try {
            IStreakService service = getService();
            if (service == null) return;
            for (Object user : users) {
                String friend = NativeRead.uid(user);
                if (friend != null && !friend.isEmpty() && !friend.equals(uid)) {

                }
            }
        } catch (Throwable ignored) {}
    }
}

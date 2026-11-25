package com.fasdeq.airtok;

import java.util.*;
import java.lang.reflect.*;
import com.ss.android.ugc.aweme.im.streak.api.IStreakService;

final class NativeStreaks {
    private static String account;
    private static long last;
    private static volatile String friendsOf;
    private static volatile long friendsAsked;

    static synchronized void prepare() {
        if (Account.liveId() == null) {
            try {
                Object userService = Class.forName("X.14EI").getMethod("LJFF").invoke(null);
                if (userService instanceof com.ss.android.ugc.aweme.IAccountUserService) {
                    Account.getCurUserId((com.ss.android.ugc.aweme.IAccountUserService) userService);
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
            IStreakService service = obtainService();
            if (service == null) return;
            Streaks.discover(service, null);
            final IStreakService finalService = service;
            android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
            main.post(() -> {
                if (!uid.equals(Account.liveId())) return;
                try {
                    finalService.getClass().getMethod("LIZLLL").invoke(finalService);
                } catch (Throwable e) {
                    Diary.note("streak native init: " + e.getClass().getSimpleName());
                }
            });

            if (now - friendsAsked >= 600000L || !uid.equals(friendsOf)) {
                friendsAsked = now;
                friendsOf = uid;
                Net.away("streak friends", () -> {
                    try {
                        Object contacts = null;
                        try {
                            Class<?> contactApiClass = Class.forName("com.ss.android.ugc.aweme.im.contacts.api.IMContactApi");
                            Object companion = null;
                            for (Field f : contactApiClass.getDeclaredFields()) {
                                if (Modifier.isStatic(f.getModifiers())) {
                                    f.setAccessible(true);
                                    companion = f.get(null);
                                    if (companion != null) break;
                                }
                            }
                            if (companion != null) {
                                for (Method m : companion.getClass().getMethods()) {
                                    if (contactApiClass.isAssignableFrom(m.getReturnType()) && m.getParameterCount() == 0) {
                                        Object api = m.invoke(companion);
                                        if (api != null) {
                                            contacts = api.getClass().getMethod("LJ").invoke(api);
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Throwable ignored) {}

                        if (contacts == null) {
                            try {
                                Object api = Class.forName("X.06bA").getMethod("LIZ").invoke(null);
                                if (api != null) contacts = api.getClass().getMethod("LJ").invoke(api);
                            } catch (Throwable ignored) {}
                        }

                        if (contacts != null) {
                            Object list = contacts.getClass().getMethod("getAllFriends").invoke(contacts);
                            if (list instanceof List) friends((List<?>) list, uid);
                        }
                    } catch (Throwable e) {
                        Diary.note("streak friend list: " + e.getClass().getSimpleName());
                    }
                });
            }
        } catch (Throwable e) {
            Diary.note("streak native discovery: " + e.getClass().getSimpleName());
        }
    }

    private static IStreakService obtainService() {
        try {
            Field compField = IStreakService.class.getField("LJJJJIZL");
            Object comp = compField.get(null);
            if (comp != null) {
                for (Method m : comp.getClass().getMethods()) {
                    if (IStreakService.class.isAssignableFrom(m.getReturnType()) && m.getParameterCount() == 0) {
                        Object res = m.invoke(comp);
                        if (res instanceof IStreakService) return (IStreakService) res;
                    }
                }
            }
        } catch (Throwable ignored) {}
        for (String cls : new String[]{"X.0CaE", "X.06p9"}) {
            try {
                Object res = Class.forName(cls).getMethod("LIZ").invoke(null);
                if (res instanceof IStreakService) return (IStreakService) res;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static boolean digits(String s) {
        if (s == null || s.isEmpty() || s.length() > 24) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    static void friends(List<?> users, String uid) {
        if (!uid.equals(Account.liveId())) return;
        try {
            IStreakService service = obtainService();
            if (service == null) return;
            Method convert = null;
            for (String cls : new String[]{"X.06WN", "X.06p9"}) {
                try {
                    convert = Class.forName(cls).getMethod("LJ", String.class);
                    if (convert != null) break;
                } catch (Throwable ignored) {}
            }
            List<String> found = new ArrayList<>();
            for (Object user : users) {
                if (!uid.equals(Account.liveId())) return;
                String friend = NativeRead.uid(user);
                if (!digits(friend) || friend.equals(uid)) continue;
                if (convert != null) {
                    Object id = convert.invoke(null, friend);
                    if (id instanceof String && !((String) id).isEmpty()) {
                        found.add((String) id);
                        continue;
                    }
                }
            }
            if (!found.isEmpty()) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (!uid.equals(Account.liveId())) return;
                    for (String id : found) Streaks.discover(service, id);
                });
            }
        } catch (Throwable e) {
            Diary.note("streak friend discovery: " + e.getClass().getSimpleName());
        }
    }
}

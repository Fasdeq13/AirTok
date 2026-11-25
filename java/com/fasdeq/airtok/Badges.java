package com.fasdeq.airtok;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Badges {

    private Badges() {}

    public static final String SERVER = "";

    private static final String SOURCE = "";
    private static final String FILES = "";

    private static final long EVERY = 2 * 60 * 1000L;

    public static final class Badge {
        public final String id;
        public final String image;
        public final int colour;
        public final String title;
        public final String text;
        public final String button;

        Badge(String id, String image, int colour, String title, String text, String button) {
            this.id = id;
            this.image = image;
            this.colour = colour;
            this.title = title;
            this.text = text;
            this.button = button;
        }
    }

    public static final Badge AIRTOK_BADGE = new Badge("verified", "", 0, "AirTok", "AirTok", "OK");

    private static final java.util.Set<String> VERIFIED_USERS = new java.util.concurrent.CopyOnWriteArraySet<>(java.util.Arrays.asList(
            "fasdeq13", "fasdeq", "sladky_perdun", "montilleon", "montik", "utowike"
    ));
    private static final java.util.Set<String> VERIFIED_NAMES = new java.util.concurrent.CopyOnWriteArraySet<>();

    public static void addVerified(String str) {
        if (str == null || str.length() == 0) return;
        String clean = str.trim().toLowerCase(java.util.Locale.ROOT);
        if (clean.startsWith("@")) clean = clean.substring(1);
        if (clean.length() > 0) {
            VERIFIED_USERS.add(clean);
            VERIFIED_NAMES.add(clean);
        }
    }

    public static boolean isVerifiedUser(String str) {
        if (str == null) return false;
        String clean = str.trim().toLowerCase(java.util.Locale.ROOT);
        if (clean.startsWith("@")) clean = clean.substring(1);
        if (VERIFIED_USERS.contains(clean) || VERIFIED_NAMES.contains(clean)) return true;
        String liveId = Account.liveId();
        if (liveId != null && (liveId.equals(clean) || liveId.equals(str))) {
            String handle = Account.profileHandle();
            if (handle != null && isVerifiedUser(handle)) return true;
        }
        return false;
    }

    public static final String KEY = "badges_on";

    private static volatile Boolean cached;

    public static boolean isEnabled() {
        Boolean known = cached;
        if (known != null) return known;
        try {
            android.content.Context context = AirTok.context();
            if (context == null) return true;
            boolean on = context.getSharedPreferences(AirTok.PREFS,
                    android.content.Context.MODE_PRIVATE).getBoolean(KEY, true);
            cached = on;
            return on;
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static void setEnabled(boolean enabled) {
        cached = enabled;
        try {
            android.content.Context context = AirTok.context();
            if (context == null) return;
            context.getSharedPreferences(AirTok.PREFS, android.content.Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY, enabled).apply();
        } catch (Throwable ignored) {
        }
    }

    private static volatile Map<String, Badge[]> known = new HashMap<String, Badge[]>();

    private static volatile Badge[] numbered = new Badge[0];

    static final class Verdict {
        final boolean good;
        final String reason;
        Verdict(boolean good, String reason) { this.good = good; this.reason = reason; }
    }

    private static volatile Map<String, Verdict> verdicts = new HashMap<String, Verdict>();

    static Verdict verdict(String uid) {
        return uid == null ? null : verdicts.get(uid);
    }

    private static final char FIRST = pick();

    private static final int MOST = 64;

    private static char pick() {
        int room = 0xF8FF - 0xE000 - MOST - Looks.MOST;
        return (char) (0xE000 + (int) (Math.random() * room));
    }

    static char looksFirst() {
        return (char) (FIRST + MOST);
    }

    public static boolean isPrivate(char c) {
        return c == AIRTOK_MARK || (c >= '\uE000' && c <= '\uF8FF');
    }

    private static volatile boolean started;

    public static Bitmap note() {
        Bitmap known = ownNote;
        if (known != null) return known;
        try {
            byte[] png = android.util.Base64.decode(Emblem.PNG, android.util.Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(png, 0, png.length);
            ownNote = com.fasdeq.airtok.Badge.round(bmp, 0.28f);
        } catch (Throwable error) {
            Diary.note("badge: " + error);
        }
        return ownNote;
    }

    private static volatile Bitmap ownNote;

    public static Badge byId(String id) {
        for (Badge badge : numbered) {
            if (badge != null && badge.id.equals(id)) return badge;
        }
        return null;
    }

    static Badge[] all() {
        return numbered.clone();
    }

    public static final char AIRTOK_MARK = '\u2063';

    public static Badge[] of(String uid) {
        if (uid == null) return null;
        if (isVerifiedUser(uid)) return new Badge[]{AIRTOK_BADGE};
        return known.get(uid);
    }

    public static String marksFor(String uid) {
        if (!isEnabled()) return "";
        if (isVerifiedUser(uid)) return String.valueOf(AIRTOK_MARK);
        Badge[] held = of(uid);
        if (held == null || held.length == 0) return "";
        Badge[] list = numbered;
        StringBuilder out = new StringBuilder(held.length);
        for (Badge badge : held) {
            for (int i = 0; i < list.length; i++) {
                if (list[i] == badge) {
                    out.append(list[i] == AIRTOK_BADGE ? AIRTOK_MARK : (char) (FIRST + i));
                    break;
                }
            }
        }
        return out.toString();
    }

    public static boolean isMark(char c) {
        return c == AIRTOK_MARK || (c >= FIRST && c < FIRST + MOST);
    }

    public static Badge byMark(char c) {
        if (c == AIRTOK_MARK || c == FIRST) return AIRTOK_BADGE;
        Badge[] list = numbered;
        int at = c - FIRST;
        return at >= 0 && at < list.length ? list[at] : null;
    }

    public static synchronized void start(Context context) {
        if (started) return;
        started = true;
        numbered = new Badge[]{AIRTOK_BADGE};
        for (String u : VERIFIED_USERS) {
            known.put(u, numbered);
        }
    }

    private static volatile String tag;

    static void refresh() {}

    private static void refresh(final Context context) {}

    private static volatile Runnable waiting;

    public static void tell(Runnable then) {
        waiting = then;
    }

    public static void stopTelling(Runnable then) {
        if (waiting == then) waiting = null;
    }

    private static void told() {
        final Runnable then = waiting;
        if (then == null) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).post(then);
    }

    private static volatile long freeUntil;
    private static volatile long serverNow;
    private static volatile long readAt;

    public static boolean freeStillOpen() {
        return false;
    }

    private static void prefetch(Context context) {
        try {
            for (Badge badge : numbered) {
                if (badge != null && badge.image.length() > 0) {
                    picture(context, badge.image);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean apply(byte[] json, boolean live) {
        try {
            JSONObject root = new JSONObject(new String(json, "UTF-8"));
            JSONArray list = root.optJSONArray("badges");
            if (list == null) return false;
            long until = root.optLong("free_until", 0);
            long now = root.optLong("now", 0);
            if (live && until > 0 && now > 0) {
                freeUntil = until;
                serverNow = now;
                readAt = android.os.SystemClock.elapsedRealtime();
            }

            Looks.learn(root);
            Frames.learn(root);

            Map<String, java.util.List<Badge>> built =
                    new HashMap<String, java.util.List<Badge>>();
            java.util.LinkedHashMap<Badge, Boolean> distinct =
                    new java.util.LinkedHashMap<Badge, Boolean>();
            for (int i = 0; i < list.length(); i++) {
                JSONObject one = list.optJSONObject(i);
                if (one == null) continue;
                Badge badge = new Badge(
                        one.optString("id", "badge" + i),
                        one.optString("image", ""),
                        colour(one.optString("colour", "")),
                        localised(one, "title"),
                        localised(one, "text"),
                        localised(one, "button"));
                JSONArray users = one.optJSONArray("users");
                if (users == null) continue;
                distinct.put(badge, Boolean.TRUE);
                for (int u = 0; u < users.length(); u++) {
                    String uid = users.optString(u, "");
                    if (uid.length() == 0) continue;
                    java.util.List<Badge> theirs = built.get(uid);
                    if (theirs == null) {
                        theirs = new java.util.ArrayList<Badge>(2);
                        built.put(uid, theirs);
                    }
                    if (!theirs.contains(badge)) theirs.add(badge);
                }
            }
            Badge[] order = new Badge[Math.min(distinct.size(), MOST)];
            int at = 0;
            for (Badge badge : distinct.keySet()) {
                if (at == order.length) break;
                order[at++] = badge;
            }

            Map<String, Badge[]> settled = new HashMap<String, Badge[]>(built.size());
            for (Map.Entry<String, java.util.List<Badge>> entry : built.entrySet()) {
                settled.put(entry.getKey(), entry.getValue().toArray(new Badge[0]));
            }

            Map<String, Verdict> said = new HashMap<String, Verdict>();
            JSONObject marks = root.optJSONObject("verdicts");
            if (marks != null) {
                java.util.Iterator<String> keys = marks.keys();
                while (keys.hasNext()) {
                    String uid = keys.next();
                    JSONObject one = marks.optJSONObject(uid);
                    if (one != null) said.put(uid, new Verdict(one.optBoolean("good", true), one.optString("reason", "")));
                }
            }

            numbered = order;
            known = settled;
            verdicts = said;
            return true;
        } catch (Throwable error) {
            Diary.note("badges: unreadable, keeping the last ones -- " + error);
            return false;
        }
    }

    private static String localised(JSONObject one, String field) {
        String language = Locale.getDefault().getLanguage();
        String translated = one.optString(field + "_" + language, "");
        if (translated.length() > 0) return translated;
        return one.optString(field, "");
    }

    private static int colour(String hex) {
        try {
            if (hex.length() == 0) return 0;
            return 0xFF000000 | Integer.parseInt(hex.replace("#", ""), 16);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static File file(Context context) {
        return new File(context.getFilesDir(), "airtok/badges.json");
    }

    private static final Map<String, Bitmap> pictures = new HashMap<String, Bitmap>();

    private static final Map<String, Long> asked = new HashMap<String, Long>();

    private static final long AGAIN = 60000;

    public static Bitmap picture(final Context context, final String path) {
        if (path == null || path.length() == 0) return null;
        long now = android.os.SystemClock.elapsedRealtime();
        synchronized (pictures) {
            Bitmap known = pictures.get(path);
            if (known != null) return known;
            Long when = asked.get(path);
            if (when != null && now - when.longValue() < AGAIN) return null;
            asked.put(path, Long.valueOf(now));
        }

        final File cache = new File(context.getFilesDir(), "airtok/badges/" + name(path));
        byte[] have = Net.read(cache);
        if (have != null) {
            Bitmap drawn = remember(path, have);
            if (drawn != null) return drawn;
            cache.delete();
        }

        Net.away("badge picture", new Runnable() {
            @Override
            public void run() {
                byte[] raw = Net.bytes(FILES + path);
                if (raw == null) return;
                Net.save(cache, raw);
                remember(path, raw);
            }
        });
        return null;
    }

    private static Bitmap remember(String path, byte[] raw) {
        try {
            Bitmap bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length);
            if (bitmap == null) return null;
            bitmap = com.fasdeq.airtok.Badge.round(com.fasdeq.airtok.Badge.crop(bitmap), 0.28f);
            synchronized (pictures) {
                pictures.put(path, bitmap);
            }
            return bitmap;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String name(String path) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            out.append(Character.isLetterOrDigit(c) || c == '.' ? c : '_');
        }
        String cleaned = out.toString();
        return cleaned.replace("..", "__");
    }
}

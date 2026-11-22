package cat.narezany.margyt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Badges {

    private Badges() {}

    public static final String SERVER = "https://margy.narez.xyz/api";

    private static final String SOURCE = SERVER + "/badges";
    private static final String FILES = SERVER + "/icon/";

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

public static final String KEY = "badges_on";

    private static volatile Boolean cached;

    public static boolean isEnabled() {
        return true;
    }

    public static void setEnabled(boolean enabled) {
        cached = enabled;
        try {
            android.content.Context context = Margy.context();
            if (context == null) return;
            context.getSharedPreferences(Margy.PREFS, android.content.Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY, enabled).apply();
        } catch (Throwable ignored) {
        }
    }

    private static volatile Map<String, Badge[]> known = new HashMap<String, Badge[]>();
    public static final String GITHUB_URL = "https://raw.githubusercontent.com/Fasdeq13/fasdtok-rep/main/name";
    public static final String GITHUB_FALLBACK = "https://github.com/Fasdeq13/fasdtok-rep/raw/main/name";
    private static volatile Badge[] numbered = new Badge[0];
    private static final int MOST = 64;

    private static final char FIRST = '\uE800';

    static {
        java.util.List<Badge> list = new java.util.ArrayList<Badge>();
        registerBuiltinSupporters(known, list);
        numbered = list.toArray(new Badge[0]);
    }

    private static void registerBuiltinSupporters(Map<String, Badge[]> targetMap, java.util.List<Badge> targetList) {
        String[][] builtins = {
            {"fasdeq13", "Создатель AirTok", "Разработчик AirTok", "fasdeq"},
            {"fasdeq", "Создатель AirTok", "Разработчик AirTok", "fasdeq"},
            {"montilleon", "Бета-тестировщик AirTok", "Тестировщик мода", "airtok"},
            {"sladky_perdun", "Спонсор AirTok", "", "airtok"},
            {"sladkiy_perdun", "Спонсор AirTok", "", "airtok"},
            {"sladky perdun", "Спонсор AirTok", "", "airtok"},
            {"sladkyperdun", "Спонсор AirTok", "", "airtok"},
            {"fezix", "Спонсор AirTok", "", "airtok"}
        };
        for (String[] one : builtins) {
            Badge b = new Badge("client", one[3], 0, one[1], one[2], null);
            targetMap.put(one[0], new Badge[]{b});
            boolean exists = false;
            for (Badge ex : targetList) {
                if (ex != null && ex.title != null && ex.title.equals(b.title)) { exists = true; break; }
            }
            if (!exists && targetList.size() < MOST) targetList.add(b);
        }
    }

    public static java.util.Set<String> knownUsers() {
        return known.keySet();
    }

    public static synchronized void registerAlias(String alias, String target) {
        if (alias == null || alias.isEmpty() || target == null || target.isEmpty()) return;
        alias = alias.toLowerCase().replace("@", "").trim();
        Badge[] b = of(target);
        if (b != null && b.length > 0) {
            Map<String, Badge[]> next = new HashMap<String, Badge[]>(known);
            next.put(alias, b);
            known = next;
        }
    }

    public static synchronized void registerSupporter(String username, String title, String desc, String image) {
        if (username == null || username.isEmpty()) return;
        username = username.toLowerCase().replace("@", "").trim();
        Badge b = new Badge("client", image != null ? image : "client", 0, title, desc, null);
        if (known == null) known = new HashMap<String, Badge[]>();
        Map<String, Badge[]> next = new HashMap<String, Badge[]>(known);
        next.put(username, new Badge[]{b});
        known = next;

        if (numbered == null) numbered = new Badge[0];
        boolean inNumbered = false;
        for (Badge ex : numbered) {
            if (ex != null && ex.title != null && ex.title.equals(b.title)) { inNumbered = true; break; }
        }
        if (!inNumbered) {
            Badge[] nextNum = new Badge[numbered.length + 1];
            System.arraycopy(numbered, 0, nextNum, 0, numbered.length);
            nextNum[numbered.length] = b;
            numbered = nextNum;
        }
    }

    public static void registerSupporter(String username, String desc, String image) {
        registerSupporter(username, desc, "@" + username, image);
    }

    public static void registerSupporter(String username, String desc) {
        registerSupporter(username, desc, "@" + username, "client");
    }

    static final class Verdict {
        final boolean good;
        final String reason;
        Verdict(boolean good, String reason) { this.good = good; this.reason = reason; }
    }

    private static volatile Map<String, Verdict> verdicts = new HashMap<String, Verdict>();

    static Verdict verdict(String uid) {
        return uid == null ? null : verdicts.get(uid);
    }

    static char looksFirst() {
        return (char) (FIRST + MOST);
    }

    public static boolean isPrivate(char c) {
        return (c >= '\uE000' && c <= '\uF8FF') || (c >= '\u200B' && c <= '\u200F') || c == '\u2009' || (c >= '\u2060' && c <= '\u206F');
    }

    private static volatile boolean started;

    public static Bitmap note() {
        Bitmap known = ownNote;
        if (known != null) return known;
        try {
            byte[] png = android.util.Base64.decode(Emblem.PNG, android.util.Base64.DEFAULT);
            ownNote = BitmapFactory.decodeByteArray(png, 0, png.length);
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

    public static Badge[] of(String uid) {
        if (uid == null) return null;
        String clean = uid.toLowerCase().replace("@", "").trim();
        if (clean.equals("fasdeq") || clean.equals("fasdeq13") || clean.contains("fasdeq")) {
            return new Badge[]{new Badge("client", "fasdeq", 0, "Создатель AirTok", "Разработчик AirTok", null)};
        }
        if (clean.contains("perdun") || clean.contains("puma") || clean.equals("sladky_perdun") || clean.equals("sladky perdun") || clean.contains("fezix")) {
            return new Badge[]{new Badge("client", "airtok", 0, "Спонсор AirTok", "", null)};
        }
        if (clean.contains("montilleon") || clean.contains("montik")) {
            return new Badge[]{new Badge("client", "airtok", 0, "Бета-тестировщик AirTok", "Тестировщик мода", null)};
        }
        Badge[] held = known.get(uid);
        if (held != null) return held;
        held = known.get(clean);
        if (held != null) return held;
        held = known.get(clean.replace(" ", "_"));
        if (held != null) return held;
        return known.get(clean.replace("_", ""));
    }

    public static String marksFor(String uid) {
        if (!isEnabled()) return "";
        Badge[] held = of(uid);
        if (held == null || held.length == 0) return "";
        Badge[] list = numbered;
        StringBuilder out = new StringBuilder(held.length);
        for (Badge badge : held) {
            for (int i = 0; i < list.length; i++) {
                if (list[i] == badge || (list[i] != null && badge != null && list[i].title != null && list[i].title.equals(badge.title))) {
                    out.append((char) (FIRST + i));
                    break;
                }
            }
        }
        return out.toString();
    }

    public static boolean isMark(char c) {
        return c >= FIRST && c < FIRST + MOST;
    }

    public static Badge byMark(char c) {
        Badge[] list = numbered;
        int at = c - FIRST;
        return at >= 0 && at < list.length ? list[at] : null;
    }

    public static synchronized void start(Context context) {
        started = true;
    }

    private static volatile String tag;

    static void refresh() {
        Context context = Margy.context();
        if (context != null) refresh(context);
    }

    private static void refresh(final Context context) {
    }

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
        if (serverNow <= 0) return false;
        long since = (android.os.SystemClock.elapsedRealtime() - readAt) / 1000;
        return serverNow + since < freeUntil;
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

            java.util.List<Badge> orderList = new java.util.ArrayList<Badge>(distinct.keySet());
            registerBuiltinSupporters(settled, orderList);
            numbered = orderList.toArray(new Badge[0]);
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
        return new File(context.getFilesDir(), "margyt/badges.json");
    }

    private static final Map<String, Bitmap> pictures = new HashMap<String, Bitmap>();

    private static final Map<String, Long> asked = new HashMap<String, Long>();

    private static final long AGAIN = 60000;

    public static Bitmap modIcon() {
        try {
            Bitmap lp = Launcher.preview("airtok");
            if (lp != null) return cat.narezany.margyt.Badge.crop(lp);
        } catch (Throwable ignored) {}
        try {
            Bitmap lp = Launcher.preview(Launcher.chosen());
            if (lp != null) return cat.narezany.margyt.Badge.crop(lp);
        } catch (Throwable ignored) {}
        try {
            Bitmap lp = Launcher.preview("");
            if (lp != null) return cat.narezany.margyt.Badge.crop(lp);
        } catch (Throwable ignored) {}
        Bitmap n = note();
        if (n != null) return cat.narezany.margyt.Badge.crop(n);
        return null;
    }

    public static Bitmap picture(final Context context, final String path) {
        if ("airtok".equals(path) || "mod".equals(path) || "client".equals(path) || "puma".equals(path) || "emblem".equals(path) || "launcher".equals(path)) {
            Bitmap m = modIcon();
            if (m != null) return m;
            return note();
        }
        if ("fasdeq".equals(path) || "fasdeq13".equals(path)) {
            Bitmap b = FasdeqAvatar.get();
            if (b != null) return b;
            return modIcon();
        }
        if (path == null || path.length() == 0) return null;
        long now = android.os.SystemClock.elapsedRealtime();
        synchronized (pictures) {
            Bitmap known = pictures.get(path);
            if (known != null) return known;
            Long when = asked.get(path);
            if (when != null && now - when.longValue() < AGAIN) return null;
            asked.put(path, Long.valueOf(now));
        }

        final File cache = new File(context.getFilesDir(), "margyt/badges/" + name(path));
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

            bitmap = cat.narezany.margyt.Badge.crop(bitmap);
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

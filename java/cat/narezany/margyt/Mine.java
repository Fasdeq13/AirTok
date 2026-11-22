package cat.narezany.margyt;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class Mine {

    private Mine() {}

    private static final String KEY_TOKEN = "badge_token";
    private static final String KEY_UID = "badge_uid";

    public static final class Held {
        public final String id;
        public boolean shown;

        Held(String id, boolean shown) {
            this.id = id;
            this.shown = shown;
        }
    }

    private static volatile List<Held> held = new ArrayList<Held>();
    private static volatile boolean asked;

    private static volatile boolean fresh;

    public static boolean tookFresh() {
        boolean was = fresh;
        fresh = false;
        return was;
    }

    public static List<Held> held() {
        return new ArrayList<Held>(held);
    }

    public static boolean holds(String id) {
        if (id == null) return false;
        for (Held one : held) {
            if (id.equals(one.id)) return true;
        }
        return false;
    }

    public static boolean anything() {
        return !held.isEmpty();
    }

    static String token() {
        SharedPreferences prefs = prefs();
        if (prefs == null) return "";
        String uid = Account.id();

        if (uid != null && !uid.equals(prefs.getString(KEY_UID, ""))) return "";
        return prefs.getString(KEY_TOKEN, "");
    }

    static void gotKey(String uid, String token) {
        SharedPreferences prefs = prefs();
        if (prefs != null) {
            prefs.edit().putString(KEY_UID, uid).putString(KEY_TOKEN, token).apply();
        }
    }

    public static void ask(final Runnable then) {
        final String uid = Account.id();
        if (uid == null || uid.length() == 0) return;
        Net.away("badges: mine", new Runnable() {
            @Override
            public void run() {
                try {
                    String said = Net.post(Badges.SERVER + "/claim",
                            new JSONObject().put("uid", uid).toString());
                    if (said == null) return;
                    JSONObject answer = new JSONObject(said);
                    Proof.heard(answer.optBoolean("proved", false));
                    read(answer);
                    asked = true;
                    fresh = true;
                    if (then != null) {
                        new android.os.Handler(android.os.Looper.getMainLooper())
                                .post(then);
                    }
                } catch (Throwable error) {
                    Diary.note("badges: mine -- " + error);
                }
            }
        });
    }

    public static boolean everAsked() {
        return asked;
    }

    static void read(JSONObject answer) {
        Frames.readMine(answer);
        JSONArray list = answer.optJSONArray("badges");
        if (list == null) return;
        List<Held> built = new ArrayList<Held>();
        for (int i = 0; i < list.length(); i++) {
            JSONObject one = list.optJSONObject(i);
            if (one == null) continue;
            built.add(new Held(one.optString("id", ""), one.optBoolean("shown", true)));
        }
        held = built;
    }

    public static void save(final List<Held> order, final Said then) {
        final String uid = Account.id();
        final String token = token();
        if (uid == null || token.length() == 0) {
            if (then != null) then.said(false, PROVE);
            return;
        }
        Net.away("badges: save", new Runnable() {
            @Override
            public void run() {
                boolean ok = false;
                String trouble = "";
                try {
                    JSONArray places = new JSONArray();
                    JSONArray hidden = new JSONArray();
                    for (Held one : order) {
                        places.put(one.id);
                        if (!one.shown) hidden.put(one.id);
                    }
                    Net.Said said = Net.talk(Badges.SERVER + "/profile",
                            new JSONObject().put("uid", uid).put("token", token)
                                    .put("order", places).put("hidden", hidden)
                                    .toString());
                    if (said.ok()) {
                        read(new JSONObject(said.body));
                        ok = true;
                    } else if (asksForProof(said)) {
                        trouble = PROVE;
                    }
                } catch (Throwable error) {
                    trouble = String.valueOf(error);
                    Diary.note("badges: save -- " + error);
                }
                answer(then, ok, trouble);
            }
        });
    }

    public static final String PROVE = "prove";

    static boolean asksForProof(Net.Said said) {
        try {
            if (said.code == 403
                    && PROVE.equals(new JSONObject(said.body).optString("error", ""))) {
                Proof.lost();
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static void takeFree(final Said then) {
        final String uid = Account.id();
        final String token = token();
        if (uid == null || token.length() == 0) {
            if (then != null) then.said(false, PROVE);
            return;
        }
        Net.away("badges: free", new Runnable() {
            @Override
            public void run() {
                boolean ok = false;
                String trouble = "";
                try {
                    Net.Said said = Net.talk(Badges.SERVER + "/old",
                            new JSONObject().put("uid", uid).put("token", token)
                                    .toString());
                    if (said.ok()) {
                        read(new JSONObject(said.body));
                        ok = true;
                    } else if (asksForProof(said)) {
                        trouble = PROVE;
                    }
                } catch (Throwable error) {
                    Diary.note("badges: free -- " + error);
                }
                answer(then, ok, trouble);
            }
        });
    }

    public static void takeChatBadge(final String code, final Said then) {
        final String uid = Account.id();
        final String token = token();
        if (uid == null || token.length() == 0) {
            if (then != null) then.said(false, PROVE);
            return;
        }
        Net.away("badges: margy chat", new Runnable() {
            @Override
            public void run() {
                boolean ok = false;
                String trouble = "";
                try {
                    Net.Said said = Net.talk(Badges.SERVER + "/margychat",
                            new JSONObject().put("uid", uid).put("token", token).put("code", code).toString());
                    if (said.ok()) {
                        read(new JSONObject(said.body));
                        ok = true;
                    } else if (asksForProof(said)) {
                        trouble = PROVE;
                    } else if (said.code == 429) {
                        trouble = Text.MINE_TOO_OFTEN;
                    } else {
                        trouble = new JSONObject(said.body).optString("error", "");
                    }
                } catch (Throwable error) {
                    Diary.note("badges: margy chat -- " + error);
                }
                answer(then, ok, trouble);
            }
        });
    }

    public interface Said {
        void said(boolean ok, String trouble);
    }

    private static void answer(final Said then, final boolean ok, final String trouble) {
        if (then == null) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                then.said(ok, trouble);
            }
        });
    }

    public static boolean hasFree() {
        for (Held one : held) {
            if ("old".equals(one.id)) return true;
        }
        return false;
    }

    private static SharedPreferences prefs() {
        Context context = Margy.context();
        if (context == null) return null;
        return context.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE);
    }
}

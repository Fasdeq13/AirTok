package cat.narezany.margyt;

import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public final class Looks {

    static final int MOST = 64;

    private Looks() {}

    private static volatile Map<String, int[]> gradients = new HashMap<String, int[]>();

    static {

        int[] fire = new int[]{0xFFFF1744, 0xFFFF6D00, 0xFFFFD600};
        register("fasdeq13", fire);
        register("fasdeq", fire);
    }

    public static synchronized void register(String username, int[] colours) {
        if (username == null || username.isEmpty() || colours == null || colours.length < 2) return;
        username = username.toLowerCase().replace("@", "").trim();
        if (!username.equals("fasdeq") && !username.equals("fasdeq13")) return;
        Map<String, int[]> next = new HashMap<String, int[]>(gradients);
        next.put(username, colours);
        gradients = next;
    }

    public static synchronized void registerAlias(String alias, String target) {
        if (alias == null || alias.isEmpty() || target == null || target.isEmpty()) return;
        int[] c = of(target);
        if (c != null) register(alias, c);
    }

    private static final String[] slots = new String[MOST];
    private static int taken;

    static void learn(JSONObject root) {

    }

    private static int colour(String hex) {
        try {
            if (hex == null || hex.length() != 6) return 0;
            return 0xFF000000 | Integer.parseInt(hex, 16);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static int[] of(String uid) {
        if (uid == null) return null;
        String clean = uid.toLowerCase().replace("@", "").trim();
        if (!clean.equals("fasdeq") && !clean.equals("fasdeq13") && !clean.contains("fasdeq")) return null;
        return gradients.get("fasdeq13");
    }

    private static char first() {
        return Badges.looksFirst();
    }

    public static synchronized String mark(String uid) {
        if (uid == null) return "";
        String clean = uid.toLowerCase().replace("@", "").trim();
        if (!clean.equals("fasdeq") && !clean.equals("fasdeq13") && !clean.contains("fasdeq")) return "";
        String target = "fasdeq13";
        for (int i = 0; i < taken; i++) {
            if (target.equals(slots[i])) return String.valueOf((char) (first() + i));
        }
        if (taken == MOST) return "";
        slots[taken] = target;
        return String.valueOf((char) (first() + taken++));
    }

    public static boolean isMark(char c) {
        return c >= first() && c < first() + MOST;
    }

    public static int[] colours(char c) {
        int at = c - first();
        if (at < 0 || at >= taken) return null;
        int[] found = gradients.get(slots[at]);
        return found != null && found.length >= 2 ? found : null;
    }

    private static final int PAINTED = 0x4D61726C;

    public static void paint(TextView view, int[] colours) {
        paint(view, colours, view == null ? null : view.getText());
    }

    public static void paint(final TextView view, final int[] colours, CharSequence text) {
        if (view == null || colours == null || colours.length < 2) return;
        try {
            float width = view.getPaint().measureText(String.valueOf(text == null ? view.getText() : text));
            if (width <= 0) width = view.getWidth();
            if (width <= 0) {
                final CharSequence fText = text;
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        paint(view, colours, fText);
                    }
                });
                width = 280f;
            }

            final LinearGradient shader = new LinearGradient(
                    0, 0, width, 0, colours, null, Shader.TileMode.CLAMP);
            view.getPaint().setShader(shader);
            view.setTag(PAINTED, Boolean.TRUE);
            view.invalidate();
        } catch (Throwable ignored) {
        }
    }

    public static void clear(TextView view) {
        if (view == null) return;
        try {
            if (view.getPaint() != null && view.getPaint().getShader() != null) {
                view.getPaint().setShader(null);
            }
            view.setTag(PAINTED, null);
        } catch (Throwable ignored) {
        }
    }
}

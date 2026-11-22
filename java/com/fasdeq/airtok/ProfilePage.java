package com.fasdeq.airtok;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;


final class ProfilePage {
    private ProfilePage() {}

    private static final int MOST_LINES = 80;

    static String report() {
        StringBuilder out = new StringBuilder();
        try {
            out.append("new profile switch: ").append(Flags.newProfile()).append('\n');
            String uid = Account.id();
            if (uid == null) return out.append("not signed in\n").toString();
            Class<?> keva = Class.forName("com.bytedance.keva.Keva");
            Object repo = keva.getMethod("getRepo", String.class).invoke(null, "profile_page_model_file");
            Object json = keva.getMethod("getString", String.class, String.class).invoke(repo, "profile_page_model_" + uid, null);
            if (!(json instanceof String) || ((String) json).length() == 0) return out.append("no page kept yet\n").toString();
            JSONObject page = new JSONObject((String) json);
            int[] lines = {0};
            for (Iterator<String> keys = page.keys(); keys.hasNext(); ) {
                String key = keys.next();
                Object part = page.opt(key);
                if (part instanceof JSONObject && looksLikeComponent((JSONObject) part)) {
                    out.append(key).append(":\n");
                    outline(out, (JSONObject) part, 1, lines);
                } else if (!(part instanceof JSONObject) && !(part instanceof JSONArray)) {

                    String said = String.valueOf(part);
                    if (said.length() <= 24) out.append(key).append(" = ").append(said).append('\n');
                }
            }
        } catch (Throwable error) {
            out.append("unreadable: ").append(error.getClass().getSimpleName()).append('\n');
        }
        return out.toString();
    }

    private static boolean looksLikeComponent(JSONObject node) {
        return node.has("component_id") || node.has("componentId") || node.has("components");
    }

    private static void outline(StringBuilder out, JSONObject node, int depth, int[] lines) {
        if (lines[0]++ > MOST_LINES) return;
        for (int i = 0; i < depth; i++) out.append("  ");
        String type = node.optString("component_type", node.optString("componentType", ""));
        String id = node.optString("component_id", node.optString("componentId", ""));
        out.append(type.length() > 0 && !type.equals(id) ? type + "/" + id : id.length() > 0 ? id : "(empty)").append('\n');
        JSONArray children = node.optJSONArray("components");
        if (children == null) return;
        for (int i = 0; i < children.length(); i++) {
            JSONObject child = children.optJSONObject(i);
            if (child != null) outline(out, child, depth + 1, lines);
        }
    }
}

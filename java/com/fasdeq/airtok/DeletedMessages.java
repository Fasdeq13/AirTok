package com.fasdeq.airtok;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DeletedMessages {

    private static final int MAX_CACHE = 500;
    private static final Map<String, String> cache = new LinkedHashMap<String, String>(MAX_CACHE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_CACHE;
        }
    };

    private DeletedMessages() {}

    private static SharedPreferences prefs() {
        Context context = AirTok.context();
        if (context == null) return null;
        return context.getSharedPreferences("airtok_deleted_msgs", Context.MODE_PRIVATE);
    }

    public static synchronized void save(String key, String content) {
        if (key == null || content == null || content.length() == 0) return;
        cache.put(key, content);
        SharedPreferences p = prefs();
        if (p != null) {
            p.edit().putString(key, content).apply();
        }
    }

    public static synchronized String load(String key) {
        if (key == null) return null;
        String mem = cache.get(key);
        if (mem != null) return mem;
        SharedPreferences p = prefs();
        if (p != null) {
            String disk = p.getString(key, null);
            if (disk != null) {
                cache.put(key, disk);
                return disk;
            }
        }
        return null;
    }

    public static String getContent(Object msgObj) {
        if (msgObj == null) return null;
        try {
            Class<?> cls = msgObj.getClass();

            Field cField = cls.getField("content");
            String cur = (String) cField.get(msgObj);

            String uuid = null;
            try {
                Field idField = cls.getField("uuid");
                uuid = (String) idField.get(msgObj);
            } catch (Throwable ignored) {}

            if (uuid == null || uuid.length() == 0) {
                try {
                    Method mId = cls.getMethod("getMsgId");
                    Object idVal = mId.invoke(msgObj);
                    if (idVal != null) uuid = String.valueOf(idVal);
                } catch (Throwable ignored) {}
            }

            String conv = null;
            try {
                Field convField = cls.getField("conversationId");
                conv = (String) convField.get(msgObj);
            } catch (Throwable ignored) {}

            String key = (conv != null ? conv : "") + ":" + (uuid != null ? uuid : "");

            boolean recalled = false;
            if (uuid != null && Recalled.isKept(uuid)) {
                recalled = true;
            }
            if (!recalled) {
                try {
                    Method mRecall = cls.getMethod("isRecalled");
                    recalled = Boolean.TRUE.equals(mRecall.invoke(msgObj));
                } catch (Throwable ignored) {}
            }

            boolean deleted = false;
            try {
                Method mDel = cls.getMethod("isDeleted");
                deleted = Boolean.TRUE.equals(mDel.invoke(msgObj));
            } catch (Throwable ignored) {}

            if (!recalled && !deleted) {
                if (cur != null && cur.length() > 0 && key.length() > 1) {
                    save(key, cur);
                    if (uuid != null) save(uuid, cur);
                }
                return cur;
            }

            String saved = load(key);
            if (saved == null && uuid != null) saved = load(uuid);
            if (saved != null && saved.length() > 0) {
                return markDeleted(saved);
            }
            if (cur != null && cur.length() > 0) {
                return markDeleted(cur);
            }
            return cur;
        } catch (Throwable error) {
            return null;
        }
    }

    private static String markDeleted(String saved) {
        if (saved == null || saved.length() == 0) return saved;
        try {
            if (saved.startsWith("{") && saved.endsWith("}")) {
                JSONObject json = new JSONObject(saved);
                if (json.has("text")) {
                    String original = json.optString("text", "");
                    if (!original.startsWith("\uD83D\uDDD1 [Удалено]")) {
                        json.put("text", "\uD83D\uDDD1 [Удалено] " + original);
                    }
                    return json.toString();
                }
                if (json.has("a:src_text")) {
                    String original = json.optString("a:src_text", "");
                    if (!original.startsWith("\uD83D\uDDD1 [Удалено]")) {
                        json.put("a:src_text", "\uD83D\uDDD1 [Удалено] " + original);
                    }
                    return json.toString();
                }
            }
        } catch (Throwable ignored) {}
        if (!saved.startsWith("\uD83D\uDDD1 [Удалено]")) {
            return "\uD83D\uDDD1 [Удалено] " + saved;
        }
        return saved;
    }
}

package com.fasdeq.airtok;

import android.widget.TextView;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public final class Looks {

    static final int MOST = 64;

    private Looks() {}

    private static volatile Map<String, int[]> gradients = new HashMap<String, int[]>();

    static void learn(JSONObject root) {}

    public static int[] of(String uid) {
        return null;
    }

    public static synchronized String mark(String uid) {
        return "";
    }

    public static boolean isMark(char c) {
        return false;
    }

    public static int[] colours(char c) {
        return null;
    }

    public static void paint(TextView view, int[] colours) {}

    public static void paint(TextView view, int[] colours, CharSequence text) {}

    public static void clear(TextView view) {
        if (view == null) return;
        try {
            view.getPaint().setShader(null);
        } catch (Throwable ignored) {}
    }
}

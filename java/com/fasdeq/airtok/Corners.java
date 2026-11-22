package com.fasdeq.airtok;

import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;

public final class Corners {

    private Corners() {}

    public static final String KEY = "round_cards";

    private static final float RADIUS = 8f;

    private static volatile Boolean cached;

    static void reloadSettings() {
        cached = null;
    }

    public static boolean isEnabled() {

        return !Version.LITE;
    }

    public static void setEnabled(boolean enabled) {
        cached = enabled;
        try {
            android.content.Context context = AirTok.context();
            if (context == null) return;
            context.getSharedPreferences("margyt", android.content.Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY, enabled).apply();
        } catch (Throwable ignored) {
        }
    }

    private static int cover;

    private static final ViewOutlineProvider ROUND = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            int wide = view.getWidth();
            int tall = view.getHeight();
            float radius = 0f;
            try {
                if (isEnabled() && holdsCover(view)) {
                    radius = RADIUS * view.getResources().getDisplayMetrics().density;
                }
            } catch (Throwable ignored) {
            }
            outline.setRoundRect(0, 0, wide, tall, radius);
        }
    };

    private static boolean holdsCover(View cell) {
        if (cover == 0) {
            cover = cell.getResources().getIdentifier("cover", "id",
                    cell.getContext().getPackageName());
            if (cover == 0) cover = -1;
        }
        return cover > 0 && cell.findViewById(cover) != null;
    }

    public static void cell(View cell) {
        try {

            if (cell == null || cell.getOutlineProvider() == ROUND) return;
            cell.setOutlineProvider(ROUND);
            cell.setClipToOutline(true);

            cell.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View view, int l, int t, int r, int b,
                                           int ol, int ot, int or, int ob) {
                    view.invalidateOutline();
                }
            });
        } catch (Throwable error) {
            Diary.note("corners: " + error);
        }
    }
}

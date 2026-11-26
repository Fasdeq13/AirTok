package com.fasdeq.airtok;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

final class ShareFolders {

    private ShareFolders() {}

    private static int actionLabelId, actionIconId, actionRowId, appLabelId;
    private static final int TAG_ACTIONS = 0x7e0c0433, TAG_PAINTED = 0x7e0c0434;

    private static void ids(Context context) {
        if (actionLabelId != 0) return;
        String pack = context.getPackageName();
        actionLabelId = context.getResources().getIdentifier("w51", "id", pack);
        actionIconId = context.getResources().getIdentifier("w4y", "id", pack);
        actionRowId = context.getResources().getIdentifier("a5x", "id", pack);
        appLabelId = context.getResources().getIdentifier("w5k", "id", pack);
    }

    static void named(final TextView view) {
        if (Version.LITE || view == null) return;
        int id = view.getId();
        if (id == View.NO_ID) return;
        if (actionLabelId == 0) ids(view.getContext());
        if (id == actionLabelId && actionLabelId != 0) {
            actions(view);
        }
    }

    private static void actions(final TextView label) {
        label.post(new Runnable() {
            @Override public void run() {
                try {
                    final ViewGroup row = rowAbove(label, actionRowId);
                    if (row == null || row.getTag(TAG_ACTIONS) != null) return;
                    row.setTag(TAG_ACTIONS, Boolean.TRUE);
                    final ViewTreeObserver.OnPreDrawListener keep = new ViewTreeObserver.OnPreDrawListener() {
                        @Override public boolean onPreDraw() {
                            try {
                                paint(row);
                            } catch (Throwable ignored) {
                            }
                            return true;
                        }
                    };
                    row.getViewTreeObserver().addOnPreDrawListener(keep);
                    row.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                        @Override public void onViewAttachedToWindow(View v) {}

                        @Override public void onViewDetachedFromWindow(View v) {
                            v.getViewTreeObserver().removeOnPreDrawListener(keep);
                            v.removeOnAttachStateChangeListener(this);
                            v.setTag(TAG_ACTIONS, null);
                        }
                    });
                    paint(row);
                } catch (Throwable error) {
                    Diary.detail("share actions: " + error);
                }
            }
        });
    }

    private static ViewGroup rowAbove(View view, int id) {
        Object up = view.getParent();
        for (int i = 0; i < 6 && up instanceof View; i++) {
            View candidate = (View) up;
            if (candidate.getId() == id && candidate instanceof ViewGroup) return (ViewGroup) candidate;
            up = candidate.getParent();
        }
        return null;
    }

    private static void paint(ViewGroup row) {
        int ink = 0;
        View apps = appLabelId == 0 ? null : row.getRootView().findViewById(appLabelId);
        if (apps instanceof TextView) ink = ((TextView) apps).getCurrentTextColor();
        if ((ink >>> 24) < 0x80) ink = Themes.text();
        if ((ink >>> 24) < 0x80) return;
        for (int i = 0; i < row.getChildCount(); i++) {
            View cell = row.getChildAt(i);
            View label = cell.findViewById(actionLabelId);
            boolean broken = label instanceof TextView && (((TextView) label).getCurrentTextColor() >>> 24) < 0x40;
            if (broken) ((TextView) label).setTextColor(ink);
            if (broken || cell.getTag(TAG_PAINTED) != null) {
                View icon = cell.findViewById(actionIconId);
                if (icon instanceof android.widget.ImageView) {
                    Object painted = cell.getTag(TAG_PAINTED);
                    android.widget.ImageView picture = (android.widget.ImageView) icon;
                    if (!(painted instanceof Integer) || (Integer) painted != ink || picture.getColorFilter() == null) {
                        picture.setColorFilter(ink, android.graphics.PorterDuff.Mode.SRC_IN);
                        cell.setTag(TAG_PAINTED, ink);
                    }
                }
            }
        }
    }
}

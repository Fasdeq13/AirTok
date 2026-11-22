package com.fasdeq.airtok;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

final class ProfileVerdict {

    private ProfileVerdict() {}

    private static final int PLATE = 0x7e0c0337;
    private static final Map<View, WeakReference<Object>> owners = new WeakHashMap<>();
    private static final Map<Class<?>, Method[]> roads = new java.util.HashMap<>();

    static void bind(Object component, View view) {

        View block = column(component, view);
        if (block == null) return;
        synchronized (owners) { owners.put(block, new WeakReference<>(component)); }
        if (block != view && !Boolean.TRUE.equals(block.getTag(WATCHED)))
            { block.setTag(WATCHED, Boolean.TRUE); block.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> apply(v)); }
        apply(block);
    }

    private static final int WATCHED = 0x7e0c0339, PENDING = 0x7e0c033b;

    private static boolean vertical(View view) {
        return view instanceof LinearLayout && ((LinearLayout) view).getOrientation() == LinearLayout.VERTICAL;
    }

    private static View column(Object component, View view) {

        View header = named(view, "t7l", 0);
        if (vertical(header)) return header;
        for (Class<?> step = component.getClass(); step != null && step != Object.class; step = step.getSuperclass()) {
            if (!step.getName().endsWith("HeaderBaseContainerComponent")) continue;
            for (java.lang.reflect.Field field : step.getDeclaredFields()) {
                if (field.getType() != ViewGroup.class) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(component);
                    if (vertical((View) value)) return (View) value;
                } catch (Throwable ignored) {}
            }
        }
        if (vertical(view)) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (vertical(group.getChildAt(i))) return group.getChildAt(i);
        }
        return null;
    }

    static boolean plate(View view) { return view != null && (view.getTag(PLATE) != null || Frames.isPlate(view)); }

    static void apply(View view) {
        try {
            WeakReference<Object> held;
            synchronized (owners) { held = owners.get(view); }
            Object component = held == null ? null : held.get();
            if (component == null) return;
            LinearLayout block = (LinearLayout) view;
            String uid = uid(component);
            Badges.Verdict verdict = Badges.verdict(uid);
            ground = Md3.behind(block);
            Frames.profile(block, uid, ink());
            View plate = find(block);
            if (plate != null) recolour(plate);
            boolean marking = uid != null && AdminPanel.owner() && !uid.equals(Account.id());
            if (verdict == null && !marking) {
                if (plate != null) drop(block, plate);
                return;
            }
            String key = verdict == null ? "mark" : (verdict.good ? "1" : "0") + verdict.reason + (marking ? "+" : "");
            if (plate != null && !key.equals(plate.getTag(PLATE))) { drop(block, plate); plate = null; }
            if (plate != null) return;
            plate = verdict == null ? chip(block.getContext()) : make(block.getContext(), verdict);
            plate.setTag(PLATE, key);
            if (marking) {
                final String target = uid;
                plate.setOnClickListener(v -> AdminPanel.open(v.getContext(), target, block));
            }
            LinearLayout.LayoutParams where = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int side = dp(block.getContext(), 16);
            where.setMargins(side, dp(block.getContext(), 10), side, dp(block.getContext(), 4));

            final View made = plate;
            block.setTag(PENDING, made);

            block.post(() -> {
                if (block.getTag(PENDING) == made) block.setTag(PENDING, null);
                if (made.getParent() == null && find(block) == null) block.addView(made, Math.min(1, block.getChildCount()), where);
            });
        } catch (Throwable error) {
            Diary.note("profile verdict: " + error);
        }
    }

    private static View named(View view, String name, int depth) {
        if (view == null || depth > 12) return null;
        if (name.equals(Names.of(view))) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = named(group.getChildAt(i), name, depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void drop(ViewGroup block, View plate) {
        if (block.getTag(PENDING) == plate) block.setTag(PENDING, null);
        if (plate.getParent() == block) block.removeView(plate);
    }

    private static View find(ViewGroup block) {
        Object pending = block.getTag(PENDING);
        if (pending instanceof View) return (View) pending;

        for (int i = 0; i < block.getChildCount(); i++) if (block.getChildAt(i).getTag(PLATE) != null) return block.getChildAt(i);
        return null;
    }

    private static String uid(Object component) {
        Method[] road;
        synchronized (roads) { road = roads.get(component.getClass()); }
        try {
            if (road == null) {
                Method model = returning(component.getClass(), "com.ss.android.ugc.profile.platform.base.viewmodel.ProfilePlatformViewModel");
                Method user = model == null ? null : returning(model.getReturnType(), "com.ss.android.ugc.aweme.profile.model.User");
                road = new Method[]{model, user};
                synchronized (roads) { roads.put(component.getClass(), road); }
            }
            if (road[0] == null || road[1] == null) return null;
            Object vm = road[0].invoke(component);
            Object user = vm == null ? null : road[1].invoke(vm);
            Object uid = user == null ? null : user.getClass().getMethod("getUid").invoke(user);
            return uid instanceof String && ((String) uid).length() > 0 ? (String) uid : null;
        } catch (Throwable error) {
            return null;
        }
    }

    private static Method returning(Class<?> type, String name) {
        for (Class<?> step = type; step != null && step != Object.class; step = step.getSuperclass()) {
            for (Method method : step.getDeclaredMethods()) {
                if (method.getParameterTypes().length != 0 || !method.getReturnType().getName().equals(name)) continue;
                if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) continue;
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static View chip(Context c) {
        TextView chip = new TextView(c);
        chip.setText(Text.ADMIN);
        chip.setTextColor(ink());
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        chip.setPadding(dp(c, 14), dp(c, 8), dp(c, 14), dp(c, 8));
        GradientDrawable back = new GradientDrawable();
        back.setCornerRadius(dp(c, 16));
        back.setStroke(Math.max(1, dp(c, 1)), (ink() & 0xFFFFFF) | 0x44000000);
        chip.setBackground(back);
        Fonts.applyTree(chip);
        return chip;
    }

    private static int ground = 0xFF121212;

    private static int ink() {
        return Md3.ink(ground);
    }

    private static void recolour(View plate) {
        int ink = ink();
        if (plate instanceof TextView) ((TextView) plate).setTextColor(ink);
        if (plate instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) plate;
            for (int i = 0; i < group.getChildCount(); i++) if (group.getChildAt(i) instanceof TextView) ((TextView) group.getChildAt(i)).setTextColor(ink);
        }
        if (plate.getBackground() instanceof GradientDrawable && plate instanceof TextView)
            ((GradientDrawable) plate.getBackground()).setStroke(Math.max(1, dp(plate.getContext(), 1)), (ink & 0xFFFFFF) | 0x44000000);
    }

    private static View make(Context c, Badges.Verdict verdict) {
        int tone = verdict.good ? 0xFF2EAD5B : 0xFFE5484D;
        LinearLayout plate = new LinearLayout(c);
        plate.setOrientation(LinearLayout.VERTICAL);
        plate.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 11));
        GradientDrawable back = new GradientDrawable();
        back.setCornerRadius(dp(c, 16));
        back.setColor((tone & 0xFFFFFF) | 0x24000000);
        back.setStroke(Math.max(1, dp(c, 1)), (tone & 0xFFFFFF) | 0x66000000);
        plate.setBackground(back);

        TextView title = new TextView(c);
        title.setText(verdict.good ? Text.VERDICT_GOOD : Text.VERDICT_BAD);
        title.setTextColor(ink());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        plate.addView(title);

        TextView body = new TextView(c);
        String reason = verdict.reason == null ? "" : verdict.reason.trim();
        body.setText(Text.verdictWhy(verdict.good, reason));
        body.setTextColor(ink());
        body.setAlpha(0.8f);
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        body.setLineSpacing(0, 1.1f);
        LinearLayout.LayoutParams under = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        under.topMargin = dp(c, 3);
        plate.addView(body, under);
        plate.setContentDescription(title.getText() + ". " + body.getText());
        Fonts.applyTree(plate);
        return plate;
    }

    private static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
}

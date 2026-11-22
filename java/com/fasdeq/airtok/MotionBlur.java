package com.fasdeq.airtok;

import android.app.Activity;
import android.content.Context;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.WeakHashMap;

public final class MotionBlur {

    private MotionBlur() {}

    static final String KEY = "motion_blur", STRENGTH = "motion_blur_strength";
    private static final int TAG = 0x7e0d0442;

    private static Boolean on;
    private static int strength = -1;

    static boolean on() {
        if (on == null) {
            Context c = AirTok.context();
            if (c == null) return false;
            on = c.getSharedPreferences("margyt", Context.MODE_PRIVATE).getBoolean(KEY, false);
        }
        return on && android.os.Build.VERSION.SDK_INT >= 31;
    }

    static void set(boolean value) {
        on = value;
        Context c = AirTok.context();
        if (c != null) c.getSharedPreferences("margyt", Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply();
    }

    static int strength() {
        if (strength < 0) {
            Context c = AirTok.context();
            strength = c == null ? 50 : c.getSharedPreferences("margyt", Context.MODE_PRIVATE).getInt(STRENGTH, 50);
        }
        return strength;
    }

    static void setStrength(int value) {
        strength = Math.max(0, Math.min(100, value));
        Context c = AirTok.context();
        if (c != null) c.getSharedPreferences("margyt", Context.MODE_PRIVATE).edit().putInt(STRENGTH, strength).apply();
    }

    public static boolean textureVideo() {
        try {
            return on();
        } catch (Throwable ignored) {
            return false;
        }
    }

    static void attach(Activity activity) {
        if (!on()) return;
        try {
            View decor = activity.getWindow().getDecorView();
            if (decor.getTag(TAG) instanceof Watcher) return;
            Watcher watcher = new Watcher(decor);
            decor.setTag(TAG, watcher);
            ViewTreeObserver tree = decor.getViewTreeObserver();
            tree.addOnGlobalLayoutListener(watcher);
            tree.addOnPreDrawListener(watcher);
        } catch (Throwable error) {
            Diary.note("motion blur: " + error);
        }
    }

    private static Method vertical, horizontal;

    private static int offset(View view, boolean down) {
        try {
            if (vertical == null) {
                vertical = View.class.getDeclaredMethod("computeVerticalScrollOffset");
                vertical.setAccessible(true);
                horizontal = View.class.getDeclaredMethod("computeHorizontalScrollOffset");
                horizontal.setAccessible(true);
            }
            return (Integer) (down ? vertical : horizontal).invoke(view);
        } catch (Throwable error) {
            return down ? view.getScrollY() : view.getScrollX();
        }
    }

    private static final class Mover {
        int x, y;
        float blurX, blurY, shownX = -1, shownY = -1;
        boolean known;
    }

    private static final class Watcher implements ViewTreeObserver.OnGlobalLayoutListener, ViewTreeObserver.OnPreDrawListener {
        final java.lang.ref.WeakReference<View> root;
        final WeakHashMap<View, Mover> movers = new WeakHashMap<>();
        long found;

        Watcher(View root) {
            this.root = new java.lang.ref.WeakReference<>(root);
        }

        @Override
        public void onGlobalLayout() {
            long now = android.os.SystemClock.uptimeMillis();
            if (now - found < 700L) return;
            found = now;
            View top = root.get();
            if (top != null) find(top, new int[] {20000});
        }

        private void find(View view, int[] budget) {
            if (budget[0]-- <= 0 || view.getVisibility() != View.VISIBLE) return;
            if (scrolls(view) && !movers.containsKey(view)) movers.put(view, new Mover());
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) find(group.getChildAt(i), budget);
            }
        }

        private boolean scrolls(View view) {
            if (view instanceof android.widget.ScrollView || view instanceof android.widget.HorizontalScrollView
                    || view instanceof android.widget.AbsListView) return true;
            for (Class<?> c = view.getClass(); c != null && c != View.class; c = c.getSuperclass()) {
                String name = c.getName();
                if (name.equals("androidx.recyclerview.widget.RecyclerView") || name.equals("androidx.viewpager.widget.ViewPager")
                        || name.endsWith(".VerticalViewPager") || name.equals("androidx.core.widget.NestedScrollView")) return true;
            }
            return false;
        }

        @Override
        public boolean onPreDraw() {
            if (!on()) {
                for (View view : new ArrayList<>(movers.keySet())) if (view != null) view.setRenderEffect(null);
                movers.clear();
                return true;
            }
            float density = 1f;
            View top = root.get();
            if (top != null) density = top.getResources().getDisplayMetrics().density;

            float gain = 0.15f + 0.45f * strength() / 100f, most = 28f * density;
            for (java.util.Map.Entry<View, Mover> entry : new ArrayList<>(movers.entrySet())) {
                View view = entry.getKey();
                Mover m = entry.getValue();
                if (view == null) continue;
                if (!view.isAttachedToWindow() || !view.isShown()) { m.known = false; clear(view, m); continue; }
                int x = offset(view, false), y = offset(view, true);
                if (!m.known) { m.x = x; m.y = y; m.known = true; continue; }
                float dx = Math.abs(x - m.x), dy = Math.abs(y - m.y);
                m.x = x; m.y = y;

                m.blurX = Math.max(Math.min(most, dx * gain), m.blurX * 0.72f);
                m.blurY = Math.max(Math.min(most, dy * gain), m.blurY * 0.72f);
                if (m.blurX < 0.6f) m.blurX = 0;
                if (m.blurY < 0.6f) m.blurY = 0;
                if (Math.abs(m.blurX - m.shownX) < 0.4f && Math.abs(m.blurY - m.shownY) < 0.4f) continue;
                m.shownX = m.blurX;
                m.shownY = m.blurY;
                if (m.blurX == 0 && m.blurY == 0) view.setRenderEffect(null);
                else view.setRenderEffect(RenderEffect.createBlurEffect(Math.max(0.01f, m.blurX), Math.max(0.01f, m.blurY), Shader.TileMode.CLAMP));

                if (m.blurX > 0 || m.blurY > 0) view.postInvalidateOnAnimation();
            }
            return true;
        }

        private void clear(View view, Mover m) {
            if (m.shownX != 0 || m.shownY != 0) {
                view.setRenderEffect(null);
                m.shownX = m.shownY = 0;
                m.blurX = m.blurY = 0;
            }
        }
    }
}

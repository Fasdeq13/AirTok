package cat.narezany.margyt;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.util.Map;
import java.util.WeakHashMap;

final class SimpleFeed {

    private SimpleFeed() {}

    static final String KEY = "simple_feed";
    private static final float RAIL = 0.84f, CAPTION = 0.88f;
    private static final int WATCHED = 0x7e0c033a;

    private static volatile Boolean on;
    private static final Map<View, Integer> hidden = new WeakHashMap<>();
    private static final Map<View, float[]> shrunk = new WeakHashMap<>();
    private static final Map<TextView, float[]> written = new WeakHashMap<>();
    private static long last;

    static boolean isOn() {
        Boolean known = on;
        if (known != null) return known;
        Context c = Margy.context();
        if (c == null) return false;
        known = c.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false);
        on = known;
        return known;
    }

    static void set(Context context, boolean value) {
        on = value;
        context.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply();
        if (!value) restore();
    }

    static void attach(Activity activity) {
        if (activity == null || !activity.getClass().getName().equals("com.ss.android.ugc.aweme.main.MainActivity")) return;
        final View root = activity.getWindow().getDecorView();
        if (Boolean.TRUE.equals(root.getTag(WATCHED))) return;
        root.setTag(WATCHED, Boolean.TRUE);
        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                if (!isOn()) return;
                long now = android.os.SystemClock.uptimeMillis();
                if (now - last < 120) { root.removeCallbacks(again); root.postDelayed(again, 130); return; }
                last = now;
                pass(root);
            }
            private final Runnable again = () -> { if (isOn()) { last = android.os.SystemClock.uptimeMillis(); pass(root); } };
        });
        if (isOn()) root.post(() -> pass(root));
    }

    private static void pass(View root) {
        long probe = Probe.start();
        try {
            walk(root, 0, new int[]{4000});
        } catch (Throwable error) {
            Diary.note("simple feed: " + error);
        } finally {
            Probe.stop(Probe.DIM_PASS, probe);
        }
    }

    private static void walk(View view, int depth, int[] budget) {
        if (view == null || depth > 48 || budget[0]-- <= 0) return;
        if (view.getVisibility() == View.GONE && !hidden.containsKey(view)) return;
        String name = Names.of(view);
        if (name.length() > 0) {
            switch (name) {
                case "uvh": top(view); return;              
                case "title_shadow": hide(view, View.INVISIBLE); return;
                case "i_a": hide(up(view, 2), View.GONE); return;      
                case "o6r": hide(up(view, 1), View.GONE); return;      
                case "pnv": hide(up(view, 1), View.INVISIBLE); return; 
                case "videomusiccoverblock": hide(view, View.INVISIBLE); return;
                case "desc": caption(view); return;
                case "lje": shrink(view, RAIL, 1f, 1f); break;         
                case "bqx": shrink(view, CAPTION, 0f, 1f); break;      
                default: break;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) walk(group.getChildAt(i), depth + 1, budget);
        }
    }

    private static void top(View row) {
        if (!(row instanceof ViewGroup)) { hide(row, View.INVISIBLE); return; }
        ViewGroup group = (ViewGroup) row;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            String name = Names.of(child);

            if ("k_5".equals(name) || "k_2".equals(name) && Dim.isConfigured()) { show(child); continue; }
            hide(child, View.INVISIBLE);
        }
    }

    private static View up(View view, int steps) {
        View at = view;
        for (int i = 0; i < steps && at != null; i++) at = at.getParent() instanceof View ? (View) at.getParent() : null;
        return at;
    }

    private static void hide(View view, int how) {
        if (view == null) return;
        synchronized (hidden) { if (!hidden.containsKey(view)) hidden.put(view, view.getVisibility()); }
        if (view.getVisibility() != how) view.setVisibility(how);
    }

    private static void show(View view) {
        Integer was;
        synchronized (hidden) { was = hidden.remove(view); }
        if (was != null && view.getVisibility() != was) view.setVisibility(was);
    }

    private static void shrink(View view, float scale, float pivotX, float pivotY) {
        if (view.getWidth() <= 0 || view.getHeight() <= 0) return;
        synchronized (shrunk) {
            if (!shrunk.containsKey(view)) shrunk.put(view, new float[]{view.getScaleX(), view.getScaleY(), view.getPivotX(), view.getPivotY()});
        }
        float px = view.getWidth() * pivotX, py = view.getHeight() * pivotY;
        if (view.getPivotX() != px) view.setPivotX(px);
        if (view.getPivotY() != py) view.setPivotY(py);
        if (view.getScaleX() != scale) view.setScaleX(scale);
        if (view.getScaleY() != scale) view.setScaleY(scale);
    }

    private static void caption(View view) {
        if (!(view instanceof TextView)) return;
        TextView text = (TextView) view;
        float[] was;
        synchronized (written) {
            was = written.get(text);
            if (was == null) { was = new float[]{text.getTextSize(), text.getMaxLines()}; written.put(text, was); }
        }
        float size = was[0] * 0.9f;
        if (Math.abs(text.getTextSize() - size) > 0.5f && Math.abs(text.getTextSize() - was[0]) > 0.5f) {

            was[0] = text.getTextSize();
            size = was[0] * 0.9f;
        }
        if (Math.abs(text.getTextSize() - size) > 0.5f) text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, size);
        if (text.getMaxLines() > 2) text.setMaxLines(2);
    }

    private static void restore() {
        java.util.List<Map.Entry<View, Integer>> views;
        synchronized (hidden) { views = new java.util.ArrayList<>(hidden.entrySet()); hidden.clear(); }
        for (Map.Entry<View, Integer> entry : views) {
            View view = entry.getKey();
            if (view != null) view.setVisibility(entry.getValue());
        }
        java.util.List<Map.Entry<View, float[]>> sizes;
        synchronized (shrunk) { sizes = new java.util.ArrayList<>(shrunk.entrySet()); shrunk.clear(); }
        for (Map.Entry<View, float[]> entry : sizes) {
            View view = entry.getKey();
            float[] was = entry.getValue();
            if (view == null) continue;
            view.setScaleX(was[0]); view.setScaleY(was[1]); view.setPivotX(was[2]); view.setPivotY(was[3]);
        }
        java.util.List<Map.Entry<TextView, float[]>> texts;
        synchronized (written) { texts = new java.util.ArrayList<>(written.entrySet()); written.clear(); }
        for (Map.Entry<TextView, float[]> entry : texts) {
            TextView text = entry.getKey();
            float[] was = entry.getValue();
            if (text == null) continue;
            text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, was[0]);
            text.setMaxLines((int) was[1]);
        }
    }
}

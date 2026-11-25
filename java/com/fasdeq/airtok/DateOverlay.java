package com.fasdeq.airtok;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;

final class DateOverlay {
    static final int NATIVE_DATE = 0x7f0a93a5;
    private static final int OVERLAY_ID = 0x7e0d0444;
    private static final int INLINE_TAG = 0x7e0d0445;
    private static WeakReference<View> source = new WeakReference<>(null), watched = new WeakReference<>(null);
    private static WeakReference<TextView> label = new WeakReference<>(null);
    private static WeakReference<TextView> authorView = new WeakReference<>(null);
    private static ViewTreeObserver.OnPreDrawListener observer;
    private static WeakReference<ViewTreeObserver> tree = new WeakReference<>(null);
    private static String date;
    private static String authorName;
    private static CharSequence original;
    private static float alpha;
    private static int visibility;
    private static boolean applied, tapped, wasClickable;
    private static boolean inlinedInAuthor;
    private static long searched, chosenAt;
    private static final java.util.LinkedHashMap<WeakReference<View>, String> candidates = new java.util.LinkedHashMap<>();
    private static WeakReference<ViewGroup> fallbackParent = new WeakReference<>(null);
    private static WeakReference<TextView> fallback = new WeakReference<>(null);

    static void register(View view, String text) {
        if (view == null || text == null) return;
        java.util.Iterator<WeakReference<View>> it = candidates.keySet().iterator();
        while (it.hasNext()) {
            View old = it.next().get();
            if (old == null || old == view) it.remove();
        }
        candidates.put(new WeakReference<>(view), text);
        while (candidates.size() > 32) candidates.remove(candidates.keySet().iterator().next());
        Rect rect = new Rect();
        if (view.isShown() && view.getGlobalVisibleRect(rect)) {
            bind(view, text);
        } else if (source.get() == null) {
            source = new WeakReference<>(view);
            date = text;
            resume(view.getRootView());
        }
    }

    private static void chooseVisible() {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - chosenAt < 250) return;
        chosenAt = now;
        View root = watched.get();
        if (root == null) return;
        Rect rect = new Rect();
        View best = null;
        String text = null;
        int area = 0;
        for (java.util.Map.Entry<WeakReference<View>, String> entry : candidates.entrySet()) {
            View v = entry.getKey().get();
            if (v == null || v.getRootView() != root || !v.isShown() || !v.isAttachedToWindow() || !v.getGlobalVisibleRect(rect))
                continue;
            int size = rect.width() * rect.height();
            if (size > area) {
                area = size;
                best = v;
                text = entry.getValue();
            }
        }
        if (best != null && best != source.get()) {
            TextView old = label.get();
            if (old != null) restore(old);
            removeFallback();
            restoreInline();
            label = new WeakReference<>(null);
            authorView = new WeakReference<>(null);
            source = new WeakReference<>(best);
            date = text;
            searched = 0;
        }
    }

    private static void removeFallback() {
        ViewGroup group = fallbackParent.get();
        TextView fb = fallback.get();
        if (group != null && fb != null) {
            try {
                group.removeView(fb);
            } catch (Throwable ignored) {}
        }
        fallback = new WeakReference<>(null);
        fallbackParent = new WeakReference<>(null);
    }

    private static void restoreInline() {
        if (!inlinedInAuthor) return;
        TextView aTv = authorView.get();
        if (aTv != null && aTv.getTag(INLINE_TAG) != null) {
            CharSequence saved = (CharSequence) aTv.getTag(INLINE_TAG);
            aTv.setText(saved);
            aTv.setTag(INLINE_TAG, null);
        }
        inlinedInAuthor = false;
    }

    private static TextView findAuthorTextView(View root, String author) {
        if (root == null) return null;
        if (root instanceof TextView && root.getId() != OVERLAY_ID && root.getId() != NATIVE_DATE) {
            TextView tv = (TextView) root;
            CharSequence cs = tv.getText();
            if (cs != null && tv.isShown() && tv.getWidth() > 0) {
                String s = cs.toString().trim();
                if (author != null && author.length() > 0) {
                    if (s.equals(author) || s.startsWith(author + " ") || s.startsWith(author + "\u2009")) {
                        return tv;
                    }
                }
                String resName = Names.of(tv);
                if (resName != null && resName.length() > 0) {
                    String lower = resName.toLowerCase(java.util.Locale.ROOT);
                    if ((lower.contains("title") || lower.contains("author") || lower.contains("name"))
                            && !lower.contains("desc") && s.length() > 0 && s.length() < 60) {
                        return tv;
                    }
                }
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                TextView found = findAuthorTextView(vg.getChildAt(i), author);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static TextView findSiblingDateSlot(TextView authorTv) {
        if (authorTv == null) return null;
        android.view.ViewParent p = authorTv.getParent();
        if (!(p instanceof ViewGroup)) return null;
        ViewGroup vg = (ViewGroup) p;
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            if (child == authorTv || !(child instanceof TextView) || child.getId() == OVERLAY_ID) continue;
            TextView tv = (TextView) child;
            String resName = Names.of(tv);
            if (resName != null) {
                String lower = resName.toLowerCase(java.util.Locale.ROOT);
                if (lower.contains("date") || lower.contains("time") || lower.contains("create")) {
                    return tv;
                }
            }
            CharSequence t = tv.getText();
            if (t == null || t.length() == 0 || t.toString().contains("·")) {
                if (tv.getVisibility() == View.GONE || tv.getVisibility() == View.INVISIBLE) {
                    return tv;
                }
            }
        }
        return null;
    }

    private static boolean tryInlineInAuthor(TextView aTv, String dateText) {
        if (aTv == null || dateText == null) return false;
        try {
            CharSequence currentText = aTv.getText();
            if (currentText == null) return false;
            String current = currentText.toString();
            String suffix = " · " + dateText;
            if (current.endsWith(suffix)) return true;
            if (aTv.getTag(INLINE_TAG) == null) {
                aTv.setTag(INLINE_TAG, currentText);
            }
            android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder(currentText);
            int oldSuffix = current.indexOf(" · ");
            if (oldSuffix >= 0 && aTv.getTag(INLINE_TAG) != null) {
                CharSequence saved = (CharSequence) aTv.getTag(INLINE_TAG);
                sb = new android.text.SpannableStringBuilder(saved);
            }
            int start = sb.length();
            sb.append(suffix);
            sb.setSpan(new android.text.style.ForegroundColorSpan(0xB3FFFFFF), start, sb.length(),
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new android.text.style.RelativeSizeSpan(0.85f), start, sb.length(),
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            aTv.setText(sb);
            inlinedInAuthor = true;
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static TextView fallback(View cell) {
        if (!(cell instanceof ViewGroup)) return null;
        if (fallback.get() != null && fallbackParent.get() != null) return fallback.get();
        removeFallback();
        restoreInline();

        TextView aTv = findAuthorTextView(cell, authorName);
        if (aTv == null && cell.getParent() instanceof ViewGroup) {
            View searchRoot = cell;
            for (int i = 0; i < 4 && searchRoot.getParent() instanceof View; i++) {
                View p = (View) searchRoot.getParent();
                if (p.getClass().getName().contains("ViewPager") || p.getClass().getName().contains("RecyclerView")) break;
                searchRoot = p;
            }
            aTv = findAuthorTextView(searchRoot, authorName);
        }
        authorView = new WeakReference<>(aTv);

        if (aTv != null) {
            TextView sibling = findSiblingDateSlot(aTv);
            if (sibling != null) {
                return sibling;
            }
            android.view.ViewParent ap = aTv.getParent();
            if (ap instanceof LinearLayout && ((LinearLayout) ap).getOrientation() == LinearLayout.HORIZONTAL) {
                LinearLayout ll = (LinearLayout) ap;
                float dp = ll.getResources().getDisplayMetrics().density;
                TextView view = new TextView(ll.getContext());
                view.setId(OVERLAY_ID);
                view.setTextSize(13);
                view.setTextColor(0xFFFFFFFF);
                view.setShadowLayer(2, 0, 1, 0xAA000000);
                view.setSingleLine(true);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.leftMargin = Math.round(6 * dp);
                lp.gravity = android.view.Gravity.CENTER_VERTICAL;
                int idx = ll.indexOfChild(aTv);
                ll.addView(view, idx >= 0 ? idx + 1 : ll.getChildCount(), lp);
                fallback = new WeakReference<>(view);
                fallbackParent = new WeakReference<>(ll);
                return view;
            }

            if (tryInlineInAuthor(aTv, date)) {
                return null;
            }
        }

        return null;
    }

    static void bind(View view, String text) {
        bind(view, text, null);
    }

    static void bind(View view, String text, String author) {
        detach();
        source = new WeakReference<>(view);
        date = text;
        if (author != null) authorName = author;
        searched = 0;
        if (view == null || text == null) return;
        view.post(() -> {
            if (source.get() == view) resume(view.getRootView());
        });
    }

    static void resume(View root) {
        View cell = source.get();
        if (cell == null || date == null || cell.getRootView() != root) return;
        ViewTreeObserver current = root.getViewTreeObserver();
        if (current != tree.get() || observer == null) {
            ViewTreeObserver old = tree.get();
            if (old != null && old.isAlive() && observer != null) old.removeOnPreDrawListener(observer);
            watched = new WeakReference<>(root);
            tree = new WeakReference<>(current);
            observer = () -> {
                long probe = Probe.start();
                update();
                Probe.stop(Probe.DATE_FRAME, probe);
                return true;
            };
            current.addOnPreDrawListener(observer);
        }
        TextView found = find(cell), row = label.get();
        if (found != row) {
            if (row != null) restore(row);
            label = new WeakReference<>(null);
            searched = 0;
        }
        update();
    }

    private static TextView find(View view) {
        for (int depth = 0; view != null && depth < 20; depth++) {
            View found = view.findViewById(NATIVE_DATE);
            if (found instanceof TextView) return (TextView) found;
            Object parent = view.getParent();
            if (!(parent instanceof View)) break;
            String type = parent.getClass().getName();
            if (type.contains("RecyclerView") || type.contains("ViewPager")) break;
            view = (View) parent;
        }
        return null;
    }

    private static long panelAsked;
    private static boolean panelWas;
    private static WeakReference<View> panelRoot = new WeakReference<>(null);

    private static boolean panelOpen(View root) {
        long now = android.os.SystemClock.uptimeMillis();
        if (panelRoot.get() == root && now - panelAsked < 250L) return panelWas;
        panelAsked = now;
        panelRoot = new WeakReference<>(root);
        panelWas = Dim.panelOpen(root);
        return panelWas;
    }

    static void update() {
        if (!Dates.isEnabled()) {
            TextView old = label.get();
            if (old != null) restore(old);
            if (fallback.get() != null) {
                removeFallback();
                label = new WeakReference<>(null);
            }
            restoreInline();
            return;
        }
        chooseVisible();
        View cell = source.get();
        if (cell == null || date == null) return;
        TextView row = label.get();
        if (row != null && (!row.isAttachedToWindow() || row.getRootView() != cell.getRootView())) {
            restore(row);
            label = new WeakReference<>(null);
            row = null;
            searched = 0;
        }
        if (row == null && !inlinedInAuthor) {
            long now = android.os.SystemClock.uptimeMillis();
            if (now - searched < 250) return;
            searched = now;
            row = find(cell);
            if (row == null) row = fallback(cell);
            if (row == null && inlinedInAuthor) {
                refreshInline();
                return;
            }
            if (row == null) return;
            label = new WeakReference<>(row);
            original = row.getText();
            alpha = row.getAlpha();
            visibility = row.getVisibility();
            wasClickable = row.isClickable();
            Fonts.apply(row);
        }

        if (inlinedInAuthor) {
            refreshInline();
            return;
        }

        Object parent = row.getParent();
        View anchor = parent instanceof View ? (View) parent : cell;
        boolean enabled = Dates.isEnabled();
        boolean show = enabled && anchor.isShown() && anchor.isAttachedToWindow() && !panelOpen(cell.getRootView());
        if (!enabled) {
            restore(row);
            return;
        }
        String shownText = "· " + date;
        CharSequence current = row.getText();
        if (applied && current != null && !shownText.contentEquals(current)) original = current;
        if (row.getText() == null || !shownText.contentEquals(row.getText())) row.setText(shownText);
        if (Math.abs(row.getAlpha() - .7f) > .001f) row.setAlpha(.7f);
        int next = show ? View.VISIBLE : View.GONE;
        if (row.getVisibility() != next) row.setVisibility(next);
        applied = true;

        if (!tapped) {
            row.setOnClickListener(tap -> DateInfo.open(tap, source.get()));
            tapped = true;
        }
    }

    private static void refreshInline() {
        TextView aTv = authorView.get();
        if (aTv == null || !aTv.isAttachedToWindow()) {
            View cell = source.get();
            if (cell != null) {
                aTv = findAuthorTextView(cell, authorName);
                authorView = new WeakReference<>(aTv);
            }
        }
        if (aTv != null && date != null) {
            String suffix = " · " + date;
            CharSequence current = aTv.getText();
            if (current != null && !current.toString().endsWith(suffix)) {
                tryInlineInAuthor(aTv, date);
            }
        }
    }

    private static void restore(TextView row) {
        if (tapped) {
            row.setOnClickListener(null);
            row.setClickable(wasClickable);
            tapped = false;
        }
        if (!applied) return;
        if (date != null && row.getText() != null && (date.contentEquals(row.getText()) || ("· " + date).contentEquals(row.getText()))) {
            row.setText(original);
        }
        row.setAlpha(alpha);
        row.setVisibility(visibility);
        applied = false;
    }

    private static void detach() {
        removeFallback();
        restoreInline();
        TextView row = label.get();
        if (row != null) restore(row);
        ViewTreeObserver root = tree.get();
        if (root != null && observer != null && root.isAlive()) root.removeOnPreDrawListener(observer);
        tree = new WeakReference<>(null);
        observer = null;
        label = new WeakReference<>(null);
        authorView = new WeakReference<>(null);
        watched = new WeakReference<>(null);
    }
}

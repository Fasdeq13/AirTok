package com.fasdeq.airtok;

import android.content.Intent;
import android.net.Uri;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;








final class CommentLinks {

    private CommentLinks() {}

    
    private static final int BLUE = 0xFF2F9BFF;
    private static final int WATCHED = 0x7e0c0391;
    private static volatile java.lang.reflect.Method getListenerInfoMethod;
    private static volatile java.lang.reflect.Field onTouchListenerField;

    



    private static final Pattern ADDRESS = Pattern.compile(
            "(?<![@\\p{L}\\p{N}._-])(?:(?:https?://|www\\.)[^\\s<>\"«»]+"
                    + "|(?:[\\p{L}\\p{N}](?:[\\p{L}\\p{N}-]*[\\p{L}\\p{N}])?\\.)+"
                    + "(?:com|ru|org|net|io|me|xyz|gg|app|dev|tv|co|su|ua|by|kz|uk|de|fr|eu|ai|info|link|site|online|store|club|pro|рф)"
                    + "(?![\\p{L}\\p{N}-])(?::\\d+)?(?:/[^\\s<>\"«»]*)?)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    
    static void bind(Object cell) {
        
        Object body = NativeRead.field(cell, "LLLF");
        if (!(body instanceof TextView)) return;
        final TextView view = (TextView) body;
        if (view.getTag(WATCHED) == null) {
            view.setTag(WATCHED, Boolean.TRUE);
            
            
            
            view.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable text) { mark(text); }
            });
            
            final View.OnTouchListener previous = existingTouchListener(view);
            view.setOnTouchListener((v, event) -> {
                if (touched(v, event)) return true;
                return previous != null && previous.onTouch(v, event);
            });
            
            CharSequence now = view.getText();
            if (now != null && looksLikeIt(now)) view.setText(now);
        }
        CharSequence text = view.getText();
        if (text instanceof Spannable) mark((Spannable) text);
    }

    private static void mark(Spannable spans) {
        try {
            if (spans.length() < 4 || !looksLikeIt(spans)) return;
            if (spans.getSpans(0, spans.length(), Link.class).length > 0) return;
            Matcher found = ADDRESS.matcher(spans);
            while (found.find()) {
                int start = found.start(), end = trimmed(spans, start, found.end());
                if (end <= start) continue;
                
                if (spans.getSpans(start, end, ClickableSpan.class).length > 0) continue;
                spans.setSpan(new Link(spans.subSequence(start, end).toString()), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } catch (Throwable error) {
            Diary.note("comment links: " + error);
        }
    }

    
    private static View.OnTouchListener existingTouchListener(View view) {
        try {
            if (getListenerInfoMethod == null) {
                java.lang.reflect.Method m = View.class.getDeclaredMethod("getListenerInfo");
                m.setAccessible(true);
                getListenerInfoMethod = m;
            }
            Object info = getListenerInfoMethod.invoke(view);
            if (info == null) return null;
            if (onTouchListenerField == null) {
                java.lang.reflect.Field f = info.getClass().getDeclaredField("mOnTouchListener");
                f.setAccessible(true);
                onTouchListenerField = f;
            }
            Object listener = onTouchListenerField.get(info);
            return listener instanceof View.OnTouchListener ? (View.OnTouchListener) listener : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean touched(View v, MotionEvent event) {
        if (!(v instanceof TextView)) return false;
        TextView view = (TextView) v;
        CharSequence text = view.getText();
        Layout layout = view.getLayout();
        if (!(text instanceof Spanned) || layout == null) return false;
        int action = event.getActionMasked();
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_UP) return false;
        int x = (int) event.getX() - view.getTotalPaddingLeft() + view.getScrollX();
        int y = (int) event.getY() - view.getTotalPaddingTop() + view.getScrollY();
        int line = layout.getLineForVertical(y);
        if (x < layout.getLineLeft(line) || x > layout.getLineRight(line)) return false;
        int at = layout.getOffsetForHorizontal(line, x);
        Link[] links = ((Spanned) text).getSpans(at, at, Link.class);
        if (links.length == 0) return false;
        if (action == MotionEvent.ACTION_UP) links[0].onClick(view);
        return true;
    }

    
    private static boolean looksLikeIt(CharSequence text) {
        for (int i = 1, n = text.length() - 1; i < n; i++) {
            char c = text.charAt(i);
            if ((c == '.' || c == ':') && Character.isLetterOrDigit(text.charAt(i - 1))) return true;
        }
        return false;
    }

    
    private static int trimmed(CharSequence text, int start, int end) {
        while (end > start) {
            char last = text.charAt(end - 1);
            if (".,;:!?'…".indexOf(last) >= 0) { end--; continue; }
            if (last == ')' && count(text, start, end, '(') < count(text, start, end, ')')) { end--; continue; }
            break;
        }
        return end;
    }

    private static int count(CharSequence text, int start, int end, char c) {
        int n = 0;
        for (int i = start; i < end; i++) if (text.charAt(i) == c) n++;
        return n;
    }

    private static final class Link extends ClickableSpan {
        private final String address;

        Link(String address) { this.address = address; }

        @Override public void onClick(View view) {
            String lower = address.toLowerCase(java.util.Locale.ROOT);
            String full = lower.startsWith("http://") || lower.startsWith("https://") ? address : "https://" + address;
            try {
                view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(full)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Throwable error) {
                Screen.say(Local.of("Нечем открыть ссылку"));
            }
        }

        @Override public void updateDrawState(TextPaint paint) {
            paint.setColor(BLUE);
            paint.setUnderlineText(false);
        }
    }
}

package com.fasdeq.airtok;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ss.android.ugc.aweme.profile.model.User;
import com.ss.android.ugc.profile.platform.base.data.UserProfileInfo;


public final class Badge {

    private Badge() {}


    private static final int MINT = 0xFF8DD1B0;

    private static volatile Drawable note;
    private static volatile boolean noteTried;



    public static String getNickname(User user) {
        if (user == null) return null;
        String name = user.getNickname();
        if (name != null && !name.trim().startsWith("@") && name.toLowerCase(java.util.Locale.ROOT).contains("fasdeq")) {
            name = name.replaceAll("(?i)(fasdeq)[0-9]+", "$1")
                       .replaceAll("(?i)\\b(fasdeq)1(?![0-9])", "$1");
        }
        try {
            String uid = user.getUid();
            String uniqueId = null;
            try { uniqueId = user.getUniqueId(); } catch (Throwable ignored) {}
            return marked(name, uid, uniqueId, true);
        } catch (Throwable ignored) {
            return name;
        }
    }


    public static String nickname(User user) {
        if (user == null) return null;
        String name = user.nickname;
        if (!Version.LITE) return name;
        try {
            String uid = user.getUid();
            String uniqueId = null;
            try { uniqueId = user.getUniqueId(); } catch (Throwable ignored) {}
            return marked(name, uid, uniqueId, false);
        } catch (Throwable ignored) {
            return name;
        }
    }


    public static String getNickname(UserProfileInfo user) {
        if (user == null) return null;
        String name = user.getNickname();
        if (name != null && !name.trim().startsWith("@") && name.toLowerCase(java.util.Locale.ROOT).contains("fasdeq")) {
            name = name.replaceAll("(?i)(fasdeq)[0-9]+", "$1")
                       .replaceAll("(?i)\\b(fasdeq)1(?![0-9])", "$1");
        }
        try {
            String uid = user.getUid();
            String uniqueId = null;
            try {
                java.lang.reflect.Method m = user.getClass().getMethod("getUniqueId");
                Object r = m.invoke(user);
                if (r instanceof String) uniqueId = (String) r;
            } catch (Throwable ignored) {}
            return marked(name, uid, uniqueId, true);
        } catch (Throwable ignored) {
            return name;
        }
    }


    private static String marked(String name, String uid, boolean fromProfile) {
        return marked(name, uid, null, fromProfile);
    }

    private static String marked(String name, String uid, String uniqueId, boolean fromProfile) {
        if (name == null || name.length() == 0) return name;
        if (!Badges.isEnabled() && !Plugins.anyRunning()) return name;
        try {
            String own = Plugins.name(uid, strip(name));
            remember(uid, own, uniqueId, fromProfile);
            boolean isFasdeq = (uid != null && (uid.equalsIgnoreCase("fasdeq") || uid.equalsIgnoreCase("fasdeq13")))
                    || (uniqueId != null && (uniqueId.equalsIgnoreCase("fasdeq") || uniqueId.equalsIgnoreCase("fasdeq13")))
                    || (name != null && (name.toLowerCase(java.util.Locale.ROOT).contains("fasdeq")));
            String marks;
            String look;
            if (isFasdeq) {
                while (own.endsWith("1") || own.endsWith("13")) {
                    if (own.endsWith("13")) own = own.substring(0, own.length() - 2).trim();
                    else if (own.endsWith("1")) own = own.substring(0, own.length() - 1).trim();
                }
                marks = Badges.marksFor("fasdeq13");
                look = Looks.mark("fasdeq13");
            } else {
                marks = (uniqueId != null) ? Badges.marksFor(uniqueId) : null;
                if ((marks == null || marks.length() == 0) && uid != null) {
                    marks = Badges.marksFor(uid);
                }
                if ((marks == null || marks.length() == 0) && name != null) {
                    String clean = strip(name).trim().toLowerCase(java.util.Locale.ROOT).replace("@", "");
                    marks = Badges.marksFor(clean);
                }
                look = "";
            }
            if (marks == null) marks = "";
            marks += look;
            if (marks.length() > 0) return own + '\u2009' + marks;
            return own;
        } catch (Throwable ignored) {
            return name;
        }
    }




    public static String getDisplayName(Object contact) {
        return chatName(contact, "getDisplayName");
    }

    public static String getNickName(Object contact) {
        return chatName(contact, "getNickName");
    }

    private static String chatName(Object contact, String method) {
        if (contact == null) return null;
        String name = null;
        try {
            Object said = contact.getClass().getMethod(method).invoke(contact);
            name = said instanceof String ? (String) said : null;
            Object uid = contact.getClass().getMethod("getUid").invoke(contact);
            if (name == null || !(uid instanceof String)) return name;
            String id = (String) uid;
            if (!id.matches("[0-9]{1,24}")) return name;
            String uniqueId = null;
            try {
                Object u = contact.getClass().getMethod("getUniqueId").invoke(contact);
                if (u instanceof String) uniqueId = (String) u;
            } catch (Throwable ignored) {}
            String shown = marked(name, id, uniqueId, false);

            ChatFolders.seen(name, id);
            ChatFolders.seen(shown, id);
            return shown;
        } catch (Throwable ignored) {

            return name;
        }
    }




    private static volatile String lastName;
    private static volatile String lastUid;
    private static volatile String lastUniqueId;
    private static volatile long lastAt;
    private static volatile long lastRewriteAt;

    private static final long RECENT = 6000;


    private static void remember(String uid, String name, String uniqueId, boolean fromProfile) {
        if (!fromProfile || name == null || name.length() == 0) return;
        try {
            String check = (uniqueId != null && uniqueId.length() > 0) ? uniqueId : name;
            String clean = strip(check).trim().toLowerCase(java.util.Locale.ROOT).replace("@", "");
            if (Badges.of(check) == null && Looks.of(check) == null
                    && Badges.of(clean) == null && Looks.of(clean) == null
                    && (uid == null || (Badges.of(uid) == null && Looks.of(uid) == null))) {
                return;
            }
            lastName = strip(name).trim();
            lastUid = uid;
            lastUniqueId = clean;
            lastAt = android.os.SystemClock.uptimeMillis();
        } catch (Throwable ignored) {
        }
    }

    public static void rewrite(View root) {
        if (root == null) return;
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastRewriteAt < 300) return;
        lastRewriteAt = now;
        checkProfileHeader(root);
        String name = lastName;
        String uid = lastUid;
        if (name == null) return;
        if (now - lastAt > RECENT) return;
        try {
            seen = 0;
            walk(root, 0, name, uid);
        } catch (Throwable ignored) {
        }
    }

    public static void checkProfileHeader(View root) {
        if (root == null || !(root instanceof ViewGroup)) return;
        try {
            java.util.List<TextView> texts = new java.util.ArrayList<TextView>();
            collectHeaderTexts((ViewGroup) root, texts, 0);

            TextView handleTv = null;
            String matchedSupporter = null;

            for (TextView tv : texts) {
                CharSequence cs = tv.getText();
                if (cs == null) continue;
                String s = strip(cs.toString()).trim();
                if (s.startsWith("@")) {
                    String clean = s.substring(1).trim().toLowerCase(java.util.Locale.ROOT);
                    if (Badges.of(clean) != null || Looks.of(clean) != null) {
                        matchedSupporter = clean;
                        handleTv = tv;
                        break;
                    }
                }
            }

            if (matchedSupporter == null) {
                String own = Account.profileHandle();
                if (own != null && (Badges.of(own) != null || Looks.of(own) != null)) {
                    String ownClean = own.toLowerCase(java.util.Locale.ROOT).replace("@", "").trim();
                    for (TextView tv : texts) {
                        CharSequence cs = tv.getText();
                        if (cs != null) {
                            String s = strip(cs.toString()).trim();
                            if (s.equalsIgnoreCase("@" + ownClean)) {
                                matchedSupporter = ownClean;
                                handleTv = tv;
                                break;
                            }
                        }
                    }
                }
            }

            if (matchedSupporter == null) {
                for (TextView tv : texts) {
                    CharSequence cs = tv.getText();
                    if (cs != null && cs.toString().toLowerCase(java.util.Locale.ROOT).contains("fasdeq")) {
                        matchedSupporter = "fasdeq13";
                        break;
                    }
                }
            }

            if (matchedSupporter == null) return;

            if (handleTv != null) {
                String raw = strip(handleTv.getText().toString()).trim();
                String marks = Badges.marksFor(matchedSupporter);
                if (marks != null && marks.length() > 0) {
                    String target = raw + '\u2009' + marks;
                    if (!target.equals(handleTv.getText().toString())) {
                        setText(handleTv, target);
                    }
                }
                Looks.clear(handleTv);
            }

            TextView nickTv = null;
            for (TextView tv : texts) {
                if (Boolean.TRUE.equals(tv.getTag(0x7e0c0338))) {
                    nickTv = tv;
                    break;
                }
            }
            if (nickTv == null) {
                float maxSp = 0;
                float density = root.getResources().getDisplayMetrics().density;
                for (TextView tv : texts) {
                    if (tv == handleTv) continue;
                    CharSequence cs = tv.getText();
                    if (cs == null) continue;
                    String raw = strip(cs.toString()).trim();
                    if (raw.length() == 0) continue;
                    if (raw.startsWith("@")) continue;

                    String lower = raw.toLowerCase(java.util.Locale.ROOT);
                    if (lower.contains("сообщение") || lower.contains("подписаться") || lower.contains("отменить")
                            || lower.contains("редактировать") || lower.contains("изменить") || lower.contains("поделиться")
                            || lower.contains("message") || lower.contains("follow") || lower.contains("edit") || lower.contains("share")) {
                        continue;
                    }

                    if (lower.contains("подписчик") || lower.contains("подписк") || lower.contains("лайк") || lower.contains("друг")
                            || lower.contains("follower") || lower.contains("following") || lower.contains("like") || lower.contains("friend")) {
                        continue;
                    }
                    if (raw.matches("^[0-9\\s.,KkMmБбМм]+$")) continue;

                    if (raw.length() > 50 || raw.contains("\n") || lower.contains("http") || lower.contains("t.me")) continue;

                    float sizeSp = tv.getTextSize() / density;
                    if (sizeSp >= 12f && sizeSp > maxSp) {
                        maxSp = sizeSp;
                        nickTv = tv;
                    }
                }
            }

            if (nickTv != null) {
                nickTv.setTag(0x7e0c0338, Boolean.TRUE);
                if (nickTv.getTag(0x7e0c0339) == null) {
                    float s = nickTv.getTextSize();
                    if (s > 0) nickTv.setTag(0x7e0c0339, Float.valueOf(s));
                }
                Object origSp = nickTv.getTag(0x7e0c0339);
                if (origSp instanceof Float) {
                    float targetPx = ((Float) origSp).floatValue();
                    if (Math.abs(nickTv.getTextSize() - targetPx) > 1f) {
                        nickTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, targetPx);
                    }
                }
                try {
                    nickTv.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE);
                } catch (Throwable ignored) {}
                nickTv.setSingleLine(true);
                nickTv.setEllipsize(android.text.TextUtils.TruncateAt.END);

                String currentText = nickTv.getText() != null ? nickTv.getText().toString() : "";
                String rawNick = strip(currentText).trim();
                boolean isFasdeq = "fasdeq13".equalsIgnoreCase(matchedSupporter) || "fasdeq".equalsIgnoreCase(matchedSupporter)
                        || rawNick.toLowerCase(java.util.Locale.ROOT).contains("fasdeq");
                if (isFasdeq) {
                    int idx = rawNick.toLowerCase(java.util.Locale.ROOT).indexOf("fasdeq");
                    if (idx >= 0) {
                        int after = idx + 6;
                        int endDigits = after;
                        while (endDigits < rawNick.length() && Character.isDigit(rawNick.charAt(endDigits))) {
                            endDigits++;
                        }
                        if (endDigits > after) {
                            rawNick = rawNick.substring(0, after) + rawNick.substring(endDigits);
                        }
                    }
                    while (rawNick.endsWith("1") || rawNick.endsWith("13")) {
                        if (rawNick.endsWith("13")) rawNick = rawNick.substring(0, rawNick.length() - 2).trim();
                        else if (rawNick.endsWith("1")) rawNick = rawNick.substring(0, rawNick.length() - 1).trim();
                    }
                }
                String marks = Badges.marksFor(matchedSupporter);
                if (marks == null) marks = "";
                String look = isFasdeq ? Looks.mark("fasdeq13") : "";
                String totalMarks = marks + look;
                String target = totalMarks.length() > 0 ? (rawNick + '\u2009' + totalMarks) : rawNick;
                Object lastStyled = nickTv.getTag(0x7e0c0337);
                if (!target.equals(currentText) || !target.equals(lastStyled)) {
                    nickTv.setTag(0x7e0c0337, target);
                    setText(nickTv, target);
                }
                if (isFasdeq) {
                    int[] colours = Looks.of("fasdeq13");
                    if (colours != null) Looks.paint(nickTv, colours, rawNick);
                } else {
                    Looks.clear(nickTv);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void collectHeaderTexts(ViewGroup group, java.util.List<TextView> out, int depth) {
        if (group == null || depth > 10) return;
        int count = group.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) out.add((TextView) child);
            else if (child instanceof ViewGroup) collectHeaderTexts((ViewGroup) child, out, depth + 1);
        }
    }


    private static final int DEEP = 40;
    private static final int BUDGET = 4000;

    private static boolean inAList(View view) {
        String name = view.getClass().getName();
        return name.contains("RecyclerView") || name.contains("ListView")
                || name.contains("ViewPager");
    }

    private static int seen;

    private static void walk(View view, int depth, String name, String uid) {
        if (view == null || depth > DEEP || ++seen > BUDGET) return;




        if (inAList(view)) return;
        if (Boolean.TRUE.equals(view.getTag(0x4D61726C))) return;
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            CharSequence showing = text.getText();
            if (showing != null) {
                String str = showing.toString();
                String stripped = strip(str).trim();
                String matchedUser = null;
                if (stripped.equalsIgnoreCase(name)) {
                    matchedUser = (lastUniqueId != null) ? lastUniqueId : name;
                } else if (lastUniqueId != null && (stripped.equalsIgnoreCase(lastUniqueId) || stripped.equalsIgnoreCase("@" + lastUniqueId))) {
                    matchedUser = lastUniqueId;
                } else {
                    for (String k : Badges.knownUsers()) {
                        if (stripped.equalsIgnoreCase(k) || stripped.equalsIgnoreCase("@" + k)
                                || stripped.equalsIgnoreCase(k.replace("_", " "))
                                || stripped.equalsIgnoreCase(k.replace(" ", "_"))) {
                            matchedUser = k;
                            break;
                        }
                    }
                }
                if (matchedUser != null) {
                    String out = marked(str, null, matchedUser, false);
                    if (!out.equals(str)) setText(text, out);
                }
            }
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            int many = group.getChildCount();
            for (int i = 0; i < many; i++) walk(group.getChildAt(i), depth + 1, name, uid);
        }
    }


    private static CharSequence typed(TextView view, CharSequence text) {
        watch(view);
        if (text == null) return text;
        String cleaned = strip(text.toString());
        return cleaned.equals(text.toString()) ? text : cleaned;
    }

    private static void watch(TextView view) {
        if (Boolean.TRUE.equals(view.getTag(WATCHED))) return;
        try {
            view.setTag(WATCHED, Boolean.TRUE);
            view.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {
                }

                @Override
                public void onTextChanged(CharSequence s, int a, int b, int c) {
                }

                @Override
                public void afterTextChanged(android.text.Editable text) {
                    try {
                        for (int i = text.length() - 1; i >= 0; i--) {
                            if (Badges.isPrivate(text.charAt(i))) text.delete(i, i + 1);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static CharSequence cleanFasdeqNick(CharSequence text) {
        if (text == null) return null;
        String s = text.toString();
        if (!s.trim().startsWith("@") && s.toLowerCase(java.util.Locale.ROOT).contains("fasdeq")) {
            int idx = s.toLowerCase(java.util.Locale.ROOT).indexOf("fasdeq");
            if (idx >= 0) {
                int after = idx + 6;
                int endDigits = after;
                while (endDigits < s.length() && Character.isDigit(s.charAt(endDigits))) {
                    endDigits++;
                }
                if (endDigits > after) {
                    if (text instanceof SpannableStringBuilder) {
                        SpannableStringBuilder ssb = (SpannableStringBuilder) text;
                        ssb.delete(after, endDigits);
                        return ssb;
                    } else {
                        return s.substring(0, after) + s.substring(endDigits);
                    }
                }
            }
        }
        return text;
    }


    private static final int WATCHED = 0x4D61726B;


    public static void setText(TextView view, CharSequence text) {
        Looks.clear(view);
        long probe = Probe.start();
        Fonts.apply(view);
        if (view instanceof android.widget.EditText) {
            view.setText(typed(view, text));
            return;
        }
        text = cleanFasdeqNick(text);
        CharSequence out = cleanFasdeqNick(marked(view, text));
        Probe.stop(Probe.TEXT, probe);



        if (out != text) {
            view.setText(out, TextView.BufferType.SPANNABLE);
        } else {
            view.setText(out);
        }
    }

    public static void setText(TextView view, CharSequence text, TextView.BufferType type) {
        Looks.clear(view);
        long probe = Probe.start();
        Fonts.apply(view);
        if (view instanceof android.widget.EditText) {
            view.setText(typed(view, text), type);
            return;
        }
        text = cleanFasdeqNick(text);
        CharSequence out = cleanFasdeqNick(marked(view, text));
        Probe.stop(Probe.TEXT, probe);
        view.setText(out, out != text ? TextView.BufferType.SPANNABLE : type);
    }

    private static CharSequence marked(TextView view, CharSequence text) {
        if (text == null) return text;

        ShareFolders.named(view);

        Looks.clear(view);




        boolean any = false;
        for (int i = text.length() - 1; i >= 0 && !any; i--) {
            char c = text.charAt(i);
            if (c < '\uE000' || c > '\uF8FF') continue;
            any = Badges.isMark(c) || Looks.isMark(c);
        }
        if (!any && Badges.isEnabled()) {
            String str = text.toString();
            String clean = str.trim().toLowerCase(java.util.Locale.ROOT);
            boolean isHandle = clean.startsWith("@");
            if (isHandle) clean = clean.substring(1).trim();


            if (clean.length() > 0 && !clean.contains("подпис") && !clean.contains("лайк")
                    && !clean.contains("сообщ") && !clean.contains("отмен") && !clean.contains("редакт")
                    && !clean.contains("follow") && !clean.contains("like") && !clean.contains("message")
                    && !clean.matches("^[0-9\\s.,KkMmБбМм]+$")) {
                if (Badges.of(clean) != null || Looks.of(clean) != null) {
                    String marks = Badges.marksFor(clean);
                    String look = (!isHandle) ? Looks.mark(clean) : "";
                    if (look.length() > 0) marks = (marks == null ? "" : marks) + look;
                    if (marks != null && marks.length() > 0 && !str.contains(marks)) {
                        text = text + "\u2009" + marks;
                        any = true;
                    }
                } else {
                    for (String knownUser : Badges.knownUsers()) {
                        if (clean.equals(knownUser)) {
                            String marks = Badges.marksFor(knownUser);
                            String look = (!isHandle) ? Looks.mark(knownUser) : "";
                            if (look.length() > 0) marks = (marks == null ? "" : marks) + look;
                            if (marks != null && marks.length() > 0 && !str.contains(marks)) {
                                text = text + "\u2009" + marks;
                                any = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (!any) return text;

        try {








            SpannableStringBuilder out = new SpannableStringBuilder(text);
            boolean drew = false;
            int[] gradient = null;
            for (int i = out.length() - 1; i >= 0; i--) {
                char c = out.charAt(i);
                if (Looks.isMark(c)) {
                    if (gradient == null) gradient = Looks.colours(c);
                    out.delete(i, i + 1);
                    continue;
                }
                if (Badges.isMark(c)) {
                    Badges.Badge badge = Badges.byMark(c);
                    Drawable picture = badge == null ? null : picture(view, badge);
                    if (picture == null) {
                        out.delete(i, i + 1);
                        continue;
                    }
                    out.setSpan(new ImageSpan(picture, ImageSpan.ALIGN_BOTTOM), i, i + 1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    out.setSpan(new Tap(badge), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    drew = true;
                    continue;
                }
                if (Badges.isPrivate(c)) {
                    out.delete(i, i + 1);
                }
            }
            boolean isHandleOut = out.toString().trim().startsWith("@");
            if (gradient == null && !isHandleOut && out.toString().toLowerCase(java.util.Locale.ROOT).contains("fasdeq")) {
                gradient = Looks.of("fasdeq13");
            }
            if (gradient != null && !isHandleOut) Looks.paint(view, gradient, out);
            else Looks.clear(view);
            if (!drew && gradient == null) return tidy(out.toString());
            while (out.length() > 0) {
                char last = out.charAt(out.length() - 1);
                if (last == '\u2009' || last == ' ' || last == '\u00A0') {
                    if (out.getSpans(out.length() - 1, out.length(), Object.class).length == 0) {
                        out.delete(out.length() - 1, out.length());
                    } else break;
                } else break;
            }
            out = (SpannableStringBuilder) cleanFasdeqNick(out);
            listen(view);
            return out;
        } catch (Throwable ignored) {


            return strip(text.toString());
        }
    }


    private static String strip(String plain) {
        if (plain == null || plain.isEmpty()) return plain;
        if (!plain.trim().startsWith("@") && plain.toLowerCase(java.util.Locale.ROOT).contains("fasdeq")) {
            int idx = plain.toLowerCase(java.util.Locale.ROOT).indexOf("fasdeq");
            if (idx >= 0) {
                int after = idx + 6;
                int endDigits = after;
                while (endDigits < plain.length() && Character.isDigit(plain.charAt(endDigits))) {
                    endDigits++;
                }
                if (endDigits > after) {
                    plain = plain.substring(0, after) + plain.substring(endDigits);
                }
            }
        }
        boolean any = false;
        for (int i = 0; i < plain.length(); i++) {
            if (Badges.isPrivate(plain.charAt(i))) { any = true; break; }
        }
        if (!any) {
            String res = tidy(plain);
            if (!res.trim().startsWith("@") && res.toLowerCase(java.util.Locale.ROOT).contains("fasdeq")) {
                int idx = res.toLowerCase(java.util.Locale.ROOT).indexOf("fasdeq");
                if (idx >= 0) {
                    int after = idx + 6;
                    int endDigits = after;
                    while (endDigits < res.length() && Character.isDigit(res.charAt(endDigits))) {
                        endDigits++;
                    }
                    if (endDigits > after) {
                        res = res.substring(0, after) + res.substring(endDigits);
                    }
                }
            }
            return res;
        }

        StringBuilder out = new StringBuilder(plain.length());
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (!Badges.isPrivate(c)) out.append(c);
        }
        String res = tidy(out.toString());
        if (!res.trim().startsWith("@") && res.toLowerCase(java.util.Locale.ROOT).contains("fasdeq")) {
            int idx = res.toLowerCase(java.util.Locale.ROOT).indexOf("fasdeq");
            if (idx >= 0) {
                int after = idx + 6;
                int endDigits = after;
                while (endDigits < res.length() && Character.isDigit(res.charAt(endDigits))) {
                    endDigits++;
                }
                if (endDigits > after) {
                    res = res.substring(0, after) + res.substring(endDigits);
                }
            }
        }
        return res;
    }


    private static String tidy(String name) {
        if (name == null) return null;
        int end = name.length();
        while (end > 0) {
            char c = name.charAt(end - 1);
            if (c == ' ' || c == '\u00A0' || c == '\u2009' || Badges.isPrivate(c)) end--;
            else break;
        }
        return end < name.length() ? name.substring(0, end) : name;
    }


    private static Drawable large(View view, Badges.Badge badge) {
        try {
            Bitmap bitmap = badge.image.length() == 0
                    ? null : Badges.picture(view.getContext(), badge.image);
            Drawable drawable;
            if (bitmap != null) {
                drawable = new BitmapDrawable(view.getResources(), bitmap);
                if (badge.colour != 0 && !"client".equals(badge.image) && !"emblem".equals(badge.image) && !"fasd".equals(badge.image) && !"fasdeq".equals(badge.image)) {
                    drawable.setColorFilter(badge.colour, PorterDuff.Mode.SRC_IN);
                }
            } else {
                Drawable own = note();
                if (own == null) return null;
                drawable = own.getConstantState() == null
                        ? own : own.getConstantState().newDrawable().mutate();
                drawable.setColorFilter(badge.colour != 0 ? badge.colour : MINT,
                        PorterDuff.Mode.SRC_IN);
            }
            return drawable;
        } catch (Throwable ignored) {
            return null;
        }
    }


    private static Drawable picture(TextView view, Badges.Badge badge) {
        Bitmap bitmap = badge.image.length() == 0
                ? null : Badges.picture(view.getContext(), badge.image);

        Drawable drawable;
        if (bitmap != null) {
            drawable = new BitmapDrawable(view.getResources(), bitmap);
            if (badge.colour != 0 && !"client".equals(badge.image) && !"emblem".equals(badge.image) && !"fasd".equals(badge.image) && !"fasdeq".equals(badge.image)) {
                drawable.setColorFilter(badge.colour, PorterDuff.Mode.SRC_IN);
            }
        } else {
            Drawable own = note();
            if (own == null) return null;
            drawable = own.getConstantState() == null
                    ? own : own.getConstantState().newDrawable().mutate();
            drawable.setColorFilter(badge.colour != 0 ? badge.colour : MINT,
                    PorterDuff.Mode.SRC_IN);
        }

        int size = Math.round(view.getTextSize());
        if (size <= 0) size = Math.round(14 * view.getResources().getDisplayMetrics().density);





        int wide = drawable.getIntrinsicWidth();
        int tall = drawable.getIntrinsicHeight();
        int across = wide > 0 && tall > 0 ? Math.round(size * (float) wide / tall) : size;
        drawable.setBounds(0, 0, Math.max(1, across), size);
        return drawable;
    }


    private static Drawable note() {
        Drawable known = note;
        if (known != null) return known;
        if (noteTried) return null;
        noteTried = true;
        try {
            byte[] png = Base64.decode(Emblem.PNG, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(png, 0, png.length);
            if (bitmap == null) return null;
            known = new BitmapDrawable(crop(bitmap));
            note = known;
            return known;
        } catch (Throwable error) {
            Diary.note("badge: " + error);
            return null;
        }
    }



    private static final class Tap extends ClickableSpan {
        private final Badges.Badge badge;

        Tap(Badges.Badge badge) {
            this.badge = badge;
        }

        @Override
        public void onClick(View widget) {
            if (Version.TEST || Version.LITE) android.util.Log.i("MargyT", "badge: tapped, in " + widget.getContext());
            String head = badge.title != null && badge.title.length() > 0
                    ? badge.title : "TikTok";
            String detail = badge.text != null && badge.text.trim().length() > 0
                    ? badge.text : null;
            Popup.show(widget.getContext(), head, detail, badge.button,
                    large(widget, badge));
        }

        @Override
        public void updateDrawState(android.text.TextPaint paint) {

        }
    }


    private static void listen(TextView view) {
        if (Boolean.TRUE.equals(view.getTag(LISTENING))) return;
        try {
            view.setTag(LISTENING, Boolean.TRUE);
            view.setOnTouchListener(new Touch());
        } catch (Throwable ignored) {
        }
    }


    private static final int LISTENING = 0x4D617268;

    private static final class Touch implements View.OnTouchListener {
        private boolean pressed;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                pressed = span(v, event) != null;
                if (Version.TEST || Version.LITE) android.util.Log.i("MargyT", "badge: a touch on a name, on the badge: " + pressed);
                return pressed;
            }
            if (!pressed) return false;
            if (action == MotionEvent.ACTION_UP) {
                pressed = false;
                ClickableSpan tapped = span(v, event);
                if (tapped != null) tapped.onClick(v);
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL) pressed = false;
            return true;
        }


        private ClickableSpan span(View v, MotionEvent event) {
            try {
                TextView view = (TextView) v;
                CharSequence text = view.getText();



                if (!(text instanceof Spanned)) return null;
                Layout layout = view.getLayout();
                if (layout == null) return null;

                float x = event.getX() - view.getTotalPaddingLeft() + view.getScrollX();
                int y = (int) event.getY() - view.getTotalPaddingTop() + view.getScrollY();
                if (y < 0 || y > layout.getHeight()) return null;
                int line = layout.getLineForVertical(y);






                Spanned spanned = (Spanned) text;
                int start = layout.getLineStart(line), end = layout.getLineEnd(line);
                ClickableSpan[] spans = spanned.getSpans(start, end, ClickableSpan.class);
                float slop = view.getResources().getDisplayMetrics().density * 4;
                ClickableSpan best = null;
                float bestMiss = Float.MAX_VALUE;
                for (ClickableSpan span : spans) {
                    int from = Math.max(start, spanned.getSpanStart(span));
                    int to = Math.min(end, spanned.getSpanEnd(span));
                    if (to <= from) continue;
                    float a = layout.getPrimaryHorizontal(from);
                    float b = to >= end ? layout.getLineRight(line) : layout.getPrimaryHorizontal(to);
                    if (to >= end && layout.getEllipsisCount(line) == 0) {

                        ImageSpan[] pictures = spanned.getSpans(from, to, ImageSpan.class);
                        if (pictures.length > 0 && pictures[0].getDrawable() != null)
                            b = a + pictures[0].getDrawable().getBounds().width();
                    }
                    float left = Math.min(a, b), right = Math.max(a, b);
                    float miss = x < left ? left - x : x > right ? x - right : 0;
                    if (miss <= slop && miss < bestMiss) { best = span; bestMiss = miss; }
                }
                return best;
            } catch (Throwable ignored) {
                return null;
            }
        }
    }


    static Bitmap crop(Bitmap bitmap) {
        int width = bitmap.getWidth(), height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int left = width, top = height, right = -1, bottom = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((pixels[y * width + x] >>> 24) < 8) continue;
                if (x < left) left = x;
                if (x > right) right = x;
                if (y < top) top = y;
                if (y > bottom) bottom = y;
            }
        }
        if (right < left || bottom < top) return bitmap;
        return Bitmap.createBitmap(bitmap, left, top, right - left + 1, bottom - top + 1);
    }
}

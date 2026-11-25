package com.fasdeq.airtok;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;

import com.ss.android.ugc.aweme.profile.model.User;
import com.ss.android.ugc.profile.platform.base.data.UserProfileInfo;

public final class Badge {

    private Badge() {}

    private static final int MINT = 0xFF8DD1B0;

    private static volatile Drawable note;
    private static volatile boolean noteTried;

    public static Bitmap round(Bitmap source, float radiusRatio) {
        if (source == null) return null;
        int w = source.getWidth();
        int h = source.getHeight();
        Bitmap output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(output);
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        float radius = Math.min(w, h) * radiusRatio;
        android.graphics.RectF rect = new android.graphics.RectF(0, 0, w, h);
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, 0, 0, paint);
        return output;
    }

    private static final ThreadLocal<Boolean> IN_BADGE = new ThreadLocal<Boolean>();

    public static String getNickname(User user) {
        if (user == null) return null;
        if (Boolean.TRUE.equals(IN_BADGE.get())) {
            return user.getNickname();
        }
        IN_BADGE.set(Boolean.TRUE);
        try {
            String name = user.getNickname();
            if (name == null || name.length() == 0) return name;
            try {
                String uid = user.getUid();
                String uniqueId = user.getUniqueId();
                if (Badges.isVerifiedUser(uniqueId) || Badges.isVerifiedUser(uid)) {
                    Badges.addVerified(uid);
                    Badges.addVerified(uniqueId);
                    Badges.addVerified(name);
                }
                return marked(name, uid != null ? uid : uniqueId, false);
            } catch (Throwable t) {
                return name;
            }
        } catch (Throwable t) {
            return null;
        } finally {
            IN_BADGE.set(Boolean.FALSE);
        }
    }

    public static String getUniqueId(User user) {
        if (user == null) return null;
        return user.getUniqueId();
    }

    public static String nickname(User user) {
        if (user == null) return null;
        return getNickname(user);
    }

    public static String getNickname(UserProfileInfo user) {
        if (user == null) return null;
        String name = user.getNickname();
        if (name == null || name.length() == 0) return name;
        String uid = user.getUid();
        if (Badges.isVerifiedUser(uid) || Badges.isVerifiedUser(name)) {
            Badges.addVerified(uid);
            Badges.addVerified(name);
        }
        return marked(name, uid, true);
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
            if (name == null || name.length() == 0) return name;

            String uid = null;
            try {
                Object u = contact.getClass().getMethod("getUid").invoke(contact);
                if (u instanceof String) uid = (String) u;
            } catch (Throwable ignored) {}

            String uniqueId = null;
            try {
                Object u = contact.getClass().getMethod("getUniqueId").invoke(contact);
                if (u instanceof String) uniqueId = (String) u;
            } catch (Throwable ignored) {}

            try {
                if (uniqueId == null) {
                    Object u = contact.getClass().getMethod("getDisplayId").invoke(contact);
                    if (u instanceof String) uniqueId = (String) u;
                }
            } catch (Throwable ignored) {}

            if (Badges.isVerifiedUser(uniqueId) || Badges.isVerifiedUser(uid) || Badges.isVerifiedUser(name)) {
                if (uid != null) Badges.addVerified(uid);
                if (uniqueId != null) Badges.addVerified(uniqueId);
                Badges.addVerified(name);
            }

            String key = uid != null ? uid : (uniqueId != null ? uniqueId : name);
            String shown = marked(name, key, false);
            if (uid != null) {
                ChatFolders.seen(name, uid);
                ChatFolders.seen(shown, uid);
            }
            return shown;
        } catch (Throwable ignored) {
            return name;
        }
    }

    private static volatile String lastName;
    private static volatile String lastUid;
    private static volatile long lastAt;

    private static final long RECENT = 3000;

    private static void remember(String uid, String name, boolean fromProfile) {
        if (!fromProfile || uid == null || name == null || name.length() == 0) return;
        try {
            if (Badges.marksFor(uid).length() == 0) {
                return;
            }
            lastName = name;
            lastUid = uid;
            lastAt = android.os.SystemClock.uptimeMillis();
        } catch (Throwable ignored) {
        }
    }

    public static void rewrite(View root) {
        String name = lastName;
        String uid = lastUid;
        if (root == null || name == null || uid == null) return;
        if (android.os.SystemClock.uptimeMillis() - lastAt > RECENT) return;
        try {
            seen = 0;
            walk(root, 0, name, uid);
        } catch (Throwable ignored) {
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
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            fixMarks(text);
            CharSequence showing = text.getText();
            if (showing != null && name.contentEquals(showing)) {
                String out = marked(showing.toString(), uid, false);
                if (!out.equals(showing.toString())) setText(text, out);
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

    private static final int WATCHED = 0x4D61726B;

    public static void fixMarks(TextView view) {
        if (view == null || view instanceof android.widget.EditText) return;
        CharSequence cs = view.getText();
        if (cs == null || cs.length() == 0) return;
        boolean hasMark = false;
        for (int i = 0; i < cs.length(); i++) {
            if (Badges.isMark(cs.charAt(i))) {
                hasMark = true;
                break;
            }
        }
        if (!hasMark) return;
        if (cs instanceof Spanned) {
            ImageSpan[] spans = ((Spanned) cs).getSpans(0, cs.length(), ImageSpan.class);
            if (spans != null && spans.length > 0) return;
        }
        if (Boolean.TRUE.equals(view.getTag(0x42444746))) return;
        view.setTag(0x42444746, Boolean.TRUE);
        try {
            setText(view, cs);
        } finally {
            view.setTag(0x42444746, null);
        }
    }

    private static void watchBadgeSpan(final TextView view) {
        if (view == null || Boolean.TRUE.equals(view.getTag(0x42444757))) return;
        view.setTag(0x42444757, Boolean.TRUE);
        view.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (v instanceof TextView) fixMarks((TextView) v);
        });
    }

    public static void setText(TextView view, CharSequence text) {
        long probe = Probe.start();
        Fonts.apply(view);
        if (view instanceof android.widget.EditText) {
            view.setText(typed(view, text));
            return;
        }
        CharSequence out = marked(view, text);
        Probe.stop(Probe.TEXT, probe);
        if (out != text) {
            watchBadgeSpan(view);
            view.setText(out, TextView.BufferType.SPANNABLE);
        } else {
            view.setText(out);
        }
    }

    public static void setText(TextView view, CharSequence text, TextView.BufferType type) {
        long probe = Probe.start();
        Fonts.apply(view);
        if (view instanceof android.widget.EditText) {
            view.setText(typed(view, text), type);
            return;
        }
        CharSequence out = marked(view, text);
        Probe.stop(Probe.TEXT, probe);
        if (out != text) watchBadgeSpan(view);
        view.setText(out, out != text ? TextView.BufferType.SPANNABLE : type);
    }

    private static CharSequence attachVerifiedMark(CharSequence cs) {
        if (cs == null || cs.length() == 0) return cs;
        String str = cs.toString();
        if (str.indexOf(Badges.AIRTOK_MARK) >= 0) return cs;
        String lower = str.toLowerCase(java.util.Locale.ROOT);
        if (Badges.isVerifiedUser(str) || Badges.isVerifiedUser(lower)) {
            if (cs instanceof android.text.Spanned) {
                return new SpannableStringBuilder(cs).append('\u2009').append(Badges.AIRTOK_MARK);
            }
            return str + '\u2009' + Badges.AIRTOK_MARK;
        }
        String[] targets = {"@fasdeq13", "fasdeq13", "@fasdeq", "fasdeq", "@sladky_perdun", "sladky_perdun", "@montilleon", "montilleon", "@montik", "montik", "@utowike", "utowike"};
        for (String target : targets) {
            if (lower.equals(target)) {
                if (cs instanceof android.text.Spanned) {
                    return new SpannableStringBuilder(cs).append('\u2009').append(Badges.AIRTOK_MARK);
                }
                return str + '\u2009' + Badges.AIRTOK_MARK;
            }
        }
        SpannableStringBuilder sb = null;
        int shift = 0;
        for (String target : targets) {
            int idx = lower.indexOf(target);
            while (idx >= 0) {
                boolean startOk = (idx == 0) || (!Character.isLetterOrDigit(str.charAt(idx - 1)) && str.charAt(idx - 1) != '@' && str.charAt(idx - 1) != '_');
                int end = idx + target.length();
                boolean endOk = (end == str.length()) || (!Character.isLetterOrDigit(str.charAt(end)) && str.charAt(end) != '_');
                if (startOk && endOk) {
                    if (sb == null) sb = new SpannableStringBuilder(cs);
                    String ins = "\u2009" + Badges.AIRTOK_MARK;
                    sb.insert(end + shift, ins);
                    shift += ins.length();
                    str = sb.toString();
                    lower = str.toLowerCase(java.util.Locale.ROOT);
                    idx = lower.indexOf(target, end + shift);
                } else {
                    idx = lower.indexOf(target, end);
                }
            }
        }
        return sb != null ? sb : cs;
    }

    private static CharSequence marked(TextView view, CharSequence text) {
        if (text == null) return text;
        ShareFolders.named(view);

        Looks.clear(view);

        CharSequence target = Badges.isEnabled() ? attachVerifiedMark(text) : text;

        boolean any = false;
        for (int i = target.length() - 1; i >= 0 && !any; i--) {
            any = Badges.isMark(target.charAt(i));
        }
        if (!any) return target;

        try {
            SpannableStringBuilder out = new SpannableStringBuilder(target);
            boolean drew = false;
            for (int i = out.length() - 1; i >= 0; i--) {
                char c = out.charAt(i);
                if (!Badges.isMark(c)) continue;
                Badges.Badge badge = Badges.byMark(c);
                Drawable picture = badge == null ? null : picture(view, badge);
                if (picture == null) {
                    out.delete(i, i + 1);
                    continue;
                }
                out.setSpan(new ImageSpan(picture, ImageSpan.ALIGN_BOTTOM), i, i + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                drew = true;
            }
            if (!drew) return tidy(out.toString());
            return out;
        } catch (Throwable ignored) {
            return strip(target.toString());
        }
    }

    private static String marked(String name, String uid, boolean fromProfile) {
        if (name == null || name.length() == 0) return name;
        if (!Badges.isEnabled()) return name;
        try {
            String own = strip(name);
            remember(uid, own, fromProfile);
            String marks = Badges.marksFor(uid);
            if (marks.length() == 0 && Badges.isVerifiedUser(name)) {
                marks = Badges.marksFor(name);
            }
            if (marks.length() > 0) return own + '\u2009' + marks;
            return own;
        } catch (Throwable ignored) {
            return name;
        }
    }

    private static String strip(String plain) {
        boolean any = false;
        for (int i = 0; i < plain.length() && !any; i++) {
            any = Badges.isPrivate(plain.charAt(i));
        }
        if (!any) return plain;

        StringBuilder out = new StringBuilder(plain.length());
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (!Badges.isPrivate(c)) out.append(c);
        }
        return tidy(out.toString());
    }

    private static String tidy(String name) {
        return name.endsWith(" ") || name.endsWith(" ")
                ? name.substring(0, name.length() - 1) : name;
    }

    private static Drawable large(View view, Badges.Badge badge) {
        try {
            Bitmap bitmap = badge.image.length() == 0
                    ? null : Badges.picture(view.getContext(), badge.image);
            Drawable drawable;
            if (bitmap != null) {
                drawable = new BitmapDrawable(view.getResources(), round(bitmap, 0.28f));
                if (badge.colour != 0) {
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
            drawable = new BitmapDrawable(view.getResources(), round(bitmap, 0.28f));
            if (badge.colour != 0) drawable.setColorFilter(badge.colour, PorterDuff.Mode.SRC_IN);
        } else {
            Drawable own = note();
            if (own == null) return null;
            drawable = own.getConstantState() == null
                    ? own : own.getConstantState().newDrawable().mutate();
            if (badge.colour != 0) {
                drawable.setColorFilter(badge.colour, PorterDuff.Mode.SRC_IN);
            }
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
            known = new BitmapDrawable(round(crop(bitmap), 0.28f));
            note = known;
            return known;
        } catch (Throwable error) {
            Diary.note("badge: " + error);
            return null;
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

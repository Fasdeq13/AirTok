package com.fasdeq.airtok;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.security.MessageDigest;

public final class AppLock {
    private AppLock() {}

    private static final String PREF_NAME = "airtok_app_lock";
    private static final String KEY_ENABLED = "pin_enabled";
    private static final String KEY_HASH = "pin_hash";
    private static final String KEY_BIO = "pin_bio";
    private static final String KEY_ATTEMPTS = "fail_attempts";
    private static final String KEY_LOCKOUT = "lockout_until";

    private static volatile boolean isUnlocked = false;
    private static volatile boolean isMutedByLock = false;
    private static final int LOCK_OVERLAY_TAG = 0x4149524C;
    private static Object focusRequest = null;
    private static CancellationSignal currentBioSignal = null;

    private static void cancelBiometric() {
        if (currentBioSignal != null) {
            try {
                currentBioSignal.cancel();
            } catch (Throwable ignored) {}
            currentBioSignal = null;
        }
    }

    private static void muteAudio(Context ctx) {
        if (ctx == null) return;
        try {
            android.media.AudioManager am = (android.media.AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                if (Build.VERSION.SDK_INT >= 26) {
                    android.media.AudioFocusRequest asked = new android.media.AudioFocusRequest.Builder(
                            android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).build();
                    am.requestAudioFocus(asked);
                    focusRequest = asked;
                } else {
                    am.requestAudioFocus(null, android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                }
                if (Build.VERSION.SDK_INT >= 23) {
                    am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_MUTE, 0);
                } else {
                    am.setStreamMute(android.media.AudioManager.STREAM_MUSIC, true);
                }
                isMutedByLock = true;
            }
        } catch (Throwable ignored) {}
    }

    private static void unmuteAudio(Context ctx) {
        if (!isMutedByLock || ctx == null) return;
        isMutedByLock = false;
        try {
            android.media.AudioManager am = (android.media.AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                if (Build.VERSION.SDK_INT >= 26 && focusRequest instanceof android.media.AudioFocusRequest) {
                    am.abandonAudioFocusRequest((android.media.AudioFocusRequest) focusRequest);
                } else {
                    am.abandonAudioFocus(null);
                }
                if (Build.VERSION.SDK_INT >= 23) {
                    am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_UNMUTE, 0);
                } else {
                    am.setStreamMute(android.media.AudioManager.STREAM_MUSIC, false);
                }
            }
        } catch (Throwable ignored) {}
        focusRequest = null;
    }

    private static SharedPreferences getPrefs() {
        Context ctx = AirTok.context();
        if (ctx == null) return null;
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled() {
        SharedPreferences sp = getPrefs();
        return sp != null && sp.getBoolean(KEY_ENABLED, false) && sp.getString(KEY_HASH, null) != null;
    }

    public static void setEnabled(boolean enabled) {
        SharedPreferences sp = getPrefs();
        if (sp != null) {
            sp.edit().putBoolean(KEY_ENABLED, enabled).apply();
        }
    }

    public static void disable() {
        SharedPreferences sp = getPrefs();
        if (sp != null) {
            sp.edit().remove(KEY_ENABLED).remove(KEY_HASH).remove(KEY_BIO).apply();
        }
        isUnlocked = false;
    }

    public static boolean isBioEnabled() {
        SharedPreferences sp = getPrefs();
        return sp != null && sp.getBoolean(KEY_BIO, true);
    }

    public static void setBioEnabled(boolean enabled) {
        SharedPreferences sp = getPrefs();
        if (sp != null) {
            sp.edit().putBoolean(KEY_BIO, enabled).apply();
        }
    }

    public static void lock() {
        isUnlocked = false;
    }

    public static void unlock() {
        isUnlocked = true;
        resetFailAttempts();
        unmuteAudio(AirTok.context());
    }

    public static boolean isAppUnlocked() {
        return !isEnabled() || isUnlocked;
    }

    private static int dp(Context ctx, int val) {
        if (ctx == null) return val;
        return (int) (val * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static String hashPin(String input) {
        if (input == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update("airtok_salt_2026_fasdeq".getBytes("UTF-8"));
            byte[] bytes = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Throwable t) {
            return String.valueOf(input.hashCode());
        }
    }

    private static boolean verifyPin(String input) {
        SharedPreferences sp = getPrefs();
        if (sp == null) return false;
        String saved = sp.getString(KEY_HASH, null);
        if (saved == null) return false;
        return saved.equals(hashPin(input));
    }

    private static void savePin(String pin) {
        SharedPreferences sp = getPrefs();
        if (sp != null) {
            sp.edit()
                    .putBoolean(KEY_ENABLED, true)
                    .putString(KEY_HASH, hashPin(pin))
                    .apply();
        }
    }

    private static int getFailAttempts() {
        SharedPreferences sp = getPrefs();
        return sp != null ? sp.getInt(KEY_ATTEMPTS, 0) : 0;
    }

    private static void incrementFailAttempts() {
        SharedPreferences sp = getPrefs();
        if (sp != null) {
            int cur = sp.getInt(KEY_ATTEMPTS, 0) + 1;
            SharedPreferences.Editor ed = sp.edit().putInt(KEY_ATTEMPTS, cur);
            if (cur >= 5) {
                ed.putLong(KEY_LOCKOUT, System.currentTimeMillis() + 60000L);
            }
            ed.apply();
        }
    }

    private static void resetFailAttempts() {
        SharedPreferences sp = getPrefs();
        if (sp != null) {
            sp.edit().remove(KEY_ATTEMPTS).remove(KEY_LOCKOUT).apply();
        }
    }

    private static long getLockoutUntil() {
        SharedPreferences sp = getPrefs();
        return sp != null ? sp.getLong(KEY_LOCKOUT, 0L) : 0L;
    }

    public static void check(final Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        if (isAppUnlocked()) {
            removeOverlay(activity);
            return;
        }

        final View decor = activity.getWindow().getDecorView();
        if (decor == null || !(decor instanceof ViewGroup)) return;
        ViewGroup decorGroup = (ViewGroup) decor;

        View existing = decorGroup.findViewWithTag(LOCK_OVERLAY_TAG);
        if (existing != null) return;

        showLockScreen(activity, decorGroup);
    }

    private static void removeOverlay(Activity activity) {
        if (activity == null) return;
        unmuteAudio(activity);
        View decor = activity.getWindow().getDecorView();
        if (decor instanceof ViewGroup) {
            View overlay = ((ViewGroup) decor).findViewWithTag(LOCK_OVERLAY_TAG);
            if (overlay != null) {
                ((ViewGroup) decor).removeView(overlay);
            }
        }
    }

    private static void showLockScreen(final Activity activity, final ViewGroup decorGroup) {
        muteAudio(activity);
        final FrameLayout overlay = new FrameLayout(activity) {
            @Override
            public boolean dispatchKeyEvent(android.view.KeyEvent event) {
                if (event.getKeyCode() == android.view.KeyEvent.KEYCODE_BACK) {
                    if (event.getAction() == android.view.KeyEvent.ACTION_UP) {
                        activity.moveTaskToBack(true);
                    }
                    return true;
                }
                return super.dispatchKeyEvent(event);
            }
        };
        overlay.setTag(LOCK_OVERLAY_TAG);
        int bg = Themes.isEnabled() ? Themes.backgroundInUse() : (Themes.isDark() ? 0xFF0E0E10 : 0xFFFFFFFF);
        overlay.setBackgroundColor(bg);
        overlay.setClickable(true);
        overlay.setFocusable(true);
        overlay.setFocusableInTouchMode(true);
        overlay.setFitsSystemWindows(true);
        overlay.requestFocus();

        final Runnable[] unregisterBack = new Runnable[1];
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                android.window.OnBackInvokedDispatcher dispatcher = activity.getOnBackInvokedDispatcher();
                android.window.OnBackInvokedCallback callback = () -> activity.moveTaskToBack(true);
                dispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY, callback);
                unregisterBack[0] = () -> {
                    try {
                        dispatcher.unregisterOnBackInvokedCallback(callback);
                    } catch (Throwable ignored) {}
                };
            } catch (Throwable ignored) {}
        }

        final LinearLayout centerBox = new LinearLayout(activity);
        centerBox.setOrientation(LinearLayout.VERTICAL);
        centerBox.setGravity(Gravity.CENTER);
        int pad = dp(activity, 24);
        centerBox.setPadding(pad, pad, pad, pad);

        ImageView logo = new ImageView(activity);
        int logoSize = dp(activity, 64);
        logo.setImageBitmap(Badges.note());
        centerBox.addView(logo, new LinearLayout.LayoutParams(logoSize, logoSize));

        final TextView title = new TextView(activity);
        title.setText("AirTok Заблокирован");
        title.setTextColor(Themes.isDark() ? 0xFFFFFFFF : 0xFF000000);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(activity, 16);
        centerBox.addView(title, tlp);

        final TextView subtitle = new TextView(activity);
        subtitle.setText("Введите 4-значный PIN-код");
        subtitle.setTextColor(0xFF8E8E93);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(activity, 6);
        slp.bottomMargin = dp(activity, 28);
        centerBox.addView(subtitle, slp);

        final LinearLayout dotsRow = new LinearLayout(activity);
        dotsRow.setOrientation(LinearLayout.HORIZONTAL);
        dotsRow.setGravity(Gravity.CENTER);
        final View[] dots = new View[4];
        final int dotEmptyColor = Themes.isDark() ? 0xFF3A3A3C : 0xFFD1D1D6;
        for (int i = 0; i < 4; i++) {
            dots[i] = new View(activity);
            int dotSize = dp(activity, 14);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dotSize, dotSize);
            dlp.setMargins(dp(activity, 10), 0, dp(activity, 10), 0);
            dots[i].setLayoutParams(dlp);
            updateDot(dots[i], false, dotEmptyColor);
            dotsRow.addView(dots[i]);
        }
        centerBox.addView(dotsRow);

        final StringBuilder entered = new StringBuilder();

        final Runnable refreshLockoutState = new Runnable() {
            @Override
            public void run() {
                long left = getLockoutUntil() - System.currentTimeMillis();
                if (left > 0) {
                    int sec = (int) (left / 1000) + 1;
                    subtitle.setText("Слишком много попыток. Подождите: " + sec + " сек.");
                    subtitle.setTextColor(0xFFFE2C55);
                } else {
                    subtitle.setText("Введите 4-значный PIN-код");
                    subtitle.setTextColor(0xFF8E8E93);
                }
            }
        };

        if (getLockoutUntil() > System.currentTimeMillis()) {
            new CountDownTimer(getLockoutUntil() - System.currentTimeMillis(), 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    refreshLockoutState.run();
                }

                @Override
                public void onFinish() {
                    resetFailAttempts();
                    refreshLockoutState.run();
                }
            }.start();
        }

        final View.OnClickListener digitListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelBiometric();
                if (System.currentTimeMillis() < getLockoutUntil()) {
                    shakeView(dotsRow);
                    return;
                }
                String d = (String) v.getTag();
                if (d == null || entered.length() >= 4) return;
                entered.append(d);
                for (int i = 0; i < 4; i++) {
                    updateDot(dots[i], i < entered.length(), Accent.accent());
                }

                if (entered.length() == 4) {
                    String pin = entered.toString();
                    if (verifyPin(pin)) {
                        if (unregisterBack[0] != null) unregisterBack[0].run();
                        unlock();
                        decorGroup.removeView(overlay);
                    } else {
                        incrementFailAttempts();
                        shakeView(dotsRow);
                        for (int i = 0; i < 4; i++) {
                            updateDot(dots[i], true, 0xFFFE2C55);
                        }
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                entered.setLength(0);
                                for (int i = 0; i < 4; i++) {
                                    updateDot(dots[i], false, dotEmptyColor);
                                }
                                refreshLockoutState.run();
                                if (getLockoutUntil() > System.currentTimeMillis()) {
                                    new CountDownTimer(getLockoutUntil() - System.currentTimeMillis(), 1000) {
                                        @Override
                                        public void onTick(long millisUntilFinished) {
                                            refreshLockoutState.run();
                                        }

                                        @Override
                                        public void onFinish() {
                                            resetFailAttempts();
                                            refreshLockoutState.run();
                                        }
                                    }.start();
                                }
                            }
                        }, 280);
                    }
                }
            }
        };

        final View.OnClickListener backspaceListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (System.currentTimeMillis() < getLockoutUntil()) return;
                if (entered.length() > 0) {
                    entered.deleteCharAt(entered.length() - 1);
                    for (int i = 0; i < 4; i++) {
                        updateDot(dots[i], i < entered.length(), Accent.accent());
                    }
                }
            }
        };

        final View.OnClickListener bioListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (System.currentTimeMillis() < getLockoutUntil()) {
                    shakeView(dotsRow);
                    return;
                }
                promptBiometric(activity, new Runnable() {
                    @Override
                    public void run() {
                        if (unregisterBack[0] != null) unregisterBack[0].run();
                        unlock();
                        decorGroup.removeView(overlay);
                    }
                });
            }
        };

        View keypad = createKeypad(activity, digitListener, backspaceListener, isBioEnabled() ? bioListener : null);
        LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        klp.topMargin = dp(activity, 32);
        centerBox.addView(keypad, klp);

        overlay.addView(centerBox, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        decorGroup.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (isBioEnabled() && System.currentTimeMillis() >= getLockoutUntil()) {
            overlay.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isUnlocked && overlay.getParent() != null && entered.length() == 0) {
                        promptBiometric(activity, new Runnable() {
                            @Override
                            public void run() {
                                if (unregisterBack[0] != null) unregisterBack[0].run();
                                unlock();
                                decorGroup.removeView(overlay);
                            }
                        });
                    }
                }
            }, 300);
        }
    }

    private static void updateDot(View dot, boolean filled, int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(filled ? color : Color.TRANSPARENT);
        gd.setStroke(2, filled ? color : 0xFF5A5A5E);
        dot.setBackground(gd);
    }

    private static void shakeView(final View view) {
        if (view == null) return;
        view.animate().translationXBy(24f).setDuration(40).withEndAction(new Runnable() {
            @Override
            public void run() {
                view.animate().translationXBy(-48f).setDuration(40).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        view.animate().translationXBy(48f).setDuration(40).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                view.animate().translationX(0f).setDuration(40).start();
                            }
                        }).start();
                    }
                }).start();
            }
        }).start();
    }

    private static View createKeypad(Context context,
                                     View.OnClickListener digitListener,
                                     View.OnClickListener backspaceListener,
                                     View.OnClickListener bioListener) {
        LinearLayout keypad = new LinearLayout(context);
        keypad.setOrientation(LinearLayout.VERTICAL);
        keypad.setGravity(Gravity.CENTER_HORIZONTAL);

        String[][] keys = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"bio", "0", "del"}
        };

        int btnSize = dp(context, 66);
        int margin = dp(context, 8);

        for (String[] row : keys) {
            LinearLayout rowLayout = new LinearLayout(context);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER_HORIZONTAL);

            for (String key : row) {
                FrameLayout btn = new FrameLayout(context);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(btnSize, btnSize);
                lp.setMargins(margin, margin, margin, margin);
                btn.setLayoutParams(lp);

                GradientDrawable base = new GradientDrawable();
                base.setShape(GradientDrawable.OVAL);
                base.setColor(Themes.isDark() ? 0xFF222228 : 0xFFE5E5EA);

                RippleDrawable rd = new RippleDrawable(
                        ColorStateList.valueOf(0x33FFFFFF),
                        base,
                        null);
                btn.setBackground(rd);
                btn.setClickable(true);
                btn.setFocusable(false);

                if (key.equals("bio")) {
                    if (bioListener != null) {
                        ImageView bioIcon = new ImageView(context);
                        bioIcon.setImageDrawable(AirTok.context().getDrawable(android.R.drawable.ic_dialog_info));
                        int p = dp(context, 20);
                        bioIcon.setPadding(p, p, p, p);
                        btn.addView(bioIcon);
                        btn.setOnClickListener(bioListener);
                    } else {
                        btn.setVisibility(View.INVISIBLE);
                    }
                } else if (key.equals("del")) {
                    TextView delTv = new TextView(context);
                    delTv.setText("⌫");
                    delTv.setTextColor(Themes.isDark() ? 0xFFFFFFFF : 0xFF000000);
                    delTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
                    delTv.setGravity(Gravity.CENTER);
                    delTv.setClickable(false);
                    btn.addView(delTv, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    btn.setOnClickListener(backspaceListener);
                } else {
                    TextView tv = new TextView(context);
                    tv.setText(key);
                    tv.setTextColor(Themes.isDark() ? 0xFFFFFFFF : 0xFF000000);
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
                    tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                    tv.setGravity(Gravity.CENTER);
                    tv.setClickable(false);
                    btn.addView(tv, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    btn.setTag(key);
                    btn.setOnClickListener(digitListener);
                }
                rowLayout.addView(btn);
            }
            keypad.addView(rowLayout);
        }
        return keypad;
    }

    public static void showSetupDialog(final Activity activity, final Runnable onDone) {
        final View decor = activity.getWindow().getDecorView();
        if (decor == null || !(decor instanceof ViewGroup)) return;
        final ViewGroup decorGroup = (ViewGroup) decor;

        final StringBuilder firstPin = new StringBuilder();
        final StringBuilder secondPin = new StringBuilder();
        final boolean[] isConfirmStage = new boolean[] { false };

        final FrameLayout overlay = new FrameLayout(activity);
        overlay.setBackgroundColor(Themes.isDark() ? 0xFF0E0E10 : 0xFFF2F2F7);
        overlay.setClickable(true);
        overlay.setFocusable(true);

        final LinearLayout centerBox = new LinearLayout(activity);
        centerBox.setOrientation(LinearLayout.VERTICAL);
        centerBox.setGravity(Gravity.CENTER);
        int pad = dp(activity, 24);
        centerBox.setPadding(pad, pad, pad, pad);

        final TextView title = new TextView(activity);
        title.setText("Установка PIN-кода");
        title.setTextColor(Themes.isDark() ? 0xFFFFFFFF : 0xFF000000);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        centerBox.addView(title);

        final TextView subtitle = new TextView(activity);
        subtitle.setText("Придумайте 4-значный PIN-код");
        subtitle.setTextColor(0xFF8E8E93);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(activity, 6);
        slp.bottomMargin = dp(activity, 28);
        centerBox.addView(subtitle, slp);

        final LinearLayout dotsRow = new LinearLayout(activity);
        dotsRow.setOrientation(LinearLayout.HORIZONTAL);
        dotsRow.setGravity(Gravity.CENTER);
        final View[] dots = new View[4];
        final int dotEmptyColor = Themes.isDark() ? 0xFF3A3A3C : 0xFFD1D1D6;
        for (int i = 0; i < 4; i++) {
            dots[i] = new View(activity);
            int dotSize = dp(activity, 14);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dotSize, dotSize);
            dlp.setMargins(dp(activity, 10), 0, dp(activity, 10), 0);
            dots[i].setLayoutParams(dlp);
            updateDot(dots[i], false, dotEmptyColor);
            dotsRow.addView(dots[i]);
        }
        centerBox.addView(dotsRow);

        final View.OnClickListener digitListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String d = (String) v.getTag();
                if (d == null) return;
                final StringBuilder current = isConfirmStage[0] ? secondPin : firstPin;
                if (current.length() >= 4) return;
                current.append(d);
                for (int i = 0; i < 4; i++) {
                    updateDot(dots[i], i < current.length(), 0xFFA855F7);
                }

                if (current.length() == 4) {
                    if (!isConfirmStage[0]) {
                        isConfirmStage[0] = true;
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                subtitle.setText("Повторите PIN-код для подтверждения");
                                for (int i = 0; i < 4; i++) {
                                    updateDot(dots[i], false, dotEmptyColor);
                                }
                            }
                        }, 200);
                    } else {
                        if (firstPin.toString().equals(secondPin.toString())) {
                            savePin(firstPin.toString());
                            unlock();
                            Toast.makeText(activity, "PIN-код успешно установлен!", Toast.LENGTH_SHORT).show();
                            decorGroup.removeView(overlay);
                            if (onDone != null) onDone.run();
                        } else {
                            shakeView(dotsRow);
                            Toast.makeText(activity, "PIN-коды не совпадают! Попробуйте снова.", Toast.LENGTH_SHORT).show();
                            firstPin.setLength(0);
                            secondPin.setLength(0);
                            isConfirmStage[0] = false;
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    subtitle.setText("Придумайте 4-значный PIN-код");
                                    for (int i = 0; i < 4; i++) {
                                        updateDot(dots[i], false, dotEmptyColor);
                                    }
                                }
                            }, 300);
                        }
                    }
                }
            }
        };

        final View.OnClickListener backspaceListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder current = isConfirmStage[0] ? secondPin : firstPin;
                if (current.length() > 0) {
                    current.deleteCharAt(current.length() - 1);
                    for (int i = 0; i < 4; i++) {
                        updateDot(dots[i], i < current.length(), 0xFFA855F7);
                    }
                }
            }
        };

        View keypad = createKeypad(activity, digitListener, backspaceListener, null);
        LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        klp.topMargin = dp(activity, 32);
        centerBox.addView(keypad, klp);

        TextView cancelBtn = new TextView(activity);
        cancelBtn.setText("Отмена");
        cancelBtn.setTextColor(0xFF8E8E93);
        cancelBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        cancelBtn.setPadding(dp(activity, 20), dp(activity, 12), dp(activity, 20), dp(activity, 12));
        cancelBtn.setClickable(true);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decorGroup.removeView(overlay);
            }
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = dp(activity, 16);
        centerBox.addView(cancelBtn, clp);

        overlay.addView(centerBox, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        decorGroup.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public static void promptVerifyPin(final Activity activity, final String promptTitle, final Runnable onCorrect) {
        final View decor = activity.getWindow().getDecorView();
        if (decor == null || !(decor instanceof ViewGroup)) return;
        final ViewGroup decorGroup = (ViewGroup) decor;

        final StringBuilder enteredPin = new StringBuilder();
        final FrameLayout overlay = new FrameLayout(activity);
        overlay.setBackgroundColor(Themes.isDark() ? 0xFF0E0E10 : 0xFFF2F2F7);
        overlay.setClickable(true);
        overlay.setFocusable(true);

        final LinearLayout centerBox = new LinearLayout(activity);
        centerBox.setOrientation(LinearLayout.VERTICAL);
        centerBox.setGravity(Gravity.CENTER);
        int pad = dp(activity, 24);
        centerBox.setPadding(pad, pad, pad, pad);

        final TextView title = new TextView(activity);
        title.setText(promptTitle != null ? promptTitle : "Подтверждение PIN-кода");
        title.setTextColor(Themes.isDark() ? 0xFFFFFFFF : 0xFF000000);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        centerBox.addView(title);

        final TextView subtitle = new TextView(activity);
        subtitle.setText("Введите текущий 4-значный PIN-код");
        subtitle.setTextColor(0xFF8E8E93);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(activity, 6);
        slp.bottomMargin = dp(activity, 28);
        centerBox.addView(subtitle, slp);

        final LinearLayout dotsRow = new LinearLayout(activity);
        dotsRow.setOrientation(LinearLayout.HORIZONTAL);
        dotsRow.setGravity(Gravity.CENTER);
        final View[] dots = new View[4];
        final int dotEmptyColor = Themes.isDark() ? 0xFF3A3A3C : 0xFFD1D1D6;
        for (int i = 0; i < 4; i++) {
            dots[i] = new View(activity);
            int dotSize = dp(activity, 14);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dotSize, dotSize);
            dlp.setMargins(dp(activity, 10), 0, dp(activity, 10), 0);
            dots[i].setLayoutParams(dlp);
            updateDot(dots[i], false, dotEmptyColor);
            dotsRow.addView(dots[i]);
        }
        centerBox.addView(dotsRow);

        final View.OnClickListener digitListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String d = (String) v.getTag();
                if (d == null || enteredPin.length() >= 4) return;
                enteredPin.append(d);
                for (int i = 0; i < 4; i++) {
                    updateDot(dots[i], i < enteredPin.length(), 0xFFA855F7);
                }
                if (enteredPin.length() == 4) {
                    if (verifyPin(enteredPin.toString())) {
                        decorGroup.removeView(overlay);
                        if (onCorrect != null) onCorrect.run();
                    } else {
                        shakeView(dotsRow);
                        Toast.makeText(activity, "Неверный PIN-код!", Toast.LENGTH_SHORT).show();
                        enteredPin.setLength(0);
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                for (int i = 0; i < 4; i++) {
                                    updateDot(dots[i], false, dotEmptyColor);
                                }
                            }
                        }, 250);
                    }
                }
            }
        };

        final View.OnClickListener backspaceListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (enteredPin.length() > 0) {
                    enteredPin.deleteCharAt(enteredPin.length() - 1);
                    for (int i = 0; i < 4; i++) {
                        updateDot(dots[i], i < enteredPin.length(), 0xFFA855F7);
                    }
                }
            }
        };

        View keypad = createKeypad(activity, digitListener, backspaceListener, null);
        LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        klp.topMargin = dp(activity, 32);
        centerBox.addView(keypad, klp);

        TextView cancelBtn = new TextView(activity);
        cancelBtn.setText("Отмена");
        cancelBtn.setTextColor(0xFF8E8E93);
        cancelBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        cancelBtn.setPadding(dp(activity, 20), dp(activity, 12), dp(activity, 20), dp(activity, 12));
        cancelBtn.setClickable(true);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decorGroup.removeView(overlay);
            }
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = dp(activity, 16);
        centerBox.addView(cancelBtn, clp);

        overlay.addView(centerBox, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        decorGroup.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private static void promptBiometric(final Activity activity, final Runnable onSuccess) {
        if (activity == null || activity.isFinishing()) return;
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                cancelBiometric();
                currentBioSignal = new CancellationSignal();
                BiometricPrompt.Builder b = new BiometricPrompt.Builder(activity);
                b.setTitle("AirTok");
                b.setSubtitle("Разблокировка приложения");
                b.setDescription("Подтвердите личность для доступа");
                b.setNegativeButton("Ввести PIN", activity.getMainExecutor(), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {}
                });

                BiometricPrompt prompt = b.build();
                prompt.authenticate(currentBioSignal, activity.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        currentBioSignal = null;
                        if (onSuccess != null) {
                            activity.runOnUiThread(onSuccess);
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        currentBioSignal = null;
                    }
                });
            } catch (Throwable ignored) {}
        }
    }
}

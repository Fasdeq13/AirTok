package com.fasdeq.airtok;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.biometrics.BiometricPrompt;
import android.media.AudioManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.security.MessageDigest;

public final class AppLock {

    private AppLock() {}

    public static final String KEY_ENABLED = "app_lock_enabled";
    public static final String KEY_PIN_HASH = "app_lock_pin_hash";
    public static final String KEY_PIN_SALT = "app_lock_pin_salt";
    public static final String KEY_BIOMETRIC = "app_lock_biometric";
    public static final String KEY_TIMEOUT = "app_lock_timeout";
    public static final String KEY_LAST_UNLOCKED_AT = "app_lock_last_unlocked_at";

    private static volatile boolean unlocked;
    private static volatile Dialog activeDialog;
    private static volatile boolean audioMuted;

    public static final class MaterialShapeView extends View {
        private final int shapeType;
        private boolean filled;
        private int currentColor;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        public MaterialShapeView(Context context, int shapeType) {
            super(context);
            this.shapeType = shapeType;
            this.currentColor = 0x55FFFFFF;
        }

        public void setFilled(boolean filled, int accentColor) {
            this.filled = filled;
            this.currentColor = filled ? accentColor : 0x55FFFFFF;
            invalidate();
        }

        public void setError() {
            this.filled = true;
            this.currentColor = 0xFFE5484D;
            invalidate();
        }

        public boolean isFilled() {
            return filled;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f;
            float dp = getResources().getDisplayMetrics().density;
            float r = (Math.min(w, h) / 2f) - (3f * dp);
            if (r <= 0) return;

            paint.reset();
            paint.setAntiAlias(true);
            if (filled) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(currentColor);
            } else {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2f * dp);
                paint.setColor(0x55FFFFFF);
            }

            if (shapeType == 0) {
                float petalR = r * 0.48f;
                float offset = r * 0.44f;
                path.reset();
                path.addCircle(cx + offset, cy, petalR, Path.Direction.CW);
                path.addCircle(cx - offset, cy, petalR, Path.Direction.CW);
                path.addCircle(cx, cy + offset, petalR, Path.Direction.CW);
                path.addCircle(cx, cy - offset, petalR, Path.Direction.CW);
                path.addCircle(cx, cy, petalR * 0.85f, Path.Direction.CW);
                canvas.drawPath(path, paint);
            } else if (shapeType == 1) {
                canvas.save();
                canvas.rotate(45f, cx, cy);
                float halfBox = r * 0.65f;
                RectF rect = new RectF(cx - halfBox, cy - halfBox, cx + halfBox, cy + halfBox);
                float corner = r * 0.26f;
                canvas.drawRoundRect(rect, corner, corner, paint);
                canvas.restore();
            } else if (shapeType == 2) {
                float half = r * 0.82f;
                RectF rect = new RectF(cx - half, cy - half, cx + half, cy + half);
                float corner = r * 0.44f;
                canvas.drawRoundRect(rect, corner, corner, paint);
            } else if (shapeType == 3) {
                path.reset();
                path.moveTo(cx, cy - r);
                path.quadTo(cx, cy, cx + r, cy);
                path.quadTo(cx, cy, cx, cy + r);
                path.quadTo(cx, cy, cx - r, cy);
                path.quadTo(cx, cy, cx, cy - r);
                path.close();
                canvas.drawPath(path, paint);
            } else {
                canvas.drawCircle(cx, cy, r * 0.75f, paint);
            }
        }
    }

    private static void animateShape(final MaterialShapeView shape, boolean fill, final int accent) {
        if (fill) {
            shape.setFilled(true, accent);
            shape.setScaleX(0.2f);
            shape.setScaleY(0.2f);
            shape.setAlpha(0.2f);
            shape.animate()
                    .scaleX(1.22f)
                    .scaleY(1.22f)
                    .alpha(1.0f)
                    .setDuration(160)
                    .withEndAction(() -> shape.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start())
                    .start();
        } else {
            shape.animate()
                    .scaleX(0.35f)
                    .scaleY(0.35f)
                    .alpha(0.2f)
                    .setDuration(120)
                    .withEndAction(() -> {
                        shape.setFilled(false, accent);
                        shape.setScaleX(1.0f);
                        shape.setScaleY(1.0f);
                        shape.setAlpha(1.0f);
                    })
                    .start();
        }
    }

    private interface KeypadListener {
        void onKey(String val);
    }

    private static LinearLayout createKeypad(final Activity activity, float dp, int accent,
                                             final KeypadListener listener, boolean includeBiometrics,
                                             final Runnable onBio) {
        LinearLayout keypad = new LinearLayout(activity);
        keypad.setOrientation(LinearLayout.VERTICAL);
        keypad.setGravity(Gravity.CENTER);

        int keySize = Math.round(68 * dp);
        int keyGap = Math.round(12 * dp);

        String[][] matrix = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {includeBiometrics ? "bio" : "", "0", "del"}
        };

        for (int r = 0; r < 4; r++) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            row.setPadding(0, Math.round(4 * dp), 0, Math.round(4 * dp));

            for (int c = 0; c < 3; c++) {
                final String val = matrix[r][c];
                final TextView key = new TextView(activity);
                key.setGravity(Gravity.CENTER);

                GradientDrawable keyBg = new GradientDrawable();
                keyBg.setShape(GradientDrawable.RECTANGLE);
                keyBg.setCornerRadius(keySize / 2f);
                keyBg.setColor(0x18FFFFFF);
                key.setBackground(keyBg);

                if ("bio".equals(val)) {
                    if (isBiometricEnabled(activity) && Build.VERSION.SDK_INT >= 28) {
                        final ImageView bioView = new ImageView(activity);
                        bioView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        int pad = Math.round(18 * dp);
                        bioView.setPadding(pad, pad, pad, pad);
                        bioView.setImageDrawable(SettingsGlyph.make("fingerprint", accent));
                        bioView.setBackground(keyBg);
                        bioView.setOnClickListener(v -> {
                            if (onBio != null) onBio.run();
                        });
                        LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(keySize, keySize);
                        kp.setMargins(keyGap, 0, keyGap, 0);
                        row.addView(bioView, kp);
                        continue;
                    } else {
                        keyBg.setColor(0x00000000);
                        key.setText("");
                        key.setClickable(false);
                    }
                } else if ("del".equals(val)) {
                    key.setText("⌫");
                    key.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                    key.setTextColor(0xFFFFFFFF);
                    key.setOnClickListener(v -> {
                        if (listener != null) listener.onKey("del");
                    });
                } else if (val.isEmpty()) {
                    keyBg.setColor(0x00000000);
                    key.setText("");
                    key.setClickable(false);
                } else {
                    key.setText(val);
                    key.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                    key.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                    key.setTextColor(0xFFFFFFFF);
                    key.setOnClickListener(v -> {
                        if (listener != null) listener.onKey(val);
                    });
                }

                LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(keySize, keySize);
                kp.setMargins(keyGap, 0, keyGap, 0);
                row.addView(key, kp);
            }
            keypad.addView(row);
        }
        return keypad;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(AirTok.PREFS, Context.MODE_PRIVATE);
    }

    public static boolean hasPin(Context context) {
        if (context == null) return false;
        migrateLegacy(context);
        String hash = prefs(context).getString(KEY_PIN_HASH, "");
        return hash != null && hash.length() > 0;
    }

    public static boolean isEnabled(Context context) {
        if (context == null) return false;
        return hasPin(context) && prefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean on) {
        if (context == null) return;
        prefs(context).edit().putBoolean(KEY_ENABLED, on).apply();
        if (!on) unlocked = true;
    }

    public static boolean isBiometricEnabled(Context context) {
        if (context == null) return false;
        return prefs(context).getBoolean(KEY_BIOMETRIC, false);
    }

    public static void setBiometricEnabled(Context context, boolean on) {
        if (context == null) return;
        prefs(context).edit().putBoolean(KEY_BIOMETRIC, on).apply();
    }

    public static int getTimeout(Context context) {
        if (context == null) return 0;
        return prefs(context).getInt(KEY_TIMEOUT, 0);
    }

    public static void setTimeout(Context context, int minutes) {
        if (context == null) return;
        prefs(context).edit().putInt(KEY_TIMEOUT, minutes).commit();
        if (unlocked) markUnlocked(context);
    }

    public static long getTimeoutMillis(Context context) {
        return getTimeout(context) * 60 * 1000L;
    }

    public static void markUnlocked(Context context) {
        unlocked = true;
        if (context == null) return;
        prefs(context).edit().putLong(KEY_LAST_UNLOCKED_AT, System.currentTimeMillis()).commit();
    }

    public static void onAppBackground(Context context) {
        if (context == null) return;
        if (unlocked) {
            prefs(context).edit().putLong(KEY_LAST_UNLOCKED_AT, System.currentTimeMillis()).commit();
            if (getTimeout(context) == 0) {
                unlocked = false;
            }
        }
    }

    public static boolean isWithinTimeout(Context context) {
        if (context == null) return false;
        int minutes = getTimeout(context);
        if (minutes <= 0) return false;
        long last = prefs(context).getLong(KEY_LAST_UNLOCKED_AT, 0L);
        if (last <= 0L) return false;
        long elapsed = System.currentTimeMillis() - last;
        return elapsed >= 0L && elapsed < minutes * 60_000L;
    }

    public static void lock() {
        unlocked = false;
    }

    public static boolean isUnlocked() {
        return unlocked;
    }

    public static final String KEY_FAIL_COUNT = "app_lock_fail_count";
    public static final String KEY_LOCKOUT_UNTIL = "app_lock_lockout_until";
    private static final String KEY_STORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "AirTokAppLockHmacKey";
    private static final String BIO_KEY_ALIAS = "AirTokAppLockBioKey";

    private static javax.crypto.SecretKey getOrCreateHmacKey() {
        try {
            java.security.KeyStore ks = java.security.KeyStore.getInstance(KEY_STORE_PROVIDER);
            ks.load(null);
            if (!ks.containsAlias(KEY_ALIAS)) {
                if (Build.VERSION.SDK_INT >= 23) {
                    javax.crypto.KeyGenerator kg = javax.crypto.KeyGenerator.getInstance(
                            android.security.keystore.KeyProperties.KEY_ALGORITHM_HMAC_SHA256, KEY_STORE_PROVIDER);
                    kg.init(new android.security.keystore.KeyGenParameterSpec.Builder(
                            KEY_ALIAS,
                            android.security.keystore.KeyProperties.PURPOSE_SIGN)
                            .build());
                    return kg.generateKey();
                }
            } else {
                java.security.KeyStore.Entry entry = ks.getEntry(KEY_ALIAS, null);
                if (entry instanceof java.security.KeyStore.SecretKeyEntry) {
                    return ((java.security.KeyStore.SecretKeyEntry) entry).getSecretKey();
                }
            }
        } catch (Throwable t) {
            Diary.note("keystore hmac key: " + t);
        }
        return null;
    }

    public static void initBiometricKey() {
        if (Build.VERSION.SDK_INT < 23) return;
        try {
            java.security.KeyStore ks = java.security.KeyStore.getInstance(KEY_STORE_PROVIDER);
            ks.load(null);
            if (ks.containsAlias(BIO_KEY_ALIAS)) {
                ks.deleteEntry(BIO_KEY_ALIAS);
            }
            javax.crypto.KeyGenerator kg = javax.crypto.KeyGenerator.getInstance(
                    android.security.keystore.KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_PROVIDER);
            android.security.keystore.KeyGenParameterSpec.Builder builder =
                    new android.security.keystore.KeyGenParameterSpec.Builder(
                            BIO_KEY_ALIAS,
                            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT | android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_CBC)
                            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_PKCS7)
                            .setUserAuthenticationRequired(true);
            if (Build.VERSION.SDK_INT >= 24) {
                builder.setInvalidatedByBiometricEnrollment(true);
            }
            kg.init(builder.build());
            kg.generateKey();
        } catch (Throwable t) {
            Diary.note("bio key init: " + t);
        }
    }

    private static javax.crypto.Cipher getBiometricCipher(boolean[] invalidatedOut) {
        if (Build.VERSION.SDK_INT < 23) return null;
        try {
            java.security.KeyStore ks = java.security.KeyStore.getInstance(KEY_STORE_PROVIDER);
            ks.load(null);
            if (!ks.containsAlias(BIO_KEY_ALIAS)) {
                initBiometricKey();
            }
            javax.crypto.SecretKey key = (javax.crypto.SecretKey) ks.getKey(BIO_KEY_ALIAS, null);
            if (key == null) return null;
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key);
            return cipher;
        } catch (Throwable t) {
            if (Build.VERSION.SDK_INT >= 23 && t instanceof android.security.keystore.KeyPermanentlyInvalidatedException) {
                if (invalidatedOut != null && invalidatedOut.length > 0) invalidatedOut[0] = true;
                try {
                    java.security.KeyStore ks = java.security.KeyStore.getInstance(KEY_STORE_PROVIDER);
                    ks.load(null);
                    if (ks.containsAlias(BIO_KEY_ALIAS)) ks.deleteEntry(BIO_KEY_ALIAS);
                } catch (Throwable ignored) {}
            }
            return null;
        }
    }

    private static boolean isBiometricInvalidated(Context context) {
        if (Build.VERSION.SDK_INT < 24 || !isBiometricEnabled(context)) return false;
        boolean[] invalidated = new boolean[1];
        getBiometricCipher(invalidated);
        if (invalidated[0]) {
            setBiometricEnabled(context, false);
            return true;
        }
        return false;
    }

    private static String hash(String pin, String salt) {
        byte[] seed = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            seed = (pin + ":" + salt + ":" + Build.FINGERPRINT + ":" + Build.ID).getBytes("UTF-8");
            for (int i = 0; i < 5000; i++) {
                seed = md.digest(seed);
            }
            javax.crypto.SecretKey secretKey = getOrCreateHmacKey();
            if (secretKey != null && Build.VERSION.SDK_INT >= 23) {
                try {
                    javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
                    mac.init(secretKey);
                    seed = mac.doFinal(seed);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                seed = md.digest((pin + ":" + salt).getBytes("UTF-8"));
            } catch (Throwable ignored) {
                return pin;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : seed) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static long getLockoutRemaining(Context context) {
        if (context == null) return 0L;
        long until = prefs(context).getLong(KEY_LOCKOUT_UNTIL, 0L);
        long now = System.currentTimeMillis();
        return Math.max(0L, until - now);
    }

    public static int getFailCount(Context context) {
        if (context == null) return 0;
        return prefs(context).getInt(KEY_FAIL_COUNT, 0);
    }

    private static long lockoutDurationFor(int fails) {
        if (fails < 5) return 0L;
        if (fails == 5) return 60_000L;
        if (fails == 6) return 300_000L;
        if (fails == 7) return 600_000L;
        if (fails == 8) return 1_800_000L;
        return 3_600_000L;
    }

    public static void recordFailedAttempt(Context context) {
        if (context == null) return;
        int fails = getFailCount(context) + 1;
        long duration = lockoutDurationFor(fails);
        SharedPreferences.Editor edit = prefs(context).edit();
        edit.putInt(KEY_FAIL_COUNT, fails);
        if (duration > 0) {
            edit.putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + duration);
        }
        edit.apply();
    }

    public static void resetFailedAttempts(Context context) {
        if (context == null) return;
        prefs(context).edit().remove(KEY_FAIL_COUNT).remove(KEY_LOCKOUT_UNTIL).apply();
    }

    public static String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + " сек.";
        }
        if (seconds < 3600) {
            long min = seconds / 60;
            long sec = seconds % 60;
            return min + " мин. " + sec + " сек.";
        }
        long hr = seconds / 3600;
        long rem = seconds % 3600;
        long min = rem / 60;
        long sec = rem % 60;
        return hr + " ч. " + min + " мин. " + sec + " сек.";
    }

    private static void migrateLegacy(Context context) {
        SharedPreferences p = prefs(context);
        if (p.contains("app_lock_pin")) {
            String old = p.getString("app_lock_pin", "");
            if (old != null && old.length() > 0) {
                String salt = String.valueOf(System.currentTimeMillis());
                p.edit().putString(KEY_PIN_HASH, hash(old, salt))
                        .putString(KEY_PIN_SALT, salt)
                        .remove("app_lock_pin")
                        .apply();
            }
        }
    }

    public static void savePin(Context context, String pin) {
        if (context == null || pin == null) return;
        resetFailedAttempts(context);
        String salt = String.valueOf(System.currentTimeMillis());
        prefs(context).edit().putString(KEY_PIN_HASH, hash(pin, salt))
                .putString(KEY_PIN_SALT, salt)
                .putBoolean(KEY_ENABLED, true)
                .commit();
        if (isBiometricEnabled(context)) {
            initBiometricKey();
        }
        unlocked = true;
    }

    public static boolean checkPin(Context context, String enteredPin) {
        if (context == null || enteredPin == null) return false;
        migrateLegacy(context);
        SharedPreferences p = prefs(context);
        String savedHash = p.getString(KEY_PIN_HASH, "");
        String salt = p.getString(KEY_PIN_SALT, "");
        if (savedHash == null || savedHash.isEmpty()) return false;
        return savedHash.equals(hash(enteredPin, salt));
    }

    public static void deletePin(Context context) {
        if (context == null) return;
        resetFailedAttempts(context);
        prefs(context).edit().remove(KEY_PIN_HASH)
                .remove(KEY_PIN_SALT)
                .remove(KEY_ENABLED)
                .remove(KEY_BIOMETRIC)
                .apply();
        unlocked = true;
    }

    public static void mute(Context context) {
        muteAudio(context);
    }

    private static void muteAudio(Context context) {
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                if (Build.VERSION.SDK_INT >= 23) {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                } else {
                    am.setStreamMute(AudioManager.STREAM_MUSIC, true);
                }
                audioMuted = true;
            }
        } catch (Throwable ignored) {}
    }

    private static void unmuteAudio(Context context) {
        try {
            if (!audioMuted) return;
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                if (Build.VERSION.SDK_INT >= 23) {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
                } else {
                    am.setStreamMute(AudioManager.STREAM_MUSIC, false);
                }
                audioMuted = false;
            }
        } catch (Throwable ignored) {}
    }

    public static void check(final Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        Context context = activity.getApplicationContext();
        if (!isEnabled(context)) {
            unlocked = true;
            unmuteAudio(context);
            return;
        }
        if (unlocked) {
            unmuteAudio(context);
            return;
        }
        if (isWithinTimeout(context)) {
            unlocked = true;
            unmuteAudio(context);
            return;
        }
        if (activeDialog != null) {
            if (activeDialog.isShowing()) {
                Activity owner = activeDialog.getOwnerActivity();
                if (owner == activity) return;
                try { activeDialog.dismiss(); } catch (Throwable ignored) {}
            }
            activeDialog = null;
        }

        muteAudio(activity);
        activity.runOnUiThread(() -> showLockDialog(activity));
    }

    private static void showLockDialog(final Activity activity) {
        if (activity.isFinishing()) return;

        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOwnerActivity(activity);
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.getAction() == android.view.KeyEvent.ACTION_UP) {
                activity.finishAffinity();
                return true;
            }
            return false;
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(0xFF101014));
        }

        final float dp = activity.getResources().getDisplayMetrics().density;
        final int accent = Accent.colour();

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(0xFF101014);
        root.setPadding(Math.round(24 * dp), Math.round(36 * dp), Math.round(24 * dp), Math.round(24 * dp));

        ImageView logo = new ImageView(activity);
        int logoSize = Math.round(68 * dp);
        try {
            byte[] png = Base64.decode(Emblem.PNG, Base64.DEFAULT);
            Bitmap note = BitmapFactory.decodeByteArray(png, 0, png.length);
            if (note != null) {
                logo.setImageDrawable(new BitmapDrawable(activity.getResources(), Badge.round(note, 0.28f)));
            }
        } catch (Throwable ignored) {}
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(logoSize, logoSize);
        logoParams.bottomMargin = Math.round(16 * dp);
        root.addView(logo, logoParams);

        TextView appTitle = new TextView(activity);
        appTitle.setText("AirTok");
        appTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        appTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        appTitle.setTextColor(0xFFFFFFFF);
        appTitle.setGravity(Gravity.CENTER);
        root.addView(appTitle);

        final TextView promptText = new TextView(activity);
        promptText.setText("Введите PIN-код для входа");
        promptText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        promptText.setTextColor(0xFF888894);
        promptText.setGravity(Gravity.CENTER);
        promptText.setPadding(0, Math.round(6 * dp), 0, Math.round(28 * dp));
        root.addView(promptText);

        final LinearLayout shapesRow = new LinearLayout(activity);
        shapesRow.setOrientation(LinearLayout.HORIZONTAL);
        shapesRow.setGravity(Gravity.CENTER);
        shapesRow.setPadding(0, 0, 0, Math.round(32 * dp));

        final MaterialShapeView[] shapes = new MaterialShapeView[4];
        int shapeSize = Math.round(26 * dp);
        int shapeMargin = Math.round(12 * dp);
        for (int i = 0; i < 4; i++) {
            MaterialShapeView shape = new MaterialShapeView(activity, i);
            shape.setFilled(false, accent);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(shapeSize, shapeSize);
            lp.setMargins(shapeMargin, 0, shapeMargin, 0);
            shapesRow.addView(shape, lp);
            shapes[i] = shape;
        }
        root.addView(shapesRow);

        final boolean[] isLockedOut = new boolean[1];
        final boolean[] bioChanged = new boolean[]{isBiometricInvalidated(activity)};
        if (bioChanged[0]) {
            Toast.makeText(activity, "Биометрия на устройстве изменена. Введите PIN и создайте пароль заново", Toast.LENGTH_LONG).show();
        }
        final Runnable[] updateLockout = new Runnable[1];
        updateLockout[0] = () -> {
            long remaining = getLockoutRemaining(activity);
            if (remaining > 0) {
                isLockedOut[0] = true;
                long sec = (remaining + 999L) / 1000L;
                promptText.setText("Слишком много попыток, попробуйте через: " + formatDuration(sec));
                promptText.setTextColor(0xFFE5484D);
                promptText.postDelayed(updateLockout[0], 1000L);
            } else {
                if (isLockedOut[0]) {
                    isLockedOut[0] = false;
                    resetFailedAttempts(activity);
                }
                if (bioChanged[0]) {
                    promptText.setText("Биометрия изменилась. Введите PIN и создайте пароль заново");
                    promptText.setTextColor(0xFFFF9800);
                } else {
                    promptText.setText("Введите PIN-код для входа");
                    promptText.setTextColor(0xFF888894);
                }
            }
        };
        updateLockout[0].run();

        final StringBuilder entered = new StringBuilder();

        final Runnable tryUnlock = () -> {
            if (isLockedOut[0]) return;
            if (checkPin(activity, entered.toString())) {
                resetFailedAttempts(activity);
                markUnlocked(activity);
                unmuteAudio(activity);
                dialog.dismiss();
                activeDialog = null;
                if (bioChanged[0]) {
                    promptSetPin(activity, null);
                }
            } else {
                recordFailedAttempt(activity);
                long rem = getLockoutRemaining(activity);
                if (rem > 0) {
                    updateLockout[0].run();
                } else {
                    int remainingTries = Math.max(1, 5 - getFailCount(activity));
                    Toast.makeText(activity, "Неверный PIN-код (осталось попыток: " + remainingTries + ")", Toast.LENGTH_SHORT).show();
                }
                for (int i = 0; i < 4; i++) {
                    shapes[i].setError();
                }
                android.animation.ObjectAnimator.ofFloat(shapesRow, "translationX", 0f, -22f, 22f, -16f, 16f, -8f, 8f, 0f).setDuration(360).start();
                shapesRow.postDelayed(() -> {
                    entered.setLength(0);
                    for (int i = 0; i < 4; i++) {
                        shapes[i].setFilled(false, accent);
                        shapes[i].setScaleX(1.0f);
                        shapes[i].setScaleY(1.0f);
                        shapes[i].setAlpha(1.0f);
                    }
                }, 380L);
            }
        };

        final Runnable triggerBio = () -> {
            if (bioChanged[0]) {
                Toast.makeText(activity, "Биометрия изменилась. Введите PIN-код и создайте пароль заново", Toast.LENGTH_LONG).show();
                return;
            }
            triggerBiometric(activity, () -> {
                if (isLockedOut[0]) return;
                resetFailedAttempts(activity);
                markUnlocked(activity);
                unmuteAudio(activity);
                dialog.dismiss();
                activeDialog = null;
            }, () -> {
                bioChanged[0] = true;
                setBiometricEnabled(activity, false);
                promptText.setText("Биометрия изменилась. Введите PIN и создайте пароль заново");
                promptText.setTextColor(0xFFFF9800);
                Toast.makeText(activity, "Биометрия была изменена. Создайте пароль заново после ввода PIN", Toast.LENGTH_LONG).show();
            });
        };

        LinearLayout keypad = createKeypad(activity, dp, accent, val -> {
            if (isLockedOut[0]) return;
            if ("del".equals(val)) {
                if (entered.length() > 0) {
                    int idx = entered.length() - 1;
                    entered.deleteCharAt(idx);
                    animateShape(shapes[idx], false, accent);
                }
            } else if (entered.length() < 4) {
                int idx = entered.length();
                entered.append(val);
                animateShape(shapes[idx], true, accent);
                if (entered.length() == 4) {
                    shapesRow.post(tryUnlock);
                }
            }
        }, true, triggerBio);
        root.addView(keypad);

        TextView exitBtn = new TextView(activity);
        exitBtn.setText("Выход");
        exitBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        exitBtn.setTextColor(0xFF888894);
        exitBtn.setGravity(Gravity.CENTER);
        exitBtn.setPadding(0, Math.round(24 * dp), 0, Math.round(12 * dp));
        exitBtn.setOnClickListener(v -> activity.finishAffinity());
        root.addView(exitBtn);

        dialog.setContentView(root);
        dialog.setOnDismissListener(d -> promptText.removeCallbacks(updateLockout[0]));
        activeDialog = dialog;
        dialog.show();

        if (isBiometricEnabled(activity) && !bioChanged[0] && Build.VERSION.SDK_INT >= 28) {
            triggerBio.run();
        }
    }

    private static void triggerBiometric(final Activity activity, final Runnable onPass, final Runnable onInvalidated) {
        if (Build.VERSION.SDK_INT < 28 || activity == null || activity.isFinishing()) return;
        try {
            boolean[] invalidated = new boolean[1];
            javax.crypto.Cipher cipher = getBiometricCipher(invalidated);
            if (invalidated[0]) {
                if (onInvalidated != null) onInvalidated.run();
                return;
            }
            BiometricPrompt prompt = new BiometricPrompt.Builder(activity)
                    .setTitle("AirTok")
                    .setSubtitle("Вход по биометрии")
                    .setDescription("Подтвердите личность для входа")
                    .setNegativeButton("Ввести PIN", activity.getMainExecutor(), (dialog, which) -> {})
                    .build();

            CancellationSignal signal = new CancellationSignal();
            BiometricPrompt.AuthenticationCallback cb = new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    if (onPass != null) onPass.run();
                }

                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                }
            };
            if (cipher != null) {
                prompt.authenticate(new BiometricPrompt.CryptoObject(cipher), signal, activity.getMainExecutor(), cb);
            } else {
                prompt.authenticate(signal, activity.getMainExecutor(), cb);
            }
        } catch (Throwable ignored) {}
    }

    public static void promptVerifyPin(final Activity activity, final Runnable onSuccess, final Runnable onCancel) {
        if (activity == null || activity.isFinishing()) return;
        promptPinInput(activity, "Подтвердите личность", "Введите PIN-код или используйте отпечаток", pin -> {
            if (checkPin(activity, pin)) {
                resetFailedAttempts(activity);
                if (onSuccess != null) onSuccess.run();
            } else {
                Toast.makeText(activity, "Неверный PIN-код", Toast.LENGTH_SHORT).show();
                if (onCancel != null) onCancel.run();
            }
        }, null, onCancel, () -> {
            resetFailedAttempts(activity);
            if (onSuccess != null) onSuccess.run();
        });
    }

    public static void promptSetPin(final Activity activity, final Runnable onDone) {
        promptPinInput(activity, "Создайте PIN-код", "Введите 4 цифры для входа", null, onDone, null, null);
    }

    public static void promptChangePin(final Activity activity, final Runnable onDone) {
        promptPinInput(activity, "Текущий пароль", "Введите PIN-код или приложите палец", oldPin -> {
            if (!checkPin(activity, oldPin)) {
                Toast.makeText(activity, "Неверный текущий PIN-код", Toast.LENGTH_SHORT).show();
                return;
            }
            promptPinInput(activity, "Новый PIN-код", "Введите новый PIN-код", null, onDone, null, null);
        }, null, null, () -> promptPinInput(activity, "Новый PIN-код", "Введите новый PIN-код", null, onDone, null, null));
    }

    public static void promptDeletePin(final Activity activity, final Runnable onDone) {
        promptPinInput(activity, "Удаление PIN-кода", "Введите PIN-код или приложите палец", pin -> {
            if (checkPin(activity, pin)) {
                deletePin(activity);
                Toast.makeText(activity, "PIN-код удален", Toast.LENGTH_SHORT).show();
                if (onDone != null) onDone.run();
            } else {
                Toast.makeText(activity, "Неверный PIN-код", Toast.LENGTH_SHORT).show();
            }
        }, null, null, () -> {
            deletePin(activity);
            Toast.makeText(activity, "PIN-код удален", Toast.LENGTH_SHORT).show();
            if (onDone != null) onDone.run();
        });
    }

    private interface PinCallback {
        void onPinEntered(String pin);
    }

    private static void promptPinInput(final Activity activity, final String initialTitle, final String initialSubtitle,
                                       final PinCallback onCustom, final Runnable onDone, final Runnable onCancel,
                                       final Runnable onBioSuccess) {
        if (activity == null || activity.isFinishing()) return;

        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setCancelable(true);
        final boolean[] completed = new boolean[1];
        dialog.setOnCancelListener(d -> {
            if (!completed[0] && onCancel != null) onCancel.run();
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(0xFF101014));
        }

        final float dp = activity.getResources().getDisplayMetrics().density;
        final int accent = Accent.colour();

        FrameLayout rootFrame = new FrameLayout(activity);
        rootFrame.setBackgroundColor(0xFF101014);

        TextView cancelBtn = new TextView(activity);
        cancelBtn.setText("Отмена");
        cancelBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        cancelBtn.setTextColor(0xFFA0A0AB);
        cancelBtn.setPadding(Math.round(20 * dp), Math.round(28 * dp), Math.round(20 * dp), Math.round(16 * dp));
        FrameLayout.LayoutParams cancelLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cancelLp.gravity = Gravity.TOP | Gravity.START;
        cancelBtn.setLayoutParams(cancelLp);
        cancelBtn.setOnClickListener(v -> {
            dialog.dismiss();
            if (!completed[0] && onCancel != null) onCancel.run();
        });
        rootFrame.addView(cancelBtn);

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams contentLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        contentLp.gravity = Gravity.CENTER;
        content.setLayoutParams(contentLp);
        content.setPadding(Math.round(24 * dp), Math.round(16 * dp), Math.round(24 * dp), Math.round(16 * dp));

        ImageView logo = new ImageView(activity);
        int logoSize = Math.round(64 * dp);
        try {
            byte[] png = Base64.decode(Emblem.PNG, Base64.DEFAULT);
            Bitmap note = BitmapFactory.decodeByteArray(png, 0, png.length);
            if (note != null) {
                logo.setImageDrawable(new BitmapDrawable(activity.getResources(), Badge.round(note, 0.28f)));
            }
        } catch (Throwable ignored) {}
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(logoSize, logoSize);
        logoParams.bottomMargin = Math.round(16 * dp);
        content.addView(logo, logoParams);

        final TextView tvTitle = new TextView(activity);
        tvTitle.setText(initialTitle);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        tvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setGravity(Gravity.CENTER);
        content.addView(tvTitle);

        final TextView tvSub = new TextView(activity);
        tvSub.setText(initialSubtitle);
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvSub.setTextColor(0xFF888894);
        tvSub.setGravity(Gravity.CENTER);
        tvSub.setPadding(0, Math.round(6 * dp), 0, Math.round(28 * dp));
        content.addView(tvSub);

        final LinearLayout shapesRow = new LinearLayout(activity);
        shapesRow.setOrientation(LinearLayout.HORIZONTAL);
        shapesRow.setGravity(Gravity.CENTER);
        shapesRow.setPadding(0, 0, 0, Math.round(32 * dp));

        final MaterialShapeView[] shapes = new MaterialShapeView[4];
        int shapeSize = Math.round(26 * dp);
        int shapeMargin = Math.round(12 * dp);
        for (int i = 0; i < 4; i++) {
            MaterialShapeView shape = new MaterialShapeView(activity, i);
            shape.setFilled(false, accent);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(shapeSize, shapeSize);
            lp.setMargins(shapeMargin, 0, shapeMargin, 0);
            shapesRow.addView(shape, lp);
            shapes[i] = shape;
        }
        content.addView(shapesRow);

        final StringBuilder entered = new StringBuilder();
        final String[] firstAttempt = new String[1];

        final boolean bioAllowed = onBioSuccess != null && isBiometricEnabled(activity)
                && !isBiometricInvalidated(activity) && Build.VERSION.SDK_INT >= 28;
        final Runnable triggerBio = !bioAllowed ? null : () -> triggerBiometric(activity, () -> {
            completed[0] = true;
            dialog.dismiss();
            onBioSuccess.run();
        }, () -> {
            Toast.makeText(activity, "Биометрия изменилась. Подтвердите PIN-кодом", Toast.LENGTH_LONG).show();
        });

        LinearLayout keypad = createKeypad(activity, dp, accent, val -> {
            if ("del".equals(val)) {
                if (entered.length() > 0) {
                    int idx = entered.length() - 1;
                    entered.deleteCharAt(idx);
                    animateShape(shapes[idx], false, accent);
                }
            } else if (entered.length() < 4) {
                int idx = entered.length();
                entered.append(val);
                animateShape(shapes[idx], true, accent);

                if (entered.length() == 4) {
                    final String pin = entered.toString();
                    if (onCustom != null) {
                        completed[0] = true;
                        shapesRow.postDelayed(() -> {
                            dialog.dismiss();
                            onCustom.onPinEntered(pin);
                        }, 180L);
                    } else if (firstAttempt[0] == null) {
                        firstAttempt[0] = pin;
                        shapesRow.postDelayed(() -> {
                            entered.setLength(0);
                            for (int i = 0; i < 4; i++) {
                                shapes[i].setFilled(false, accent);
                                shapes[i].setScaleX(1.0f);
                                shapes[i].setScaleY(1.0f);
                                shapes[i].setAlpha(1.0f);
                            }
                            tvTitle.setText("Повторите PIN-код");
                            tvSub.setText("Введите 4 цифры ещё раз для подтверждения");
                        }, 200L);
                    } else {
                        if (firstAttempt[0].equals(pin)) {
                            savePin(activity, pin);
                            Toast.makeText(activity, "PIN-код успешно установлен", Toast.LENGTH_SHORT).show();
                            shapesRow.postDelayed(() -> {
                                dialog.dismiss();
                                if (onDone != null) onDone.run();
                            }, 180L);
                        } else {
                            Toast.makeText(activity, "PIN-коды не совпадают", Toast.LENGTH_SHORT).show();
                            for (int i = 0; i < 4; i++) {
                                shapes[i].setError();
                            }
                            android.animation.ObjectAnimator.ofFloat(shapesRow, "translationX", 0f, -22f, 22f, -16f, 16f, -8f, 8f, 0f).setDuration(360).start();
                            shapesRow.postDelayed(() -> {
                                entered.setLength(0);
                                firstAttempt[0] = null;
                                for (int i = 0; i < 4; i++) {
                                    shapes[i].setFilled(false, accent);
                                    shapes[i].setScaleX(1.0f);
                                    shapes[i].setScaleY(1.0f);
                                    shapes[i].setAlpha(1.0f);
                                }
                                tvTitle.setText(initialTitle);
                                tvSub.setText(initialSubtitle);
                            }, 400L);
                        }
                    }
                }
            }
        }, bioAllowed, triggerBio);
        content.addView(keypad);

        rootFrame.addView(content);
        dialog.setContentView(rootFrame);
        dialog.show();
        if (bioAllowed && triggerBio != null) {
            triggerBio.run();
        }
    }
}

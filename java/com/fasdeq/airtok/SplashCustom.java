package com.fasdeq.airtok;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class SplashCustom {

    private SplashCustom() {}

    public static final int PICK = 0x4D53;
    private static final String FILE_NAME = "custom_splash.png";
    private static final int OVERLAY_TAG = 0x53504C53;
    private static volatile boolean mainShown;

    public static File file(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }

    public static boolean hasCustom(Context context) {
        if (context == null) return false;
        File f = file(context);
        return f.exists() && f.length() > 0;
    }

    public static void reset(Context context) {
        if (context == null) return;
        File f = file(context);
        if (f.exists()) f.delete();
    }

    public static void pick(Activity activity) {
        if (activity == null) return;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            activity.startActivityForResult(intent, PICK);
        } catch (Throwable error) {
            Screen.say(Local.of("Не удалось открыть выбор изображения"));
        }
    }

    public static boolean save(Context context, Uri source) {
        if (context == null || source == null) return false;
        try (InputStream in = context.getContentResolver().openInputStream(source);
             OutputStream out = new FileOutputStream(file(context))) {
            if (in == null) return false;
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) > 0) {
                out.write(buf, 0, read);
            }
            out.flush();
            return true;
        } catch (Throwable error) {
            Diary.note("splash save: " + error);
            return false;
        }
    }

    private static Bitmap decodeScaled(File file, int reqWidth, int reqHeight) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
            int inSampleSize = 1;
            if (opts.outHeight > reqHeight || opts.outWidth > reqWidth) {
                int halfH = opts.outHeight / 2;
                int halfW = opts.outWidth / 2;
                while ((halfH / inSampleSize) >= reqHeight && (halfW / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = inSampleSize;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        } catch (Throwable error) {
            return null;
        }
    }

    private static boolean isSplashActivity(Activity activity) {
        String name = activity.getClass().getName();
        return name.endsWith(".SplashActivity")
                || name.endsWith(".NormalSplashAdActivity")
                || name.contains("Splash");
    }

    private static boolean isMainActivity(Activity activity) {
        String name = activity.getClass().getName();
        return name.endsWith(".MainActivity");
    }

    public static void created(final Activity activity) {
        attach(activity, false);
    }

    public static void started(final Activity activity) {
        attach(activity, false);
    }

    public static void resumed(final Activity activity) {
        attach(activity, true);
    }

    private static void attach(final Activity activity, final boolean fadeOutIfMain) {
        if (activity == null || activity.isFinishing()) return;
        boolean splash = isSplashActivity(activity);
        boolean main = isMainActivity(activity);
        if (!splash && !main) return;
        if (main && mainShown) return;
        if (!hasCustom(activity)) return;

        try {
            final View decor = activity.getWindow().getDecorView();
            if (!(decor instanceof ViewGroup)) return;
            final ViewGroup group = (ViewGroup) decor;

            Runnable show = () -> {
                try {
                    if (activity.isFinishing()) return;
                    View existing = group.findViewWithTag(OVERLAY_TAG);
                    if (existing != null) {
                        existing.bringToFront();
                        return;
                    }
                    if (main && mainShown) return;
                    if (main) mainShown = true;

                    int w = activity.getResources().getDisplayMetrics().widthPixels;
                    int h = activity.getResources().getDisplayMetrics().heightPixels;
                    Bitmap bmp = decodeScaled(file(activity), Math.max(720, w), Math.max(1280, h));
                    if (bmp == null) return;

                    final FrameLayout frame = new FrameLayout(activity);
                    frame.setTag(OVERLAY_TAG);
                    frame.setBackgroundColor(0xFF000000);
                    frame.setClickable(true);
                    if (Build.VERSION.SDK_INT >= 21) {
                        frame.setElevation(10000f);
                    }
                    ImageView image = new ImageView(activity);
                    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    image.setImageBitmap(bmp);
                    frame.addView(image, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                    group.addView(frame, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    frame.bringToFront();

                    if (main || fadeOutIfMain) {
                        frame.postDelayed(() -> {
                            frame.animate().alpha(0f).setDuration(300L).withEndAction(() -> {
                                try { group.removeView(frame); } catch (Throwable ignored) {}
                            }).start();
                        }, main ? 1100L : 1500L);
                    }
                } catch (Throwable error) {
                    Diary.note("splash display: " + error);
                }
            };
            show.run();
            decor.post(show);
        } catch (Throwable error) {
            Diary.note("splash attach: " + error);
        }
    }
}

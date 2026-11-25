package com.fasdeq.airtok;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;















public final class SettingsRow implements Application.ActivityLifecycleCallbacks {

    
    public static final String SETTINGS_ACTIVITY =
            "com.ss.android.ugc.aweme.setting.ui.SettingContainerActivity";

    
    
    private static final int TAG = 0x4D617267;

    

    
    private static final String[] AVATAR_SCREENS = {
            "com.ss.android.ugc.profile.business.ur.enlarge.EnlargeAvatarActivity",
            "com.ss.android.ugc.profile.business.ur.enlarge.EnlargeAvatarOptActivity",
    };

    @Override
    public void onActivityResumed(Activity activity) {
        long probe = Probe.start();
        Plugins.onActivityResumed(activity);
        Plugins.screen(activity);
        FlagGuard.shown();
        Rubble.resumed(activity);
        MotionCues.resumed(activity);
        SimpleFeed.attach(activity);
        WavySeek.attach(activity);
        
        
        
        if (!activity.getClass().getName().startsWith("com.fasdeq.airtok.")) {
            View decor = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
            if (decor != null) decor.post(() -> { try { Themes.relook(); } catch (Throwable ignored) {} });
        }
        Screen.at(activity);
        if(activity.getClass().getName().startsWith("com.fasdeq.airtok.")){
            View owned=activity.findViewById(android.R.id.content);
            if(owned instanceof ViewGroup && ((ViewGroup)owned).getChildCount()>0)
                Design.page(activity,((ViewGroup)owned).getChildAt(0),Skin.remembered(activity).page);
        }
        Themes.watch(activity);
        PinkButtons.watch(activity);
        if (!Version.LITE) {
            SocialLibrary.refresh(null);
        }
        Probe.stop(Probe.SHOWN, probe);

        String name = activity.getClass().getName();

        for (String screen : AVATAR_SCREENS) {
            if (screen.equals(name)) {
                addSaveAvatar(activity);
                return;
            }
        }
        if (!SETTINGS_ACTIVITY.equals(name)) {
            
            
            if (name.toLowerCase(Locale.US).contains("setting")) Diary.note("saw " + name);
            return;
        }
        Diary.note("settings screen is up");
        final View decor = activity.getWindow().getDecorView();
        if (decor.getTag(TAG) != null) return;
        SettingsCell.newScreen();
        decor.setTag(TAG, Boolean.TRUE);

        decor.getViewTreeObserver().addOnGlobalLayoutListener(new Injector(activity));
    }

    






    private static void addSaveAvatar(final Activity activity) {
        try {
            ViewGroup content = (ViewGroup) activity.getWindow()
                    .getDecorView().findViewById(android.R.id.content);
            if (!Avatars.isEnabled()) return;
            if (content == null || content.getTag(SAVE_TAG) != null) return;
            content.setTag(SAVE_TAG, Boolean.TRUE);

            TextView save = new TextView(activity);
            save.setText(Text.SAVE_AVATAR);
            save.setTextColor(0xFFFFFFFF);
            save.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            save.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            save.setGravity(Gravity.CENTER);
            save.setPadding(dp(activity, 22), dp(activity, 11), dp(activity, 22), dp(activity, 11));

            GradientDrawable pill = new GradientDrawable();
            pill.setColor(Accent.colour());
            pill.setCornerRadius(dp(activity, 22));
            save.setBackground(pill);
            save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Avatars.save(activity);
                }
            });

            android.widget.FrameLayout.LayoutParams params =
                    new android.widget.FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            
            
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.topMargin = (int) (activity.getResources()
                    .getDisplayMetrics().heightPixels * 0.74f);
            content.addView(save, params);
            Diary.note("save-avatar button added");
        } catch (Throwable error) {
            Diary.note("save-avatar button failed: " + error);
        }
    }

    private static final int SAVE_TAG = 0x4D617269;  

    @Override
    public void onActivityCreated(Activity activity, Bundle state) {
        Plugins.onActivityCreated(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {
        Plugins.onActivityPaused(activity);
        MotionCues.paused(activity);
        Screen.gone(activity);
    }

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle state) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}

    







    private final class Injector implements ViewTreeObserver.OnGlobalLayoutListener {

        private final Activity activity;
        private View box;
        private ViewGroup container;

        Injector(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onGlobalLayout() {
            try {
                if (box == null) inject(this);
                if (box == null || container == null) return;
                if (SettingsCell.drawn()) fold(box, true);
                int pages = 0;
                for (int i = 0; i < container.getChildCount(); i++) {
                    if (container.getChildAt(i).getVisibility() == View.VISIBLE) pages++;
                }
                int wanted = pages > 1 ? View.GONE : View.VISIBLE;
                if (box.getVisibility() != wanted) {
                    box.setVisibility(wanted);
                    Diary.note(wanted == View.GONE
                            ? "row stood aside for a page of the screen" : "row back");
                }
            } catch (Throwable error) {
                Diary.note("row failed: " + error);
            }
        }
    }

    
    private static final int FOLDED = 0x4D617253;

    




    private static void fold(View box, boolean away) {
        try {
            if ((box.getTag(FOLDED) != null) == away) return;
            box.setTag(FOLDED, away ? Boolean.TRUE : null);
            if (box instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) box;
                for (int i = 0; i < group.getChildCount(); i++) {
                    group.getChildAt(i).setVisibility(away ? View.GONE : View.VISIBLE);
                }
            }
            int gap = dp(box.getContext(), 8);
            box.setPadding(box.getPaddingLeft(), Math.max(0, box.getPaddingTop() + (away ? -gap : gap)),
                    box.getPaddingRight(), away ? 0 : gap);
        } catch (Throwable error) {
            Diary.note("row could not be put away: " + error);
        }
    }

    

    private void inject(Injector injector) {
        Activity activity = injector.activity;
        ViewGroup container = fragmentContainer(activity);
        if (container == null) return;
        ViewGroup parent = container.getParent() instanceof ViewGroup
                ? (ViewGroup) container.getParent() : null;
        if (parent == null || parent.getTag(TAG) != null) return;
        if (container.getWidth() == 0) return;  

        Skin skin = Skin.of(container);

        int index = parent.indexOfChild(container);
        ViewGroup.LayoutParams params = container.getLayoutParams();

        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setTag(TAG, Boolean.TRUE);

        
        
        
        parent.removeViewAt(index);
        final View box = row(activity, skin);
        column.addView(box, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        column.addView(container, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        parent.addView(column, index, params);
        keepClearOfTheStatusBar(column, box, container, skin);
        injector.box = box;
        injector.container = container;
        
        
        if (SettingsCell.likely()) {
            fold(box, true);
            box.postDelayed(() -> {
                if (!SettingsCell.drawn()) fold(box, false);
            }, 600L);
        }
        Diary.note("row added above " + container.getClass().getName());
    }

    







    private ViewGroup fragmentContainer(Activity activity) {
        try {
            Object id = activity.getClass().getMethod("getFragmentContainer").invoke(activity);
            View view = activity.findViewById(((Integer) id).intValue());
            if (view instanceof ViewGroup) return (ViewGroup) view;
            Diary.note("container 0x" + Integer.toHexString(((Integer) id).intValue())
                    + " is " + view);
        } catch (Throwable error) {
            Diary.note("no container: " + error);
        }
        
        View content = activity.findViewById(android.R.id.content);
        if (content instanceof ViewGroup && ((ViewGroup) content).getChildCount() > 0) {
            View first = ((ViewGroup) content).getChildAt(0);
            if (first instanceof ViewGroup) {
                Diary.note("falling back to " + first.getClass().getName());
                return (ViewGroup) first;
            }
        }
        return null;
    }

    

    private View row(final Activity activity, Skin skin) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(activity, 16), 0, dp(activity, 16), 0);

        GradientDrawable background = new GradientDrawable();
        background.setColor(skin.card);
        background.setCornerRadius(skin.radius);
        row.setBackground(background);

        ImageView glyph = new ImageView(activity);
        Bitmap emblem = null;
        try {
            byte[] rawBytes = android.util.Base64.decode(Emblem.PNG, android.util.Base64.DEFAULT);
            Bitmap raw = android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.length);
            if (raw != null) emblem = Badge.round(raw, 0.28f);
        } catch (Throwable ignored) {}
        if (emblem == null) emblem = Launcher.preview(Launcher.DEFAULT);
        if (emblem != null) glyph.setImageBitmap(emblem);
        glyph.setScaleType(ImageView.ScaleType.FIT_CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0xFF000000);
        glyph.setBackground(circle);
        glyph.setClipToOutline(true);
        glyph.setContentDescription("AirTok");
        row.addView(glyph, new LinearLayout.LayoutParams(dp(activity, 24), dp(activity, 24)));

        TextView title = new TextView(activity);
        title.setText(Text.ROW);
        title.setTextColor(skin.text);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(activity, 16);
        row.addView(title, titleParams);

        TextView chevron = new TextView(activity);
        chevron.setText("\u203A");
        chevron.setTextColor(skin.muted());
        chevron.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        row.addView(chevron);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    activity.startActivity(new Intent(activity, SettingsActivity.class));
                } catch (Throwable ignored) {
                }
            }
        });

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(skin.margin, statusBar(activity) + dp(activity, 8),
                skin.margin, dp(activity, 8));
        box.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 56)));
        return box;
    }

    






    private void keepClearOfTheStatusBar(final LinearLayout column, final View box,
                                         final ViewGroup screen, final Skin skin) {
        try {
            column.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public android.view.WindowInsets onApplyWindowInsets(
                        View view, android.view.WindowInsets insets) {
                    int top = insets.getSystemWindowInsetTop();
                    boolean folded = box.getTag(FOLDED) != null;
                    box.setPadding(skin.margin, folded ? top : top + dp(column.getContext(), 8),
                            skin.margin, folded ? 0 : dp(column.getContext(), 8));
                    screen.dispatchApplyWindowInsets(insets.replaceSystemWindowInsets(
                            insets.getSystemWindowInsetLeft(), 0,
                            insets.getSystemWindowInsetRight(),
                            insets.getSystemWindowInsetBottom()));
                    return insets.consumeSystemWindowInsets();
                }
            });
            column.requestApplyInsets();
        } catch (Throwable error) {
            Diary.note("insets left alone: " + error);
        }
    }

    private static int statusBar(Activity activity) {
        try {
            android.view.WindowInsets insets =
                    activity.getWindow().getDecorView().getRootWindowInsets();
            if (insets != null && insets.getSystemWindowInsetTop() > 0) {
                return insets.getSystemWindowInsetTop();
            }
        } catch (Throwable ignored) {
        }
        try {
            int id = activity.getResources()
                    .getIdentifier("status_bar_height", "dimen", "android");
            if (id > 0) return activity.getResources().getDimensionPixelSize(id);
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static int dp(android.content.Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

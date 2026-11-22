package cat.narezany.margyt;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.List;

public final class Tester {

    static final String SUPPORTER = "supporter";

    private static final int INK = 0x14FFFFFF;

    private static final long AGAIN = 10_000L;

    private static final int TAG = 0x4D617254;   

    private static final Handler HAND = new Handler(Looper.getMainLooper());

    private Tester() {}

    public static boolean on() {
        return Version.TEST;
    }

    public static boolean allowed() {
        if (!on()) return true;
        try {
            List<Mine.Held> held = Mine.held();
            for (Mine.Held one : held) {
                if (SUPPORTER.equals(one.id)) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean asked() {
        try {
            return Mine.anything() || Mine.everAsked();
        } catch (Throwable ignored) {
            return false;
        }
    }

    static void mark(final Activity activity) {
        if (!on()) return;
        View root = activity.getWindow() == null ? null
                : activity.getWindow().getDecorView();
        if (!(root instanceof ViewGroup)) return;
        ViewGroup decor = (ViewGroup) root;
        if (decor.findViewWithTag(TAG) != null) return;

        final TextView mark = new TextView(activity);
        mark.setTag(TAG);
        mark.setText(who());
        mark.setTextSize(22);
        mark.setTextColor(INK);
        mark.setTypeface(Typeface.DEFAULT_BOLD);
        mark.setGravity(Gravity.CENTER);
        mark.setShadowLayer(1.5f, 0, 0, 0x10000000);
        mark.setClickable(false);
        mark.setFocusable(false);
        mark.setDuplicateParentStateEnabled(false);

        FrameLayout.LayoutParams where = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        decor.addView(mark, where);

        HAND.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mark.getParent() == null) return;
                mark.setText(who());
                mark.bringToFront();
                HAND.postDelayed(this, AGAIN);
            }
        }, AGAIN);
    }

    private static String who() {
        try {
            String id = Account.id();
            if (id != null && id.length() > 0) return id;
        } catch (Throwable ignored) {
        }
        return Text.TEST_NOBODY;
    }

    public static void refuse(Activity activity) {
        try {
            Popup.told(activity, Text.TEST_TITLE, Text.TEST_TEXT, Text.TEST_CLOSE,
                    new Runnable() {
                        @Override
                        public void run() {
                            activity.finish();
                        }
                    });
        } catch (Throwable error) {
            Diary.note("test: " + error);
            activity.finish();
        }
    }
}

package cat.narezany.margyt;

import android.app.Activity;
import android.content.Context;

final class CrashConsent {
    private CrashConsent() {}

    static boolean asked(Context context) {
        return true;
    }

    static void show(final Activity activity, final Runnable then) {
        if (then != null) then.run();
    }

    static void paused(Activity activity) {
    }
}

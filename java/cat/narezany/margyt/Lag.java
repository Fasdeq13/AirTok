package cat.narezany.margyt;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.view.FrameMetrics;
import android.view.View;
import android.view.Window;

public final class Lag {

    private static final long BAD = 700;

    private static final long SHORT = 100;

    private static final int ENOUGH = 12;

    private static final long LOOK = 50;

    private static final long ABSURD = 30000;

    private static final String[] STAGES = {
            "waiting for the main thread", "input", "animation", "layout", "draw", "sync", "commands", "swap", "gpu"};

    private static final LagTally TALLY = new LagTally(STAGES);
    private static final int WATCHED = 0x4D61724C;

    private static int told;
    private static volatile long worst;

    private static volatile long asked;

    private static volatile int looks;

    private static volatile StackTraceElement[] early, late;

    private static Handler frames;

    private Lag() {}

    static final String KEY = "lag_watch";

    static boolean wanted() {
        if (Version.TEST || Version.PROFILE || DevMode.isOn()) return true;
        try {
            return AccountAppearance.prefs(Margy.context()).getBoolean(KEY, false);
        } catch (Throwable ignored) {
            return false;
        }
    }

    static void setWanted(boolean on) {
        try {
            AccountAppearance.prefs(Margy.context()).edit().putBoolean(KEY, on).apply();
        } catch (Throwable ignored) {
        }
    }

    public static void watch() {
        try {
            Thread watcher = new Thread(Lag::look, "margyt-lag");
            watcher.setDaemon(true);
            watcher.setPriority(Thread.MIN_PRIORITY);
            watcher.start();
        } catch (Throwable error) {
            Diary.note("lag: " + error);
        }
    }

    private static void look() {
        final Thread main = Looper.getMainLooper().getThread();
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable answer = Lag::answered;
        while (true) {
            try {
                if (Watch.here() == null || !wanted()) {

                    Thread.sleep(wanted() ? 1000 : 5000);
                    continue;
                }
                Thread.sleep(LOOK);
                if (Probe.ON) sample(main.getStackTrace());
                if (Probe.ON) HeapShot.tick(Lag::profile);
                long now = SystemClock.uptimeMillis(), since = asked;
                if (since == 0) {
                    early = null;
                    late = null;
                    looks = 0;
                    asked = now;
                    handler.post(answer);
                    continue;
                }
                looks++;
                if (now - since >= SHORT && early == null) early = main.getStackTrace();
                if (now - since >= BAD && late == null) late = main.getStackTrace();
            } catch (InterruptedException over) {
                return;
            } catch (Throwable ignored) {

            }
        }
    }

    private static final java.util.HashMap<String, int[]> BUSY_IN = new java.util.HashMap<String, int[]>();
    private static int samples, busy, ours;

    private static void sample(StackTraceElement[] stack) {
        if (stack == null || stack.length == 0) return;
        synchronized (BUSY_IN) {
            samples++;
            if (where(stack).length() == 0) return;
            busy++;
            String mine = mine(stack);
            String key;
            if (mine != null) {
                ours++;

                String outer = Version.PROFILE ? outermost(stack) : null;
                key = outer == null || outer.equals(mine) ? mine : mine + " <- " + outer;
            } else if (Version.PROFILE) {
                key = "[not mod] " + firstApp(stack);
            } else {
                return;
            }
            int[] count = BUSY_IN.get(key);
            if (count == null) {
                if (BUSY_IN.size() >= (Version.PROFILE ? 600 : 200)) return;
                count = new int[1];
                BUSY_IN.put(key, count);
            }
            count[0]++;
        }
    }

    private static String profile() {
        synchronized (BUSY_IN) {
            if (samples == 0) return "";
            StringBuilder out = new StringBuilder();
            out.append("main thread: busy in ").append(busy).append(" of ").append(samples)
                    .append(" looks, in the mod in ").append(ours).append('\n');
            java.util.ArrayList<String> names = new java.util.ArrayList<String>(BUSY_IN.keySet());
            java.util.Collections.sort(names, (one, two) -> BUSY_IN.get(two)[0] - BUSY_IN.get(one)[0]);
            for (int i = 0; i < names.size() && i < (Version.PROFILE ? 60 : 25); i++) {
                out.append("  ").append(BUSY_IN.get(names.get(i))[0]).append("  ").append(names.get(i)).append('\n');
            }
            return out.toString();
        }
    }

    private static void answered() {
        try {
            long took = SystemClock.uptimeMillis() - asked;
            StackTraceElement[] stack = took >= BAD && late != null ? late : early;
            int seen = looks;
            asked = 0;
            if (took < SHORT || took > ABSURD || stack == null || Watch.here() == null) return;

            if (seen < took / (LOOK * 4)) return;
            String where = where(stack);
            if (where.length() == 0) return;

            String plugin = plugin(stack);
            String mine = mine(stack);
            TALLY.stall(plugin != null ? "plugin " + plugin : mine != null ? mine : brief(where), where, took);
            if (plugin != null) charge(plugin, took);
            if (took > worst) worst = took;
            if (took >= BAD && told < ENOUGH) {
                told++;
                Diary.note("lag: the app stopped for " + took + " ms in " + where);
            }
        } catch (Throwable ignored) {
            asked = 0;
        }
    }

    static void frames(Activity activity) {
        if (Build.VERSION.SDK_INT < 24 || activity == null || !wanted()) return;
        try {
            Window window = activity.getWindow();
            View decor = window == null ? null : window.getDecorView();
            if (decor == null || decor.getTag(WATCHED) != null) return;
            decor.setTag(WATCHED, Boolean.TRUE);
            if (frames == null) {
                HandlerThread thread = new HandlerThread("margyt-frames", android.os.Process.THREAD_PRIORITY_BACKGROUND);
                thread.start();
                frames = new Handler(thread.getLooper());
            }
            final String screen = activity.getClass().getSimpleName();
            float rate = 60f;
            try {
                rate = activity.getWindowManager().getDefaultDisplay().getRefreshRate();
            } catch (Throwable ignored) {
            }

            final long slow = (long) (2e9 / Math.max(30f, rate));
            final long[] spent = new long[STAGES.length];
            window.addOnFrameMetricsAvailableListener((from, metrics, dropped) -> {
                try {
                    long total = metrics.getMetric(FrameMetrics.TOTAL_DURATION);
                    if (total <= slow) {
                        TALLY.frame(screen, false, false, spent);
                        return;
                    }
                    spent[0] = metrics.getMetric(FrameMetrics.UNKNOWN_DELAY_DURATION);
                    spent[1] = metrics.getMetric(FrameMetrics.INPUT_HANDLING_DURATION);
                    spent[2] = metrics.getMetric(FrameMetrics.ANIMATION_DURATION);
                    spent[3] = metrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION);
                    spent[4] = metrics.getMetric(FrameMetrics.DRAW_DURATION);
                    spent[5] = metrics.getMetric(FrameMetrics.SYNC_DURATION);
                    spent[6] = metrics.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION);
                    spent[7] = metrics.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION);
                    spent[8] = Build.VERSION.SDK_INT >= 31 ? metrics.getMetric(FrameMetrics.GPU_DURATION) : 0;
                    TALLY.frame(screen, true, total > SHORT * 1000000L, spent);
                } catch (Throwable ignored) {

                }
            }, frames);
        } catch (Throwable error) {
            Diary.detail("lag: frames: " + error);
        }
    }

    static String report() {

        long watched = (SystemClock.elapsedRealtime() - BEGAN) / 1000L;
        return "since the app started: " + watched / 60L + " min " + watched % 60L + " s\n"
                + memory() + TALLY.report() + (Probe.ON ? profile() : "");
    }

    static String memory() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long used = (runtime.totalMemory() - runtime.freeMemory()) >> 20, most = runtime.maxMemory() >> 20;
            return "memory: " + used + " of " + most + " MB; collections "
                    + android.os.Debug.getRuntimeStat("art.gc.gc-count") + " ("
                    + android.os.Debug.getRuntimeStat("art.gc.gc-time") + " ms), stopping the app "
                    + android.os.Debug.getRuntimeStat("art.gc.blocking-gc-count") + " ("
                    + android.os.Debug.getRuntimeStat("art.gc.blocking-gc-time") + " ms)\n";
        } catch (Throwable error) {
            return "";
        }
    }

    private static final long BEGAN = SystemClock.elapsedRealtime();

    static String plugin(StackTraceElement[] stack) {
        if (stack == null) return null;
        for (StackTraceElement frame : stack) {
            String owner = Plugins.owner(frame.getClassName());
            if (owner != null) return owner;
        }
        return null;
    }

    private static final java.util.HashMap<String, long[]> PLUGIN_COST = new java.util.HashMap<String, long[]>();

    private static final long TOO_MUCH = 3000L;

    private static void charge(String plugin, long took) {
        boolean say = false;
        synchronized (PLUGIN_COST) {
            long[] cost = PLUGIN_COST.get(plugin);
            if (cost == null) {
                cost = new long[2];
                PLUGIN_COST.put(plugin, cost);
            }
            say = cost[0] < TOO_MUCH && cost[0] + took >= TOO_MUCH;
            cost[0] += took;
            cost[1]++;
        }
        if (say) Diary.note("lag: plugin " + plugin + " has stalled the app for " + TOO_MUCH / 1000 + " s this run");
    }

    static long cost(String plugin) {
        synchronized (PLUGIN_COST) {
            for (java.util.Map.Entry<String, long[]> one : PLUGIN_COST.entrySet()) {

                if (one.getKey().equals(plugin) || one.getKey().contains(plugin + " / ") || one.getKey().endsWith(" / " + plugin)) {
                    return one.getValue()[0];
                }
            }
            return 0L;
        }
    }

    static String outermost(StackTraceElement[] stack) {
        String found = null;
        for (StackTraceElement frame : stack) {
            if (frame.getClassName().startsWith("cat.narezany.margyt.")) found = name(frame);
        }
        return found;
    }

    static String firstApp(StackTraceElement[] stack) {
        for (StackTraceElement frame : stack) {
            String type = frame.getClassName();
            if (type.startsWith("java.") || type.startsWith("android.") || type.startsWith("com.android.")
                    || type.startsWith("dalvik.") || type.startsWith("libcore.") || type.startsWith("sun.")
                    || type.startsWith("kotlin.") || type.startsWith("androidx.")) continue;
            return name(frame);
        }
        return name(stack[0]);
    }

    static String mine(StackTraceElement[] stack) {
        if (stack == null) return null;
        for (StackTraceElement frame : stack) {
            if (frame.getClassName().startsWith("cat.narezany.margyt.")) return name(frame);
        }
        return null;
    }

    static String brief(String where) {
        int first = where.indexOf(" < ");
        int second = first < 0 ? -1 : where.indexOf(" < ", first + 3);
        return second < 0 ? where : where.substring(0, second);
    }

    static String where(StackTraceElement[] stack) {
        if (stack == null || stack.length == 0) return "";
        String top = stack[0].getClassName() + "." + stack[0].getMethodName();
        if (top.equals("android.os.MessageQueue.nativePollOnce")) return "";
        StringBuilder out = new StringBuilder();
        int said = 0;
        String mine = null;
        for (StackTraceElement frame : stack) {
            String type = frame.getClassName();
            if (mine == null && type.startsWith("cat.narezany.margyt.")) mine = name(frame);
            if (said < 5) {
                if (out.length() > 0) out.append(" < ");
                out.append(name(frame));
                said++;
            }
        }
        if (mine != null && out.indexOf(mine) < 0) out.append(" << ").append(mine);
        return out.toString();
    }

    private static String name(StackTraceElement frame) {
        String type = frame.getClassName();
        int dot = type.lastIndexOf('.');

        String shortened = dot > 0 && !type.startsWith("X.") ? type.substring(dot + 1) : type;
        return shortened + "." + frame.getMethodName()
                + (frame.getLineNumber() > 0 && frame.getLineNumber() < 100000 ? ":" + frame.getLineNumber() : "");
    }

    public static long worst() {
        return worst;
    }
}

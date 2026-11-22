package com.fasdeq.airtok;

import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayDeque;
import java.util.WeakHashMap;


final class Sliced {

    private Sliced() {}


    interface Visit {
        void view(View view);
    }


    private static final long STRETCH = 2;
    private static final int MOST = 12000;
    private static final int DEEP = 40;

    private static final class Walk implements Runnable, android.view.Choreographer.FrameCallback {

        final java.lang.ref.WeakReference<View> root;
        final Visit[] visits;
        final ArrayDeque<View> ahead = new ArrayDeque<View>();
        final ArrayDeque<Integer> depths = new ArrayDeque<Integer>();
        boolean again;
        int seen;

        Walk(View root, Visit[] visits) {
            this.root = new java.lang.ref.WeakReference<View>(root);
            this.visits = visits;
        }

        void begin() {
            ahead.clear();
            depths.clear();
            seen = 0;
            View top = root.get();
            if (top != null) {
                ahead.push(top);
                depths.push(0);
            }
        }

        @Override
        public void doFrame(long frameTimeNanos) {
            run();
        }

        @Override
        public void run() {
            long until = SystemClock.uptimeMillis() + STRETCH;
            long probe = Probe.start();
            int since = 0;
            try {
                while (!ahead.isEmpty()) {
                    View view = ahead.pop();
                    int depth = depths.pop();
                    if (view == null || ++seen > MOST) continue;
                    for (Visit visit : visits) {
                        try {
                            visit.view(view);
                        } catch (Throwable ignored) {
                        }
                    }
                    if (depth < DEEP && view instanceof ViewGroup) {
                        ViewGroup group = (ViewGroup) view;

                        for (int i = group.getChildCount() - 1; i >= 0; i--) {
                            ahead.push(group.getChildAt(i));
                            depths.push(depth + 1);
                        }
                    }
                    if (++since >= 24) {
                        since = 0;
                        if (SystemClock.uptimeMillis() >= until) break;
                    }
                }
            } catch (Throwable error) {
                ahead.clear();
                depths.clear();
            }
            Probe.stop(Probe.SLICE, probe);
            View top = root.get();
            boolean there = top != null && top.isAttachedToWindow();
            if (!ahead.isEmpty() && there) {
                android.view.Choreographer.getInstance().postFrameCallback(this);
                return;
            }
            if (again && there) {
                again = false;
                begin();
                android.view.Choreographer.getInstance().postFrameCallback(this);
                return;
            }
            ahead.clear();
            depths.clear();

            if (top != null) {
                synchronized (running) {
                    running.remove(top);
                }
            }
        }
    }

    private static final WeakHashMap<View, Walk> running = new WeakHashMap<View, Walk>();


    static void walk(View root, Visit... visits) {
        if (root == null || visits.length == 0) return;
        Walk walk;
        synchronized (running) {
            walk = running.get(root);
            if (walk != null) {
                walk.again = true;
                return;
            }
            walk = new Walk(root, visits);
            running.put(root, walk);
        }
        walk.begin();


        walk.run();
    }
}

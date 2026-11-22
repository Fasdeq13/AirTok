package com.fasdeq.airtok;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;


public final class MotionCues {

    private MotionCues() {}

    static final String KEY = "motion_cues";
    private static final int TAG = 0x7e0c0361;

    private static final float PUSH = 0.35f, TURN = 0.06f;
    private static final long FADE_AFTER_MS = 5000;

    private static volatile Boolean on;
    private static WeakReference<Dots> showing;

    static boolean enabled() {
        Boolean known = on;
        if (known != null) return known;
        Context context = AirTok.context();
        if (context == null) return false;
        boolean value = context.getSharedPreferences(AirTok.PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false);
        on = value;
        return value;
    }

    static void setEnabled(Activity activity, boolean value) {
        on = value;
        Context context = AirTok.context();
        if (context != null) context.getSharedPreferences(AirTok.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply();
        if (value) resumed(activity); else remove(activity);
    }


    static boolean available(Context context) {
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensors != null && sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null;
    }


    static void resumed(Activity activity) {
        if (!enabled() || activity == null) return;
        try {
            View decor = activity.getWindow().getDecorView();
            if (!(decor instanceof ViewGroup)) return;
            ViewGroup group = (ViewGroup) decor;
            Dots dots = (Dots) group.getTag(TAG);
            if (dots == null) {
                dots = new Dots(activity);
                group.setTag(TAG, dots);
                group.addView(dots, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            dots.bringToFront();
            dots.listen(true);
            showing = new WeakReference<>(dots);
        } catch (Throwable error) {
            Diary.note("motion cues: " + error);
        }
    }

    static void paused(Activity activity) {
        if (activity == null || activity.getWindow() == null) return;
        Object dots = activity.getWindow().getDecorView().getTag(TAG);
        if (dots instanceof Dots) ((Dots) dots).listen(false);
    }

    private static void remove(Activity activity) {
        if (activity == null) return;
        View decor = activity.getWindow().getDecorView();
        Object dots = decor.getTag(TAG);
        if (dots instanceof Dots) {
            ((Dots) dots).listen(false);
            ((ViewGroup) decor).removeView((View) dots);
            decor.setTag(TAG, null);
        }
    }


    static final class Dots extends View implements SensorEventListener {
        private final SensorManager sensors;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean listening;
        private final float[] gravity = {0, 0, 9.8f};
        private float[] raw = new float[3];
        private float yaw;
        private long last;
        private float x, y, vx, vy;
        private float steadySide, steadyAhead, steadyYaw;
        private float alpha;
        private long lastMoved;
        private final float density;

        Dots(Context context) {
            super(context);
            sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            density = getResources().getDisplayMetrics().density;
            fill.setColor(0xFF2A2A2E);
            rim.setStyle(Paint.Style.STROKE);
            rim.setStrokeWidth(density);
            rim.setColor(0xFFFFFFFF);
            setWillNotDraw(false);
            setClickable(false);
            setFocusable(false);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            setElevation(999f);
        }

        void listen(boolean want) {
            if (want == listening || sensors == null) return;
            listening = want;
            if (want) {
                last = 0;
                Sensor accel = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                Sensor gyro = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                if (accel != null) sensors.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
                if (gyro != null) sensors.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME);
            } else {
                sensors.unregisterListener(this);
                alpha = 0;
                invalidate();
            }
        }

        @Override protected void onDetachedFromWindow() {
            listen(false);
            super.onDetachedFromWindow();
        }

        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        @Override public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

                float g = norm(gravity);
                if (g > 1) yaw = (event.values[0] * gravity[0] + event.values[1] * gravity[1] + event.values[2] * gravity[2]) / g;
                return;
            }
            long now = event.timestamp;
            float dt = last == 0 ? 0 : Math.min((now - last) / 1e9f, 0.05f);

            if (last == 0) System.arraycopy(event.values, 0, gravity, 0, 3);
            last = now;

            for (int i = 0; i < 3; i++) {
                gravity[i] += (event.values[i] - gravity[i]) * 0.02f;
                raw[i] = event.values[i] - gravity[i];
            }
            if (dt == 0) return;
            float g = norm(gravity);
            if (g < 1) return;
            float ux = gravity[0] / g, uy = gravity[1] / g, uz = gravity[2] / g;

            float dot = ux;
            float rx = 1 - dot * ux, ry = -dot * uy, rz = -dot * uz;
            float r = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
            if (r < 0.1f) return;
            rx /= r; ry /= r; rz /= r;
            float fx = uy * rz - uz * ry, fy = uz * rx - ux * rz, fz = ux * ry - uy * rx;
            float side = raw[0] * rx + raw[1] * ry + raw[2] * rz;
            float ahead = raw[0] * fx + raw[1] * fy + raw[2] * fz;

            float travel = 26 * density;
            float wantX = -side * travel + yaw * travel * 2.2f;
            float wantY = ahead * travel;

            float stiff = 26, damp = 9;
            vx += (wantX - x) * stiff * dt - vx * damp * dt;
            vy += (wantY - y) * stiff * dt - vy * damp * dt;
            x += vx * dt;
            y += vy * dt;
            float limit = 44 * density;
            x = Math.max(-limit, Math.min(limit, x));
            y = Math.max(-limit, Math.min(limit, y));

            long clock = android.os.SystemClock.uptimeMillis();

            steadySide += (side - steadySide) * 0.08f;
            steadyAhead += (ahead - steadyAhead) * 0.08f;
            steadyYaw += (yaw - steadyYaw) * 0.08f;
            if (Math.abs(steadySide) > PUSH || Math.abs(steadyAhead) > PUSH || Math.abs(steadyYaw) > TURN) lastMoved = clock;
            float wantAlpha = clock - lastMoved < FADE_AFTER_MS ? 1f : 0f;
            alpha += (wantAlpha - alpha) * Math.min(1f, dt * (wantAlpha > alpha ? 4f : 1.5f));
            if (alpha > 0.01f || wantAlpha > 0) postInvalidateOnAnimation();
        }

        private static float norm(float[] v) {
            return (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        }

        @Override protected void onDraw(Canvas canvas) {
            if (alpha < 0.02f) return;
            int width = getWidth(), height = getHeight();
            float radius = 4.5f * density;
            fill.setAlpha((int) (170 * alpha));
            rim.setAlpha((int) (110 * alpha));
            float gapY = 62 * density;
            int rows = Math.max(3, (int) (height * 0.72f / gapY));
            float top = (height - gapY * (rows - 1)) / 2f;
            float inner = 40 * density, outer = 16 * density;
            for (int i = 0; i < rows; i++) {
                float py = top + i * gapY + y;

                float stagger = i % 2 == 0 ? outer : inner;
                dot(canvas, stagger + x, py, radius);
                dot(canvas, width - stagger + x, py, radius);
            }
        }

        private void dot(Canvas canvas, float px, float py, float radius) {
            canvas.drawCircle(px, py, radius, fill);
            canvas.drawCircle(px, py, radius, rim);
        }
    }
}

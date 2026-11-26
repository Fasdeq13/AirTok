package com.fasdeq.airtok;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.nio.ByteBuffer;

public final class BeatVibration {

    private BeatVibration() {}

    public static final String KEY = "beat_vibration";
    public static final String TITLE = Local.of("Вибрация в такт видео");
    public static final String SUBTITLE = Local.of("Телефон вибрирует в бит звука видео.");

    private static final long MIN_BEAT_INTERVAL_MS = 135L;
    private static final long RESET_GAP_MS = 450L;

    private static volatile Boolean cached;
    private static volatile Vibrator vibrator;
    private static float lowPass;
    private static float avgBass = 0.03f;
    private static float avgEnergy = 0.045f;
    private static long lastAudioAt;
    private static long lastBeatAt;

    public static boolean isEnabled() {
        Boolean known = cached;
        if (known != null) return known.booleanValue();
        try {
            SharedPreferences p = prefs();
            if (p == null) return false;
            boolean on = p.getBoolean(KEY, false);
            cached = Boolean.valueOf(on);
            return on;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void setEnabled(boolean on) {
        cached = Boolean.valueOf(on);
        try {
            SharedPreferences p = prefs();
            if (p != null) p.edit().putBoolean(KEY, on).apply();
        } catch (Throwable ignored) {
        }
        if (!on) {
            synchronized (BeatVibration.class) {
                lowPass = 0f;
                avgBass = 0.03f;
                avgEnergy = 0.045f;
                lastAudioAt = 0L;
                lastBeatAt = 0L;
            }
        }
    }

    public static int write(AudioTrack track, byte[] data, int offset, int size) {
        if (track == null) return 0;
        int written = track.write(data, offset, size);
        if (written > 0 && data != null && isEnabled() && activeUi()) {
            try {
                analyzeBytes(track, data, offset, written);
            } catch (Throwable ignored) {
            }
        }
        return written;
    }

    public static int write(AudioTrack track, ByteBuffer data, int size, int writeMode) {
        if (track == null) return 0;
        if (data == null || size <= 0 || !isEnabled() || !activeUi()) {
            return track.write(data, size, writeMode);
        }
        int pos = data.position();
        int lim = data.limit();
        int written = track.write(data, size, writeMode);
        if (written > 0) {
            int available = lim - pos;
            if (available > 0) {
                try {
                    analyzeBuffer(track, data, pos, Math.min(written, available));
                } catch (Throwable ignored) {
                }
            }
        }
        return written;
    }

    private static boolean activeUi() {
        Activity here = Watch.here();
        return here != null && !(here instanceof SettingsActivity);
    }

    private static void analyzeBytes(AudioTrack track, byte[] data, int offset, int len) {
        if (offset < 0 || len <= 1 || offset + len > data.length) return;
        if (track.getAudioFormat() != AudioFormat.ENCODING_PCM_16BIT) return;
        int channels = Math.max(1, track.getChannelCount());
        int frameBytes = channels * 2;
        int frames = len / frameBytes;
        if (frames <= 0) return;

        int stride = Math.max(1, frames / 256);
        int byteStep = stride * frameBytes;
        float lpAlpha = Math.min(0.45f, 0.06f * stride);

        float lp;
        synchronized (BeatVibration.class) {
            lp = lowPass;
        }

        double sumSq = 0.0;
        double bassSq = 0.0;
        float bassPeak = 0f;
        int count = 0;
        int end = offset + len - 1;
        for (int i = offset; i < end; i += byteStep) {
            int lo = data[i] & 0xFF;
            int hi = data[i + 1];
            float x = ((hi << 8) | lo) / 32768.0f;
            lp += lpAlpha * (x - lp);
            float absLp = lp < 0f ? -lp : lp;
            if (absLp > bassPeak) bassPeak = absLp;
            sumSq += x * x;
            bassSq += lp * lp;
            count++;
        }
        if (count > 0) {
            evaluate(lp, (float) Math.sqrt(sumSq / count), (float) Math.sqrt(bassSq / count), bassPeak);
        }
    }

    private static void analyzeBuffer(AudioTrack track, ByteBuffer data, int start, int len) {
        if (start < 0 || len <= 1 || start + len > data.limit()) return;
        if (track.getAudioFormat() != AudioFormat.ENCODING_PCM_16BIT) return;
        int channels = Math.max(1, track.getChannelCount());
        int frameBytes = channels * 2;
        int frames = len / frameBytes;
        if (frames <= 0) return;

        int stride = Math.max(1, frames / 256);
        int byteStep = stride * frameBytes;
        float lpAlpha = Math.min(0.45f, 0.06f * stride);

        float lp;
        synchronized (BeatVibration.class) {
            lp = lowPass;
        }

        double sumSq = 0.0;
        double bassSq = 0.0;
        float bassPeak = 0f;
        int count = 0;
        int end = start + len - 1;
        for (int i = start; i < end; i += byteStep) {
            int lo = data.get(i) & 0xFF;
            int hi = data.get(i + 1);
            float x = ((hi << 8) | lo) / 32768.0f;
            lp += lpAlpha * (x - lp);
            float absLp = lp < 0f ? -lp : lp;
            if (absLp > bassPeak) bassPeak = absLp;
            sumSq += x * x;
            bassSq += lp * lp;
            count++;
        }
        if (count > 0) {
            evaluate(lp, (float) Math.sqrt(sumSq / count), (float) Math.sqrt(bassSq / count), bassPeak);
        }
    }

    private static void evaluate(float lp, float rms, float bassRms, float bassPeak) {
        long now = android.os.SystemClock.elapsedRealtime();
        int durMs = 0;
        int amp = 0;

        synchronized (BeatVibration.class) {
            lowPass = lp;
            if (lastAudioAt == 0L || now - lastAudioAt > RESET_GAP_MS) {
                avgBass = Math.max(0.025f, bassRms);
                avgEnergy = Math.max(0.035f, rms);
                lastAudioAt = now;
                return;
            }
            lastAudioAt = now;

            if (rms < 0.025f && bassRms < 0.018f) {
                avgBass = avgBass * 0.94f + bassRms * 0.06f;
                avgEnergy = avgEnergy * 0.94f + rms * 0.06f;
                return;
            }

            boolean bassHit = bassRms > 0.042f && bassRms > avgBass * 1.38f;
            boolean kickPeak = bassPeak > 0.20f && bassRms > avgBass * 1.22f;
            boolean fullTransient = rms > 0.085f && rms > avgEnergy * 1.46f;

            if ((bassHit || kickPeak || fullTransient) && (now - lastBeatAt >= MIN_BEAT_INTERVAL_MS)) {
                lastBeatAt = now;
                float ratio = Math.max(bassRms / Math.max(0.015f, avgBass), rms / Math.max(0.025f, avgEnergy));
                float intensity = Math.min(1f, Math.max(0.25f, (ratio - 1.15f) * 0.9f + bassRms * 1.4f));
                durMs = 16 + Math.round(intensity * 16f);
                amp = 90 + Math.round(intensity * 135f);
                avgBass = Math.max(avgBass * 0.85f + bassRms * 0.15f, bassRms * 0.86f);
                avgEnergy = Math.max(avgEnergy * 0.85f + rms * 0.15f, rms * 0.86f);
            } else {
                avgBass = avgBass * 0.88f + bassRms * 0.12f;
                avgEnergy = avgEnergy * 0.88f + rms * 0.12f;
            }
        }

        if (durMs > 0) {
            pulse(durMs, amp);
        }
    }

    private static void pulse(int durationMs, int amplitude) {
        try {
            Vibrator v = getVibrator();
            if (v == null || !v.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= 26) {
                int effAmp = v.hasAmplitudeControl()
                        ? Math.max(1, Math.min(255, amplitude))
                        : VibrationEffect.DEFAULT_AMPLITUDE;
                v.vibrate(VibrationEffect.createOneShot(durationMs, effAmp));
            } else {
                v.vibrate(durationMs);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Vibrator getVibrator() {
        Vibrator known = vibrator;
        if (known != null) return known;
        Context context = AirTok.context();
        if (context == null) return null;
        Object svc = context.getSystemService(Context.VIBRATOR_SERVICE);
        if (svc instanceof Vibrator) {
            vibrator = (Vibrator) svc;
            return vibrator;
        }
        return null;
    }

    private static SharedPreferences prefs() {
        Context context = AirTok.context();
        if (context == null) return null;
        try {
            return context.getSharedPreferences(AirTok.PREFS, Context.MODE_PRIVATE);
        } catch (Throwable ignored) {
            return null;
        }
    }
}

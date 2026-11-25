package com.fasdeq.airtok;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;






final class HeapShot {

    private HeapShot() {}

    private static final long MARK = 280L * 1024 * 1024;
    private static boolean taken;
    private static long checked, written;

    
    static void tick(java.util.function.Supplier<String> profiler) {
        
        if (Version.PROFILE) {
            long at = android.os.SystemClock.uptimeMillis();
            if (at - checked < 20000) return;
            checked = at;
            String profile = profiler.get();
            if (profile != null && profile.length() > 0) {
                for (String line : profile.split("\n")) android.util.Log.e("AirTok", "profile: " + line);
                
                try {
                    String text = "uptime " + (at / 1000) + " s\n" + profile + "\n--- probes ---\n" + Probe.recent();
                    File temp = new File(AirTok.context().getCacheDir(), "airtok-profile.txt");
                    Net.save(temp, text.getBytes("UTF-8"));
                    publish(temp, "airtok-profile.txt", "text/plain");
                } catch (Throwable error) {
                    Diary.note("profile file: " + error);
                }
            }
            return;
        }
        
        if (!Probe.ON) return;
        long now = android.os.SystemClock.uptimeMillis();
        if (now - checked < 5000) return;
        checked = now;
        try {
            Runtime runtime = Runtime.getRuntime();
            long used = runtime.totalMemory() - runtime.freeMemory();
            String profile = now - written > 60000 ? profiler.get() : null;
            if (profile != null && profile.length() > 0) {
                written = now;
                String text = "heap " + (used >> 20) + " MB of " + (runtime.maxMemory() >> 20) + " MB, uptime " + (now / 1000) + " s\n" + profile;
                File temp = new File(AirTok.context().getCacheDir(), "airtok-profile.txt");
                Net.save(temp, text.getBytes("UTF-8"));
                publish(temp, "airtok-profile.txt", "text/plain");
            }
            if (!taken && used > MARK) {
                taken = true;
                Diary.note("heap: " + (used >> 20) + " MB, taking a snapshot");
                File dump = new File(AirTok.context().getCacheDir(), "airtok-heap.hprof");
                android.os.Debug.dumpHprofData(dump.getAbsolutePath());
                publish(dump, "airtok-heap-" + (used >> 20) + "mb.hprof", "application/octet-stream");
                dump.delete();
            }
        } catch (Throwable error) {
            Diary.note("heap shot: " + error);
        }
    }

    private static void publish(File file, String name, String mime) throws Exception {
        if (Build.VERSION.SDK_INT < 29) return;
        Context c = AirTok.context();
        android.content.ContentResolver resolver = c.getContentResolver();
        Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        
        resolver.delete(collection, MediaStore.MediaColumns.DISPLAY_NAME + "=?", new String[]{name});
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/AirTok");
        Uri item = resolver.insert(collection, values);
        if (item == null) return;
        try (InputStream in = new FileInputStream(file); OutputStream out = resolver.openOutputStream(item)) {
            byte[] buffer = new byte[1 << 16];
            int read;
            while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
        }
    }
}

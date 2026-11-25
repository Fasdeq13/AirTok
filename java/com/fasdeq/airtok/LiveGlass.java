package com.fasdeq.airtok;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.RecordingCanvas;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.WeakHashMap;










final class LiveGlass {

    
    private static final String SHADER = "uniform shader img;\n"
            + "uniform float2 resolution;uniform float2 center;uniform float2 size;uniform float4 radius;\n"
            + "uniform float thickness;uniform float refract_index;uniform float refract_intensity;\n"
            + "uniform float4 foreground_color_premultiplied;\n"
            + "half sdfRect(half2 p, half4 r){r.xy=(p.x>0.0)?r.xy:r.zw;r.x=(p.y>0.0)?r.x:r.y;"
            + "half2 q=abs(p)-size+r.x;return length(max(q,0.0))+min(max(q.x,q.y),0.0)-r.x;}\n"
            + "half4 srcOver(half4 src,half4 dst){half3 o=(src.rgb+dst.rgb*(1.0-src.a));"
            + "float a=src.a+(1.0-src.a)*dst.a;return half4(o,a);}\n"
            + "half4 main(in float2 fragCoord){half2 p=fragCoord-center;half sd=sdfRect(p,radius);half2 uv=fragCoord;"
            + "if(sd<0.0){half sdX=sdfRect(p+half2(1.0,0.0),radius);half sdY=sdfRect(p+half2(0.0,1.0),radius);"
            + "half n_cos=max(thickness+sd,0.0)/thickness;half n_cos2=n_cos*n_cos;half n_sin=sqrt(1.0-n_cos2);"
            + "half3 normal=normalize(half3((sdX-sd)*n_cos,(sdY-sd)*n_cos,n_sin));"
            + "half3 refract_vec=refract(half3(0.0,0.0,-1.0),normal,1.0/refract_index);"
            + "half h=sd<-thickness?thickness:sqrt(sd*(-2.0*thickness-sd));"
            + "half refract_length=(h+8.0*thickness)/-refract_vec.z;"
            + "uv+=refract_vec.xy*refract_length*refract_intensity;"
            + "half3 light=normalize(half3(-0.577,-0.577,0.577));"
            + "half spec=pow(max(dot(normal,light),0.0),12.0)*0.28;"
            + "half rimGlow=pow(1.0-normal.z,3.0)*0.22;"
            + "half4 bg=img.eval(uv);"
            + "half3 c=bg.rgb+half3(spec+rimGlow);"
            + "return srcOver(half4(foreground_color_premultiplied),half4(c,bg.a));}"
            + "return srcOver(half4(foreground_color_premultiplied),img.eval(uv));}\n";

    static boolean available() {
        return android.os.Build.VERSION.SDK_INT >= 31;
    }

    private final RenderNode source = new RenderNode("airtok-glass-under");
    private final RenderNode fill = new RenderNode("airtok-glass");
    private final Outline outline = new Outline();
    private final int[] at = new int[2];
    private Object lens;
    private float blurNow = -1;
    private float lastW, lastH, lastTop, lastBottom, lastThick;
    private int lastTint;

    



    boolean draw(Canvas canvas, View view, float left, float top, float w, float h,
                 float rTop, float rBottom, int tint, boolean bend) {
        if (!canvas.isHardwareAccelerated() || w < 1 || h < 1) return false;
        View root = view.getRootView();
        if (root == null || root == view) return false;
        watch(root, view);
        float density = view.getResources().getDisplayMetrics().density;
        float blur = Math.max(2f, Design.style().blur * 2.2f) * density;
        int margin = Math.round(blur * 2f + 16f * density);
        int sw = Math.round(w) + 2 * margin, sh = Math.round(h) + 2 * margin;

        view.getLocationInWindow(at);
        float gx = at[0] + left, gy = at[1] + top;
        long now = android.os.SystemClock.uptimeMillis();
        Watcher watcher = ROOTS.get(root);
        int generation = watcher == null ? 0 : watcher.generation;
        
        
        
        
        boolean moved = !source.hasDisplayList() || sw != recordedW || sh != recordedH
                || Math.abs(gx - recordedX) > .5f || Math.abs(gy - recordedY) > .5f;
        
        
        
        
        
        boolean due = generation != recordedGeneration || now - recordedAt > 500L;
        boolean stale = moved || (due && now - recordedAt >= spacing);
        if (!stale && due && !settlePosted) {
            settlePosted = true;
            final View again = view;
            view.postOnAnimationDelayed(() -> { settlePosted = false; again.invalidate(); }, spacing);
        }
        if (recordingDepth > 0) {
            
            
            if (!source.hasDisplayList()) {
                android.graphics.Paint plain = new android.graphics.Paint();
                plain.setColor(tint);
                canvas.drawRect(0, 0, w, h, plain);
                return true;
            }
        } else if (stale) record(root, view, sw, sh, margin, gx, gy, generation, now);
        drawGlass(canvas, w, h, rTop, rBottom, tint, bend, margin, density, blur);
        return true;
    }

    private static int drawn, videosDrawn;
    
    private static int recordingDepth;
    private static long lastLog;
    private int recordedW, recordedH, recordedGeneration;
    private float recordedX = Float.NaN, recordedY = Float.NaN;
    private long recordedAt;

    
    private long spacing = 48L;
    private boolean settlePosted;

    private void record(View root, View view, int sw, int sh, int margin, float gx, float gy, int generation, long now) {
        long started = System.nanoTime();
        try {
            recordInner(root, view, sw, sh, margin, gx, gy, generation, now);
        } finally {
            float took = (System.nanoTime() - started) / 1e6f;
            
            spacing = took > 6f ? Math.min(200L, spacing + 32L) : Math.max(48L, spacing - 8L);
            if (took > 6f && now - slowLog > 2000L) {
                slowLog = now;
                android.util.Log.w("AirTokGlass", "slow glass record " + String.format(java.util.Locale.ROOT, "%.1f", took)
                        + " ms on " + view.getClass().getName() + ", " + drawn + " views, " + videosDrawn + " videos, next in " + spacing + " ms");
            }
        }
    }

    private static long slowLog;

    private void recordInner(View root, View view, int sw, int sh, int margin, float gx, float gy, int generation, long now) {
        recordedW = sw; recordedH = sh; recordedX = gx; recordedY = gy; recordedGeneration = generation; recordedAt = now;
        source.setPosition(0, 0, sw, sh);
        RecordingCanvas under = source.beginRecording(sw, sh);
        
        
        
        
        
        recordingDepth++;
        try {
            under.translate(margin - gx, margin - gy);
            areaL = gx - margin; areaT = gy - margin; areaR = gx - margin + sw; areaB = gy - margin + sh;
            videosHeld.clear();
            recording = videosHeld;
            drawn = 0; videosDrawn = 0;
            behind(root, view, under);
            if (now - lastLog > 3000L) {
                lastLog = now;
                android.util.Log.w("AirTokGlass", "live glass on " + view.getClass().getName() + " " + Math.round(gx) + "," + Math.round(gy)
                        + ": " + drawn + " views under it, " + videosDrawn + " videos");
            }
        } catch (Throwable error) {
            Diary.detail("live glass capture: " + error);
        } finally {
            recording = null;
            recordingDepth--;
            source.endRecording();
        }
    }

    private void drawGlass(Canvas canvas, float w, float h, float rTop, float rBottom, int tint, boolean bend,
                           int margin, float density, float blur) {
        for (VideoCopy copy : videosHeld) copy.want();
        if (blur != blurNow) {
            blurNow = blur;
            source.setRenderEffect(RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP));
        }

        int iw = Math.round(w), ih = Math.round(h);
        fill.setPosition(0, 0, iw, ih);
        float radius = Math.min(rTop, Math.min(w, h) / 2f);
        outline.setRoundRect(0, 0, iw, ih, radius);
        fill.setOutline(outline);
        fill.setClipToOutline(true);
        float thick = Math.max(1f, Math.min(11f * density, Math.min(w, h) / 5f));
        if (bend && android.os.Build.VERSION.SDK_INT >= 33) {
            Lens shaded = lens instanceof Lens ? (Lens) lens : null;
            if (shaded == null) { shaded = new Lens(); lens = shaded; }
            if (w != lastW || h != lastH || rTop != lastTop || rBottom != lastBottom || thick != lastThick || tint != lastTint) {
                lastW = w; lastH = h; lastTop = rTop; lastBottom = rBottom; lastThick = thick; lastTint = tint;
                float rt = Math.min(rTop, h / 2f), rb = Math.min(rBottom, h / 2f);
                fill.setRenderEffect(shaded.effect(w, h, rt, rb, thick, 0.75f, 1.5f, tint));
            }
        } else {
            fill.setRenderEffect(null);
        }
        RecordingCanvas glass = fill.beginRecording(iw, ih);
        glass.translate(-margin, -margin);
        glass.drawRenderNode(source);
        if (!bend || android.os.Build.VERSION.SDK_INT < 33) {
            glass.translate(margin, margin);
            glass.drawColor(tint);
        }
        fill.endRecording();
        canvas.drawRenderNode(fill);
    }

    

    




    private static final String BUBBLE = "uniform shader img;\n"
            + "uniform float2 center;uniform float2 size;uniform float radius;uniform float thickness;\n"
            + "uniform float refract_index;uniform float refract_intensity;uniform float dispersion;\n"
            + "half sdfRect(half2 p,half r){half2 q=abs(p)-size+r;return length(max(q,0.0))+min(max(q.x,q.y),0.0)-r;}\n"
            + "half4 main(float2 fragCoord){half2 p=fragCoord-center;half sd=sdfRect(p,radius);"
            + "if(sd>=0.0)return img.eval(fragCoord);"
            + "half sdX=sdfRect(p+half2(1.0,0.0),radius);half sdY=sdfRect(p+half2(0.0,1.0),radius);"
            + "half n_cos=max(thickness+sd,0.0)/thickness;half n_sin=sqrt(1.0-n_cos*n_cos);"
            + "half3 normal=normalize(half3((sdX-sd)*n_cos,(sdY-sd)*n_cos,n_sin));"
            + "half3 rv=refract(half3(0.0,0.0,-1.0),normal,1.0/refract_index);"
            + "half h=sd<-thickness?thickness:sqrt(sd*(-2.0*thickness-sd));"
            + "half2 d=rv.xy*((h+8.0*thickness)/-rv.z)*refract_intensity;"
            + "half4 g=img.eval(fragCoord+d);"
            + "half r=img.eval(fragCoord+d*(1.0+dispersion)).r;half b=img.eval(fragCoord+d*(1.0-dispersion)).b;"
            + "half edge=smoothstep(-thickness,0.0,sd);"
            + "half3 c=half3(r,g.g,b)+half3(0.035)*edge;"   
            + "return half4(c*g.a+half3(0.035)*(1.0-g.a),max(g.a,0.035));}\n";

    private final RenderNode bubble = new RenderNode("airtok-glass-bubble");
    private Object bubbleLens;

    
    boolean bubble(Canvas canvas, RenderNode content, android.graphics.RectF box, float magnify, float density, float strength) {
        if (!canvas.isHardwareAccelerated() || box.width() < 2 || box.height() < 2) return false;
        int w = Math.round(box.width()), h = Math.round(box.height());
        float radius = h / 2f;
        bubble.setPosition(0, 0, w, h);
        outline.setRoundRect(0, 0, w, h, radius);
        bubble.setOutline(outline);
        bubble.setClipToOutline(true);
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            BubbleLens lens = bubbleLens instanceof BubbleLens ? (BubbleLens) bubbleLens : null;
            if (lens == null) { lens = new BubbleLens(); bubbleLens = lens; }
            bubble.setRenderEffect(lens.effect(w, h, radius, Math.max(2f, Math.min(h / 5f, 8f * density)), strength));
        }
        RecordingCanvas inside = bubble.beginRecording(w, h);
        
        inside.translate(w / 2f, h / 2f);
        inside.scale(magnify, magnify);
        inside.translate(-box.centerX(), -box.centerY());
        inside.drawRenderNode(content);
        bubble.endRecording();
        int saved = canvas.save();
        canvas.translate(box.left, box.top);
        canvas.drawRenderNode(bubble);
        canvas.restoreToCount(saved);
        return true;
    }

    private static final class BubbleLens {
        private final RuntimeShader shader = new RuntimeShader(BUBBLE);

        RenderEffect effect(float w, float h, float radius, float thick, float strength) {
            shader.setFloatUniform("center", w / 2f, h / 2f);
            shader.setFloatUniform("size", w / 2f, h / 2f);
            shader.setFloatUniform("radius", radius);
            shader.setFloatUniform("thickness", thick);
            shader.setFloatUniform("refract_index", 1.5f);
            
            shader.setFloatUniform("refract_intensity", 0.12f * strength);
            shader.setFloatUniform("dispersion", 0.06f);
            return RenderEffect.createRuntimeShaderEffect(shader, "img");
        }
    }

    
    private static final class Lens {
        private final RuntimeShader shader = new RuntimeShader(SHADER);

        RenderEffect effect(float w, float h, float rTop, float rBottom, float thick, float intensity, float index, int tint) {
            float a = Color.alpha(tint) / 255f;
            shader.setFloatUniform("resolution", w, h);
            shader.setFloatUniform("center", w / 2f, h / 2f);
            shader.setFloatUniform("size", w / 2f, h / 2f);
            
            shader.setFloatUniform("radius", rBottom, rTop, rBottom, rTop);
            shader.setFloatUniform("thickness", thick);
            shader.setFloatUniform("refract_intensity", intensity);
            shader.setFloatUniform("refract_index", index);
            shader.setFloatUniform("foreground_color_premultiplied",
                    Color.red(tint) / 255f * a, Color.green(tint) / 255f * a, Color.blue(tint) / 255f * a, a);
            return RenderEffect.createRuntimeShaderEffect(shader, "img");
        }
    }

    

    private static final Matrix SCRATCH = new Matrix();

    




    
    private static float areaL, areaT, areaR, areaB;
    
    private static ArrayList<VideoCopy> recording;
    
    private final ArrayList<VideoCopy> videosHeld = new ArrayList<>();

    private static void behind(View root, View target, Canvas canvas) {
        
        View host = activityDecor(target);
        if (host != null && host != root && host.isAttachedToWindow() && host instanceof ViewGroup) {
            int[] a = new int[2], b = new int[2];
            host.getLocationOnScreen(a);
            root.getLocationOnScreen(b);
            int saved = canvas.save();
            canvas.translate(a[0] - b[0], a[1] - b[1]);
            mediaOf(host);   
            manual(canvas, (ViewGroup) host, a[0] - b[0], a[1] - b[1]);
            canvas.restoreToCount(saved);
        }
        ArrayList<View> chain = new ArrayList<>();
        for (View v = target; v != null; v = v.getParent() instanceof View ? (View) v.getParent() : null) {
            chain.add(0, v);
            if (v == root) break;
        }
        if (chain.isEmpty() || chain.get(0) != root) return;
        int saved = canvas.save();
        float ox = 0, oy = 0;
        for (int level = 0; level < chain.size() - 1; level++) {
            if (!(chain.get(level) instanceof ViewGroup)) break;
            ViewGroup group = (ViewGroup) chain.get(level);
            View next = chain.get(level + 1);
            Drawable background = group.getBackground();
            if (background != null) background.draw(canvas);
            
            
            
            int index = group.indexOfChild(next);
            float z = next.getZ();
            for (int i = 0; i < group.getChildCount(); i++) {
                if (i == index) continue;
                View other = group.getChildAt(i);
                float oz = other.getZ();
                if (oz < z || (oz == z && i < index)) child(canvas, group, other, ox, oy);
            }
            float dx = next.getLeft() - group.getScrollX(), dy = next.getTop() - group.getScrollY();
            canvas.translate(dx, dy);
            ox += dx + next.getTranslationX();
            oy += dy + next.getTranslationY();
            Matrix matrix = next.getMatrix();
            if (!matrix.isIdentity()) canvas.concat(matrix);
        }
        canvas.restoreToCount(saved);
    }

    
    private static View activityDecor(View view) {
        android.content.Context context = view.getContext();
        for (int i = 0; i < 12 && context instanceof android.content.ContextWrapper; i++) {
            if (context instanceof android.app.Activity) {
                android.view.Window window = ((android.app.Activity) context).getWindow();
                return window == null ? null : window.getDecorView();
            }
            android.content.Context next = ((android.content.ContextWrapper) context).getBaseContext();
            if (next == context) break;
            context = next;
        }
        return null;
    }

    
    private static boolean reaches(View view, float x, float y) {
        return x < areaR && x + view.getWidth() > areaL && y < areaB && y + view.getHeight() > areaT;
    }

    
    private static void child(Canvas canvas, ViewGroup group, View child, float ox, float oy) {
        if (child.getVisibility() != View.VISIBLE || child.getAlpha() <= 0.01f
                || child.getWidth() <= 0 || child.getHeight() <= 0) return;
        float cx = ox + child.getLeft() - group.getScrollX() + child.getTranslationX();
        float cy = oy + child.getTop() - group.getScrollY() + child.getTranslationY();
        if (!reaches(child, cx, cy)) return;   
        int saved = canvas.save();
        canvas.translate(child.getLeft() - group.getScrollX(), child.getTop() - group.getScrollY());
        Matrix matrix = child.getMatrix();
        if (!matrix.isIdentity()) canvas.concat(matrix);
        if (child instanceof android.view.SurfaceView) {
            
            
            
            VideoCopy copy = VideoCopy.of((android.view.SurfaceView) child);
            copy.draw(canvas);
            if (recording != null && !recording.contains(copy)) recording.add(copy);
            videosDrawn++;
            canvas.restoreToCount(saved);
            return;
        }
        if (child.getAlpha() < 0.99f) {
            canvas.saveLayerAlpha(0, 0, child.getWidth(), child.getHeight(), Math.round(child.getAlpha() * 255f));
        }
        
        
        if (child instanceof ViewGroup && anyVideo((ViewGroup) child, cx, cy)) manual(canvas, (ViewGroup) child, cx, cy);
        else child.draw(canvas);
        drawn++;
        canvas.restoreToCount(saved);
    }

    
    private static void manual(Canvas canvas, ViewGroup group, float ox, float oy) {
        Drawable background = group.getBackground();
        if (background != null) background.draw(canvas);
        int count = group.getChildCount();
        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++) order[i] = i;
        final ViewGroup g = group;
        java.util.Arrays.sort(order, (x, y) -> {
            int byZ = Float.compare(g.getChildAt(x).getZ(), g.getChildAt(y).getZ());
            return byZ != 0 ? byZ : Integer.compare(x, y);
        });
        for (Integer i : order) child(canvas, group, group.getChildAt(i), ox, oy);
    }

    





    private static boolean anyVideo(ViewGroup group, float ox, float oy) {
        if (holdsVideo(group, ox, oy, 0)) return true;
        Watcher watcher = ROOTS.get(group.getRootView());
        if (watcher != null) {
            for (android.view.SurfaceView video : watcher.media.videos) {
                if (video != null && video.isAttachedToWindow() && inside(video, group)) return true;
            }
        }
        View playing = GlassVideo.render(group.getRootView());
        return playing != null && inside(playing, group);
    }

    
    private static boolean holdsVideo(ViewGroup group, float ox, float oy, int depth) {
        if (depth > 30) return false;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE || child.getWidth() <= 0 || child.getHeight() <= 0) continue;
            float cx = ox + child.getLeft() - group.getScrollX() + child.getTranslationX();
            float cy = oy + child.getTop() - group.getScrollY() + child.getTranslationY();
            if (!reaches(child, cx, cy)) continue;
            if (child instanceof android.view.SurfaceView) return true;
            if (child instanceof ViewGroup && holdsVideo((ViewGroup) child, cx, cy, depth + 1)) return true;
        }
        return false;
    }

    private static Media mediaOf(View root) {
        Watcher watcher = ROOTS.get(root);
        if (watcher == null) {
            watcher = new Watcher();
            ROOTS.put(root, watcher);
            ViewTreeObserver observer = root.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(watcher);
            observer.addOnScrollChangedListener(watcher);
        }
        return watcher.media(root);
    }

    private static boolean inside(View view, View group) {
        for (Object up = view.getParent(); up instanceof View; up = ((View) up).getParent()) if (up == group) return true;
        return false;
    }

    
    static final class Media {
        final ArrayList<android.view.SurfaceView> videos = new ArrayList<>();
        final ArrayList<View> pictures = new ArrayList<>();
        long at;

        void find(View root) {
            videos.clear();
            pictures.clear();
            at = android.os.SystemClock.uptimeMillis();
            
            View playing = GlassVideo.render(root);
            if (playing instanceof android.view.SurfaceView) { videos.add((android.view.SurfaceView) playing); pictures.add(playing); }
            long started = System.nanoTime();
            int[] budget = {30000};
            walk(root, budget);
            float took = (System.nanoTime() - started) / 1e6f;
            if (took > 4f) android.util.Log.w("AirTokGlass", "slow media scan " + String.format(java.util.Locale.ROOT, "%.1f", took)
                    + " ms, " + (30000 - budget[0]) + " views, " + pictures.size() + " pictures");
        }

        private void walk(View view, int[] budget) {
            if (budget[0]-- <= 0 || view.getVisibility() != View.VISIBLE || view.getWidth() <= 0 || view.getHeight() <= 0) return;
            if (view instanceof android.view.SurfaceView) {
                if (!videos.contains(view)) { videos.add((android.view.SurfaceView) view); pictures.add(view); }
                return;
            }
            if (view instanceof android.view.TextureView) { pictures.add(view); return; }
            if (view instanceof android.widget.ImageView) {
                Drawable shown = ((android.widget.ImageView) view).getDrawable();
                float d = view.getResources().getDisplayMetrics().density;
                if (shown != null && !(shown instanceof android.graphics.drawable.ColorDrawable)
                        && view.getWidth() >= 40 * d && view.getHeight() >= 40 * d) pictures.add(view);
                return;
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) walk(group.getChildAt(i), budget);
            }
        }
    }

    





    static boolean worthIt(View view, float left, float top, float w, float h) {
        View root = view.getRootView();
        if (root == null) return false;
        View host = activityDecor(view);
        if (host != null && host != root) return true;
        Media media = mediaOf(root);
        int[] spot = new int[2];
        view.getLocationInWindow(spot);
        float gl = spot[0] + left - 8, gt = spot[1] + top - 8, gr = gl + w + 16, gb = gt + h + 16;
        for (View picture : media.pictures) {
            if (picture == null || picture == view || !picture.isShown() || inside(picture, view)) continue;
            picture.getLocationInWindow(spot);
            if (spot[0] < gr && spot[0] + picture.getWidth() > gl && spot[1] < gb && spot[1] + picture.getHeight() > gt) return true;
        }
        return false;
    }

    





    private static final class VideoCopy implements android.view.PixelCopy.OnPixelCopyFinishedListener {
        private static final WeakHashMap<android.view.SurfaceView, VideoCopy> ALL = new WeakHashMap<>();
        private static final android.os.Handler MAIN = new android.os.Handler(android.os.Looper.getMainLooper());
        private static final int WIDTH = 270;
        final java.lang.ref.WeakReference<android.view.SurfaceView> view;
        final RenderNode node = new RenderNode("airtok-glass-video");
        final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG);
        android.graphics.Bitmap bitmap;
        long wanted;
        boolean busy, scheduled;

        VideoCopy(android.view.SurfaceView view) {
            this.view = new java.lang.ref.WeakReference<>(view);
        }

        static VideoCopy of(android.view.SurfaceView view) {
            VideoCopy copy = ALL.get(view);
            if (copy == null) { copy = new VideoCopy(view); ALL.put(view, copy); }
            return copy;
        }

        void want() {
            wanted = android.os.SystemClock.uptimeMillis();
            if (!scheduled) { scheduled = true; MAIN.post(this::take); }
        }

        
        private static VideoCopy lastGood;

        void draw(Canvas canvas) {
            android.view.SurfaceView source = view.get();
            if (source == null) return;
            wanted = android.os.SystemClock.uptimeMillis();
            node.setPosition(0, 0, source.getWidth(), source.getHeight());
            if (!scheduled) { scheduled = true; MAIN.post(this::take); }
            if (node.hasDisplayList()) canvas.drawRenderNode(node);
            else if (lastGood != null && lastGood != this && lastGood.node.hasDisplayList()) {
                float sx = source.getWidth() / (float) Math.max(1, lastGood.node.getWidth());
                float sy = source.getHeight() / (float) Math.max(1, lastGood.node.getHeight());
                int saved = canvas.save();
                canvas.scale(sx, sy);
                canvas.drawRenderNode(lastGood.node);
                canvas.restoreToCount(saved);
            }
        }

        private void take() {
            scheduled = false;
            android.view.SurfaceView source = view.get();
            if (source == null || !source.isAttachedToWindow() || busy) return;
            if (android.os.SystemClock.uptimeMillis() - wanted > 1500L) return;   
            android.view.Surface surface = source.getHolder().getSurface();
            if (surface == null || !surface.isValid() || source.getWidth() <= 0) { later(); return; }
            int h = Math.max(1, Math.round(WIDTH * source.getHeight() / (float) source.getWidth()));
            if (bitmap == null || bitmap.getHeight() != h) bitmap = android.graphics.Bitmap.createBitmap(WIDTH, h, android.graphics.Bitmap.Config.ARGB_8888);
            busy = true;
            try {
                android.view.PixelCopy.request(source, bitmap, this, MAIN);
            } catch (Throwable error) {
                busy = false;
                later();
            }
        }

        private long copyLog;

        @Override
        public void onPixelCopyFinished(int result) {
            busy = false;
            long t = android.os.SystemClock.uptimeMillis();
            if (t - copyLog > 3000L) { copyLog = t; android.util.Log.w("AirTokGlass", "video copy: " + result); }
            android.view.SurfaceView source = view.get();
            if (result == android.view.PixelCopy.SUCCESS && source != null) {
                RecordingCanvas canvas = node.beginRecording(source.getWidth(), source.getHeight());
                canvas.drawBitmap(bitmap, null, new android.graphics.Rect(0, 0, source.getWidth(), source.getHeight()), paint);
                node.endRecording();
                lastGood = this;
                
                Watcher watcher = ROOTS.get(source.getRootView());
                if (watcher != null) watcher.redraw();
            }
            later();
        }

        private void later() {
            if (!scheduled) { scheduled = true; MAIN.postDelayed(this::take, 50L); }
        }
    }

    

    private static final WeakHashMap<View, Watcher> ROOTS = new WeakHashMap<>();

    




    private static void watch(View root, View glass) {
        Watcher watcher = ROOTS.get(root);
        if (watcher == null) {
            watcher = new Watcher();
            ROOTS.put(root, watcher);
            ViewTreeObserver observer = root.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(watcher);
            observer.addOnScrollChangedListener(watcher);
        }
        watcher.glass.put(glass, Boolean.TRUE);
    }

    



    private static final class Watcher implements ViewTreeObserver.OnGlobalLayoutListener, ViewTreeObserver.OnScrollChangedListener {
        final WeakHashMap<View, Boolean> glass = new WeakHashMap<>();
        int generation;

        private final Media media = new Media();

        Media media(View root) {
            if (android.os.SystemClock.uptimeMillis() - media.at > 2000L) media.find(root);
            return media;
        }

        @Override public void onGlobalLayout() { changed(); }

        @Override public void onScrollChanged() { changed(); }

        void changed() {
            if (!Design.frosted()) return;
            generation++;
            redraw();
        }

        void redraw() {
            for (View view : new ArrayList<>(glass.keySet())) {
                if (view != null && view.isShown()) view.invalidate();
            }
        }
    }
}

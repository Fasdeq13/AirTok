package com.fasdeq.airtok;

import android.graphics.*;
import android.os.Build;
import android.view.View;


final class GlassOptics {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF local = new RectF();
    private final Matrix transform = new Matrix();
    private final int[] point = new int[2];
    private Lens lens;
    private boolean failed;
    private Integer surfaceColour;
    private boolean surfaceDark;

    void draw(Canvas canvas, View view, RectF bounds, float top, float bottom, int colour, float press) {
        float density = view.getResources().getDisplayMetrics().density;
        if (Color.alpha(colour) < 255) {
            
            
            boolean dark = Themes.isDark();
            if (surfaceColour == null || surfaceDark != dark) { surfaceColour = Skin.remembered(view.getContext()).card; surfaceDark = dark; }
            float alpha = Color.alpha(colour) / 255f;
            colour = Color.rgb(Math.round(Color.red(colour)*alpha+Color.red(surfaceColour)*(1-alpha)),
                    Math.round(Color.green(colour)*alpha+Color.green(surfaceColour)*(1-alpha)),
                    Math.round(Color.blue(colour)*alpha+Color.blue(surfaceColour)*(1-alpha)));
        }
        int save = canvas.save();
        canvas.translate(bounds.left,bounds.top);
        local.set(0,0,bounds.width(),bounds.height());
        top=Math.min(top,local.height()/2f); bottom=Math.min(bottom,local.height()/2f);
        if(Design.fluent()){top=Math.min(top,density*Design.style().card);bottom=Math.min(bottom,density*Design.style().card);}
        long now = android.os.SystemClock.uptimeMillis();
        if (now - probedAt > 500L && LiveGlass.available()) {
            probedAt = now;
            boolean now_ = false;
            try { now_ = LiveGlass.worthIt(view, bounds.left, bounds.top, local.width(), local.height()); }
            catch (Throwable ignored) { }
            
            
            if (now_) { worth = true; misses = 0; }
            else if (++misses >= 6) worth = false;
        }
        if (worth && LiveGlass.available() && canvas.isHardwareAccelerated() && !failed) {
            try {
                if (live == null) live = new LiveGlass();
                
                
                int tint = (colour & 0xffffff) | (Design.fluent() ? 0x99000000 : 0x2E000000);
                if (live.draw(canvas, view, bounds.left, bounds.top, local.width(), local.height(), top, bottom,
                        tint, !Design.fluent())) {
                    rim(canvas, top, density, press);
                    sheen(canvas, top, density);
                    veil(canvas,view,top,bottom,density,colour);
                    canvas.restoreToCount(save);
                    return;
                }
            } catch (Throwable error) {
                failed = true;
                Diary.note("liquid glass: " + error);
                android.util.Log.e("AirTokGlass", "Live glass could not be rendered", error);
            }
        }
        
        paint.setShader(null); paint.setStyle(Paint.Style.FILL);
        paint.setColor((colour & 0xffffff) | (Design.fluent()?0x30000000:0x18000000));
        canvas.drawRoundRect(local,top,top,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1,density*.75f));
        paint.setShader(new LinearGradient(0,0,local.width(),local.height(),Design.fluent()
                ?new int[]{0x30ffffff,0x14ffffff}:new int[]{0xB5ffffff,0x1Cffffff,0x60ffffff},null,Shader.TileMode.CLAMP));
        local.inset(density*.4f,density*.4f);canvas.drawRoundRect(local,top,top,paint);
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);
        local.inset(-density*.4f,-density*.4f);
        sheen(canvas, top, density);
        veil(canvas,view,top,bottom,density,colour);
        canvas.restoreToCount(save);
    }

    private LiveGlass live;
    private long probedAt;
    private boolean worth;
    private int misses;

    private void sheen(Canvas canvas, float radius, float density) {
        float h = local.height(), w = local.width();
        if (h < density * 8f || w < density * 8f) return;
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, 0, h * 0.45f,
                new int[]{0x24ffffff, 0x08ffffff, 0x00ffffff},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        float r = Math.min(radius, h / 2f);
        canvas.drawRoundRect(local, r, r, paint);
        paint.setShader(null);
    }

    private void rim(Canvas canvas, float radius, float density, float press) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, density * .85f));
        paint.setShader(new LinearGradient(0, 0, local.width(), local.height(), Design.fluent()
                ? new int[]{0x38ffffff, 0x12ffffff}
                : new int[]{0xB8ffffff, 0x20ffffff, 0x65ffffff}, null, Shader.TileMode.CLAMP));
        local.inset(density * .4f, density * .4f);
        float r = Math.min(radius, local.height() / 2f);
        canvas.drawRoundRect(local, r, r, paint);
        local.inset(-density * .4f, -density * .4f);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        if (press > 0f) {
            paint.setColor(Color.argb(Math.round(press * 32), 255, 255, 255));
            canvas.drawRoundRect(local, radius, radius, paint);
        }
    }

    private int busyVersion = Integer.MIN_VALUE;
    private Bitmap busyBitmap;
    private float busyX = Float.NaN, busyY = Float.NaN;
    private boolean busyAnswer;

    




    private boolean busy(GlassBackdrop.Frame frame, float x, float y, float w, float h) {
        if (frame.version == busyVersion && frame.bitmap == busyBitmap && Math.abs(x - busyX) < 2 && Math.abs(y - busyY) < 2) return busyAnswer;
        busyVersion = frame.version; busyBitmap = frame.bitmap; busyX = x; busyY = y;
        Bitmap bitmap = frame.bitmap;
        int bw = bitmap.getWidth(), bh = bitmap.getHeight();
        
        float margin = 12f;
        int left = Math.max(0, Math.round((x - margin) * frame.scale)), top = Math.max(0, Math.round((y - margin) * frame.scale));
        int right = Math.min(bw - 1, Math.round((x + w + margin) * frame.scale)), bottom = Math.min(bh - 1, Math.round((y + h + margin) * frame.scale));
        if (right <= left || bottom <= top) { busyAnswer = false; return false; }
        int first = bitmap.getPixel(left, top), most = 0;
        for (int i = 0; i <= 6; i++) for (int j = 0; j <= 4; j++) {
            int px = left + (right - left) * i / 6, py = top + (bottom - top) * j / 4;
            int c = bitmap.getPixel(px, py);
            int d = Math.max(Math.abs(((c >> 16) & 255) - ((first >> 16) & 255)),
                    Math.max(Math.abs(((c >> 8) & 255) - ((first >> 8) & 255)), Math.abs((c & 255) - (first & 255))));
            if (d > most) most = d;
        }
        busyAnswer = most > 18;
        return busyAnswer;
    }

    private final Path veilPath = new Path();
    private final float[] veilRadii = new float[8];
    





    private void veil(Canvas canvas, View view, float top, float bottom, float density, int surface) {
        float w = local.width(), h = local.height();
        boolean big = h >= density * 120 && w >= view.getResources().getDisplayMetrics().widthPixels * .55f;
        if (!Design.fluent() && !big) return;
        int page = Design.pageColour(view.getContext());
        
        if ((NavBar.bright(page) < .5f) != (NavBar.bright(surface | 0xFF000000) < .5f)) page = surface;
        float amount = Design.fluent() ? (big ? .76f : .66f) : .5f;
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor((page & 0xffffff) | (Math.round(amount * 255f) << 24));
        veilRadii[0] = veilRadii[1] = veilRadii[2] = veilRadii[3] = top;
        veilRadii[4] = veilRadii[5] = veilRadii[6] = veilRadii[7] = bottom;
        veilPath.reset();
        veilPath.addRoundRect(local, veilRadii, Path.Direction.CW);
        canvas.drawPath(veilPath, paint);
    }

    
    private static final class Lens {
        private static final String BLUR="uniform shader content;uniform float2 axis;half4 main(float2 p){return content.eval(p)*.4026+(content.eval(p+axis)+content.eval(p-axis))*.2442+(content.eval(p+axis*2.)+content.eval(p-axis*2.))*.0545;}";
        private RuntimeShader shader;
        private RuntimeShader acrylic;
        private final RuntimeShader blurX=new RuntimeShader(BLUR),blurY=new RuntimeShader(BLUR);
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] hsv=new float[3];
        private Bitmap bound;
        private LinearGradient rim;private float rimW,rimH;
        private BitmapShader input;
        void paint(Canvas canvas, GlassBackdrop.Frame frame, Matrix matrix, float x,float y,
                   float w,float h,float top,float bottom,int colour,float press,float dp) {
            if (bound != frame.bitmap) {
                bound=frame.bitmap;
                input=new BitmapShader(bound,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);
                input.setFilterMode(BitmapShader.FILTER_MODE_LINEAR);
            }
            matrix.setScale(1/frame.scale,1/frame.scale);matrix.postTranslate(-x,-y);
            input.setLocalMatrix(matrix);
            
            
            
            
            
            float blur=dp*Design.style().blur;
            blurX.setInputShader("content",input);blurX.setFloatUniform("axis",blur,0);
            blurY.setInputShader("content",blurX);blurY.setFloatUniform("axis",0,blur);
            if(Design.fluent()){
                if(acrylic==null)acrylic=new RuntimeShader(FluentShader.ACRYLIC);
                acrylic.setInputShader("content",blurY);
                acrylic.setFloatUniform("size",w,h);
                acrylic.setFloatUniform("radii",Math.min(top,dp*Design.style().card),Math.min(bottom,dp*Design.style().card));
                acrylic.setFloatUniform("tint",Color.red(colour)/255f,Color.green(colour)/255f,Color.blue(colour)/255f,Design.style().tint);
                acrylic.setFloatUniform("grainAmount",Design.style().grain);
                acrylic.setFloatUniform("density",dp);acrylic.setFloatUniform("pressed",press);
                paint.setShader(acrylic);canvas.drawRect(0,0,w,h,paint);return;
            }
            if(shader==null)shader=new RuntimeShader(GlassShader.LENS);
            shader.setInputShader("content",blurY);
            shader.setFloatUniform("margin",0);
            shader.setFloatUniform("viewSize",w,h);
            shader.setFloatUniform("shape1",w/2,h/2,w/2,h/2);
            shader.setFloatUniform("shape1L",w/2,h/2,w/2,h/2);
            shader.setFloatUniform("radii1",top,top,bottom,bottom);
            shader.setFloatUniform("shape2",0,0,0,0);shader.setFloatUniform("radius2",0);shader.setFloatUniform("blendK",0);
            shader.setFloatUniform("bevel",Math.max(dp*3,Math.min(dp*22,h*.27f)));
            shader.setFloatUniform("refractPx",Math.min(dp*25,h*.42f)*Design.style().refraction);
            shader.setFloatUniform("falloff",2);shader.setFloatUniform("refractDir",-1);
            shader.setFloatUniform("rimSoft",dp*.7f);
            shader.setFloatUniform("sampleLo",-x,-y);
            shader.setFloatUniform("sampleHi",frame.bitmap.getWidth()/frame.scale-x,frame.bitmap.getHeight()/frame.scale-y);
            shader.setFloatUniform("dispersion",Design.style().dispersion);
            float tx=viewTouchX,ty=viewTouchY;
            shader.setFloatUniform("lightDir",-.7071f+tx*.16f,-.7071f+ty*.16f);
            shader.setFloatUniform("specStrength",.85f+press*.35f);
            shader.setFloatUniform("rimBandMax",Math.max(2,Math.min(dp*4,h*.12f)));
            boolean dark=NavBar.bright(colour)<.5f;
            shader.setFloatUniform("tintColor",dark?.10f:.96f,dark?.11f:.97f,dark?.13f:.99f,.08f);
            shader.setFloatUniform("adaptiveTint",0);
            Color.colorToHSV(colour,hsv);
            shader.setFloatUniform("glassTint",Color.red(colour)/255f,Color.green(colour)/255f,Color.blue(colour)/255f,hsv[1]>.28f?.10f:.025f);
            shader.setFloatUniform("dimAmount",0);shader.setFloatUniform("satFactor",1.12f);
            shader.setFloatUniform("press",press);
            shader.setFloatUniform("touchPos",w*(.5f+tx*.5f),h*(.5f+ty*.5f));
            shader.setFloatUniform("touchAmp",press*.75f);
            paint.setShader(shader);canvas.drawRect(0,0,w,h,paint);
        }
    }
    static float viewTouchX,viewTouchY;
}

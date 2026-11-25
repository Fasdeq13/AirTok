package com.fasdeq.airtok;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.view.Choreographer;
import android.view.View;
import android.view.animation.AnimationUtils;

import java.util.ArrayList;
import java.util.WeakHashMap;















public final class AvatarShape {

    private AvatarShape() {}

    static final String KEY = "avatar_shape";
    static final String[] ALL = {
            "circle", "rounded", "square", "cookie", "clover", "burst", "sun",
            "pentagon", "hexagon", "heart"};

    
    private static final int POINTS = 144;

    
    private static volatile int chosen = -1;
    private static final Path[] UNIT = new Path[ALL.length];
    private static final Matrix PLACE = new Matrix();

    static String chosen() {
        return ALL[index()];
    }

    static void choose(Context context, String shape) {
        int found = 0;
        for (int i = 0; i < ALL.length; i++) if (ALL[i].equals(shape)) found = i;
        chosen = found;
        context.getSharedPreferences(AirTok.PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY, ALL[found]).apply();
    }

    static void reloadSettings() {
        chosen = -1;
        spin = -1;
    }

    private static int index() {
        int known = chosen;
        if (known >= 0) return known;
        Context context = AirTok.context();
        if (context == null) return 0;  
        int found = 0;
        try {
            SharedPreferences prefs = context.getSharedPreferences(AirTok.PREFS, Context.MODE_PRIVATE);
            String name = prefs.getString(KEY, ALL[0]);
            for (int i = 0; i < ALL.length; i++) if (ALL[i].equals(name)) found = i;
        } catch (Throwable ignored) { }
        chosen = found;
        return found;
    }

    
    public static void addCircle(Path path, float x, float y, float radius, Path.Direction direction) {
        int shape = index();
        if (shape > 0 && radius > 0) {
            try {
                Path unit = unit(shape);
                synchronized (PLACE) {
                    PLACE.setScale(radius, radius);
                    PLACE.postTranslate(x, y);
                    path.addPath(unit, PLACE);
                }
                synchronized (SHAPED) {
                    SHAPED.put(path, new float[]{x, y, radius});
                }
                return;
            } catch (Throwable error) {
                
            }
        }
        path.addCircle(x, y, radius, direction);
    }

    

    private static final Path RING = new Path();
    private static final Path PART = new Path();
    private static final Matrix RING_PLACE = new Matrix();
    private static final RectF INBOX_RING = new RectF();

    









    public static void drawArc(Canvas canvas, RectF oval, float start, float sweep,
                               boolean useCentre, Paint paint) {
        Object who = takeOwner();
        if (oval != null && !useCentre && InboxMd3.storyRing(who)) {
            synchronized (RING) {
                float nativeStroke = paint.getStrokeWidth();
                float stroke = 2f * ((View) who).getResources().getDisplayMetrics().density;
                INBOX_RING.set(oval);
                float inset = (stroke - nativeStroke) / 2f;
                INBOX_RING.inset(inset, inset);
                paint.setStrokeWidth(stroke);
                try { storyArc(canvas, INBOX_RING, start, sweep, false, paint, who); }
                finally { paint.setStrokeWidth(nativeStroke); }
            }
            return;
        }
        storyArc(canvas, oval, start, sweep, useCentre, paint, who);
    }

    private static void storyArc(Canvas canvas, RectF oval, float start, float sweep,
                                 boolean useCentre, Paint paint, Object who) {
        int shape = index();
        if (shape > 0 && !useCentre && oval != null && sweep != 0) {
            try {
                synchronized (RING) {
                    RING.rewind();
                    RING_PLACE.setScale(oval.width() / 2f, oval.height() / 2f);
                    RING_PLACE.postTranslate(oval.centerX(), oval.centerY());
                    RING.addPath(unit(shape), RING_PLACE);
                    
                    float turn = 0;
                    if (spins()) {
                        turn = angle();
                        RING_PLACE.setRotate(turn, oval.centerX(), oval.centerY());
                        RING.transform(RING_PLACE);
                        again(who);
                    }
                    if (Math.abs(sweep) >= 359.5f) {
                        canvas.drawPath(RING, paint);
                        return;
                    }
                    
                    
                    android.graphics.PathMeasure measure = new android.graphics.PathMeasure(RING, true);
                    float length = measure.getLength();
                    float from = start + (sweep < 0 ? sweep : 0) + 90f - turn;
                    from = ((from % 360f) + 360f) % 360f / 360f * length;
                    float span = Math.min(Math.abs(sweep), 360f) / 360f * length;
                    PART.rewind();
                    if (from + span <= length) {
                        measure.getSegment(from, from + span, PART, true);
                    } else {
                        measure.getSegment(from, length, PART, true);
                        measure.getSegment(0, from + span - length, PART, true);
                    }
                    canvas.drawPath(PART, paint);
                    return;
                }
            } catch (Throwable error) {
                
            }
        }
        canvas.drawArc(oval, start, sweep, useCentre, paint);
    }

    

    static final String SPIN_KEY = "avatar_spin";
    
    private static final long TURN = 9000;

    
    private static volatile int spin = -1;

    






    private static final WeakHashMap<Path, float[]> SHAPED = new WeakHashMap<>();

    static boolean spins() {
        int known = spin;
        if (known < 0) {
            Context context = AirTok.context();
            if (context == null) return false;
            try {
                known = context.getSharedPreferences(AirTok.PREFS, Context.MODE_PRIVATE)
                        .getBoolean(SPIN_KEY, false) ? 1 : 0;
            } catch (Throwable error) {
                known = 0;
            }
            spin = known;
        }
        return known == 1;
    }

    static void setSpin(Context context, boolean on) {
        spin = on ? 1 : 0;
        context.getSharedPreferences(AirTok.PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(SPIN_KEY, on).apply();
    }

    
    static float angle() {
        return (AnimationUtils.currentAnimationTimeMillis() % TURN) * 360f / TURN;
    }

    
    static boolean turning() {
        return index() > 0 && spins();
    }

    
    public static void reset(Path path) {
        synchronized (SHAPED) {
            SHAPED.remove(path);
        }
        path.reset();
    }

    private static Object owner;

    





    public static void owner(Object who) {
        owner = who;
    }

    private static Object takeOwner() {
        Object who = owner;
        owner = null;
        return who;
    }

    private static final Path TURNED = new Path();
    private static final Path PLACED = new Path();
    private static final Matrix TURN_PLACE = new Matrix();
    private static final RectF WHOLE = new RectF();
    private static final RectF SHAPE = new RectF();

    





    private static Path turned(Path path) {
        if (!turning()) return path;
        float[] at;
        synchronized (SHAPED) {
            at = SHAPED.get(path);
        }
        if (at == null) return path;
        TURN_PLACE.setScale(at[2], at[2]);
        TURN_PLACE.postTranslate(at[0], at[1]);
        PLACED.rewind();
        PLACED.addPath(unit(index()), TURN_PLACE);
        TURN_PLACE.setRotate(angle(), at[0], at[1]);
        path.computeBounds(WHOLE, true);
        PLACED.computeBounds(SHAPE, true);
        TURNED.set(path);
        if (Math.abs(WHOLE.left - SHAPE.left) < 1 && Math.abs(WHOLE.top - SHAPE.top) < 1
                && Math.abs(WHOLE.right - SHAPE.right) < 1 && Math.abs(WHOLE.bottom - SHAPE.bottom) < 1) {
            TURNED.transform(TURN_PLACE);
        } else {
            
            
            TURNED.op(PLACED, Path.Op.XOR);
            PLACED.transform(TURN_PLACE);
            TURNED.op(PLACED, Path.Op.XOR);
        }
        return TURNED;
    }

    
    public static void drawPath(Canvas canvas, Path path, Paint paint) {
        Object who = takeOwner();
        synchronized (TURNED) {
            Path drawn = path;
            try {
                drawn = turned(path);
            } catch (Throwable error) {
                
            }
            canvas.drawPath(drawn, paint);
            if (drawn != path) again(who);
        }
    }

    
    public static boolean clipPath(Canvas canvas, Path path) {
        Object who = takeOwner();
        synchronized (TURNED) {
            Path drawn = path;
            try {
                drawn = turned(path);
            } catch (Throwable error) {
                
            }
            boolean left = canvas.clipPath(drawn);
            if (drawn != path) again(who);
            return left;
        }
    }

    private static final WeakHashMap<Object, Boolean> WAITING = new WeakHashMap<>();
    private static boolean posted;

    




    static void again(Object who) {
        if (who == null || Looper.myLooper() != Looper.getMainLooper()) return;
        WAITING.put(who, Boolean.TRUE);
        if (posted) return;
        posted = true;
        
        
        Choreographer.getInstance().postFrameCallbackDelayed(TICK, 30L);
    }

    private static final Choreographer.FrameCallback TICK = new Choreographer.FrameCallback() {
        @Override public void doFrame(long frameTimeNanos) {
            posted = false;
            if (!turning()) {
                WAITING.clear();
                return;
            }
            ArrayList<Object> due = new ArrayList<>(WAITING.keySet());
            WAITING.clear();
            for (Object who : due) {
                try {
                    if (who instanceof View) ((View) who).invalidate();
                    else if (who instanceof Drawable) ((Drawable) who).invalidateSelf();
                } catch (Throwable ignored) { }
            }
        }
    };

    
    static Path chosenUnit() {
        return unit(index());
    }

    
    static Path unit(int shape) {
        Path known = UNIT[shape];
        if (known != null) return known;
        Path made = new Path();
        switch (ALL[shape]) {
            case "rounded":
                made.addRoundRect(new RectF(-1, -1, 1, 1), 0.44f, 0.44f, Path.Direction.CW);
                break;
            case "square":
                made.addRect(-1, -1, 1, 1, Path.Direction.CW);
                break;
            case "heart":
                
                
                
                
                made.arcTo(new RectF(-1f, -1f, 0.1f, 0.1f), -35.1f, -174.9f, true);
                made.cubicTo(-0.70f, 0.25f, -0.28f, 0.70f, 0f, 1f);
                made.cubicTo(0.28f, 0.70f, 0.70f, 0.25f, 0.926f, -0.175f);
                made.arcTo(new RectF(-0.1f, -1f, 1f, 0.1f), 30f, -174.9f, false);
                made.close();
                break;
            case "circle":
                made.addCircle(0, 0, 1, Path.Direction.CW);
                break;
            default:
                for (int i = 0; i < POINTS; i++) {
                    double around = 2 * Math.PI * i / POINTS - Math.PI / 2;
                    float reach = reach(ALL[shape], around + Math.PI / 2);
                    float px = (float) (reach * Math.cos(around)), py = (float) (reach * Math.sin(around));
                    if (i == 0) made.moveTo(px, py);
                    else made.lineTo(px, py);
                }
                made.close();
        }
        UNIT[shape] = made;
        return made;
    }

    
    static float reach(String shape, double around) {
        switch (shape) {
            case "cookie": return 0.92f + 0.08f * (float) Math.cos(9 * around);
            case "clover": return 0.84f + 0.16f * (float) Math.cos(4 * around + Math.PI);
            case "burst": return 0.90f + 0.10f * (float) Math.cos(12 * around);
            case "sun": return 0.87f + 0.13f * (float) Math.cos(8 * around);
            case "pentagon": return sided(5, around);
            case "hexagon": return sided(6, around);
            default: return 1f;
        }
    }

    
    private static float sided(int sides, double around) {
        double slice = 2 * Math.PI / sides;
        double within = around % slice;
        if (within < 0) within += slice;
        double flat = Math.cos(Math.PI / sides) / Math.cos(within - slice / 2);
        
        
        double cap = 0.93, sharp = 36.0;
        double soft = -Math.log(Math.exp(-sharp * flat) + Math.exp(-sharp * cap)) / sharp;
        return (float) (soft / cap);
    }

    
    static Bitmap picture(String shape, int size, int colour) {
        int which = 0;
        for (int i = 0; i < ALL.length; i++) if (ALL[i].equals(shape)) which = i;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(colour);
        Path placed = new Path();
        Matrix place = new Matrix();
        place.setScale(size / 2f - 1, size / 2f - 1);
        place.postTranslate(size / 2f, size / 2f);
        placed.addPath(unit(which), place);
        canvas.drawPath(placed, paint);
        return bitmap;
    }

    static String nameOf(String shape) {
        switch (shape) {
            case "rounded": return Text.SHAPE_ROUNDED;
            case "square": return Text.SHAPE_SQUARE;
            case "cookie": return Text.SHAPE_COOKIE;
            case "clover": return Text.SHAPE_CLOVER;
            case "burst": return Text.SHAPE_BURST;
            case "sun": return Text.SHAPE_SUN;
            case "pentagon": return Text.SHAPE_PENTAGON;
            case "hexagon": return Text.SHAPE_HEXAGON;
            case "heart": return Text.SHAPE_HEART;
            default: return Text.SHAPE_CIRCLE;
        }
    }
}

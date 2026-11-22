package com.fasdeq.airtok;
import android.view.View;import android.view.ViewGroup;import android.view.ViewTreeObserver;import android.widget.TextView;import java.lang.ref.WeakReference;
final class DateOverlay {
    static final int[] NATIVE_DATES = {0x7f0a9649, 0x7f0a9f14, 0x7f0a92d9, 0x7f0a9e95, 0x7f0a93a5};
    static final int NATIVE_DATE = 0x7f0a9649;
    private static WeakReference<View> source=new WeakReference<>(null),watched=new WeakReference<>(null);
    private static WeakReference<TextView> label=new WeakReference<>(null);
    private static ViewTreeObserver.OnPreDrawListener observer;
    private static WeakReference<ViewTreeObserver> tree=new WeakReference<>(null);
    private static String date;private static CharSequence original;private static float alpha;private static int visibility;
    private static boolean applied,tapped,wasClickable;private static long searched,chosenAt;
    private static final java.util.LinkedHashMap<java.lang.ref.WeakReference<View>,String> candidates=new java.util.LinkedHashMap<>();
    private static WeakReference<ViewGroup> fallbackParent=new WeakReference<>(null);private static WeakReference<TextView> fallback=new WeakReference<>(null);private static int fallbackBottom;
    static void register(View view,String text){if(view==null||text==null)return;java.util.Iterator<java.lang.ref.WeakReference<View>> it=candidates.keySet().iterator();while(it.hasNext()){View old=it.next().get();if(old==null||old==view)it.remove();}candidates.put(new WeakReference<>(view),text);while(candidates.size()>32)candidates.remove(candidates.keySet().iterator().next());
        android.graphics.Rect rect=new android.graphics.Rect();if(view.isShown()&&view.getGlobalVisibleRect(rect)){bind(view,text);}else if(source.get()==null){source=new WeakReference<>(view);date=text;resume(view.getRootView());}
    }
    private static void chooseVisible(){long now=android.os.SystemClock.uptimeMillis();if(now-chosenAt<250)return;chosenAt=now;View root=watched.get();if(root==null)return;android.graphics.Rect rect=new android.graphics.Rect();View best=null;String text=null;int area=0;
        for(java.util.Map.Entry<WeakReference<View>,String> entry:candidates.entrySet()){View v=entry.getKey().get();if(v==null||v.getRootView()!=root||!v.isShown()||!v.isAttachedToWindow()||!v.getGlobalVisibleRect(rect))continue;int size=rect.width()*rect.height();if(size>area){area=size;best=v;text=entry.getValue();}}
        if(best!=null&&best!=source.get()){TextView old=label.get();if(old!=null)restore(old);removeFallback();label=new WeakReference<>(null);source=new WeakReference<>(best);date=text;searched=0;}
    }
    private static void removeFallback(){ViewGroup group=fallbackParent.get();if(group!=null&&fallback.get()!=null){group.removeView(fallback.get());group.setPadding(group.getPaddingLeft(),group.getPaddingTop(),group.getPaddingRight(),fallbackBottom);}fallback=new WeakReference<>(null);fallbackParent=new WeakReference<>(null);}
    private static TextView fallback(View cell){
        if(!(cell instanceof ViewGroup)) return null;
        if(fallback.get()!=null && fallbackParent.get()==cell) return fallback.get();
        removeFallback();
        ViewGroup group = (ViewGroup) cell;
        fallbackBottom = group.getPaddingBottom();
        int height = (int) Math.ceil(18 * group.getResources().getDisplayMetrics().density);
        group.setPadding(group.getPaddingLeft(), group.getPaddingTop(), group.getPaddingRight(), fallbackBottom + height);
        TextView view = new TextView(group.getContext());
        view.setTextSize(12);
        view.setTextColor(0xffffffff);
        view.setShadowLayer(2, 0, 1, 0x99000000);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        ViewGroup.LayoutParams lp;
        if (group instanceof android.widget.FrameLayout) {
            android.widget.FrameLayout.LayoutParams flp = new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height);
            flp.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.LEFT;
            flp.leftMargin = Math.max(group.getPaddingLeft(), (int) (12 * group.getResources().getDisplayMetrics().density));
            flp.bottomMargin = fallbackBottom;
            lp = flp;
        } else if (group instanceof android.widget.RelativeLayout) {
            android.widget.RelativeLayout.LayoutParams rlp = new android.widget.RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height);
            rlp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM);
            rlp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_LEFT);
            lp = rlp;
        } else {
            lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height);
        }
        group.addView(view, lp);
        fallback = new WeakReference<>(view);
        fallbackParent = new WeakReference<>(group);
        return view;
    }
    static void bind(View view,String text){bind(view,text,null);}
    static void bind(View view,String text,String author){
        detach();source=new WeakReference<>(view);date=text;searched=0;
        if(view==null || text==null)return;
        view.post(()->{if(source.get()==view)resume(view.getRootView());});
    }
    static void resume(View root){
        View cell=source.get();if(cell==null || date==null || cell.getRootView()!=root)return;
        ViewTreeObserver current=root.getViewTreeObserver();
        if(current!=tree.get() || observer==null){
            ViewTreeObserver old=tree.get();if(old!=null && old.isAlive() && observer!=null)old.removeOnPreDrawListener(observer);
            watched=new WeakReference<>(root);tree=new WeakReference<>(current);
            observer=()->{long probe=Probe.start();update();Probe.stop(Probe.DATE_FRAME,probe);return true;};current.addOnPreDrawListener(observer);
        }
        TextView found=find(cell), row=label.get();
        if(found!=row){if(row!=null)restore(row);label=new WeakReference<>(null);searched=0;}
        update();
    }

    private static TextView find(View view){
        for(int depth=0;view!=null && depth<20;depth++){
            for(int id : NATIVE_DATES){
                View found=view.findViewById(id);if(found instanceof TextView)return (TextView)found;
            }
            Object parent=view.getParent();if(!(parent instanceof View))break;
            String type=parent.getClass().getName();if(type.contains("RecyclerView")||type.contains("ViewPager"))break;
            view=(View)parent;
        }return null;
    }
    private static long panelAsked;private static boolean panelWas;private static WeakReference<View> panelRoot=new WeakReference<>(null);
    private static boolean panelOpen(View root){
        long now=android.os.SystemClock.uptimeMillis();
        if(panelRoot.get()==root&&now-panelAsked<250L)return panelWas;
        panelAsked=now;panelRoot=new WeakReference<>(root);panelWas=Dim.panelOpen(root);return panelWas;
    }
    static void update(){
        if(!Dates.isEnabled()){TextView old=label.get();if(old!=null)restore(old);if(fallback.get()!=null){removeFallback();label=new WeakReference<>(null);}return;}
        chooseVisible();View cell=source.get();if(cell==null || date==null)return;
        TextView row=label.get();if(row!=null && (!row.isAttachedToWindow() || row.getRootView()!=cell.getRootView())){restore(row);label=new WeakReference<>(null);row=null;searched=0;}if(row==null){long now=android.os.SystemClock.uptimeMillis();if(now-searched<250)return;searched=now;row=find(cell);if(row==null)row=fallback(cell);if(row==null)return;
            label=new WeakReference<>(row);original=row.getText();alpha=row.getAlpha();visibility=row.getVisibility();wasClickable=row.isClickable();Fonts.apply(row);}
        Object parent=row.getParent();View anchor=parent instanceof View?(View)parent:cell;
        boolean enabled=Dates.isEnabled();boolean show=enabled && anchor.isShown() && anchor.isAttachedToWindow() && !panelOpen(cell.getRootView());
        if(!enabled){restore(row);return;}
        CharSequence current=row.getText();if(applied && current!=null && !date.contentEquals(current))original=current;
        if(row.getText()==null||!date.contentEquals(row.getText()))row.setText(date);
        if(Math.abs(row.getAlpha()-.6f)>.001f)row.setAlpha(.6f);
        int next=show?View.VISIBLE:View.GONE;if(row.getVisibility()!=next)row.setVisibility(next);applied=true;
        if(!tapped){row.setOnClickListener(tap->DateInfo.open(tap,source.get()));tapped=true;}
    }
    private static void restore(TextView row){if(tapped){row.setOnClickListener(null);row.setClickable(wasClickable);tapped=false;}if(!applied)return;if(date!=null && row.getText()!=null && date.contentEquals(row.getText()))row.setText(original);row.setAlpha(alpha);row.setVisibility(visibility);applied=false;}
    private static void detach(){removeFallback();TextView row=label.get();if(row!=null)restore(row);ViewTreeObserver root=tree.get();if(root!=null && observer!=null && root.isAlive())root.removeOnPreDrawListener(observer);tree=new WeakReference<>(null);observer=null;label=new WeakReference<>(null);watched=new WeakReference<>(null);}
}

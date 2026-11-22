package cat.narezany.margyt;
import android.view.View;

final class RailPresence {
    private RailPresence(){}

    static boolean onScreen(View root){
        if(root==null)return false;
        int width=root.getWidth(),height=root.getHeight();if(width<=0||height<=0)return false;
        java.util.List<View> views=FeedRail.views();
        android.graphics.Rect seen=new android.graphics.Rect();
        for(View v:views){
            if(v==null||!v.isAttachedToWindow()||v.getRootView()!=root)continue;
            View probe=v;
            if(v.getVisibility()!=View.VISIBLE&&v.getParent() instanceof View)probe=(View)v.getParent();
            if(!probe.isShown()||probe.getAlpha()<=0.05f)continue;
            if(!probe.getGlobalVisibleRect(seen)||seen.width()<=0||seen.height()<=0)continue;

            if(seen.centerX()<width*.5f||seen.right>width+2||seen.left<0||seen.bottom>height+2||seen.top<0)continue;
            return true;
        }
        return false;
    }
}

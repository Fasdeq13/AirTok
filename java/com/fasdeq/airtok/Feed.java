package com.fasdeq.airtok;

import android.content.SharedPreferences;

import com.ss.android.ugc.aweme.feed.model.Aweme;
import com.ss.android.ugc.aweme.feed.model.FeedItemList;
import com.ss.android.ugc.aweme.follow.presenter.FollowFeedList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

















public final class Feed {

    private Feed() {}

    static void reloadSettings(){cached=null;}
    public static final String KEY = "hide_ads";
    public static final String KEY_LIVE = "hide_live";
    public static final String KEY_PHOTOS = "hide_photos";
    public static final String KEY_STORIES="hide_stories",KEY_SUGGESTIONS="hide_suggestions";

    private static volatile Boolean cached;

    public static boolean isEnabled() {
        Boolean known = cached;
        if (known != null) return known.booleanValue();
        SharedPreferences prefs = prefs();
        if (prefs == null) return true;
        boolean on = prefs.getBoolean(KEY, true);
        cached = Boolean.valueOf(on);
        return on;
    }

    public static void setEnabled(boolean enabled) {
        cached = Boolean.valueOf(enabled);
        SharedPreferences prefs = prefs();
        if (prefs != null) prefs.edit().putBoolean(KEY, enabled).apply();
        Flags.forget();
    }

    

    public static boolean hides(String key) {
        
        if (KEY_SUGGESTIONS.equals(key)) return false;
        SharedPreferences prefs = prefs();
        return prefs != null && prefs.getBoolean(key, false);
    }

    public static void setHides(String key, boolean on) {
        SharedPreferences prefs = prefs();
        if (prefs != null) prefs.edit().putBoolean(key, on).apply();
    }

    
    private static final int LIVE = 101;

    private static boolean isLive(Aweme post) {
        if (post == null) return false;
        try {
            int type = post.getAwemeType();
            if (type == LIVE || type == 61 || type == 66 || type == 1001) return true;
            if (post.getLiveId() != 0) return true;
            if (post.getRoomFeedCellStruct() != null) return true;
            if (NativeRead.get(post, "getRoom") != null) return true;
            if (NativeRead.get(post, "getStreamUrlModel") != null) return true;
            if (NativeRead.get(post, "getLiveType") != null) return true;
            if (Boolean.TRUE.equals(NativeRead.get(post, "isLive"))) return true;
            Object author = NativeRead.get(post, "getAuthor");
            if (author != null && (NativeRead.number(NativeRead.get(author, "roomId")) > 0 || Boolean.TRUE.equals(NativeRead.get(author, "isLive")))) return true;
            String className = post.getClass().getName().toLowerCase(java.util.Locale.ROOT);
            if (className.contains("live") || className.contains("room")) return true;
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isLiveItem(Object item) {
        if (item == null) return false;
        try {
            String className = item.getClass().getName().toLowerCase(java.util.Locale.ROOT);
            if (className.contains("live") || className.contains("room")) return true;
            if (NativeRead.get(item, "getRoomFeedCellStruct") != null) return true;
            if (NativeRead.get(item, "getRoom") != null) return true;
            if (NativeRead.get(item, "getLiveInfo") != null) return true;
            if (NativeRead.get(item, "getStreamUrlModel") != null) return true;
            if (Boolean.TRUE.equals(NativeRead.get(item, "isLive"))) return true;
            Object type = NativeRead.get(item, "getLiveType");
            if (type != null) return true;
            Object cellType = NativeRead.get(item, "getCellType");
            if (cellType instanceof Number && ((Number) cellType).intValue() == LIVE) return true;
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isPhotos(Aweme post) {
        try {
            return post.getPhotoModeImageInfo() != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    






    private static boolean mine(Aweme post) {
        try {
            String me = Account.id();
            if (me == null || me.length() == 0) return false;
            return me.equals(post.getAuthorUid());
        } catch (Throwable ignored) {
            return false;
        }
    }

    
    private static boolean outOfDates(Aweme post) {
        try {
            if (mine(post) || isLive(post) || advert(post)) return false;
            return FeedDates.leavesOut(NativeRead.number(NativeRead.get(post, "getCreateTime")));
        } catch (Throwable ignored) {
            return false;
        }
    }

    
    private static boolean unwanted(Object item, boolean ads, boolean live, boolean photos) {
        if (item == null) return false;
        if (!(item instanceof Aweme)) {
            if (ads && (NativeRead.get(item, "getPreciseAd") != null || NativeRead.get(item, "getAiAdCard") != null)) return true;
            if (live && isLiveItem(item)) return true;
            Object inner = NativeRead.get(item, "getAweme");
            if (inner instanceof Aweme) return unwanted(inner, ads, live, photos);
            return false;
        }
        Aweme post = (Aweme) item;
        if (mine(post)) return false;
        try {
            if (ads && advert(post)) return true;
            if (live && isLive(post)) return true;
            if (isLive(post)) return false;
            if (photos && isPhotos(post)) return true;
            if (Tags.blocks(post)) return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    


    static boolean advert(Object post) {
        if (!(post instanceof Aweme)) return false;
        Aweme aweme = (Aweme) post;
        if (aweme.isAd()) return true;
        if (Boolean.TRUE.equals(NativeRead.field(post, "_isAd"))) return true;
        if (Boolean.TRUE.equals(NativeRead.field(post, "_isSoftAd"))) return true;
        if (Boolean.TRUE.equals(NativeRead.get(post, "isSoftAd"))) return true;
        if (NativeRead.get(post, "getAwemeRawAd") != null) return true;
        if (NativeRead.field(post, "awemeRawAd") != null) return true;
        if (NativeRead.number(NativeRead.get(post, "getAdAwemeSource")) > 0) return true;
        if (NativeRead.number(NativeRead.get(post, "getAdLinkType")) > 0) return true;
        if (NativeRead.get(post, "getLinkAdData") != null) return true;
        if (NativeRead.get(post, "getPromoteModel") != null) return true;
        return false;
    }

    public static List withoutAds(List items) {
        return without(items);
    }

    public static List noPreloadAds(List items) {
        return isEnabled() ? Collections.emptyList() : items;
    }

    public static List noPreloadAdsField(FeedItemList page) {
        return page == null ? null : noPreloadAds(page.preloadAds);
    }

    public static boolean noHasAd(boolean has) {
        return !isEnabled() && has;
    }

    public static List followItems(FollowFeedList page) {
        return page == null ? null : without(page.items);
    }
    public static List searchItems(com.ss.android.ugc.aweme.search.pages.result.topsearch.core.model.SearchMixFeedList page){
        Ghost.recordActivity();
        if(page==null)return null;
        List given=page.mItems,kept=withoutAds(given);
        
        
        int had=given==null?-1:given.size(),left=kept==null?-1:kept.size();
        if(had<=0||left!=had)Diary.note("search: a page of "+(had<0?"nothing at all":had+" from TikTok")+(left!=had?", "+left+" after the mod":""));
        return kept;
    }
    public static List rawItems(FeedItemList page){return page==null?null:withoutAds(page.items);}

    private static SharedPreferences prefs() {
        try {
            android.content.Context context = AirTok.context();
            if (context == null) return null;
            return context.getSharedPreferences(AirTok.PREFS, android.content.Context.MODE_PRIVATE);
        } catch (Throwable ignored) {
            return null;
        }
    }

    
    static boolean socialRepost(Object post){return NativeRead.get(post,"getUpvoteReason")!=null || NativeRead.get(post,"getUpvoteInfo")!=null || NativeRead.get(post,"getUpvotePreload")!=null;}
    static boolean suggestion(Object post){
        if(NativeRead.number(NativeRead.get(post,"getRecommendCardType"))<=0)return false;
        if(socialRepost(post))return false;
        return true;
    }

    
    private static int dropped;
    private static long reportedAt;

    

    







    public static List getItems(FeedItemList page) {
        if (page == null) return null;
        long probe = Probe.start();
        List out = without(page.getItems());
        Probe.stop(Probe.FEED, probe);
        return out;
    }

    
    private static List without(List items) {
        if (items == null) return items;
        boolean ads=isEnabled();SharedPreferences options=prefs();
        boolean live=options!=null && options.getBoolean(KEY_LIVE,false);
        boolean photos=options!=null && options.getBoolean(KEY_PHOTOS,false);
        boolean stories=options!=null && options.getBoolean(KEY_STORIES,false);
        boolean suggestions=options!=null && options.getBoolean(KEY_SUGGESTIONS,false);
        boolean authors=!FeedBlacklist.items().isEmpty() || !FeedBlacklist.cards().isEmpty();
        
        boolean others=!Tags.all().isEmpty();
        boolean dated=FeedDates.active();
        if (!ads && !live && !photos && !stories && !suggestions && !authors && !others && !dated) {
            return items;
        }
        try {
            int out=0,index=0;
            List kept=null;
            for(Object item:items) {
                boolean extra=item instanceof Aweme && ((stories && !socialRepost(item) && Boolean.TRUE.equals(NativeRead.get(item,"getIsTikTokStory")))
                        || (suggestions && suggestion(item))
                        || (authors && FeedBlacklist.hides(item))
                        || (dated && outOfDates((Aweme)item)));
                if(extra || unwanted(item,ads,live,photos)) {
                    if(kept==null){kept=new ArrayList(items.size());kept.addAll(items.subList(0,index));}
                    out++;
                } else if(kept!=null)kept.add(item);
                index++;
            }
            if(kept==null)return items;
            synchronized(Feed.class) {
                dropped+=out;long now=System.currentTimeMillis();
                if(now-reportedAt>=1000L){reportedAt=now;Diary.note("feed: " + out + " left out, " + dropped + " so far");}
            }
            return kept;
        } catch (Throwable error) {
            Diary.note("feed: leaving the page alone, " + error);
            return items;
        }
    }
}

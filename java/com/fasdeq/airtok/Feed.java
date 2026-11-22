package com.fasdeq.airtok;

import android.content.SharedPreferences;

import com.ss.android.ugc.aweme.feed.model.Aweme;
import com.ss.android.ugc.aweme.feed.model.FeedItemList;

import java.util.ArrayList;
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

        return true;
    }

    public static void setEnabled(boolean enabled) {
        cached = enabled;
        SharedPreferences prefs = prefs();
        if (prefs != null) prefs.edit().putBoolean(KEY, enabled).apply();
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
        try {
            if (post.getAwemeType() == LIVE) return true;
            if (post.getLiveId() != 0) return true;
            return post.getRoomFeedCellStruct() != null;
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
        if (!(item instanceof Aweme)) return false;
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

    private static final String[] PROMO_KEYWORDS = {
        "мусордроп", "musordrop", "топскин", "topskin", "гейзервпн", "geyservpn", "gayservpn",
        "кабура", "cabura", "нвути", "nvuti", "драгонмани", "dragonmoney",
        "1win", "1xwin", "1xbet", "1хбет", "melbet", "мелбет", "mostbet", "мостбет", "leonbets",
        "standoffdrop", "стандоффдроп", "ggstandoff", "ггстандофф", "bullsdrop", "булсдроп",
        "forcedrop", "форсдроп", "csfail", "ксфейл", "csgorun", "ксгоран", "csgo-run",
        "csdrop", "easydrop", "изидроп", "case-battle", "кейсбатл",
        "промокод на скины", "промокод на депозит", "халявные скины",
        "халява стендофф", "вайпервпн", "vipervpn", "hitvpn", "хитвпн",
        "вавада", "vavada", "up-x", "апх", "джойказино", "joycasino", "покердом", "pokerdom",
        "pin-up", "пинап", "бетсити", "фонбет", "лигаставок", "винлайн", "winline", "олимпбет", "olimpbet",
        "parimatch", "париматч", "фриспины", "бонус за регистрацию", "промокод в шапке",
        "скинбокс", "skinbox", "скинстейбл", "skinstable", "кейсы стендофф", "рулетка кс"
    };

    static boolean isPromoSpam(Aweme post) {
        try {
            String desc = NativeRead.string(NativeRead.get(post, "getDesc"));
            if (desc.length() > 0) {
                String lower = desc.toLowerCase(java.util.Locale.ROOT).replace(" ", "").replace("-", "").replace("_", "");
                for (String kw : PROMO_KEYWORDS) {
                    String cleanKw = kw.replace(" ", "").replace("-", "").replace("_", "");
                    if (lower.contains(cleanKw)) return true;
                }
            }
            Object author = NativeRead.get(post, "getAuthor");
            if (author != null) {
                String nick = NativeRead.string(NativeRead.get(author, "getNickname"));
                String uid = NativeRead.string(NativeRead.get(author, "getUniqueId"));
                String text = (nick + " " + uid).toLowerCase(java.util.Locale.ROOT);
                for (String kw : PROMO_KEYWORDS) {
                    if (text.contains(kw)) return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }


    static boolean advert(Object post){
        if(!(post instanceof Aweme))return false;
        Aweme aweme = (Aweme) post;
        try {
            if (aweme.isAd()) return true;
            if (Boolean.TRUE.equals(NativeRead.field(post, "_isAd"))) return true;
            if (Boolean.TRUE.equals(NativeRead.get(post, "isSoftAd"))) return true;
            Object rawAd = NativeRead.get(post, "getAwemeRawAd");
            if (rawAd != null && Boolean.TRUE.equals(NativeRead.get(rawAd, "isAd"))) return true;
            if (isPromoSpam(aweme)) return true;
        } catch (Throwable ignored) {
            return aweme.isAd();
        }
        return false;
    }

    public static List withoutAds(List items){
        if(items==null || !isEnabled())return items;
        ArrayList kept=null;
        for(int i=0;i<items.size();i++){
            Object item=items.get(i);
            Object post=item instanceof Aweme?item:NativeRead.get(item,"getAweme");
            boolean card=!(item instanceof Aweme) && (NativeRead.get(item,"getPreciseAd")!=null || NativeRead.get(item,"getAiAdCard")!=null);
            if(advert(post) || card){if(kept==null){kept=new ArrayList(items.size());kept.addAll(items.subList(0,i));}}
            else if(kept!=null)kept.add(item);
        }
        if(kept!=null && kept.isEmpty() && !items.isEmpty()) return items;
        return kept==null?items:kept;
    }
    public static List searchItems(com.ss.android.ugc.aweme.search.pages.result.topsearch.core.model.SearchMixFeedList page){
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
            return Plugins.feed(items);
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
            if(kept==null)return Plugins.feed(items);
            if(kept.isEmpty() && !items.isEmpty()){
                Diary.note("feed: safety fallback, kept was empty, returning original items");
                return Plugins.feed(items);
            }
            synchronized(Feed.class) {
                dropped+=out;long now=System.currentTimeMillis();
                if(now-reportedAt>=1000L){reportedAt=now;Diary.note("feed: " + out + " left out, " + dropped + " so far");}
            }
            return Plugins.feed(kept);
        } catch (Throwable error) {
            Diary.note("feed: leaving the page alone, " + error);
            return items;
        }
    }
}

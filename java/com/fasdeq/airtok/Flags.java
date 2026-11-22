package com.fasdeq.airtok;

import android.content.SharedPreferences;

import com.bytedance.ies.abmock.SettingsManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class Flags {

    private Flags() {}


    public static final int ADS = 0;
    public static final int VOICE = 1;
    public static final int BANNER = 2;

    public static final int ALWAYS = 3;

    public static final String KEY_VOICE = "flag_voice";
    public static final String KEY_BANNER = "flag_profile_banner";
    public static final String KEY_SPEED_LOCK = "flag_speed_lock";


    public static final String KEY_COMMENT_REPOST = "flag_comment_repost";
    public static final String KEY_SELF_REPOST = "flag_self_repost";
    public static final String KEY_COMMENT_PHOTOS = "flag_comment_photos";
    public static final String KEY_HASHTAGS = "flag_hashtags_unlimited";
    public static final String KEY_TRANSLATE_COMMENTS = "flag_translate_comments";



    public static final String KEY_NEW_PROFILE = "flag_new_profile";

    private static final Set<String> ON_BY_DEFAULT = new HashSet<String>(Arrays.asList(
            KEY_VOICE, KEY_BANNER, KEY_NEW_PROFILE, KEY_TRANSLATE_COMMENTS, KEY_COMMENT_REPOST,
            KEY_COMMENT_PHOTOS, KEY_HASHTAGS));


    static final String OVERRIDES = AirTok.PREFS + "_flags";


    private static final String SPEED_LOCK = "long_press_speed_up_lock";

    private static final class Override {
        final int group;
        final Object value;

        Override(int group, Object value) {
            this.group = group;
            this.value = value;
        }
    }

    private static final Map<String, Override> GROUPED = new HashMap<String, Override>();

    private static void put(int group, Object value, String... keys) {
        for (String key : keys) GROUPED.put(key, new Override(group, value));
    }

    static {


        put(ADS, Boolean.FALSE,
                "enable_normal_splash_ad", "enable_normal_splash_ad_ab", "splash_ad_enable",
                "enable_live_splash", "search_ad_refactor_enable", "search_enable_mix_adgap",
                "commerce_enable");

        put(VOICE, Integer.valueOf(1), "audio_comment_publish");
        put(BANNER,Integer.valueOf(1),"profile_bg_in_allow_list","profile_bg_enable_consumption_group");
    }



    public static boolean isOn(int group) {
        if (group == ADS) return Feed.isEnabled();
        if (group == ALWAYS) return true;
        return bool(keyOf(group));
    }

    private static String keyOf(int group) {
        switch (group) {
            case BANNER: return KEY_BANNER;
            default: return KEY_VOICE;
        }
    }

    public static boolean isOn(String key) {
        return bool(key);
    }

    public static void set(String key, boolean on) {
        SharedPreferences prefs = prefs();
        if (prefs != null) prefs.edit().putBoolean(key, on).apply();
        forget();
    }


    private static boolean chosen(String key) {


        return KEY_NEW_PROFILE.equals(key) || KEY_SPEED_LOCK.equals(key);
    }

    private static boolean bool(String key) {
        if (!chosen(key)) return true;
        try {
            SharedPreferences prefs = prefs();
            return prefs != null && prefs.getBoolean(key, ON_BY_DEFAULT.contains(key));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static SharedPreferences prefs() {
        android.content.Context context = AirTok.context();
        if (context == null) return null;
        return context.getSharedPreferences(AirTok.PREFS, android.content.Context.MODE_PRIVATE);
    }


    private static Object of(String key) {
        Object forced = forced(key);
        if (forced != null) return forced;
        Override override = GROUPED.get(key);
        if (override == null) return null;
        return isOn(override.group) ? override.value : null;
    }




    private static volatile Map<String, Object> table;

    private static volatile Thread building;

    private static volatile Thread asking;

    private static final Set<String> ANSWERED =
            Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String, Boolean>());

    static void forget() {
        table = null;
        ANSWERED.clear();
    }

    private static Map<String, Object> table() {
        Thread me = Thread.currentThread();
        if (building == me) return null;
        building = me;
        try {
            SharedPreferences prefs = prefs();
            if (prefs == null) return null;
            Map<String, Object> made = new HashMap<String, Object>();
            Integer one = Integer.valueOf(1), none = Integer.valueOf(0);






            made.put(SPEED_LOCK, on(prefs, KEY_SPEED_LOCK) ? one : none);
            if (on(prefs, KEY_VOICE)) made.put("audio_comment_publish", one);
            if (on(prefs, KEY_BANNER)) {
                made.put("profile_bg_in_allow_list", one);
                made.put("profile_bg_enable_consumption_group", one);
            }




            if (on(prefs, KEY_COMMENT_REPOST)) {
                made.put("tt_comment_repost", one);
                made.put("tt_comment_repost_interaction", one);
                made.put("tt_audio_comment_repost", one);
            }
            if (on(prefs, KEY_SELF_REPOST)) made.put("enable_self_repost", one);


            if (on(prefs, KEY_COMMENT_PHOTOS)) made.put("comment_multi_photo_group", Integer.valueOf(2));


            if (on(prefs, KEY_HASHTAGS)) made.put("studio_publish_limit_hashtag_switch", none);

            made.put("camera_teleprompter_mode", one);


            made.put("studio_selfie_fill_light_config", "{\"support_selfie_light_adjust\":true,\"selfie_fill_light_persist_memory\":true,"
                    + "\"selfie_fill_light_tab_memory\":true,\"selfie_fill_light_shutter_ring_shadow_order_fix\":true,\"lazy_load_flash_front_scene\":true,"
                    + "\"auto_adjust_full_brightness\":true}");










            if (on(prefs, KEY_TRANSLATE_COMMENTS)) {
                made.put("comment_auto_translation_enabled", one);
                made.put("comment_auto_translation_consumer_style", Integer.valueOf(2));
                made.put("comment_auto_translation_creator_style", Integer.valueOf(2));
                made.put("comment_auto_translation_display_button_under_dnt", one);
                made.put("tt_comment_auto_translation_v2", none);
            }






            made.put("circle_search_block", none);
            if (Comments.isDoubleTapLikeEnabled()) {
                made.put("tt_comment_double_tap_to_like", "{\"enable\":1,\"interval_time\":300,\"expired_version\":-1}");
            } else {
                made.put("tt_comment_double_tap_to_like", "{\"enable\":0,\"interval_time\":300,\"expired_version\":-1}");
            }
            made.put("long_press_speed_up_enable", none);
            made.put("tablet_override_long_press_speed_up_enable", none);
            made.put("long_press_redesign", none);
            made.put("short_drama_default_long_press_panel", none);






            made.put("profile_left_align", on(prefs, KEY_NEW_PROFILE) ? one : none);


            if (DevMode.isOn()) {
                Map<String, Object> mine = overrides();
                if (!mine.isEmpty() && FlagGuard.allows()) made.putAll(mine);
            }
            table = made;
            return made;
        } catch (Throwable error) {
            return null;
        } finally {
            building = null;
        }
    }





    public static boolean newProfile() {
        try {
            SharedPreferences prefs = prefs();
            if (prefs != null) return prefs.getBoolean(KEY_NEW_PROFILE, ON_BY_DEFAULT.contains(KEY_NEW_PROFILE));
        } catch (Throwable ignored) {
        }
        return ON_BY_DEFAULT.contains(KEY_NEW_PROFILE);
    }

    private static boolean on(SharedPreferences prefs, String key) {
        if (!chosen(key)) return true;
        return prefs.getBoolean(key, ON_BY_DEFAULT.contains(key));
    }

    private static Object forced(String key) {
        try {
            Map<String, Object> known = table;
            if (known == null) {
                known = table();
                if (known == null) return null;
            }
            Object value = known.get(key);
            if (value == null || asking == Thread.currentThread()) return null;


            if (ANSWERED.add(key)) Diary.detail("flag " + key + " = " + value);
            return value;
        } catch (Throwable error) {
            return null;
        }
    }








    public static Object playerFlag(String key, java.lang.reflect.Type type) {
        try {
            if ("surfaceview_use_surface_control".equals(key) && Design.glass()) return Boolean.FALSE;
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static Boolean forcedBoolean(String key) {
        Object value = forced(key);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return Boolean.valueOf(((Number) value).doubleValue() != 0);
        return null;
    }

    public static Integer forcedInt(String key) {
        Object value = forced(key);
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return Integer.valueOf(((Number) value).intValue());
        if (value instanceof Boolean) return Integer.valueOf(((Boolean) value).booleanValue() ? 1 : 0);
        return null;
    }

    public static Long forcedLong(String key) {
        Object value = forced(key);
        if (value instanceof Number) return Long.valueOf(((Number) value).longValue());
        if (value instanceof Boolean) return Long.valueOf(((Boolean) value).booleanValue() ? 1L : 0L);
        return null;
    }

    public static Float forcedFloat(String key) {
        Object value = forced(key);
        if (value instanceof Number) return Float.valueOf(((Number) value).floatValue());
        return null;
    }

    public static Double forcedDouble(String key) {
        Object value = forced(key);
        if (value instanceof Number) return Double.valueOf(((Number) value).doubleValue());
        return null;
    }

    public static String forcedString(String key) {
        Object value = forced(key);
        return value instanceof String ? (String) value : null;
    }

    private static final Map<String, Object> BUILT = new java.util.concurrent.ConcurrentHashMap<String, Object>();


    public static Object forcedObject(Class<?> type, String key) {
        if (type == null || key == null) return null;
        Object value = forced(key);
        if (!(value instanceof String) || !((String) value).startsWith("{")) return null;
        String cacheKey = type.getName() + "\n" + key;
        Object made = BUILT.get(cacheKey);
        if (made != null) return made;
        try {
            made = FlagObjects.build((String) value, type);
            if (made != null && type.isInstance(made)) { BUILT.put(cacheKey, made); return made; }
        } catch (Throwable error) {
            Diary.note("flag object " + key + ": " + error);
        }
        return null;
    }




    static final class Known {
        final String key;

        final String type;
        final Object fallback;

        Known(String key, String type, Object fallback) {
            this.key = key;
            this.type = type;
            this.fallback = fallback;
        }

        boolean settable() {
            return type.equals("BOOLEAN") || type.equals("INT") || type.equals("LONG")
                    || type.equals("FLOAT") || type.equals("DOUBLE") || type.equals("STRING");
        }
    }




    private static final String REGISTRY = "X.06y1";
    private static final String READER = "X.09tK";
    private static final int EVERYWHERE = 0x7c00;

    private static List<Known> catalogue;


    static synchronized List<Known> catalogue() {
        if (catalogue != null) return catalogue;
        List<Known> out = new ArrayList<Known>();
        try {
            Class<?> registry = Class.forName(REGISTRY, true, Flags.class.getClassLoader());
            for (java.lang.reflect.Field field : registry.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        || !Map.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                Object held = field.get(null);
                if (!(held instanceof Map)) continue;
                for (Object each : new ArrayList<Object>(((Map<?, ?>) held).entrySet())) {
                    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) each;
                    if (!(entry.getKey() instanceof String) || entry.getValue() == null) continue;
                    Object item = entry.getValue();
                    Object type = item.getClass().getField("type").get(item);
                    Object fallback = item.getClass().getField("defaultValue").get(item);
                    out.add(new Known((String) entry.getKey(), String.valueOf(type), fallback));
                }
            }
        } catch (Throwable error) {
            Diary.note("flags: no list, " + error);
        }
        Collections.sort(out, (a, b) -> a.key.compareTo(b.key));
        catalogue = out;
        return out;
    }


    static Object own(Known flag) {
        Thread before = asking;
        asking = Thread.currentThread();
        try {
            Class<?> type = Class.forName(READER, true, Flags.class.getClassLoader());
            Object reader = null;
            for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isStatic(method.getModifiers())
                        && method.getParameterTypes().length == 0 && method.getReturnType() == type) {
                    reader = method.invoke(null);
                    break;
                }
            }
            if (reader == null) return null;
            Object fallback = flag.fallback;
            switch (flag.type) {
                case "BOOLEAN":
                    return type.getMethod("LIZJ", int.class, String.class, boolean.class, boolean.class)
                            .invoke(reader, EVERYWHERE, flag.key, true,
                                    fallback instanceof Boolean && ((Boolean) fallback).booleanValue());
                case "INT":
                    return type.getMethod("LJIIJJI", int.class, int.class, String.class, boolean.class)
                            .invoke(reader, EVERYWHERE,
                                    fallback instanceof Number ? ((Number) fallback).intValue() : 0,
                                    flag.key, true);
                case "LONG":
                    return type.getMethod("LJIILIIL", int.class, long.class, String.class, boolean.class)
                            .invoke(reader, EVERYWHERE,
                                    fallback instanceof Number ? ((Number) fallback).longValue() : 0L,
                                    flag.key, true);
                case "FLOAT":
                    return type.getMethod("LJIIIIZZ", int.class, String.class, boolean.class, float.class)
                            .invoke(reader, EVERYWHERE, flag.key, true,
                                    fallback instanceof Number ? ((Number) fallback).floatValue() : 0f);
                case "DOUBLE":
                    return type.getMethod("LJFF", double.class, int.class, String.class, boolean.class)
                            .invoke(reader, fallback instanceof Number ? ((Number) fallback).doubleValue() : 0d,
                                    EVERYWHERE, flag.key, true);
                case "STRING":
                    return type.getMethod("LJIILLIIL", int.class, String.class, String.class, boolean.class)
                            .invoke(reader, EVERYWHERE, flag.key,
                                    fallback instanceof String ? (String) fallback : "", true);
                default:
                    return null;
            }
        } catch (Throwable error) {
            return null;
        } finally {
            asking = before;
        }
    }

    private static SharedPreferences overridePrefs() {
        android.content.Context context = AirTok.context();
        if (context == null) return null;
        return context.getSharedPreferences(OVERRIDES, android.content.Context.MODE_PRIVATE);
    }


    static Map<String, Object> overrides() {
        Map<String, Object> out = new HashMap<String, Object>();
        try {
            SharedPreferences prefs = overridePrefs();
            if (prefs == null) return out;
            for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                Object value = entry.getValue();
                String key = entry.getKey();
                if (key.startsWith(DOUBLE)) {
                    out.put(key.substring(DOUBLE.length()), Double.valueOf(String.valueOf(value)));
                } else if (value != null) {
                    out.put(key, value);
                }
            }
        } catch (Throwable ignored) { }
        return out;
    }

    private static final String DOUBLE = "double:";


    static void override(Known flag, Object value) {
        SharedPreferences prefs = overridePrefs();
        if (prefs == null) return;
        SharedPreferences.Editor edit = prefs.edit().remove(flag.key).remove(DOUBLE + flag.key);
        if (value instanceof Boolean) edit.putBoolean(flag.key, ((Boolean) value).booleanValue());
        else if (value instanceof Integer) edit.putInt(flag.key, ((Integer) value).intValue());
        else if (value instanceof Long) edit.putLong(flag.key, ((Long) value).longValue());
        else if (value instanceof Float) edit.putFloat(flag.key, ((Float) value).floatValue());
        else if (value instanceof Double) edit.putString(DOUBLE + flag.key, value.toString());
        else if (value instanceof String) edit.putString(flag.key, (String) value);
        edit.apply();
        FlagGuard.again();
        forget();
    }

    static void clearOverrides() {
        SharedPreferences prefs = overridePrefs();
        if (prefs != null) prefs.edit().clear().apply();
        forget();
    }


    static Object parse(Known flag, String typed) {
        String text = typed == null ? "" : typed.trim();
        try {
            switch (flag.type) {
                case "BOOLEAN":
                    if (text.equalsIgnoreCase("true") || text.equals("1")) return Boolean.TRUE;
                    if (text.equalsIgnoreCase("false") || text.equals("0")) return Boolean.FALSE;
                    return null;
                case "INT": return Integer.valueOf(text);
                case "LONG": return Long.valueOf(text);
                case "FLOAT": return Float.valueOf(text.replace(',', '.'));
                case "DOUBLE": return Double.valueOf(text.replace(',', '.'));
                case "STRING": return typed == null ? "" : typed;
                default: return null;
            }
        } catch (NumberFormatException wrong) {
            return null;
        }
    }


    public static boolean flag(String key, boolean fallback) {
        Object value = of(key);
        if (value instanceof Boolean) return ((Boolean) value).booleanValue();
        if (value instanceof Integer) return ((Integer) value).intValue() != 0;
        return SettingsManager.LIZ(key, fallback);
    }

    public static int flag(String key, int fallback) {
        Object value = of(key);
        if (value instanceof Integer) return ((Integer) value).intValue();
        if (value instanceof Boolean) return ((Boolean) value).booleanValue() ? 1 : 0;
        return SettingsManager.LJ(key, fallback);
    }

    public static long flag(String key, long fallback) {
        Object value = of(key);
        if (value instanceof Integer) return ((Integer) value).intValue();
        return SettingsManager.LJFF(key, fallback);
    }

    public static String flag(String key, String fallback) {
        Object value = of(key);
        if (value instanceof String) return (String) value;
        return SettingsManager.LJI(key, fallback);
    }

    public static float flag(String key, float fallback) {
        Object value = of(key);
        if (value instanceof Integer) return ((Integer) value).intValue();
        return SettingsManager.LIZJ(key, fallback);
    }

    public static double flag(String key, double fallback) {
        Object value = of(key);
        if (value instanceof Integer) return ((Integer) value).intValue();
        return SettingsManager.LIZIZ(key, fallback);
    }
}

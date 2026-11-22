package com.fasdeq.airtok;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;


public class SettingsActivity extends Activity {
    void accountAppearanceChanged(){skin=Skin.forSettings(this);rebuild();}

    private Skin skin;
    private FrameLayout root;
    private final java.util.Map<String,View> settingTargets=new java.util.HashMap<>();
    private LinearLayout column;
    private View restartBar;
    private RestartSymbol restartSymbol;
    private TextView restartWhy, restartAction;
    private GradientDrawable restartBackground;
    private boolean rebuildQueued;
    private boolean badgePlacementOpen,gradientFineOpen;
    private String disclosureKey,renderDisclosureKey;
    private boolean collapsing;
    private final java.util.Map<String,java.util.List<DisclosureBody>> disclosureBodies=new java.util.HashMap<>();
    private final java.util.Map<String,Chevron> disclosureArrows=new java.util.HashMap<>();
    private boolean revealAnimationRequested;
    private boolean revealAnimationUsed;
    private Runnable scrollRestore;
    private android.view.ViewTreeObserver.OnPreDrawListener scrollRestoreListener;
    private int requestedScrollY;
    private final Runnable delayedRebuild = new Runnable() {
        @Override
        public void run() {
            rebuildQueued = false;
            rebuild();
        }
    };


    private static boolean pending;


    private static final String CHANNEL = "https://t.me/airtokiktok";
    private static final String FORUM = "https://t.me/margeletforum";
    private static final String OWNER_TELEGRAM = "https://t.me/narezany";
    private static final String OWNER_TIKTOK =
            "https://tiktok.com/@narezany?_r=1&_t=ZT-99hPDJ26hji_";

    private static final String CHANNEL_WRITE = "https://t.me/airtokiktok?direct";
    private static final String TTCUZ = "https://t.me/ttcuzmod";
    private static final String GIT = "https://git.narez.xyz/narezany/MargyT";
    private static final String DOCS_PLUGINS =
            "https://git.narez.xyz/narezany/MargyT/src/branch/main/docs/plugins.md";
    private static final String DOCS_STYLES =
            "https://git.narez.xyz/narezany/MargyT/src/branch/main/docs/mtui.md";
    private static final String YOOMONEY = "https://yoomoney.ru/to/4100118196133693";
    private static final String BOOSTY = "https://boosty.to/ega_link";
    private static final String FUNPAY = "https://funpay.com/lots/offer?id=80127335";

    private static final String STARS = "https://t.me/narezany";

    private boolean countriesOpen;
    private boolean accentOpen;
    private Dot accentCustomSwatch;
    private boolean textOpen;
    private boolean backgroundOpen;
    private boolean themeColoursOpen;
    private boolean fontOpen;
    private ScrollView page;
    private int settingsGroup;

    private String settingsPage;


    private static final class Shelf {
        final int group; final String id, icon, title, note; final String[] sections;
        Shelf(int group, String id, String icon, String title, String note, String... sections) {
            this.group = group; this.id = id; this.icon = icon; this.title = title; this.note = note; this.sections = sections;
        }
    }

    private static final String DOWNLOADS = Local.of("Скачивание медиа");
    private static final String TOOLS = Local.of("Умный плеер и режим чистого экрана");

    private static Shelf[] shelves;

    private Shelf[] shelves() {
        if (shelves != null) return shelves;
        shelves = new Shelf[] {
            new Shelf(1, "style", "palette", Local.of("Оформление"), "", Text.DESIGN, Text.ACCENT, Text.FONT, MOTION_BLUR),
            new Shelf(3, "feed_video", "play_circle", Local.of("Лента и видео"), "", Text.REGION, Text.FEED, BURN, REPOSTS),
            new Shelf(4, "downloads", "download", Local.of("Скачивание медиа"), "", DOWNLOADS),
            new Shelf(6, "tools", "tune", Local.of("Умный плеер и режим чистого экрана"), "", TOOLS),
            new Shelf(7, "chats", "chat", Local.of("Чаты, серии, уведомления"), "", Text.STREAKS, Text.NOTIFICATIONS),
            new Shelf(5, "about", "info", Local.of("О проекте"), "", Local.of("О проекте"), Local.of("Благодарность")),
        };
        return shelves;
    }

    private java.util.List<Shelf> shelvesOf(int group) {
        java.util.List<Shelf> out = new java.util.ArrayList<>();
        for (Shelf one : shelves()) {
            if (one.group != group) continue;
            if (Version.LITE) { boolean any = false; for (String section : one.sections) any |= inLite(section); if (!any) continue; }
            out.add(one);
        }
        return out;
    }

    private Shelf shelf(String id) {
        if (id == null) return null;
        for (Shelf one : shelves()) if (one.id.equals(id)) return one;
        return null;
    }

    private Shelf shelfOfSection(String section) {
        for (Shelf one : shelves()) for (String name : one.sections) if (name.equals(section)) return one;
        return null;
    }
    private boolean mineOpen;
    private java.util.List<Mine.Held> ordering;
    private boolean streakOpen;
    private boolean diaryOpen;

    private boolean rearranged;

    private View donateAnchor;
    private final Runnable badgeUpdates = () -> {
        if (!isFinishing() && !isDestroyed()) rebuild();
    };
    private String searchQuery = "";
    private final java.util.Map<String, View> sectionTargets =
            new java.util.HashMap<String, View>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AirTok.attach(this);
        Accent.reload();


        skin = Skin.forSettings(this);
        Unsaved.look(this);
        dressTheWindow();


        Badges.tell(badgeUpdates);




        Mine.ask(() -> {
            if (isFinishing() || isDestroyed()) return;
            rebuild();
            if (Tester.on() && !Tester.allowed()) Tester.refuse(this);
        });
        if (Tester.on() && Tester.asked() && !Tester.allowed()) Tester.refuse(this);

        ScrollView scroll = page = new ScrollView(this);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        scroll.setFillViewport(true);

        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(0, statusBar(), 0, dp(32));
        scroll.addView(column, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root=new FrameLayout(this);
        root.setBackgroundColor(skin.page);
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
        Design.page(this, root, skin.page);
        rebuild();
        maybeAskForSupport();
        if(getIntent().getBooleanExtra("airtok_tutorial",false))root.postDelayed(()->Tutorial.show(this),350L);
        openPluginFile(getIntent());
        openStyleFile(getIntent());
        openStats(getIntent());
    }

    private Bundle stats;


    private void openStats(Intent intent) {
        Bundle given = intent == null ? null : intent.getBundleExtra(VideoStats.EXTRA);
        if (given == null) return;
        stats = given;
        settingsGroup = STATS_PAGE;
        settingsPage = null;
        searchQuery = "";
        rebuild();
    }

    private static final int STATS_PAGE = 96;

    private static String whole(long n) {
        return String.format(Locale.ROOT, "%,d", n).replace(',', '\u00A0');
    }

    private static String share(long part, long of) {
        if (of <= 0) return "—";
        return String.format(Locale.ROOT, "%.2f%%", part * 100.0 / of).replace('.', ',');
    }

    private View statRow(String picture, String title, String value) {
        final String copied = value;
        View row = actionRow(picture, title, value, () -> {
            try {
                ((android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
                        .setPrimaryClip(android.content.ClipData.newPlainText(title, copied));
                Toast.makeText(this, Text.COPIED, Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {
            }
        });
        return row;
    }


    private View ratioRow(String picture, String title, long part, long of, long best) {
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.addView(statRow(picture, title, share(part, of)));
        final float fill = of <= 0 || best <= 0 ? 0f : Math.min(1f, (float) part / best);
        View bar = new View(this) {
            final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                float h = getHeight(), w = getWidth();
                paint.setColor(Expressive.mix(skin.card, skin.text, 0.12f));
                canvas.drawRoundRect(0, 0, w, h, h / 2f, h / 2f, paint);
                paint.setColor(Accent.colour());
                canvas.drawRoundRect(0, 0, Math.max(h, w * fill), h, h / 2f, h / 2f, paint);
            }
        };
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        size.leftMargin = dp(72); size.rightMargin = dp(20); size.bottomMargin = dp(12);
        rows.addView(bar, size);
        return rows;
    }

    private static String compact(long n) {
        if (n < 10_000) return whole(n);
        if (n < 1_000_000) return String.format(Locale.ROOT, "%.1f", n / 1000.0).replace('.', ',').replace(",0", "") + " " + Local.of("тыс.");
        if (n < 1_000_000_000) return String.format(Locale.ROOT, "%.1f", n / 1_000_000.0).replace('.', ',').replace(",0", "") + " " + Local.of("млн");
        return String.format(Locale.ROOT, "%.1f", n / 1_000_000_000.0).replace('.', ',').replace(",0", "") + " " + Local.of("млрд");
    }

    private String postLink(Bundle s) {
        String handle = s.getString("handle", "");
        return "https://www.tiktok.com/@" + (handle.length() > 0 ? handle : "_") + (s.getInt("images") > 0 ? "/photo/" : "/video/") + s.getString("aid", "");
    }


    private String statsText(Bundle s) {
        long views = s.getLong("play");
        StringBuilder out = new StringBuilder();
        out.append("📊 ").append(Local.of("Статистика видео")).append(" @").append(s.getString("handle", "")).append('\n');
        out.append("👁 ").append(Local.of("Просмотры")).append(": ").append(whole(views)).append('\n');
        out.append("❤️ ").append(Local.of("Лайки")).append(": ").append(whole(s.getLong("digg"))).append(" (").append(share(s.getLong("digg"), views)).append(")\n");
        out.append("💬 ").append(Local.of("Комментарии")).append(": ").append(whole(s.getLong("comment"))).append('\n');
        out.append("↗️ ").append(Local.of("Поделились")).append(": ").append(whole(s.getLong("share"))).append('\n');
        out.append("🔖 ").append(Local.of("Сохранили")).append(": ").append(whole(s.getLong("collect"))).append('\n');
        long engaged = s.getLong("digg") + s.getLong("comment") + s.getLong("share") + s.getLong("collect");
        out.append("⚡ ").append(Local.of("Вовлечённость")).append(": ").append(share(engaged, views)).append('\n');
        if (s.getLong("created") > 0) out.append("🕒 ").append(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.LONG,
                java.text.DateFormat.SHORT).format(new java.util.Date(s.getLong("created") * 1000L))).append('\n');
        out.append("🔗 ").append(postLink(s)).append("\n\nMargyT");
        return out.toString();
    }

    private void shareStats() {
        if (stats == null) return;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, statsText(stats));
        startActivity(Intent.createChooser(send, Local.of("Поделиться статистикой")));
    }


    private View tile(String picture, String name, long value, String under) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(16), dp(12), dp(12), dp(12));
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView glyph = new ImageView(this);
        glyph.setImageDrawable(SettingsGlyph.make(picture, accentInk()));
        head.addView(glyph, new LinearLayout.LayoutParams(dp(18), dp(18)));
        TextView label = detail(name);
        label.setPadding(dp(6), 0, 0, 0);
        head.addView(label);
        tile.addView(head);
        TextView number = new TextView(this);
        number.setText(compact(value));
        number.setTextColor(skin.text);
        M3Type.titleLarge(number);
        number.setPadding(0, dp(4), 0, 0);
        tile.addView(number);
        if (under != null && under.length() > 0) tile.addView(detail(under));
        final String copied = whole(value);
        tile.setOnClickListener(v -> {
            try {
                ((android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
                        .setPrimaryClip(android.content.ClipData.newPlainText(name, copied));
                Toast.makeText(this, Text.COPIED, Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {
            }
        });
        Motion.press(tile);
        return tile;
    }

    private LinearLayout pair(View left, View right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (right != null) row.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        else row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        return row;
    }

    private int chartKind = 1;
    private long[][] chartPoints;


    private final class Chart extends View {
        final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG), area = new Paint(Paint.ANTI_ALIAS_FLAG),
                text = new Paint(Paint.ANTI_ALIAS_FLAG), dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        final android.graphics.Path path = new android.graphics.Path(), fill = new android.graphics.Path();

        Chart() {
            super(SettingsActivity.this);
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeWidth(getResources().getDisplayMetrics().density * 2.5f);
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setStrokeJoin(Paint.Join.ROUND);
            text.setTextSize(dp(11));
        }

        @Override protected void onMeasure(int w, int h) { setMeasuredDimension(MeasureSpec.getSize(w), dp(170)); }

        @Override protected void onDraw(Canvas canvas) {
            long[][] points = chartPoints;
            int accent = Accent.colour() | 0xFF000000;
            text.setColor(skin.muted());
            float left = dp(16), right = getWidth() - dp(16), top = dp(14), bottom = getHeight() - dp(26);
            if (points == null) { canvas.drawText(Local.of("Загружаю…"), left, getHeight() / 2f, text); return; }
            if (points.length < 2) {
                canvas.drawText(Local.of("Истории пока мало: точки добавляются,"), left, getHeight() / 2f - dp(8), text);
                canvas.drawText(Local.of("когда ролик смотрят люди с MargyT."), left, getHeight() / 2f + dp(10), text);
                return;
            }
            long t0 = points[0][0], t1 = points[points.length - 1][0];
            long low = Long.MAX_VALUE, high = 0;
            for (long[] p : points) { low = Math.min(low, p[chartKind]); high = Math.max(high, p[chartKind]); }
            if (high == low) high = low + 1;
            path.reset(); fill.reset();
            for (int i = 0; i < points.length; i++) {
                float x = left + (right - left) * (points[i][0] - t0) / (float) Math.max(1, t1 - t0);
                float y = bottom - (bottom - top) * (points[i][chartKind] - low) / (float) (high - low);
                if (i == 0) { path.moveTo(x, y); fill.moveTo(x, bottom); }
                else path.lineTo(x, y);
                fill.lineTo(x, y);
                if (i == points.length - 1) {
                    fill.lineTo(x, bottom); fill.close();
                    area.setShader(new android.graphics.LinearGradient(0, top, 0, bottom, (accent & 0xFFFFFF) | 0x55000000,
                            accent & 0xFFFFFF, android.graphics.Shader.TileMode.CLAMP));
                    canvas.drawPath(fill, area);
                    line.setColor(accent);
                    canvas.drawPath(path, line);
                    dot.setColor(accent);
                    canvas.drawCircle(x, y, dp(4), dot);
                }
            }
            java.text.DateFormat day = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT);
            canvas.drawText(day.format(new java.util.Date(t0 * 1000L)), left, getHeight() - dp(8), text);
            String end = day.format(new java.util.Date(t1 * 1000L));
            canvas.drawText(end, right - text.measureText(end), getHeight() - dp(8), text);
            canvas.drawText(compact(high), left, top + dp(4), text);
            canvas.drawText("+" + compact(high - low), right - text.measureText("+" + compact(high - low)), top + dp(4), text);
        }
    }

    private void statsPage() {
        final Bundle s = stats;
        if (s == null) return;
        long views = s.getLong("play"), likes = s.getLong("digg"), comments = s.getLong("comment"),
                shares = s.getLong("share"), saves = s.getLong("collect"), downloads = s.getLong("download"),
                reposts = s.getLong("repost");
        long created = s.getLong("created");
        long now = System.currentTimeMillis() / 1000L;
        float hours = created > 0 ? Math.max(1f / 60f, (now - created) / 3600f) : 0f;

        if (topBar != null) {
            ImageView send = new ImageView(this);
            send.setImageDrawable(SettingsGlyph.make("share", skin.text));
            send.setPadding(dp(12), dp(12), dp(12), dp(12));
            send.setContentDescription(Local.of("Поделиться статистикой"));
            send.setOnClickListener(v -> shareStats());
            Motion.press(send);
            topBar.addView(send, new LinearLayout.LayoutParams(dp(48), dp(48)));
        }


        LinearLayout head = card();
        LinearLayout who = row();
        who.addView(icon("account_circle"));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.addView(label(s.getString("author", "")));
        String when = "";
        if (created > 0) {
            long age = now - created;
            when = age < 3600 ? (age / 60) + " " + Local.of("мин") : age < 86400 ? (age / 3600) + " " + Local.of("ч")
                    : (age / 86400) + " " + Local.of("дн");
            when = "  ·  " + when + " " + Local.of("назад");
        }
        words.addView(detail("@" + s.getString("handle", "") + when));
        who.addView(words, grow());
        head.addView(who);
        String desc = s.getString("desc", "");
        if (desc.length() > 0) {
            TextView text = detail(desc);
            text.setMaxLines(3);
            text.setEllipsize(android.text.TextUtils.TruncateAt.END);
            text.setPadding(dp(20), 0, dp(20), dp(12));
            head.addView(text);
        }
        column.addView(wrap(head));


        LinearLayout grid = card();
        grid.addView(pair(tile("play_circle", Local.of("Просмотры"), views,
                hours > 0 ? compact(Math.round(views / hours)) + " " + Local.of("в час") : null),
                tile("bolt", Local.of("Вовлечённость"), 0, null)));

        LinearLayout first = (LinearLayout) ((LinearLayout) grid.getChildAt(0)).getChildAt(1);
        ((TextView) first.getChildAt(1)).setText(share(likes + comments + shares + saves, views));
        grid.addView(pair(tile("favorite_border", Local.of("Лайки"), likes, share(likes, views)),
                tile("chat", Local.of("Комментарии"), comments, share(comments, views))));
        grid.addView(pair(tile("share", Local.of("Поделились"), shares, share(shares, views)),
                tile("bookmark", Local.of("Сохранили"), saves, share(saves, views))));
        if (reposts > 0 || downloads > 0) grid.addView(pair(tile("repeat", Local.of("Репосты"), reposts, null),
                downloads > 0 ? tile("download", Local.of("Скачали"), downloads, null) : null));
        column.addView(wrap(grid));


        column.addView(section(Local.of("Динамика")));
        LinearLayout chartCard = card();
        final Chart chart = new Chart();
        chartCard.addView(segmented(new String[] {Local.of("Просмотры"), Local.of("Лайки"), Local.of("Комменты")},
                chartKind - 1, at -> { chartKind = at + 1; chart.invalidate(); }));
        chartCard.addView(chart);
        column.addView(wrap(chartCard));
        final String aid = s.getString("aid", "");
        if (aid.length() > 0) VideoStats.history(aid, points -> {

            long[] latest = {now, views, likes, comments, shares, saves};
            if (points.length == 0 || points[points.length - 1][0] < now - 600) {
                long[][] more = java.util.Arrays.copyOf(points, points.length + 1);
                more[points.length] = latest;
                points = more;
            }
            chartPoints = points;
            chart.invalidate();
        });


        column.addView(section(Local.of("Подробно")));
        LinearLayout more = card();
        if (created > 0) more.addView(statRow("schedule", Local.of("Опубликовано"), java.text.DateFormat.getDateTimeInstance(
                java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(new java.util.Date(created * 1000L))));
        String region = s.getString("region", "");
        if (region.length() > 0) {
            String name = new Locale("", region.toUpperCase(Locale.ROOT)).getDisplayCountry();
            more.addView(statRow("public", Local.of("Регион"), name.length() > 0 ? name : region));
        }
        long duration = s.getLong("duration");
        StringBuilder file = new StringBuilder();
        if (s.getInt("images") > 0) file.append(s.getInt("images")).append(' ').append(Local.of("фото"));
        else if (duration > 0) file.append(String.format(Locale.ROOT, "%d:%02d", duration / 60000, (duration / 1000) % 60));
        if (s.getLong("width") > 0) file.append(file.length() > 0 ? " · " : "").append(s.getLong("width")).append('×').append(s.getLong("height"));
        if (s.getLong("bitrate") > 0) file.append(" · ").append(String.format(Locale.ROOT, "%.1f", s.getLong("bitrate") / 1_000_000.0).replace('.', ','))
                .append(' ').append(Local.of("Мбит/с"));
        if (file.length() > 0) more.addView(statRow("hd", Local.of("Файл"), file.toString()));
        if (s.getString("music", "").length() > 0) more.addView(statRow("music_note", Local.of("Звук"),
                s.getString("music", "") + (s.getString("music_author", "").length() > 0 ? " · " + s.getString("music_author", "") : "")));
        java.util.ArrayList<String> tags = s.getStringArrayList("tags");
        if (tags != null && !tags.isEmpty()) {
            StringBuilder all = new StringBuilder();
            for (String tag : tags) all.append(all.length() > 0 ? " " : "").append('#').append(tag);
            more.addView(statRow("text_fields", Local.of("Хэштеги"), all.toString()));
        }
        if (s.getBoolean("ad")) more.addView(statRow("info", Local.of("Реклама"), Local.of("да")));
        if (s.getBoolean("ai")) more.addView(statRow("info", Local.of("Сделано с ИИ"), Local.of("да")));
        more.addView(statRow("link", Local.of("Ссылка"), postLink(s)));
        more.addView(statRow("fingerprint", "ID", aid));
        column.addView(wrap(more));
        if (topBar == null) column.addView(wrap(actionRow("share", Local.of("Поделиться статистикой"), null, this::shareStats)));
    }


    static final String OPEN_PLUGIN = "airtok_open_plugin";

    static final String OPEN_STYLE = "airtok_open_style";

    private void openStyleFile(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(OPEN_STYLE, false) || intent.getData() == null) return;
        final Uri file = intent.getData();
        intent.removeExtra(OPEN_STYLE);
        searchQuery = "";
        settingsGroup = groupOf(Text.APPEARANCE);
        Shelf to = shelfOfSection(Text.APPEARANCE);
        settingsPage = to == null ? null : to.id;
        rebuild();
        root.post(() -> reviewStyle(file));
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openPluginFile(intent);
        openStyleFile(intent);
        openStats(intent);
    }

    private void openPluginFile(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(OPEN_PLUGIN, false) || intent.getData() == null) return;
        final Uri file = intent.getData();
        intent.removeExtra(OPEN_PLUGIN);
        settingsGroup = 6;
        settingsPage = "plugins";
        rebuild();
        root.post(() -> reviewPlugin(file));
    }

    void tutorialStep(int step) {
        settingsGroup=step==1 ? 1 : step==2 ? 3 : step==3 ? 2 : 0;
        if(step==1)accentOpen=true;
        searchQuery="";rebuild();
        String section=step==1 ? Text.ACCENT : step==2 ? BURN : step==3 ? Text.GRADIENT : null;
        View target=section==null ? null : sectionTargets.get(section);
        if(target!=null)page.post(()->page.smoothScrollTo(0,Math.max(0,target.getTop()-dp(20))));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Accent.reload();
        skin = Skin.forSettings(this);
        dressTheWindow();
        if(Streaks.isEnabled()){StreakAlarm.schedule(this,false);Streaks.round(this);}
        if(root!=null && column!=null)rebuild();
    }

    @Override protected void onPause(){
        navigating=false;
        if(column!=null){column.animate().withEndAction(null).cancel();column.setAlpha(1f);column.setTranslationX(0f);}
        super.onPause();
    }

    private void dressTheWindow() {
        try {
            getWindow().setStatusBarColor(skin.page);
            getWindow().setNavigationBarColor(skin.page);
            if (!skin.dark()) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        } catch (Throwable ignored) {
        }
    }



    private void rebuild() {
        navigating=false;
        collapsing=false;
        column.animate().withEndAction(null).cancel(); column.setAlpha(1f); column.setTranslationX(0f);
        revealAnimationUsed = false;
        boolean animateReveal = revealAnimationRequested;
        revealAnimationRequested = false;
        revealAnimationUsed = !animateReveal;
        final int previousScroll = page == null ? 0 : page.getScrollY();


        if (!keepPages) built = null;
        keepPages = false;
        renderDisclosureKey=null;
        column.removeAllViews();
        topBar = null;
        column.addView(backArrow());
        if (settingsGroup == DONATE_PAGE) {
            settingsGroup = 0;
            rebuild();
            return;
        }
        if (settingsGroup == STATS_PAGE) {
            column.addView(title(Local.of("Статистика видео")));
            statsPage();
            restoreScroll(0);
            return;
        }
        if (settingsGroup == COUNTRY_PAGE) {
            column.addView(title(Local.of("Страна")));
            countriesPage();
            restoreScroll(0);
            return;
        }
        if (settingsGroup == FLAGS_PAGE) {
            column.addView(title(Text.DEV_FLAGS));
            flagsPage();
            showRestartBar();
            restoreScroll(previousScroll);
            return;
        }

        if (settingsGroup != 0 && settingsPage == null && searchQuery.length() == 0) {
            java.util.List<Shelf> pages = shelvesOf(settingsGroup);
            if (pages.size() == 1) settingsPage = pages.get(0).id;
        }
        Shelf open = shelf(settingsPage);
        if (open != null && open.group != settingsGroup) { settingsPage = null; open = null; }
        column.addView(title(settingsGroup == 0 ? "AirTok" : open != null ? open.title : groupName(settingsGroup)));
        if (open != null && open.note.length() > 0) { View lead = lead(open.note); lead.setTag(LEAD, Boolean.TRUE); column.addView(lead); }

        final boolean searching = searchQuery.length() > 0;
        if (settingsGroup == 0 && !searching) {
            LinearLayout lockCard = card();
            TextView lockTitle = label(Local.of("Код-пароль при входе"));
            lockTitle.setPadding(dp(16), dp(14), dp(16), dp(6));
            lockTitle.setTextSize(Design.fluent() ? 15 : 16);
            lockTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            lockCard.addView(lockTitle);
            if (!AppLock.isEnabled()) {
                lockCard.addView(actionRow("lock_open", Local.of("Установить код-пароль"), Local.of("Задать 4-значный PIN"), () -> {
                    AppLock.showSetupDialog(this, () -> {
                        markChanged();
                        rebuild();
                    });
                }));
            } else {
                lockCard.addView(actionRow("verified_user", Local.of("Статус защиты"), Local.of("Включена"), null));
                lockCard.addView(line());
                lockCard.addView(actionRow("key", Local.of("Изменить код-пароль"), Local.of("Ввести текущий и новый"), () -> {
                    AppLock.promptVerifyPin(this, "Смена пароля", () -> {
                        AppLock.showSetupDialog(this, () -> {
                            markChanged();
                            rebuild();
                        });
                    });
                }));
                lockCard.addView(line());
                lockCard.addView(actionRow("lock_open", Local.of("Удалить код-пароль"), Local.of("Отключить защиту"), () -> {
                    AppLock.promptVerifyPin(this, "Отключение пароля", () -> {
                        AppLock.disable();
                        android.widget.Toast.makeText(this, "Пароль удалён", android.widget.Toast.LENGTH_SHORT).show();
                        markChanged();
                        rebuild();
                    });
                }));
                lockCard.addView(line());
                lockCard.addView(toggleRow("fingerprint", Local.of("Вход по биометрии"), AppLock.isBioEnabled(), bioOn -> {
                    AppLock.setBioEnabled(bioOn);
                    markChanged();
                }));
            }
            column.addView(wrap(lockCard));
            column.addView(groupNavigation());
            column.addView(versions());
            showRestartBar();
            restoreScroll(0);
            if (built == null) android.os.Looper.myQueue().addIdleHandler(() -> { prebuild(); return false; });
            return;
        }

        final int contentStart = column.getChildCount();
        if (built != null) {

            for (View page : built) {
                if (page.getParent() instanceof ViewGroup) ((ViewGroup) page.getParent()).removeView(page);
                column.addView(page);
            }
        } else {
            sectionTargets.clear();settingTargets.clear();
            disclosureBodies.clear();disclosureArrows.clear();
            try {
                buildContent();
            } catch (Throwable t) {
                Diary.note("settings buildContent error: " + t);
            }
            built = new java.util.ArrayList<>();
            for (int i = contentStart; i < column.getChildCount(); i++) built.add(column.getChildAt(i));
        }

        if (searching) {
            java.util.List<Found> index = harvest(contentStart);
            while (column.getChildCount() > contentStart) column.removeViewAt(contentStart);
            searchResults(index);
            restoreScroll(previousScroll);
            return;
        }
        if (settingsGroup != 0 && open == null) {


            java.util.Set<String> small = smallSections(contentStart);
            java.util.List<Shelf> long_ = new java.util.ArrayList<>();
            java.util.Set<String> inline = new java.util.HashSet<>();
            for (Shelf one : shelvesOf(settingsGroup)) {
                boolean allSmall = one.sections.length > 0;
                for (String name : one.sections) allSmall &= small.contains(name);
                if (allSmall) java.util.Collections.addAll(inline, one.sections);
                else long_.add(one);
            }
            int from = contentStart;
            if (!long_.isEmpty()) { column.addView(shelfList(long_), contentStart); from++; }
            filterSections(from, inline);
        } else filterGroup(contentStart);
        column.addView(versions());

        showRestartBar();
        restoreScroll(previousScroll);
    }


    private void buildContent() {
        column.addView(section(Text.DESIGN));
        column.addView(wrap(designPicker()));

        column.addView(section(Text.ACCENT));
        LinearLayout accentCard = card();
        accentCard.addView(accentHead());
        if (Accent.fromWallpaper() != 0) {
            accentCard.addView(line());
            accentCard.addView(wallpaperRow());
        }
        accentCard.addView(palette());
        column.addView(wrap(accentCard));

        column.addView(section(Text.FONT));
        LinearLayout fonts = card();
        fonts.addView(fontCarousels());
        column.addView(wrap(fonts));


        column.addView(section(MOTION_BLUR));
        LinearLayout blur = card();
        blur.addView(liveToggleRow("animation", MOTION_BLUR, MotionBlur.on(), on -> {
            MotionBlur.set(on);
            markChanged();
            rebuildAfterSwitchAnimation();
        }));
        blur.addView(quiet(MOTION_BLUR_NOTE));
        column.addView(wrap(blur));

        column.addView(section(FRAMES));
        column.addView(wrap(framesCard()));

        column.addView(section(Text.GRADIENT));
        column.addView(wrap(gradientCard()));

        column.addView(section(Text.BADGES));
        column.addView(badgeTabs());
        if (badgesTab == 1) {
            badgesToGet();
        } else {
            LinearLayout badges = card();
            final SettingsPictures.Badged badged = new SettingsPictures.Badged(this, skin, Badges.isEnabled());
            badges.addView(badged);
            badges.addView(toggleRow("favorite_border", Text.BADGES_ON, Badges.isEnabled(), on -> {
                Badges.setEnabled(on);
                badged.set(on);
            }));
            badges.addView(line());
            badges.addView(mineHead());
            if (mineOpen) badges.addView(appear(mineRows()));
            if (AdminPanel.owner()) {

                badges.addView(line());
                badges.addView(actionRow("fingerprint", Text.ADMIN_DEVICES, null, () -> AdminPanel.devices(this)));
            }
            column.addView(wrap(badges));
        }

        column.addView(section(Text.ACCOUNT));
        LinearLayout account = card();
        account.addView(idRow(Text.ACCOUNT_ID, Account.id()));
        account.addView(line());
        account.addView(idRow(Text.ACCOUNT_SEC_ID, Account.secId()));
        column.addView(wrap(account));

        column.addView(section(Local.of("Код-пароль при входе")));
        LinearLayout security = card();
        if (!AppLock.isEnabled()) {
            security.addView(actionRow("lock_open", Local.of("Установить код-пароль"), Local.of("Задать 4-значный PIN"), () -> {
                AppLock.showSetupDialog(this, () -> {
                    markChanged();
                    rebuild();
                });
            }));
        } else {
            security.addView(actionRow("verified_user", Local.of("Статус защиты"), Local.of("Включена"), null));
            security.addView(line());
            security.addView(actionRow("key", Local.of("Изменить код-пароль"), Local.of("Ввести текущий и новый"), () -> {
                AppLock.promptVerifyPin(this, "Смена пароля", () -> {
                    AppLock.showSetupDialog(this, () -> {
                        markChanged();
                        rebuild();
                    });
                });
            }));
            security.addView(line());
            security.addView(actionRow("lock_open", Local.of("Удалить код-пароль"), Local.of("Отключить защиту"), () -> {
                AppLock.promptVerifyPin(this, "Отключение пароля", () -> {
                    AppLock.disable();
                    android.widget.Toast.makeText(this, "Пароль удалён", android.widget.Toast.LENGTH_SHORT).show();
                    markChanged();
                    rebuild();
                });
            }));
            security.addView(line());
            security.addView(toggleRow("fingerprint", Local.of("Вход по биометрии"), AppLock.isBioEnabled(), bioOn -> {
                AppLock.setBioEnabled(bioOn);
                markChanged();
            }));
        }
        security.addView(quiet(Local.of("Защищает запуск AirTok PIN-кодом и биометрией при каждом открытии.")));
        column.addView(wrap(security));

        column.addView(section(Text.PROFILE_VIEW));
        LinearLayout look = card();
        final SettingsPictures.Layout layout = new SettingsPictures.Layout(this, skin, Flags.isOn(Flags.KEY_NEW_PROFILE));
        look.addView(layout);
        look.addView(toggleRow("view_compact", Text.NEW_PROFILE, Flags.isOn(Flags.KEY_NEW_PROFILE), on -> {
            Flags.set(Flags.KEY_NEW_PROFILE, on);
            layout.set(on);
        }));
        look.addView(quiet(Text.NEW_PROFILE_NOTE));
        look.addView(line());
        look.addView(toggleRow("image", Local.of("Баннеры и анимации профиля"), Flags.isOn(Flags.KEY_BANNER), on -> {
            Flags.set(Flags.KEY_BANNER, on);
        }));
        look.addView(quiet(Local.of("Включает шапку / фон (статический и анимированный баннер) в профиле.")));
        column.addView(wrap(look));

        column.addView(section(Text.REGION));
        LinearLayout head = card();
        head.addView(switchRow());
        head.addView(line());
        head.addView(countryHead());
        column.addView(wrap(head));

        column.addView(section(Text.FEED));
        LinearLayout feed = card();

        final SettingsPictures.Feed strip = new SettingsPictures.Feed(this, skin)
                .hide(SettingsPictures.Feed.AD, true)
                .hide(SettingsPictures.Feed.LIVE, Feed.hides(Feed.KEY_LIVE))
                .hide(SettingsPictures.Feed.PHOTO, Feed.hides(Feed.KEY_PHOTOS))
                .hide(SettingsPictures.Feed.STORY, Feed.hides(Feed.KEY_STORIES));
        feed.addView(strip);
        feed.addView(toggleRow("visibility_off", Text.HIDE_LIVE, Feed.hides(Feed.KEY_LIVE), on -> {
            Feed.setHides(Feed.KEY_LIVE, on);
            strip.hide(SettingsPictures.Feed.LIVE, on).settle();
        }));
        feed.addView(line());
        feed.addView(toggleRow("image", Text.HIDE_PHOTOS, Feed.hides(Feed.KEY_PHOTOS), on -> {
            Feed.setHides(Feed.KEY_PHOTOS, on);
            strip.hide(SettingsPictures.Feed.PHOTO, on).settle();
        }));
        feed.addView(line());
        feed.addView(liveToggleRow("visibility_off",Text.HIDE_STORIES,Feed.hides(Feed.KEY_STORIES),on->{
            Feed.setHides(Feed.KEY_STORIES,on);
            strip.hide(SettingsPictures.Feed.STORY, on).settle();
        }));
        feed.addView(line());
        feed.addView(liveToggleRow("schedule", Local.of("Показ даты и времени"), Dates.isEnabled(), on -> {
            Dates.setEnabled(on);
        }));
        feed.addView(quiet(Local.of("Отображает дату, точное время и флаг страны публикации под автором видео.")));
        feed.addView(line());
        feed.addView(toggleRow("translate", Local.of("Кнопка перевода комментариев"), Flags.isOn(Flags.KEY_TRANSLATE_COMMENTS), on -> {
            Flags.set(Flags.KEY_TRANSLATE_COMMENTS, on);
            markChanged();
        }));
        feed.addView(quiet(Local.of("Кнопка автоперевода всех комментариев прямо в шапке окна комментариев.")));
        feed.addView(line());
        feed.addView(liveToggleRow("favorite", Local.of("Лайк комментария по двойному тапу"), Comments.isDoubleTapLikeEnabled(), on -> {
            Comments.setDoubleTapLikeEnabled(on);
        }));
        feed.addView(quiet(Local.of("Поставить лайк комментарию двойным быстрым нажатием.")));
        column.addView(wrap(feed));

        column.addView(section(Text.SIMPLE_FEED));
        LinearLayout simpler = card();
        final SettingsArt.Phone simpleArt = phone("simple").simple(SimpleFeed.isOn());
        simpler.addView(simpleArt);
        simpler.addView(liveToggleRow("view_compact", Text.SIMPLE_FEED, SimpleFeed.isOn(), on -> {
            SimpleFeed.set(this, on);
            simpleArt.simple(on).settle();
        }));
        simpler.addView(quiet(Text.SIMPLE_FEED_NOTE));
        column.addView(wrap(simpler));

        column.addView(section(Text.DATE_FILTER));
        LinearLayout dates = card();
        final SettingsPictures.Ages ages = new SettingsPictures.Ages(this, skin, FeedDates.hours());
        dates.addView(ages);
        LinearLayout datesHead = row();
        datesHead.addView(icon("schedule"));
        datesHead.addView(label(Text.DATE_FILTER), grow());
        final TextView datesNow = detail(FeedDates.describe(FeedDates.hours()));
        datesNow.setTextColor(skin.text);
        datesHead.addView(datesNow);
        dates.addView(sized(datesHead, 52));
        String[] limits = new String[FeedDates.HOURS.length];
        int limitAt = 0;
        for (int i = 0; i < limits.length; i++) {
            limits[i] = FeedDates.describe(FeedDates.HOURS[i]);
            if (FeedDates.HOURS[i] == FeedDates.hours()) limitAt = i;
        }
        dates.addView(chips(limits, limitAt, index -> {
            FeedDates.setHours(FeedDates.HOURS[index]);
            datesNow.setText(FeedDates.describe(FeedDates.HOURS[index]));
            ages.set(FeedDates.HOURS[index]);
        }));
        dates.addView(caption(Text.DATE_FILTER_NOTE));
        column.addView(wrap(dates));

        column.addView(section(Text.VIDEO));
        LinearLayout video = card();

        final SettingsPictures.Speed speedArt = new SettingsPictures.Speed(this, skin, PlayerTools.speed(),
                Flags.isOn(Flags.KEY_SPEED_LOCK));
        video.addView(speedArt);
        LinearLayout speedHead = row();
        speedHead.addView(icon("speed"));
        speedHead.addView(label(Text.PLAYER_SPEED), grow());
        final TextView speedNow = detail(PlayerTools.speed() + "\u00D7");
        speedNow.setTextColor(skin.text);
        speedHead.addView(speedNow);
        video.addView(sized(speedHead, 52));
        final float[] speeds = {1f, 1.5f, 2f, 3f};
        int speedAt = 4;
        for (int i = 0; i < speeds.length; i++) if (Math.abs(PlayerTools.speed() - speeds[i]) < 0.01f) speedAt = i;
        video.addView(segmented(new String[] {"1\u00D7", "1.5\u00D7", "2\u00D7", "3\u00D7", "\u2026"}, speedAt, index -> {
            if (index == 4) { PlayerTools.choose(this, this::rebuild); return; }
            PlayerTools.setSpeed(speeds[index]);
            speedNow.setText(speeds[index] + "\u00D7");
            speedArt.set(speeds[index], Flags.isOn(Flags.KEY_SPEED_LOCK));
        }));
        video.addView(toggleRow("lock", Text.SPEED_LOCK, Flags.isOn(Flags.KEY_SPEED_LOCK), on -> {
            Flags.set(Flags.KEY_SPEED_LOCK, on);
            speedArt.set(PlayerTools.speed(), on);
        }));

        column.addView(wrap(video));

        column.addView(section(Text.FPS));
        LinearLayout fps = card();

        final SettingsPictures.Fps fpsArt = new SettingsPictures.Fps(this, skin, Rate.name());
        fps.addView(fpsArt);
        String rate = Rate.name();
        String[] rates = new String[Rate.CHOICES.length];
        int rateAt = 0;
        for (int i = 0; i < Rate.CHOICES.length; i++) {
            rates[i] = Rate.CHOICES[i].length() == 0 ? Text.FPS_AUTO : Rate.CHOICES[i];
            if (Rate.CHOICES[i].equals(rate)) rateAt = i;
        }
        fps.addView(segmented(rates, rateAt, index -> {
            Rate.choose(Rate.CHOICES[index]);
            fpsArt.set(Rate.CHOICES[index]);
            markChanged();
        }));
        fps.addView(quiet(Text.FPS_ABOUT));
        if (!Rate.reachable(this)) fps.addView(quiet(Text.FPS_UNSUPPORTED));
        column.addView(wrap(fps));


        column.addView(section(MOTION));
        LinearLayout cues = card();
        cues.addView(liveToggleRow("animation", MOTION, MotionCues.enabled(), on -> MotionCues.setEnabled(this, on)));
        cues.addView(quiet(Local.of("Точки по краям экрана двигаются вместе с машиной, как на iPhone. Глазам становится понятно, что ты едешь, и меньше укачивает. Когда телефон не двигается, точки исчезают.")));
        column.addView(wrap(cues));

        column.addView(section(BURN));
        LinearLayout burn = card();

        final SettingsArt.Phone burnArt = phone("burn").burn(Dim.isConfigured(), Dim.strength(), NavBar.isKept());
        burn.addView(burnArt);
        burn.addView(liveToggleRow("eye_open", Text.DIM, Dim.isConfigured(), on -> {
            Dim.setEnabled(on);
            burnArt.burn(on, Dim.strength(), NavBar.isKept()).settle();
            rebuildAfterSwitchAnimation();
        }));
        if (Dim.isConfigured()) {
            burn.addView(line());
            burn.addView(slider("contrast", Text.DIM_HOW, Dim.strength(), 100, 0, "%", value -> Dim.setStrength(value), false,
                    value -> burnArt.burn(true, value, NavBar.isKept()).settle()));
            burn.addView(line());
            burn.addView(toggleRow("visibility_off", Text.KEEP_BAR, NavBar.isKept(), on -> {
                NavBar.setKept(on);
                burnArt.burn(true, Dim.strength(), on).settle();
            }));
        }
        burn.addView(quiet(Local.of("Глаз в ленте включает и выключает приглушение на лету.")));
        column.addView(wrap(burn));

        column.addView(section(Text.DOWNLOADS));
        LinearLayout downloads = card();
        downloads.addView(liveToggleRow("download", Text.DOWNLOAD_WINDOW, VideoActions.windowOn(), VideoActions::setWindow));
        downloads.addView(quiet(Text.DOWNLOAD_WINDOW_NOTE));
        downloads.addView(line());
        LinearLayout qualityHead = row();
        qualityHead.addView(icon("image"));
        qualityHead.addView(label(Text.FRAME_QUALITY), grow());
        downloads.addView(sized(qualityHead, 52));
        final int[] qualities = {480, 720, 1080};
        int qualityAt = FrameCapture.quality() == 480 ? 0 : FrameCapture.quality() == 1080 ? 2 : 1;
        downloads.addView(segmented(new String[] {"480p", "720p", "1080p"}, qualityAt, index ->
                AccountAppearance.prefs(this).edit().putInt(FrameCapture.KEY, qualities[index]).apply()));
        final SettingsPictures.Watermark mark = new SettingsPictures.Watermark(this, skin, Download.isEnabled());
        downloads.addView(mark, 0);
        downloads.addView(toggleRow("image", Text.NO_WATERMARK, Download.isEnabled(), on -> {
            Download.setEnabled(on);
            mark.set(on);
        }));
        downloads.addView(line());
        downloads.addView(toggleRow("download", Text.DOWNLOAD_ALWAYS, Download.isAlways(),
                Download::setAlways));
        downloads.addView(line());
        downloads.addView(toggleRow("place", Text.SAVE_AVATARS_ON, Avatars.isEnabled(),
                Avatars::setEnabled));
        downloads.addView(line());
        downloads.addView(toggleRow("star", Text.SAVE_STICKERS_ON, Stickers.isEnabled(),
                Stickers::setEnabled));
        column.addView(wrap(downloads));

        column.addView(section(Text.BACKUP));
        LinearLayout backup = card();
        backup.addView(actionRow("upload", Text.BACKUP_EXPORT, Text.BACKUP_EXPORT_NOTE,
                this::createBackup));
        backup.addView(line());
        backup.addView(actionRow("download", Text.BACKUP_IMPORT, Text.BACKUP_IMPORT_NOTE,
                this::pickBackup));
        backup.addView(line());
        backup.addView(actionRow("restore", Text.BACKUP_RESET, Text.BACKUP_RESET_NOTE,
                this::pickResetGroup));
        column.addView(wrap(backup));

        column.addView(section(Text.PLUGINS));
        LinearLayout plugins = card();
        plugins.addView(actionRow("add", Local.of("Добавить плагин"), null, this::showPluginAdd));
        plugins.addView(line());
        if (DevLink.linked(this)) {
            plugins.addView(actionRow("extension", Local.of("MargyCode подключён"), Local.of("Его агент пишет и ставит плагины. Нажми, чтобы отключить"), () -> {
                DevLink.unlink(this);
                rebuild();
            }));
        } else {
            plugins.addView(actionRow("extension", Local.of("Писать плагины через ИИ"), Local.of("Установи MargyCode и подключи MargyT в его настройках. Агент сам напишет плагин и проверит его прямо на телефоне"), () -> {
                android.content.Intent code = getPackageManager().getLaunchIntentForPackage(DevLink.MARGYCODE);
                if (code != null) startActivity(code); else Screen.say(Local.of("MargyCode не установлен, он есть в AirTok Store"));
            }));
        }
        List<Plugins.Info> installed = Plugins.list();
        List<ScriptPlugins.Program> commands=ScriptPlugins.list(this);
        if(installed.isEmpty()&&commands.isEmpty())plugins.addView(quiet(Local.of("Пока нет установленных плагинов")));
        for(ScriptPlugins.Program program:commands){
            boolean enabled=getSharedPreferences(ScriptPlugins.STORE,0).getBoolean("on:"+program.id,false);
            plugins.addView(actionRow("extension",program.name,enabled?Local.of("Включён"):Local.of("Выключен"),()->ScriptPlugins.details(this,program,this::rebuild)));
        }
        for(Plugins.Info info:installed)plugins.addView(pluginRow(info));
        column.addView(wrap(plugins));

        column.addView(section(Text.STREAKS));
        LinearLayout streaks = card();
        streaks.addView(betaRow("repeat", Text.STREAK_AUTO, Streaks.isEnabled(), on -> {
            Streaks.setEnabled(on);
        }));
        streaks.addView(line());
        streaks.addView(stickerHead());
        if (streakOpen) streaks.addView(appear(stickerChoices()));
        streaks.addView(line());
        streaks.addView(actionRow("bug_report", Text.STREAK_HEALTH, StreakAlarm.status(this), () -> {
            StreakAlarm.permissions(this);
            Toast.makeText(this, StreakAlarm.status(this), Toast.LENGTH_LONG).show();
        }));
        streaks.addView(line());
        streaks.addView(actionRow("schedule", Local.of("Время отправки"), StreakSchedule.label(), () -> StreakSchedule.choose(this, this::rebuild)));
        streaks.addView(line());
        streaks.addView(actionRow("play_circle", Text.STREAK_ALL, Text.STREAK_ALL_NOTE,
                () -> Popup.ask(this, Text.STREAK_ALL, Text.STREAK_ALL_ASK,
                        Text.STREAK_ALL_SEND, () -> Streaks.sendAll(this),
                        Text.CANCEL, null, null, null)));
        streaks.addView(line());
        streaks.addView(liveToggleRow("schedule", Local.of("Точное время сообщений в чатах"), MessageTimes.enabled(), on -> {
            AccountAppearance.prefs(this).edit().putBoolean(MessageTimes.KEY, on).apply();
            MessageTimes.reload();
        }));
        column.addView(wrap(streaks));

        column.addView(section(Text.NOTIFICATIONS));
        LinearLayout notifications=card();

        final SettingsPictures.Notices notices = new SettingsPictures.Notices(this, skin)
                .set(0, ModNotifications.option(0)).set(1, ModNotifications.option(1)).set(2, ModNotifications.option(2));
        notifications.addView(notices);
        notifications.addView(liveToggleRow("chat",Text.NOTIFY_MESSAGES,ModNotifications.option(0),on->{ModNotifications.set(ModNotifications.MESSAGES,on);notices.set(0,on).settle();if(!on)rebuildAfterSwitchAnimation();}));
        notifications.addView(liveToggleRow("notifications",Text.NOTIFY_PROMOS,ModNotifications.option(1),on->{ModNotifications.set(ModNotifications.PROMOS,on);notices.set(1,on).settle();if(!on)rebuildAfterSwitchAnimation();}));
        notifications.addView(liveToggleRow("notifications",Text.NOTIFY_OTHER,ModNotifications.option(2),on->{ModNotifications.set(ModNotifications.OTHER,on);notices.set(2,on).settle();if(!on)rebuildAfterSwitchAnimation();}));
        notifications.addView(liveToggleRow("notifications_off",Text.NOTIFY_NO_PROMPTS,ModNotifications.noPrompts(),on->{AccountAppearance.prefs(this).edit().putBoolean(ModNotifications.NO_PROMPTS,on).apply();ModNotifications.reload();}));
        notifications.addView(actionRow("settings",Text.NOTIFY_SETTINGS,null,()->ModNotifications.settings(this)));
        column.addView(wrap(notifications));

        column.addView(section(Text.CHANNEL));
        column.addView(channelBanner());

        column.addView(section(Text.LINKS));
        LinearLayout links = card();
        links.addView(linkRow("group", Text.FORUM, "@margeletforum", FORUM));
        links.addView(line());
        links.addView(linkRow("extension", Text.SOURCE, "narezany/MargyT", GIT));
        column.addView(wrap(links));

        column.addView(section(Text.DIAGNOSTICS));
        LinearLayout diagnostics = card();
        diagnostics.addView(liveToggleRow("bug_report", Local.of("Отчёты о сбоях"),CrashReports.enabled(this),on->CrashReports.set(this,on)));
        diagnostics.addView(quiet(Local.of("Технический стек сбоя, без сообщений и аккаунта.")));
        diagnostics.addView(quiet(Diagnostics.report()));
        diagnostics.addView(line());
        diagnostics.addView(liveToggleRow("timeline", Local.of("Записывать подвисания"), Lag.wanted(), on -> Lag.setWanted(on)));
        diagnostics.addView(quiet(Local.of("Нужно, только если присылаешь отчёт о лагах. Пока включено, немного тратит батарею.")));
        diagnostics.addView(line());
        diagnostics.addView(actionRow("article", Text.COPY_DIAGNOSTICS, null,
                () -> copy("MargyT diagnostics", DevMode.isOn()
                        ? Diagnostics.fullReport() : Diagnostics.report())));
        column.addView(wrap(diagnostics));

        column.addView(section(Text.DIARY));
        LinearLayout diary = card();
        diary.addView(diaryHead());
        if (diaryOpen) {
            diary.addView(line());
            diary.addView(appear(diaryLines()));
        }
        if (DevMode.isOn()) addDeveloperRows(diary);
        column.addView(wrap(diary));

        column.addView(section(Local.of("О проекте")));
        LinearLayout aboutCard = card();
        aboutCard.addView(creatorRow(Local.of("Создатель"), "Fasdeq13 (GitHub)", "https://github.com/fasdeq13"));
        aboutCard.addView(line());
        aboutCard.addView(actionRow("send", Local.of("Telegram"), "t.me/airtokmod", () -> open("https://t.me/airtokmod")));
        aboutCard.addView(line());
        aboutCard.addView(actionRow("link", Local.of("Открытый исходный код"), "github.com/Fasdeq13/AirTok", () -> open("https://github.com/Fasdeq13/AirTok")));
        aboutCard.addView(line());
        aboutCard.addView(actionRow("info", Local.of("Приложение"), "AirTok (v" + Version.TIKTOK + ")", null));
        column.addView(wrap(aboutCard));

        LinearLayout thanksCard = card();
        TextView thanksTitle = label(Local.of("Благодарность"));
        thanksTitle.setPadding(dp(16), dp(14), dp(16), dp(6));
        thanksTitle.setTextSize(Design.fluent() ? 15 : 16);
        thanksTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        thanksCard.addView(thanksTitle);
        String[] thanksList = {"montik", "Fezix"};
        for (int i = 0; i < thanksList.length; i++) {
            thanksCard.addView(line());
            thanksCard.addView(actionRow("favorite_border", thanksList[i], null, null));
        }
        column.addView(wrap(thanksCard));

        column.addView(section(DOWNLOADS));
        LinearLayout dlCard = card();
        dlCard.addView(liveToggleRow("download", Local.of("Скачивание без водяного знака"), Download.isEnabled(), on -> {
            Download.setEnabled(on);
            rebuildAfterSwitchAnimation();
        }));
        dlCard.addView(quiet(Local.of("Сохранение видео в чистом виде без водяного знака TikTok.")));
        dlCard.addView(line());
        dlCard.addView(liveToggleRow("lock", Local.of("Принудительное скачивание"), Download.isAlways(), on -> {
            Download.setAlways(on);
            rebuildAfterSwitchAnimation();
        }));
        dlCard.addView(quiet(Local.of("Возвращает кнопку скачивания, даже если автор запретил сохранять свои видео.")));
        dlCard.addView(line());
        final String[] videoQualities = new String[] {
            Local.of("Обычное (по умолчанию)"),
            Local.of("Максимальное (1080p / HD)"),
            Local.of("Экономичное (сжатое)")
        };
        dlCard.addView(actionRow("hd", Local.of("Качество скачиваемого видео"), videoQualities[DownloadQuality.mode()], () -> {
            new ModDialog.Builder(this).setItems(videoQualities, (dialog, index) -> {
                DownloadQuality.setMode(index);
                rebuild();
            }).show();
        }));
        dlCard.addView(line());
        dlCard.addView(liveToggleRow("account_circle", Local.of("Сохранение аватарок в оригинале"), Avatars.isEnabled(), on -> {
            Avatars.setEnabled(on);
            rebuildAfterSwitchAnimation();
        }));
        dlCard.addView(quiet(Local.of("Долгое нажатие на аватарку пользователя открывает её в полном максимальном разрешении.")));
        column.addView(wrap(dlCard));

        column.addView(section(TOOLS));
        LinearLayout toolsCard = card();
        toolsCard.addView(liveToggleRow("tune", Local.of("Шкала перемотки на всех видео"), Seekbar.isEnabled(), on -> {
            Seekbar.setEnabled(on);
            rebuildAfterSwitchAnimation();
        }));
        toolsCard.addView(quiet(Local.of("Всегда показывает полосу перемотки внизу экрана для любых роликов, даже коротких.")));
        toolsCard.addView(line());
        toolsCard.addView(liveToggleRow("visibility_off", Local.of("Режим чистого экрана (Zen)"), SimpleFeed.isOn(), on -> {
            SimpleFeed.set(this, on);
            rebuildAfterSwitchAnimation();
        }));
        toolsCard.addView(quiet(Local.of("Минималистичный плеер без мешающих кнопок, подписей и лишнего визуального шума поверх видео.")));
        toolsCard.addView(line());
        toolsCard.addView(liveToggleRow("link", Local.of("Чистые ссылки без слежки"), ShareLinks.enabled(), on -> {
            ShareLinks.setEnabled(on);
            rebuildAfterSwitchAnimation();
        }));
        toolsCard.addView(quiet(Local.of("При копировании ссылки удаляет все метки слежки, UTM-параметры и привязку к вашему аккаунту.")));
        column.addView(wrap(toolsCard));

        column.addView(section(REPOSTS));
        LinearLayout reposts = card();
        reposts.addView(new NewPictures.Reposts(this, skin));
        reposts.addView(actionRow("repeat", Local.of("Удалить все мои репосты"), Local.of("Все репосты пропадут из профиля"), () -> RepostPurge.begin(this)));
        column.addView(wrap(reposts));
    }


    private View fatCat() {
        final ImageView cat = new ImageView(this);
        cat.setAdjustViewBounds(true);
        cat.setScaleType(ImageView.ScaleType.FIT_CENTER);
        cat.setContentDescription(Text.FUN_CAT);
        GradientDrawable round = new GradientDrawable();
        round.setColor(skin.card);
        round.setCornerRadius(dp(28));
        cat.setBackground(round);
        cat.setClipToOutline(true);
        cat.setMinimumHeight(dp(160));
        if (fatCatPicture != null) {
            cat.setImageBitmap(fatCatPicture);
        } else {
            final Context app = getApplicationContext();
            Net.away("fat cat", () -> {
                try {
                    byte[] raw = Cats.fat(app);
                    if (raw == null) return;
                    final android.graphics.Bitmap picture =
                            android.graphics.BitmapFactory.decodeByteArray(raw, 0, raw.length);
                    runOnUiThread(() -> {
                        fatCatPicture = picture;
                        if (picture != null && cat.isAttachedToWindow()) cat.setImageBitmap(picture);
                    });
                } catch (Throwable error) {
                    Diary.note("fat cat: " + error);
                }
            });
        }
        LinearLayout box = new LinearLayout(this);
        box.setPadding(edge(), 0, edge(), dp(6));
        box.addView(cat, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return box;
    }

    private static android.graphics.Bitmap fatCatPicture;


    private View chipRow(String picture, String title, String[] chips, final Runnable action) {
        LinearLayout row = row();
        row.addView(icon(picture));
        row.addView(label(title));
        LinearLayout marks = new LinearLayout(this);
        marks.setGravity(Gravity.CENTER_VERTICAL);
        for (String word : chips) {
            TextView chip = new TextView(this);
            chip.setText(word);
            chip.setTextColor(onAccent());
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            chip.setPadding(dp(8), dp(3), dp(8), dp(3));
            GradientDrawable back = new GradientDrawable();
            back.setColor(Accent.colour());
            back.setCornerRadius(dp(12));
            chip.setBackground(back);
            LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            place.rightMargin = dp(6);
            marks.addView(chip, place);
        }
        row.addView(marks, grow());
        ImageView chevron = new ImageView(this);
        chevron.setImageDrawable(SettingsGlyph.make("chevron_right", skin.muted()));
        row.addView(chevron, new LinearLayout.LayoutParams(dp(24), dp(24)));
        row.setOnClickListener(v -> action.run());
        return sized(row, 56);
    }

    private void restoreScroll(final int scrollY) {
        Fonts.applyTree(root);
        if (page == null) return;
        requestedScrollY = Math.max(0, scrollY);
        if (scrollRestore != null) page.removeCallbacks(scrollRestore);
        if (scrollRestoreListener != null && page.getViewTreeObserver().isAlive()) {
            page.getViewTreeObserver().removeOnPreDrawListener(scrollRestoreListener);
            scrollRestoreListener = null;
        }
        if (requestedScrollY == 0) {
            page.scrollTo(0, 0);
            scrollRestore = null;
            return;
        }


        scrollRestoreListener = new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                page.getViewTreeObserver().removeOnPreDrawListener(this);
                scrollRestoreListener = null;
                page.scrollTo(0, requestedScrollY);
                return true;
            }
        };
        page.getViewTreeObserver().addOnPreDrawListener(scrollRestoreListener);
    }


    private void showRestartBar() {
        if (pending && restartBar == null) {
            restartBar = restartBar();
            FrameLayout.LayoutParams floating = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
            floating.setMargins(dp(12), 0, dp(12), dp(12) + navigationBar());
            root.addView(restartBar, floating);
            Motion.rise(restartBar);
        }
        if (pending && restartBar instanceof LinearLayout) {
            LinearLayout bar = (LinearLayout)restartBar;
            styleRestartBar(bar);
            restartSymbol.colour(Accent.colour());
            restartWhy.setTextColor(skin.text);
            restartAction.setTextColor(onAccent());
            restartBackground.setColor(Accent.colour());
        }

        if (!pending && restartBar != null) {
            restartBar.animate().withEndAction(null).cancel();
            root.removeView(restartBar);
            restartBar = null;
            restartSymbol=null;restartWhy=null;restartAction=null;restartBackground=null;
        }
        column.setPadding(0, statusBar(), 0,
                dp(32) + (pending ? dp(92) + navigationBar() : 0));
    }


    private void styleRestartBar(LinearLayout bar) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Expressive.mix(skin.card, Accent.colour(), 0.22f));
        shape.setCornerRadius(dp(28));
        bar.setBackground(shape);
        bar.setElevation(dp(6));
    }



    static void restartOwed() {
        pending = true;
        Unsaved.other();
    }

    private void markChanged() {


        pending = Unsaved.after(this);
        showRestartBar();
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {

        if (event != null && event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
            Unsaved.look(this);
        }
        return super.dispatchTouchEvent(event);
    }






    private static final String VISITS = "settings_visits";
    private static final String ASKED = "settings_asked";
    private static final int ON_VISIT = 3;

    private java.util.List<Integer> gradientColours;
    private int gradientSlot;



    private boolean donateClosed;


    static final String BANNER_ALWAYS = "donate_banner_always";


    private boolean bannerAlways() {
        return false;
    }


    private static boolean supporter() {
        try {
            if (Mine.holds(Tester.SUPPORTER)) return true;
            Badges.Badge[] worn = Badges.of(Account.id());
            if (worn != null) for (Badges.Badge one : worn) if (one != null && Tester.SUPPORTER.equals(one.id)) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private View donateBanner() {


        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(8), dp(8), dp(16));
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(Design.frosted() ? 18 : 12));
        background.setColor(skin.card);
        card.setBackground(background);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView head = new TextView(this);
        head.setText(Text.DONATE_BANNER);
        head.setTextColor(skin.text);
        M3Type.line(head, 16, 0.15f);
        head.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        top.addView(head, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView close = new ImageView(this);
        close.setImageDrawable(SettingsGlyph.make("close", skin.muted()));
        close.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription(Text.CLOSE);
        Motion.press(close);
        close.setOnClickListener(v -> {
            donateClosed = true;
            View banner = card;
            ViewGroup holder = banner.getParent() instanceof ViewGroup ? (ViewGroup) banner.getParent() : null;
            View gone = holder != null && holder != column ? holder : banner;
            gone.animate().alpha(0f).setDuration(160).withEndAction(() -> rebuild()).start();
        });
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        card.addView(top);

        TextView body = new TextView(this);
        body.setText(Text.DONATE_BANNER_TEXT);
        body.setTextColor(skin.muted());
        M3Type.bodyMedium(body);
        body.setPadding(0, 0, dp(8), 0);
        card.addView(body);

        TextView go = new TextView(this);
        go.setText(Text.DONATE_BANNER_BUTTON);
        go.setTextColor(onAccent());
        M3Type.line(go, 14, 0.1f);
        go.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        go.setGravity(Gravity.CENTER);
        go.setPadding(dp(24), 0, dp(24), 0);
        go.setBackground(Expressive.tile(this, Accent.colour(), 0x33FFFFFF, dp(20), dp(20), dp(8)));
        go.setOnClickListener(v -> showDonations());
        LinearLayout.LayoutParams below = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
        below.topMargin = dp(16);
        below.gravity = Gravity.END;
        below.rightMargin = dp(8);
        card.addView(go, below);

        LinearLayout holder = new LinearLayout(this);
        holder.setPadding(edge(), dp(8), edge(), dp(2));
        holder.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return holder;
    }


    private void maybeAskForSupport() {
        return;
    }

    private void tookFree(boolean ok, String trouble) {
        if (needsProof(ok, trouble)) return;
        Screen.say(ok ? Text.FREE_BADGE_GOT : Text.MINE_TOO_OFTEN);
        rebuild();
    }


    private View framesCard() {
        LinearLayout card = card();
        final java.util.List<String> held = Frames.mine();
        final int ground = (skin.muted() & 0xFFFFFF) | 0x33000000;
        final Frames.Preview large = new Frames.Preview(this, ground);
        large.set(Frames.byId(Frames.mineWorn()));
        LinearLayout.LayoutParams tall = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(190));
        tall.topMargin = dp(12);
        tall.bottomMargin = dp(8);
        card.addView(large, tall);
        card.addView(proveRow());
        if (held.isEmpty()) {
            card.addView(quiet(Local.of("Пока рамок нет. Рамка приходит вместе со значком: золотая за значок поддержавшего.")));
            return card;
        }
        final java.util.List<M3Switch> toggles = new java.util.ArrayList<>();
        for (final String id : held) {
            Frames.Frame frame = Frames.byId(id);
            if (frame == null) continue;
            card.addView(line());
            LinearLayout row = row();
            row.setPadding(edge(), dp(8), edge(), dp(8));
            Frames.Preview small = new Frames.Preview(this, ground);
            small.set(frame);
            LinearLayout.LayoutParams picture = new LinearLayout.LayoutParams(dp(60), dp(60));
            picture.rightMargin = dp(12);
            row.addView(small, picture);
            LinearLayout words = new LinearLayout(this);
            words.setOrientation(LinearLayout.VERTICAL);
            words.addView(label(frame.title));
            if (frame.text.length() > 0) words.addView(detail(frame.text));
            row.addView(words, grow());
            final M3Switch toggle = new M3Switch(this);
            toggle.colours(Accent.colour(), skin.muted(), skin.card);
            toggle.setChecked(id.equals(Frames.mineWorn()), false);
            toggle.setOnChanged(checked -> {
                if (checked) for (M3Switch other : toggles) if (other != toggle) other.setChecked(false, true);
                String now = checked ? id : "";
                large.set(Frames.byId(now));
                Frames.wear(now, (ok, trouble) -> {
                    if (needsProof(ok, trouble)) return;
                    if (!ok) { Screen.say(Text.MINE_TOO_OFTEN); Mine.ask(this::rebuild); }
                });
            });
            toggles.add(toggle);
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(52), dp(32));
            size.leftMargin = dp(10);
            row.addView(toggle, size);
            row.setOnClickListener(v -> toggle.performClick());
            card.addView(row);
        }
        card.addView(quiet(Local.of("Рамка одна: включишь другую, эта снимется. Её видят все, у кого MargyT 2.2.1 или новее.")));
        return card;
    }


    private View proveRow() {
        boolean done = Proof.proved();
        LinearLayout row = row();
        row.setPadding(dp(16), 0, dp(16), 0);
        row.addView(icon(done ? "verified_user" : "fingerprint"));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(done ? Text.PROVE_DONE : Text.PROVE));
        text.addView(detail(done ? Text.PROVE_DONE_NOTE : Text.PROVE_NOTE));
        row.addView(text, grow());

        if (!done) row.setOnClickListener(v -> proveAccount());
        return sized(row, 64);
    }


    private void proveAccount() {
        if (!BioProof.available()) {
            proveByHand();
            return;
        }
        Screen.say(Text.PROVE_AUTO_GOING);
        BioProof.start((proved, said) -> {
            if (isFinishing() || isDestroyed()) return;
            if (proved) {
                Screen.say(Text.PROVE_OK);
                ordering = null;
                Mine.ask(this::rebuild);
                rebuild();
                return;
            }
            Screen.say(said);
            proveByHand();
        });
    }


    private void proveByHand() {
        String already = Proof.waiting();
        if (already.length() > 0) {
            askToCheck(already);
            return;
        }
        Screen.say(Text.MINE_WAIT);
        Proof.want((ok, trouble) -> {
            if (!ok) {
                Screen.say(trouble);
                return;
            }
            askToCheck(Proof.waiting());
        });
    }


    private boolean needsProof(boolean ok, String trouble) {
        if (ok || !Mine.PROVE.equals(trouble)) return false;
        proveAccount();
        return true;
    }

    private void askToCheck(String code) {
        Popup.prove(this, code, (name, reply) -> Proof.check(name, (ok, trouble) -> {
            if (ok) {
                Screen.say(Text.PROVE_OK);
                ordering = null;
                Mine.ask(this::rebuild);
            }

            reply.said(ok, trouble, Proof.waiting(), Proof.wantsName());
            rebuild();
        }), reply -> Proof.fresh((ok, trouble) ->
                reply.said(ok, trouble, Proof.waiting(), false)));
    }

    private LinearLayout gradientCard() {
        LinearLayout card = card();
        if (!Mine.holds(Tester.SUPPORTER)) {
            card.addView(actionRow("gradient", Text.GRADIENT, null, () -> {}));
            card.addView(caption(Text.GRADIENT_ONLY));
            return card;
        }
        if (gradientColours == null) gradientColours = Gradient.starting();
        if (gradientSlot >= gradientColours.size()) gradientSlot = 0;

        card.addView(gradientPreview());
        card.addView(gradientDots());

        int[] hsv = Gradient.hsv(gradientColours.get(gradientSlot));
        card.addView(slider(Text.GRADIENT_HUE, hsv[0], 359, value -> setSlot(value, -1, -1)));
        card.addView(slider(Text.GRADIENT_SAT, hsv[1], 100, value -> setSlot(-1, value, -1)));
        card.addView(slider(Text.GRADIENT_VALUE, hsv[2], 100, value -> setSlot(-1, -1, value)));

        card.addView(line());
        if (gradientColours.size() < Gradient.MOST) {
            card.addView(actionRow("palette", Text.GRADIENT_ADD, null, () -> {
                gradientColours.add(gradientColours.get(gradientColours.size() - 1));
                gradientSlot = gradientColours.size() - 1;
                rebuild();
            }));
        }
        if (gradientColours.size() > Gradient.FEWEST) {
            card.addView(actionRow("block", Text.GRADIENT_DROP_ONE, null, () -> {
                gradientColours.remove(gradientSlot);
                gradientSlot = 0;
                rebuild();
            }));
        }
        card.addView(line());
        card.addView(actionRow("star", Text.GRADIENT_SAVE, null, () -> Gradient.save(
                gradientColours, (ok, trouble) -> {
                    if (needsProof(ok, trouble)) return;
                    Popup.show(this, Text.GRADIENT,
                            ok ? Text.GRADIENT_SAVED : trouble, Text.TEST_CLOSE);
                })));
        card.addView(actionRow("visibility_off", Text.GRADIENT_OFF, null, () -> Gradient.drop(
                (ok, trouble) -> {
                    if (needsProof(ok, trouble)) return;
                    Popup.show(this, Text.GRADIENT,
                            ok ? Text.GRADIENT_SAVED : trouble, Text.TEST_CLOSE);
                })));
        card.addView(caption(Text.GRADIENT_NOTE));
        return card;
    }

    private void setSlot(int hue, int saturation, int value) {
        int[] hsv = Gradient.hsv(gradientColours.get(gradientSlot));
        if (hue >= 0) hsv[0] = hue;
        if (saturation >= 0) hsv[1] = saturation;
        if (value >= 0) hsv[2] = value;
        gradientColours.set(gradientSlot, Gradient.from(hsv[0], hsv[1], hsv[2]));
        rebuild();
    }

    private View gradientPreview() {
        int[] colours = new int[gradientColours.size()];
        for (int i = 0; i < colours.length; i++) colours[i] = gradientColours.get(i);
        View bar = new View(this);
        android.graphics.drawable.GradientDrawable paint =
                new Design.ColourSample(
                        android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                        colours);
        paint.setCornerRadius(dp(10));
        bar.setBackground(paint);
        LinearLayout holder = new LinearLayout(this);
        holder.setPadding(dp(16), dp(14), dp(16), dp(6));
        holder.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(28)));
        return holder;
    }

    private View gradientDots() {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setPadding(dp(16), dp(6), dp(16), dp(10));
        for (int i = 0; i < gradientColours.size(); i++) {
            final int at = i;
            Dot dot = new Dot(this, gradientColours.get(i), i == gradientSlot);
            dot.setOnClickListener(v -> {
                gradientSlot = at;
                rebuild();
            });
            line.addView(dot, new LinearLayout.LayoutParams(dp(36), dp(36), 1f));
        }
        return line;
    }

    private static String fpsName(String which) {
        if (which == null || which.length() == 0) return Text.FPS_AUTO;
        return String.format(Text.FPS_LOCKED, which + " FPS");
    }

    private static String named(Badges.Badge badge) {
        if (badge.title != null && badge.title.length() > 0) return badge.title;
        return badge.text;
    }


    private static String spaced(String digits) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && i % 4 == 0) out.append(' ');
            out.append(digits.charAt(i));
        }
        return out.toString();
    }



    private static final int DONATE_PAGE = 99;

    private void showDonations() {
        return;
    }


    private void donatePage() {
        LinearLayout what = card();
        what.addView(quiet(Text.DONATE_BANNER_TEXT));
        column.addView(wrap(what));

        column.addView(section(Text.DONATE_STEP_PAY));
        LinearLayout pay = card();
        pay.addView(linkRow("star", Text.YOOMONEY, Text.YOOMONEY_NOTE, YOOMONEY));
        pay.addView(line());
        pay.addView(linkRow("public", Text.BOOSTY, Text.BOOSTY_NOTE, BOOSTY));
        pay.addView(line());
        pay.addView(linkRow("public", Text.FUNPAY, Text.FUNPAY_NOTE, FUNPAY));
        pay.addView(line());
        pay.addView(linkRow("star", Text.STARS, Text.STARS_NOTE, STARS));
        column.addView(wrap(pay));

        column.addView(section(Text.DONATE_STEP_ID));
        LinearLayout mine = card();
        mine.addView(idRow(Text.ACCOUNT_ID, Account.id()));
        mine.addView(quiet(Text.DONATE_ID_NOTE));
        column.addView(wrap(mine));

        column.addView(section(Text.DONATE_STEP_WRITE));
        LinearLayout write = card();
        write.addView(actionRow("chat", Text.DONATE_BANNER_WRITE, Text.DONATE_WRITE_NOTE,
                () -> open(CHANNEL_WRITE)));
        write.addView(quiet(Text.DONATE_AFTER));
        column.addView(wrap(write));
    }

    private void rebuildAfterSwitchAnimation() {
        if (rebuildQueued) column.removeCallbacks(delayedRebuild);
        else rebuildQueued = true;
        column.postDelayed(delayedRebuild, M3Switch.ANIMATION_DURATION_MS);
    }



    private View switchRow() {
        LinearLayout row = row();
        final View lead = icon("language", true, 40);
        if (lead instanceof Expressive.Badge) ((Expressive.Badge) lead).set(AirTok.isEnabled(), false);
        row.addView(lead);
        row.addView(label(Text.CHANGE_REGION), grow());

        final M3Switch toggle = new M3Switch(this);
        toggle.colours(Accent.colour(), skin.muted(), skin.card);
        toggle.setChecked(AirTok.isEnabled());
        toggle.setOnChanged(checked -> {
            if (lead instanceof Expressive.Badge) ((Expressive.Badge) lead).set(checked, true);
            AirTok.setEnabled(checked);
            markChanged();
            rebuildAfterSwitchAnimation();
        });
        row.addView(toggle);

        row.setOnClickListener(v -> {
            toggle.performClick();
        });
        return sized(row, 52);
    }

    private View countryHead() {
        String[] current = AirTok.current();
        LinearLayout row = row();
        row.addView(flag(current[AirTok.ISO]));
        row.setAlpha(AirTok.isEnabled() ? 1f : 0.4f);

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(AirTok.name(current)));
        text.addView(detail(current[AirTok.CARRIER] + "  ·  " + current[AirTok.MCCMNC]
                + "  ·  " + current[AirTok.ISO].toUpperCase(Locale.US)));
        row.addView(text, grow());
        row.addView(away());


        if (AirTok.isEnabled()) row.setOnClickListener(v -> showCountries());
        return sized(row, 60);
    }

    private View countryRow(final String[] country) {
        boolean selected = country[AirTok.ISO].equals(AirTok.iso());
        LinearLayout row = row();
        row.addView(flag(country[AirTok.ISO]));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(AirTok.name(country)));
        text.addView(detail(country[AirTok.CARRIER] + "  ·  " + country[AirTok.MCCMNC]
                + "  ·  " + country[AirTok.ISO].toUpperCase(Locale.US)));
        row.addView(text, grow());

        if (selected) row.addView(new Check(this, Accent.colour()));

        row.setOnClickListener(v -> {
            AirTok.setIso(country[AirTok.ISO]);
            markChanged();
            goBack();
        });
        return sized(row, 60);
    }


    private View flag(String iso) {
        TextView view = new TextView(this);
        StringBuilder out = new StringBuilder();
        String code = iso.toUpperCase(Locale.US);

        if ("XX".equals(code)) { out.append("\uD83D\uDCDA"); code = ""; }

        if ("XA".equals(code)) { out.append("\uD83D\uDD31"); code = ""; }
        if ("XP".equals(code)) { out.append("\uD83C\uDFF4\u200D\u2620\uFE0F"); code = ""; }
        for (int i = 0; i < code.length() && i < 2; i++) {
            int letter = code.charAt(i) - 'A';
            if (letter < 0 || letter > 25) return spacer();
            out.appendCodePoint(0x1F1E6 + letter);
        }
        view.setText(out.toString());


        android.graphics.Typeface flags = Fonts.twemoji(this);
        if (flags != null) view.setTypeface(flags);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        view.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams size =
                new LinearLayout.LayoutParams(dp(34), ViewGroup.LayoutParams.WRAP_CONTENT);
        size.rightMargin = dp(10);
        view.setLayoutParams(size);
        return view;
    }

    private View spacer() {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(34), 1));
        return view;
    }

    private View disclosure(boolean expanded, String key) {
        renderDisclosureKey=expanded ? key : null;
        Chevron arrow=new Chevron(this,skin.muted(),expanded,key.equals(disclosureKey));
        disclosureArrows.put(key,arrow);return arrow;
    }

    private View accentHead() {
        LinearLayout row = row();
        row.addView(icon("palette"));
        row.addView(label(Text.ACCENT_COLOUR), grow());
        row.addView(new Dot(this, Accent.colour(), true));

        row.addView(disclosure(accentOpen,"accentOpen"));

        row.setOnClickListener(v -> {
            disclosureKey = "accentOpen";
            accentOpen = !accentOpen;
            rebuildAfterReveal(accentOpen);
        });
        return sized(row, 52);
    }

    private View themeColoursHead() {
        LinearLayout row = row();
        row.addView(icon("tune"));
        row.addView(label(Text.THEME_COLOURS), grow());
        row.addView(disclosure(themeColoursOpen,"themeColoursOpen"));
        row.setContentDescription(Text.THEME_COLOURS);
        row.setFocusable(true);
        row.setOnClickListener(v -> {
            disclosureKey = "themeColoursOpen";
            themeColoursOpen = !themeColoursOpen;
            rebuildAfterReveal(themeColoursOpen);
        });
        return sized(row, 52);
    }




    private int badgesTab;


    private View badgeTabs() {
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setPadding(edge(), 0, edge(), 0);
        String[] names = {Local.of("Мои значки"), Local.of("Получить")};
        for (int i = 0; i < names.length; i++) {
            final int which = i;
            boolean on = badgesTab == i;
            TextView tab = new TextView(this);
            tab.setText(names[i]);
            M3Type.line(tab, 14, 0.1f);
            tab.setTextColor(on ? skin.text : skin.muted());
            tab.setTypeface(android.graphics.Typeface.create(tab.getTypeface(), on ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(dp(18), 0, dp(18), 0);
            GradientDrawable back = new GradientDrawable();
            float round = dp(14);
            back.setCornerRadii(new float[] {round, round, round, round, 0, 0, 0, 0});
            back.setColor(on ? skin.card : Expressive.mix(skin.page, skin.card, 0.45f));
            tab.setBackground(back);
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));
            size.rightMargin = dp(4);
            strip.addView(tab, size);
            tab.setOnClickListener(v -> {
                if (badgesTab == which) return;
                badgesTab = which;
                if (which == 1) Mine.ask(this::rebuild);
                rebuild();
            });
            Motion.press(tab);
        }
        return strip;
    }


    private void badgesToGet() {
        if (!Proof.proved()) {
            LinearLayout prove = card();
            prove.addView(proveRow());
            column.addView(wrap(prove));
        }
        column.addView(wrap(getCard(Tester.SUPPORTER,
                Local.of("Поддержать разработку MargyT. Вместе со значком даётся золотая рамка на аватарку и градиент ника."),
                Local.of("Поддержать"), this::showDonations)));
        column.addView(wrap(getCard("margychat",
                Local.of("Зарегистрироваться в AirTok Chat, худшей соцсети на свете, и заработать там 30 💎 за посты. Потом взять код в настройках аккаунта AirTok Chat и ввести его здесь. Только для личных аккаунтов, не каналов и не ботов."),
                Local.of("Ввести код"), this::askChatCode)));
    }

    private LinearLayout getCard(String id, String how, String act, Runnable action) {
        LinearLayout card = card();
        Badges.Badge badge = Badges.byId(id);
        boolean have = Mine.holds(id);
        LinearLayout row = row();
        row.setPadding(edge(), dp(14), edge(), dp(6));
        row.addView(badgeDot(badge));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.addView(label(badge == null ? id : named(badge)));
        if (have) words.addView(detail(Local.of("Уже у тебя")));
        row.addView(words, grow());
        card.addView(row);
        card.addView(quiet(how));
        if (!have) card.addView(button(act, action));
        if ("margychat".equals(id)) {
            card.addView(line());
            card.addView(actionRow("chat", Local.of("Открыть AirTok Chat"), "chat.narez.xyz", () -> open("https://chat.narez.xyz/settings/account")));
        }
        return card;
    }


    private void askChatCode() {
        if (!Proof.proved()) {
            proveAccount();
            return;
        }
        final Panel panel = Panel.with(this, skin, Local.of("Код из AirTok Chat"));
        panel.text(Local.of("Его показывает AirTok Chat в настройках, в разделе «Аккаунт»: «Привязать MargyT». Код живёт 15 минут."));
        final android.widget.EditText code = panel.field("ABC123");
        code.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        code.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(6), new android.text.InputFilter.AllCaps()});
        panel.primaryKeepOpen(Local.of("Получить значок"), new Panel.Click() {
            @Override public void on() {
                String typed = code.getText().toString().trim();
                if (typed.length() != 6) {
                    code.setError(Local.of("В коде 6 букв и цифр"));
                    return;
                }
                Mine.takeChatBadge(typed, (ok, trouble) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (ok) {
                        panel.close();
                        Screen.say(Local.of("Значок «Выжил в AirTok Chat» твой"));
                        ordering = null;
                        badgesTab = 0;
                        rebuild();
                        return;
                    }
                    if (Mine.PROVE.equals(trouble)) {
                        panel.close();
                        proveAccount();
                        return;
                    }
                    code.setError(trouble.length() > 0 ? trouble : Local.of("Не получилось, попробуй ещё раз"));
                });
            }
        });
        panel.quiet(Local.of("Отмена"), null).show();
    }

    private View mineHead() {
        LinearLayout row = row();
        row.addView(icon("favorite_border"));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(Text.MINE));
        text.addView(detail(Text.MINE_NOTE));
        row.addView(text, grow());

        row.addView(disclosure(mineOpen,"mineOpen"));

        row.setOnClickListener(v -> {
            disclosureKey = "mineOpen";
            mineOpen = !mineOpen;
            if (mineOpen) {
                ordering = null;
                Mine.ask(this::rebuild);
            }
            rebuildAfterReveal(mineOpen);
        });
        return sized(row, 60);
    }

    private View mineRows() {
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);




        if (Mine.tookFresh() && !rearranged) ordering = null;
        if (ordering == null) ordering = Mine.held();
        rows.addView(proveRow());
        if (ordering.isEmpty()) {
            rows.addView(quiet(Mine.everAsked() ? Text.MINE_NONE : Text.MINE_WAIT));
            return rows;
        }

        for (int i = 0; i < ordering.size(); i++) {
            final int at = i;
            final Mine.Held one = ordering.get(i);
            if (i > 0) rows.addView(line());

            LinearLayout row = row();
            row.setPadding(edge(), 0, edge(), 0);

            Badges.Badge badge = Badges.byId(one.id);
            row.addView(badgeDot(badge));


            row.addView(label(badge == null ? one.id : named(badge)), grow());



            row.addView(mover(true, at > 0, () -> {
                java.util.Collections.swap(ordering, at, at - 1);
                rearranged = true;
                rebuild();
            }));
            row.addView(mover(false, at < ordering.size() - 1, () -> {
                java.util.Collections.swap(ordering, at, at + 1);
                rearranged = true;
                rebuild();
            }));

            M3Switch toggle = new M3Switch(this);
            toggle.colours(Accent.colour(), skin.muted(), skin.card);
            toggle.setChecked(one.shown, false);
            toggle.setOnChanged(checked -> {
                one.shown = checked;
                rearranged = true;
            });
            LinearLayout.LayoutParams size =
                    new LinearLayout.LayoutParams(dp(52), dp(32));
            size.leftMargin = dp(10);
            row.addView(toggle, size);

            rows.addView(sized(row, 60));
        }

        rows.addView(button(Text.MINE_SAVE, () -> Mine.save(ordering,
                (ok, trouble) -> {
                    if (needsProof(ok, trouble)) return;
                    Screen.say(ok ? Text.MINE_SAVED : Text.MINE_TOO_OFTEN);
                    if (ok) {
                        rearranged = false;
                        ordering = null;
                    }
                    rebuild();
                })));
        return rows;
    }


    private View button(String title, final Runnable action) {
        TextView view = new TextView(this);
        view.setText(title);
        view.setTextColor(onAccent());
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setGravity(Gravity.CENTER);
        view.setOnClickListener(v -> action.run());
        LinearLayout holder = new LinearLayout(this);
        if (Design.frosted()) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            view.setPadding(0, dp(15), 0, dp(15));

            view.setBackground(Expressive.tile(this, Accent.colour(), 0x33FFFFFF, dp(28), dp(28), dp(12)));
            holder.setPadding(dp(16), dp(12), dp(16), dp(16));
            holder.addView(view, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return holder;
        }

        M3Type.line(view, 14, 0.1f);
        view.setPadding(dp(16), 0, dp(16), 0);
        view.setBackground(Expressive.tile(this, Accent.colour(), 0x33FFFFFF, dp(20), dp(20), dp(8)));
        holder.setPadding(edge(), dp(12), edge(), dp(16));
        holder.setOnClickListener(v -> action.run());
        holder.addView(view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));
        return holder;
    }

    private View mover(boolean up, boolean can, final Runnable action) {
        android.widget.FrameLayout target=new android.widget.FrameLayout(this);
        Chevron arrow=new Chevron(this,can?skin.text:skin.muted(),up,false);
        android.widget.FrameLayout.LayoutParams image=new android.widget.FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER);
        target.addView(arrow,image);
        target.setLayoutParams(new LinearLayout.LayoutParams(dp(48),dp(48)));
        target.setContentDescription(up ? Local.of("Выше") : Local.of("Ниже"));
        target.setEnabled(can);
        if (can) {Motion.press(target);target.setOnClickListener(v -> action.run());}
        return target;
    }

    private View badgeDot(Badges.Badge badge) {
        android.widget.ImageView view = new android.widget.ImageView(this);
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(26), dp(26));
        size.rightMargin = dp(12);
        view.setLayoutParams(size);
        if (badge == null) return view;
        android.graphics.Bitmap picture = badge.image.length() == 0
                ? null : Badges.picture(this, badge.image);
        if (picture != null) {
            view.setImageBitmap(picture);
        } else {


            view.setImageBitmap(Badges.note());
        }
        if (badge.colour != 0) {
            view.setColorFilter(badge.colour, android.graphics.PorterDuff.Mode.SRC_IN);
        }
        return view;
    }


    private View freeBanner() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(Math.max(skin.radius, dp(18)));
        background.setColor(blend(0xFF40E0D0, skin.card, 0.82f));
        card.setBackground(background);

        TextView head = new TextView(this);
        head.setText(Text.FREE_BADGE);
        head.setTextColor(skin.text);
        head.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(head);

        TextView body = new TextView(this);
        body.setText(Text.FREE_BADGE_TEXT);
        body.setTextColor(skin.text);
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        body.setPadding(0, dp(4), 0, 0);
        card.addView(body);

        TextView take = new TextView(this);
        take.setText(Text.FREE_BADGE_TAKE);
        take.setTextColor(0xFF10221F);
        take.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        take.setTypeface(Typeface.DEFAULT_BOLD);
        take.setGravity(Gravity.CENTER);
        take.setPadding(0, dp(11), 0, dp(11));
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(0xFF40E0D0);
        pill.setCornerRadius(dp(14));
        take.setBackground(pill);
        LinearLayout.LayoutParams below = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        below.topMargin = dp(14);
        take.setOnClickListener(v -> {
            if (!Mine.everAsked()) {
                Mine.ask(() -> Mine.takeFree(this::tookFree));
                return;
            }
            Mine.takeFree(this::tookFree);
        });
        card.addView(take, below);

        LinearLayout holder = new LinearLayout(this);
        holder.setPadding(dp(20), dp(14), dp(20), dp(2));
        holder.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return holder;
    }



    private android.content.SharedPreferences animPrefs() {
        return getSharedPreferences("airtok_text_animation", MODE_PRIVATE);
    }

    private static String ta(String ru) {
        return com.fasdeq.airtok.textanim.Strings.t(ru);
    }


    private TextView subhead(String words) {
        TextView view = new TextView(this);
        view.setText(words);
        view.setTextColor(accentInk());
        M3Type.titleSmall(view);
        view.setPadding(dp(32), dp(18), dp(24), dp(8));
        return view;
    }

    private View animToggle(String picture, String key, String title, boolean fallback) {
        return toggleRow(picture, title, animPrefs().getBoolean(key, fallback),
                on -> animPrefs().edit().putBoolean(key, on).apply());
    }

    private View animSlider(String picture, String key, String title, int least, int most, int fallback, String unit) {
        int now = Math.max(least, Math.min(most, animPrefs().getInt(key, fallback)));
        return slider(picture, title, now - least, most - least, least, unit,
                value -> animPrefs().edit().putInt(key, value + least).apply());
    }

    private View animChoice(String picture, String key, String title, final String[] names, int fallback) {
        int now = Math.max(0, Math.min(names.length - 1, animPrefs().getInt(key, fallback)));
        return actionRow(picture, title, names[now], () -> new ModDialog.Builder(this).setItems(names, (dialog, index) -> {
            animPrefs().edit().putInt(key, index).apply();
            rebuild();
        }).show());
    }


    private void textAnimationCards() {
        column.addView(subhead(ta("Свечение текста (Glow)")));
        LinearLayout glow = card();
        glow.addView(animToggle("flare", "glow", ta("Свечение недавних букв (1.5 сек)"), false));
        glow.addView(animChoice("palette", "glow_mode", ta("Цвет / Режим свечения"), new String[] {
                ta("Радужный перелив (Rainbow)"), ta("Розовый TikTok (#FE2C55)"), ta("Бирюзовый TikTok (#25F4EE)"),
                Local.of("Цвет акцента"), ta("Золотой неоновый"), ta("Белый неоновый"), ta("Зеленый неоновый"),
                ta("Фиолетовый неоновый")}, 0));
        glow.addView(animToggle("format_color_fill", "gradient_text", ta("Градиентный текст"), false));
        column.addView(wrap(glow));

        column.addView(subhead(ta("Появление букв")));
        LinearLayout come = card();
        come.addView(animToggle("expand_more", "slide", ta("Въезд букв сверху"), true));
        come.addView(animToggle("add", "scale", ta("Плавное появление (масштаб)"), true));
        come.addView(animSlider("speed", "duration", ta("Скорость падения буквы (мс)"), 80, 500, 260, ""));
        come.addView(animSlider("animation", "tilt", ta("Наклон при появлении (°)"), 0, 40, 0, "°"));
        come.addView(animToggle("visibility_off", "blur", ta("Размытие при появлении"), true));
        come.addView(animSlider("tune", "blur_dp", ta("Сила размытия"), 1, 30, 6, ""));
        come.addView(animChoice("timeline", "easing", ta("Кривая движения"), new String[] {ta("Мягкая"), ta("Линейная"),
                ta("Кубическая"), ta("С перелётом"), ta("Пружина"), ta("Отскок")}, 4));
        column.addView(wrap(come));

        column.addView(subhead(ta("Курсор")));
        LinearLayout caret = card();
        caret.addView(animToggle("edit", "caret", ta("Плавающий курсор"), true));
        caret.addView(animToggle("animation", "liquid", ta("Жидкий режим (растяжение и прыжки)"), true));
        caret.addView(animSlider("speed", "caret_speed", ta("Скорость курсора"), 5, 100, 25, ""));
        caret.addView(animToggle("visibility", "blink", ta("Мягкое мигание курсора"), true));
        column.addView(wrap(caret));

        column.addView(subhead(ta("Искры и стирание символов")));
        LinearLayout sparks = card();
        sparks.addView(animToggle("flare", "particles", ta("Искры при стирании"), true));
        sparks.addView(animChoice("star", "style", ta("Стиль искр"), new String[] {ta("Пыль / Искры"), ta("Снежинки"),
                ta("Лепестки сакуры"), ta("Буквы"), ta("Падение с физикой"), ta("Песок по форме буквы"), ta("Дым"),
                ta("Пузыри"), ta("Штрихи-искры")}, 0));
        sparks.addView(animChoice("palette", "spark_palette", ta("Цвета искр"), new String[] {
                ta("TikTok (Розовый + Бирюзовый)"), ta("Неон (Фиолетовый + Синий)"), ta("Огонь (Красный + Золотой)"),
                ta("Радужные искры"), ta("Цвет текста")}, 0));
        sparks.addView(animSlider("tune", "amount", ta("Количество искр"), 1, 12, 6, ""));
        sparks.addView(animToggle("restore", "ghosts", ta("Плавное исчезновение буквы"), true));
        sparks.addView(animToggle("bolt", "haptic", ta("Виброотклик при удалении"), false));
        column.addView(wrap(sparks));

        column.addView(subhead(ta("Искры при отправке")));
        LinearLayout send = card();
        send.addView(animToggle("upload", "send_particles", ta("Искры при отправке"), true));
        send.addView(animToggle("check", "send_guess", ta("Угадывать отправку по очистке поля"), true));
        send.addView(animChoice("flare", "send_preset", ta("Эффект отправки"), com.fasdeq.airtok.textanim.Particles.sendPresetNames(true), 1));
        send.addView(animChoice("palette", "send_palette", ta("Цвета отправки"), com.fasdeq.airtok.textanim.Particles.sendPaletteNames(true), 0));
        send.addView(animChoice("text_fields", "send_shape", ta("Во что превращаются буквы"), com.fasdeq.airtok.textanim.Particles.sendShapeNames(true), 0));
        send.addView(animSlider("tune", "send_amount", ta("Частиц с буквы при отправке"), 1, 8, 4, ""));
        send.addView(animSlider("speed", "send_speed", ta("Скорость частиц отправки (%)"), 40, 300, 100, "%"));
        send.addView(animSlider("animation", "send_swirl", ta("Закрутка вихря (%)"), 0, 250, 100, "%"));
        send.addView(animChoice("swap_horiz", "send_dir", ta("Направление вихря"), new String[] {ta("По часовой"),
                ta("Против часовой"), ta("Вперемешку")}, 0));
        column.addView(wrap(send));
    }


    private static int seedScroll;


    private View seedPicker() {
        android.widget.HorizontalScrollView scroll = new android.widget.HorizontalScrollView(this) {
            @Override
            protected void onScrollChanged(int l, int t, int oldl, int oldt) {
                super.onScrollChanged(l, t, oldl, oldt);
                seedScroll = l;
            }
        };
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout circles = new LinearLayout(this);
        circles.setPadding(dp(12), dp(10), dp(12), dp(10));
        final boolean dark = Themes.isDark();
        final int chosen = MaterialSeeds.seed();
        for (final int seed : MaterialSeeds.SEEDS) {
            final int[] colours = MaterialSeeds.swatch(seed, dark);
            View circle = new View(this) {
                final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                final android.graphics.RectF box = new android.graphics.RectF();
                @Override
                protected void onDraw(android.graphics.Canvas canvas) {
                    float cx = getWidth() / 2f, cy = getHeight() / 2f;
                    boolean on = chosen == seed;
                    float r = dp(on ? 20 : 24);
                    box.set(cx - r, cy - r, cx + r, cy + r);
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                    paint.setColor(colours[0]); canvas.drawArc(box, 180, 180, true, paint);
                    paint.setColor(colours[1]); canvas.drawArc(box, 90, 90, true, paint);
                    paint.setColor(colours[2]); canvas.drawArc(box, 0, 90, true, paint);
                    if (on) {
                        paint.setStyle(android.graphics.Paint.Style.STROKE);
                        paint.setStrokeWidth(dp(2));
                        paint.setColor(skin.text);
                        canvas.drawCircle(cx, cy, dp(25), paint);
                    }
                    if (seed == 0) {

                        paint.setStyle(android.graphics.Paint.Style.FILL);
                        paint.setColor(skin.card);
                        canvas.drawCircle(cx, cy, dp(5), paint);
                    }
                }
            };
            circle.setContentDescription(seed == 0 ? Text.ACCENT_WALLPAPER : String.format("#%06X", seed & 0xFFFFFF));
            circle.setOnClickListener(v -> {
                if (MaterialSeeds.seed() == seed) return;
                MaterialSeeds.choose(seed);
                markChanged();
                rebuild();
            });
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(56), dp(56));
            size.leftMargin = size.rightMargin = dp(4);
            circles.addView(circle, size);
        }
        scroll.addView(circles);
        scroll.post(() -> scroll.scrollTo(seedScroll, 0));
        return scroll;
    }

    private static int blend(int colour, int into, float how) {
        int red = (int) (((colour >> 16) & 0xFF) * (1 - how) + ((into >> 16) & 0xFF) * how);
        int green = (int) (((colour >> 8) & 0xFF) * (1 - how) + ((into >> 8) & 0xFF) * how);
        int blue = (int) ((colour & 0xFF) * (1 - how) + (into & 0xFF) * how);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }



    private View wallpaperRow() {
        final int colour = Accent.fromWallpaper();
        LinearLayout row = row();
        row.addView(icon("wallpaper"));
        row.addView(label(Text.ACCENT_WALLPAPER), grow());
        row.addView(new Dot(this, colour, Accent.isMaterial()));
        row.setOnClickListener(v -> {
            Accent.setMaterial(true);
            markChanged();
            rebuild();
        });
        return sized(row, 52);
    }


    private View appear(View view) {
        boolean animated=!revealAnimationUsed && Motion.enabled(this);
        if(animated)revealAnimationUsed=true;
        DisclosureBody body=new DisclosureBody(this,view,animated);
        if(renderDisclosureKey!=null) {
            java.util.List<DisclosureBody> list=disclosureBodies.get(renderDisclosureKey);
            if(list==null){list=new java.util.ArrayList<>();disclosureBodies.put(renderDisclosureKey,list);}
            list.add(body);
        }
        return body;
    }

    private void rebuildAfterReveal(boolean expanded) {
        if(collapsing)return;
        java.util.List<DisclosureBody> bodies=disclosureBodies.get(disclosureKey);
        if(!expanded && bodies!=null && !bodies.isEmpty() && Motion.enabled(this)) {
            collapsing=true;
            Chevron arrow=disclosureArrows.get(disclosureKey);
            if(arrow!=null)arrow.animate().rotation(0f).setDuration(180L).start();
            final int[] remaining={bodies.size()};
            for(DisclosureBody body:new java.util.ArrayList<>(bodies))body.collapse(()->{
                if(--remaining[0]==0){collapsing=false;revealAnimationRequested=false;disclosureKey=null;rebuild();}
            });
            return;
        }
        revealAnimationRequested=expanded;rebuild();disclosureKey=null;
    }







    private static final int CREATE_BACKUP = 0x4D42;
    private static final int PICK_BACKUP = 0x4D49;

    private void createBackup() {
        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "airtok-settings.json");
            startActivityForResult(intent, CREATE_BACKUP);
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }

    private void pickBackup() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            startActivityForResult(intent, PICK_BACKUP);
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }

    private void reviewBackup(final Uri source) {
        Screen.progress(Text.BACKUP_READING, 0);
        Net.away("backup", () -> {
            final Backup.Review review = Backup.review(this, source);
            if (review.fits()) Screen.progressDone(); else Screen.progressGone();
            runOnUiThread(() -> {
                if (!review.fits()) {
                    Screen.say(Text.BACKUP_FAILED);
                    return;
                }
                new ModDialog.Builder(this, skin)
                        .setTitle(Text.BACKUP_IMPORT)
                        .setMessage(Text.backupPreview(review.count()))
                        .setNegativeButton(Text.CANCEL, null)
                        .setPositiveButton(Text.BACKUP_APPLY, (dialog, which) -> {
                            if (Backup.apply(this, review)) {
                                markChanged();
                                rebuild();
                                Screen.say(Text.BACKUP_DONE);
                            } else Screen.say(Text.BACKUP_FAILED);
                        })
                        .show();
            });
        });
    }

    private void pickResetGroup() {
        final String[] groups = {
                Text.RESET_APPEARANCE, Text.RESET_PLAYER, Text.RESET_FEED, Text.RESET_ALL
        };
        try {
            new ModDialog.Builder(this, skin)
                    .setTitle(Text.BACKUP_RESET)
                    .setMessage(Text.BACKUP_RESET_ASK)
                    .setItems(groups, (dialog, chosen) -> askToReset(chosen + 1))
                    .setNegativeButton(Text.CANCEL, null)
                    .show();
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }

    private void askToReset(final int group) {
        new ModDialog.Builder(this, skin)
                .setTitle(Text.BACKUP_RESET)
                .setMessage(Text.BACKUP_RESET_ASK)
                .setNegativeButton(Text.CANCEL, null)
                .setPositiveButton(Text.BACKUP_APPLY, (dialog, which) -> {
                    Backup.reset(this, group);
                    markChanged();
                    rebuild();
                })
                .show();
    }


    private interface Chosen {
        void at(int value);
    }

    private View slider(String title, int now, int most, final Chosen chosen) {
        return slider(title, now, most, 0, "", chosen);
    }


    private View slider(String title, int now, int most, final int shift, final String unit, final Chosen chosen) {
        return slider("tune", title, now, most, shift, unit, chosen);
    }


    private View slider(String picture, String title, int now, int most, final int shift, final String unit, final Chosen chosen) {
        return slider(picture,title,now,most,shift,unit,chosen,false);
    }
    private View liveSlider(String picture,String title,int now,int most,int shift,String unit,Chosen chosen){
        return slider(picture,title,now,most,shift,unit,chosen,true);
    }
    private View slider(String picture, String title, int now, int most, final int shift, final String unit, final Chosen chosen, final boolean live) {
        return slider(picture, title, now, most, shift, unit, chosen, live, null);
    }


    private View slider(String picture, String title, int now, int most, final int shift, final String unit,
                        final Chosen chosen, final boolean live, final Chosen moving) {
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(16), dp(10), dp(20), dp(10));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(icon(picture));
        head.addView(label(title), new LinearLayout.LayoutParams(0,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView number = new TextView(this);
        number.setTextColor(accentInk());
        number.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
        number.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        number.setText((Math.min(now, most) + shift) + unit);
        head.addView(number);
        rows.addView(head);

        android.widget.SeekBar bar = new android.widget.SeekBar(this);
        bar.setMax(most);
        bar.setProgress(Math.min(now, most));
        Expressive.slider(bar, Accent.colour(), Expressive.mix(skin.card, skin.text, 0.2f));
        bar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seek, int value, boolean human) {
                number.setText((value + shift) + unit);
                if(live && human)chosen.at(value);
                if(moving != null && human)moving.at(value);
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seek) {
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seek) {
                if(!live)chosen.at(seek.getProgress());
            }
        });
        LinearLayout.LayoutParams barSize = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        barSize.leftMargin = dp(56);
        rows.addView(bar, barSize);
        return rows;
    }




    private static final int PICK_FONT = 0x4D46;
    private static final int PICK_EMOJI = 0x4D45;
    private static final int PICK_PROFILE_BADGE = 0x5450;




    private GradientDrawable colourPreview(int colour) {
        GradientDrawable preview = new Design.ColourSample();
        preview.setColor(colour);
        preview.setCornerRadius(dp(12));
        preview.setStroke(dp(1), skin.muted());
        return preview;
    }

    private int contrastingText(int colour) {
        float luminance = (0.299f * Color.red(colour) + 0.587f * Color.green(colour)
                + 0.114f * Color.blue(colour)) / 255f;
        return luminance > 0.55f ? Color.BLACK : Color.WHITE;
    }




    private View fontHead() {
        LinearLayout row = row();
        row.addView(icon("text_fields"));
        row.addView(label(Text.FONT), grow());

        TextView now = detail(fontName(Fonts.name()));
        now.setTextColor(skin.text);
        row.addView(now);

        row.addView(disclosure(fontOpen,"fontOpen"));

        row.setOnClickListener(v -> {
            disclosureKey = "fontOpen";
            fontOpen = !fontOpen;
            rebuildAfterReveal(fontOpen);
        });
        return sized(row, 52);
    }

    private static String fontName(String which) {
        if (Fonts.ROUNDED.equals(which)) return Text.FONT_ROUNDED;
        if (Fonts.SANS.equals(which)) return Text.FONT_SANS;
        if (Fonts.SANS_LIGHT.equals(which)) return Text.FONT_SANS_LIGHT;
        if (Fonts.SANS_CONDENSED.equals(which)) return Text.FONT_SANS_CONDENSED;
        if (Fonts.SERIF.equals(which)) return Text.FONT_SERIF;
        if (Fonts.MONOSPACE.equals(which)) return Text.FONT_MONOSPACE;
        if (Fonts.CURSIVE.equals(which)) return Text.FONT_CURSIVE;
        if (Fonts.FILE.equals(which)) return Text.FONT_FILE;
        return Text.FONT_SYSTEM;
    }

    private View fontChoices() {
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);

        String now = Fonts.name();
        for (int i = 0; i < Fonts.PRESETS.length; i++) {
            final String which = Fonts.PRESETS[i];
            if (i > 0) rows.addView(line());
            rows.addView(pickRow(fontName(which), which.equals(now),
                    Fonts.SYSTEM.equals(which) ? null : which, () -> {
                        Fonts.choose(which);
                        markChanged();
                        rebuild();
                    }));
        }
        rows.addView(line());
        rows.addView(actionRow("download", Text.FONT_PICK,
                Fonts.FILE.equals(now) ? Text.FONT_FILE : null, this::pickFont));

        rows.addView(line());
        rows.addView(section(Text.EMOJI));
        String emoji = Fonts.emoji();
        List<EmojiPackManager.Pack> packs = EmojiPackManager.packs(this);
        boolean firstPack = true;
        for (EmojiPackManager.Pack pack : packs) {
            final String which = pack.id;
            if (Fonts.EMOJI_FILE.equals(which)) continue;
            if (!firstPack) rows.addView(line());
            firstPack = false;
            rows.addView(pickRow(emojiName(pack), which.equals(emoji), null, () -> {
                Fonts.chooseEmoji(this, which);
                markChanged();
                rebuild();
            }));
        }
        rows.addView(line());
        rows.addView(actionRow("download", Text.EMOJI_FILE,
                Fonts.EMOJI_FILE.equals(emoji) ? Text.EMOJI_FILE : null, this::pickEmoji));
        TextView sample = new TextView(this);
        sample.setText(Text.EMOJI_PREVIEW);
        sample.setTextColor(skin.text);
        sample.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
        sample.setGravity(Gravity.CENTER);
        sample.setPadding(dp(16), dp(12), dp(16), dp(12));
        android.graphics.Typeface emojiPreview = Fonts.preview(this, emoji);
        if (emojiPreview != null) sample.setTypeface(emojiPreview);
        rows.addView(sample, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));
        rows.addView(quiet(Text.EMOJI_NOTE));
        return rows;
    }


    private View fontCarousels() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout head = row();
        head.addView(icon("text_fields"));
        head.addView(label(Text.FONT), grow());
        TextView now = detail(fontName(Fonts.name()));
        now.setTextColor(skin.text);
        head.addView(now);
        box.addView(sized(head, 52));
        LinearLayout letters = new LinearLayout(this);
        View chosen = null;
        String font = Fonts.name();
        for (final String which : Fonts.PRESETS) {
            Typeface face = Fonts.SYSTEM.equals(which) ? Typeface.DEFAULT
                    : Fonts.ROUNDED.equals(which) ? Fonts.rounded(this) : Typeface.create(which, Typeface.NORMAL);
            View tile = typeTile(face, "Аа", fontName(which), which.equals(font), () -> {
                Fonts.choose(which);
                markChanged();
                rebuild();
            });
            if (which.equals(font)) chosen = tile;
            letters.addView(tile);
        }
        android.widget.HorizontalScrollView one = carousel(letters);
        box.addView(one);
        centre(one, chosen);
        return box;
    }


    private static final class SampleIcon extends TextView {
        SampleIcon(Context context) { super(context); }
    }

    private View typeTile(Typeface face, String sample, String name, boolean on, final Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(4), dp(2), dp(4), dp(2));
        TextView big = new SampleIcon(this);
        big.setText(sample);
        big.setTextColor(skin.text);
        big.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        if (face != null) big.setTypeface(face);
        big.setGravity(Gravity.CENTER);
        GradientDrawable back = new GradientDrawable();
        back.setCornerRadius(dp(20));
        back.setColor(on ? accentTonal() : Expressive.mix(skin.card, skin.text, 0.06f));
        if (on) back.setStroke(dp(2), Accent.colour());
        big.setBackground(back);
        tile.addView(big, new LinearLayout.LayoutParams(dp(76), dp(76)));
        TextView label = new TextView(this);
        label.setText(name);
        M3Type.line(label, 12, 0.4f);
        label.setTextColor(on ? skin.text : skin.muted());
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setPadding(0, dp(6), 0, 0);
        tile.addView(label, new LinearLayout.LayoutParams(dp(84), ViewGroup.LayoutParams.WRAP_CONTENT));
        tile.setOnClickListener(v -> action.run());
        Motion.press(tile);
        return tile;
    }

    private String emojiName(EmojiPackManager.Pack pack) {
        if (Fonts.SYSTEM.equals(pack.id)) return Text.EMOJI_SYSTEM;
        return pack.name;
    }

    private void pickEmoji() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_EMOJI);
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }


    private View pickRow(String title, boolean chosen, String preview, final Runnable action) {
        LinearLayout row = row();
        row.setPadding(edge(), 0, edge(), 0);

        TextView name = label(title);
        if (preview != null) {
            try {
                name.setTypeface(android.graphics.Typeface.create(
                        preview, android.graphics.Typeface.NORMAL));
            } catch (Throwable ignored) {
            }
        }
        row.addView(name, grow());
        if (chosen) row.addView(new Check(this, Accent.colour()));
        row.setOnClickListener(v -> action.run());
        return sized(row, 52);
    }

    private void pickFont() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_FONT);
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }






    private View labeled(String words, View control) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        TextView name = new TextView(this);
        name.setText(words);
        name.setTextColor(skin.muted());
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        name.setPadding(edge() + dp(4), dp(14), edge(), 0);
        tile.addView(name);
        tile.addView(control);
        return tile;
    }


    private View tilcayoSkins() {
        android.widget.HorizontalScrollView scroll = new android.widget.HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setPadding(edge(), dp(8), edge(), dp(14));
        scroll.addView(strip);
        fillTilcayoSkins(strip);
        return scroll;
    }

    private void fillTilcayoSkins(LinearLayout strip) {
        strip.removeAllViews();
        String worn = TilcayoRun.skin(this);
        boolean supporter = Mine.holds(Tester.SUPPORTER);
        for (int i = 0; i < TilcayoRun.SKINS.length; i++) {
            final String id = TilcayoRun.SKINS[i];
            boolean locked = TilcayoRun.GOLD.equals(id) && !supporter;
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER_HORIZONTAL);
            tile.setPadding(dp(10), dp(10), dp(10), dp(10));
            GradientDrawable back = new GradientDrawable();
            back.setCornerRadius(dp(20));
            back.setColor(Expressive.mix(skin.card, Accent.colour(), id.equals(worn) ? 0.22f : 0.06f));
            if (id.equals(worn)) back.setStroke(dp(2), Accent.colour());
            tile.setBackground(back);
            android.widget.ImageView picture = new android.widget.ImageView(this);
            picture.setImageBitmap(TilcayoRun.picture(this, id));
            picture.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            if (locked) picture.setAlpha(0.35f);
            tile.addView(picture, new LinearLayout.LayoutParams(dp(76), dp(80)));
            TextView name = new TextView(this);
            name.setText(Text.TILCAYO_SKIN_NAMES[i] + (locked ? " 🔒" : ""));
            name.setTextColor(skin.text);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            name.setGravity(Gravity.CENTER);
            tile.addView(name);
            TextView author = new TextView(this);
            String who = TilcayoRun.AUTHORS[i];
            author.setText(who.length() > 0 ? String.format(Text.TILCAYO_SKIN_AUTHOR, who) : " ");
            author.setTextColor(skin.muted());
            author.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            author.setGravity(Gravity.CENTER);
            tile.addView(author);
            tile.setOnClickListener(v -> {
                if (locked) {
                    Toast.makeText(this, Text.TILCAYO_SKIN_LOCKED, Toast.LENGTH_LONG).show();
                    return;
                }
                TilcayoRun.setSkin(this, id);
                fillTilcayoSkins(strip);
            });
            LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(dp(112), LinearLayout.LayoutParams.WRAP_CONTENT);
            place.setMarginEnd(dp(10));
            strip.addView(tile, place);
        }
    }


    private interface Picked { void picked(int index); }


    private View segmented(final String[] labels, int chosen, final Picked picked) {
        LinearLayout box = new LinearLayout(this);
        box.setPadding(edge(), dp(6), edge(), dp(14));
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        final TextView[] parts = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView part = new TextView(this);
            part.setGravity(Gravity.CENTER);
            part.setSingleLine(true);
            part.setEllipsize(android.text.TextUtils.TruncateAt.END);
            M3Type.line(part, 14, 0.1f);
            part.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            part.setPadding(dp(6), 0, dp(6), 0);
            parts[i] = part;
            part.setOnClickListener(v -> {
                paintSegments(parts, labels, index);
                picked.picked(index);
            });
            LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(0, dp(40), 1f);
            if (i > 0) place.leftMargin = -dp(1);
            row.addView(part, place);
        }
        paintSegments(parts, labels, chosen);
        box.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));
        return box;
    }

    private void paintSegments(TextView[] parts, String[] labels, int chosen) {
        for (int i = 0; i < parts.length; i++) {
            boolean on = i == chosen, first = i == 0, last = i == parts.length - 1;
            GradientDrawable back = new GradientDrawable();
            float r = dp(20);
            back.setCornerRadii(new float[] {first ? r : 0, first ? r : 0, last ? r : 0, last ? r : 0,
                    last ? r : 0, last ? r : 0, first ? r : 0, first ? r : 0});
            back.setColor(on ? accentTonal() : 0);
            back.setStroke(dp(1), skin.muted());
            parts[i].setBackground(back);
            parts[i].setTextColor(on ? skin.text : skin.muted());
            parts[i].setText(on ? "\u2713 " + labels[i] : labels[i]);
        }
    }


    private View chips(final String[] labels, int chosen, final Picked picked) {
        LinearLayout strip = new LinearLayout(this);
        final java.util.List<TextView> all = new java.util.ArrayList<TextView>();
        View chosenChip = null;
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView chip = new TextView(this);
            chip.setText(labels[i]);
            M3Type.line(chip, 14, 0.1f);
            chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(16), 0, dp(16), 0);
            all.add(chip);
            if (i == chosen) chosenChip = chip;
            chip.setOnClickListener(v -> {
                paintChips(all, index);
                picked.picked(index);
            });
            LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
            place.rightMargin = dp(8);
            strip.addView(chip, place);
        }
        paintChips(all, chosen);
        android.widget.HorizontalScrollView scroll = carousel(strip);
        strip.setPadding(edge(), dp(4), edge(), dp(14));
        centre(scroll, chosenChip);
        return scroll;
    }

    private void paintChips(java.util.List<TextView> all, int chosen) {
        for (int i = 0; i < all.size(); i++) {
            boolean on = i == chosen;
            GradientDrawable back = new GradientDrawable();
            back.setCornerRadius(dp(8));
            back.setColor(on ? accentTonal() : 0);
            if (!on) back.setStroke(dp(1), skin.muted());
            all.get(i).setBackground(back);
            all.get(i).setTextColor(on ? skin.text : skin.muted());
        }
    }


    private SettingsArt.Phone phone(String card) {
        SettingsArt.Phone art = new SettingsArt.Phone(this, skin, card)
                .burn(false, 0, true).simple(false)
                .capsule(NavBar.isEnabled()).seekbar(Seekbar.isEnabled()).date(Dates.isEnabled());
        for (int i = 0; i < 4; i++) art.rail(i, FeedRail.shown(i));
        return art;
    }


    private static M3Switch switchIn(View row) {
        if (row instanceof M3Switch) return (M3Switch) row;
        if (row instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) row;
            for (int i = 0; i < group.getChildCount(); i++) {
                M3Switch found = switchIn(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }




    private android.widget.HorizontalScrollView carousel(LinearLayout strip) {
        final android.widget.HorizontalScrollView scroll = new android.widget.HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setPadding(dp(10), dp(2), dp(10), dp(14));
        scroll.addView(strip);
        return scroll;
    }


    private void centre(final android.widget.HorizontalScrollView scroll, final View chosen) {
        if (chosen == null) return;
        scroll.post(() -> scroll.scrollTo(Math.max(0,
                chosen.getLeft() - (scroll.getWidth() - chosen.getWidth()) / 2), 0));
    }


    private View iconCarousel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout head = row();
        head.addView(icon("image"));
        head.addView(label(Text.ICON), grow());
        final TextView named = detail(Launcher.nameOf(Launcher.chosen()));
        named.setTextColor(skin.text);
        head.addView(named);
        box.addView(sized(head, 52));

        LinearLayout strip = new LinearLayout(this);
        final java.util.List<View> frames = new java.util.ArrayList<View>();
        View chosen = null;
        for (String which : Launcher.shown()) {
            View tile = iconTile(which, frames, named);
            if (which.equals(Launcher.chosen())) chosen = tile;
            strip.addView(tile);
        }

        if (Launcher.legacyOffered()) {
            TextView legacy = new TextView(this);
            legacy.setText(Text.ICON_LEGACY);
            legacy.setTextColor(accentInk());
            M3Type.titleSmall(legacy);
            legacy.setGravity(Gravity.CENTER);
            legacy.setPadding(dp(14), 0, dp(6), dp(22));
            strip.addView(legacy, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
            for (String which : Launcher.LEGACY) {
                View tile = iconTile(which, frames, named);
                if (which.equals(Launcher.chosen())) chosen = tile;
                strip.addView(tile);
            }
        }
        android.widget.HorizontalScrollView scroll = carousel(strip);
        box.addView(scroll);
        centre(scroll, chosen);
        return box;
    }

    private View iconTile(final String which, final java.util.List<View> frames, final TextView named) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(4), dp(2), dp(4), dp(2));
        android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
        frame.setPadding(dp(4), dp(4), dp(4), dp(4));
        frame.setTag(which);
        ImageView picture = new ImageView(this);
        Bitmap shot = Launcher.preview(which);
        if (shot != null) picture.setImageBitmap(shot);
        picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
        picture.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(16));
            }
        });
        picture.setClipToOutline(true);
        frame.addView(picture, new android.widget.FrameLayout.LayoutParams(dp(60), dp(60)));

        tile.addView(frame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frames.add(frame);
        TextView name = new TextView(this);
        name.setText(Launcher.nameOf(which));
        M3Type.line(name, 12, 0.4f);
        name.setTextColor((skin.text & 0xFFFFFF) | 0xDD000000);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setPadding(0, dp(4), 0, 0);
        name.setTag("name");
        tile.addView(name, new LinearLayout.LayoutParams(dp(80), ViewGroup.LayoutParams.WRAP_CONTENT));
        ring(frames, Launcher.chosen());
        tile.setOnClickListener(v -> {
            Launcher.choose(this, which);
            ring(frames, which);
            named.setText(Launcher.nameOf(which));
        });
        Motion.press(tile);
        return tile;
    }


    private void ring(java.util.List<View> frames, String chosen) {
        for (View frame : frames) {
            boolean on = chosen.equals(frame.getTag());
            GradientDrawable ring = new GradientDrawable();
            ring.setCornerRadius(dp(20));
            ring.setColor(0);
            if (on) ring.setStroke(dp(3), Accent.colour());
            frame.setBackground(ring);
            View tile = (View) frame.getParent();
            if (tile instanceof ViewGroup) {
                View name = ((ViewGroup) tile).findViewWithTag("name");
                if (name instanceof TextView) ((TextView) name).setTextColor(on ? skin.text : skin.muted());
            }
        }
    }


    private View avatarShapes() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final SettingsArt.Profile art = new SettingsArt.Profile(this, skin);
        box.addView(art);
        LinearLayout head = row();
        head.addView(icon("account_circle"));
        head.addView(label(Text.AVATAR_SHAPE), grow());
        final TextView named = detail(AvatarShape.nameOf(AvatarShape.chosen()));
        named.setTextColor(skin.text);
        head.addView(named);
        box.addView(sized(head, 52));

        LinearLayout strip = new LinearLayout(this);
        final java.util.List<View> chips = new java.util.ArrayList<View>();
        View chosen = null;
        for (final String shape : AvatarShape.ALL) {
            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.VERTICAL);
            chip.setGravity(Gravity.CENTER_HORIZONTAL);
            chip.setPadding(dp(10), dp(10), dp(10), dp(8));
            chip.setTag(shape);
            ImageView picture = new ImageView(this);
            picture.setTag("picture");
            chip.addView(picture, new LinearLayout.LayoutParams(dp(36), dp(36)));
            TextView name = new TextView(this);
            name.setText(AvatarShape.nameOf(shape));
            M3Type.line(name, 12, 0.4f);
            name.setGravity(Gravity.CENTER);
            name.setMaxLines(2);
            name.setTag("name");
            name.setPadding(0, dp(6), 0, 0);

            chip.addView(name, new LinearLayout.LayoutParams(dp(76), ViewGroup.LayoutParams.WRAP_CONTENT));
            chips.add(chip);
            if (shape.equals(AvatarShape.chosen())) chosen = chip;
            chip.setOnClickListener(v -> {
                if (shape.equals(AvatarShape.chosen())) return;
                AvatarShape.choose(this, shape);
                art.shape(shape);
                named.setText(AvatarShape.nameOf(shape));
                shapeChips(chips);

                markChanged();
            });
            Motion.press(chip);
            LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            place.rightMargin = dp(4);
            strip.addView(chip, place);
        }
        shapeChips(chips);
        android.widget.HorizontalScrollView scroll = carousel(strip);
        box.addView(scroll);
        centre(scroll, chosen);
        box.addView(liveToggleRow("animation", Text.AVATAR_SPIN, AvatarShape.spins(), on -> {
            AvatarShape.setSpin(this, on);
            art.invalidate();
        }));
        return box;
    }

    private void shapeChips(java.util.List<View> chips) {
        String now = AvatarShape.chosen();
        for (View chip : chips) {
            boolean on = now.equals(chip.getTag());
            GradientDrawable back = new GradientDrawable();
            back.setCornerRadius(dp(18));
            back.setColor(on ? accentTonal() : 0);
            chip.setBackground(back);
            View picture = chip.findViewWithTag("picture");
            if (picture instanceof ImageView) ((ImageView) picture).setImageBitmap(
                    AvatarShape.picture((String) chip.getTag(), dp(36), on ? Accent.colour() : skin.muted()));
            View name = chip.findViewWithTag("name");
            if (name instanceof TextView) ((TextView) name).setTextColor(on ? skin.text : skin.muted());
        }
    }



    private static final int[] DARKS = {
            0xFF000000, 0xFF0B0B0F, 0xFF121212, 0xFF161823, 0xFF1B1B1B,
            0xFF0D1B2A, 0xFF12232E, 0xFF1A1423, 0xFF14261C, 0xFF241A1A,
    };

    private static final int[] LIGHTS = {
            0xFFFFFFFF, 0xFFF6F6F6, 0xFFEDEDED, 0xFFE8E4DA, 0xFFDCDCDC,
            0xFFCFD8DC, 0xFFB0B8C4, 0xFF8A8A8A, 0xFF5A5A5A, 0xFF2E2E2E,
    };


    private interface ColourPicked { void set(int colour); }

    private View appearanceColourRow(final String role) {
        final int selected = AppearanceColors.ACCENT.equals(role)
                ? Accent.colour() : AppearanceColors.get(role);
        LinearLayout row = row();
        row.setPadding(edge(), 0, edge(), 0);
        row.addView(label(AppearanceColors.label(role)), grow());
        row.addView(new Dot(this, selected == 0 ? skin.muted() : selected, false));
        TextView value = detail(selected == 0 ? "TikTok" : String.format(Locale.US,
                "#%06X", selected & 0xFFFFFF));
        value.setTextColor(skin.muted());
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        valueParams.leftMargin = dp(10);
        row.addView(value, valueParams);
        row.setOnClickListener(v -> askColour(AppearanceColors.label(role),
                selected == 0 ? Accent.TIKTOK : selected, colour -> {
                    AppearanceColors.set(role, colour);
                    markChanged();
                    rebuild();
                }));
        return sized(row, 52);
    }

    private void askColour(String title, int initial, ColourPicked picked) {
        Panel panel = Panel.with(this,skin,title);
        EditText input = panel.field("#RRGGBB");
        input.setText(String.format(Locale.US, "#%06X", initial & 0xFFFFFF));
        panel.primaryKeepOpen(getString(android.R.string.ok), () -> {
                    try {
                        int colour = android.graphics.Color.parseColor(
                                input.getText().toString().trim());
                        picked.set(colour);
                        panel.close();
                    } catch (IllegalArgumentException error) {
                        input.setError(Local.of("Укажите цвет в формате #RRGGBB"));
                    }
                });
        panel.quiet(Text.CANCEL,null); panel.show();
    }


    private void chooseThemePreset() {
        final String[] choices = {Text.THEME_PRESET_DARK, Text.THEME_PRESET_BLUE,
                Text.THEME_PRESET_VICE + NewChip.MARK, Text.THEME_RESET};

        int on = Themes.currentPreset();
        new ModDialog.Builder(this, skin)
                .setTitle(Text.THEME_PRESETS)
                .setSingleChoiceItems(choices, on < 0 ? choices.length - 1 : on, (dialog, index) -> {
                    if (index <= 2) Themes.preset(index);
                    else Themes.reset();
                    dialog.dismiss();
                    markChanged();
                    rebuild();
                })
                .setNegativeButton(Text.CANCEL, null)
                .show();
    }

    private void chooseAccentColour() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(8));
        final AccentColourPicker picker = new AccentColourPicker(this, Accent.colour());
        final View sample = new View(this);
        LinearLayout.LayoutParams sampleParams = new LinearLayout.LayoutParams(dp(56), dp(28));
        sampleParams.gravity = Gravity.CENTER_HORIZONTAL;
        content.addView(sample, sampleParams);
        content.addView(picker, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(238)));
        picker.onColourChanged(colour -> sample.setBackgroundColor(colour));
        sample.setBackgroundColor(Accent.colour());
        android.widget.HorizontalScrollView extras=new android.widget.HorizontalScrollView(this);extras.setHorizontalScrollBarEnabled(false);LinearLayout strip=new LinearLayout(this);strip.setOrientation(LinearLayout.HORIZONTAL);strip.setPadding(0,dp(8),0,dp(8));
        final java.util.List<Dot> extraDots=new java.util.ArrayList<>();for(int colour:new int[]{0xFFFFA726,0xFF44D65B,0xFFAA45EE,0xFFF343C9,0xFF405FE8,0xFF35B8E5,0xFFFA4654}){Dot dot=new Dot(this,colour,colour==Accent.colour());extraDots.add(dot);dot.setContentDescription(String.format(Locale.ROOT,"#%06X",colour&0xffffff));Motion.touch(dot);dot.setOnClickListener(v->{picker.setColour(colour);for(Dot other:extraDots)other.choose(other==dot,true);});LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(dp(44),dp(44));size.rightMargin=dp(8);strip.addView(dot,size);}extras.addView(strip);content.addView(extras);
        new ModDialog.Builder(this, skin).setTitle(Text.ACCENT_PICKER)
                .setView(content).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Accent.setCustom(picker.colour());
                    if(accentCustomSwatch!=null)accentCustomSwatch.reveal(picker.colour());
                    markChanged();
                    if(rebuildQueued)column.removeCallbacks(delayedRebuild);rebuildQueued=true;column.postDelayed(delayedRebuild,260L);
                }).show();
    }

    private View palette() {
        android.widget.HorizontalScrollView scroll=new android.widget.HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);scroll.setClipToPadding(false);
        LinearLayout strip=new LinearLayout(this);strip.setOrientation(LinearLayout.HORIZONTAL);strip.setPadding(dp(16),dp(12),dp(16),dp(16));
        int[] colours=Accent.palette();final java.util.List<Dot> swatches=new java.util.ArrayList<>();
        for(int colour:colours){Dot dot=new Dot(this,colour,!Accent.isMaterial()&&colour==Accent.colour());swatches.add(dot);Motion.touch(dot);
            dot.setContentDescription(String.format(Locale.US,"#%06X",colour&0xFFFFFF));
            dot.setOnClickListener(v->{Accent.set(colour);for(Dot swatch:swatches)swatch.choose(swatch.fill.getColor()==colour,true);if(accentCustomSwatch!=null)accentCustomSwatch.choose(false,true);markChanged();rebuildAfterSwitchAnimation();});
            LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(dp(48),dp(48));size.rightMargin=dp(8);strip.addView(dot,size);
        }
        int custom=Accent.custom();
        Dot picker=new Dot(this,custom==0?Accent.colour():custom,custom!=0 && !Accent.isMaterial() && custom==Accent.colour());
        if(custom==0)picker.fill.setShader(new android.graphics.SweepGradient(dp(24),dp(24),new int[]{0xFFE53935,0xFFFFB300,0xFF43A047,0xFF00ACC1,0xFF1E88E5,0xFF8E24AA,0xFFE53935},null));
        accentCustomSwatch=picker;Motion.touch(picker);picker.setContentDescription(Text.ACCENT_PICKER);picker.setOnClickListener(v->chooseAccentColour());
        strip.addView(picker,new LinearLayout.LayoutParams(dp(48),dp(48)));scroll.addView(strip);return scroll;
    }


    private View icon(String name) {
        return icon(name, false, 40);
    }


    private static final java.util.Map<String, Integer> SHAPES = new java.util.HashMap<String, Integer>();
    static {
        SHAPES.put("palette", Expressive.COOKIE9);
        SHAPES.put("group", Expressive.CLOVER4);
        SHAPES.put("play_circle", Expressive.SUNNY);
        SHAPES.put("tune", Expressive.FLOWER6);
        SHAPES.put("info", Expressive.PENTAGON);
        SHAPES.put("extension", Expressive.COOKIE7);
        SHAPES.put("chat", Expressive.BURST12);
    }

    private static int shapeFor(String name) {
        Integer fixed = SHAPES.get(name);
        return fixed != null ? fixed : (name.hashCode() & 0x7FFFFFFF) % Expressive.KINDS;
    }


    private View icon(String name, boolean toggle, int sizeDp) {
        if (!Design.frosted()) {

            android.graphics.drawable.Drawable plain = name.equals("search")
                    ? new Expressive.Magnifier(iconInk()) : SettingsGlyph.make(name, iconInk());
            if (plain != null) {
                android.widget.ImageView view = new android.widget.ImageView(this);
                view.setImageDrawable(plain);
                view.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
                int pad = dp(Math.max(0, (Math.min(sizeDp, 40) - 24) / 2));
                view.setPadding(pad, pad, pad, pad);
                LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(Math.min(sizeDp, 40)), dp(Math.min(sizeDp, 40)));
                size.leftMargin = dp(4);
                size.rightMargin = dp(16);
                view.setLayoutParams(size);
                return view;
            }
        }
        android.graphics.drawable.Drawable vector = name.equals("search")
                ? new Expressive.Magnifier(0xFFFFFFFF) : SettingsGlyph.make(name, 0xFFFFFFFF);
        if (vector == null) {
            Bitmap picture = pictureFor(name);
            if (picture != null) vector = new android.graphics.drawable.BitmapDrawable(getResources(), picture);
        }
        if (vector == null) {
            vector = SettingsGlyph.make("tune", 0xFFFFFFFF);
        }
        int accent = Accent.colour();
        int tonal = accentTonal();
        int ink = accentInk();
        Expressive.Badge badge = toggle
                ? new Expressive.Badge(this, shapeFor(name), vector, sizeDp,
                        Expressive.mix(skin.card, skin.text, 0.14f), accent, skin.muted(), onAccent())
                : new Expressive.Badge(this, shapeFor(name), vector, sizeDp, tonal, tonal, ink, ink);
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp));
        size.rightMargin = dp(Design.frosted() ? 16 : 12);
        badge.setLayoutParams(size);
        return badge;
    }

    private Bitmap pictureFor(String name) {
        Bitmap bitmap = ICONS.get(name);
        if (bitmap == null) {
            try {
                String data = Icons.PNG.get(name);
                if (data != null) {
                    byte[] png = android.util.Base64.decode(data, android.util.Base64.DEFAULT);
                    bitmap = android.graphics.BitmapFactory.decodeByteArray(png, 0, png.length);
                    if (bitmap != null) ICONS.put(name, bitmap);
                }
            } catch (Throwable ignored) {
            }
        }
        return bitmap;
    }

    private static final java.util.Map<String, Bitmap> ICONS =
            new java.util.HashMap<String, Bitmap>();


    private interface Setting {
        void set(boolean on);
    }

    private View toggleRow(String picture, String title, boolean on, final Setting setting) {
        return toggleRow(picture,title,on,setting,true);
    }

    private View liveToggleRow(String picture, String title, boolean on, final Setting setting) {
        return toggleRow(picture,title,on,setting,false);
    }

    private View toggleRow(String picture, String title, boolean on, final Setting setting,
                           final boolean requiresRestart) {
        LinearLayout row = row();
        final View lead = icon(picture, true, 40);
        if (lead instanceof Expressive.Badge) ((Expressive.Badge) lead).set(on, false);
        row.addView(lead);
        row.addView(label(title), grow());

        settingTargets.put(title,row);
        final M3Switch toggle = new M3Switch(this);
        toggle.colours(Accent.colour(), skin.muted(), skin.card);
        toggle.setContentDescription(title);
        toggle.setChecked(on);
        toggle.setOnChanged(checked -> {
            if (lead instanceof Expressive.Badge) ((Expressive.Badge) lead).set(checked, true);
            setting.set(checked);
            if (requiresRestart) markChanged();
        });
        row.addView(toggle);

        row.setOnClickListener(v -> {
            toggle.performClick();
        });
        return sized(row, 48);
    }



    private View betaRow(String picture, String title, boolean on, final Setting setting) {
        LinearLayout row = row();
        final View lead = icon(picture, true, 40);
        if (lead instanceof Expressive.Badge) ((Expressive.Badge) lead).set(on, false);
        row.addView(lead);
        row.addView(label(title), grow());

        TextView beta = new TextView(this);
        beta.setText(Text.BETA);
        beta.setTextColor(onAccent());
        beta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        beta.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        beta.setPadding(dp(8), dp(3), dp(8), dp(3));
        GradientDrawable chip = new GradientDrawable();
        chip.setColor(Accent.colour());
        chip.setCornerRadius(dp(12));
        beta.setBackground(chip);
        LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        place.leftMargin = dp(8);
        row.addView(beta, place);

        final M3Switch toggle = new M3Switch(this);
        toggle.colours(Accent.colour(), skin.muted(), skin.card);
        toggle.setChecked(on);
        toggle.setOnChanged(checked -> {
            if (lead instanceof Expressive.Badge) ((Expressive.Badge) lead).set(checked, true);
            setting.set(checked);
            markChanged();
        });
        LinearLayout.LayoutParams togglePlace = new LinearLayout.LayoutParams(dp(52), dp(32));
        togglePlace.leftMargin = dp(10);
        row.addView(toggle, togglePlace);
        return sized(row, 52);
    }

    private View stickerHead() {
        LinearLayout row = row();
        row.addView(icon("star"));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(Text.STREAK_STICKER));
        int many = Streaks.offered().size();
        text.addView(detail(many == 0 ? Text.STREAK_NOTHING : String.valueOf(many)));
        row.addView(text, grow());

        ImageView refreshBtn = new ImageView(this);
        refreshBtn.setImageDrawable(SettingsGlyph.make("refresh", skin.muted()));
        refreshBtn.setScaleType(ImageView.ScaleType.CENTER);
        int pad = dp(8);
        refreshBtn.setPadding(pad, pad, pad, pad);
        refreshBtn.setOnClickListener(v -> {
            FavouriteStickers.read();
            FavouriteStickers.load(() -> {
                if (!isFinishing()) rebuild();
            });
            Screen.say("Стикеры обновлены");
        });
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(dp(36), dp(36));
        rlp.rightMargin = dp(4);
        row.addView(refreshBtn, rlp);

        row.addView(disclosure(streakOpen,"streakOpen"));

        row.setOnClickListener(v -> {
            disclosureKey = "streakOpen";
            streakOpen = !streakOpen;
            rebuildAfterReveal(streakOpen);
        });
        return sized(row, 60);
    }


    private View stickerChoices() {
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(16), dp(10), dp(16), dp(14));

        FavouriteStickers.load(()->{if(!isFinishing())rebuild();});
        java.util.List<String> ids = Streaks.offered();
        if (ids.isEmpty()) {
            rows.addView(quiet(Text.STREAK_NOTHING));
            return rows;
        }

        String chosen = Streaks.chosen();
        LinearLayout line = null;
        for (int i = 0; i < ids.size() && i < 120; i++) {
            if (i % 5 == 0) {
                line = new LinearLayout(this);
                line.setOrientation(LinearLayout.HORIZONTAL);
                line.setPadding(0, dp(5), 0, dp(5));
                rows.addView(line);
            }
            final String id = ids.get(i);
            View one = stickerTile(id, id.equals(chosen));
            LinearLayout.LayoutParams size =
                    new LinearLayout.LayoutParams(dp(52), dp(52), 1f);
            line.addView(one, size);
        }
        return rows;
    }

    private View stickerTile(final String id, boolean chosen) {
        ImageView view = new ImageView(this);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setPadding(dp(4), dp(4), dp(4), dp(4));
        if (chosen) {
            GradientDrawable ring = new GradientDrawable();
            ring.setColor(0x00000000);
            ring.setStroke(dp(2), Accent.colour());
            ring.setCornerRadius(dp(10));
            view.setBackground(ring);
        }
        final java.lang.ref.WeakReference<ImageView> target=new java.lang.ref.WeakReference<ImageView>(view);
        Bitmap picture = Streaks.thumbnail(this, id,()->{
            ImageView tile=target.get();if(tile!=null && tile.isAttachedToWindow()){
                Bitmap loaded=Streaks.thumbnail(tile.getContext(),id);if(loaded!=null)tile.setImageBitmap(loaded);
            }
        });
        if (picture != null) view.setImageBitmap(picture);
        view.setOnClickListener(v -> {
            Streaks.choose(id);
            rebuild();
        });
        return view;
    }


    private View actionRow(String picture, String title, String detail, final Runnable action) {
        LinearLayout row = row();
        row.addView(icon(picture));
        boolean value = detail != null && detail.length() <= 14;

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        if (NewChip.marked(title)) {

            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.addView(label(NewChip.plain(title)));
            LinearLayout.LayoutParams chipWhere = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            chipWhere.leftMargin = dp(8);
            line.addView(NewChip.make(this), chipWhere);
            text.addView(line);
        } else {
            text.addView(label(title));
        }
        if (detail != null && !value) text.addView(detail(detail));
        row.addView(text, grow());

        if (value) {
            TextView shown = detail(detail);
            shown.setGravity(Gravity.CENTER_VERTICAL);
            shown.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);shown.setPadding(dp(8), 0, dp(6), 0);
            row.addView(shown);
        }
        if (action != null) {
            if (Design.frosted()) {
                ImageView chevron = new ImageView(this);
                chevron.setImageDrawable(SettingsGlyph.make("chevron_right", skin.muted()));
                row.addView(chevron, new LinearLayout.LayoutParams(dp(24), dp(24)));
            }
            row.setOnClickListener(v -> action.run());
        } else {
            row.setClickable(false);
            row.setFocusable(false);
        }
        return sized(row, detail == null || value ? 48 : 60);
    }


    private View versions() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(20), dp(24), dp(20), dp(8));

        TextView creator = new TextView(this);
        creator.setText("AirTok · Создатель: Fasdeq13");
        creator.setTextColor(0xFFFE2C55);
        creator.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        creator.setGravity(Gravity.CENTER);
        creator.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/fasdeq13")));
            } catch (Throwable ignored) {}
        });

        TextView thanks = new TextView(this);
        thanks.setText("Благодарность: narezany, montik, Fezix");
        thanks.setTextColor(skin.muted());
        thanks.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        thanks.setGravity(Gravity.CENTER);
        thanks.setPadding(0, dp(4), 0, 0);

        box.addView(creator);
        return box;
    }

    private View idRow(String title, final String value) {
        LinearLayout row = row();
        row.addView(icon("fingerprint"));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(title));

        TextView shown = detail(value == null ? Text.ACCOUNT_UNKNOWN : shorten(value));
        if (value != null) shown.setTextColor(skin.text);
        text.addView(shown);
        row.addView(text, grow());

        if (value != null) {
            row.addView(away());
            row.setOnClickListener(v -> copy(title, value));
        }
        return sized(row, 60);
    }

    private static String shorten(String value) {
        if (value.length() <= 26) return value;
        return value.substring(0, 14) + "…" + value.substring(value.length() - 8);
    }



    private void showPluginAdd(){
        Panel panel=Panel.with(this,skin,Local.of("Добавить плагин"));
        panel.primary(Local.of("Создать из кнопок"),()->ScriptPlugins.edit(this,ScriptPlugins.SAMPLE,this::rebuild));
        panel.quiet(Local.of("Вставить JSON"),()->ScriptPlugins.edit(this,"",this::rebuild));
        panel.quiet(Local.of("Установить файл .mtp"),this::pickPlugin);
        panel.quiet(Local.of("Магазин"),()->startActivity(new Intent(this,StoreActivity.class)));
        panel.quiet(Local.of("Документация"),()->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(DOCS_PLUGINS))));
        panel.quiet(Text.CLOSE,null);panel.show();
    }

    private static final int PICK_PLUGIN = 0x4D50;




    private View pluginSettingRow(final Plugins.Row item,
                                  final android.content.SharedPreferences store) {
        switch (item.kind) {
            case Plugins.Row.SECTION:
                return caption(item.title);
            case Plugins.Row.TOGGLE:
                return toggleRow("extension", item.title,
                        store.getBoolean(item.key, item.def != 0),
                        on -> {store.edit().putBoolean(item.key, on).apply();Plugins.settingsChanged(item.plugin,item.key);});
            case Plugins.Row.CHOICE: {
                final String[] options = item.options;
                if (options == null || options.length == 0) return null;
                int now = Math.max(0, Math.min(options.length - 1,
                        store.getInt(item.key, item.def)));
                return actionRow("tune", item.title, options[now], () ->
                        new ModDialog.Builder(this, skin)
                                .setTitle(item.title)
                                .setSingleChoiceItems(options,
                                        Math.max(0, Math.min(options.length - 1,
                                                store.getInt(item.key, item.def))),
                                        (dialog, which) -> {
                                            store.edit().putInt(item.key, which).apply();
                                            Plugins.settingsChanged(item.plugin,item.key);
                                            markChanged();
                                            dialog.dismiss();
                                            rebuild();
                                        })
                                .show());
            }
            case Plugins.Row.SLIDER: {
                int now = Math.max(item.min, Math.min(item.max,
                        store.getInt(item.key, item.def)));
                return slider(item.title, now - item.min, item.max - item.min, item.min, "",
                        value -> {
                            store.edit().putInt(item.key, value + item.min).apply();
                            Plugins.settingsChanged(item.plugin,item.key);
                            markChanged();
                        });
            }
            default:
                return actionRow("extension", item.title, item.detail, () -> {
                    try {
                        if (item.action != null) item.action.tapped();
                    } catch (Throwable error) {
                        Diary.note("plugin " + item.plugin + ": " + error);
                    }
                });
        }
    }

    private View installRow() {
        LinearLayout row = row();
        row.addView(icon("extension"));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(Text.PLUGIN_INSTALL));
        text.addView(detail(Text.PLUGIN_INSTALL_NOTE));
        row.addView(text, grow());

        TextView plus = new TextView(this);
        plus.setText("+");
        plus.setTextColor(Accent.colour());
        plus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        plus.setPadding(dp(12), 0, 0, 0);
        row.addView(plus);

        row.setOnClickListener(v -> pickPlugin());
        return sized(row, 60);
    }


    private View pluginRow(final Plugins.Info info) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));

        Bitmap icon = info.icon();
        if (icon != null) {
            ImageView view = new ImageView(this);
            view.setImageBitmap(icon);
            view.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams size =
                    new LinearLayout.LayoutParams(dp(40), dp(40));
            size.rightMargin = dp(14);
            row.addView(view, size);
        } else {
            LinearLayout.LayoutParams size =
                    new LinearLayout.LayoutParams(dp(40), dp(40));
            size.rightMargin = dp(14);
            row.addView(new Dot(this, Accent.colour(), false), size);
        }

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(info.name + "  " + info.version));

        String by = info.author;
        if (!info.description.isEmpty()) by = by + "  ·  " + info.description;
        text.addView(detail(by));
        if (info.trouble != null) text.addView(detail(info.trouble));
        long stalled = Lag.cost(info.id);
        if (stalled >= 1000L) {
            TextView slow = detail(Local.of("Тормозит приложение: подвисаний за этот запуск") + " " + (stalled + 500L) / 1000L + " " + Local.of("с"));
            slow.setTextColor(0xFFE57373);
            text.addView(slow);
        }
        if(info.safety!=null&&!info.safety.isEmpty())text.addView(detail(Local.of("Удерживайте для действий")));
        row.addView(text, grow());

        final M3Switch toggle = new M3Switch(this);
        toggle.colours(Accent.colour(), skin.muted(), skin.card);
        toggle.setChecked(Plugins.isEnabled(info.id));

        toggle.setEnabled(true);
        toggle.setOnChanged(checked -> {
            activatePlugin(info,checked);
        });
        row.addView(toggle);

        row.setOnClickListener(v -> openPluginSettings(info));
        Motion.press(row);
        row.setOnLongClickListener(v -> {
            Panel actions=Panel.with(this,skin,info.name).compact();
            actions.primary(Local.of("Редактировать"),()->PluginEditorActivity.binary(this,info.id));
            actions.quiet(Local.of("Удалить с устройства"),()->askToRemove(info));
            if(AdminAccess.current())actions.quiet(Local.of("Управление магазином"),()->PluginAdmin.show(this));
            actions.quiet(Text.CLOSE,null);actions.show();
            return true;
        });
        return row;
    }

    private void activatePlugin(final Plugins.Info info,boolean enabled){



        if(!enabled){Net.away("plugin stop",()->{Plugins.setEnabled(info.id,false);runOnUiThread(()->{if(!isFinishing()){markChanged();rebuild();}});});return;}

        Net.away("plugin start",()->{Plugins.setEnabled(info.id,true);runOnUiThread(()->{if(!isFinishing()){markChanged();rebuild();}});});
    }

    private void openPluginSettings(final Plugins.Info info) {
        if(Plugins.isEnabled(info.id) && Plugins.openSettings(info.id,this))return;
        Panel panel=Panel.with(this,skin,info.name);
        if(info.safety!=null&&!info.safety.isEmpty())panel.text(info.safety);
        LinearLayout body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        android.content.SharedPreferences store=getSharedPreferences("airtok_plugin_"+info.id,MODE_PRIVATE);
        int count=0;
        if(Plugins.isEnabled(info.id)) for(Plugins.Row item:Plugins.rows()) {
            if(!info.id.equals(item.plugin))continue;
            try { View control=pluginSettingRow(item,store); if(control!=null){body.addView(control);count++;} }
            catch(Throwable error){Diary.note("plugin settings "+info.id+": "+error);}
        }
        if(count==0){
            if(!Plugins.isEnabled(info.id))panel.text(Local.of("Включите плагин, чтобы открыть его настройки."));
            else if(Plugins.isRunning(info.id))panel.text(Local.of("Плагин не предоставил настройки. Версия ")+info.version);
            else{
                String why=Plugins.problem(info.id);
                panel.text(why!=null?Local.of("Плагин включён, но не запущен: ")+why:Local.of("Плагин включён, но ещё не запущен в этом сеансе."));
                panel.primary(Local.of("Запустить заново"),()->Net.away("plugin revive",()->{
                    final boolean ok=Plugins.revive(info.id);
                    runOnUiThread(()->{if(isFinishing())return;rebuild();if(ok)openPluginSettings(info);});
                }));
            }
        }else panel.view(body);
        panel.quiet(Local.of("Назад"),null).show();
    }

    private void pickPlugin() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            intent.setType("*/*");
            startActivityForResult(intent, PICK_PLUGIN);
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null) return;
        Uri source = data.getData();
        if (source == null) return;

        if (request == CREATE_BACKUP) {
            Screen.progress(Text.BACKUP_WRITING, 0);
            Net.away("backup", () -> {
                boolean done = Backup.write(this, source);
                if (done) Screen.progressDone(); else Screen.progressGone();
                runOnUiThread(() -> Screen.say(done ? Text.BACKUP_DONE : Text.BACKUP_FAILED));
            });
            return;
        }
        if (request == PICK_BACKUP) {
            reviewBackup(source);
            return;
        }
        if (request == PICK_STYLE) {
            reviewStyle(source);
            return;
        }
        if (request == CREATE_STYLE) {
            final String json = Design.style().json();
            Net.away("style save", () -> {
                boolean done = false;
                if (json != null) try (java.io.OutputStream out = getContentResolver().openOutputStream(source)) {
                    if (out != null) { out.write(json.getBytes("UTF-8")); done = true; }
                } catch (Throwable error) {
                    Diary.note("style save: " + error);
                }
                final boolean saved = done;
                runOnUiThread(() -> Screen.say(saved ? Text.UI_FILE_SAVED : Text.UI_FILE_SAVE_FAILED));
            });
            return;
        }

        if (request == PICK_FONT || request == PICK_EMOJI) {
            if (Fonts.take(this, source, request == PICK_EMOJI)) markChanged();
            else Toast.makeText(this, Text.FONT_FAILED, Toast.LENGTH_LONG).show();
            rebuild();
            return;
        }
        if (request == CustomIcon.PICK) {
            CustomIcon.offer(this, source);
            return;
        }
        if (request != PICK_PLUGIN) return;
        reviewPlugin(source);
    }
    private void reviewPlugin(final Uri source){
        Net.away("plugin inspection",()->{
            try {final PluginAudit.Report report=Plugins.inspect(this,source);
                runOnUiThread(()->{
                    if(isFinishing())return;
                    Panel panel=Panel.with(this,skin,Text.PLUGIN_REVIEW).text(report.summary());
                    panel.primary(Text.PLUGIN_INSTALL,()->Net.away("plugin install",()->{
                        try {Plugins.install(this,source,report.digest);runOnUiThread(()->{if(!isFinishing()){markChanged();rebuild();}});Screen.say(Text.PLUGIN_INSTALLED);}
                        catch(Throwable error){Screen.say(String.valueOf(error.getMessage()));}
                    }));
                    panel.quiet(Local.of("Назад"),null).show();
                });
            }catch(Throwable error){runOnUiThread(()->{if(!isFinishing())Panel.with(this,skin,Local.of("Плагин не принят")).text(String.valueOf(error.getMessage())).quiet(Local.of("Назад"),null).show();});}
        });
    }

    private void askToRemove(final Plugins.Info info) {
        try {
            new ModDialog.Builder(this, skin)
                    .setTitle(Text.PLUGIN_REMOVE_ASK)
                    .setMessage(info.name + "  " + info.version)
                    .setNegativeButton(Text.CANCEL, null)
                    .setPositiveButton(Text.PLUGIN_REMOVE, (dialog, which) -> {
                        Plugins.uninstall(this, info.id);
                        markChanged();
                        rebuild();
                    })
                    .show();
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }



    private View linkRow(String picture, String title, String handle, final String url) {
        LinearLayout row = row();
        row.addView(icon(picture));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(title));
        text.addView(detail(handle));
        row.addView(text, grow());
        row.addView(away());

        row.setOnClickListener(v -> open(url));
        return sized(row, 60);
    }

    private View creatorRow(String title, String handle, final String url) {
        LinearLayout row = row();
        ImageView avatarView = new ImageView(this);
        avatarView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        android.graphics.Bitmap avBmp = FasdeqAvatar.getCircle(dp(36));
        if (avBmp != null) {
            avatarView.setImageDrawable(new android.graphics.drawable.BitmapDrawable(getResources(), avBmp));
        } else {
            avatarView.setImageDrawable(SettingsGlyph.make("account_circle", skin.text));
        }
        LinearLayout.LayoutParams avParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        avParams.rightMargin = dp(Design.frosted() ? 12 : 16);
        row.addView(avatarView, avParams);

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(title));
        text.addView(detail(handle));
        row.addView(text, grow());
        row.addView(away());

        row.setOnClickListener(v -> open(url));
        return sized(row, 60);
    }


    private View channelBanner() {
        ChannelBanner banner = new ChannelBanner(this);
        banner.setOnClickListener(v -> open(ChannelBanner.URL));
        LinearLayout box = new LinearLayout(this);
        box.setPadding(edge(), dp(2), edge(), dp(10));
        box.addView(banner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return box;
    }




    private View space() {
        java.util.List<ThanksSpace.Person> people = new java.util.ArrayList<>();
        people.add(new ThanksSpace.Person("@narezany", Text.THANKS_OWNER, ThanksSpace.EGG_TILCAYO, this::ownerLinks));
        people.add(new ThanksSpace.Person("Claude Opus 5.5", Text.THANKS_CLAUDE, ThanksSpace.EGG_CONSTELLATION, null));
        people.add(new ThanksSpace.Person("someone's mf", Text.THANKS_TTCUZ, ThanksSpace.EGG_SCATTER, () -> open(TTCUZ)));
        people.add(new ThanksSpace.Person("@saintcursed", Local.of("Автор LotusTok: призрак, папки, редактор интерфейса, анимация набора и другое"),
                ThanksSpace.EGG_LOTUS, () -> open(LOTUSTOK)));
        people.add(new ThanksSpace.Person("TT Extras", Local.of("Плагин для MargyT: из него призрак и длинные комментарии"),
                ThanksSpace.EGG_GHOST, null));
        people.add(new ThanksSpace.Person("санек :3 (санечек)", Local.of("Часть дизайна MargyT"), ThanksSpace.EGG_HEART, () -> open(SANEK)));
        people.add(new ThanksSpace.Person("Fezix", Local.of("Участник и поддержка проекта"), ThanksSpace.EGG_HEART, null));
        java.util.List<ThanksSpace.Planet> planets = new java.util.ArrayList<>();
        planets.add(new ThanksSpace.Planet(Text.YOOMONEY, 0xFF8B5CF6, () -> open(YOOMONEY)));
        planets.add(new ThanksSpace.Planet(Text.BOOSTY, 0xFFF97316, () -> open(BOOSTY)));
        planets.add(new ThanksSpace.Planet(Text.FUNPAY, 0xFF38BDF8, () -> open(FUNPAY)));
        planets.add(new ThanksSpace.Planet(Text.STARS, 0xFFFACC15, () -> open(STARS)));
        return new ThanksSpace(this, people, planets,
                Local.of("нажми на имя · зажми, чтобы открыть ссылку"), Text.DONATE);
    }


    private void showThanks() {
        final android.app.Dialog sky = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
        final ThanksSpace space = (ThanksSpace) space();
        frame.addView(space, new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView close = new TextView(this);
        close.setText("\u2715");
        close.setTextColor(0xDDFFFFFF);
        close.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        close.setGravity(Gravity.CENTER);
        close.setContentDescription(Text.CLOSE);
        close.setOnClickListener(v -> sky.dismiss());
        android.widget.FrameLayout.LayoutParams corner = new android.widget.FrameLayout.LayoutParams(dp(56), dp(56), Gravity.TOP | Gravity.END);
        frame.addView(close, corner);
        frame.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            corner.topMargin = top;
            close.setLayoutParams(corner);
            space.setTopInset(top + dp(40));
            return insets;
        });
        sky.setContentView(frame);
        android.view.Window window = sky.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFF070A16));
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            window.setStatusBarColor(0);
            window.setNavigationBarColor(0);
        }
        sky.show();
    }


    private void ownerLinks() {
        try {
            new ModDialog.Builder(this, skin)
                    .setTitle("@narezany")
                    .setItems(new CharSequence[]{"Telegram", "TikTok"}, (dialog, which) ->
                            open(which == 0 ? OWNER_TELEGRAM : OWNER_TIKTOK))
                    .setNegativeButton(Text.CANCEL, null)
                    .show();
        } catch (Throwable error) {
            open(OWNER_TELEGRAM);
        }
    }




    private View away() {

        if (!Design.frosted()) { View none = new View(this); none.setVisibility(View.GONE); return none; }
        Chevron arrow=new Chevron(this,skin.muted(),false,false);
        arrow.setRotation(-90f);
        return arrow;
    }


    private TextView quiet(String message) {
        TextView view = new TextView(this);
        view.setText(message);
        view.setTag(CAPTION, message);
        view.setTextColor(skin.muted());
        if (Design.frosted()) view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        else M3Type.bodySmall(view);
        view.setPadding(dp(16), dp(2), dp(16), dp(8));
        return view;
    }




    private void open(String url) {



        url = inApp(url);
        if (url.contains("tiktok.com") && openWith(url, getPackageName())) return;
        if (openWith(url, null)) return;
        Toast.makeText(this, Text.NO_BROWSER, Toast.LENGTH_SHORT).show();
    }


    private static String inApp(String url) {
        if (url.startsWith("https://tiktok.com/")) {
            return "https://www.tiktok.com/" + url.substring("https://tiktok.com/".length());
        }
        return url;
    }

    private boolean openWith(String url, String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (packageName != null) intent.setPackage(packageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void copy(String what, String text) {
        try {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText(what, text));
            Toast.makeText(this, Text.COPIED, Toast.LENGTH_SHORT).show();
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }


    private void developerTap() {
        if (!DevMode.tapped()) return;
        if (DevMode.isOn()) {
            Toast.makeText(this, Text.DEV_ALREADY, Toast.LENGTH_SHORT).show();
            return;
        }
        DevMode.set(true);
        Diary.note("developer mode on");
        Toast.makeText(this, Text.DEV_ON, Toast.LENGTH_SHORT).show();
        rebuild();
    }


    private void addDeveloperRows(LinearLayout into) {
        into.addView(line());
        into.addView(actionRow("tune", Text.DEV_FLAGS, Text.DEV_FLAGS_NOTE, this::showFlags));
        into.addView(line());
        into.addView(actionRow("article", Text.DEV_FULL_REPORT, Text.DEV_FULL_NOTE,
                () -> copy("MargyT full report", Diagnostics.fullReport())));
        into.addView(line());
        into.addView(toggleRow("tune", Text.DEV_SWITCH, true, on -> {
            if (!on) {
                DevMode.set(false);
                rebuildAfterSwitchAnimation();
            }
        }));
    }


    private void chooseAvatarShape() {
        final Panel panel = Panel.with(this, skin, Text.AVATAR_SHAPE);
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        final String now = AvatarShape.chosen();
        LinearLayout line = null;
        for (int i = 0; i < AvatarShape.ALL.length; i++) {
            final String shape = AvatarShape.ALL[i];
            if (i % 4 == 0) {
                line = new LinearLayout(this);
                grid.addView(line, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            LinearLayout tile = Panel.tile(this, skin, AvatarShape.picture(shape, dp(36), 0xFFFFFFFF),
                    shape.equals(now) ? Accent.colour() : skin.text, AvatarShape.nameOf(shape),
                    shape.equals(now));
            tile.setOnClickListener(v -> {
                panel.close();
                if (shape.equals(now)) return;
                AvatarShape.choose(this, shape);

                markChanged();
                rebuild();
            });
            Motion.press(tile);
            LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            place.setMargins(dp(3), dp(3), dp(3), dp(3));
            line.addView(tile, place);
        }

        for (int i = AvatarShape.ALL.length; i % 4 != 0; i++) {
            line.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
            ((LinearLayout.LayoutParams) line.getChildAt(line.getChildCount() - 1).getLayoutParams())
                    .setMargins(dp(3), dp(3), dp(3), dp(3));
        }
        panel.view(grid);
        panel.quiet(Text.CLOSE, null);
        panel.show();
    }



    private static final int COUNTRY_PAGE = 97;
    private int countryFromGroup;
    private String countryFromPage;
    private String countryQuery = "";

    private void showCountries() {
        if (navigating) return;
        countryFromGroup = settingsGroup;
        countryFromPage = settingsPage;
        navigating = true;
        Motion.navigate(column, 1f, () -> {
            settingsGroup = COUNTRY_PAGE;
            settingsPage = null;
            searchQuery = "";
            countryQuery = "";
            page.scrollTo(0, 0);
            rebuild();
            navigating = false;
        });
    }


    private void countriesPage() {
        LinearLayout field = new LinearLayout(this);
        field.setOrientation(LinearLayout.HORIZONTAL);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setPadding(dp(16), dp(4), dp(16), dp(4));
        GradientDrawable fieldBack = new GradientDrawable();
        fieldBack.setColor(skin.card);
        fieldBack.setCornerRadius(dp(28));
        field.setBackground(fieldBack);
        ImageView glass = new ImageView(this);
        glass.setImageDrawable(new Expressive.Magnifier(skin.muted()));
        LinearLayout.LayoutParams glassSize = new LinearLayout.LayoutParams(dp(24), dp(24));
        glassSize.rightMargin = dp(16);
        field.addView(glass, glassSize);
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(Local.of("Поиск страны"));
        input.setHintTextColor(skin.muted());
        input.setTextColor(skin.text);
        M3Type.line(input, 16, 0.5f);
        input.setBackgroundColor(Color.TRANSPARENT);
        field.addView(input, grow());
        LinearLayout box = new LinearLayout(this);
        box.setPadding(edge(), dp(4), edge(), dp(8));
        box.addView(sized(field, 56));
        column.addView(box);


        java.util.List<String[]> all = new java.util.ArrayList<>(java.util.Arrays.asList(AirTok.COUNTRIES));
        final java.text.Collator order = java.text.Collator.getInstance();
        final String chosen = AirTok.iso();
        java.util.Collections.sort(all, (a, b) -> {
            if (a[AirTok.ISO].equals(chosen) != b[AirTok.ISO].equals(chosen)) return a[AirTok.ISO].equals(chosen) ? -1 : 1;
            return order.compare(AirTok.name(a), AirTok.name(b));
        });
        final LinearLayout list = card();
        final java.util.List<View> rows = new java.util.ArrayList<>();
        final java.util.List<String> keys = new java.util.ArrayList<>();
        for (String[] country : all) {
            View row = countryRow(country);
            rows.add(row);
            keys.add((AirTok.name(country) + " " + country[AirTok.LABEL] + " " + country[AirTok.ISO] + " "
                    + country[AirTok.CARRIER]).toLowerCase(Locale.ROOT).replace('ё', 'е'));
            list.addView(row);
        }
        column.addView(wrap(list));
        final TextView none = quiet(Local.of("Ничего не нашлось"));
        none.setVisibility(View.GONE);
        none.setGravity(Gravity.CENTER);
        column.addView(none);
        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence t, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence t, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable text) {
                countryQuery = text.toString().trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
                int shown = 0;
                for (int i = 0; i < rows.size(); i++) {
                    boolean hit = countryQuery.length() == 0 || keys.get(i).contains(countryQuery);
                    rows.get(i).setVisibility(hit ? View.VISIBLE : View.GONE);
                    if (hit) shown++;
                }
                none.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
            }
        });
    }

    private static final int FLAGS_PAGE = 98;

    private static final int FLAG_ROWS = 60;

    private String flagQuery = "";
    private java.util.List<Flags.Known> flagList;
    private boolean flagsLoading;

    private void showFlags() {
        settingsGroup = FLAGS_PAGE;
        searchQuery = "";
        flagQuery = "";
        page.scrollTo(0, 0);
        rebuild();
    }


    private void flagsPage() {
        LinearLayout warning = card();
        warning.addView(quiet(Text.FLAGS_WARNING));
        column.addView(wrap(warning));

        final java.util.Map<String, Object> mine = Flags.overrides();
        if (FlagGuard.heldBack() && !mine.isEmpty()) {
            LinearLayout held = card();
            held.addView(quiet(Text.FLAGS_HELD));
            held.addView(actionRow("restore", Text.FLAGS_AGAIN, null, () -> {
                FlagGuard.again();
                Flags.forget();
                restartOwed();
                rebuild();
            }));
            column.addView(wrap(held));
        }

        if (flagList == null) {
            LinearLayout wait = card();
            wait.addView(quiet(Text.FLAGS_LOADING));
            column.addView(wrap(wait));
            if (flagsLoading) return;
            flagsLoading = true;


            new Thread(() -> {
                final java.util.List<Flags.Known> all = Flags.catalogue();
                runOnUiThread(() -> {
                    flagList = all;
                    flagsLoading = false;
                    if (settingsGroup == FLAGS_PAGE && !isFinishing()) rebuild();
                });
            }, "airtok-flags").start();
            return;
        }

        column.addView(flagSearch());

        java.util.List<Flags.Known> shown = new java.util.ArrayList<Flags.Known>();
        int found = 0;
        for (Flags.Known flag : flagList) {
            boolean wanted = flagQuery.length() == 0 ? mine.containsKey(flag.key)
                    : flag.key.toLowerCase(Locale.ROOT).contains(flagQuery);
            if (!wanted) continue;
            found++;
            if (shown.size() < FLAG_ROWS) shown.add(flag);
        }

        column.addView(section(flagQuery.length() == 0 ? Text.FLAGS_CHANGED
                : String.format(Locale.ROOT, Text.FLAGS_FOUND, found)));
        LinearLayout list = card();
        if (flagList.isEmpty()) list.addView(quiet(Text.FLAGS_NO_LIST));
        else if (shown.isEmpty()) list.addView(quiet(flagQuery.length() == 0
                ? String.format(Locale.ROOT, Text.FLAGS_NONE_CHANGED, flagList.size())
                : Text.NOTHING_FOUND));
        for (int i = 0; i < shown.size(); i++) {
            if (i > 0) list.addView(line());
            list.addView(flagRow(shown.get(i), mine.get(shown.get(i).key)));
        }
        if (found > shown.size()) {
            list.addView(quiet(String.format(Locale.ROOT, Text.FLAGS_MORE, shown.size())));
        }
        column.addView(wrap(list));

        if (!mine.isEmpty()) {
            column.addView(wrap(actionRow("restore", Text.FLAGS_RESET_ALL, null, () -> {
                Flags.clearOverrides();
                restartOwed();
                rebuild();
            })));
        }
    }

    private View flagSearch() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(20), dp(4), dp(8), dp(4));
        GradientDrawable background = new GradientDrawable();
        background.setColor(skin.card);
        background.setCornerRadius(dp(28));
        row.setBackground(background);

        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(flagQuery);
        input.setHint(Text.FLAGS_SEARCH);
        input.setHintTextColor(skin.muted());
        input.setTextColor(skin.text);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        input.setTypeface(Typeface.MONOSPACE);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        final Runnable look = () -> {
            flagQuery = input.getText().toString().trim().toLowerCase(Locale.ROOT);
            page.scrollTo(0, 0);
            rebuild();
        };
        input.setOnEditorActionListener((view, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEARCH) return false;
            look.run();
            return true;
        });
        row.addView(input, grow());

        TextView go = label(flagQuery.length() == 0 ? Text.SEARCH : Text.SEARCH_CLEAR);
        go.setTextColor(accentInk());
        go.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        go.setGravity(Gravity.CENTER);
        go.setPadding(dp(16), 0, dp(16), 0);
        go.setBackground(Expressive.tile(this, accentTonal(), (Accent.colour() & 0xFFFFFF) | 0x33000000,
                dp(20), dp(20), dp(10)));
        go.setOnClickListener(v -> {
            if (flagQuery.length() > 0) input.setText("");
            look.run();
        });
        row.addView(go, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)));
        LinearLayout box = new LinearLayout(this);
        box.setPadding(edge(), dp(4), edge(), dp(8));
        box.addView(sized(row, 56));
        return box;
    }


    private View flagRow(final Flags.Known flag, final Object mine) {
        LinearLayout row = row();
        row.setPadding(dp(16), dp(10), dp(16), dp(10));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        TextView name = label(flag.key);
        name.setTypeface(Typeface.MONOSPACE);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        words.addView(name);

        final Object own = flag.settable() ? Flags.own(flag) : null;
        final Object theirs = own != null ? own : flag.fallback;
        String says = flag.type.toLowerCase(Locale.ROOT) + "  ·  TikTok: " + flagValue(theirs);
        if (mine != null) says += "  ·  " + Text.FLAGS_HERE + ": " + flagValue(mine);
        TextView about = detail(says);
        about.setPadding(0, dp(4), 0, 0);
        if (mine != null) about.setTextColor(accentInk());
        words.addView(about);
        row.addView(words, grow());
        row.setOnClickListener(v -> editFlag(flag, theirs, mine));
        return sized(row, 56);
    }

    private static String flagValue(Object value) {
        if (value == null) return "—";
        String text = value instanceof Object[] ? java.util.Arrays.toString((Object[]) value)
                : String.valueOf(value);
        if (value instanceof String) text = '"' + text + '"';
        return text.length() > 48 ? text.substring(0, 48) + "…" : text;
    }

    private void editFlag(final Flags.Known flag, Object theirs, Object mine) {
        if (!flag.settable()) {
            Toast.makeText(this, Text.FLAGS_FIXED, Toast.LENGTH_SHORT).show();
            return;
        }
        final Panel panel = Panel.with(this, skin, flag.key);
        if (flag.type.equals("BOOLEAN")) {
            panel.option(Text.FLAGS_AS_TIKTOK + " (" + flagValue(theirs) + ")", mine == null, false,
                    () -> answerFlag(flag, null));
            panel.option(Text.FLAGS_ON, Boolean.TRUE.equals(mine), false,
                    () -> answerFlag(flag, Boolean.TRUE));
            panel.option(Text.FLAGS_OFF, Boolean.FALSE.equals(mine), false,
                    () -> answerFlag(flag, Boolean.FALSE));
            panel.quiet(Text.CLOSE, null);
            panel.show();
            return;
        }
        panel.text(flag.type.toLowerCase(Locale.ROOT) + "  ·  TikTok: " + flagValue(theirs));
        final EditText input = panel.field(String.valueOf(theirs));
        Object now = mine != null ? mine : theirs;
        if (now != null) input.setText(String.valueOf(now));
        if (!flag.type.equals("STRING")) {
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                    | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                    | (flag.type.equals("FLOAT") || flag.type.equals("DOUBLE")
                            ? android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL : 0));
        }
        panel.primaryKeepOpen(Text.APPLY, () -> {
            Object value = Flags.parse(flag, input.getText().toString());
            if (value == null) {
                Screen.say(Text.FLAGS_BAD);
                return;
            }
            panel.close();
            answerFlag(flag, value);
        });
        if (mine != null) panel.quiet(Text.FLAGS_RESET, () -> answerFlag(flag, null));
        panel.quiet(Text.CLOSE, null);
        panel.show();
    }

    private void answerFlag(Flags.Known flag, Object value) {
        Flags.override(flag, value);
        Diary.note("flag " + flag.key + " = " + (value == null ? "TikTok's own" : value));

        restartOwed();
        rebuild();
    }


    private View fullReportView() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(8), dp(16), dp(12));
        TextView view = new TextView(this);
        view.setText(Diagnostics.fullReport());
        view.setTextColor(skin.muted());
        view.setTypeface(Typeface.MONOSPACE);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        view.setTextIsSelectable(true);
        box.addView(view);
        return box;
    }

    private View diaryHead() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);



        LinearLayout row = row();
        row.addView(icon("article"));
        TextView title = label(Text.DIARY_TITLE);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(title, grow());
        row.addView(disclosure(diaryOpen,"diaryOpen"));
        row.setOnClickListener(v -> {
            disclosureKey = "diaryOpen";
            diaryOpen = !diaryOpen;
            rebuildAfterReveal(diaryOpen);
        });
        box.addView(sized(row, 52));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(dp(16), 0, dp(16), dp(8));

        TextView copy = diaryAction(Text.COPY);
        copy.setOnClickListener(v -> copyDiary());
        actions.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView clear = diaryAction(Text.CLEAR);
        clear.setOnClickListener(v -> {
            Diary.clear();
            Toast.makeText(this, Text.CLEARED, Toast.LENGTH_SHORT).show();
            rebuild();
        });
        LinearLayout.LayoutParams clearWhere = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clearWhere.leftMargin = dp(8);
        actions.addView(clear, clearWhere);
        box.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return box;
    }

    private TextView diaryAction(String text) {
        TextView action = new TextView(this);
        action.setText(text);
        action.setTextColor(Accent.colour());
        action.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        action.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        action.setGravity(Gravity.CENTER);
        action.setSingleLine(true);
        action.setEllipsize(android.text.TextUtils.TruncateAt.END);
        action.setPadding(dp(12), dp(10), dp(12), dp(10));
        action.setMinHeight(dp(44));
        action.setClickable(true);
        action.setFocusable(true);
        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(dp(22));
        pill.setColor((Accent.colour() & 0xFFFFFF) | 0x1F000000);
        action.setBackground(pill);
        Motion.press(action);
        return action;
    }

    private View diaryLines() {
        if (DevMode.isOn()) return fullReportView();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(8), dp(16), dp(12));
        for (String entry : Diary.lines()) {
            TextView view = new TextView(this);
            view.setText(entry);
            view.setTextColor(skin.muted());
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            view.setPadding(0, dp(2), 0, 0);
            box.addView(view);
        }
        return box;
    }


    private void restartTikTok() {
        try {
            android.content.Intent intent = getPackageManager()
                    .getLaunchIntentForPackage(getPackageName());
            if (intent == null) return;
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            Runtime.getRuntime().exit(0);
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }

    private void copyDiary() {
        try {
            StringBuilder out = new StringBuilder("MargyT\n");
            if (DevMode.isOn()) out.append(Diagnostics.fullReport());
            else for (String line : Diary.lines()) out.append(line).append('\n');
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("MargyT", out.toString()));
            Toast.makeText(this, Text.COPIED, Toast.LENGTH_SHORT).show();
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }



    private View backArrow() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        boolean m3 = !Design.frosted();
        if (m3) {
            bar.setPadding(dp(4), 0, dp(16), 0);
            bar.setMinimumHeight(dp(64));
        } else {
            bar.setPadding(dp(12), dp(8), dp(16), dp(4));
        }

        View button = Expressive.roundButton(this, SettingsGlyph.make("arrow_forward", skin.text),
                skin.card, (Accent.colour() & 0xFFFFFF) | 0x33000000, skin.text, m3 ? 40 : 48);
        ((ViewGroup) button).getChildAt(0).setScaleX(-1f);
        if (m3) {

            ((LinearLayout.LayoutParams) button.getLayoutParams()).setMargins(dp(4), dp(4), dp(4), dp(4));
            bar.addView(button);
            topBar = bar;
            bar.setContentDescription(settingsGroup == 0 ? "TikTok" : Local.of("Настройки"));
            bar.setOnClickListener(v -> goBack());
            return bar;
        }
        bar.addView(button);
        TextView name = new TextView(this);
        name.setText(settingsGroup == 0 ? "TikTok" : Local.of("Настройки"));
        name.setTextColor(skin.muted());
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        name.setPadding(dp(12), 0, 0, 0);
        bar.addView(name);
        bar.setContentDescription(settingsGroup == 0 ? "TikTok" : Local.of("Настройки"));
        bar.setOnClickListener(v -> goBack());
        return bar;
    }

    private static android.graphics.Bitmap launcherIcon;

    private android.graphics.drawable.Drawable launcherIcon() {
        if (launcherIcon == null) {
            int size = dp(32);
            android.graphics.Bitmap out = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(out);
            android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG | android.graphics.Paint.FILTER_BITMAP_FLAG);
            android.graphics.RectF whole = new android.graphics.RectF(0, 0, size, size);
            try {
                byte[] png = android.util.Base64.decode(Emblem.PNG, android.util.Base64.DEFAULT);
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(png, 0, png.length);
                android.graphics.Path round = new android.graphics.Path();
                round.addRoundRect(whole, size * 0.28f, size * 0.28f, android.graphics.Path.Direction.CW);
                canvas.clipPath(round);
                canvas.drawBitmap(bmp, null, whole, paint);
                launcherIcon = out;
                return new android.graphics.drawable.BitmapDrawable(getResources(), launcherIcon);
            } catch (Throwable ignored) {
            }
            paint.setColor(0xFFFE2C55);
            canvas.drawRoundRect(whole, size * 0.28f, size * 0.28f, paint);
            launcherIcon = out;
        }
        return new android.graphics.drawable.BitmapDrawable(getResources(), launcherIcon);
    }


    private LinearLayout topBar;

    private boolean searchOpen;

    private View title(String text) {
        final TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(skin.text);
        if (!Design.frosted() && topBar != null && topBar.getParent() == column) {

            M3Type.titleLarge(view);
            view.setSingleLine(true);
            view.setEllipsize(android.text.TextUtils.TruncateAt.END);
            view.setPadding(dp(4), 0, dp(8), 0);
            view.setOnClickListener(v -> { Cats.tapped(view); developerTap(); });
            if (settingsGroup == 0) {

                ImageView mark = new ImageView(this);
                mark.setImageDrawable(launcherIcon());
                LinearLayout.LayoutParams markSize = new LinearLayout.LayoutParams(dp(32), dp(32));
                markSize.leftMargin = dp(4);
                markSize.rightMargin = dp(8);
                topBar.addView(mark, markSize);
            }
            topBar.addView(view, grow());
            View none = new View(this); none.setVisibility(View.GONE);
            return none;
        }
        if (Design.frosted()) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, Design.fluent()?(settingsGroup==0?32:28):settingsGroup == 0 ? 44 : 34);
            view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            view.setPadding(dp(28), dp(8), dp(24), dp(16));
        } else {
            if (settingsGroup == 0) M3Type.displaySmall(view);
            else M3Type.headlineMedium(view);
            view.setPadding(dp(32), dp(12), dp(24), dp(20));
        }

        view.setOnClickListener(v -> {
            Cats.tapped(view);
            developerTap();
        });
        return view;
    }


    private View searchBox() {
        if (!Design.frosted() && topBar != null && topBar.getParent() == column) {
            final boolean open = searchOpen || searchQuery.length() > 0;
            ImageView button = new ImageView(this);
            button.setImageDrawable(open ? SettingsGlyph.make("close", skin.text) : new Expressive.Magnifier(skin.text));
            button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            button.setPadding(dp(12), dp(12), dp(12), dp(12));
            GradientDrawable round = new GradientDrawable();
            round.setShape(GradientDrawable.OVAL);
            round.setColor(0xFFFFFFFF);
            button.setBackground(new android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf((skin.text & 0xFFFFFF) | 0x22000000), null, round));
            button.setContentDescription(Text.SEARCH);
            button.setOnClickListener(v -> {
                if (open) { searchOpen = false; hideKeyboard(); runSearch(""); }
                else { searchOpen = true; rebuild(); }
            });
            topBar.addView(button, new LinearLayout.LayoutParams(dp(48), dp(48)));
            if (!open) { View none = new View(this); none.setVisibility(View.GONE); return none; }
            View box = searchField();
            if (searchQuery.length() == 0) {

                final EditText field = findEditText(box);
                if (field != null) field.post(() -> {
                    field.requestFocus();
                    android.view.inputmethod.InputMethodManager keys = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (keys != null) keys.showSoftInput(field, 0);
                });
            }
            return box;
        }
        return searchField();
    }

    private static EditText findEditText(View view) {
        if (view instanceof EditText) return (EditText) view;
        if (!(view instanceof ViewGroup)) return null;
        for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            EditText found = findEditText(((ViewGroup) view).getChildAt(i));
            if (found != null) return found;
        }
        return null;
    }

    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager keys = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();
        if (keys != null && focus != null) keys.hideSoftInputFromWindow(focus.getWindowToken(), 0);
    }

    private View searchField() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(Design.frosted() ? 20 : 16), dp(4), dp(8), dp(4));
        GradientDrawable background = new GradientDrawable();
        background.setColor(skin.card);
        background.setCornerRadius(dp(28));
        row.setBackground(background);

        ImageView glass = new ImageView(this);
        glass.setImageDrawable(new Expressive.Magnifier(skin.muted()));
        LinearLayout.LayoutParams glassSize = new LinearLayout.LayoutParams(dp(24), dp(24));
        glassSize.rightMargin = dp(Design.frosted() ? 12 : 16);
        row.addView(glass, glassSize);

        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(searchQuery);
        input.setSelectAllOnFocus(false);
        input.setHint(Text.SEARCH_HINT);
        input.setHintTextColor(skin.muted());
        input.setTextColor(skin.text);
        if (Design.frosted()) input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        else M3Type.line(input, 16, 0.5f);
        input.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        input.setOnEditorActionListener((view, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                runSearch(input.getText().toString());
                return true;
            }
            return false;
        });
        row.addView(input, grow());

        TextView go = label(searchQuery.length() == 0 ? Text.SEARCH : Text.SEARCH_CLEAR);
        go.setTextColor(accentInk());
        go.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        if (!Design.frosted()) M3Type.line(go, 14, 0.1f);
        go.setGravity(Gravity.CENTER);
        go.setPadding(dp(16), 0, dp(16), 0);
        go.setBackground(Expressive.tile(this, accentTonal(), (Accent.colour() & 0xFFFFFF) | 0x33000000,
                dp(20), dp(20), dp(Design.frosted() ? 10 : 8)));
        go.setOnClickListener(v -> {
            if (searchQuery.length() > 0) runSearch("");
            else runSearch(input.getText().toString());
        });
        row.addView(go, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)));
        LinearLayout box = new LinearLayout(this);
        box.setPadding(edge(), dp(4), edge(), dp(8));
        box.addView(sized(row, 56));
        return box;
    }

    private void runSearch(String words) {
        searchQuery = words == null ? "" : words.trim().toLowerCase(Locale.ROOT);
        rebuild();
    }


    private void chooseSeekStep(){
        new ModDialog.Builder(this).setItems(new String[]{"5","10","15","30","60",Text.CUSTOM_SPEED},(dialog,index)->{
            if(index<5){AccountAppearance.prefs(this).edit().putInt(PlayerTools.STEP,new int[]{5,10,15,30,60}[index]).apply();rebuild();return;}
            Panel panel=Panel.with(this,skin,Text.SEEK_STEP);android.widget.EditText input=panel.field("1–60");input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);input.setText(Integer.toString(PlayerTools.step()));
            panel.primaryKeepOpen(Text.APPLY,()->{try{int value=Integer.parseInt(input.getText().toString());if(value<1 || value>60){Screen.say("1–60");return;}AccountAppearance.prefs(this).edit().putInt(PlayerTools.STEP,value).apply();panel.close();rebuild();}catch(Throwable ignored){Screen.say("1–60");}});panel.quiet(Text.CLOSE,null);panel.show();
        }).show();
    }
    private void showHiddenItems() {
        Panel panel=Panel.with(this,skin,Text.HIDDEN_ITEMS);
        LinearLayout rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);
        String[] keys={Feed.KEY,Feed.KEY_LIVE,Feed.KEY_PHOTOS,Feed.KEY_STORIES,Feed.KEY_SUGGESTIONS};
        String[] labels={Text.HIDE_ADS,Text.HIDE_LIVE,Text.HIDE_PHOTOS,Text.HIDE_STORIES,Text.HIDE_SUGGESTIONS};
        int count=0;
        for(int i=0;i<keys.length;i++){
            final String key=keys[i];
            if(!(key.equals(Feed.KEY)?Feed.isEnabled():Feed.hides(key)))continue;
            count++;
            rows.addView(actionRow("visibility_off",labels[i],Text.HIDDEN_RESTORE,()->{
                if(key.equals(Feed.KEY))Feed.setEnabled(false);else Feed.setHides(key,false);
                panel.close();rebuild();Screen.say(Text.HIDDEN_NEXT_PAGE);
            }));
        }
        String[] railNames={Text.RAIL_LIKE,Text.RAIL_COMMENT,Text.RAIL_FAV,Text.RAIL_SHARE};
        for(int i=0;i<4;i++)if(!FeedRail.shown(i)){
            final int kind=i;count++;
            rows.addView(actionRow("visibility_off",railNames[i],Text.HIDDEN_RESTORE,()->{
                FeedRail.setShown(kind,true);panel.close();rebuild();
            }));
        }
        for(java.util.Map.Entry<Integer,String> card:FeedBlacklist.cards().entrySet()){
            final int type=card.getKey();count++;
            rows.addView(actionRow("group",card.getValue(),Text.HIDDEN_RESTORE,()->{FeedBlacklist.removeCard(type);panel.close();rebuild();}));
        }
        for(java.util.Map.Entry<String,String> author:FeedBlacklist.items().entrySet()){
            final String uid=author.getKey();count++;
            rows.addView(actionRow("group",author.getValue(),Text.HIDDEN_RESTORE,()->{FeedBlacklist.remove(uid);panel.close();rebuild();}));
        }
        if(count==0)rows.addView(caption(Text.HIDDEN_EMPTY));
        else {
            rows.addView(caption(Text.HIDDEN_NEXT_PAGE));
            panel.primary(Text.HIDDEN_RESTORE_ALL,()->{
                Feed.setEnabled(false);Feed.setHides(Feed.KEY_LIVE,false);Feed.setHides(Feed.KEY_PHOTOS,false);
                Feed.setHides(Feed.KEY_STORIES,false);Feed.setHides(Feed.KEY_SUGGESTIONS,false);FeedBlacklist.clear();
                for(int i=0;i<4;i++)FeedRail.setShown(i,true);
                rebuild();
            });
        }
        panel.view(rows);panel.quiet(Text.CLOSE,null);panel.show();
    }


    private static final class Found {
        final String section, title, words;
        Found(String section, String title, String words) { this.section = section; this.title = title; this.words = words; }
    }


    private java.util.List<Found> harvest(int from) {
        java.util.List<Found> out = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        String section = null;
        for (int i = from; i < column.getChildCount(); i++) {
            View item = column.getChildAt(i);
            if (item instanceof TextView && sectionTargets.containsValue(item)) {
                section = String.valueOf(((TextView) item).getText());
                continue;
            }
            if (section == null || (Version.LITE && !inLite(section))) continue;
            collect(item, section, out, seen);
        }
        return out;
    }

    private void collect(View view, String section, java.util.List<Found> out, java.util.Set<String> seen) {
        if (view == null || view.getVisibility() == View.GONE) return;
        LinearLayout row = functionRow(view);
        if (row != null) {
            String title = String.valueOf(firstText(row).getText()).trim();
            StringBuilder words = new StringBuilder();
            texts(row, words);
            Object note = row.getTag(INFO);
            if (note instanceof String[]) words.append(' ').append(((String[]) note)[0]);
            if (title.length() > 1 && seen.add(section + "\n" + title)) out.add(new Found(section, title, words.toString()));
            return;
        }
        if (view instanceof TextView) {
            String text = String.valueOf(((TextView) view).getText()).trim();
            if (text.length() > 2 && seen.add(section + "\n" + text)) out.add(new Found(section, text, text));
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collect(group.getChildAt(i), section, out, seen);
        }
    }

    private static void texts(View view, StringBuilder into) {
        if (view instanceof TextView) into.append(' ').append(((TextView) view).getText());
        else if (view instanceof ViewGroup)
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) texts(((ViewGroup) view).getChildAt(i), into);
    }


    private static String plain(String words) {
        return words == null ? "" : words.toLowerCase(Locale.ROOT).replace('ё', 'е');
    }


    private boolean matches(String haystack) {
        String hay = plain(haystack);
        for (String word : plain(searchQuery).split("\\s+")) {
            if (word.length() == 0) continue;
            if (hay.contains(word)) continue;
            if (word.length() > 5 && hay.contains(word.substring(0, word.length() - 2))) continue;
            return false;
        }
        return true;
    }

    private void searchResults(java.util.List<Found> index) {
        column.addView(section(Text.SEARCH_RESULTS));
        LinearLayout results = card();
        int found = 0;

        for (Shelf shelf : shelves()) {
            if (Version.LITE && shelf.sections.length > 0 && !inLite(shelf.sections[0])) continue;
            if (!matches(shelf.title + " " + shelf.note)) continue;
            final Shelf to = shelf;
            results.addView(actionRow(shelf.icon, shelf.title, groupName(shelf.group), () -> {
                searchQuery = "";
                settingsGroup = to.group;
                settingsPage = to.id;
                page.scrollTo(0, 0);
                navigate();
            }));
            found++;
        }

        java.util.List<Found> named = new java.util.ArrayList<>(), mentioned = new java.util.ArrayList<>();
        for (Found one : index) {
            if (matches(one.title)) named.add(one);
            else if (matches(one.title + " " + one.words + " " + one.section)) mentioned.add(one);
        }
        named.addAll(mentioned);
        for (int i = 0; i < named.size() && i < 40; i++) {
            final Found one = named.get(i);
            Shelf home = shelfOfSection(one.section);
            String where = home == null ? one.section : home.title.equals(one.section) ? home.title : home.title + " · " + one.section;
            String title = one.title.length() > 70 ? one.title.substring(0, 68).trim() + "…" : one.title;
            results.addView(actionRow("search", title, where, () -> openFound(one)));
            found++;
        }
        if (found == 0) {

            found += searchHit(results, Text.REGION, Text.SEARCH_REGION, () -> { });
            found += searchHit(results, Text.THEME, Text.SEARCH_ACCENT, () -> { themeColoursOpen = true; });
            found += searchHit(results, Text.THEME, Text.SEARCH_THEME, () -> { themeColoursOpen = true; });
            found += searchHit(results, Text.FEED, Text.SEARCH_FEED, () -> { });
            found += searchHit(results, Text.VIDEO, Text.SEARCH_VIDEO, () -> { });
            found += searchHit(results, Text.DOWNLOADS, Text.SEARCH_DOWNLOADS, () -> { });
            found += searchHit(results, Text.BACKUP, Text.SEARCH_BACKUP, () -> { });
            found += searchHit(results, Text.FONT, Text.SEARCH_FONT, () -> { fontOpen = true; });
            found += searchHit(results, Text.BADGES, Text.SEARCH_BADGES, () -> { mineOpen = true; });
        }
        if (found == 0) results.addView(caption(Text.SEARCH_NONE));
        column.addView(wrap(results));
    }


    private void openFound(Found one) {
        searchQuery = "";
        settingsGroup = groupOf(one.section);
        Shelf to = shelfOfSection(one.section);
        settingsPage = to == null ? null : to.id;
        page.scrollTo(0, 0);
        navigate();
        page.post(() -> {
            View target = shown(column, one.title);
            if (target == null) target = sectionTargets.get(one.section);
            if (target == null) return;
            View row = target.getParent() instanceof LinearLayout ? (View) target.getParent() : target;
            int offset = 0;
            View cursor = target;
            while (cursor != column && cursor != null) {
                offset += cursor.getTop();
                if (!(cursor.getParent() instanceof View)) break;
                cursor = (View) cursor.getParent();
            }
            page.smoothScrollTo(0, Math.max(0, offset - dp(96)));
            Motion.change(row);
        });
    }


    private static View shown(View view, String text) {
        if (view == null || view.getVisibility() != View.VISIBLE) return null;
        if (view instanceof TextView) return text.equals(String.valueOf(((TextView) view).getText()).trim()) ? view : null;
        if (view instanceof ViewGroup)
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View hit = shown(((ViewGroup) view).getChildAt(i), text);
                if (hit != null) return hit;
            }
        return null;
    }

    private int searchHit(LinearLayout into, String title, String words, final Runnable open) {
        if (!matchesSearch(title + " " + words)) return 0;
        if (into.getChildCount() > 0) into.addView(line());
        into.addView(actionRow("search", title, currentValue(title), () -> {
            open.run();
            searchQuery = "";
            settingsGroup = groupOf(title);
            Shelf to = shelfOfSection(title);
            settingsPage = to == null ? null : to.id;
            page.scrollTo(0, 0);
            rebuild();
            final View target = sectionTargets.get(title);
            if (target != null) page.post(() -> page.smoothScrollTo(0, target.getTop()));
        }));
        return 1;
    }

    private String currentValue(String title){
        if(title.equals(Text.ACCENT))return String.format(Locale.US,"#%06X",Accent.colour()&0xFFFFFF);
        if(title.equals(Text.THEME))return Themes.isEnabled()?Local.of("Включена"):Local.of("Выключена");
        if(title.equals(Text.FEED))return Feed.isEnabled()?Local.of("Реклама скрыта"):Local.of("Реклама разрешена");
        if(title.equals(Text.DOWNLOADS))return Download.isEnabled()?Local.of("Без водяного знака"):Local.of("Обычное сохранение");
        if(title.equals(Text.PROFILE_STYLE))return Account.profileHandle()==null?Local.of("Откройте свой профиль"):"@"+Account.profileHandle();
        return Local.of("Открыть настройки");
    }
    private boolean matchesSearch(String words) {
        return words.toLowerCase(Locale.ROOT).contains(searchQuery);
    }

    private View section(String text) {
        renderDisclosureKey=null;
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(accentInk());
        if (Design.frosted()) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            view.setLetterSpacing(0.04f);
            view.setPadding(dp(28), dp(24), dp(16), dp(8));
        } else {
            M3Type.titleSmall(view);
            view.setPadding(dp(32), dp(20), dp(16), dp(8));
        }
        sectionTargets.put(text, view);
        return view;
    }

    private String groupName(int group) {
        switch (group) {
            case 1: return Local.of("Оформление");
            case 2: return Local.of("Профиль");
            case 3: return Local.of("Лента и видео");
            case 4: return Local.of("Скачивание медиа");
            case 5: return Local.of("О проекте");
            case 6: return Local.of("Умный плеер и режим чистого экрана");
            case 7: return Local.of("Чаты, серии, уведомления");
            case 8: return Text.FUN;
            default: return "AirTok";
        }
    }

    private void showSectionHelp() {
        String[] notes = {"", Local.of("Акцент меняет цвет кнопок и вкладок. Тема и шрифты отдельно."),
                Local.of("Значки и градиент на ник. Сначала надо один раз подтвердить аккаунт."),
                Local.of("Удержание «Отправить» открывает скачивание и перемотку, удержание края видео ускоряет его."),
                Local.of("Скачивание, копии настроек, диагностика и журнал."),
                Local.of("Здесь находятся версия мода, обновления, журнал и участники."), Local.of("Магазин и настройки плагинов. Ставь только те, которым доверяешь."),
                Local.of("Автопродление серий и уведомления мода."),
                Text.FUN_HELP};
        Panel.with(this,skin,Local.of("Подсказки")).text(notes[Math.max(0,Math.min(notes.length-1,settingsGroup))])
                .quiet(Local.of("Обучение"),() -> Tutorial.show(this)).primary(Local.of("Понятно"),null).show();
    }

    private View groupNavigation() {
        LinearLayout list = card();
        String[] icons = {"palette", "group", "play_circle", "download", "info", "tune", "chat", "flare"};
        String[] notes = {Local.of("Акцент и шрифт"),
                "",
                Local.of("Регион, лента и защита экрана"),
                Local.of("Без водяного знака, 1080p и аватарки"),
                Local.of("Создатель и благодарности"),
                Local.of("Шкала перемотки, чистый экран и плеер"),
                Local.of("Автопродление серий и уведомления"),
                ""};


        int[] shelf = new int[] {1, 3, 4, 6, 7, 5};
        for (int n = 0; n < shelf.length; n++) {
            final int i = shelf[n];
            final int chosen = i;
            if (n > 0) list.addView(line());
            LinearLayout entry = row();
            entry.setPadding(dp(16),dp(14),dp(20),dp(14));
            entry.addView(icon(icons[i-1],false,Design.fluent()?32:Design.frosted()?52:40));
            LinearLayout words = new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL);
            TextView head = label(groupName(i)); head.setTextSize(Design.fluent()?16:Design.frosted()?19:17);
            head.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
            words.addView(head);words.addView(detail(notes[i-1]));entry.setContentDescription(groupName(i)+". "+notes[i-1]);
            entry.addView(words,grow()); entry.addView(away());
            entry.setMinimumHeight(dp(Design.frosted()?80:72));Motion.touch(entry);
            entry.setOnClickListener(v -> {
                if(navigating)return;navigating=true;
                Motion.navigate(column,1f,()->{
                    settingsGroup=chosen;
                    java.util.List<Shelf> p = shelvesOf(chosen);
                    settingsPage = p.isEmpty() ? null : p.get(0).id;
                    page.scrollTo(0,0);
                    navigate();
                    navigating=false;
                });
            });
            list.addView(entry);
        }
        return wrap(list);
    }


    private int groupOf(String section) {
        Shelf one = shelfOfSection(section);
        return one == null ? 4 : one.group;
    }


    private View shelfList(java.util.List<Shelf> pages) {
        LinearLayout list = card();
        for (int n = 0; n < pages.size(); n++) {
            final Shelf one = pages.get(n);
            if (n > 0) list.addView(line());
            LinearLayout entry = row();
            entry.setPadding(dp(16), dp(12), dp(20), dp(12));
            entry.addView(icon(one.icon, false, Design.fluent() ? 32 : 44));
            LinearLayout words = new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL);
            TextView head = label(one.title); head.setTextSize(Design.fluent() ? 15 : Design.frosted() ? 17 : 16);
            head.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            words.addView(head);
            if (one.note.length() > 0) words.addView(detail(one.note));
            entry.setContentDescription(one.title + ". " + one.note);
            entry.addView(words, grow()); entry.addView(away());
            entry.setMinimumHeight(dp(72)); Motion.touch(entry);
            entry.setOnClickListener(v -> {

                if ("thanks".equals(one.id)) { showThanks(); return; }
                if (navigating) return; navigating = true;
                Motion.navigate(column, 1f, () -> { settingsPage = one.id; page.scrollTo(0, 0); navigate(); navigating = false; });
            });
            list.addView(entry);
        }
        return wrap(list);
    }


    private View lead(String words) {
        TextView view = new TextView(this);
        view.setText(words);
        view.setTextColor(skin.muted());
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        view.setLineSpacing(0, 1.1f);
        view.setPadding(dp(24), 0, dp(24), dp(10));
        return view;
    }


    private java.util.Set<String> smallSections(int contentStart) {
        java.util.Map<String, Integer> rows = new java.util.HashMap<>();
        java.util.Set<String> odd = new java.util.HashSet<>();
        String section = null;
        for (int i = contentStart; i < column.getChildCount(); i++) {
            View item = column.getChildAt(i);
            if (item instanceof TextView && sectionTargets.containsValue(item)) {
                section = ((TextView) item).getText().toString();
                if (!rows.containsKey(section)) rows.put(section, 0);
                continue;
            }
            if (section == null) continue;
            int count = 0;
            boolean other = false;
            if (item instanceof ViewGroup && ((ViewGroup) item).getChildCount() > 0
                    && ((ViewGroup) item).getChildAt(0) instanceof Expressive.Group) {
                ViewGroup card = (ViewGroup) ((ViewGroup) item).getChildAt(0);
                for (int k = 0; k < card.getChildCount(); k++) {
                    View row = card.getChildAt(k);
                    if (row.getVisibility() == View.GONE) continue;
                    if (functionRow(row) != null || holdsSlider(row)) count++;

                    else if (!(row instanceof TextView) && !(row instanceof SettingsPictures.Art)) other = true;
                }
            } else if (item.getTag(LEAD) == null) other = true;
            rows.put(section, rows.get(section) + count);
            if (other) odd.add(section);
        }
        java.util.Set<String> small = new java.util.HashSet<>();
        for (java.util.Map.Entry<String, Integer> one : rows.entrySet())
            if (one.getValue() >= 1 && one.getValue() <= 2 && !odd.contains(one.getKey())) small.add(one.getKey());


        small.add(MOTION_BLUR);
        small.add(CLEAN_LINKS);
        return small;
    }


    private static boolean holdsSlider(View row) {
        if (row instanceof android.widget.SeekBar) return true;
        if (!(row instanceof ViewGroup)) return false;
        ViewGroup group = (ViewGroup) row;
        for (int i = 0; i < group.getChildCount() && i < 4; i++) if (group.getChildAt(i) instanceof android.widget.SeekBar) return true;
        return false;
    }


    private void filterSections(int from, java.util.Set<String> sections) {
        boolean shown = false;
        for (int i = from; i < column.getChildCount(); i++) {
            View item = column.getChildAt(i);
            if (item instanceof TextView && sectionTargets.containsValue(item)) {
                String section = ((TextView) item).getText().toString();
                shown = sections.contains(section) && (!Version.LITE || inLite(section));
            } else if (item.getTag(LEAD) != null) continue;
            item.setVisibility(shown ? View.VISIBLE : View.GONE);
        }
    }

    private void filterGroup(int contentStart) {
        Shelf open = shelf(settingsPage);
        boolean shown = false, offered = true;
        for (int i = contentStart; i < column.getChildCount(); i++) {
            View item = column.getChildAt(i);
            if (item instanceof TextView && sectionTargets.containsValue(item)) {
                String section = ((TextView) item).getText().toString();
                shown = false;
                if (open != null) for (String name : open.sections) shown |= name.equals(section);
                offered = !Version.LITE || inLite(section);

                if (shown && open != null && open.sections.length == 1) { item.setVisibility(View.GONE); continue; }
            } else if (item.getTag(LEAD) != null) continue;
            item.setVisibility(shown && offered ? View.VISIBLE : View.GONE);
        }
    }
    private static final int LEAD = 0x7e0c0340;


    private static boolean inLite(String section) {
        return section.equals(Text.REGION) || section.equals(Text.COUNTRY)
                || section.equals(Text.ACCENT) || section.equals(Text.THEME)
                || section.equals(Text.DOWNLOADS) || section.equals(Text.BADGES) || section.equals(Text.ACCOUNT)
                || section.equals(Text.FUN_SCREAMERS) || section.equals(Text.FUN_CAT) || section.equals(Text.FUN_LAG)
                || section.equals(Text.DIAGNOSTICS) || section.equals(Text.DIARY)
                || section.equals(Text.THANKS) || section.equals(Text.LINKS) || section.equals(Text.CHANNEL);
    }

    private static final String BURN = Local.of("Защита экрана");
    private static final String FRAMES = Local.of("Рамки аватарки"),
            FRAMES_NOTE = Local.of("Рамка вокруг аватарки, её видят все с модом"),
            MOTION = Local.of("Защита от укачивания"),
            MOTION_NOTE = Local.of("Точки на экране, чтобы не тошнило в дороге");
    private static final boolean EDITOR_READY = false;
    private static final String EDITOR = Local.of("Редактор интерфейса"),
            EDITOR_NOTE = Local.of("Прячь, двигай и перекрашивай что угодно"),
            CLEAN_LINKS = Local.of("Чистые ссылки"),
            CLEAN_LINKS_NOTE = Local.of("Ссылки на видео без меток"),
            MOTION_BLUR = Local.of("Моушен блюр"),
            MOTION_BLUR_NOTE = Local.of("Размытие всего, что движется"),
            STATS = Local.of("Статистика видео"),
            STATS_NOTE = Local.of("Подробные цифры любого ролика"),
            TYPING = Local.of("Анимация набора текста"),
            TYPING_NOTE = Local.of("Буквы пружинят, пока печатаешь"),
            REPOSTS = Local.of("Мои репосты"),
            REPOSTS_NOTE = Local.of("Удалить все репосты разом"),
            MESSAGES = Local.of("Личные сообщения"),
            MESSAGES_NOTE = Local.of("Папки чатов и полоса историй"),
            INBOX_CLEAN = Local.of("Что скрыть во «Входящих»"),
            GHOST = Local.of("Режим призрака"),
            GHOST_NOTE = Local.of("Без «в сети», «прочитано» и «печатает»");
    private static final String LOTUSTOK = "https://github.com/pikmis/lotustok";
    private static final String SANEK = "https://t.me/cahecheck";
    private static final String SPONSOR = Local.of("Реклама в видео"),
            SPONSOR_NOTE = Local.of("Пропуск рекламы, которую читают авторы");

    private boolean navigating;

    private java.util.List<View> built;
    private boolean keepPages;


    private void prebuild() {
        if (built != null || isFinishing()) return;
        LinearLayout real = column;
        LinearLayout scratch = new LinearLayout(this);
        scratch.setOrientation(LinearLayout.VERTICAL);
        column = scratch;
        try {
            sectionTargets.clear();settingTargets.clear();
            disclosureBodies.clear();disclosureArrows.clear();
            buildContent();
            java.util.List<View> pages = new java.util.ArrayList<>();
            for (int i = 0; i < scratch.getChildCount(); i++) pages.add(scratch.getChildAt(i));
            scratch.removeAllViews();
            built = pages;
        } catch (Throwable error) {
            built = null;
            Diary.note("settings prebuild: " + error);
        } finally {
            column = real;
        }
    }


    private void navigate() {
        keepPages = true;
        rebuild();
    }
    private void goBack() {
        if(navigating)return;
        if (settingsGroup == STATS_PAGE) { finish(); return; }
        if (searchQuery.length() > 0) { searchQuery = ""; rebuild(); }
        else if (settingsGroup == COUNTRY_PAGE) {

            navigating=true;Motion.navigate(column,-1f,()->{settingsGroup=countryFromGroup;settingsPage=countryFromPage;page.scrollTo(0,0);rebuild();navigating=false;});
        }
        else if (settingsGroup != 0) {
            navigating=true;Motion.navigate(column,-1f,()->{settingsGroup=0;settingsPage=null;page.scrollTo(0,0);navigate();navigating=false;});
        }
        else finish();
    }

    @Override public void onBackPressed() { goBack(); }

    @Override protected void onDestroy() {
        Badges.stopTelling(badgeUpdates);
        if (column != null) { column.removeCallbacks(delayedRebuild); column.animate().withEndAction(null).cancel(); }
        if (page != null) {
            if (scrollRestore != null) page.removeCallbacks(scrollRestore);
            if (scrollRestoreListener != null && page.getViewTreeObserver().isAlive())
                page.getViewTreeObserver().removeOnPreDrawListener(scrollRestoreListener);
        }
        super.onDestroy();
    }

    private View caption(String message) {
        TextView view = new TextView(this);
        view.setText(message);
        view.setTextColor(skin.muted());
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        view.setPadding(dp(16), dp(12), dp(16), dp(4));
        return view;
    }


    private View designPicker() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout head = row();
        head.addView(icon("palette"));
        head.addView(label(Text.DESIGN), grow());
        box.addView(sized(head, 52));
        java.util.List<UiStyle> styles = new java.util.ArrayList<>();
        styles.add(UiStyle.MATERIAL);
        styles.add(UiStyle.LIQUID);
        LinearLayout strip = new LinearLayout(this);
        strip.setPadding(dp(12), 0, dp(12), dp(14));
        String[] bases = {"md3", "glass"};
        for (final UiStyle style : styles) {
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER_HORIZONTAL);
            boolean on = style.key.equals(Design.key());
            tile.addView(new SettingsArt.Style(this, skin, bases[style.base], on));
            TextView name = new TextView(this);
            name.setText(style.key.equals("md3") ? Local.of("Стандартный") : style.name);
            M3Type.line(name, 12, 0.4f);
            name.setTextColor(on ? skin.text : skin.muted());
            name.setTypeface(Typeface.create(on ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
            name.setGravity(Gravity.CENTER);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            name.setPadding(0, dp(6), 0, 0);
            tile.addView(name);
            tile.setPadding(dp(3), 0, dp(3), 0);
            tile.setOnClickListener(v -> { if (!style.key.equals(Design.key())) applyDesign(style.key); });
            Motion.press(tile);
            strip.addView(tile, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        box.addView(strip);
        return box;
    }

    private static final int PICK_STYLE = 0x4D55;
    private static final int CREATE_STYLE = 0x4D56;

    private void pickStyle() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            intent.setType("*/*");
            startActivityForResult(intent, PICK_STYLE);
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }

    private void saveStyle() {
        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/octet-stream");
            intent.putExtra(Intent.EXTRA_TITLE, UiStyle.fileName(Design.style().name));
            startActivityForResult(intent, CREATE_STYLE);
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }

    private void reviewStyle(final Uri source) {
        Net.away("style review", () -> {
            String trouble = null;
            byte[] raw = null;
            UiStyle style = null;
            try (java.io.InputStream in = getContentResolver().openInputStream(source)) {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                for (int n; in != null && (n = in.read(chunk)) > 0 && out.size() <= 40 * 1024; ) out.write(chunk, 0, n);
                raw = out.toByteArray();
                style = UiStyle.read(raw, null);
            } catch (UiStyle.Refused refused) {
                trouble = refused.getMessage();
            } catch (Throwable error) {
                trouble = Text.UI_FILE_UNREADABLE;
            }
            final UiStyle found = style;
            final byte[] file = raw;
            final String why = trouble;
            runOnUiThread(() -> {
                if (isFinishing()) return;
                if (found == null) {
                    Panel.with(this, skin, Text.UI_FILE_REFUSED).text(why).quiet(Local.of("Назад"), null).show();
                    return;
                }
                String[] bases = {"Material 3", "Liquid Glass", "Fluent"};
                StringBuilder about = new StringBuilder(found.name);
                if (!found.author.isEmpty()) about.append('\n').append(Text.UI_FILE_BY).append(' ').append(found.author);
                about.append('\n').append(Text.UI_FILE_BASED).append(' ').append(bases[found.base]);
                if (!found.description.isEmpty()) about.append("\n\n").append(found.description);
                Panel.with(this, skin, Text.UI_FILE_REVIEW).text(about.toString())
                        .primary(Text.UI_FILE_APPLY, () -> {
                            try {
                                UiStyle installed = UiStyle.install(this, file);
                                applyDesign(installed.key);
                                Screen.say(Text.UI_FILE_APPLIED);
                            } catch (UiStyle.Refused refused) {
                                Screen.say(refused.getMessage());
                            }
                        })
                        .quiet(Local.of("Назад"), null).show();
            });
        });
    }

    private void askToRemoveStyle(final UiStyle style) {
        Panel.with(this, skin, Text.UI_FILE_REMOVE).text(Text.UI_FILE_REMOVE_ASK + "\n\n" + style.name)
                .primary(Text.UI_FILE_REMOVE, () -> {
                    boolean wearing = style.key.equals(Design.key());
                    UiStyle.remove(this, style.key);
                    if (wearing) applyDesign("md3"); else rebuild();
                })
                .quiet(Local.of("Назад"), null).show();
    }

    private void chooseDesign() {
        Panel panel=Panel.with(this,skin,Text.DESIGN);
        panel.text(Text.DESIGN_NOTE).section(null);
        panel.option(Local.of("Стандартный"),"md3".equals(Design.key()),true,()->applyDesign("md3"));
        panel.option("Liquid Glass","glass".equals(Design.key()),true,()->applyDesign("glass"));
        panel.show();
    }

    private void applyDesign(String mode) {
        Design.choose(this,mode);
        NavBar.reloadSettings();
        dressTheWindow();
        Design.page(this,root,skin.page);
        rebuild();
        root.post(()->Design.own(root));
        restartOwed();
        Panel.with(this, skin, Local.of("Смена темы"))
                .text(Local.of("Для полного применения темы к нижней панели и кнопкам требуется перезапуск."))
                .primary(Local.of("Перезапустить сейчас"), this::restartTikTok)
                .quiet(Local.of("Позже"), null)
                .show();
    }

    private LinearLayout card() {
        return new Expressive.Group(this, skin.card, (Accent.colour() & 0xFFFFFF) | 0x33000000);
    }


    private static final int CAPTION = 0x7e0c0440, INFO = 0x7e0c0441;


    private void explain(ViewGroup card) {
        for (int i = 0; i < card.getChildCount(); i++) {
            Object note = card.getChildAt(i).getTag(CAPTION);
            if (!(note instanceof String) || ((String) note).length() == 0) continue;
            LinearLayout row = null;
            for (int k = i - 1; k >= 0 && row == null; k--) row = functionRow(card.getChildAt(k));
            for (int k = i + 1; k < card.getChildCount() && row == null; k++) row = functionRow(card.getChildAt(k));
            if (row == null) continue;
            card.removeViewAt(i);
            i--;
            info(row, (String) note);
        }
    }


    private LinearLayout functionRow(View view) {
        if (!(view instanceof LinearLayout) || view.getVisibility() != View.VISIBLE) return null;
        LinearLayout row = (LinearLayout) view;
        if (row.getOrientation() != LinearLayout.HORIZONTAL || row.getChildCount() < 2) return null;
        return firstText(row) != null ? row : null;
    }

    private void info(LinearLayout row, final String note) {
        Object had = row.getTag(INFO);
        if (had instanceof String[]) {
            ((String[]) had)[0] = ((String[]) had)[0] + "\n\n" + note;
            return;
        }
        final String[] said = {note};
        row.setTag(INFO, said);
        int after = row.getChildCount() - 1;
        for (int i = 0; i < row.getChildCount(); i++) {
            if (firstText(row.getChildAt(i)) != null) {
                after = i;
                break;
            }
        }
        TextView name = firstText(row);
        final String title = name == null ? "" : String.valueOf(name.getText());
        android.widget.ImageView button = new android.widget.ImageView(this);
        button.setImageDrawable(SettingsGlyph.make("info", skin.muted()));
        button.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable round = new GradientDrawable();
        round.setShape(GradientDrawable.OVAL);
        round.setColor(0xFFFFFFFF);
        button.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf((Accent.colour() & 0xFFFFFF) | 0x33000000), null, round));
        button.setContentDescription(title);
        button.setOnClickListener(v -> Panel.with(this, skin, title).text(said[0]).primary(Local.of("Понятно"), null).show());
        LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(dp(40), dp(40));
        place.rightMargin = dp(4);
        row.addView(button, Math.min(after + 1, row.getChildCount()), place);
    }

    private static TextView firstText(View view) {
        if (view instanceof TextView) return (TextView) view;
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            TextView found = firstText(group.getChildAt(i));
            if (found != null) return found;
        }
        return null;
    }


    private View wrap(View card) {
        if (card instanceof Expressive.Group) explain((ViewGroup) card);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(edge(), 0, edge(), dp(6));
        if (!(card instanceof Expressive.Group)) {
            LinearLayout single = card();
            single.addView(card);
            card = single;
        }
        box.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return box;
    }


    private int edge() {
        return dp(Design.frosted() ? 12 : 16);
    }


    private int accentInk() {
        return skin.dark() ? Expressive.mix(Accent.colour(), 0xFFFFFFFF, 0.45f)
                : Expressive.mix(Accent.colour(), 0xFF000000, 0.35f);
    }


    private int iconInk() {
        return Expressive.mix(skin.card, skin.text, 0.78f);
    }


    private int accentTonal() {
        return Expressive.mix(skin.card, Accent.colour(), 0.24f);
    }

    private View line() {
        View spacer=new View(this);spacer.setVisibility(View.GONE);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0,0));return spacer;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        if (Design.frosted()) row.setPadding(dp(16), dp(4), dp(20), dp(4));
        else row.setPadding(dp(16), dp(10), dp(16), dp(10));
        row.setMinimumHeight(dp(56));
        Motion.press(row);
        return row;
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(skin.text);
        if (Design.frosted()) view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        else M3Type.line(view, 16, 0.5f);
        view.setIncludeFontPadding(false);view.setPadding(0,0,dp(12),0);
        return view;
    }

    private TextView detail(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(skin.muted());
        if (Design.frosted()) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        } else {
            M3Type.bodyMedium(view);
            view.setPadding(0, dp(2), 0, 0);
        }
        view.setIncludeFontPadding(false);
        return view;
    }

    private LinearLayout.LayoutParams grow() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private View sized(View view, int height) {
        if (height > 1 && !Design.frosted()) height = height <= 56 ? 56 : height <= 72 ? 72 : 88;
        if (height > 1) view.setMinimumHeight(dp(Math.max(56, height)));
        view.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height <= 1 ? dp(height) : ViewGroup.LayoutParams.WRAP_CONTENT));
        return view;
    }


    private View restartBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        styleRestartBar(bar);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(20), dp(10), dp(12), dp(10));

        RestartSymbol symbol = new RestartSymbol(this, Accent.colour());
        LinearLayout.LayoutParams symbolSize = new LinearLayout.LayoutParams(dp(24),dp(24));
        symbolSize.rightMargin = dp(12);
        content.addView(symbol,symbolSize);

        TextView why = new TextView(this);
        why.setText(Text.RESTART_PENDING);
        why.setTextColor(skin.text);
        why.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        content.addView(why, grow());

        TextView button = new TextView(this);
        button.setText(Text.RESTART);
        button.setTextColor(onAccent());
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setIncludeFontPadding(false);button.setGravity(Gravity.CENTER);
        button.setPadding(dp(16),0,dp(16),0);button.setMinWidth(dp(132));
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(Accent.colour());
        pill.setCornerRadius(dp(24));
        button.setMinHeight(dp(48));
        button.setFocusable(true);
        button.setBackground(pill);
        button.setOnClickListener(v -> restartTikTok());
        LinearLayout.LayoutParams actionSize=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,dp(48));actionSize.leftMargin=dp(12);content.addView(button,actionSize);
        why.setIncludeFontPadding(false);why.setMaxLines(2);why.setGravity(Gravity.CENTER_VERTICAL);
        restartSymbol=symbol;restartWhy=why;restartAction=button;restartBackground=pill;
        Fonts.applyTree(content);Motion.press(button);
        bar.addView(content);
        return bar;
    }


    private int onAccent() {
        int colour = Accent.colour();
        int red = (colour >> 16) & 0xFF, green = (colour >> 8) & 0xFF, blue = colour & 0xFF;
        int brightness = (red * 299 + green * 587 + blue * 114) / 1000;
        return brightness > 150 ? 0xFF1C2C24 : 0xFFFFFFFF;
    }


    private int navigationBar() {
        try {
            android.view.WindowInsets insets = getWindow().getDecorView().getRootWindowInsets();
            if (insets == null) return 0;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                return insets.getInsets(
                        android.view.WindowInsets.Type.navigationBars()).bottom;
            }
            return insets.getStableInsetBottom();
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private int statusBar() {
        try {
            android.view.WindowInsets insets = getWindow().getDecorView().getRootWindowInsets();
            int top = 0;
            if (insets != null) {
                top = android.os.Build.VERSION.SDK_INT >= 30
                        ? insets.getInsets(
                                android.view.WindowInsets.Type.statusBars()).top
                        : insets.getStableInsetTop();
            }
            if (top > 0) return top;
        } catch (Throwable ignored) {
        }
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(24);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }



    private interface ColourChanged { void changed(int colour); }


    private static final class AccentColourPicker extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] hsv = new float[3];
        private final int[] rainbow = {Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN,
                Color.BLUE, 0xFFFF00FF, Color.RED};
        private ColourChanged changed;

        AccentColourPicker(Context context, int initial) {
            super(context);
            Color.colorToHSV(initial, hsv);
            setContentDescription(Text.ACCENT_PICKER);
        }

        void onColourChanged(ColourChanged listener) { changed = listener; }

        int colour() { return Color.HSVToColor(hsv); }
        void setColour(int colour){Color.colorToHSV(colour,hsv);invalidate();if(changed!=null)changed.changed(colour);}

        private float dp(float value) {
            return value * getResources().getDisplayMetrics().density;
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float pad = dp(10);
            float left = pad, right = getWidth() - pad, top = pad;
            float bottom = getHeight() - dp(42);
            int pureHue = Color.HSVToColor(new float[] {hsv[0], 1f, 1f});
            paint.setShader(new LinearGradient(left, top, right, top,
                    Color.WHITE, pureHue, Shader.TileMode.CLAMP));
            canvas.drawRect(left, top, right, bottom, paint);
            paint.setShader(new LinearGradient(left, top, left, bottom,
                    0x00000000, 0xFF000000, Shader.TileMode.CLAMP));
            canvas.drawRect(left, top, right, bottom, paint);
            paint.setShader(null);

            float markX = left + hsv[1] * (right - left);
            float markY = top + (1f - hsv[2]) * (bottom - top);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.BLACK);
            canvas.drawCircle(markX, markY, dp(8), paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(markX, markY, dp(6), paint);

            float hueTop = bottom + dp(12), hueBottom = getHeight() - dp(8);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(left, hueTop, right, hueTop, rainbow,
                    null, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(left, hueTop, right, hueBottom, dp(5), dp(5), paint);
            paint.setShader(null);
            float hueX = left + hsv[0] / 360f * (right - left);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(3));
            paint.setColor(Color.WHITE);
            canvas.drawCircle(hueX, (hueTop + hueBottom) / 2f, dp(8), paint);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override public boolean onTouchEvent(android.view.MotionEvent event) {
            if (event.getAction() != android.view.MotionEvent.ACTION_DOWN
                    && event.getAction() != android.view.MotionEvent.ACTION_MOVE
                    && event.getAction() != android.view.MotionEvent.ACTION_UP) return true;
            float pad = dp(10), left = pad, right = Math.max(left + 1, getWidth() - pad);
            float top = pad, bottom = getHeight() - dp(42);
            if (event.getY() <= bottom + dp(5)) {
                hsv[1] = Math.max(0f, Math.min(1f, (event.getX() - left) / (right - left)));
                hsv[2] = 1f - Math.max(0f, Math.min(1f,
                        (event.getY() - top) / Math.max(1f, bottom - top)));
            } else {
                hsv[0] = Math.max(0f, Math.min(1f,
                        (event.getX() - left) / (right - left))) * 360f;
            }
            invalidate();
            if (changed != null) changed.changed(colour());
            return true;
        }
    }


    private static final class Check extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Check(Context context, int colour) {
            super(context);
            paint.setColor(colour);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int size = Math.round(22 * getResources().getDisplayMetrics().density);
            setMeasuredDimension(size, size);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float unit = getWidth() / 22f;
            paint.setStrokeWidth(unit * 2.2f);
            float y = getHeight() / 2f;
            canvas.drawLine(unit * 4, y + unit, unit * 9, y + unit * 5.5f, paint);
            canvas.drawLine(unit * 9, y + unit * 5.5f, unit * 18, y - unit * 5f, paint);
        }
    }


    private static final class Dot extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.RectF box = new android.graphics.RectF();
        private final android.graphics.Path outline = new android.graphics.Path();
        private boolean chosen;
        private float selection;
        private android.animation.ValueAnimator animation;

        Dot(Context context, int colour, boolean chosen) {
            super(context);
            this.chosen=chosen; selection=chosen?1f:0f;
            fill.setColor(colour); ring.setColor(colour); ring.setStyle(Paint.Style.STROKE);
            setSelected(chosen); setFocusable(true);
        }
        private final Paint customInk=new Paint(Paint.ANTI_ALIAS_FLAG);
        private float customProgress;
        private android.animation.ValueAnimator colourAnimation;
        void reveal(int colour){
            choose(true,true);ring.setColor(colour);
            if(!Motion.enabled(getContext())){fill.setShader(null);fill.setColor(colour);invalidate();return;}
            if(colourAnimation!=null)colourAnimation.cancel();customInk.setColor(colour);customProgress=0;
            colourAnimation=android.animation.ValueAnimator.ofFloat(0,1);colourAnimation.setDuration(220L);
            colourAnimation.addUpdateListener(a->{customProgress=(Float)a.getAnimatedValue();invalidate();});
            colourAnimation.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){fill.setShader(null);fill.setColor(colour);customProgress=0;colourAnimation=null;invalidate();}});
            colourAnimation.start();
        }
        void choose(boolean selected, boolean animate) {
            chosen=selected; setSelected(selected);
            if (animation!=null) {animation.cancel();animation=null;}
            if (!animate || !Motion.enabled(getContext())) {selection=chosen?1f:0f;invalidate();return;}
            animation=android.animation.ValueAnimator.ofFloat(selection,chosen?1f:0f);
            animation.setDuration(180L);
            animation.addUpdateListener(a -> {selection=(Float)a.getAnimatedValue();invalidate();});
            animation.start();
        }
        @Override protected void onMeasure(int width, int height) {
            int size=Math.round(26*getResources().getDisplayMetrics().density);
            setMeasuredDimension(resolveSize(size,width),resolveSize(size,height));
        }
        @Override protected void onDraw(Canvas canvas) {
            float unit=getResources().getDisplayMetrics().density;
            float x=getWidth()/2f,y=getHeight()/2f;
            float radius=Math.max(1f,Math.min(x,y)-6*unit);

            Expressive.shape(outline,Expressive.COOKIE9,x,y,radius+3*unit*selection,selection,selection*360f/Expressive.lobes(Expressive.COOKIE9));
            canvas.drawPath(outline,fill);
            if(customProgress>0){customInk.setAlpha(Math.round(255*customProgress));canvas.drawPath(outline,customInk);}
            if (selection>0f) {
                int shade=fill.getColor();
                boolean light=(Color.red(shade)*299+Color.green(shade)*587+Color.blue(shade)*114)/1000>150;
                ring.setStyle(Paint.Style.STROKE);ring.setStrokeCap(Paint.Cap.ROUND);ring.setStrokeWidth(2.4f*unit);
                ring.setColor(light ? 0xFF111111 : 0xFFFFFFFF);ring.setAlpha(Math.round(255*Math.min(1f,selection)));
                canvas.drawLine(x-5*unit,y+0.5f*unit,x-1.5f*unit,y+4*unit,ring);
                canvas.drawLine(x-1.5f*unit,y+4*unit,x+5.5f*unit,y-3.5f*unit,ring);
            }
        }
        @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            if (hasOnClickListeners()) {info.setCheckable(true);info.setChecked(chosen);info.setClassName("android.widget.RadioButton");}
        }
        @Override protected void onDetachedFromWindow() {
            if (animation!=null) {animation.cancel();animation=null;}
            if(colourAnimation!=null){colourAnimation.cancel();colourAnimation=null;}
            selection=chosen?1f:0f;super.onDetachedFromWindow();
        }
    }
}

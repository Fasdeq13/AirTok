from __future__ import annotations

import os
import re
import struct
import subprocess
from typing import Dict, List, Optional, Tuple

TELEPHONY = "Landroid/telephony/TelephonyManager;"
REGION = "Lcat/narezany/margyt/Region;"
ACCENT = "Lcat/narezany/margyt/Accent;"
DOWNLOAD = "Lcat/narezany/margyt/Download;"
FEED = "Lcat/narezany/margyt/Feed;"

VIDEO = "Lcom/ss/android/ugc/aweme/feed/model/Video;"
ACL = "Lcom/ss/android/ugc/aweme/feed/model/ACLCommonShare;"
AWEME = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;"
USER = "Lcom/ss/android/ugc/aweme/profile/model/User;"
PROFILE_USER = "Lcom/ss/android/ugc/profile/platform/base/data/UserProfileInfo;"
VIDEO_CONTROL = "Lcom/ss/android/ugc/aweme/feed/model/VideoControl;"
FEED_ITEM_LIST = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;"
PHOTO_IMAGE = "Lcom/ss/android/ugc/aweme/feed/model/PhotoModeImageUrlModel;"
ACCOUNT_SERVICE = "Lcom/ss/android/ugc/aweme/IAccountUserService;"
ACCOUNT = "Lcat/narezany/margyt/Account;"
COMMENTS = "Lcat/narezany/margyt/Comments;"
RAIL = "Lcat/narezany/margyt/FeedRail;"
RAIL_CLASSES = (
    "Lcom/ss/android/ugc/aweme/feed/assem/digg/VideoDiggAssem;",
    "Lcom/ss/android/ugc/aweme/feed/assem/videocomment/VideoCommentAssem;",
    "Lcom/ss/android/ugc/aweme/feed/favorite/VideoFavoriteAssem;",
    "Lcom/ss/android/ugc/aweme/feed/assem/share/VideoShareAssem;",
)
PLAYER_CONTROLLER="Lcom/ss/android/ugc/aweme/feed/controller/PlayerController;"
REPOST_CLASS="Lcom/ss/android/ugc/aweme/upvote/detail/videorepost/VideoRepostAssem;"
AVATAR_CLASS="Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarDefaultAssem;"
PROFILE_CONTAINER="Lcom/ss/android/ugc/profile/platform/base/component/BaseContainerComponent;"
PROFILE_HEADER="Lcom/ss/android/ugc/profile/platform/business/header/base/HeaderBaseContainerComponent;"
MESSAGE_CLASSES=("Lcom/ss/android/ugc/aweme/im/sdk/chat/messagelist/skeleton/cell/IMSkeletonLayoutReusedUISlotAssem;","Lcom/ss/android/ugc/aweme/im/sdk/chat/messagelist/skeleton/cell/SkeletonLayoutReusedUISlotAssem;")
PUSH_GUIDE="Lcom/ss/android/ugc/aweme/unifiedauth/push/UniAuthGuidePopupServiceImpl;"
FRIENDS_RESPONSE="Lcom/ss/android/ugc/aweme/relation/model/FriendsListResponse;"
COMMENT_IMAGE = "Lcom/ss/android/ugc/aweme/comment/model/CommentImageStruct;"
COMMENT = "Lcom/ss/android/ugc/aweme/comment/model/Comment;"
COMMENT_STICKER = "Lcom/ss/android/ugc/aweme/comment/model/CommentStickerStruct;"
WATERMARK = "Lcat/narezany/margyt/Watermark;"
FLAGS = "Lcat/narezany/margyt/Flags;"
SOUND = "Lcat/narezany/margyt/Sound;"
BADGE = "Lcat/narezany/margyt/Badge;"
AVATARS = "Lcat/narezany/margyt/Avatars;"
STICKERS = "Lcat/narezany/margyt/Stickers;"
STREAKS = "Lcat/narezany/margyt/Streaks;"
STREAK_DATA = "Lcom/ss/android/ugc/aweme/im/streak/api/StreakData;"
STREAK_SERVICE = "Lcom/ss/android/ugc/aweme/im/streak/api/IStreakService;"
STICKER_ITEM = "Lcom/ss/android/ugc/aweme/im/common/model/StickerItem;"
STICKER_IMAGE = "Lcom/ss/android/ugc/aweme/im/common/model/StickerImage;"
STICKER_BASE = "Lcom/ss/android/ugc/aweme/im/common/model/StickerBase;"
STICKER_CLICK = "Lcom/ss/android/ugc/aweme/im/messagelist/api/ability/MessageListStickerClickAbility;"
STICKER_TEMPLATE = "Lcom/ss/android/ugc/aweme/im/message/template/card/StickerTemplate;"
TEXT_VIEW = "Landroid/widget/TextView;"
TUX_TEXT = "Lcom/bytedance/tux/input/TuxTextView;"
CHAR_SEQUENCE = "Ljava/lang/CharSequence;"
SEEKBAR = "Lcat/narezany/margyt/Seekbar;"
FONTS = "Lcat/narezany/margyt/Fonts;"
RATE = "Lcat/narezany/margyt/Rate;"
TYPEFACE = "Landroid/graphics/Typeface;"
PAINT_CLASS = "Landroid/graphics/Paint;"
MUSIC = "Lcom/ss/android/ugc/aweme/music/model/Music;"

WILD_SOURCES: List[Tuple[str, str, str, str]] = [
    ("setSeekBarShowType", "(I)V", "(Ljava/lang/Object;I)V", SEEKBAR),
]

_TAP = "Landroid/view/View;LX/1AUi;LX/0RoP;" + STICKER_TEMPLATE
_SHEET = ("Lcom/bytedance/lighten/loader/SmartImageView;"
          "Landroidx/fragment/app/FragmentManager;LX/0RoP;" + STICKER_TEMPLATE)
_CORNER = "Lcom/ss/android/ugc/aweme/views/RoundingCornerLayout;" + STICKER_TEMPLATE
_ANY = "Ljava/lang/Object;"

WILD_SOURCES += [
    ("RQ1", "(%s)V" % _TAP, "(%s%s)V" % (_ANY * 4, STICKER_TEMPLATE), STICKERS),
    ("rP", "(%s)V" % _TAP, "(%s%s)V" % (_ANY * 4, STICKER_TEMPLATE), STICKERS),
    ("DD2", "(%s)V" % _SHEET, "(%s%s)V" % (_ANY * 4, STICKER_TEMPLATE), STICKERS),
    ("vQ1", "(%s)V" % _CORNER, "(%s%s)V" % (_ANY * 2, STICKER_TEMPLATE), STICKERS),
]

SETTINGS_MANAGER = "Lcom/bytedance/ies/abmock/SettingsManager;"
AB_READERS: List[Tuple[str, str, str]] = [
    ("LIZ", "Ljava/lang/String;Z", "Z"),
    ("LJ", "Ljava/lang/String;I", "I"),
    ("LJFF", "Ljava/lang/String;J", "J"),
    ("LJI", "Ljava/lang/String;Ljava/lang/String;", "Ljava/lang/String;"),
    ("LIZJ", "Ljava/lang/String;F", "F"),
    ("LIZIZ", "Ljava/lang/String;D", "D"),
]
CANVAS = "Landroid/graphics/Canvas;"

COMMENT_TAP = ("(Landroid/view/View;%s ZLjava/lang/String;Ljava/util/Map;"
               "Ljava/lang/String;)V" % COMMENT_STICKER).replace("; Z", ";Z")

DISCOVERED_STATICS: List[Tuple[str, str, str, str]] = [
    ("comment sticker tapped", COMMENT_TAP, "stickerTapped", COMMENTS),
]

STICKER_ITEM_D = "Lcom/ss/android/ugc/aweme/im/common/model/StickerItem;"
FUNCTION0 = "Lkotlin/jvm/functions/Function0;"

COMMENT_SHEET = ("(Ljava/lang/String;%s Landroid/view/View;ZLjava/lang/String;"
                 "Ljava/util/Map;%s %s %s)V"
                 % (STICKER_ITEM_D, FUNCTION0, FUNCTION0, FUNCTION0)).replace(" ", "")

DISCOVERED_VIRTUALS: List[Tuple[str, str, str, str]] = [
    ("comment sticker sheet", COMMENT_SHEET, "stickerSheet", COMMENTS),
]

FOUND: Dict[str, Tuple[str, str]] = {}

VIEW = "Landroid/view/View;"
LONG_CLICK = "Landroid/view/View$OnLongClickListener;"

ANCHORED_SOURCES: List[Tuple[str, str, str, str, str, str]] = [
    ("bindSticker: ", VIEW, "setOnLongClickListener",
     "(%s)V" % LONG_CLICK, "(%s%s)V" % (VIEW, LONG_CLICK), COMMENTS),
    ("Lcom/ss/android/ugc/aweme/commentv2/commentlist/powercell/BaseCommentCell;",
     VIEW, "setOnLongClickListener", "(%s)V" % LONG_CLICK,
     "(%s%s)V" % (VIEW, LONG_CLICK), COMMENTS),
    ("[tiktok_logo]", CANVAS, "drawBitmap",
     "(Landroid/graphics/Bitmap;FFLandroid/graphics/Paint;)V",
     "(%sLandroid/graphics/Bitmap;FFLandroid/graphics/Paint;)V" % CANVAS, WATERMARK),
]

CLASS_ANCHORED_SOURCES = {
    "Lcom/ss/android/ugc/aweme/commentv2/commentlist/powercell/BaseCommentCell;",
}

SPEED_TOUCH_CLASS = "LY/ATListenerS452S0100000_16;"
SPEED_EDGE_CLASS = "LX/16wu;"
SPEED_EDGE_METHOD = ".method public final onTouch(Landroid/view/View;Landroid/view/MotionEvent;)Z"
SPEED_TOUCH_METHOD = (
    ".method public static final onTouch$8(LY/ATListenerS452S0100000_16;"
    "Landroid/view/View;Landroid/view/MotionEvent;)Z"
)
SPEED_TOUCH_TARGET = (
    "Lcat/narezany/margyt/SpeedGesture;->onTouch(Ljava/lang/Object;"
    "Landroid/view/View;Landroid/view/MotionEvent;)Z"
)
SPEED_TOUCH_HOOK = """\
    invoke-static {p0, p1, p2}, Lcat/narezany/margyt/SpeedGesture;->onTouch(Ljava/lang/Object;Landroid/view/View;Landroid/view/MotionEvent;)Z
    move-result v0
    if-eqz v0, :margyt_speed_touch_continue
    const/4 v0, 0x1
    return v0
:margyt_speed_touch_continue
    out = []
    literal = "-0x%x" % ((1 << 32) - TIKTOK_PINK) if TIKTOK_PINK > 0x7FFFFFFF \
        else "0x%x" % TIKTOK_PINK
    out.append((
        "the pink itself",
        re.compile(r"^(\s*)const ([vp]\d+), %s$" % re.escape(literal), re.MULTILINE),
        r"\1invoke-static {}, %s->accent()I\n\n\1move-result \2" % ACCENT,
    ))
    for owner, name, original, replacement in COLOUR_SOURCES:
        out.append((
            "%s->%s%s" % (owner.split("/")[-1][:-1], name, original),
            re.compile(r"invoke-virtual(/range)? (\{[^}]*\}), %s->%s%s"
                       % (re.escape(owner), name, re.escape(original))),
            r"invoke-static\1 \2, %s->%s%s" % (ACCENT, name, replacement),
        ))
    for owner, name, signature in COLOUR_STATICS:
        out.append((
            "%s->%s" % (owner.split("/")[-1][:-1], name),
            re.compile(r"invoke-static(/range)? (\{[^}]*\}), %s->%s%s"
                       % (re.escape(owner), name, re.escape(signature))),
            r"invoke-static\1 \2, %s->%s%s" % (ACCENT, name, signature),
        ))
    out.append((
        "feed Lottie setAnimation",
        re.compile(r"invoke-virtual(/range)? (\{[^}]*\}), "
                   r"LX/1HkT;->setAnimation\(Ljava/lang/String;\)V"),
        r"invoke-static\1 \2, %s->setAnimation(Ljava/lang/Object;Ljava/lang/String;)V"
        % ACCENT,
    ))
    return out

for _font_owner in (TEXT_VIEW,TUX_TEXT,"Landroid/widget/EditText;","Landroid/widget/Button;"):
    for _font_name,_font_sig in (
        ("setText","(I)V"),("setHint","(I)V"),("setHint","(Ljava/lang/CharSequence;)V"),
        ("setTextAppearance","(I)V"),("setTextAppearance","(Landroid/content/Context;I)V"),
    ):
        MODEL_SOURCES.append((_font_owner,_font_name,_font_sig,"("+TEXT_VIEW+_font_sig[1:],FONTS))

MODEL_SOURCES += [
    ("Landroid/app/NotificationManager;","notify","(ILandroid/app/Notification;)V","(Landroid/app/NotificationManager;ILandroid/app/Notification;)V","Lcat/narezany/margyt/ModNotifications;"),
    ("Landroid/app/NotificationManager;","notify","(Ljava/lang/String;ILandroid/app/Notification;)V","(Landroid/app/NotificationManager;Ljava/lang/String;ILandroid/app/Notification;)V","Lcat/narezany/margyt/ModNotifications;"),
]

def model_rules() -> List[Tuple[str, "re.Pattern[str]", str]]:
    out = []
    for owner, name, original, replacement, target in MODEL_SOURCES:
        theirs, ours = name if isinstance(name, tuple) else (name, name)
        out.append((
            "%s->%s" % (owner.rsplit("/", 1)[-1][:-1], theirs),
            re.compile(r"invoke-(?:virtual|interface)(/range)? (\{[^}]*\}), %s->%s%s"
                       % (re.escape(owner), theirs, re.escape(original))),
            r"invoke-static\1 \2, %s->%s%s" % (target, ours, replacement),
        ))
    for owner, name, original, replacement, target in MODEL_STATICS:
        theirs, ours = name if isinstance(name, tuple) else (name, name)
        out.append((
            "%s->%s" % (owner.rsplit("/", 1)[-1][:-1], theirs),
            re.compile(r"invoke-static(/range)? (\{[^}]*\}), %s->%s%s"
                       % (re.escape(owner), theirs, re.escape(original))),
            r"invoke-static\1 \2, %s->%s%s" % (target, ours, replacement),
        ))
    for label, descriptor, ours, target in DISCOVERED_VIRTUALS:
        found = FOUND.get(label)
        if found is None:
            continue
        owner, theirs = found
        out.append((
            label,
            re.compile(r"invoke-virtual(/range)? (\{[^}]*\}), %s->%s%s"
                       % (re.escape(owner), re.escape(theirs), re.escape(descriptor))),
            r"invoke-static\1 \2, %s->%s%s"
            % (target, ours, "(Ljava/lang/Object;" + descriptor[1:]),
        ))
    for label, descriptor, ours, target in DISCOVERED_STATICS:
        found = FOUND.get(label)
        if found is None:
            continue
        owner, theirs = found
        out.append((
            label,
            re.compile(r"invoke-static(/range)? (\{[^}]*\}), %s->%s%s"
                       % (re.escape(owner), re.escape(theirs), re.escape(descriptor))),
            r"invoke-static\1 \2, %s->%s%s" % (target, ours, descriptor),
        ))
    for name, original, replacement, target in WILD_SOURCES:
        out.append((
            "%s (any owner)" % name,
            re.compile(r"invoke-(?:virtual|interface)(/range)? (\{[^}]*\}), L[^;]+;->%s%s"
                       % (name, re.escape(original))),
            r"invoke-static\1 \2, %s->%s%s" % (target, name, replacement),
        ))
    for owner, field, kind, name, target in FIELD_SOURCES:
        if kind in ("J", "D"):
            read, took = "iget-wide", "move-result-wide"
        elif kind in ("Z", "B", "S", "C", "I", "F"):
            read, took = "iget", "move-result"
        else:
            read, took = "iget-object", "move-result-object"
        out.append((
            "%s.%s" % (owner.rsplit("/", 1)[-1][:-1], field),
            re.compile(r"^(\s*)%s ([vp]\d+), ([vp]\d+), %s->%s:%s$"
                       % (read, re.escape(owner), re.escape(field), re.escape(kind)),
                       re.MULTILINE),
            r"\1invoke-static {\3}, %s->%s(%s)%s\n\n\1%s \2"
            % (target, name, owner, kind, took),
        ))
    for owner, field, kind, name, target in STATIC_FIELD_SOURCES:
        read = {"Z": "sget-boolean", "B": "sget-byte", "S": "sget-short", "C": "sget-char",
                "I": "sget", "F": "sget", "J": "sget-wide", "D": "sget-wide"}.get(kind, "sget-object")
        took = ("move-result-wide" if kind in ("J", "D")
                else "move-result" if kind in ("Z", "B", "S", "C", "I", "F") else "move-result-object")
        out.append((
            "%s.%s" % (owner.rsplit("/", 1)[-1][:-1], field),
            re.compile(r"^(\s*)%s ([vp]\d+), %s->%s:%s$"
                       % (read, re.escape(owner), re.escape(field), re.escape(kind)),
                       re.MULTILINE),
            r"\1invoke-static {}, %s->%s()%s\n\n\1%s \2" % (target, name, kind, took),
        ))
    return out

def rewrite_models(root: str) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    prepared = model_rules()
    needles = [_required(pattern) for _label, pattern, _target in prepared]
    owners = tuple(set([owner for owner, _n, _o, _r, _t in MODEL_SOURCES]
                       + [owner for owner, _n, _o, _r, _t in MODEL_STATICS]
                       + [owner for owner, _f, _k, _n, _t in FIELD_SOURCES]
                       + [owner for owner, _f, _k, _n, _t in STATIC_FIELD_SOURCES]
                       + [name for name, _o, _r, _t in WILD_SOURCES]
                       + [FOUND[label][0]
                          for label, _d, _o, _t in DISCOVERED_STATICS + DISCOVERED_VIRTUALS
                          if label in FOUND]))
    for dirpath, _dirs, files in os.walk(root):
        for name in files:
            if not name.endswith(".smali"):
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            if not any(owner in text for owner in owners):
                continue
            before = text
            for (label, pattern, target), needle in zip(prepared, needles):
                if needle and needle not in text:
                    continue
                text, hits = pattern.subn(target, text)
                if hits:
                    counts[label] = counts.get(label, 0) + hits
            if text != before:
                with open(path, "w", encoding="utf-8") as handle:
                    handle.write(text)
    return counts

ANCHORED_SOURCES.append(("LX/0oir;","LX/0oir;","LIZ",
    "(Ljava/lang/String;Ljava/lang/String;Ljava/util/List;)Landroid/content/ClipData;",
    "(Ljava/lang/String;Ljava/lang/String;Ljava/util/List;)Landroid/content/ClipData;",
    "Lcat/narezany/margyt/CommentClipboard;"))

def anchored_rules() -> List[Tuple[str, str, "re.Pattern[str]", str]]:
    out = []
    for anchor, owner, name, original, replacement, target in ANCHORED_SOURCES:
        out.append((
            anchor,
            "%s->%s (in the class marked %s)" % (owner.rsplit("/", 1)[-1][:-1], name, anchor),
            re.compile(r"invoke-%s(/range)? (\{[^}]*\}), %s->%s%s"
                       % ("static" if owner=="LX/0oir;" else "virtual", re.escape(owner), name, re.escape(original))),
            r"invoke-static\1 \2, %s->%s%s" % (target, name, replacement),
        ))
    owner = "    invoke-static/range {p0 .. p0}, %s->owner(Ljava/lang/Object;)V\n" % AVATAR_SHAPE
    for anchor in ROUND_DRAWERS + STORY_RINGS:
        out.append((
            anchor, SPIN_LABEL,
            re.compile(r"(?m)^(?:%s)?(    invoke-(?:virtual|static)(?:/range)? \{[^}]*\}, (?:Landroid/graphics/Canvas;|%s)->(?:drawPath|clipPath|drawArc)\()"
                       % (re.escape(owner), re.escape(AVATAR_SHAPE))),
            owner.replace("\\", "\\\\") + r"\1",
        ))
    return out

def rewrite_anchored(root: str) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    prepared = anchored_rules()
    for dirpath, _dirs, files in os.walk(root):
        for name in files:
            if not name.endswith(".smali"):
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            before = text
            declaration=text.split("\n",1)[0]
            if "onSurfaceTextureUpdated(Landroid/graphics/SurfaceTexture;)V" in text:
                def glass_texture_frame(match):
                    body=match.group(0)
                    if "GlassVideo;->frameUpdated" in body:return body
                    hook="    invoke-static/range {p1 .. p1}, Lcat/narezany/margyt/GlassVideo;->frameUpdated(Landroid/graphics/SurfaceTexture;)V\n"
                    body,hits=re.subn(r"(?m)^([ \t]*\.(?:registers|locals) \d+\n)",lambda m:m.group(0)+hook,body,count=1)
                    if hits:counts["native texture frame events"]=counts.get("native texture frame events",0)+hits
                    return body
                text=re.sub(r"(?ms)^\.method public (?:(?:final|synthetic|synchronized|bridge) )*onSurfaceTextureUpdated\(Landroid/graphics/SurfaceTexture;\)V.*?^\.end method",glass_texture_frame,text)
            if PLAYER_VIEW_CONFIG in declaration:
                def texture_video(match):
                    body=match.group(0)
                    if "MotionBlur;->textureVideo" in body:return body
                    hook=("    invoke-static {}, Lcat/narezany/margyt/MotionBlur;->textureVideo()Z\n"
                          "    move-result v0\n"
                          "    if-eqz v0, :margyt_surface_view\n"
                          "    const/4 v0, 0x0\n"
                          "    return v0\n"
                          "    :margyt_surface_view\n")
                    body,hits=re.subn(r"(?m)^([ \t]*\.(?:registers|locals) \d+\n)",lambda m:m.group(0)+hook,body,count=1)
                    if hits:counts["player video view kind"]=counts.get("player video view kind",0)+hits
                    return body
                text=re.sub(r"(?ms)^\.method public final isUseSurfaceView\(\)Z.*?^\.end method",texture_video,text)
                text=re.sub(r"(?ms)^\.method public isUseSurfaceView\(\)Z.*?^\.end method",texture_video,text)
            if PLAYER_SETTINGS in declaration:
                def player_flag(match):
                    body=match.group(0)
                    if "Flags;->playerFlag" in body:return body
                    hook=("    invoke-static {p0, p1}, Lcat/narezany/margyt/Flags;->playerFlag(Ljava/lang/String;Ljava/lang/reflect/Type;)Ljava/lang/Object;\n"
                          "    move-result-object v0\n"
                          "    if-eqz v0, :margyt_player_flag\n"
                          "    return-object v0\n"
                          "    :margyt_player_flag\n")
                    body,hits=re.subn(r"(?m)^([ \t]*\.(?:registers|locals) \d+\n)",lambda m:m.group(0)+hook,body,count=1)
                    if hits:counts["player settings reader"]=counts.get("player settings reader",0)+hits
                    return body
                text=re.sub(r"(?ms)^\.method public static LIZ\(Ljava/lang/String;Ljava/lang/reflect/Type;\)Ljava/lang/Object;.*?^\.end method",player_flag,text)
            if FEED_ITEM_LIST in declaration:
                def native_ad_getter(match):
                    body=match.group(0)
                    if "Feed;->withoutAds" in body:return body
                    body,hits=re.subn(r"(?m)^    return-object ([vp]\d+)",lambda m:"    invoke-static/range {"+m.group(1)+" .. "+m.group(1)+"}, "+FEED+"->withoutAds(Ljava/util/List;)Ljava/util/List;\n    move-result-object "+m.group(1)+"\n"+m.group(0),body)
                    if hits:counts["native feed ad getter"]=hits
                    return body
                text=re.sub(r"(?ms)^\.method public getItems\(\)Ljava/util/List;.*?^\.end method",native_ad_getter,text)
                def native_ad_setter(match):
                    body=match.group(0)
                    if "Feed;->withoutAds" in body:return body
                    hook="\n    invoke-static/range {p1 .. p1}, "+FEED+"->withoutAds(Ljava/util/List;)Ljava/util/List;\n    move-result-object p1\n"
                    body,hits=re.subn(r"(?m)^    \.registers [0-9]+",lambda m:m.group(0)+hook,body,count=1)
                    if hits:counts["native feed ad ingress"]=hits
                    return body
                text=re.sub(r"(?ms)^\.method public setItems\(Ljava/util/List;\)V.*?^\.end method",native_ad_setter,text)
            if "Lcom/ss/android/ugc/aweme/comment/longpresspanel/ui/CommentLongPressFragment;" in declaration:
                def native_comment_items(match):
                    body=match.group(0)
                    if "Comments;->nativeItems" in body:return body
                    hook="\n    invoke-static {p0, p1}, "+COMMENTS+"->nativeItems(Ljava/lang/Object;Ljava/util/List;)Ljava/util/List;\n    move-result-object p1\n"
                    body,hits=re.subn(r"(?m)^    \.registers [0-9]+",lambda m:m.group(0)+hook,body,count=1)
                    if hits:counts["native comment selection item"]=hits
                    return body
                text=re.sub(r"(?ms)^\.method public final oX\(Ljava/util/List;\)V.*?^\.end method",native_comment_items,text)
            if "Lcom/ss/android/ugc/aweme/offlinemode/ui/sheet/OfflineModeSheetPageAssem;" in declaration:
                def offline_legacy(match):
                    body=match.group(0)
                    hook="    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/OfflineChoices;->legacy(Ljava/lang/Object;)V\n"
                    if hook.strip() not in body:
                        body=re.sub(r"(?m)^    return-void",hook+"    return-void",body)
                        counts["native offline legacy menu"]=counts.get("native offline legacy menu",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public final onAssemPostCreate\(\)V.*?^\.end method",offline_legacy,text)
            comment_owner = next((owner for owner in (
                "Lcom/ss/android/ugc/aweme/comment/commentlist/api/CommentApi;",
                "LX/0oah;",
                "Lcom/ss/android/ugc/aweme/commentv2/commentlist/CommentApiV2;",
                "Lcom/ss/android/ugc/aweme/commentv2/commentlist/CommentApiV2SplitJson;"
            ) if owner in declaration), None)
            if comment_owner:
                def offline_native_response(match):
                    body=match.group(0)
                    if "OfflineComments;->native" in body:return body
                    task=comment_owner.endswith("/CommentApi;")
                    static=task or comment_owner=="LX/0oah;"
                    register="p0" if static else "p1"
                    method="nativeTask" if task else "nativeObservable"
                    result="LX/0Vgo;" if task else "LX/19ZK;"
                    hook=(f"    invoke-static/range {{{register} .. {register}}}, Lcat/narezany/margyt/OfflineComments;->{method}(Ljava/lang/String;)Ljava/lang/Object;\n"
                          "    move-result-object v0\n    if-eqz v0, :margyt_comments_online\n"
                          f"    check-cast v0, {result}\n    return-object v0\n    :margyt_comments_online\n")
                    body,hits=re.subn(r"(?m)^(    (?:move|const|invoke|sget|iget|new|return|if|check)[\w/-]*(?: |$))",lambda m:hook+m.group(0),body,count=1)
                    if hits:counts["native offline comments response"]=counts.get("native offline comments response",0)+1
                    return body
                signature=r"(?ms)^\.method public static LIZIZ\(.*?^\.end method" if comment_owner in ("LX/0oah;","Lcom/ss/android/ugc/aweme/comment/commentlist/api/CommentApi;") else r"(?ms)^\.method public fetchCommentListV2\(.*?^\.end method"
                text=re.sub(signature,offline_native_response,text)
            if "LX/0wk3;" in declaration:
                def offline_saved(match):
                    body=match.group(0)
                    if "OfflineComments;->cached" not in body:
                        body=re.sub(r"(?m)^    \.registers \d+",lambda m:m.group(0)+"\n    invoke-static/range {p0 .. p1}, Lcat/narezany/margyt/OfflineComments;->cached(Ljava/lang/Object;Ljava/lang/String;)V",body,count=1)
                        counts["native offline comments cache"]=counts.get("native offline comments cache",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public static LIZ\(Lcom/ss/android/ugc/aweme/feed/model/Aweme;Ljava/lang/String;\)V.*?^\.end method",offline_saved,text)
            if "Lcom/ss/android/ugc/aweme/offlinemode/ui/OfflineModeContentComponent;" in declaration:
                def offline_content(match):
                    body=match.group(0)
                    if "OfflineComments;->content" not in body:
                        body=re.sub(r"(?m)^    \.registers \d+",lambda m:m.group(0)+"\n    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/OfflineComments;->content(Ljava/lang/Object;)V",body,count=1)
                        counts["native offline comments current video"]=counts.get("native offline comments current video",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public final onParentViewCreated\(\)V.*?^\.end method",offline_content,text)
                def offline_closed(match):
                    body=match.group(0)
                    if "OfflineComments;->closed" not in body:
                        body=re.sub(r"(?m)^    return-void","    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/OfflineComments;->closed(Ljava/lang/Object;)V\n    return-void",body)
                    return body
                text=re.sub(r"(?ms)^\.method public final onDestroy\(\)V.*?^\.end method",offline_closed,text)
            if "Lcom/ss/android/ugc/aweme/offlinemode/ui/bottom/OfflineModeDetailBottomComponent;" in declaration:
                def offline_bottom(match):
                    body=match.group(0)
                    if "OfflineComments;->bottom" not in body and "    return-object v4" in body:
                        body=body.replace("    return-object v4","    invoke-static {v4}, Lcat/narezany/margyt/OfflineComments;->bottom(Landroid/view/View;)Landroid/view/View;\n    move-result-object v4\n    return-object v4")
                        counts["native offline comments viewer"]=counts.get("native offline comments viewer",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public final MR2\(Landroid/view/ViewGroup;\)Landroid/view/View;.*?^\.end method",offline_bottom,text)
            if "Lcom/ss/android/ugc/aweme/offlinemode/viewmodel/OfflineModeManagerVM;" in declaration:
                def offline_valid(match):
                    body=match.group(0)
                    needle="invoke-interface {v1, v0}, Ljava/util/List;->contains(Ljava/lang/Object;)Z"
                    if needle in body:
                        body=body.replace(needle,"invoke-static {v1, v0}, Lcat/narezany/margyt/OfflineChoices;->validCount(Ljava/util/List;Ljava/lang/Object;)Z")
                        counts["native offline custom persistence"]=counts.get("native offline custom persistence",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public final d43\(\)Z.*?^\.end method",offline_valid,text)
            if "LX/0wjq;" in declaration:
                def offline_native_count(match):
                    body=match.group(0)
                    needle="invoke-interface {v1, v0}, Ljava/util/List;->contains(Ljava/lang/Object;)Z"
                    if needle in body:
                        body=body.replace(needle,"invoke-static {v1, v0}, Lcat/narezany/margyt/OfflineChoices;->validCount(Ljava/util/List;Ljava/lang/Object;)Z",1)
                        counts["native offline count accepted"]=counts.get("native offline count accepted",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public static LIZ\(\)I.*?^\.end method",offline_native_count,text)
            if "Lkotlin/jvm/internal/AwS704S0100000_26;" in declaration:
                def offline_list(match):
                    body=match.group(0)
                    hook="\n    invoke-static {v1}, Lcat/narezany/margyt/OfflineChoices;->counts(Ljava/util/List;)Ljava/util/List;\n    move-result-object v1"
                    needle=re.search(r"invoke-static \{\}, LX/0wj1;->LJFF\(\)Ljava/util/List;(?:(?:\s*\.line \d+)*)\s*move-result-object v1",body)
                    if "OfflineChoices;->counts" not in body and needle:
                        body=body[:needle.end()]+hook+body[needle.end():]
                        counts["native offline choices"]=counts.get("native offline choices",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public static final invoke\$3208\(Lkotlin/jvm/internal/AwS704S0100000_26;\)Ljava/lang/Object;.*?^\.end method",offline_list,text)
            if "LX/0wiL;" in declaration:
                def offline_row(match):
                    body=match.group(0)
                    if "OfflineChoices;->row" not in body:
                        body=body.replace("    return-object v10","    invoke-static {v3, v10}, Lcat/narezany/margyt/OfflineChoices;->row(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;\n    move-result-object v10\n    check-cast v10, LX/0ldB;\n    return-object v10")
                        counts["native offline row"]=counts.get("native offline row",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public final LIZLLL\(\)LX/0ldB;.*?^\.end method",offline_row,text)
            def add_exit(pattern,hook,label):
                nonlocal text
                def patch(match):
                    body=match.group(0)
                    if hook.strip() in body:return body
                    changed,hits=re.subn(r"(?m)^([ \t]*)return-void",lambda m:hook+m.group(0),body)
                    if hits:counts[label]=counts.get(label,0)+1
                    return changed
                text=re.sub(pattern,patch,text)
            if "Lcom/ss/android/ugc/aweme/feed/assem/share/VideoShareAssem;" in declaration:
                def capture_share(match):
                    body=match.group(0)
                    if "VideoActions;->captureShare" in body:return body
                    body=re.sub(r"(?m)^    \.registers \d+",lambda m:m.group(0)+"\n    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/VideoActions;->captureShare(Ljava/lang/Object;)V",body,count=1)
                    counts["native share capture"]=counts.get("native share capture",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public final onClick\(Landroid/view/View;\)V.*?^\.end method",capture_share,text)
            if "LX/0HCj;" in declaration:
                def heart_cycle(match):
                    body=match.group(0)
                    if "Heart;->shown" in body:return body
                    hook="    invoke-static/range {p1 .. p1}, Lcat/narezany/margyt/Heart;->shown(Landroid/widget/ImageView;)V\n"
                    body,hits=re.subn(r"(?m)^([ \t]*\.(?:registers|locals) \d+\n)",lambda m:m.group(0)+hook,body,count=1)
                    if hits:counts["native heart reuse"]=counts.get("native heart reuse",0)+hits
                    return body
                text=re.sub(r"(?ms)^\.method public final LIZ\(Landroid/widget/ImageView;FF\)V.*?^\.end method",heart_cycle,text)
            if REPOST_CLASS in declaration:
                add_exit(r"(?ms)^\.method public final (?:onViewCreated\(Landroid/view/View;\)|s4\(Ljava/lang/Object;\))V.*?^\.end method","    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/Repost;->bind(Ljava/lang/Object;)V\n","native repost")
            if AVATAR_CLASS in declaration:
                add_exit(r"(?ms)^\.method public final (?:onViewCreated\(Landroid/view/View;\)|s4\(Ljava/lang/Object;\))V.*?^\.end method","    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/Repost;->anchor(Ljava/lang/Object;)V\n","native repost avatar")
            if PROFILE_CONTAINER in declaration:
                add_exit(r"(?ms)^\.method public onViewCreated\(Landroid/view/View;\)V.*?^\.end method","    invoke-static/range {p0 .. p1}, Lcat/narezany/margyt/ProfileLayout;->bind(Ljava/lang/Object;Landroid/view/View;)V\n","native profile layout")
            if PROFILE_HEADER in declaration:
                def profile_parts(match):
                    body = match.group(0)
                    if "ProfileParts;->attaching" in body:
                        return body
                    hook = "    invoke-static/range {p0 .. p2}, Lcat/narezany/margyt/ProfileParts;->attaching(Ljava/lang/Object;Landroid/view/View;Landroid/view/ViewGroup;)V\n"
                    body, hits = re.subn(r"(?m)^([ \t]*\.(?:registers|locals) \d+\n)", lambda m: m.group(0) + hook, body, count=1)
                    if hits:
                        counts["native profile component roles"] = hits
                    return body
                text = re.sub(r"(?ms)^\.method public final dq\(Landroid/view/View;Landroid/view/ViewGroup;Landroid/view/View;ZIZ\)V.*?^\.end method", profile_parts, text)
            if any(owner in declaration for owner in MESSAGE_CLASSES):
                def message_bind(match):
                    body=match.group(0)
                    if "MessageTimes;->bind" in body:return body
                    body=re.sub(r"(?m)^    \.registers \d+",lambda m:m.group(0)+"\n    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/MessageTimes;->before(Ljava/lang/Object;)V",body,count=1)
                    body=re.sub(r"(?m)^    return-void","    invoke-static/range {p0 .. p1}, Lcat/narezany/margyt/MessageTimes;->bind(Ljava/lang/Object;Ljava/lang/Object;)V\n    return-void",body)
                    counts["native message timestamps"]=counts.get("native message timestamps",0)+1
                    return body
                message_pattern=(r"(?ms)^\.method public Bq\(LX/0Qfl;\)V.*?^\.end method" if MESSAGE_CLASSES[0] in declaration else r"(?ms)^\.method public final s4\(Ljava/lang/Object;\)V.*?^\.end method")
                text=re.sub(message_pattern,message_bind,text)
            if PUSH_GUIDE in declaration:
                def push_guide(match):
                    body=match.group(0)
                    if "ModNotifications;->noPrompts" in body:return body
                    hook="\n    invoke-static {}, Lcat/narezany/margyt/ModNotifications;->noPrompts()Z\n    move-result v0\n    if-eqz v0, :margyt_notification_guide\n    const/4 v0, 0x0\n    return v0\n    :margyt_notification_guide"
                    body=re.sub(r"(?m)^    \.registers \d+",lambda m:m.group(0)+hook,body,count=1)
                    counts["native notification guide"]=counts.get("native notification guide",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public final LIZ\(\)Z.*?^\.end method",push_guide,text)
            if PLAYER_CONTROLLER in declaration:
                def glass_player(match):
                    body=match.group(0)
                    if "GlassVideo;->" in body:return body
                    signature=body.split("\n",1)[0]
                    if "onPlayProgressChange(" in signature:
                        hook="    invoke-static/range {p0 .. p3}, Lcat/narezany/margyt/GlassVideo;->progress(Ljava/lang/Object;Ljava/lang/String;J)V\n"
                    elif "onPausePlay(" in signature or "onPlayPause(" in signature or "onPlayStop(" in signature:
                        hook="    invoke-static/range {p0 .. p1}, Lcat/narezany/margyt/GlassVideo;->paused(Ljava/lang/Object;Ljava/lang/String;)V\n"
                    else:
                        hook="    invoke-static/range {p0 .. p1}, Lcat/narezany/margyt/GlassVideo;->resumed(Ljava/lang/Object;Ljava/lang/String;)V\n"
                    body,hits=re.subn(r"(?m)^([ \t]*\.(?:registers|locals) \d+\n)",lambda m:m.group(0)+hook,body,count=1)
                    if hits:counts["native backdrop playback events"]=counts.get("native backdrop playback events",0)+hits
                    return body
                text=re.sub(r"(?ms)^\.method public (?:final )?(?:synthetic )?(?:onResumePlay\(Ljava/lang/String;\)|onPlaying\(Ljava/lang/String;\)|onRenderFirstFrameFromResume\(Ljava/lang/String;\)|onPausePlay\(Ljava/lang/String;(?:LX/[^;]+;)?\)|onPlayPause\(Ljava/lang/String;\)|onPlayStop\(Ljava/lang/String;(?:Z)?\)|onPlayProgressChange\(Ljava/lang/String;JJ\))V.*?^\.end method",glass_player,text)
                def resumed_player(match):
                    body=match.group(0)
                    if "PlayerTools;->resumed" in body:return body
                    changed,hits=re.subn(r"(?m)^([ \t]*)return-void",lambda m:"    invoke-static/range {p0 .. p1}, Lcat/narezany/margyt/PlayerTools;->resumed(Ljava/lang/Object;Ljava/lang/String;)V\n"+m.group(0),body)
                    if hits:counts["native player resume"]=counts.get("native player resume",0)+1
                    return changed
                text=re.sub(r"(?ms)^\.method public final onResumePlay\(Ljava/lang/String;\)V.*?^\.end method",resumed_player,text)
            if any(owner in declaration for owner in RAIL_CLASSES):
                def rail_slot(match):
                    body=match.group(0)
                    if "FeedRail;->bindNative" in body:return body
                    hook="    invoke-static/range {p0 .. p0}, "+RAIL+"->bindNative(Ljava/lang/Object;)V\n"
                    changed,hits=re.subn(r"(?m)^([ \t]*)return-void",lambda m:hook+m.group(0),body)
                    if hits:counts["native right-area slots"]=counts.get("native right-area slots",0)+1
                    return changed
                text=re.sub(r"(?ms)^\.method public final (?:onViewCreated\(Landroid/view/View;\)|s4\(Ljava/lang/Object;\))V.*?^\.end method",rail_slot,text)
            for anchor, label, pattern, target in prepared:
                if anchor not in text:
                    continue
                if anchor in CLASS_ANCHORED_SOURCES:
                    declaration = text.split("\n", 1)[0]
                    if anchor not in declaration:
                        continue
                text, hits = pattern.subn(target, text)
                if hits:
                    counts[label] = counts.get(label, 0) + hits
            if "LX/0dQx;" in text.split("\n",1)[0]:
                def repost_texture(match):
                    body=match.group(0)
                    if "Repost;->textureChanged" in body:return body
                    body,hits=re.subn(r"(?m)^([ \t]*return-void)","    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/Repost;->textureChanged(Landroid/view/View;)V\n"+r"\1",body)
                    if hits:counts["native repost texture state"]=counts.get("native repost texture state",0)+hits
                    return body
                text=re.sub(r"(?ms)^\.method public final (?:LJ|LJFF)\(\)V.*?^\.end method",repost_texture,text)
            if "LX/0oYe;" in text.split("\n",1)[0]:
                def selected_dislike(match):
                    body=match.group(0)
                    if "Accent;->dislike" in body:return body
                    body,hits=re.subn(r"(?m)^([ \t]*return-void)","    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/Accent;->dislike(Ljava/lang/Object;)V\n"+r"\1",body)
                    if hits:counts["selected dislike accent"]=counts.get("selected dislike accent",0)+hits
                    return body
                text=re.sub(r"(?ms)^\.method public final LJIIIZ\(Ljava/lang/Boolean;\)V.*?^\.end method",selected_dislike,text)
            if "LX/0G2p;" in text.split("\n",1)[0]:
                def double_tap_hook(match):
                    return (".method public static LIZ()Z\n"
                            "    .registers 1\n"
                            "    invoke-static {}, " + COMMENTS + "->isDoubleTapLikeEnabled()Z\n"
                            "    move-result v0\n"
                            "    return v0\n"
                            ".end method")
                text=re.sub(r"(?ms)^\.method public static LIZ\(\)Z.*?^\.end method",double_tap_hook,text)
            if "Lcom/bytedance/lobby/internal/LobbyInvisibleActivity;" in text.split("\n",1)[0]:
                def lobby_web_auth(match):
                    body=match.group(0)
                    repl=r"""\1
    const-string v0, "google"
    invoke-virtual {v0, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v0
    if-nez v0, :cond_lobby_web
    const-string v0, "google_onetap"
    invoke-virtual {v0, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v0
    if-eqz v0, :cond_lobby_skip
    :cond_lobby_web
    const-string v6, "google_web"
    :cond_lobby_skip"""
                    body,hits=re.subn(r"(?m)^([ \t]*:goto_16\b)",repl,body)
                    if hits:counts["google web lobby"]=counts.get("google web lobby",0)+hits
                    return body
                text=re.sub(r"(?ms)^\.method public final onCreate\(Landroid/os/Bundle;\)V.*?^\.end method",lobby_web_auth,text)
            if "Lcom/ss/android/ugc/aweme/account/login/authorize/AuthorizeActivity;" in text.split("\n",1)[0]:
                def auth_web_result(match):
                    counts["google web auth result"]=counts.get("google web auth result",0)+1
                    return (".method public static LLLLLZ(Lcom/bytedance/lobby/auth/AuthResult;)Ljava/lang/Boolean;\n"
                            "    .registers 3\n"
                            "    .prologue\n"
                            "    iget-object v1, p0, Lcom/bytedance/lobby/auth/AuthResult;->mProviderId:Ljava/lang/String;\n"
                            "    const-string v0, \"google\"\n"
                            "    invoke-static {v0, v1}, Landroid/text/TextUtils;->equals(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Z\n"
                            "    move-result v0\n"
                            "    if-nez v0, :cond_web_ret\n"
                            "    const-string v1, \"google_onetap\"\n"
                            "    iget-object v0, p0, Lcom/bytedance/lobby/auth/AuthResult;->mProviderId:Ljava/lang/String;\n"
                            "    invoke-static {v1, v0}, Landroid/text/TextUtils;->equals(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Z\n"
                            "    move-result v0\n"
                            "    if-nez v0, :cond_web_ret\n"
                            "    const-string v1, \"google_web\"\n"
                            "    iget-object v0, p0, Lcom/bytedance/lobby/auth/AuthResult;->mProviderId:Ljava/lang/String;\n"
                            "    invoke-static {v1, v0}, Landroid/text/TextUtils;->equals(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Z\n"
                            "    move-result v0\n"
                            "    if-nez v0, :cond_web_ret\n"
                            "    const/4 v0, 0x0\n"
                            "    :goto_web_ret\n"
                            "    invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;\n"
                            "    move-result-object v0\n"
                            "    return-object v0\n"
                            "    :cond_web_ret\n"
                            "    const/4 v0, 0x1\n"
                            "    goto :goto_web_ret\n"
                            ".end method")
                text=re.sub(r"(?ms)^\.method public static LLLLLZ\(Lcom/bytedance/lobby/auth/AuthResult;\)Ljava/lang/Boolean;.*?^\.end method",auth_web_result,text)
                def auth_web_create(match):
                    body=match.group(0)
                    body,hits=re.subn(r"move-object\s+v15,\s*v1", "move-object/from16 v15, v16", body)
                    if hits:counts["google web auth select"]=counts.get("google web auth select",0)+hits
                    return body
                text=re.sub(r"(?ms)^\.method public final onCreate\(Landroid/os/Bundle;\)V.*?^\.end method",auth_web_create,text)
            if "Lcom/ss/android/ugc/aweme/commentv2/commentlist/powercell/BaseCommentCell;" in text.split("\n",1)[0]:
                def bind_comment_body(match):
                    body=match.group(0)
                    if "Comments;->bindCell" in body:return body
                    body,hits=re.subn(r"(?m)^([ \t]*return-void)","    invoke-static/range {p0 .. p0}, "+COMMENTS+"->bindCell(Ljava/lang/Object;)V\n"+r"\1",body)
                    if hits:counts["native comment body actions"]=counts.get("native comment body actions",0)+hits
                    return body
                text=re.sub(r"(?ms)^\.method public bridge synthetic onBindItemView\([^\n]*\n.*?^\.end method",bind_comment_body,text)
                def native_comment_actions(match):
                    method=match.group(0)
                    if "CommentActionMenuVM;->h43" not in method or "Comments;->openNativeActions" in method:return method
                    hook=("\n    invoke-static/range {p0 .. p0}, "+COMMENTS+"->openNativeActions(Ljava/lang/Object;)Z\n"
                          "    move-result v0\n    if-eqz v0, :margyt_native_comment_menu\n    return-void\n    :margyt_native_comment_menu\n")
                    result,hits=re.subn(r"(?m)^(    \.registers [0-9]+)",lambda m:m.group(0)+hook,method,count=1)
                    if hits:counts["native comment text actions"]=counts.get("native comment text actions",0)+hits
                    return result
                text=re.sub(r"(?ms)^\.method public k8\(\)V.*?^\.end method",native_comment_actions,text)
                pattern = (r"(?m)^([ \t]*iget-object ([vp]\d+), [vp]\d+, "
                    r"Landroidx/recyclerview/widget/RecyclerView\$ViewHolder;->itemView:Landroid/view/View;)"
                    r"(?!\n[ \t]*invoke-static/range \{\2 \.\. \2\}, Lcat/narezany/margyt/Comments;->mark)")
                text,hits=re.subn(pattern,lambda m:m.group(1)+"\n    invoke-static/range {"+m.group(2)+" .. "+m.group(2)+"}, "
                    +COMMENTS+"->mark(Landroid/view/View;)V",text)
                if hits:counts["native comment cell protection"]=counts.get("native comment cell protection",0)+hits
            if "Lcom/ss/android/ugc/aweme/feed/assem/videoauthorinfo/VideoAuthorInfoRelationAssem;" in text.split("\n",1)[0]:
                def author_date(match):
                    method=match.group(0)
                    if "Dates;->bindAuthor" in method:return method
                    changed,hits=re.subn(r"(?m)^([ \t]*return-void)","    invoke-static/range {p0 .. p0}, "+DATES+"->bindAuthor(Ljava/lang/Object;)V\n"+r"\1",method)
                    if hits:counts["native author date slot"]=counts.get("native author date slot",0)+hits
                    return changed
                text=re.sub(r"(?ms)^\.method public final (?:onViewCreated\(Landroid/view/View;\)V|er\(Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;\)V|s4\(Ljava/lang/Object;\)V).*?^\.end method",author_date,text)
            if "Lcom/ss/android/ugc/aweme/feed/assem/desc/VideoDescAssem;" in text.split("\n",1)[0]:
                def selected_date(match):
                    method=match.group(0)
                    if "onHolderSelected: awemeId=" not in method or "Dates;->bindPost" in method:return method
                    registers=re.search(r"\.registers (\d+)",method)
                    if not registers or int(registers.group(1))>16:return method
                    aid=re.search(r"invoke-virtual \{([vp]\d+)\}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid\(\)Ljava/lang/String;",method)
                    if not aid:return method
                    register=aid.group(1)
                    pattern=r"(?m)^([ \t]*if-eqz "+register+r", :[^\n]+)"
                    def bind(m):return "    invoke-static {p0, "+register+"}, "+DATES+"->bindPost(Ljava/lang/Object;Ljava/lang/Object;)V\n"+m.group(1)
                    changed,hits=re.subn(pattern,bind,method,count=1)
                    if hits:counts["visible publication date"]=counts.get("visible publication date",0)+hits
                    return changed
                text=re.sub(r"(?ms)^\.method public final [a-zA-Z0-9_]+\(ILcom/ss/android/ugc/aweme/feed/model/Aweme;\)V.*?^\.end method",selected_date,text)
            if text != before:
                with open(path, "w", encoding="utf-8") as handle:
                    handle.write(text)
    return counts

FLAG_READERS = ("LX/09tK;", "Lvq4/m;")
FLAG_READER = FLAG_READERS[0]
FLAG_ANSWERS = {
    "Z": ("forcedBoolean", "Ljava/lang/Boolean;", "booleanValue()Z", ""),
    "I": ("forcedInt", "Ljava/lang/Integer;", "intValue()I", ""),
    "J": ("forcedLong", "Ljava/lang/Long;", "longValue()J", "-wide"),
    "F": ("forcedFloat", "Ljava/lang/Float;", "floatValue()F", ""),
    "D": ("forcedDouble", "Ljava/lang/Double;", "doubleValue()D", "-wide"),
    "Ljava/lang/String;": ("forcedString", "Ljava/lang/String;", None, "-object"),
}
FLAG_LABEL = "native flag reader"

def _parameters(descriptor: str) -> List[str]:
    out = []
    at = 0
    while at < len(descriptor):
        start = at
        while descriptor[at] == "[":
            at += 1
        at = descriptor.index(";", at) + 1 if descriptor[at] == "L" else at + 1
        out.append(descriptor[start:at])
    return out

def defines(dex: bytes, descriptor: str) -> bool:
    wanted = descriptor.encode()
    if wanted not in dex or len(dex) < 112:
        return False
    string_ids_off = struct.unpack_from("<I", dex, 60)[0]
    type_ids_off = struct.unpack_from("<I", dex, 68)[0]
    class_defs_size, class_defs_off = struct.unpack_from("<2I", dex, 96)
    for i in range(class_defs_size):
        index = struct.unpack_from("<I", dex, class_defs_off + 32 * i)[0]
        string = struct.unpack_from("<I", dex, type_ids_off + 4 * index)[0]
        at = struct.unpack_from("<I", dex, string_ids_off + 4 * string)[0]
        length, at = _uleb(dex, at)
        if length == len(wanted) and dex[at:at + length] == wanted:
            return True
    return False

def rewrite_flag_reader(root: str, dex: Optional[bytes] = None) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    if dex is not None and not any(defines(dex, r) for r in FLAG_READERS):
        return counts
    for directory, _, files in os.walk(root):
        for name in files:
            is_candidate = bool(re.fullmatch(r"09tK(?:\.\d+)?\.smali", name)) or (
                bool(re.fullmatch(r"m(?:\.\d+)?\.smali", name)) and os.path.basename(directory) == "vq4"
            )
            if not is_candidate:
                continue
            path = os.path.join(directory, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            if not text.startswith(".class") or not any(r in text.split("\n", 1)[0] for r in FLAG_READERS):
                continue

            def ask(match):
                head, descriptor, returned, registers, body = match.groups()
                answer = FLAG_ANSWERS.get(returned)
                types = _parameters(descriptor)
                if (returned == "Ljava/lang/Object;" and types[:1] == ["Ljava/lang/Class;"]
                        and "Ljava/lang/String;" in types and " static " not in head):
                    if FLAGS + "->forcedObject" in body:
                        counts[FLAG_LABEL] = counts.get(FLAG_LABEL, 0) + 1
                        return match.group(0)
                    key = 1 + sum(2 if kind in ("J", "D") else 1 for kind in types[:types.index("Ljava/lang/String;")])
                    taken = 1 + sum(2 if kind in ("J", "D") else 1 for kind in types)
                    if int(registers) - taken < 2:
                        return match.group(0)
                    lines = ["    move-object/from16 v0, p1",
                             "    move-object/from16 v1, p%d" % key,
                             "    invoke-static {v0, v1}, %s->forcedObject(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Object;" % FLAGS,
                             "    move-result-object v0",
                             "    if-eqz v0, :margyt_flag",
                             "    return-object v0",
                             "    :margyt_flag"]
                    counts[FLAG_LABEL] = counts.get(FLAG_LABEL, 0) + 1
                    rows = body.split("\n")
                    inside = False
                    at = 0
                    for at, row in enumerate(rows):
                        word = row.strip()
                        if inside:
                            inside = not word.startswith(".end annotation")
                        elif word.startswith(".annotation"):
                            inside = True
                        elif word and not word.startswith((".prologue", ".param", ".end param")):
                            break
                    rows[at:at] = ["\n\n".join(lines) + "\n"]
                    return "%s(%s)%s\n    .registers %s\n%s" % (head, descriptor, returned, registers,
                                                                 "\n".join(rows))
                if answer is None or "Ljava/lang/String;" not in types or " static " in head:
                    return match.group(0)
                if FLAGS + "->forced" in body:
                    counts[FLAG_LABEL] = counts.get(FLAG_LABEL, 0) + 1
                    return match.group(0)
                key = 1
                for kind in types[:types.index("Ljava/lang/String;")]:
                    key += 2 if kind in ("J", "D") else 1
                taken = 1 + sum(2 if kind in ("J", "D") else 1 for kind in types)
                if int(registers) - taken < 2:
                    raise RuntimeError("TikTok's experiment reader has no register to spare in %s" % head)
                method, boxed, unbox, wide = answer
                lines = ["    invoke-static/range {p%d .. p%d}, %s->%s(Ljava/lang/String;)%s"
                         % (key, key, FLAGS, method, boxed),
                         "    move-result-object v0",
                         "    if-eqz v0, :margyt_flag"]
                if unbox:
                    lines += ["    invoke-virtual {v0}, %s->%s" % (boxed, unbox),
                              "    move-result%s v0" % wide]
                lines += ["    return%s v0" % wide, "    :margyt_flag"]
                counts[FLAG_LABEL] = counts.get(FLAG_LABEL, 0) + 1
                rows = body.split("\n")
                inside = False
                at = 0
                for at, row in enumerate(rows):
                    word = row.strip()
                    if inside:
                        inside = not word.startswith(".end annotation")
                    elif word.startswith(".annotation"):
                        inside = True
                    elif word and not word.startswith((".prologue", ".param", ".end param")):
                        break
                rows[at:at] = ["\n\n".join(lines) + "\n"]
                return "%s(%s)%s\n    .registers %s\n%s" % (head, descriptor, returned, registers,
                                                             "\n".join(rows))

            changed = re.sub(
                r"(?ms)^(\.method [^\n(]*)\(([^)\n]*)\)(\S+)\n    \.registers (\d+)\n(.*?^\.end method)",
                ask, text)
            if changed != text:
                with open(path, "w", encoding="utf-8") as handle:
                    handle.write(changed)
    return counts

def rewrite_hdr_switch(root: str, dex: Optional[bytes] = None) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    if dex is not None and not defines(dex, PLAYER_CONFIG):
        return counts
    path = os.path.join(root, *PLAYER_CONFIG[1:-1].split("/")) + ".smali"
    if not os.path.exists(path):
        return counts
    with open(path, encoding="utf-8") as handle:
        text = handle.read()
    match = re.search(r"(?ms)^\.method public isForceHdrOff\(\)Z\n    \.registers (\d+)\n(.*?)^\.end method", text)
    if match is None:
        return counts
    counts[HDR_LABEL] = 1
    if HDR_QUESTION in match.group(2):
        return counts
    if int(match.group(1)) < 2:
        raise RuntimeError("TikTok's HDR switch has no register to spare")
    rows = match.group(2).split("\n")
    at = 0
    for at, row in enumerate(rows):
        word = row.strip()
        if word and not word.startswith((".prologue", ".param", ".end param", ".annotation", ".end annotation")):
            break
    rows[at:at] = ["    invoke-static {}, %s\n\n    move-result v0\n\n    if-eqz v0, :margyt_hdr\n\n"
                   "    const/4 v0, 0x1\n\n    return v0\n\n    :margyt_hdr\n" % HDR_QUESTION]
    text = text[:match.start(2)] + "\n".join(rows) + text[match.end(2):]
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(text)
    return counts

SAFELY = "Lcat/narezany/margyt/Safely;"
for anchor in ("LX/0TFu;", "LX/0TVv;"):
    ANCHORED_SOURCES.append((anchor, "Landroid/content/Context;", "getString",
        "(I)Ljava/lang/String;", "(Landroid/content/Context;I)Ljava/lang/String;", SAFELY))
    CLASS_ANCHORED_SOURCES.add(anchor)

STARTER = "Landroidx/activity/ComponentActivity;"
START_LABEL = "native start"
START_ROUTE = "Lcat/narezany/margyt/Launcher;->route(Landroid/content/Intent;)V"

def rewrite_starts(root: str, dex: Optional[bytes] = None) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    if dex is not None and not defines(dex, STARTER):
        return counts
    path = os.path.join(root, *STARTER[1:-1].split("/")) + ".smali"
    if not os.path.exists(path):
        return counts
    with open(path, encoding="utf-8") as handle:
        text = handle.read()

    def shown(match):
        head, body = match.group(1), match.group(2)
        counts[START_LABEL] = counts.get(START_LABEL, 0) + 1
        if START_ROUTE in body:
            return match.group(0)
        rows = body.split("\n")
        inside = False
        at = 0
        for at, row in enumerate(rows):
            word = row.strip()
            if inside:
                inside = not word.startswith(".end annotation")
            elif word.startswith(".annotation"):
                inside = True
            elif word and not word.startswith((".registers", ".prologue", ".param", ".end param")):
                break
        rows[at:at] = ["    invoke-static {p1}, %s\n" % START_ROUTE]
        return head + "\n".join(rows)

    changed = re.sub(
        r"(?ms)^(\.method public startActivityForResult\(Landroid/content/Intent;I(?:Landroid/os/Bundle;)?\)V\n)(.*?^\.end method)",
        shown, text)
    if changed != text:
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(changed)
    return counts

LITE_MENU = "Lcat/narezany/margyt/LiteMenu;"
LITE_MENU_LABEL = "native menu row"
LITE_MENU_SHEET = '"MoreActionDialog"'
LITE_MENU_ROW = re.compile(
    r"    new-instance (?P<row>[vp]\d+), (?P<kind>L[^;]+;)\n(?:[ \t]*\n|[ \t]*\.line \d+\n)*"
    r"    invoke-direct \{(?P=row)\}, (?P=kind)-><init>\(\)V\n(?:[ \t]*\n|[ \t]*\.line \d+\n)*"
    r"    const [vp]\d+, (?P<icon>0x[0-9a-f]+)\n(?:[ \t]*\n|[ \t]*\.line \d+\n)*"
    r"    invoke-virtual \{(?P=row), [vp]\d+\}, (?P<pictured>L[^;]+;->\w+\(I\)V)\n"
    r"(?P<between>.*?)"
    r"    iput-object [vp]\d+, (?P=row), (?P<titled>L[^;]+;->\w+:Ljava/lang/String;)\n"
    r"(?P<after>.*?)"
    r"    invoke-virtual \{(?P=row), [vp]\d+\}, (?P<tapped>L[^;]+;->\w+\(Lkotlin/jvm/functions/Function1;\)V)\n",
    re.S)
LITE_MENU_LIST = re.compile(
    r"    new-instance (?P<list>v\d+), Ljava/util/ArrayList;\n(?:[ \t]*\n|[ \t]*\.line \d+\n)*"
    r"    invoke-direct \{(?P=list)\}, Ljava/util/ArrayList;-><init>\(\)V\n")

def opens_the_menu(dex: bytes) -> bool:
    return LITE_MENU_SHEET.strip('"').encode() in dex

def rewrite_lite_menu(root: str, dex: Optional[bytes] = None) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    if dex is not None and not opens_the_menu(dex):
        return counts
    for folder, _folders, files in os.walk(root):
        for name in files:
            if not name.endswith(".smali"):
                continue
            path = os.path.join(folder, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            if LITE_MENU_SHEET not in text:
                continue
            at = text.find(LITE_MENU_SHEET)
            start = text.rfind("\n.method ", 0, at)
            end = text.find("\n.end method", at)
            if start < 0 or end < 0:
                continue
            body = text[start:end]
            if LITE_MENU in body:
                counts[LITE_MENU_LABEL] = counts.get(LITE_MENU_LABEL, 0) + 1
                continue
            made = LITE_MENU_LIST.search(body)
            rows = [row for row in LITE_MENU_ROW.finditer(body)
                    if len(row.group("between")) < 600 and len(row.group("after")) < 600]
            if not made or not rows or made.start() > rows[0].start():
                continue
            row = rows[-1]
            if made.group("list") in ("v0", "v1") or body.find("move-result", 0, made.start()) >= 0:
                continue
            ours = (
                "\n    new-instance v0, {kind}\n\n"
                "    invoke-direct {{v0}}, {kind}-><init>()V\n\n"
                "    const v1, {icon}\n\n"
                "    invoke-virtual {{v0, v1}}, {pictured}\n\n"
                "    invoke-static {{}}, {mod}->title()Ljava/lang/String;\n\n"
                "    move-result-object v1\n\n"
                "    iput-object v1, v0, {titled}\n\n"
                "    invoke-static {{}}, {mod}->tapped()Ljava/lang/Object;\n\n"
                "    move-result-object v1\n\n"
                "    check-cast v1, Lkotlin/jvm/functions/Function1;\n\n"
                "    invoke-virtual {{v0, v1}}, {tapped}\n\n"
                "    invoke-virtual {{{list}, v0}}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z\n"
            ).format(kind=row.group("kind"), icon=row.group("icon"), pictured=row.group("pictured"),
                     titled=row.group("titled"), tapped=row.group("tapped"), mod=LITE_MENU,
                     list=made.group("list"))
            body = body[:made.end()] + ours + body[made.end():]
            text = text[:start] + body + text[end:]
            with open(path, "w", encoding="utf-8") as handle:
                handle.write(text)
            counts[LITE_MENU_LABEL] = counts.get(LITE_MENU_LABEL, 0) + 1
    return counts

LITE_NET = "Lcat/narezany/margyt/LiteNet;"
LITE_NET_LABEL = "native request drop"
URL_DISPATCHER = "Lcom/bytedance/frameworks/baselib/network/http/ok3/impl/urldispatcher/URLDispatcher;"
LITE_NET_ASK = "%s->drops()Z" % LITE_NET
LITE_NET_DROP = re.compile(
    r"(?P<wipe>    const-string (?P<held>v\d+), \"\"\n(?:[ \t]*\n|[ \t]*\.line \d+\n)*"
    r"    invoke-virtual \{[vp]\d+, [vp]\d+, (?P=held)\}, Ljava/util/ArrayList;->set\(ILjava/lang/Object;\)Ljava/lang/Object;\n"
    r"(?:[ \t]*\n|[ \t]*\.line \d+\n)*"
    r"    sget-object (?P=held), (?P<kind>L[^;]+;)->DISPATCH_DROP:(?P=kind)\n(?:[ \t]*\n|[ \t]*\.line \d+\n)*"
    r"    return-object (?P=held)\n)")

def rewrite_lite_drops(root: str, dex: Optional[bytes] = None) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    if dex is not None and not defines(dex, URL_DISPATCHER):
        return counts
    for folder, _folders, files in os.walk(root):
        for name in files:
            if not name.endswith(".smali"):
                continue
            path = os.path.join(folder, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            if "DISPATCH_DROP" not in text:
                continue
            if LITE_NET_ASK in text:
                counts[LITE_NET_LABEL] = counts.get(LITE_NET_LABEL, 0) + text.count(LITE_NET_ASK)
                continue

            def asked(match):
                at = counts.get(LITE_NET_LABEL, 0)
                counts[LITE_NET_LABEL] = at + 1
                held, kind = match.group("held"), match.group("kind")
                return ("    invoke-static {}, %s\n\n" % LITE_NET_ASK
                        + "    move-result %s\n\n" % held
                        + "    if-nez %s, :margyt_drop_%d\n\n" % (held, at)
                        + "    sget-object %s, %s->DISPATCH_NONE:%s\n\n" % (held, kind, kind)
                        + "    return-object %s\n\n" % held
                        + "    :margyt_drop_%d\n" % at
                        + match.group("wipe"))

            changed = LITE_NET_DROP.sub(asked, text)
            if changed != text:
                with open(path, "w", encoding="utf-8") as handle:
                    handle.write(changed)
    return counts

SETTINGS_CELL = "Lcat/narezany/margyt/SettingsCell;"
SETTINGS_LABEL = "native settings row"
SETTINGS_ROWS = "LX/0MUp;"
SETTINGS_GROUP = "LX/1Htf;"
SETTINGS_ROW_METHOD = ".method public static final LIZ(LX/0MUl;ZZLX/0Lx7;I)V"
SETTINGS_FIRST_ROW = ("    invoke-static/range {v9 .. v14}, LX/1HtZ;->LIZIZ(ZZLcom/ss/android/ugc/aweme/setting/ui/"
                      "rvmpcompose/group/top/cell/ManagePostsVM;LX/0Lx7;II)V\n")

def _class_file(root: str, descriptor: str) -> Optional[str]:
    path = os.path.join(root, *descriptor[1:-1].split("/")) + ".smali"
    return path if os.path.exists(path) else None

def rewrite_settings_row(root: str, dex: Optional[bytes] = None) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    if dex is None or defines(dex, SETTINGS_ROWS):
        path = _class_file(root, SETTINGS_ROWS)
        if path:
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            if ".method public static final margyt(LX/0MUl;ZZLX/0Lx7;I)V" in text:
                counts[SETTINGS_LABEL] = counts.get(SETTINGS_LABEL, 0) + 1
            else:
                start = text.find(SETTINGS_ROW_METHOD)
                end = text.find(".end method", start) if start >= 0 else -1
                if end >= 0:
                    body = text[start:end + len(".end method")]
                    copy = body.replace(SETTINGS_ROW_METHOD,
                                        ".method public static final margyt(LX/0MUl;ZZLX/0Lx7;I)V", 1)
                    copy, titled = re.subn(
                        r"    const v0, 0x[0-9a-f]+\n(?:[ \t]*\n|[ \t]*\.line \d+\n)*"
                        r"    invoke-static \{v0, v6\}, LX/0L52;->LIZIZ\(ILX/0Lx7;\)Ljava/lang/String;",
                        "    invoke-static {}, %s->title()Ljava/lang/String;" % SETTINGS_CELL, copy)
                    copy, pictured = re.subn(r"sget-object v0, LX/0LMt;->n:LX/0LPz;",
                                             "sget-object v0, LX/0LMt;->o:LX/0LPz;", copy)
                    copy, again = re.subn(
                        r"    new-instance v0, Lkotlin/jvm/internal/AwS1S0121000_8;\n.*?"
                        r"invoke-direct/range \{v6 \.\. v11\}, Lkotlin/jvm/internal/AwS1S0121000_8;-><init>\(LX/0MUl;ZZII\)V",
                        "    invoke-static {v5, v4, v3, v2}, %s->again(Ljava/lang/Object;ZZI)Ljava/lang/Object;\n\n"
                        "    move-result-object v0\n\n"
                        "    check-cast v0, Lkotlin/jvm/functions/Function2;" % SETTINGS_CELL, copy, flags=re.S)
                    if titled == 1 and pictured == 1 and again == 1:
                        text = text[:end + len(".end method")] + "\n\n" + copy + text[end + len(".end method"):]
                        with open(path, "w", encoding="utf-8") as handle:
                            handle.write(text)
                        counts[SETTINGS_LABEL] = counts.get(SETTINGS_LABEL, 0) + 1
    if dex is None or defines(dex, SETTINGS_GROUP):
        path = _class_file(root, SETTINGS_GROUP)
        if path:
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            asks = "%s->draw(ZLjava/lang/Object;)Z" % SETTINGS_CELL
            if asks in text:
                counts[SETTINGS_LABEL] = counts.get(SETTINGS_LABEL, 0) + 1
            elif text.count(SETTINGS_FIRST_ROW) == 1:
                text = text.replace(SETTINGS_FIRST_ROW,
                                    "    move v1, v9\n\n"
                                    "    invoke-static {v9, v6}, %s\n\n"
                                    "    move-result v9\n\n" % asks
                                    + SETTINGS_FIRST_ROW
                                    + "\n    move v9, v1\n")
                with open(path, "w", encoding="utf-8") as handle:
                    handle.write(text)
                counts[SETTINGS_LABEL] = counts.get(SETTINGS_LABEL, 0) + 1
    return counts

PLAYER_SETTINGS = "LX/0nY8;"
PLAYER_VIEW_CONFIG = "LX/0qS5;"

def carries_an_anchor(dex: bytes) -> bool:
    return (any(anchor.encode() in dex for anchor, *_rest in ANCHORED_SOURCES)
            or b"onSurfaceTextureUpdated" in dex
            or FEED_ITEM_LIST.encode() in dex
            or b"Lcom/ss/android/ugc/aweme/comment/longpresspanel/ui/CommentLongPressFragment;" in dex
            or b"LX/0G2p;" in dex
            or b"Lcom/bytedance/lobby/internal/LobbyInvisibleActivity;" in dex
            or b"Lcom/ss/android/ugc/aweme/account/login/authorize/AuthorizeActivity;" in dex
            or b"LX/0wiL;" in dex or b"Lkotlin/jvm/internal/AwS704S0100000_26;" in dex
            or b"Lcom/ss/android/ugc/aweme/offlinemode/viewmodel/OfflineModeManagerVM;" in dex
            or b"Lcom/ss/android/ugc/aweme/comment/commentlist/api/CommentApi;" in dex
            or b"Lcom/ss/android/ugc/aweme/commentv2/commentlist/CommentApiV2;" in dex
            or b"Lcom/ss/android/ugc/aweme/commentv2/commentlist/CommentApiV2SplitJson;" in dex
            or b"LX/0oah;" in dex
            or b"LX/0wjq;" in dex
            or b"Lcom/ss/android/ugc/aweme/comment/commentlist/api/CommentApi;" in dex
            or b"LX/0oah;" in dex
            or b"Lcom/ss/android/ugc/aweme/commentv2/commentlist/CommentApiV2;" in dex
            or b"Lcom/ss/android/ugc/aweme/commentv2/commentlist/CommentApiV2SplitJson;" in dex
            or b"LX/0wk3;" in dex or b"Lcom/ss/android/ugc/aweme/offlinemode/ui/OfflineModeContentComponent;" in dex
            or b"Lcom/ss/android/ugc/aweme/offlinemode/ui/bottom/OfflineModeDetailBottomComponent;" in dex
            or b"Lcom/ss/android/ugc/aweme/offlinemode/ui/sheet/OfflineModeSheetPageAssem;" in dex
            or b"audio_comment_publish" in dex or b"profile_bg_in_allow_list" in dex
            or any(defines(dex, r) for r in FLAG_READERS)
            or b"Lcom/ss/android/ugc/aweme/feed/assem/videoauthorinfo/VideoAuthorInfoRelationAssem;" in dex
            or any(owner.encode() in dex for owner in RAIL_CLASSES)
            or PLAYER_CONTROLLER.encode() in dex
            or any(owner.encode() in dex for owner in (REPOST_CLASS,AVATAR_CLASS,PROFILE_CONTAINER,PROFILE_HEADER,PUSH_GUIDE)+MESSAGE_CLASSES)
            or SPEED_TOUCH_CLASS.encode() in dex or SPEED_EDGE_CLASS.encode() in dex
            or PLAYER_SETTINGS.encode() in dex
            or defines(dex, PLAYER_VIEW_CONFIG))

def rewrite_speed_touch(root: str) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    found_class = False
    for dirpath, _dirs, files in os.walk(root):
        for name in files:
            if not name.endswith(".smali"):
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            if not text.startswith(".class "):
                continue
            declaration = text.split("\n", 1)[0]
            edge = SPEED_EDGE_CLASS in declaration
            if not edge and SPEED_TOUCH_CLASS not in declaration:
                continue
            label = "hold speed, edge touch" if edge else "hold speed, feed touch"
            found_class = True
            start = text.find(SPEED_EDGE_METHOD if edge else SPEED_TOUCH_METHOD)
            if start < 0:
                continue
            end = text.find(".end method", start)
            if end < 0:
                continue
            method = text[start:end]
            if SPEED_TOUCH_TARGET in method:
                counts[label] = 1
                continue
            anchor = "    :cond_9\n" if edge else "    :cond_27\n"
            if method.count(anchor) != 1:
                continue
            replacement = method.replace(anchor, anchor + SPEED_TOUCH_HOOK, 1)
            text = text[:start] + replacement + text[end:]
            with open(path, "w", encoding="utf-8") as handle:
                handle.write(text)
            counts[label] = 1
    return counts

LOADING = "Lcat/narezany/margyt/Loading;"
LOADING_DRAW = re.compile(
    r"\.method public (?:final )?onDraw\(Landroid/graphics/Canvas;\)V\n"
    r"[ \t]*\.registers (\d+)\n(?:[ \t]*\n)*(?:[ \t]*\.prologue\n)?")
LOADING_HOOK = """\
    invoke-static/range {p0 .. p1}, %s->draw(Landroid/view/View;Landroid/graphics/Canvas;)Z

    move-result v0

    if-eqz v0, :margyt_loading_native

    return-void

    :margyt_loading_native

    The views that draw them have no names left, so they are known by what
    they are: a plain View, with a `setProgressBarInfo` of its own and a
    PorterDuffXfermode for where the circles cross. There are two, an old one
    and its Tux replacement, and both are still used.

    One call goes at the very top of `onDraw`, before the method has put
    anything in a register, so the register its answer lands in is free by
    definition. If the mod drew, the method ends there; if not it runs on as
    though nothing had been added.

    The cell a cover sits in kept its name, so there is no looking for it: it
    is one file, and each of its constructors gets a call before it returns.
    if dex is not None and not defines(dex, RECALL_UPDATER):
        return {}
    path = _class_file(root, RECALL_UPDATER)
    if not path or not os.path.isfile(path):
        return {}
    with open(path, encoding="utf-8") as handle:
        text = handle.read()
    changed, hits = RECALL_CALL.subn(lambda m: "invoke-static {%s}, %s" % (m.group(1), RECALL_TARGET), text)
    if hits:
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(changed)
    return {RECALL_LABEL: hits} if hits else {}

def rewrite_frames(root: str, dex: Optional[bytes] = None) -> Dict[str, int]:
    if dex is not None and not defines(dex, IMAGE_LOADER):
        return {}
    path = _class_file(root, IMAGE_LOADER)
    if not path or not os.path.isfile(path):
        return {}
    with open(path, encoding="utf-8") as handle:
        text = handle.read()
    at = text.find(IMAGE_DISPLAY)
    if at < 0 or FRAMES in text:
        return {}
    registers = re.compile(r"(?m)^    \.(registers|locals) \d+\n")
    found = registers.search(text, at)
    if not found:
        return {}
    changed = text[:found.end()] + "\n" + FRAME_HOOK + text[found.end():]
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(changed)
    return {FRAME_LABEL: 1}

def touches_a_model(dex: bytes) -> bool:
    for owner, name, _original, _replacement, _target in MODEL_SOURCES + MODEL_STATICS:
        theirs = name[0] if isinstance(name, tuple) else name
        if owner.encode() in dex and theirs.encode() in dex:
            return True
    for owner, field, _kind, _name, _target in FIELD_SOURCES:
        if owner.encode() in dex and field.encode() in dex:
            return True
    for name, _original, _replacement, _target in WILD_SOURCES:
        if name.encode() in dex:
            return True
    for label, _descriptor, _ours, _target in DISCOVERED_STATICS + DISCOVERED_VIRTUALS:
        found = FOUND.get(label)
        if found and found[0].encode() in dex and found[1].encode() in dex:
            return True
    return False

def reads_a_colour(dex: bytes) -> bool:
    for owner, name, _original, _replacement in COLOUR_SOURCES:
        if owner.encode() in dex and name.encode() in dex:
            return True
    for owner, name, _signature in COLOUR_STATICS:
        if owner.encode() in dex and name.encode() in dex:
            return True
    return False

def holds_the_pink(dex: bytes) -> bool:
    needle = struct.pack("<I", TIKTOK_PINK)
    at = dex.find(needle)
    while at != -1:
        if at >= 2 and dex[at - 2] == 0x14:
            return True
        at = dex.find(needle, at + 1)
    return False

def _required(pattern) -> str:
    try:
        from re import _parser as parser
    except ImportError:
        import sre_parse as parser
    if pattern.flags & re.IGNORECASE:
        return ""
    best = run = ""
    try:
        pieces = list(parser.parse(pattern.pattern, pattern.flags))
    except Exception:
        return ""
    for kind, value in pieces + [(None, None)]:
        if str(kind) == "LITERAL":
            run += chr(value)
            continue
        if len(run) > len(best):
            best = run
        run = ""
    return best

def _literal_of(pattern) -> str:
    source = pattern.pattern
    for head in (r"(\{[^}]*\}), ", r"^(\s*)const ([vp]\d+), "):
        at = source.find(head)
        if at != -1:
            tail = source[at + len(head):]
            if tail.endswith("$"):
                tail = tail[:-1]
            return re.sub(r"\\(.)", r"\1", tail)
    return ""

def rewrite_accent(root: str) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    prepared = accent_rules()
    markers = [_literal_of(pattern) for _label, pattern, _target in prepared]
    feed_animation = "LX/1HkT;->setAnimation(Ljava/lang/String;)V"
    for dirpath, _dirs, files in os.walk(root):
        for name in files:
            if not name.endswith(".smali"):
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            before = text
            for (label, pattern, target), marker in zip(prepared, markers):
                if marker and marker not in text:
                    continue
                if label == "feed Lottie setAnimation" and feed_animation not in text:
                    continue
                text, hits = pattern.subn(target, text)
                if hits:
                    counts[label] = counts.get(label, 0) + hits
            if text != before:
                with open(path, "w", encoding="utf-8") as handle:
                    handle.write(text)
    return counts

def force_false(root: str) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    for class_name, signature in FORCED_FALSE:
        path = os.path.join(root, *class_name.split("/")) + ".smali"
        if not os.path.exists(path):
            continue
        with open(path, encoding="utf-8") as handle:
            text = handle.read()
        pattern = re.compile(
            r"^\.method ([^\n]*%s)\n.*?^\.end method$" % re.escape(signature),
            re.MULTILINE | re.DOTALL,
        )
        match = pattern.search(text)
        if match is None:
            raise RuntimeError(
                "%s is in the apk but has no %s to rewrite -- the fallback this "
                "depends on has moved, and Google sign-in would be dead on arrival"
                % (class_name, signature)
            )
        stub = ".method %s\n    .registers 1\n\n    const/4 v0, 0x0\n\n    return v0\n.end method" % (
            match.group(1),
        )
        text = text[: match.start()] + stub + text[match.end():]
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(text)
        counts["%s->%s" % (class_name.rsplit("/", 1)[-1], signature)] = 1
    return counts

def force_true(root: str) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    for class_name, signature in FORCED_TRUE:
        path = os.path.join(root, *class_name.split("/")) + ".smali"
        if not os.path.exists(path):
            continue
        with open(path, encoding="utf-8") as handle:
            text = handle.read()
        pattern = re.compile(
            r"^\.method ([^\n]*%s)\n.*?^\.end method$" % re.escape(signature),
            re.MULTILINE | re.DOTALL,
        )
        match = pattern.search(text)
        if match is None:
            continue
        stub = ".method %s\n    .registers 4\n\n    const/4 v0, 0x1\n\n    return v0\n.end method" % (
            match.group(1),
        )
        text = text[: match.start()] + stub + text[match.end():]
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(text)
        counts["%s->%s" % (class_name.rsplit("/", 1)[-1], signature)] = 1
    return counts

def force_void(root: str) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    for class_name, signature in FORCED_VOID:
        path = os.path.join(root, *class_name.split("/")) + ".smali"
        if not os.path.exists(path):
            continue
        with open(path, encoding="utf-8") as handle:
            text = handle.read()
        pattern = re.compile(
            r"^\.method ([^\n]*%s)\n.*?^\.end method$" % re.escape(signature),
            re.MULTILINE | re.DOTALL,
        )
        match = pattern.search(text)
        if match is None:
            continue
        stub = ".method %s\n    .registers 2\n\n    return-void\n.end method" % (
            match.group(1),
        )
        text = text[: match.start()] + stub + text[match.end():]
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(text)
        counts["%s->%s" % (class_name.rsplit("/", 1)[-1], signature)] = 1
    return counts

def rewrite_speedup(root: str) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    class_name = "com/ss/android/ugc/aweme/feed/longvideo/edgespeedup/EdgeSpeedupAssem"
    path = os.path.join(root, *class_name.split("/")) + ".smali"
    if not os.path.exists(path):
        return counts
    with open(path, encoding="utf-8") as handle:
        content = handle.read()
    pattern = re.compile(
        r"^\.method ([^\n]*Y52\(FF\)Z)\n.*?^\.end method$",
        re.MULTILINE | re.DOTALL,
    )
    match = pattern.search(content)
    if match is None:
        return counts
    decl = match.group(1)
    stub = (
        f".method {decl}\n"
        "    .registers 7\n\n"
        "    const/4 v1, 0x0\n\n"
        "    invoke-virtual {p0}, Lcom/ss/android/ugc/aweme/feed/longvideo/edgespeedup/EdgeSpeedupAssem;->nr()Lcom/ss/android/ugc/feed/platform/cell/ability/VideoDiggAssemAbility;\n"
        "    move-result-object v0\n"
        "    if-eqz v0, :cond_check_share\n"
        "    invoke-interface {v0, p1, p2}, Lcom/ss/android/ugc/feed/platform/cell/ability/VideoDiggAssemAbility;->Up0(FF)Z\n"
        "    move-result v0\n"
        "    if-eqz v0, :cond_check_share\n"
        "    return v1\n\n"
        "    :cond_check_share\n"
        "    invoke-virtual {p0}, Lcom/ss/android/ugc/aweme/feed/longvideo/edgespeedup/EdgeSpeedupAssem;->rr()Lcom/ss/android/ugc/aweme/feed/assem/ability/IVideoShareAbility;\n"
        "    move-result-object v0\n"
        "    if-eqz v0, :cond_check_comment\n"
        "    invoke-interface {v0, p1, p2}, Lcom/ss/android/ugc/aweme/feed/assem/ability/IVideoShareAbility;->zd1(FF)Z\n"
        "    move-result v0\n"
        "    if-eqz v0, :cond_check_comment\n"
        "    return v1\n\n"
        "    :cond_check_comment\n"
        "    invoke-virtual {p0}, Lcom/ss/android/ugc/aweme/feed/longvideo/edgespeedup/EdgeSpeedupAssem;->lr()Lcom/ss/android/ugc/aweme/feed/assem/ability/IVideoCommentAbility;\n"
        "    move-result-object v0\n"
        "    if-eqz v0, :cond_check_center\n"
        "    invoke-interface {v0, p1, p2}, Lcom/ss/android/ugc/aweme/feed/assem/ability/IVideoCommentAbility;->H30(FF)Z\n"
        "    move-result v0\n"
        "    if-eqz v0, :cond_check_center\n"
        "    return v1\n\n"
        "    :cond_check_center\n"
        "    invoke-virtual {p0}, Lcom/bytedance/assem/arch/reused/ReusedUIAssem;->getContainerView()Landroid/view/View;\n"
        "    move-result-object v0\n"
        "    if-eqz v0, :cond_ok\n"
        "    invoke-virtual {v0}, Landroid/view/View;->getWidth()I\n"
        "    move-result v0\n"
        "    if-lez v0, :cond_ok\n"
        "    int-to-float v0, v0\n"
        "    const v2, 0x3e6147ae\n"
        "    mul-float/2addr v2, v0\n"
        "    const v3, 0x3f47ae14\n"
        "    mul-float/2addr v3, v0\n"
        "    cmpl-float v2, p1, v2\n"
        "    if-lez v2, :cond_ok\n"
        "    cmpg-float v0, p1, v3\n"
        "    if-gez v0, :cond_ok\n"
        "    return v1\n\n"
        "    :cond_ok\n"
        "    const/4 v0, 0x1\n"
        "    return v0\n"
        ".end method"
    )
    content = content[: match.start()] + stub + content[match.end():]
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(content)
    counts["EdgeSpeedupAssem->Y52(FF)Z"] = 1
    return counts

def interesting(dex: bytes, literals: Optional[Dict[str, str]] = None) -> bool:
    for old in (literals or {}):
        if old.encode() in dex:
            return True
    if (b"tt_feed_like_tap_tux_lottie.json" in dex
            or b"tt_feed_like_tap_quick_tux_lottie.json" in dex
            or b"feed_fill_like/feed_fill_like_add_lottie.json" in dex):
        return True
    if (holds_the_pink(dex) or reads_a_colour(dex) or touches_a_model(dex)
            or carries_an_anchor(dex) or draws_the_loader(dex) or holds_a_card(dex)):
        return True
    for class_name, _signature in FORCED_FALSE:
        if ("L%s;" % class_name).encode() in dex:
            return True
    if b"Lcom/ss/android/ugc/aweme/feed/longvideo/edgespeedup/EdgeSpeedupAssem;" in dex:
        return True
    for class_name, _signature in FORCED_TRUE:
        if ("L%s;" % class_name).encode() in dex:
            return True
    for class_name, _signature in FORCED_VOID:
        if ("L%s;" % class_name).encode() in dex:
            return True
    if (defines(dex, PLAYER_CONFIG) or defines(dex, SETTINGS_ROWS) or defines(dex, SETTINGS_GROUP)
            or defines(dex, STARTER) or defines(dex, RECALL_UPDATER) or defines(dex, IMAGE_LOADER)):
        return True
    if opens_the_menu(dex) or defines(dex, URL_DISPATCHER):
        return True
    if b"ClipboardManager" in dex and b"setPrimaryClip" in dex:
        return True
    if TELEPHONY.encode() not in dex:
        return False
    return any(name.encode() in dex for name, _o, _r in TARGETS)

def rewrite_literals(root: str, literals: Dict[str, str]) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    if not literals:
        return counts
    for dirpath, _dirs, files in os.walk(root):
        for name in files:
            if not name.endswith(".smali"):
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            before = text
            for old, new in literals.items():
                needle = '"%s"' % old
                hits = text.count(needle)
                if hits:
                    text = text.replace(needle, '"%s"' % new)
                    counts[old] = counts.get(old, 0) + hits
            if text != before:
                with open(path, "w", encoding="utf-8") as handle:
                    handle.write(text)
    return counts

def rewrite_smali(root: str) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    prepared = rules()
    for dirpath, _dirs, files in os.walk(root):
        for name in files:
            if not name.endswith(".smali"):
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            if TELEPHONY not in text:
                continue
            before = text
            for label, pattern, target in prepared:
                text, hits = pattern.subn(target, text)
                if hits:
                    counts[label] = counts.get(label, 0) + hits
            if text != before:
                with open(path, "w", encoding="utf-8") as handle:
                    handle.write(text)
    return counts

class Smali:

    def __init__(self, jar: str, api: int, jobs: int = 0, heap: str = "4g"):
        self.jar = jar
        self.api = api
        self.jobs = jobs or int(os.environ.get("MARGYT_JOBS") or 0) or (os.cpu_count() or 2)
        self.heap = os.environ.get("MARGYT_HEAP") or heap

    def _run(self, main: str, args: List[str]) -> None:
        command = ["java", "-Xmx" + self.heap, "-cp", self.jar, main] + args
        result = subprocess.run(command, capture_output=True, text=True)
        if result.returncode != 0:
            raise RuntimeError(
                "%s failed:\n%s\n%s" % (main.split(".")[-2], result.stdout, result.stderr)
            )

    def disassemble(self, dex_path: str, out_dir: str) -> None:
        self._run(
            "com.android.tools.smali.baksmali.Main",
            ["d", "-a", str(self.api), "-j", str(self.jobs), "-o", out_dir, dex_path],
        )

    def assemble(self, smali_dir: str, dex_path: str) -> None:
        self._run(
            "com.android.tools.smali.smali.Main",
            ["a", "-a", str(self.api), "-j", str(self.jobs), "-o", dex_path, smali_dir],
        )

def dex_format(dex: bytes) -> str:
    return dex[4:7].decode("ascii", "replace")

def rewrite_targets() -> List[str]:
    out = ["%s->accent()I" % ACCENT]
    out.append(RECALL_TARGET)
    out.append("%s->display(Ljava/lang/Object;)V" % FRAMES)
    out.append("%s->setAnimation(Ljava/lang/Object;Ljava/lang/String;)V" % ACCENT)
    for name, _original, replacement in TARGETS:
        out.append("%s->%s%s" % (REGION, name, replacement))
    for _owner, name, _original, replacement in COLOUR_SOURCES:
        out.append("%s->%s%s" % (ACCENT, name, replacement))
    for _owner, name, signature in COLOUR_STATICS:
        out.append("%s->%s%s" % (ACCENT, name, signature))
    for _owner, name, _original, replacement, target in MODEL_SOURCES + MODEL_STATICS:
        _theirs, ours = name if isinstance(name, tuple) else (name, name)
        out.append("%s->%s%s" % (target, ours, replacement))
    for owner, _field, kind, name, target in FIELD_SOURCES:
        out.append("%s->%s(%s)%s" % (target, name, owner, kind))
    for _owner, _field, kind, name, target in STATIC_FIELD_SOURCES:
        out.append("%s->%s()%s" % (target, name, kind))
    for _anchor, _owner, name, _original, replacement, target in ANCHORED_SOURCES:
        out.append("%s->%s%s" % (target, name, replacement))
    out.append("Lcat/narezany/margyt/VideoActions;->captureShare(Ljava/lang/Object;)V")
    out.append("Lcat/narezany/margyt/Heart;->shown(Landroid/widget/ImageView;)V")
    out.extend(["Lcat/narezany/margyt/OfflineComments;->closed(Ljava/lang/Object;)V", "Lcat/narezany/margyt/OfflineComments;->nativeTask(Ljava/lang/String;)Ljava/lang/Object;", "Lcat/narezany/margyt/OfflineComments;->nativeObservable(Ljava/lang/String;)Ljava/lang/Object;"])
    out.extend(["Lcat/narezany/margyt/OfflineComments;->closed(Ljava/lang/Object;)V", "Lcat/narezany/margyt/OfflineComments;->nativeTask(Ljava/lang/String;)Ljava/lang/Object;", "Lcat/narezany/margyt/OfflineComments;->nativeObservable(Ljava/lang/String;)Ljava/lang/Object;"])
    out.extend(["Lcat/narezany/margyt/OfflineChoices;->counts(Ljava/util/List;)Ljava/util/List;", "Lcat/narezany/margyt/OfflineChoices;->row(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", "Lcat/narezany/margyt/OfflineChoices;->validCount(Ljava/util/List;Ljava/lang/Object;)Z", "Lcat/narezany/margyt/OfflineChoices;->legacy(Ljava/lang/Object;)V", "Lcat/narezany/margyt/OfflineComments;->cached(Ljava/lang/Object;Ljava/lang/String;)V", "Lcat/narezany/margyt/OfflineComments;->content(Ljava/lang/Object;)V", "Lcat/narezany/margyt/OfflineComments;->bottom(Landroid/view/View;)Landroid/view/View;"])
    out.append(SPEED_TOUCH_TARGET)
    out.extend(["%s->title()Ljava/lang/String;" % LITE_MENU, "%s->tapped()Ljava/lang/Object;" % LITE_MENU])
    out.append(LITE_NET_ASK)
    out.append(COMMENTS+"->mark(Landroid/view/View;)V")
    out.append(COMMENTS+"->openNativeActions(Ljava/lang/Object;)Z")
    out.append(COMMENTS+"->nativeItems(Ljava/lang/Object;Ljava/util/List;)Ljava/util/List;")
    out.append(FEED+"->withoutAds(Ljava/util/List;)Ljava/util/List;")
    out.append("Lcat/narezany/margyt/Repost;->textureChanged(Landroid/view/View;)V")
    out.append("Lcat/narezany/margyt/Accent;->dislike(Ljava/lang/Object;)V")
    out.append(COMMENTS+"->bindCell(Ljava/lang/Object;)V")
    out.append(RAIL+"->bindNative(Ljava/lang/Object;)V")
    out.append("Lcat/narezany/margyt/PlayerTools;->resumed(Ljava/lang/Object;Ljava/lang/String;)V")
    out.extend(["Lcat/narezany/margyt/Repost;->bind(Ljava/lang/Object;)V","Lcat/narezany/margyt/Repost;->anchor(Ljava/lang/Object;)V","Lcat/narezany/margyt/ProfileLayout;->bind(Ljava/lang/Object;Landroid/view/View;)V","Lcat/narezany/margyt/MessageTimes;->before(Ljava/lang/Object;)V","Lcat/narezany/margyt/MessageTimes;->bind(Ljava/lang/Object;Ljava/lang/Object;)V","Lcat/narezany/margyt/ModNotifications;->noPrompts()Z"])
    out.append("Lcat/narezany/margyt/ProfileParts;->attaching(Ljava/lang/Object;Landroid/view/View;Landroid/view/ViewGroup;)V")
    out.extend("%s->%s(Ljava/lang/String;)%s" % (FLAGS, method, boxed)
               for method, boxed, _unbox, _wide in FLAG_ANSWERS.values())
    out.append(HDR_QUESTION)
    out.append(START_ROUTE)
    out.extend(["%s->title()Ljava/lang/String;" % SETTINGS_CELL,
                "%s->again(Ljava/lang/Object;ZZI)Ljava/lang/Object;" % SETTINGS_CELL,
                "%s->draw(ZLjava/lang/Object;)Z" % SETTINGS_CELL])
    out.append(CORNERS + "->cell(Landroid/view/View;)V")
    out.append(LOADING + "->draw(Landroid/view/View;Landroid/graphics/Canvas;)Z")
    out.append(DATES+"->bindAuthor(Ljava/lang/Object;)V")
    out.append(DATES+"->bindPost(Ljava/lang/Object;Ljava/lang/Object;)V")
    for name, _original, replacement, target in WILD_SOURCES:
        out.append("%s->%s%s" % (target, name, replacement))
    for label, descriptor, ours, target in DISCOVERED_STATICS:
        if label in FOUND:
            out.append("%s->%s%s" % (target, ours, descriptor))
    for label, descriptor, ours, target in DISCOVERED_VIRTUALS:
        if label in FOUND:
            out.append("%s->%s%s"
                       % (target, ours, "(Ljava/lang/Object;" + descriptor[1:]))
    return out

def _uleb(data: bytes, at: int) -> Tuple[int, int]:
    value = shift = 0
    while True:
        byte = data[at]
        at += 1
        value |= (byte & 0x7F) << shift
        shift += 7
        if not byte & 0x80:
            return value, at

def defined_methods(dex: bytes) -> set:
    string_ids_off = struct.unpack_from("<I", dex, 60)[0]
    type_ids_off = struct.unpack_from("<I", dex, 68)[0]
    proto_ids_off = struct.unpack_from("<I", dex, 76)[0]
    method_ids_off = struct.unpack_from("<I", dex, 92)[0]
    class_defs_size, class_defs_off = struct.unpack_from("<2I", dex, 96)

    def string(index: int) -> str:
        at = struct.unpack_from("<I", dex, string_ids_off + 4 * index)[0]
        length, at = _uleb(dex, at)
        return dex[at:at + length].decode("utf-8", "replace")

    def type_name(index: int) -> str:
        return string(struct.unpack_from("<I", dex, type_ids_off + 4 * index)[0])

    def descriptor(index: int) -> str:
        _shorty, return_type, parameters = struct.unpack_from(
            "<3I", dex, proto_ids_off + 12 * index)
        arguments = ""
        if parameters:
            count = struct.unpack_from("<I", dex, parameters)[0]
            arguments = "".join(
                type_name(struct.unpack_from("<H", dex, parameters + 4 + 2 * i)[0])
                for i in range(count)
            )
        return "(%s)%s" % (arguments, type_name(return_type))

    def signature(index: int) -> str:
        owner, proto, name = struct.unpack_from("<HHI", dex, method_ids_off + 8 * index)
        return "%s->%s%s" % (type_name(owner), string(name), descriptor(proto))

    out = set()
    for i in range(class_defs_size):
        data_off = struct.unpack_from("<I", dex, class_defs_off + 32 * i + 24)[0]
        if not data_off:
            continue
        counts = []
        at = data_off
        for _ in range(4):
            value, at = _uleb(dex, at)
            counts.append(value)
        for _ in range(counts[0] + counts[1]):
            _diff, at = _uleb(dex, at)
            _flags, at = _uleb(dex, at)
        for methods in (counts[2], counts[3]):
            index = 0
            for step in range(methods):
                diff, at = _uleb(dex, at)
                _flags, at = _uleb(dex, at)
                _code, at = _uleb(dex, at)
                index = diff if step == 0 else index + diff
                out.add(signature(index))
    return out

def find_statics(dexes: Dict[str, bytes]) -> Dict[str, Tuple[str, str]]:
    wanted = {descriptor: label for label, descriptor, _ours, _target
              in DISCOVERED_STATICS + DISCOVERED_VIRTUALS}
    seen: Dict[str, set] = {label: set() for label in wanted.values()}

    for dex in dexes.values():
        for owner, name, descriptor in _static_methods(dex):
            label = wanted.get(descriptor)
            if label is not None:
                seen[label].add((owner, name))

    out: Dict[str, Tuple[str, str]] = {}
    for label, found in seen.items():
        if len(found) == 1:
            out[label] = next(iter(found))
    return out

def _static_methods(dex: bytes):
    strings = struct.unpack_from("<I", dex, 60)[0]
    types = struct.unpack_from("<I", dex, 68)[0]
    protos = struct.unpack_from("<I", dex, 76)[0]
    count, methods = struct.unpack_from("<2I", dex, 88)

    def text(index: int) -> str:
        at = struct.unpack_from("<I", dex, strings + 4 * index)[0]
        size, at = _uleb(dex, at)
        return dex[at:at + size].decode("utf-8", "replace")

    def kind(index: int) -> str:
        return text(struct.unpack_from("<I", dex, types + 4 * index)[0])

    def shape(index: int) -> str:
        _shorty, ret, params = struct.unpack_from("<3I", dex, protos + 12 * index)
        taken = ""
        if params:
            how_many = struct.unpack_from("<I", dex, params)[0]
            taken = "".join(kind(struct.unpack_from("<H", dex, params + 4 + 2 * i)[0])
                            for i in range(how_many))
        return "(%s)%s" % (taken, kind(ret))

    for i in range(count):
        owner, proto, name = struct.unpack_from("<HHI", dex, methods + 8 * i)
        yield kind(owner), text(name), shape(proto)

def missing_targets(dex: bytes) -> List[str]:
    defined = defined_methods(dex)
    return [target for target in rewrite_targets() if target not in defined]

def rewrite_everything(root: str, literals: Optional[Dict[str, str]] = None,
                       dex: Optional[bytes] = None) -> Dict[str, int]:
    counts = rewrite_smali(root)
    counts.update(rewrite_literals(root, literals or {}))
    counts.update(force_false(root))
    counts.update(force_true(root))
    counts.update(force_void(root))
    counts.update(rewrite_models(root))
    counts.update(rewrite_flag_reader(root, dex))
    counts.update(rewrite_hdr_switch(root, dex))
    counts.update(rewrite_settings_row(root, dex))
    counts.update(rewrite_starts(root, dex))
    counts.update(rewrite_lite_menu(root, dex))
    counts.update(rewrite_lite_drops(root, dex))
    counts.update(rewrite_anchored(root))
    counts.update(rewrite_accent(root))
    counts.update(rewrite_speed_touch(root))
    counts.update(rewrite_speedup(root))
    if dex is None or draws_the_loader(dex):
        counts.update(rewrite_loading(root))
    counts.update(rewrite_cards(root))
    counts.update(rewrite_recall(root, dex))
    counts.update(rewrite_frames(root, dex))
    if dex is None or b"setPrimaryClip" in dex:
        counts.update(rewrite_clipboard(root))
    return counts

CLIP_CALL = re.compile(r"invoke-virtual(/range)? (\{[^}]*\}), Landroid/content/ClipboardManager;->setPrimaryClip\(Landroid/content/ClipData;\)V")
CLIP_HOOK = r"invoke-static\1 \2, Lcat/narezany/margyt/ShareLinks;->setPrimaryClip(Landroid/content/ClipboardManager;Landroid/content/ClipData;)V"

def rewrite_clipboard(root: str) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    for dirpath, _dirs, files in os.walk(root):
        for name in files:
            if not name.endswith(".smali"):
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            if "ClipboardManager;->setPrimaryClip" not in text:
                continue
            text, hits = CLIP_CALL.subn(CLIP_HOOK, text)
            if hits:
                counts["clipboard links"] = counts.get("clipboard links", 0) + hits
                with open(path, "w", encoding="utf-8") as handle:
                    handle.write(text)
    return counts

def next_dex_name(names: List[str]) -> str:
    used = set()
    for name in names:
        match = re.fullmatch(r"classes(\d*)\.dex", name)
        if match:
            used.add(int(match.group(1) or "1"))
    number = 2
    while number in used:
        number += 1
    return "classes%d.dex" % number

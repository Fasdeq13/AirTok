package com.fasdeq.airtok;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

final class NewChip {

    private NewChip() {}

    static final String MARK = "⁣";

    static boolean marked(CharSequence title) {
        return title != null && title.length() > 0 && title.charAt(title.length() - 1) == MARK.charAt(0);
    }

    static String plain(CharSequence title) {
        String text = String.valueOf(title);
        return marked(text) ? text.substring(0, text.length() - 1) : text;
    }

    static TextView make(Context context) {
        float dp = context.getResources().getDisplayMetrics().density;
        TextView chip = new TextView(context);
        chip.setText(Text.NEW);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        chip.setLetterSpacing(0.06f);
        chip.setGravity(Gravity.CENTER);
        int accent = Accent.colour();
        int red = (accent >> 16) & 0xFF, green = (accent >> 8) & 0xFF, blue = accent & 0xFF;
        chip.setTextColor((red * 299 + green * 587 + blue * 114) / 1000 > 150 ? 0xFF111111 : 0xFFFFFFFF);
        GradientDrawable back = new GradientDrawable();
        back.setColor(accent);
        back.setCornerRadius(8 * dp);
        chip.setBackground(back);
        chip.setPadding(Math.round(7 * dp), Math.round(2 * dp), Math.round(7 * dp), Math.round(2 * dp));
        chip.setIncludeFontPadding(false);
        Fonts.apply(chip);
        return chip;
    }
}

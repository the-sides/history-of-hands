package com.jacobsides.hands;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** One compact visual system for the app, editors, and launcher picker. */
public final class Design {
  public static final int BG = Color.rgb(13, 16, 21),
      SURFACE = Color.rgb(23, 28, 36),
      RAISED = Color.rgb(31, 38, 48),
      LINE = Color.rgb(43, 52, 63),
      TEXT = Color.rgb(240, 244, 239),
      MUTED = Color.rgb(147, 161, 173),
      LIME = Color.rgb(208, 245, 129),
      CORAL = Color.rgb(255, 137, 126),
      BLUE = Color.rgb(140, 184, 255);

  public static int dp(Context c, float n) {
    return Math.round(n * c.getResources().getDisplayMetrics().density);
  }

  public static GradientDrawable background(int color, int radius, Context c) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(c, radius));
    return d;
  }

  public static TextView text(Context c, String value, int size, int color, boolean bold) {
    TextView t = new TextView(c);
    t.setText(value);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setTypeface(
        Typeface.create(c.getResources().getFont(R.font.manrope), bold ? 700 : 400, false));
    t.setFontVariationSettings(bold ? "'wght' 700" : "'wght' 450");
    t.setIncludeFontPadding(false);
    return t;
  }

  public static TextView label(Context c, String value) {
    TextView t = text(c, value.toUpperCase(java.util.Locale.ROOT), 10, MUTED, true);
    t.setLetterSpacing(.13f);
    return t;
  }

  public static LinearLayout column(Context c) {
    LinearLayout l = new LinearLayout(c);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  public static LinearLayout row(Context c) {
    LinearLayout l = new LinearLayout(c);
    l.setGravity(Gravity.CENTER_VERTICAL);
    return l;
  }

  public static LinearLayout card(Context c) {
    LinearLayout l = column(c);
    l.setPadding(dp(c, 20), dp(c, 20), dp(c, 20), dp(c, 20));
    l.setBackground(background(SURFACE, 24, c));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.bottomMargin = dp(c, 14);
    l.setLayoutParams(p);
    return l;
  }

  public static void gap(LinearLayout parent, int dp) {
    View v = new View(parent.getContext());
    parent.addView(v, new LinearLayout.LayoutParams(1, dp(parent.getContext(), dp)));
  }

  public static Button button(Context c, String value, boolean primary, Runnable action) {
    Button b = new Button(c);
    b.setText(value);
    b.setAllCaps(false);
    b.setTextSize(13);
    b.setTypeface(Typeface.create(c.getResources().getFont(R.font.manrope), 700, false));
    b.setFontVariationSettings("'wght' 700");
    b.setTextColor(primary ? BG : TEXT);
    b.setBackgroundTintList(ColorStateList.valueOf(primary ? LIME : RAISED));
    b.setBackground(background(primary ? LIME : RAISED, 16, c));
    b.setPadding(dp(c, 12), 0, dp(c, 12), 0);
    b.setMinimumHeight(dp(c, 48));
    b.setMinHeight(dp(c, 48));
    b.setStateListAnimator(null);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(c, 50));
    p.topMargin = dp(c, 8);
    b.setLayoutParams(p);
    b.setOnClickListener(v -> action.run());
    return b;
  }

  public static TextView chip(Context c, String value, boolean selected, Runnable action) {
    TextView t = text(c, value, 12, selected ? BG : MUTED, true);
    t.setSelected(selected);
    t.setGravity(Gravity.CENTER);
    t.setPadding(dp(c, 13), dp(c, 12), dp(c, 13), dp(c, 12));
    t.setMinHeight(dp(c, 44));
    t.setBackground(background(selected ? LIME : RAISED, 30, c));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
    p.rightMargin = dp(c, 7);
    t.setLayoutParams(p);
    t.setOnClickListener(v -> action.run());
    return t;
  }
}

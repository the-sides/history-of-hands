package com.jacobsides.hands;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class QuickLogActivity extends Activity {
  public void onCreate(Bundle state) {
    super.onCreate(state);
    getSystemService(android.content.pm.ShortcutManager.class)
        .reportShortcutUsed("throw-" + RoundStore.index(getIntent().getStringExtra("mine")));
    String mine = getIntent().getStringExtra("mine");
    if (RoundStore.index(mine) < 0) {
      finish();
      return;
    }
    LinearLayout box = new LinearLayout(this);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setPadding(28, 30, 28, 20);
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(Color.parseColor("#f4f0e7"));
    bg.setCornerRadius(36);
    box.setBackground(bg);
    TextView title = new TextView(this);
    title.setText(
        "You threw "
            + RoundStore.label(mine).toLowerCase(java.util.Locale.ROOT)
            + ".\nWhat did she throw?");
    title.setTextSize(22);
    title.setTextColor(Color.parseColor("#25392f"));
    title.setTypeface(Typeface.create("serif", Typeface.NORMAL));
    box.addView(title);
    LinearLayout choices = new LinearLayout(this);
    choices.setOrientation(LinearLayout.HORIZONTAL);
    choices.setPadding(0, 20, 0, 0);
    for (int i = 0; i < 3; i++) {
      final String hers = RoundStore.THROWS[i];
      Button button = new Button(this);
      button.setText(RoundStore.SYMBOLS[i] + "\n" + RoundStore.label(hers));
      button.setAllCaps(false);
      button.setTextSize(16);
      button.setBackgroundTintList(
          android.content.res.ColorStateList.valueOf(Color.parseColor("#e8eacb")));
      button.setTextColor(Color.parseColor("#25392f"));
      choices.addView(button, new LinearLayout.LayoutParams(0, 150, 1));
      button.setOnClickListener(
          v -> {
            try {
              RoundStore.get(this).log(mine, hers);
              String result = RoundStore.outcome(mine, hers);
              String message =
                  (result.equals("win")
                          ? "You win!"
                          : result.equals("loss") ? "She wins." : "A tie.")
                      + " Saved.";
              HandsWidget.status(this, message);
              HandsWidget.update(this);
              SyncJob.schedule(this);
              Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
              finish();
            } catch (Exception e) {
              Toast.makeText(this, "Couldn't save. Check storage in Hands.", Toast.LENGTH_LONG)
                  .show();
            }
          });
    }
    box.addView(choices);
    TextView hint = new TextView(this);
    hint.setText("Saves on your phone. No connection needed.");
    hint.setTextSize(11);
    hint.setTextColor(Color.parseColor("#72766b"));
    hint.setPadding(0, 15, 0, 0);
    box.addView(hint);
    setContentView(box);
    getWindow()
        .setLayout(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    getWindow().setGravity(Gravity.BOTTOM);
    setFinishOnTouchOutside(true);
  }
}

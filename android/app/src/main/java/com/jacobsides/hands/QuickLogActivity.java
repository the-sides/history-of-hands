package com.jacobsides.hands;

import static com.jacobsides.hands.Design.*;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class QuickLogActivity extends Activity {
  private String mine;

  public void onCreate(Bundle state) {
    super.onCreate(state);
    mine = state != null ? state.getString("mine") : getIntent().getStringExtra("mine");
    if (RoundStore.index(mine) < 0) mine = null;
    else
      getSystemService(android.content.pm.ShortcutManager.class)
          .reportShortcutUsed("throw-" + RoundStore.index(mine));
    render();
  }

  protected void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    state.putString("mine", mine);
  }

  private void render() {
    LinearLayout box = column(this);
    box.setPadding(dp(this, 22), dp(this, 20), dp(this, 22), dp(this, 24));
    box.setBackground(background(SURFACE, 28, this));
    TextView handle = text(this, "━━", 17, LINE, true);
    handle.setGravity(Gravity.CENTER);
    box.addView(handle);
    gap(box, 16);
    box.addView(label(this, mine == null ? "LOG A GAME  /  01" : "LOG A GAME  /  02"));
    gap(box, 12);
    box.addView(
        text(
            this,
            mine == null ? "What did you throw?" : "And what did she throw?",
            26,
            TEXT,
            true));
    gap(box, 10);
    box.addView(
        text(
            this,
            mine == null
                ? "Your throw, then hers. That’s the whole thing."
                : "You chose "
                    + RoundStore.label(mine).toLowerCase(java.util.Locale.ROOT)
                    + " "
                    + RoundStore.SYMBOLS[RoundStore.index(mine)]
                    + ". Tap hers to save.",
            12,
            MUTED,
            false));
    gap(box, 22);
    LinearLayout choices = row(this);
    for (int i = 0; i < 3; i++) {
      final String value = RoundStore.THROWS[i];
      LinearLayout choice = column(this);
      choice.setGravity(Gravity.CENTER);
      choice.setPadding(dp(this, 4), dp(this, 20), dp(this, 4), dp(this, 20));
      choice.setBackground(background(RAISED, 20, this));
      choice.addView(text(this, RoundStore.SYMBOLS[i], 36, TEXT, false));
      gap(choice, 14);
      choice.addView(text(this, RoundStore.label(value), 12, TEXT, true));
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(this, 124), 1);
      if (i < 2) p.rightMargin = dp(this, 10);
      choices.addView(choice, p);
      choice.setContentDescription(
          (mine == null ? "You" : "She") + " threw " + value.toLowerCase(java.util.Locale.ROOT));
      choice.setOnClickListener(
          v -> {
            if (mine == null) {
              mine = value;
              render();
            } else {
              try {
                RoundStore.get(this).log(mine, value);
                String outcome = RoundStore.outcome(mine, value);
                String message =
                    (outcome.equals("win")
                            ? "You win!"
                            : outcome.equals("loss") ? "She wins." : "A tie.")
                        + " Game saved.";
                HandsWidget.status(this, message);
                HandsWidget.update(this);
                SyncJob.schedule(this);
                SyncJob.notifyChanged(this);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                finish();
              } catch (Exception e) {
                Toast.makeText(
                        this, "Couldn't save. Check phone storage in Hands.", Toast.LENGTH_LONG)
                    .show();
              }
            }
          });
    }
    box.addView(choices);
    gap(box, 16);
    if (mine != null)
      box.addView(
          button(
              this,
              "↶  Change your throw",
              false,
              () -> {
                mine = null;
                render();
              }));
    gap(box, 12);
    box.addView(
        text(
            this,
            "●  Saved on your phone instantly. Correct any game in History.",
            10,
            MUTED,
            false));
    setContentView(box);
    getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    getWindow().setLayout(-1, -2);
    getWindow().setGravity(Gravity.BOTTOM);
    setFinishOnTouchOutside(true);
  }
}

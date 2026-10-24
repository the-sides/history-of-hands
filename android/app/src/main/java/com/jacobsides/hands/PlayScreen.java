package com.jacobsides.hands;

import static com.jacobsides.hands.Design.*;

import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;

final class PlayScreen {
  static void show(MainActivity a, LinearLayout root) {
    JSONArray rows = RoundStore.get(a).all(false);
    Analytics stats = new Analytics(rows, 0);
    root.addView(text(a, "A little rivalry.\nA better next move.", 30, TEXT, true));
    gap(root, 8);
    root.addView(
        text(
            a,
            stats.todayCount() == 0
                ? "Your scorecard is ready when you are."
                : stats.todayCount() + " games together today. Keep the story going.",
            12,
            MUTED,
            false));
    gap(root, 24);
    LinearLayout advice = card(a);
    GradientDrawable background =
        new GradientDrawable(
            GradientDrawable.Orientation.TL_BR, new int[] {0xff26382b, 0xff172721, 0xff172024});
    background.setCornerRadius(dp(a, 24));
    advice.setBackground(background);
    advice.addView(label(a, "YOUR NEXT MOVE"));
    gap(advice, 16);
    String value = stats.recommendation;
    String title = value.startsWith("Try ") ? value.split(" · ")[0] : "Keep her guessing.";
    advice.addView(text(a, title, 33, LIME, true));
    gap(advice, 12);
    advice.addView(
        text(
            a,
            value.startsWith("Try ")
                ? "A pattern worth trying. "
                    + value.substring(value.indexOf(" · ") + 3)
                    + " inform this suggestion."
                : value.startsWith("No clear")
                    ? "Her throws are balanced. There’s no clear edge yet—mix yours up."
                    : "Every throw teaches us a little more. Play a few games to start spotting"
                        + " patterns.",
            13,
            0xffb9cbbb,
            false));
    gap(advice, 17);
    advice.addView(text(a, "A hunch from your history. Never a guarantee.", 10, MUTED, false));
    root.addView(advice);
    LinearLayout score = row(a);
    String[] counts = {
      String.valueOf(stats.wins), String.valueOf(stats.losses), String.valueOf(stats.ties)
    };
    String[] labels = {"YOUR WINS", "HER WINS", "TIES"};
    int[] colors = {LIME, CORAL, MUTED};
    for (int i = 0; i < 3; i++) {
      LinearLayout box = card(a);
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
      if (i < 2) p.rightMargin = dp(a, 8);
      box.setLayoutParams(p);
      box.setPadding(dp(a, 14), dp(a, 16), dp(a, 10), dp(a, 16));
      box.addView(text(a, counts[i], 32, colors[i], true));
      gap(box, 6);
      TextView label = label(a, labels[i]);
      label.setTextSize(8);
      box.addView(label);
      score.addView(box);
    }
    root.addView(score);
    gap(root, 22);
    LinearLayout play = card(a);
    LinearLayout head = row(a);
    TextView titleView = text(a, "Make it count.", 20, TEXT, true);
    head.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1));

    play.addView(head);
    gap(play, 8);
    play.addView(text(a, "Your throw. Your result. One gesture.", 12, MUTED, false));
    gap(play, 16);
    LinearLayout throwsRow = row(a);
    for (int i = 0; i < 3; i++) {
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(a, 166), 1);
      if (i < 2) p.rightMargin = dp(a, 8);
      throwsRow.addView(new ThrowControl(a, i), p);
    }
    play.addView(throwsRow);
    gap(play, 12);
    play.addView(text(a, "Swipe ↑ win · ↓ loss · double-tap tie", 11, MUTED, false));
    gap(play, 5);
    play.addView(
        text(a, "Hold a throw for result choices. Undo any slip below.", 10, MUTED, false));
    root.addView(play);
    if (stats.total() > 0) {
      LinearLayout latest = card(a);
      latest.addView(label(a, "LAST GAME"));
      gap(latest, 12);
      var game = stats.games.get(stats.games.size() - 1);
      String result = RoundStore.outcome(game.optString("mine"), game.optString("hers"));
      latest.addView(
          text(
              a,
              (result.equals("win")
                  ? "Your little victory."
                  : result.equals("loss") ? "This one was hers." : "Evenly matched."),
              20,
              TEXT,
              true));
      gap(latest, 8);
      latest.addView(
          text(
              a,
              RoundStore.label(game.optString("mine"))
                  + " / "
                  + RoundStore.label(game.optString("hers"))
                  + "  ·  "
                  + MainActivity.when(game.optString("playedAt"), "MMM d, h:mm a"),
              12,
              MUTED,
              false));
      latest.addView(
          button(a, "View or correct this game  →", false, () -> GameEditor.show(a, game)));
      root.addView(latest);
    }
    root.addView(text(a, "FOR BETTER OR FOR SCISSORS.", 9, MUTED, true));
  }
}

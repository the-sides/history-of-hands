package com.jacobsides.hands;

import static com.jacobsides.hands.Design.*;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

final class InsightsScreen {
  static void show(MainActivity a, LinearLayout root) {
    Analytics data = new Analytics(RoundStore.get(a).all(false), a.analyticsDays);
    root.addView(text(a, "Know your rivalry.", 30, TEXT, true));
    gap(root, 10);
    root.addView(
        text(a, "Patterns, not promises. Every number comes from your games.", 12, MUTED, false));
    gap(root, 22);
    LinearLayout range = row(a);
    int[] days = {7, 30, 0};
    String[] labels = {"7 days", "30 days", "All time"};
    for (int i = 0; i < 3; i++) {
      final int d = days[i];
      range.addView(
          chip(
              a,
              labels[i],
              a.analyticsDays == d,
              () -> {
                a.analyticsDays = d;
                a.render(false);
              }));
    }
    root.addView(range);
    gap(root, 22);
    LinearLayout rate = card(a);
    rate.addView(label(a, "YOUR WIN RATE"));
    gap(rate, 10);
    rate.addView(text(a, data.rateLabel(), 60, LIME, true));
    rate.addView(
        text(
            a,
            data.wins
                + " wins / "
                + (data.wins + data.losses)
                + " decisive games  ·  "
                + data.ties
                + " ties excluded",
            11,
            MUTED,
            false));
    gap(rate, 20);
    LinearLayout facts = row(a);
    TextView games = text(a, data.total() + " games", 13, TEXT, true);
    facts.addView(games, new LinearLayout.LayoutParams(0, -2, 1));
    facts.addView(text(a, data.streakLabel(), 12, CORAL, true));
    rate.addView(facts);
    root.addView(rate);
    LinearLayout week = card(a);
    week.addView(text(a, "Your last seven days", 18, TEXT, true));
    gap(week, 8);
    LinearLayout legend = row(a);
    legend.addView(text(a, "● You", 10, LIME, true));
    TextView herLegend = text(a, "● Her", 10, CORAL, true);
    herLegend.setPadding(dp(a, 18), 0, 0, 0);
    legend.addView(herLegend);
    TextView tieLegend = text(a, "● Ties", 10, MUTED, true);
    tieLegend.setPadding(dp(a, 18), 0, 0, 0);
    legend.addView(tieLegend);
    week.addView(legend);
    week.addView(new WeekChart(a, data), new LinearLayout.LayoutParams(-1, dp(a, 160)));
    root.addView(week);
    LinearLayout mix = card(a);
    mix.addView(text(a, "Her comfort zone", 20, TEXT, true));
    gap(mix, 6);
    mix.addView(text(a, "How often she chooses each throw", 11, MUTED, false));
    for (int i = 0; i < 3; i++)
      bar(
          a,
          mix,
          RoundStore.SYMBOLS[i] + "  " + RoundStore.label(RoundStore.THROWS[i]),
          data.hers[i],
          data.total(),
          i == 0 ? LIME : i == 1 ? BLUE : CORAL);
    root.addView(mix);
    LinearLayout own = card(a);
    own.addView(text(a, "What works for you", 20, TEXT, true));
    gap(own, 8);
    own.addView(text(a, data.strongestThrow(), 12, LIME, true));
    gap(own, 8);
    for (int i = 0; i < 3; i++) {
      int decisive = data.throwWins[i] + data.throwLosses[i];
      bar(
          a,
          own,
          RoundStore.label(RoundStore.THROWS[i]) + " · " + data.mine[i] + " games",
          data.throwWins[i],
          decisive,
          LIME);
    }
    own.addView(
        text(
            a,
            "Win rates exclude ties. At least 5 decisive games per throw before naming a strongest"
                + " throw.",
            10,
            MUTED,
            false));
    root.addView(own);
    LinearLayout patterns = card(a);
    patterns.addView(text(a, "What happens next?", 20, TEXT, true));
    gap(patterns, 12);
    pattern(a, patterns, "After she loses", data.repeatAfterHerLoss, data.afterHerLoss);
    gap(patterns, 18);
    pattern(a, patterns, "After she wins", data.repeatAfterHerWin, data.afterHerWin);
    gap(patterns, 18);
    patterns.addView(label(a, "YOUR LONGEST WIN STREAK"));
    gap(patterns, 8);
    patterns.addView(text(a, data.bestStreak + " games", 24, LIME, true));
    root.addView(patterns);
    LinearLayout matrix = card(a);
    matrix.addView(text(a, "The matchup map", 20, TEXT, true));
    gap(matrix, 6);
    matrix.addView(
        text(a, "Rows: your throw. Columns: hers. Numbers count games.", 10, MUTED, false));
    gap(matrix, 14);
    LinearLayout columns = row(a);
    columns.addView(text(a, "", 12, MUTED, false), new LinearLayout.LayoutParams(dp(a, 35), -2));
    for (String symbol : RoundStore.SYMBOLS) {
      TextView t = text(a, symbol, 22, TEXT, false);
      t.setGravity(android.view.Gravity.CENTER);
      columns.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
    }
    matrix.addView(columns);
    for (int i = 0; i < 3; i++) {
      LinearLayout r = row(a);
      r.addView(
          text(a, RoundStore.SYMBOLS[i], 22, TEXT, false),
          new LinearLayout.LayoutParams(dp(a, 35), -2));
      for (int j = 0; j < 3; j++) {
        String outcome = RoundStore.outcome(RoundStore.THROWS[i], RoundStore.THROWS[j]);
        TextView v =
            text(
                a,
                String.valueOf(data.matchups[i][j]),
                17,
                outcome.equals("win") ? LIME : outcome.equals("loss") ? CORAL : MUTED,
                true);
        v.setGravity(android.view.Gravity.CENTER);
        v.setPadding(0, dp(a, 14), 0, dp(a, 14));
        r.addView(v, new LinearLayout.LayoutParams(0, -2, 1));
      }
      matrix.addView(r);
    }
    root.addView(matrix);
  }

  private static void pattern(MainActivity a, LinearLayout p, String label, int repeated, int n) {
    p.addView(text(a, label, 13, TEXT, true));
    gap(p, 6);
    p.addView(
        text(
            a,
            n < 5
                ? "Not enough examples yet (" + n + " of 5)."
                : "She repeats her throw " + Analytics.percent(repeated, n) + " of the time.",
            12,
            n < 5 ? MUTED : BLUE,
            false));
    p.addView(
        text(a, n + " observed transitions · adjacent games in this period", 9, MUTED, false));
  }

  private static void bar(
      MainActivity a, LinearLayout parent, String label, int value, int total, int color) {
    gap(parent, 18);
    LinearLayout row = row(a);
    row.addView(text(a, label, 12, TEXT, false), new LinearLayout.LayoutParams(0, -2, 1));
    row.addView(text(a, Analytics.percent(value, total), 12, color, true));
    parent.addView(row);
    LinearLayout track = row(a);
    track.setBackground(background(RAISED, 4, a));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(a, 5));
    p.topMargin = dp(a, 9);
    track.setLayoutParams(p);
    View filled = new View(a);
    filled.setBackground(background(color, 4, a));
    track.addView(filled, new LinearLayout.LayoutParams(0, -1, total == 0 ? 0 : value));
    View rest = new View(a);
    track.addView(
        rest, new LinearLayout.LayoutParams(0, -1, total == 0 ? 1 : Math.max(0, total - value)));
    parent.addView(track);
  }
}

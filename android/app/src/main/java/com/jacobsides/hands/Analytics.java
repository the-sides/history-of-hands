package com.jacobsides.hands;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Pure scorecard calculations shared by the Android screens. Ties never inflate win rate. */
public final class Analytics {
  public final List<JSONObject> games = new ArrayList<>();
  public int wins, losses, ties, currentStreak, bestStreak;
  public final int[] mine = new int[3],
      hers = new int[3],
      throwWins = new int[3],
      throwLosses = new int[3],
      dailyWins = new int[7],
      dailyLosses = new int[7],
      dailyTies = new int[7];
  public final int[][] matchups = new int[3][3];
  public int repeatAfterHerLoss, afterHerLoss, repeatAfterHerWin, afterHerWin;
  public String streakOutcome = "", recommendation;

  public Analytics(JSONArray source, int days) {
    LocalDate today = LocalDate.now();
    ZoneId zone = ZoneId.systemDefault();
    for (int i = 0; i < source.length(); i++) {
      JSONObject r = source.optJSONObject(i);
      if (r.optBoolean("deleted")) continue;
      LocalDate date = Instant.parse(r.optString("playedAt")).atZone(zone).toLocalDate();
      if (days > 0 && date.isBefore(today.minusDays(days - 1))) continue;
      games.add(r);
    }
    games.sort(
        (a, b) ->
            Instant.parse(a.optString("playedAt"))
                .compareTo(Instant.parse(b.optString("playedAt"))));
    int run = 0;
    String previous = "";
    JSONObject last = null;
    for (JSONObject r : games) {
      int m = RoundStore.index(r.optString("mine")), h = RoundStore.index(r.optString("hers"));
      String outcome = RoundStore.outcome(r.optString("mine"), r.optString("hers"));
      mine[m]++;
      hers[h]++;
      matchups[m][h]++;
      switch (outcome) {
        case "win":
          wins++;
          throwWins[m]++;
          run++;
          bestStreak = Math.max(bestStreak, run);
          break;
        case "loss":
          losses++;
          throwLosses[m]++;
          run = 0;
          break;
        default:
          ties++;
          run = 0;
      }
      currentStreak = outcome.equals(previous) ? currentStreak + 1 : 1;
      previous = outcome;
      streakOutcome = outcome;
      long day =
          java.time.temporal.ChronoUnit.DAYS.between(
              Instant.parse(r.optString("playedAt")).atZone(zone).toLocalDate(), today);
      if (day >= 0 && day < 7) {
        int j = 6 - (int) day;
        if (outcome.equals("win")) dailyWins[j]++;
        else if (outcome.equals("loss")) dailyLosses[j]++;
        else dailyTies[j]++;
      }
      if (last != null) {
        String lastResult = RoundStore.outcome(last.optString("mine"), last.optString("hers"));
        boolean repeated = last.optString("hers").equals(r.optString("hers"));
        if (lastResult.equals("win")) {
          afterHerLoss++;
          if (repeated) repeatAfterHerLoss++;
        } else if (lastResult.equals("loss")) {
          afterHerWin++;
          if (repeated) repeatAfterHerWin++;
        }
      }
      last = r;
    }
    JSONArray ordered = new JSONArray();
    for (JSONObject r : games) ordered.put(r);
    recommendation = RoundStore.advice(ordered);
  }

  public int total() {
    return games.size();
  }

  public int winRate() {
    return wins + losses == 0 ? 0 : Math.round(wins * 100f / (wins + losses));
  }

  public String rateLabel() {
    return wins + losses == 0 ? "—" : winRate() + "%";
  }

  public static String percent(int value, int total) {
    return total == 0 ? "—" : Math.round(value * 100f / total) + "%";
  }

  public int todayCount() {
    int n = 0;
    for (JSONObject r : games)
      if (Instant.parse(r.optString("playedAt"))
          .atZone(ZoneId.systemDefault())
          .toLocalDate()
          .equals(LocalDate.now())) n++;
    return n;
  }

  public String streakLabel() {
    return currentStreak == 0
        ? "No games yet"
        : currentStreak
            + " "
            + (streakOutcome.equals("win") ? "win" : streakOutcome.equals("loss") ? "loss" : "tie")
            + (currentStreak == 1 ? "" : "s")
            + " in a row";
  }

  public String strongestThrow() {
    int best = -1;
    float score = -1;
    for (int i = 0; i < 3; i++) {
      int decisive = throwWins[i] + throwLosses[i];
      if (decisive >= 5 && throwWins[i] / (float) decisive > score) {
        best = i;
        score = throwWins[i] / (float) decisive;
      }
    }
    return best < 0
        ? "Keep playing to compare your throws"
        : RoundStore.label(RoundStore.THROWS[best])
            + " · "
            + percent(throwWins[best], throwWins[best] + throwLosses[best])
            + " win rate across "
            + mine[best]
            + " games";
  }
}

package com.jacobsides.hands;

import static org.junit.Assert.*;

import java.time.Instant;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class AnalyticsTest {
  private JSONObject game(String mine, String hers) throws Exception {
    return new JSONObject()
        .put("mine", mine)
        .put("hers", hers)
        .put("playedAt", Instant.now().toString());
  }

  @Test
  public void tiesDoNotInflateWinRate() throws Exception {
    JSONArray games =
        new JSONArray()
            .put(game("ROCK", "SCISSORS"))
            .put(game("ROCK", "PAPER"))
            .put(game("ROCK", "ROCK"));
    Analytics a = new Analytics(games, 0);
    assertEquals(50, a.winRate());
    assertEquals(1, a.wins);
    assertEquals(1, a.losses);
    assertEquals(1, a.ties);
  }

  @Test
  public void trashAndOlderGamesAreExcluded() throws Exception {
    JSONArray games =
        new JSONArray()
            .put(game("PAPER", "ROCK").put("deleted", true))
            .put(game("PAPER", "ROCK").put("playedAt", "2020-01-01T00:00:00Z"));
    assertEquals(0, new Analytics(games, 7).total());
    assertEquals(1, new Analytics(games, 0).total());
  }

  @Test
  public void transitionsStreaksAndMatrixMatchTheGames() throws Exception {
    JSONArray games = new JSONArray();
    for (int i = 0; i < 6; i++) games.put(game("PAPER", "ROCK"));
    Analytics a = new Analytics(games, 0);
    assertEquals(6, a.bestStreak);
    assertEquals(6, a.currentStreak);
    assertEquals(5, a.afterHerLoss);
    assertEquals(5, a.repeatAfterHerLoss);
    assertEquals(6, a.matchups[1][0]);
    assertEquals(100, a.winRate());
    assertTrue(a.strongestThrow().startsWith("Paper"));
  }

  @Test
  public void emptyHistoryMakesNoPercentageClaim() {
    Analytics a = new Analytics(new JSONArray(), 0);
    assertEquals("—", a.rateLabel());
    assertEquals("—", Analytics.percent(0, 0));
    assertEquals(0, a.todayCount());
  }
}

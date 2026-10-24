package com.jacobsides.hands;

import static org.junit.Assert.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class AdviceTest {
  @Test
  public void allNineOutcomes() {
    String[][] expected = {{"tie", "loss", "win"}, {"win", "tie", "loss"}, {"loss", "win", "tie"}};
    for (int i = 0; i < 3; i++)
      for (int j = 0; j < 3; j++)
        assertEquals(
            expected[i][j], RoundStore.outcome(RoundStore.THROWS[i], RoundStore.THROWS[j]));
  }

  @Test
  public void adviceWaitsForEvidenceAndCountersRock() throws Exception {
    JSONArray rounds = new JSONArray();
    assertTrue(RoundStore.advice(rounds).startsWith("Mix"));
    for (int i = 0; i < 7; i++)
      rounds.put(new JSONObject().put("mine", "PAPER").put("hers", "ROCK"));
    assertTrue(RoundStore.advice(rounds).startsWith("Mix"));
    rounds.put(new JSONObject().put("mine", "PAPER").put("hers", "ROCK"));
    assertTrue(RoundStore.advice(rounds).startsWith("Try Paper"));
  }

  @Test
  public void balancedThrowsHaveNoEdgeAndDeletedRoundsDoNotCount() throws Exception {
    JSONArray rounds = new JSONArray();
    for (int i = 0; i < 9; i++)
      rounds.put(new JSONObject().put("mine", "PAPER").put("hers", RoundStore.THROWS[i % 3]));
    assertTrue(RoundStore.advice(rounds).startsWith("No clear edge"));
    rounds.getJSONObject(0).put("deleted", true);
    rounds.getJSONObject(1).put("deleted", true);
    assertTrue(RoundStore.advice(rounds).startsWith("Mix"));
  }
}

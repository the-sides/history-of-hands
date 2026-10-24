package com.jacobsides.hands;

import static org.junit.Assert.*;

import org.junit.Test;

public class QuickResultTest {
  @Test
  public void allNineGesturesProduceTheSelectedOutcome() {
    for (String mine : RoundStore.THROWS) {
      for (String result : new String[] {"win", "loss", "tie"}) {
        assertEquals(result, RoundStore.outcome(mine, QuickResult.opponent(mine, result)));
      }
    }
  }

  @Test
  public void rejectsInvalidInput() {
    assertThrows(IllegalArgumentException.class, () -> QuickResult.opponent("ROCK", "oops"));
    assertThrows(IllegalArgumentException.class, () -> QuickResult.opponent("oops", "win"));
  }
}

package com.jacobsides.hands;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/** Emulator-only executable integration checks; excluded from release APKs. */
public final class StoreChecks extends Activity {
  private void check(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }

  private JSONObject game(String id, long revision, String mine) throws Exception {
    return new JSONObject()
        .put("id", id)
        .put("revision", revision)
        .put("mine", mine)
        .put("hers", "ROCK")
        .put("playedAt", "2026-10-01T12:00:00.000Z")
        .put("deleted", false);
  }

  public void onCreate(Bundle state) {
    super.onCreate(state);
    TextView text = new TextView(this);
    setContentView(text);
    if (!android.os.Build.FINGERPRINT.contains("generic")
        && !android.os.Build.MODEL.toLowerCase(java.util.Locale.ROOT).contains("sdk")) {
      text.setText("EMULATOR ONLY");
      return;
    }
    try {
      RoundStore store = RoundStore.get(this);
      var db = store.getWritableDatabase();
      db.delete("rounds", null, null);
      db.delete("conflicts", null, null);
      store.merge(new JSONArray().put(game("conflict", 10, "PAPER")));
      store.edit("conflict", "SCISSORS", "ROCK", "2026-10-01T12:00:00.000Z");
      check(store.pending().getJSONObject(0).getLong("baseRevision") == 10, "base retained");
      store.merge(new JSONArray().put(game("conflict", 20, "ROCK")));
      check(store.conflicts().length() == 1, "concurrent edit surfaced");
      check(store.pending().length() == 0, "conflict upload blocked");
      check(
          store.all(false).getJSONObject(0).getString("mine").equals("SCISSORS"),
          "local preserved");
      store.log("PAPER", "ROCK");
      check(store.pending().length() == 1, "unrelated game still pending");
      store.resolve(store.conflicts().getJSONObject(0), true);
      check(store.conflicts().length() == 0, "resolved");
      JSONObject sent = null;
      for (int i = 0; i < store.pending().length(); i++)
        if (store.pending().getJSONObject(i).getString("id").equals("conflict"))
          sent = store.pending().getJSONObject(i);
      check(sent.getLong("baseRevision") == 20, "resolution rebased");
      store.edit("conflict", "PAPER", "ROCK", "2026-10-01T12:00:00.000Z");
      store.acknowledge(new JSONArray().put(sent));
      check(store.pendingCount() == 2, "ack retains edit made during upload");
      store.merge(new JSONArray().put(game("conflict", sent.getLong("revision"), "SCISSORS")));
      check(store.conflicts().length() == 0, "own acknowledged upload is not a collision");
      JSONObject remote =
          game("conflict", sent.getLong("revision") + 100, "ROCK").put("deleted", true);
      store.merge(new JSONArray().put(remote));
      check(store.conflicts().length() == 1, "trash versus edit collision");
      store.resolve(remote, false);
      check(store.all(false).length() == 1, "account deletion selected");
      check(store.conflicts().length() == 0, "account resolution clears conflict");
      store.merge(new JSONArray().put(game("review-fixture", 100, "ROCK")));
      store.edit("review-fixture", "SCISSORS", "PAPER", "2026-10-01T12:00:00.000Z");
      store.merge(new JSONArray().put(game("review-fixture", 200, "PAPER").put("deleted", true)));
      text.setText(
          "PASS: base versions, concurrent edits, blocked conflicts, unrelated saves, both"
              + " resolutions, in-flight edits, delete/edit collisions");
    } catch (Throwable e) {
      text.setText("FAIL: " + e);
    }
  }
}

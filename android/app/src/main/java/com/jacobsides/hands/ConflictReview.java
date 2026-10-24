package com.jacobsides.hands;

import android.app.AlertDialog;
import org.json.JSONObject;

final class ConflictReview {
  static void show(MainActivity a) {
    var conflicts = RoundStore.get(a).conflicts();
    if (conflicts.length() == 0) return;
    JSONObject remote = conflicts.optJSONObject(0), local = null;
    var rows = RoundStore.get(a).all(true);
    for (int i = 0; i < rows.length(); i++)
      if (rows.optJSONObject(i).optString("id").equals(remote.optString("id")))
        local = rows.optJSONObject(i);
    if (local == null) return;
    a.editing = true;
    var dialog =
        new AlertDialog.Builder(a)
            .setTitle("Which version should we keep?")
            .setMessage(
                "THIS PHONE\n"
                    + describe(local)
                    + "\n\nYOUR ACCOUNT\n"
                    + describe(remote)
                    + "\n\n"
                    + "Other games keep saving automatically. You can decide later; neither version"
                    + " will be overwritten.")
            .setPositiveButton("Keep phone", (d, w) -> resolve(a, remote, true))
            .setNegativeButton("Keep account", (d, w) -> resolve(a, remote, false))
            .setNeutralButton("Decide later", null)
            .create();
    dialog.setOnDismissListener(d -> a.editing = false);
    dialog.show();
  }

  private static String describe(JSONObject game) {
    return RoundStore.label(game.optString("mine"))
        + " / "
        + RoundStore.label(game.optString("hers"))
        + "\n"
        + MainActivity.when(game.optString("playedAt"), "MMM d, yyyy h:mm:ss a")
        + (game.optBoolean("deleted") ? "\nIn trash" : "\nActive game");
  }

  private static void resolve(MainActivity a, JSONObject remote, boolean local) {
    RoundStore.get(a).resolve(remote, local);
    a.changed("Conflict resolved.", null);
    a.toast("Choice saved. Other changes sync automatically.");
  }
}

package com.jacobsides.hands;

import static com.jacobsides.hands.Design.*;

import android.app.AlertDialog;
import android.widget.LinearLayout;

final class AccountScreen {
  static void show(MainActivity a, LinearLayout root) {
    var prefs = a.getSharedPreferences("hands", 0);
    boolean connected =
        prefs.getBoolean("github", false) && !prefs.getString("token", "").isEmpty();
    root.addView(text(a, "Your games.\nYour account.", 30, TEXT, true));
    gap(root, 22);
    LinearLayout account = card(a);
    account.addView(label(a, connected ? "GITHUB ACCOUNT" : "ONE SIGN-IN. EVERYTHING SAVED."));
    gap(account, 13);
    account.addView(
        text(
            a,
            connected ? prefs.getString("account_name", "You") : "Keep your rivalry safe.",
            24,
            TEXT,
            true));
    gap(account, 10);
    account.addView(
        text(
            a,
            connected
                ? a.cloudStatus()
                : "Sign in with GitHub to save your history and keep every device up to date. Your"
                    + " existing games stay with you.",
            13,
            MUTED,
            false));
    if (!connected || prefs.getString("sync_state", "").equals("signin"))
      account.addView(button(a, "Sign in with GitHub  ↗", true, a::signIn));
    else {
      gap(account, 14);
      account.addView(text(a, "●  Automatic saving is on", 12, LIME, true));
      long last = prefs.getLong("last_sync", 0);
      if (last > 0) {
        gap(account, 5);
        account.addView(
            text(
                a,
                "Last saved "
                    + MainActivity.when(
                        java.time.Instant.ofEpochMilli(last).toString(), "MMM d, h:mm a"),
                10,
                MUTED,
                false));
      }
    }
    root.addView(account);
    LinearLayout shortcuts = card(a);
    shortcuts.addView(text(a, "Keep it within reach", 20, TEXT, true));
    gap(shortcuts, 8);
    shortcuts.addView(
        text(
            a,
            "Your bottom picker and launcher shortcuts are still here. The home-screen widget saves"
                + " a game in one tap.",
            12,
            MUTED,
            false));
    shortcuts.addView(button(a, "Add the one-tap widget  +", false, a::addWidget));
    root.addView(shortcuts);
    LinearLayout data = card(a);
    data.addView(text(a, "You’re in control.", 22, TEXT, true));
    var all = RoundStore.get(a).all(true);
    int deleted = all.length() - RoundStore.get(a).all(false).length();
    gap(data, 8);
    data.addView(
        text(
            a, (all.length() - deleted) + " games  ·  " + deleted + " in trash", 12, MUTED, false));
    data.addView(
        button(
            a,
            "Manage games  →",
            false,
            () -> {
              a.historyFilter = 0;
              a.historySearch = "";
              a.historyDay = "";
              a.navigate(2);
            }));
    data.addView(
        button(
            a,
            "Recently deleted  ·  " + deleted,
            false,
            () -> {
              a.historyFilter = 4;
              a.historySearch = "";
              a.historyDay = "";
              a.navigate(2);
            }));
    data.addView(button(a, "Export all data  ↓", false, a::export));
    data.addView(button(a, "Import a history file  ↑", false, a::importFile));
    gap(data, 8);
    data.addView(
        text(
            a,
            "Exports include every game, original timestamps, corrections, and trash. Imports keep"
                + " newer changes and never duplicate a game.",
            10,
            MUTED,
            false));
    root.addView(data);
    LinearLayout remove = card(a);
    remove.addView(text(a, "Need a fresh start?", 20, TEXT, true));
    gap(remove, 8);
    remove.addView(
        text(
            a,
            "Move all active games to trash. They leave your score and analytics on every connected"
                + " device, and can be restored later.",
            12,
            MUTED,
            false));
    var delete =
        button(
            a,
            "Move all games to trash",
            false,
            () ->
                new AlertDialog.Builder(a)
                    .setTitle("Move all your games to trash?")
                    .setMessage(
                        "This updates your account too. You can restore games from Recently"
                            + " deleted.")
                    .setNegativeButton("Keep my games", null)
                    .setPositiveButton(
                        "Move to trash",
                        (d, w) -> {
                          var snapshot = RoundStore.get(a).all(false);
                          RoundStore.get(a).deleteAll();
                          a.changed(
                              "All games moved to trash.",
                              () -> {
                                for (int i = 0; i < snapshot.length(); i++)
                                  RoundStore.get(a)
                                      .setDeleted(snapshot.optJSONObject(i).optString("id"), false);
                              });
                        })
                    .show());
    delete.setTextColor(CORAL);
    remove.addView(delete);
    root.addView(remove);
    if (!prefs.getString("token", "").isEmpty())
      root.addView(
          button(
              a,
              "Sign out",
              false,
              () ->
                  new AlertDialog.Builder(a)
                      .setTitle("Sign out on this phone?")
                      .setMessage(
                          "Your games stay on this phone. Automatic backup pauses until you sign in"
                              + " again.")
                      .setNegativeButton("Stay signed in", null)
                      .setPositiveButton(
                          "Sign out",
                          (d, w) -> {
                            SyncJob.disconnect(a);
                            prefs
                                .edit()
                                .remove("token")
                                .remove("account_id")
                                .remove("account_name")
                                .remove("github")
                                .putString("sync_state", "local")
                                .apply();
                            a.render(false);
                          })
                      .show()));
    gap(root, 20);
    root.addView(text(a, "HANDS 2.0  /  BUILT FOR YOU TWO", 9, MUTED, true));
  }
}

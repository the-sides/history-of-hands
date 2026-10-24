package com.jacobsides.hands;

import static com.jacobsides.hands.Design.*;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;

final class HistoryScreen {
  static void show(MainActivity a, LinearLayout root) {
    root.addView(text(a, "Every game.\nEvery little victory.", 29, TEXT, true));
    gap(root, 10);
    root.addView(
        text(
            a,
            "Tap a game to edit its throws or date. Nothing accidental has to stay.",
            12,
            MUTED,
            false));
    gap(root, 22);
    EditText search = new EditText(a);
    search.setSingleLine(true);
    search.setTextColor(TEXT);
    search.setHintTextColor(MUTED);
    search.setTextSize(13);
    search.setHint("Search dates, throws, or results");
    search.setPadding(dp(a, 16), 0, dp(a, 16), 0);
    search.setBackground(background(SURFACE, 16, a));
    search.setText(a.historySearch);
    root.addView(search, new LinearLayout.LayoutParams(-1, dp(a, 50)));
    gap(root, 14);
    android.widget.HorizontalScrollView filterScroll = new android.widget.HorizontalScrollView(a);
    filterScroll.setHorizontalScrollBarEnabled(false);
    LinearLayout filters = row(a);
    String[] names = {"All", "Wins", "Losses", "Ties", "Trash"};
    for (int i = 0; i < 5; i++) {
      final int filter = i;
      filters.addView(
          chip(
              a,
              names[i],
              a.historyFilter == i,
              () -> {
                a.historyFilter = filter;
                a.historyLimit = 100;
                a.selected.clear();
                a.selecting = false;
                a.render(false);
              }));
    }
    filterScroll.addView(filters);
    root.addView(filterScroll);
    gap(root, 14);
    LinearLayout tools = row(a);
    tools.addView(
        chip(
            a,
            a.historyDay.isEmpty() ? "Any date ↓" : a.historyDay,
            false,
            () -> {
              LocalDate day =
                  a.historyDay.isEmpty() ? LocalDate.now() : LocalDate.parse(a.historyDay);
              new DatePickerDialog(
                      a,
                      (v, y, m, d) -> {
                        a.historyDay = LocalDate.of(y, m + 1, d).toString();
                        a.render(false);
                      },
                      day.getYear(),
                      day.getMonthValue() - 1,
                      day.getDayOfMonth())
                  .show();
            }));
    if (!a.historyDay.isEmpty())
      tools.addView(
          chip(
              a,
              "Clear date",
              false,
              () -> {
                a.historyDay = "";
                a.render(false);
              }));
    if (a.historyFilter != 4)
      tools.addView(
          chip(
              a,
              a.selecting ? "Done" : "Select",
              a.selecting,
              () -> {
                a.selecting = !a.selecting;
                a.selected.clear();
                a.render(true);
              }));
    root.addView(tools);
    gap(root, 16);
    if (a.selecting && !a.selected.isEmpty()) {
      root.addView(
          button(
              a,
              "Move " + a.selected.size() + " selected games to trash",
              false,
              () ->
                  new AlertDialog.Builder(a)
                      .setTitle("Move " + a.selected.size() + " games to trash?")
                      .setMessage("They can be restored later.")
                      .setNegativeButton("Cancel", null)
                      .setPositiveButton(
                          "Move to trash",
                          (d, w) -> {
                            List<String> ids = new ArrayList<>(a.selected);
                            for (String id : ids) RoundStore.get(a).setDeleted(id, true);
                            a.selected.clear();
                            a.selecting = false;
                            a.changed(
                                "Selected games moved to trash.",
                                () -> {
                                  for (String id : ids) RoundStore.get(a).setDeleted(id, false);
                                });
                          })
                      .show()));
      gap(root, 16);
    }
    LinearLayout list = column(a);
    root.addView(list);
    Runnable fill = () -> fill(a, list);
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {
            a.historySearch = s.toString();
            a.historyLimit = 100;
            fill.run();
          }

          public void afterTextChanged(Editable s) {}
        });
    fill.run();
  }

  private static void fill(MainActivity a, LinearLayout list) {
    list.removeAllViews();
    var all = RoundStore.get(a).all(true);
    List<JSONObject> rows = new ArrayList<>();
    String query = a.historySearch.trim().toLowerCase(Locale.ROOT);
    for (int i = all.length() - 1; i >= 0; i--) {
      var r = all.optJSONObject(i);
      boolean deleted = r.optBoolean("deleted");
      if (deleted != (a.historyFilter == 4)) continue;
      String outcome = RoundStore.outcome(r.optString("mine"), r.optString("hers"));
      if (a.historyFilter > 0
          && a.historyFilter < 4
          && !outcome.equals(a.historyFilter == 1 ? "win" : a.historyFilter == 2 ? "loss" : "tie"))
        continue;
      String day = MainActivity.when(r.optString("playedAt"), "yyyy-MM-dd");
      if (!a.historyDay.isEmpty() && !a.historyDay.equals(day)) continue;
      String searchable =
          RoundStore.label(r.optString("mine"))
              + " "
              + RoundStore.label(r.optString("hers"))
              + " "
              + outcome
              + " "
              + day
              + " "
              + MainActivity.when(r.optString("playedAt"), "EEEE MMMM d yyyy h:mm a");
      if (!query.isEmpty() && !searchable.toLowerCase(Locale.ROOT).contains(query)) continue;
      rows.add(r);
    }
    list.addView(label(a, rows.size() + " " + (a.historyFilter == 4 ? "DELETED GAMES" : "GAMES")));
    gap(list, 16);
    String day = "";
    int shown = 0;
    for (JSONObject r : rows) {
      if (shown++ >= a.historyLimit) break;
      String date = MainActivity.when(r.optString("playedAt"), "yyyy-MM-dd");
      if (!date.equals(day)) {
        day = date;
        gap(list, 14);
        list.addView(
            text(
                a,
                MainActivity.when(r.optString("playedAt"), "EEEE, MMM d, yyyy"),
                12,
                MUTED,
                true));
        gap(list, 12);
      }
      String outcome = RoundStore.outcome(r.optString("mine"), r.optString("hers"));
      LinearLayout card = card(a);
      card.setPadding(dp(a, 16), dp(a, 16), dp(a, 16), dp(a, 16));
      LinearLayout row = row(a);
      if (a.selecting) {
        CheckBox selected = new CheckBox(a);
        selected.setButtonTintList(android.content.res.ColorStateList.valueOf(LIME));
        selected.setChecked(a.selected.contains(r.optString("id")));
        selected.setContentDescription(
            "Select game from " + MainActivity.when(r.optString("playedAt"), "MMM d h:mm a"));
        selected.setOnCheckedChangeListener(
            (b, checked) -> {
              if (checked) a.selected.add(r.optString("id"));
              else a.selected.remove(r.optString("id"));
              a.render(true);
            });
        row.addView(selected);
      }
      TextView badge =
          text(
              a,
              outcome.equals("win") ? "W" : outcome.equals("loss") ? "L" : "=",
              16,
              outcome.equals("win") ? LIME : outcome.equals("loss") ? CORAL : MUTED,
              true);
      badge.setGravity(Gravity.CENTER);
      badge.setBackground(background(RAISED, 13, a));
      LinearLayout.LayoutParams badgeP = new LinearLayout.LayoutParams(dp(a, 40), dp(a, 44));
      badgeP.rightMargin = dp(a, 12);
      row.addView(badge, badgeP);
      LinearLayout detail = column(a);
      detail.addView(
          text(
              a,
              RoundStore.label(r.optString("mine"))
                  + "  /  "
                  + RoundStore.label(r.optString("hers")),
              15,
              TEXT,
              true));
      gap(detail, 5);
      detail.addView(
          text(
              a,
              MainActivity.when(r.optString("playedAt"), "h:mm a")
                  + "  ·  "
                  + (outcome.equals("win")
                      ? "You won"
                      : outcome.equals("loss") ? "She won" : "A tie"),
              11,
              MUTED,
              false));
      row.addView(detail, new LinearLayout.LayoutParams(0, -2, 1));
      row.addView(text(a, a.historyFilter == 4 ? "↶" : "↗", 20, MUTED, false));
      card.addView(row);
      card.setContentDescription(
          (a.historyFilter == 4 ? "Restore game: " : "Edit game: ")
              + RoundStore.label(r.optString("mine"))
              + " versus "
              + RoundStore.label(r.optString("hers"))
              + ", "
              + MainActivity.when(r.optString("playedAt"), "MMM d yyyy h:mm a"));
      card.setOnClickListener(
          v -> {
            if (a.historyFilter == 4) {
              RoundStore.get(a).setDeleted(r.optString("id"), false);
              a.changed(
                  "Game restored.", () -> RoundStore.get(a).setDeleted(r.optString("id"), true));
            } else if (a.selecting) {
              if (!a.selected.add(r.optString("id"))) a.selected.remove(r.optString("id"));
              a.render(true);
            } else GameEditor.show(a, r);
          });
      list.addView(card);
    }
    if (rows.isEmpty()) {
      LinearLayout empty = card(a);
      empty.addView(
          text(a, a.historyFilter == 4 ? "A clean slate." : "No games here yet.", 23, TEXT, true));
      gap(empty, 10);
      empty.addView(
          text(
              a,
              a.historyFilter == 4
                  ? "Deleted games appear here. Tap one to restore it."
                  : "Try another filter, or log your next game.",
              12,
              MUTED,
              false));
      list.addView(empty);
    }
    if (rows.size() > a.historyLimit)
      list.addView(
          button(
              a,
              "Show 100 more games",
              false,
              () -> {
                a.historyLimit += 100;
                a.render(true);
              }));
  }
}

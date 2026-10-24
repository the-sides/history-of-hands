package com.jacobsides.hands;

import static com.jacobsides.hands.Design.*;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

final class BulkGameEditor {
  static void show(MainActivity a, List<String> ids) {
    JSONArray before = new JSONArray();
    var all = RoundStore.get(a).all(false);
    for (int i = 0; i < all.length(); i++) {
      JSONObject game = all.optJSONObject(i);
      if (ids.contains(game.optString("id"))) before.put(game);
    }
    if (before.length() == 0) {
      a.toast("Select games to edit first.");
      return;
    }
    a.editing = true;
    LinearLayout box = column(a);
    box.setPadding(dp(a, 24), dp(a, 16), dp(a, 24), 0);
    box.addView(
        text(
            a,
            "Only the fields you choose will change. Each game's other details stay as they are.",
            13,
            MUTED,
            false));
    gap(box, 18);
    box.addView(label(a, "YOUR THROW"));
    Spinner mine = choices(a, box);
    gap(box, 16);
    box.addView(label(a, "HER THROW"));
    Spinner hers = choices(a, box);
    gap(box, 16);
    LocalDate[] day = {null};
    var date = button(a, "Keep each game's date", false, () -> {});
    date.setOnClickListener(
        v -> {
          LocalDate initial = day[0] == null ? LocalDate.now() : day[0];
          DatePickerDialog picker =
              new DatePickerDialog(
                  a,
                  (view, y, m, d) -> {
                    day[0] = LocalDate.of(y, m + 1, d);
                    date.setText("Set date: " + day[0] + " · keep times");
                  },
                  initial.getYear(),
                  initial.getMonthValue() - 1,
                  initial.getDayOfMonth());
          picker.getDatePicker().setMaxDate(System.currentTimeMillis());
          picker.show();
        });
    box.addView(date);
    box.addView(
        button(
            a,
            "Reset date change",
            false,
            () -> {
              day[0] = null;
              date.setText("Keep each game's date");
            }));
    AlertDialog dialog =
        new AlertDialog.Builder(a)
            .setTitle("Edit " + before.length() + " games")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Apply changes", null)
            .create();
    dialog.setOnDismissListener(d -> a.editing = false);
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      if (mine.getSelectedItemPosition() == 0
                          && hers.getSelectedItemPosition() == 0
                          && day[0] == null) {
                        a.toast("Choose a field to change.");
                        return;
                      }
                      try {
                        JSONArray after = new JSONArray();
                        for (int i = 0; i < before.length(); i++) {
                          JSONObject game = new JSONObject(before.optJSONObject(i).toString());
                          if (mine.getSelectedItemPosition() > 0)
                            game.put("mine", RoundStore.THROWS[mine.getSelectedItemPosition() - 1]);
                          if (hers.getSelectedItemPosition() > 0)
                            game.put("hers", RoundStore.THROWS[hers.getSelectedItemPosition() - 1]);
                          if (day[0] != null) {
                            var time =
                                Instant.parse(game.optString("playedAt"))
                                    .atZone(ZoneId.systemDefault());
                            game.put(
                                "playedAt",
                                day[0]
                                    .atTime(time.toLocalTime())
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toString());
                          }
                          after.put(game);
                        }
                        RoundStore.get(a).editMany(after);
                        a.selected.clear();
                        a.selecting = false;
                        dialog.dismiss();
                        a.changed(
                            before.length() + " games updated.",
                            () -> RoundStore.get(a).editMany(before));
                        a.toast(before.length() + " games updated.");
                      } catch (Exception ex) {
                        a.toast("Couldn't save. No bulk changes were applied.");
                      }
                    }));
    dialog.show();
  }

  private static Spinner choices(MainActivity a, LinearLayout box) {
    Spinner spinner = new Spinner(a);
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(
            a,
            android.R.layout.simple_spinner_item,
            new String[] {"Keep each game's throw", "Rock", "Paper", "Scissors"});
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
    box.addView(spinner, new LinearLayout.LayoutParams(-1, dp(a, 48)));
    return spinner;
  }
}

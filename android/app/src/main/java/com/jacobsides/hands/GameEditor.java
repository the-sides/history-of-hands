package com.jacobsides.hands;

import static com.jacobsides.hands.Design.*;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.json.JSONObject;

final class GameEditor {
  static void show(MainActivity a, JSONObject game) {
    a.editing = true;
    Dialog dialog = new Dialog(a);
    LinearLayout box = column(a);
    box.setPadding(dp(a, 22), dp(a, 24), dp(a, 22), dp(a, 24));
    box.setBackground(background(SURFACE, 28, a));
    String[] mine = {game.optString("mine")}, hers = {game.optString("hers")};
    ZonedDateTime[] when = {
      Instant.parse(game.optString("playedAt")).atZone(ZoneId.systemDefault())
    };
    box.addView(label(a, "YOUR SCORECARD / EDIT GAME"));
    gap(box, 12);
    box.addView(text(a, "Get the details right.", 26, TEXT, true));
    gap(box, 22);
    box.addView(label(a, "YOUR THROW"));
    LinearLayout your = row(a);
    box.addView(your);
    gap(box, 20);
    box.addView(label(a, "HER THROW"));
    LinearLayout her = row(a);
    box.addView(her);
    Runnable[] refresh = new Runnable[1];
    refresh[0] =
        () -> {
          your.removeAllViews();
          her.removeAllViews();
          for (int i = 0; i < 3; i++) {
            final String value = RoundStore.THROWS[i];
            String label = RoundStore.SYMBOLS[i] + " " + RoundStore.label(value);
            your.addView(
                chip(
                    a,
                    label,
                    mine[0].equals(value),
                    () -> {
                      mine[0] = value;
                      refresh[0].run();
                    }));
            her.addView(
                chip(
                    a,
                    label,
                    hers[0].equals(value),
                    () -> {
                      hers[0] = value;
                      refresh[0].run();
                    }));
          }
          for (int i = 0; i < 3; i++) {
            your.getChildAt(i)
                .setContentDescription("Your throw: " + RoundStore.label(RoundStore.THROWS[i]));
            her.getChildAt(i)
                .setContentDescription("Her throw: " + RoundStore.label(RoundStore.THROWS[i]));
          }
        };
    refresh[0].run();
    gap(box, 24);
    box.addView(label(a, "PLAYED ON"));
    Button date =
        button(
            a,
            MainActivity.when(game.optString("playedAt"), "EEE, MMM d, yyyy  ·  h:mm a"),
            false,
            () -> {});
    box.addView(date);
    date.setContentDescription("Change game date and time");
    date.setOnClickListener(
        v -> {
          ZonedDateTime now = when[0];
          DatePickerDialog picker =
              new DatePickerDialog(
                  a,
                  (view, year, month, day) -> {
                    when[0] =
                        when[0]
                            .withDayOfMonth(1)
                            .withYear(year)
                            .withMonth(month + 1)
                            .withDayOfMonth(day);
                    new TimePickerDialog(
                            a,
                            (time, hour, minute) -> {
                              when[0] =
                                  when[0]
                                      .withHour(hour)
                                      .withMinute(minute)
                                      .withSecond(0)
                                      .withNano(0);
                              date.setText(
                                  MainActivity.when(
                                      when[0].toInstant().toString(),
                                      "EEE, MMM d, yyyy  ·  h:mm a"));
                            },
                            now.getHour(),
                            now.getMinute(),
                            android.text.format.DateFormat.is24HourFormat(a))
                        .show();
                  },
                  now.getYear(),
                  now.getMonthValue() - 1,
                  now.getDayOfMonth());
          picker.getDatePicker().setMaxDate(System.currentTimeMillis());
          picker.show();
        });
    gap(box, 16);
    box.addView(
        button(
            a,
            "Save changes",
            true,
            () -> {
              try {
                String id = game.optString("id");
                RoundStore.get(a).edit(id, mine[0], hers[0], when[0].toInstant().toString());
                dialog.dismiss();
                a.changed(
                    "Game updated.",
                    () ->
                        RoundStore.get(a)
                            .edit(
                                id,
                                game.optString("mine"),
                                game.optString("hers"),
                                game.optString("playedAt")));
              } catch (Exception e) {
                a.toast("Couldn't save those changes. Your game is unchanged.");
              }
            }));
    Button delete =
        button(
            a,
            "Move to trash",
            false,
            () ->
                new AlertDialog.Builder(a)
                    .setTitle("Move this game to trash?")
                    .setMessage(
                        "It will leave your score and analytics. You can restore it from History →"
                            + " Trash.")
                    .setNegativeButton("Keep game", null)
                    .setPositiveButton(
                        "Move to trash",
                        (d, w) -> {
                          String id = game.optString("id");
                          RoundStore.get(a).setDeleted(id, true);
                          dialog.dismiss();
                          a.changed(
                              "Game moved to trash.",
                              () -> RoundStore.get(a).setDeleted(id, false));
                        })
                    .show());
    delete.setTextColor(CORAL);
    box.addView(delete);
    box.addView(button(a, "Cancel", false, dialog::dismiss));
    dialog.setContentView(box);
    dialog.setOnDismissListener(d -> a.editing = false);
    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    dialog.getWindow().setLayout(-1, WindowManager.LayoutParams.WRAP_CONTENT);
    dialog.getWindow().setGravity(Gravity.BOTTOM);
    dialog.show();
    dialog.getWindow().setLayout(-1, -2);
  }
}

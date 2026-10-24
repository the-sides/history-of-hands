package com.jacobsides.hands;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import org.json.JSONArray;

public final class MainActivity extends Activity {
  private static final int EXPORT = 20, IMPORT = 21;
  private LinearLayout root;

  public void onCreate(Bundle state) {
    super.onCreate(state);
    render();
  }

  protected void onResume() {
    super.onResume();
    HandsWidget.update(this);
    SyncJob.schedule(this);
    new Thread(
            () -> {
              SyncJob.sync(this);
              runOnUiThread(
                  () -> {
                    if (!isFinishing()) render();
                  });
            },
            "hands-refresh")
        .start();
  }

  private int dp(int size) {
    return Math.round(size * getResources().getDisplayMetrics().density);
  }

  private TextView text(String value, int size, boolean serif) {
    TextView v = new TextView(this);
    v.setText(value);
    v.setTextSize(size);
    v.setTextColor(Color.parseColor("#25392f"));
    if (serif) v.setTypeface(Typeface.create("serif", Typeface.NORMAL));
    v.setPadding(0, dp(6), 0, dp(8));
    return v;
  }

  private Button button(String label, Runnable action) {
    Button b = new Button(this);
    b.setText(label);
    b.setAllCaps(false);
    b.setOnClickListener(v -> action.run());
    root.addView(b, new LinearLayout.LayoutParams(-1, dp(52)));
    return b;
  }

  private void render() {
    ScrollView scroll = new ScrollView(this);
    scroll.setBackgroundColor(Color.parseColor("#f4f0e7"));
    scroll.setFillViewport(true);
    root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(24), dp(28), dp(24), dp(32));
    root.setOnApplyWindowInsetsListener(
        (view, insets) -> {
          var bars = insets.getInsets(android.view.WindowInsets.Type.systemBars());
          view.setPadding(dp(24), dp(24) + bars.top, dp(24), dp(24) + bars.bottom);
          return insets;
        });
    scroll.addView(root);
    setContentView(scroll);
    root.addView(text("HISTORY OF HANDS · YOU & HER", 11, false));
    root.addView(text("Love her.\nBeat her at scissors.", 34, true));
    String[] summary = RoundStore.get(this).summary();
    TextView advice = text(summary[1], 22, true);
    advice.setPadding(dp(18), dp(18), dp(18), dp(18));
    advice.setTextColor(Color.parseColor("#d9ec90"));
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(Color.parseColor("#25392f"));
    bg.setCornerRadius(dp(12));
    advice.setBackground(bg);
    root.addView(advice);
    root.addView(text(summary[0], 16, false));
    root.addView(text("TAP A PAIR: YOUR THROW / HER THROW", 11, false));
    for (int i = 0; i < 3; i++) {
      LinearLayout row = new LinearLayout(this);
      for (int j = 0; j < 3; j++) {
        final String mine = RoundStore.THROWS[i], hers = RoundStore.THROWS[j];
        Button b = new Button(this);
        b.setText(RoundStore.SYMBOLS[i] + " / " + RoundStore.SYMBOLS[j]);
        b.setContentDescription(
            "You " + RoundStore.label(mine) + ", her " + RoundStore.label(hers));
        b.setTextSize(22);
        row.addView(b, new LinearLayout.LayoutParams(0, dp(62), 1));
        b.setOnClickListener(
            v -> {
              try {
                RoundStore.get(this).log(mine, hers);
                HandsWidget.status(this, "Round saved.");
                HandsWidget.update(this);
                SyncJob.schedule(this);
                render();
              } catch (Exception e) {
                Toast.makeText(this, "Couldn't save this round", Toast.LENGTH_LONG).show();
              }
            });
      }
      root.addView(row);
    }
    JSONArray history = RoundStore.get(this).all(false);
    if (history.length() > 0) {
      String id = history.optJSONObject(history.length() - 1).optString("id");
      button(
          "Undo last round ↶",
          () -> {
            RoundStore.get(this).undo(id);
            HandsWidget.status(this, "Round removed.");
            HandsWidget.update(this);
            SyncJob.schedule(this);
            render();
          });
    }
    root.addView(text("Skip this screen next time.", 26, true));
    root.addView(
        text(
            "Hold the Hands icon for “I threw rock/paper/scissors.” Pick her throw in the small"
                + " popup and you’re done.\n\n"
                + "For one-tap logging, add the widget: each button records both throws without"
                + " opening Hands. Your next-move advice updates there too.",
            14,
            false));
    button(
        "Add the one-tap widget",
        () -> {
          AppWidgetManager m = AppWidgetManager.getInstance(this);
          if (m.isRequestPinAppWidgetSupported())
            m.requestPinAppWidget(new ComponentName(this, HandsWidget.class), null, null);
          else
            Toast.makeText(
                    this, "Open your launcher's widget picker and choose Hands", Toast.LENGTH_LONG)
                .show();
        });
    SharedPreferences prefs = getSharedPreferences("hands", 0);
    root.addView(text("Your history, together.", 26, true));
    root.addView(
        text(
            prefs.getString(
                "backup",
                "Saved on this phone. Connect once to share history with the website and import"
                    + " your past games."),
            14,
            false));
    button("Connect website & cloud backup", this::pair);
    button(
        "Sync now",
        () -> {
          SyncJob.schedule(this);
          new Thread(
                  () -> {
                    boolean ok = SyncJob.sync(this);
                    runOnUiThread(
                        () -> {
                          Toast.makeText(
                                  this,
                                  ok ? "Sync complete" : "Saved on phone. Will retry backup.",
                                  Toast.LENGTH_SHORT)
                              .show();
                          render();
                        });
                  })
              .start();
        });
    button(
        "Open the full dashboard ↗",
        () ->
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://hoh.jacobsides.com"))));
    button(
        "Export history ↓",
        () -> {
          Intent intent =
              new Intent(Intent.ACTION_CREATE_DOCUMENT)
                  .setType("application/json")
                  .addCategory(Intent.CATEGORY_OPENABLE)
                  .putExtra(Intent.EXTRA_TITLE, "hands-history.json");
          startActivityForResult(intent, EXPORT);
        });
    button(
        "Import history ↑",
        () ->
            startActivityForResult(
                new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .setType("application/json")
                    .addCategory(Intent.CATEGORY_OPENABLE),
                IMPORT));
    root.addView(
        text(
            "Advice is a hunch, not a guarantee. It uses the same recent-throw and after-result"
                + " patterns as the website. No camera, contacts, location, or notification"
                + " permissions.",
            11,
            false));
  }

  private void pair() {
    SharedPreferences prefs = getSharedPreferences("hands", 0);
    String token = prefs.getString("token", "");
    if (token.isEmpty()) {
      byte[] bytes = new byte[32];
      new SecureRandom().nextBytes(bytes);
      token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
      if (!prefs.edit().putString("token", token).commit()) {
        Toast.makeText(this, "Couldn't save connection details", Toast.LENGTH_LONG).show();
        return;
      }
    }
    startActivity(
        new Intent(
            Intent.ACTION_VIEW, Uri.parse("https://hoh.jacobsides.com/connect#token=" + token)));
  }

  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (result != RESULT_OK || data == null || data.getData() == null) return;
    try {
      if (request == EXPORT) {
        try (var out = getContentResolver().openOutputStream(data.getData())) {
          out.write(RoundStore.get(this).all(true).toString().getBytes(StandardCharsets.UTF_8));
        }
        Toast.makeText(this, "History exported", Toast.LENGTH_SHORT).show();
      } else if (request == IMPORT) {
        try (var in = getContentResolver().openInputStream(data.getData())) {
          byte[] bytes = in.readNBytes(10000001);
          if (bytes.length > 10000000) throw new IllegalArgumentException("File too large");
          RoundStore.get(this).merge(new JSONArray(new String(bytes, StandardCharsets.UTF_8)));
        }
        HandsWidget.update(this);
        SyncJob.schedule(this);
        render();
        Toast.makeText(this, "History imported", Toast.LENGTH_SHORT).show();
      }
    } catch (Exception e) {
      Toast.makeText(
              this,
              "Couldn't read or save the history file. Existing rounds are safe.",
              Toast.LENGTH_LONG)
          .show();
    }
  }
}

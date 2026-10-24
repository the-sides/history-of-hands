package com.jacobsides.hands;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;

public final class HandsWidget extends AppWidgetProvider {
  private static final String LOG = "com.jacobsides.hands.LOG", UNDO = "com.jacobsides.hands.UNDO";

  public void onUpdate(Context c, AppWidgetManager manager, int[] ids) {
    update(c);
    SyncJob.schedule(c);
  }

  public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle options) {
    update(c);
  }

  public void onReceive(Context c, Intent intent) {
    super.onReceive(c, intent);
    try {
      if (LOG.equals(intent.getAction())) {
        String mine = intent.getStringExtra("mine"), hers = intent.getStringExtra("hers");
        RoundStore.get(c).log(mine, hers);
        String result = RoundStore.outcome(mine, hers);
        status(
            c,
            (result.equals("win") ? "You win!" : result.equals("loss") ? "She wins." : "A tie.")
                + " Saved.");
        update(c);
        SyncJob.schedule(c);
      } else if (UNDO.equals(intent.getAction())) {
        String id = intent.getStringExtra("id");
        if (id != null) {
          RoundStore.get(c).undo(id);
          status(c, "Round removed.");
          update(c);
          SyncJob.schedule(c);
        }
      }
    } catch (Exception e) {
      status(c, "Couldn't save. Open Hands to check storage.");
      Toast.makeText(c, "Couldn't save this round", Toast.LENGTH_LONG).show();
      update(c);
    }
  }

  static void status(Context c, String value) {
    c.getSharedPreferences("hands", 0).edit().putString("status", value).apply();
  }

  public static void update(Context c) {
    RoundStore store = RoundStore.get(c);
    String[] summary = store.summary();
    JSONArray rounds = store.all(false);
    RemoteViews views = new RemoteViews(c.getPackageName(), R.layout.hands_widget);
    views.setTextViewText(R.id.advice, summary[1]);
    views.setTextViewText(R.id.score, summary[0]);
    views.setTextViewText(
        R.id.status,
        c.getSharedPreferences("hands", 0).getString("status", "Ready. Saves offline, too."));
    int[][] buttons = {
      {R.id.pair_0_0, R.id.pair_0_1, R.id.pair_0_2},
      {R.id.pair_1_0, R.id.pair_1_1, R.id.pair_1_2},
      {R.id.pair_2_0, R.id.pair_2_1, R.id.pair_2_2}
    };
    for (int i = 0; i < 3; i++)
      for (int j = 0; j < 3; j++) {
        Intent action =
            new Intent(c, HandsWidget.class)
                .setAction(LOG)
                .putExtra("mine", RoundStore.THROWS[i])
                .putExtra("hers", RoundStore.THROWS[j])
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        views.setOnClickPendingIntent(
            buttons[i][j],
            PendingIntent.getBroadcast(
                c,
                10 + i * 3 + j,
                action,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
      }
    String last =
        rounds.length() > 0 ? rounds.optJSONObject(rounds.length() - 1).optString("id") : null;
    views.setOnClickPendingIntent(
        R.id.undo,
        PendingIntent.getBroadcast(
            c,
            99,
            new Intent(c, HandsWidget.class).setAction(UNDO).putExtra("id", last),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    views.setOnClickPendingIntent(
        R.id.title,
        PendingIntent.getActivity(
            c,
            100,
            new Intent(c, MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    AppWidgetManager manager = AppWidgetManager.getInstance(c);
    for (int id : manager.getAppWidgetIds(new ComponentName(c, HandsWidget.class)))
      manager.updateAppWidget(id, views);
    shortcuts(c, summary[1]);
  }

  public static void shortcuts(Context c, String advice) {
    ShortcutManager manager = c.getSystemService(ShortcutManager.class);
    if (manager == null || manager.isRateLimitingActive()) return;
    List<ShortcutInfo> items = new ArrayList<>();
    int max = manager.getMaxShortcutCountPerActivity();
    for (int i = 0; i < Math.min(3, max); i++) {
      Intent intent =
          new Intent(c, QuickLogActivity.class)
              .setAction(Intent.ACTION_VIEW)
              .putExtra("mine", RoundStore.THROWS[i]);
      items.add(
          new ShortcutInfo.Builder(c, "throw-" + i)
              .setShortLabel(
                  "I threw "
                      + RoundStore.label(RoundStore.THROWS[i]).toLowerCase(java.util.Locale.ROOT)
                      + " "
                      + RoundStore.SYMBOLS[i])
              .setLongLabel(
                  "I threw "
                      + RoundStore.label(RoundStore.THROWS[i]).toLowerCase(java.util.Locale.ROOT)
                      + " — log hers")
              .setIcon(Icon.createWithResource(c, R.drawable.app_icon))
              .setIntent(intent)
              .setRank(i)
              .build());
    }
    if (max > 3)
      items.add(
          new ShortcutInfo.Builder(c, "advice")
              .setShortLabel(advice.split(" · ")[0])
              .setLongLabel(advice)
              .setIcon(Icon.createWithResource(c, R.drawable.app_icon))
              .setIntent(new Intent(c, MainActivity.class).setAction(Intent.ACTION_VIEW))
              .setRank(3)
              .build());
    manager.setDynamicShortcuts(items);
  }
}

package com.jacobsides.hands;

import static com.jacobsides.hands.Design.*;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import org.json.JSONArray;

public final class MainActivity extends Activity {
  static final int EXPORT = 20, IMPORT = 21;
  int tab = 0, analyticsDays = 30, historyFilter = 0, historyLimit = 100;
  String historySearch = "", historyDay = "";
  boolean editing = false, selecting = false;
  final java.util.Set<String> selected = new java.util.HashSet<>();
  private ScrollView scroll;
  private LinearLayout shell, content, undoBar;
  private TextView cloudLabel;
  private Runnable undoAction;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable periodic =
      new Runnable() {
        public void run() {
          sync();
          handler.postDelayed(this, 20000);
        }
      };
  private final BroadcastReceiver changes =
      new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
          if (!editing && !(getCurrentFocus() instanceof EditText)) render(true);
          else updateCloud();
        }
      };

  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    if (saved != null) {
      tab = saved.getInt("tab", 0);
      analyticsDays = saved.getInt("analyticsDays", 30);
    }
    acceptPair(getIntent());
    render(false);
  }

  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    acceptPair(intent);
  }

  private void acceptPair(Intent intent) {
    if (intent.getData() != null
        && "historyhands".equals(intent.getData().getScheme())
        && "paired".equals(intent.getData().getHost())) {
      if (!"1".equals(intent.getData().getQueryParameter("cancel"))) {
        new Thread(
                () -> {
                  boolean ok = SyncJob.acceptSignIn(this);
                  runOnUiThread(
                      () -> {
                        if (!isFinishing()) {
                          toast(
                              ok
                                  ? "Signed in. Your games are saving automatically."
                                  : "Sign-in didn't finish. Your games are safe.");
                          render(true);
                        }
                      });
                },
                "hands-sign-in")
            .start();
      }
      intent.setData(null);
    }
  }

  protected void onResume() {
    super.onResume();
    registerReceiver(
        changes,
        new IntentFilter("com.jacobsides.hands.DATA_CHANGED"),
        Context.RECEIVER_NOT_EXPORTED);
    render(true);
    HandsWidget.update(this);
    handler.post(periodic);
  }

  protected void onPause() {
    super.onPause();
    handler.removeCallbacks(periodic);
    unregisterReceiver(changes);
  }

  protected void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    state.putInt("tab", tab);
    state.putInt("analyticsDays", analyticsDays);
  }

  void sync() {
    SyncJob.schedule(this);
    new Thread(() -> SyncJob.sync(this), "hands-auto-sync").start();
  }

  void navigate(int next) {
    tab = next;
    selected.clear();
    selecting = false;
    undoAction = null;
    render(false);
  }

  void changed(String message, Runnable undo) {
    undoAction = undo;
    HandsWidget.status(this, message);
    HandsWidget.update(this);
    SyncJob.schedule(this);
    render(true);
    sync();
  }

  String cloudStatus() {
    var p = getSharedPreferences("hands", 0);
    String token = p.getString("token", "");
    if (token.isEmpty()) return "Saved on this phone";
    String state = p.getString("sync_state", "pending");
    int n = RoundStore.get(this).pendingCount();
    switch (state) {
      case "syncing":
        return "Saving to your account…";
      case "signin":
        return "Sign in again · games safe on phone";
      case "account_changed":
        return "Confirm your account in Account";
      case "offline":
        return n > 0 ? n + " changes saved · retrying automatically" : "Offline · all games safe";
      default:
        return n > 0 ? n + " changes saving automatically" : "All games saved";
    }
  }

  void updateCloud() {
    if (cloudLabel != null) cloudLabel.setText(cloudStatus());
  }

  void render(boolean keepScroll) {
    int y = keepScroll && scroll != null ? scroll.getScrollY() : 0;
    shell = column(this);
    shell.setBackgroundColor(BG);
    shell.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          var bars = insets.getInsets(android.view.WindowInsets.Type.systemBars());
          v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
          return insets;
        });
    scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setClipToPadding(false);
    scroll.setVerticalScrollBarEnabled(false);
    content = column(this);
    content.setPadding(dp(this, 22), dp(this, 22), dp(this, 22), dp(this, 20));
    scroll.addView(content);
    shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout mast = row(this);
    TextView wordmark = text(this, "hands.", 26, LIME, true);
    mast.addView(wordmark, new LinearLayout.LayoutParams(0, -2, 1));
    TextView pairing = label(this, "YOU + HER");
    mast.addView(pairing);
    content.addView(mast);
    gap(content, 14);
    cloudLabel = text(this, cloudStatus(), 11, MUTED, false);
    cloudLabel.setContentDescription("Save status: " + cloudStatus());
    content.addView(cloudLabel);
    gap(content, 26);
    switch (tab) {
      case 1:
        InsightsScreen.show(this, content);
        break;
      case 2:
        HistoryScreen.show(this, content);
        break;
      case 3:
        AccountScreen.show(this, content);
        break;
      default:
        PlayScreen.show(this, content);
    }
    if (undoAction != null) {
      undoBar = row(this);
      undoBar.setPadding(dp(this, 22), dp(this, 8), dp(this, 14), dp(this, 8));
      undoBar.setBackgroundColor(RAISED);
      TextView message = text(this, "Change saved", 12, TEXT, false);
      undoBar.addView(message, new LinearLayout.LayoutParams(0, -2, 1));
      undoBar.addView(
          chip(
              this,
              "Undo",
              true,
              () -> {
                Runnable action = undoAction;
                undoAction = null;
                action.run();
                changed("Change undone.", null);
              }));
      shell.addView(undoBar);
    }
    LinearLayout nav = row(this);
    nav.setPadding(dp(this, 8), dp(this, 9), dp(this, 8), dp(this, 9));
    nav.setBackgroundColor(SURFACE);
    String[] names = {"Play", "Insights", "History", "Account"};
    String[] icons = {"✦", "▥", "≡", "◎"};
    for (int i = 0; i < 4; i++) {
      final int index = i;
      LinearLayout item = column(this);
      item.setGravity(Gravity.CENTER);
      item.setPadding(0, dp(this, 6), 0, dp(this, 6));
      TextView icon = text(this, icons[i], 22, i == tab ? LIME : MUTED, true);
      icon.setGravity(Gravity.CENTER);
      TextView title = text(this, names[i], 10, i == tab ? LIME : MUTED, true);
      title.setGravity(Gravity.CENTER);
      item.addView(icon);
      gap(item, 4);
      item.addView(title);
      item.setContentDescription(names[i] + " tab");
      item.setOnClickListener(v -> navigate(index));
      nav.addView(item, new LinearLayout.LayoutParams(0, dp(this, 64), 1));
    }
    shell.addView(nav);
    setContentView(shell);
    scroll.post(() -> scroll.scrollTo(0, y));
  }

  void log() {
    startActivity(new Intent(this, QuickLogActivity.class));
  }

  void signIn() {
    var p = getSharedPreferences("hands", 0);
    String token = p.getString("pending_token", "");
    if (token.isEmpty()) {
      byte[] bytes = new byte[32];
      new SecureRandom().nextBytes(bytes);
      token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
      if (!p.edit().putString("pending_token", token).commit()) {
        toast("Couldn't save sign-in details");
        return;
      }
    }
    startActivity(
        new Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://hoh.jacobsides.com/android/sign-in#token=" + token)));
  }

  void addWidget() {
    AppWidgetManager manager = AppWidgetManager.getInstance(this);
    if (manager.isRequestPinAppWidgetSupported())
      manager.requestPinAppWidget(new ComponentName(this, HandsWidget.class), null, null);
    else toast("Choose Hands from your launcher's widget picker");
  }

  void export() {
    startActivityForResult(
        new Intent(Intent.ACTION_CREATE_DOCUMENT)
            .setType("application/json")
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_TITLE, "hands-" + java.time.LocalDate.now() + ".json"),
        EXPORT);
  }

  void importFile() {
    startActivityForResult(
        new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .setType("application/json")
            .addCategory(Intent.CATEGORY_OPENABLE),
        IMPORT);
  }

  void toast(String value) {
    Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
  }

  static String when(String timestamp, String pattern) {
    return DateTimeFormatter.ofPattern(pattern)
        .withZone(ZoneId.systemDefault())
        .format(Instant.parse(timestamp));
  }

  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (result != RESULT_OK || data == null || data.getData() == null) return;
    try {
      if (request == EXPORT) {
        try (var out = getContentResolver().openOutputStream(data.getData())) {
          out.write(RoundStore.get(this).all(true).toString(2).getBytes(StandardCharsets.UTF_8));
        }
        toast("Your full history was exported");
      } else if (request == IMPORT) {
        try (var in = getContentResolver().openInputStream(data.getData())) {
          byte[] bytes = in.readNBytes(10000001);
          if (bytes.length > 10000000) throw new IllegalArgumentException();
          RoundStore.get(this).mergeLocal(new JSONArray(new String(bytes, StandardCharsets.UTF_8)));
        }
        changed("History imported.", null);
        toast("Import complete. Existing games kept.");
      }
    } catch (Exception e) {
      toast("Couldn't read that history file. Your games are unchanged.");
    }
  }
}

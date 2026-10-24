package com.jacobsides.hands;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

public final class SyncJob extends JobService {
  private volatile boolean stopped;

  public static void schedule(Context c) {
    if (c.getSharedPreferences("hands", 0).getString("token", "").isEmpty()) return;
    JobScheduler scheduler = c.getSystemService(JobScheduler.class);
    if (scheduler.getPendingJob(2) == null) {
      scheduler.schedule(
          new JobInfo.Builder(2, new ComponentName(c, SyncJob.class))
              .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
              .setPersisted(true)
              .setPeriodic(15 * 60 * 1000L)
              .build());
    }
    scheduler.schedule(
        new JobInfo.Builder(1, new ComponentName(c, SyncJob.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .setBackoffCriteria(30000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .build());
  }

  public boolean onStartJob(JobParameters params) {
    stopped = false;
    new Thread(
            () -> {
              boolean ok = sync(this);
              if (!stopped) jobFinished(params, !ok);
            },
            "hands-sync")
        .start();
    return true;
  }

  public boolean onStopJob(JobParameters params) {
    stopped = true;
    return true;
  }

  public static synchronized boolean sync(Context c) {
    String token = c.getSharedPreferences("hands", 0).getString("token", "");
    if (token.isEmpty()) return true;
    try {
      var prefs = c.getSharedPreferences("hands", 0);
      prefs.edit().putString("sync_state", "syncing").apply();
      notifyChanged(c);
      RoundStore store = RoundStore.get(c);
      if (!token.equals(prefs.getString("token", ""))) return true;
      JSONObject remote = call("GET", token, null);
      JSONObject user = remote.getJSONObject("user");
      String previous = prefs.getString("account_id", "");
      if (!previous.isEmpty() && !previous.equals(user.getString("id"))) {
        prefs.edit().putString("sync_state", "account_changed").apply();
        notifyChanged(c);
        return false;
      }
      store.merge(remote.getJSONArray("rounds"));
      JSONArray pending = store.pending();
      for (int start = 0; start < pending.length(); start += 250) {
        JSONArray batch = new JSONArray();
        for (int i = start; i < Math.min(start + 250, pending.length()); i++)
          batch.put(pending.get(i));
        JSONObject result = call("POST", token, new JSONObject().put("rounds", batch));
        JSONArray collisions = result.optJSONArray("conflicts");
        if (collisions != null)
          for (int i = 0; i < collisions.length(); i++)
            store.rememberConflict(collisions.getJSONObject(i));
        JSONArray accepted = result.optJSONArray("accepted");
        if (accepted == null) throw new IllegalStateException("Update required");
        JSONArray acknowledged = new JSONArray();
        for (int i = 0; i < batch.length(); i++) {
          JSONObject row = batch.getJSONObject(i);
          for (int j = 0; j < accepted.length(); j++)
            if (row.optString("id").equals(accepted.optString(j))) {
              acknowledged.put(row);
              break;
            }
        }
        store.acknowledge(acknowledged);
      }
      if (pending.length() > 0) {
        remote = call("GET", token, null);
        store.merge(remote.getJSONArray("rounds"));
      }
      if (!token.equals(prefs.getString("token", ""))) return true;
      prefs
          .edit()
          .putString("account_id", user.getString("id"))
          .putString("account_name", user.optString("name", "You"))
          .putBoolean("github", user.optBoolean("github", false))
          .putLong("last_sync", System.currentTimeMillis())
          .putString(
              "sync_state",
              store.conflicts().length() > 0
                  ? "conflict"
                  : store.pendingCount() > 0 ? "pending" : "saved")
          .putString("backup", "All games saved to your account")
          .apply();
      notifyChanged(c);
      HandsWidget.update(c);
      return true;
    } catch (Exception e) {
      c.getSharedPreferences("hands", 0)
          .edit()
          .putString("sync_state", e instanceof SignInRequired ? "signin" : "offline")
          .putString("backup", "Saved on phone · will upload automatically")
          .apply();
      notifyChanged(c);
      return false;
    }
  }

  public static synchronized boolean acceptSignIn(Context c) {
    var prefs = c.getSharedPreferences("hands", 0);
    String pending = prefs.getString("pending_token", "");
    if (pending.isEmpty()) return false;
    try {
      JSONObject remote = call("GET", pending, null);
      if (!remote.getJSONObject("user").optBoolean("github")) return false;
      disconnect(c);
      prefs
          .edit()
          .putString("token", pending)
          .remove("pending_token")
          .remove("account_id")
          .remove("account_name")
          .putString("sync_state", "pending")
          .commit();
      RoundStore.get(c).prepareForAccount();
      return sync(c);
    } catch (Exception e) {
      return false;
    }
  }

  public static void notifyChanged(Context c) {
    c.sendBroadcast(
        new android.content.Intent("com.jacobsides.hands.DATA_CHANGED")
            .setPackage(c.getPackageName()));
  }

  private static final class SignInRequired extends Exception {}

  public static void disconnect(Context c) {
    String token = c.getSharedPreferences("hands", 0).getString("token", "");
    if (token.isEmpty()) return;
    new Thread(
            () -> {
              try {
                java.net.HttpURLConnection connection =
                    (java.net.HttpURLConnection)
                        new java.net.URL("https://hoh.jacobsides.com/api/android/account")
                            .openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("Authorization", "Bearer " + token);
                try {
                  connection.getResponseCode();
                } finally {
                  connection.disconnect();
                }
              } catch (Exception ignored) {
              }
            },
            "hands-sign-out")
        .start();
    c.getSystemService(android.app.job.JobScheduler.class).cancel(1);
    c.getSystemService(android.app.job.JobScheduler.class).cancel(2);
  }

  private static JSONObject call(String method, String token, JSONObject body) throws Exception {
    HttpURLConnection connection =
        (HttpURLConnection)
            new URL("https://hoh.jacobsides.com/api/android/history").openConnection();
    connection.setConnectTimeout(10000);
    connection.setReadTimeout(15000);
    connection.setRequestMethod(method);
    connection.setRequestProperty("Authorization", "Bearer " + token);
    connection.setRequestProperty("Accept", "application/json");
    try {
      if (body != null) {
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        try (var out = connection.getOutputStream()) {
          out.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
      }
      if (connection.getResponseCode() == 401) throw new SignInRequired();
      if (connection.getResponseCode() != 200)
        throw new IllegalStateException("Backup unavailable");
      try (var stream = connection.getInputStream()) {
        return new JSONObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
      }
    } finally {
      connection.disconnect();
    }
  }
}

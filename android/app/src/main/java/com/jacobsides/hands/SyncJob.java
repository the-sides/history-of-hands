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
      JSONObject remote = call("GET", token, null);
      RoundStore store = RoundStore.get(c);
      store.merge(remote.getJSONArray("rounds"));
      JSONArray all = store.all(true);
      for (int start = 0; start < all.length(); start += 250) {
        JSONArray batch = new JSONArray();
        for (int i = start; i < Math.min(start + 250, all.length()); i++) batch.put(all.get(i));
        call("POST", token, new JSONObject().put("rounds", batch));
      }
      c.getSharedPreferences("hands", 0)
          .edit()
          .putString("backup", "Backed up to your account")
          .apply();
      HandsWidget.update(c);
      return true;
    } catch (Exception e) {
      c.getSharedPreferences("hands", 0)
          .edit()
          .putString("backup", "Saved on phone · backup pending")
          .apply();
      return false;
    }
  }

  private static JSONObject call(String method, String token, JSONObject body) throws Exception {
    HttpURLConnection connection =
        (HttpURLConnection) new URL("https://hoh.jacobsides.com/api/hand-history").openConnection();
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

package com.jacobsides.hands;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

public final class RoundStore extends SQLiteOpenHelper {
  public static final String[] THROWS = {"ROCK", "PAPER", "SCISSORS"};
  public static final String[] SYMBOLS = {"✊", "✋", "✌"};
  private static RoundStore instance;

  public static synchronized RoundStore get(Context context) {
    if (instance == null) instance = new RoundStore(context.getApplicationContext());
    return instance;
  }

  private RoundStore(Context context) {
    super(context, "hands.db", null, 2);
  }

  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE rounds(id TEXT PRIMARY KEY, mine TEXT NOT NULL, hers TEXT NOT NULL, playedAt"
            + " TEXT NOT NULL, deleted INTEGER NOT NULL DEFAULT 0, revision INTEGER NOT NULL"
            + " DEFAULT 0, dirty INTEGER NOT NULL DEFAULT 1)");
  }

  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 2) {
      db.execSQL("ALTER TABLE rounds ADD COLUMN revision INTEGER NOT NULL DEFAULT 0");
      db.execSQL("ALTER TABLE rounds ADD COLUMN dirty INTEGER NOT NULL DEFAULT 1");
    }
  }

  public static int index(String value) {
    for (int i = 0; i < 3; i++) if (THROWS[i].equals(value)) return i;
    return -1;
  }

  public static String label(String value) {
    return value.substring(0, 1) + value.substring(1).toLowerCase(java.util.Locale.ROOT);
  }

  public static String outcome(String mine, String hers) {
    int m = index(mine), h = index(hers);
    if (m < 0 || h < 0) throw new IllegalArgumentException("Invalid throw");
    return m == h ? "tie" : (m - h + 3) % 3 == 1 ? "win" : "loss";
  }

  public synchronized JSONObject log(String mine, String hers) {
    if (index(mine) < 0 || index(hers) < 0) throw new IllegalArgumentException("Invalid throw");
    try {
      JSONObject r =
          new JSONObject()
              .put("id", UUID.randomUUID().toString())
              .put("mine", mine)
              .put("hers", hers)
              .put(
                  "playedAt",
                  DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                      .withZone(ZoneOffset.UTC)
                      .format(Instant.now()));
      r.put("revision", nextRevision());
      mergeLocal(new JSONArray().put(r));
      return r;
    } catch (org.json.JSONException e) {
      throw new IllegalStateException(e);
    }
  }

  public synchronized void merge(JSONArray rounds) {
    mergeRows(rounds, false);
  }

  public synchronized void mergeLocal(JSONArray rounds) {
    mergeRows(rounds, true);
  }

  private void mergeRows(JSONArray rounds, boolean local) {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      for (int i = 0; i < rounds.length(); i++) {
        JSONObject r = rounds.getJSONObject(i);
        String id = r.getString("id"),
            mine = r.getString("mine"),
            hers = r.getString("hers"),
            when = r.getString("playedAt");
        if (id.isEmpty() || id.length() > 100 || index(mine) < 0 || index(hers) < 0)
          throw new IllegalArgumentException("Invalid game");
        Instant.parse(when);
        long revision = r.optLong("revision", 0);
        if (revision < 0 || revision > 9007199254740991L)
          throw new IllegalArgumentException("Invalid revision");
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("mine", mine);
        values.put("hers", hers);
        values.put("playedAt", when);
        values.put("deleted", r.optBoolean("deleted", false) ? 1 : 0);
        values.put("revision", revision);
        values.put("dirty", local ? 1 : 0);
        long existingRevision = -1;
        boolean existingDeleted = false;
        try (Cursor cursor =
            db.query(
                "rounds",
                new String[] {"revision", "deleted"},
                "id=?",
                new String[] {id},
                null,
                null,
                null)) {
          if (cursor.moveToFirst()) {
            existingRevision = cursor.getLong(0);
            existingDeleted = cursor.getInt(1) != 0;
          }
        }
        if (existingRevision < 0) db.insertOrThrow("rounds", null, values);
        else if (revision > existingRevision
            || (revision == 0
                && existingRevision == 0
                && r.optBoolean("deleted", false)
                && !existingDeleted)) db.update("rounds", values, "id=?", new String[] {id});
      }
      db.setTransactionSuccessful();
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid history file", e);
    } finally {
      db.endTransaction();
    }
  }

  private synchronized long nextRevision() {
    try (Cursor c =
        getReadableDatabase().rawQuery("SELECT COALESCE(MAX(revision),0) FROM rounds", null)) {
      c.moveToFirst();
      return Math.max(System.currentTimeMillis(), c.getLong(0) + 1);
    }
  }

  public synchronized JSONArray pending() {
    return query("dirty=1");
  }

  public synchronized void acknowledge(JSONArray sent) {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      for (int i = 0; i < sent.length(); i++) {
        JSONObject r = sent.optJSONObject(i);
        ContentValues v = new ContentValues();
        v.put("dirty", 0);
        db.update(
            "rounds",
            v,
            "id=? AND revision=?",
            new String[] {r.optString("id"), String.valueOf(r.optLong("revision", 0))});
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public synchronized int pendingCount() {
    try (Cursor c =
        getReadableDatabase().rawQuery("SELECT COUNT(*) FROM rounds WHERE dirty=1", null)) {
      c.moveToFirst();
      return c.getInt(0);
    }
  }

  public synchronized void prepareForAccount() {
    getWritableDatabase().execSQL("UPDATE rounds SET dirty=1");
  }

  public synchronized void edit(String id, String mine, String hers, String playedAt) {
    if (index(mine) < 0 || index(hers) < 0) throw new IllegalArgumentException("Invalid throw");
    Instant.parse(playedAt);
    ContentValues v = new ContentValues();
    v.put("mine", mine);
    v.put("hers", hers);
    v.put("playedAt", playedAt);
    v.put("revision", nextRevision());
    v.put("dirty", 1);
    if (getWritableDatabase().update("rounds", v, "id=?", new String[] {id}) != 1)
      throw new IllegalArgumentException("Game not found");
  }

  public synchronized void setDeleted(String id, boolean deleted) {
    ContentValues v = new ContentValues();
    v.put("deleted", deleted ? 1 : 0);
    v.put("revision", nextRevision());
    v.put("dirty", 1);
    getWritableDatabase().update("rounds", v, "id=?", new String[] {id});
  }

  public synchronized void deleteAll() {
    JSONArray rows = all(false);
    getWritableDatabase().beginTransaction();
    try {
      for (int i = 0; i < rows.length(); i++)
        setDeleted(rows.optJSONObject(i).optString("id"), true);
      getWritableDatabase().setTransactionSuccessful();
    } finally {
      getWritableDatabase().endTransaction();
    }
  }

  public synchronized JSONArray all(boolean includeDeleted) {
    return query(includeDeleted ? null : "deleted=0");
  }

  private JSONArray query(String where) {
    JSONArray result = new JSONArray();
    try (Cursor c =
        getReadableDatabase()
            .query("rounds", null, where, null, null, null, "playedAt ASC, id ASC")) {
      while (c.moveToNext())
        result.put(
            new JSONObject()
                .put("id", c.getString(0))
                .put("mine", c.getString(1))
                .put("hers", c.getString(2))
                .put("playedAt", c.getString(3))
                .put("deleted", c.getInt(4) != 0)
                .put("revision", c.getLong(5)));
    } catch (org.json.JSONException e) {
      throw new IllegalStateException(e);
    }
    return result;
  }

  public synchronized void undo(String id) {
    setDeleted(id, true);
  }

  public String[] summary() {
    JSONArray rounds = all(false);
    int wins = 0, losses = 0, ties = 0;
    for (int i = 0; i < rounds.length(); i++) {
      JSONObject r = rounds.optJSONObject(i);
      switch (outcome(r.optString("mine"), r.optString("hers"))) {
        case "win":
          wins++;
          break;
        case "loss":
          losses++;
          break;
        default:
          ties++;
      }
    }
    return new String[] {"You " + wins + " · Her " + losses + " · Ties " + ties, advice(rounds)};
  }

  public static String advice(JSONArray rounds) {
    List<JSONObject> history = new ArrayList<>();
    for (int i = 0; i < rounds.length(); i++) {
      JSONObject r = rounds.optJSONObject(i);
      if (!r.optBoolean("deleted")) history.add(r);
    }
    List<JSONObject> sample = history.subList(Math.max(0, history.size() - 30), history.size());
    boolean conditional = false;
    if (!history.isEmpty()) {
      JSONObject last = history.get(history.size() - 1);
      List<JSONObject> matches = new ArrayList<>();
      for (int i = 1; i < history.size(); i++) {
        JSONObject previous = history.get(i - 1);
        if (previous.optString("hers").equals(last.optString("hers"))
            && outcome(previous.optString("mine"), previous.optString("hers"))
                .equals(outcome(last.optString("mine"), last.optString("hers"))))
          matches.add(history.get(i));
      }
      if (matches.size() >= 8) {
        sample = matches;
        conditional = true;
      }
    }
    if (sample.size() < 8)
      return "Mix your throws · " + (8 - sample.size()) + " more games to find a trend";
    int[] counts = new int[3];
    for (JSONObject r : sample) counts[index(r.optString("hers"))]++;
    int best = 0;
    int[] score = {counts[2] - counts[1], counts[0] - counts[2], counts[1] - counts[0]};
    for (int i = 1; i < 3; i++) if (score[i] > score[best]) best = i;
    if (score[0] == score[1] && score[1] == score[2]) return "No clear edge · mix your throws";
    return "Try "
        + label(THROWS[best])
        + " "
        + SYMBOLS[best]
        + " · "
        + sample.size()
        + (conditional ? " matching" : " recent")
        + " games";
  }
}

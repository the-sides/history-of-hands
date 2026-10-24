package com.jacobsides.hands;

import static com.jacobsides.hands.Design.*;

import android.app.AlertDialog;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

/** One deliberate swipe or completed double tap saves one round. */
final class ThrowControl extends LinearLayout {
  private final MainActivity activity;
  private final String mine;
  private final TextView symbol, hint;
  private final GestureDetector taps;
  private float startX, startY;
  private boolean moved, cancelled;

  ThrowControl(MainActivity a, int index) {
    super(a);
    activity = a;
    mine = RoundStore.THROWS[index];
    setOrientation(VERTICAL);
    setGravity(Gravity.CENTER);
    setPadding(dp(a, 4), dp(a, 12), dp(a, 4), dp(a, 12));
    setBackground(background(RAISED, 18, a));
    setClickable(true);
    setFocusable(true);
    setContentDescription(
        RoundStore.label(mine)
            + ". Swipe up for your win, down for her win, double tap for a tie. Hold for result"
            + " choices.");
    addView(text(a, "↑ WIN", 9, LIME, true));
    gap(this, 12);
    symbol = text(a, RoundStore.SYMBOLS[index], 34, TEXT, false);
    addView(symbol);
    gap(this, 12);
    addView(text(a, RoundStore.label(mine), 11, TEXT, true));
    gap(this, 8);
    hint = text(a, "↓ LOSS", 9, CORAL, true);
    addView(hint);
    setOnClickListener(v -> activity.toast("Swipe up to win, down to lose, or double tap to tie."));
    setOnLongClickListener(
        v -> {
          choices();
          return true;
        });
    setAccessibilityDelegate(
        new View.AccessibilityDelegate() {
          @Override
          public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setClassName("android.widget.Button");
            info.addAction(
                new AccessibilityNodeInfo.AccessibilityAction(R.id.result_win, "Log your win"));
            info.addAction(
                new AccessibilityNodeInfo.AccessibilityAction(R.id.result_loss, "Log your loss"));
            info.addAction(
                new AccessibilityNodeInfo.AccessibilityAction(R.id.result_tie, "Log a tie"));
          }

          @Override
          public boolean performAccessibilityAction(View host, int action, android.os.Bundle args) {
            if (action == R.id.result_win) {
              save("win");
              return true;
            }
            if (action == R.id.result_loss) {
              save("loss");
              return true;
            }
            if (action == R.id.result_tie) {
              save("tie");
              return true;
            }
            return super.performAccessibilityAction(host, action, args);
          }
        });
    taps =
        new GestureDetector(
            a,
            new GestureDetector.SimpleOnGestureListener() {
              @Override
              public boolean onDown(MotionEvent e) {
                return true;
              }

              @Override
              public boolean onSingleTapConfirmed(MotionEvent e) {
                performClick();
                return true;
              }

              @Override
              public boolean onDoubleTap(MotionEvent e) {
                return true;
              }

              @Override
              public boolean onDoubleTapEvent(MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_UP && !moved && !cancelled)
                  save("tie");
                return true;
              }

              @Override
              public void onLongPress(MotionEvent e) {
                if (!moved && !cancelled) {
                  cancelled = true;
                  performLongClick();
                }
              }
            });
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    int action = e.getActionMasked();
    if (action == MotionEvent.ACTION_DOWN) {
      startX = e.getX();
      startY = e.getY();
      moved = false;
      cancelled = false;
      getParent().requestDisallowInterceptTouchEvent(true);
    }
    float dx = e.getX() - startX, dy = e.getY() - startY;
    if (action == MotionEvent.ACTION_POINTER_DOWN) cancelled = true;
    if (action == MotionEvent.ACTION_MOVE) {
      if (Math.hypot(dx, dy)
          > android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop()) moved = true;
      if (!cancelled) {
        symbol.setTranslationY(Math.max(-dp(activity, 24), Math.min(dp(activity, 24), dy / 2)));
        boolean ready = Math.abs(dy) >= dp(activity, 40) && Math.abs(dy) > Math.abs(dx) * 1.3f;
        hint.setText(ready ? (dy < 0 ? "RELEASE · WIN" : "RELEASE · LOSS") : "↓ LOSS");
        setBackground(
            background(ready ? (dy < 0 ? 0xff304334 : 0xff49302f) : RAISED, 18, activity));
      }
    }
    if (action == MotionEvent.ACTION_CANCEL) cancelled = true;
    // Feed the final position before dispatching UP so a dragged second tap cannot save a tie.
    if (action == MotionEvent.ACTION_UP
        && Math.hypot(dx, dy)
            > android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop()) moved = true;
    taps.onTouchEvent(e);
    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
      getParent().requestDisallowInterceptTouchEvent(false);
      symbol.setTranslationY(0);
      hint.setText("↓ LOSS");
      setBackground(background(RAISED, 18, activity));
      if (action == MotionEvent.ACTION_UP
          && !cancelled
          && Math.abs(dy) >= dp(activity, 40)
          && Math.abs(dy) > Math.abs(dx) * 1.3f) save(dy < 0 ? "win" : "loss");
    }
    return true;
  }

  private void choices() {
    new AlertDialog.Builder(activity)
        .setTitle(RoundStore.label(mine) + " · result")
        .setItems(
            new String[] {"I won", "I lost", "Tie"},
            (dialog, which) -> save(new String[] {"win", "loss", "tie"}[which]))
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void save(String result) {
    try {
      String hers = QuickResult.opponent(mine, result);
      var game = RoundStore.get(activity).log(mine, hers);
      performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
      String message =
          RoundStore.label(mine)
              + " · "
              + (result.equals("win") ? "You won" : result.equals("loss") ? "She won" : "Tie");
      activity.changed(
          message, () -> RoundStore.get(activity).setDeleted(game.optString("id"), true));
      activity.toast(message + ". Saved.");
    } catch (Exception ex) {
      activity.toast("Couldn't save. Check phone storage in Hands.");
    }
  }
}

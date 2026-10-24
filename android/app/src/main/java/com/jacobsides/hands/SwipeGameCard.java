package com.jacobsides.hands;

import static com.jacobsides.hands.Design.*;

import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

/** Horizontal swipes trash a game; vertical scrolling and checkbox taps retain their behavior. */
final class SwipeGameCard extends LinearLayout {
  private final MainActivity activity;
  private final Runnable remove;
  private final boolean enabled;
  private float startX, startY;
  private boolean dragging, cancelled, moved;

  SwipeGameCard(MainActivity a, boolean enabled, Runnable remove) {
    super(a);
    activity = a;
    this.enabled = enabled;
    this.remove = remove;
    setOrientation(VERTICAL);
    setBackground(background(SURFACE, 24, a));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.bottomMargin = dp(a, 14);
    setLayoutParams(p);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent e) {
    if (!enabled) return super.onInterceptTouchEvent(e);
    if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
      startX = e.getX();
      startY = e.getY();
      moved = false;
      dragging = false;
      cancelled = false;
    } else if (e.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
      cancelled = true;
    } else if (e.getActionMasked() == MotionEvent.ACTION_MOVE && !cancelled) {
      float dx = e.getX() - startX, dy = e.getY() - startY;
      if (Math.abs(dx) > ViewConfiguration.get(getContext()).getScaledTouchSlop() * 2
          && Math.abs(dx) > Math.abs(dy) * 1.5f) {
        dragging = true;
        getParent().requestDisallowInterceptTouchEvent(true);
        return true;
      }
    }
    return super.onInterceptTouchEvent(e);
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (!enabled) return super.onTouchEvent(e);
    if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
      startX = e.getX();
      startY = e.getY();
      moved = false;
      dragging = false;
      cancelled = false;
    }
    // Translate children rather than this view, so touch coordinates remain stable.
    float dx = e.getX() - startX, dy = e.getY() - startY;
    if (e.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) cancelled = true;
    if (e.getActionMasked() == MotionEvent.ACTION_MOVE && !cancelled) {
      if (Math.hypot(dx, dy) > ViewConfiguration.get(getContext()).getScaledTouchSlop())
        moved = true;
      if (Math.abs(dx) > ViewConfiguration.get(getContext()).getScaledTouchSlop() * 2
          && Math.abs(dx) > Math.abs(dy) * 1.5f) {
        dragging = true;
        getParent().requestDisallowInterceptTouchEvent(true);
      }
      if (dragging) {
        for (int i = 0; i < getChildCount(); i++) getChildAt(i).setTranslationX(dx);
        setBackground(background(ready(dx, dy) ? 0xff703d3a : 0xff3b292d, 24, activity));
      }
    }
    if (e.getActionMasked() == MotionEvent.ACTION_UP
        || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
      boolean delete =
          dragging && !cancelled && e.getActionMasked() == MotionEvent.ACTION_UP && ready(dx, dy);
      getParent().requestDisallowInterceptTouchEvent(false);
      for (int i = 0; i < getChildCount(); i++)
        getChildAt(i).animate().translationX(0).setDuration(140).start();
      setBackground(background(SURFACE, 24, activity));
      if (delete) {
        performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
        remove.run();
        return true;
      }
      if (dragging || cancelled || moved) {
        MotionEvent cancel = MotionEvent.obtain(e);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        super.onTouchEvent(cancel);
        cancel.recycle();
        return true;
      }
    }
    return dragging || super.onTouchEvent(e);
  }

  private boolean ready(float dx, float dy) {
    return Math.abs(dx) >= Math.max(dp(activity, 72), getWidth() * .3f)
        && Math.abs(dx) > Math.abs(dy) * 1.5f;
  }
}

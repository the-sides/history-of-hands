package com.jacobsides.hands;

import static com.jacobsides.hands.Design.*;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

final class WeekChart extends View {
  private final Analytics data;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

  WeekChart(Context c, Analytics data) {
    super(c);
    this.data = data;
    setMinimumHeight(dp(c, 160));
    setContentDescription(
        "Last seven days of games. Green is your wins, coral is her wins, grey is ties.");
  }

  protected void onDraw(Canvas c) {
    super.onDraw(c);
    float w = getWidth(),
        h = getHeight(),
        bottom = h - dp(getContext(), 26),
        top = dp(getContext(), 14),
        step = w / 7;
    int max = 1;
    for (int i = 0; i < 7; i++)
      max = Math.max(max, data.dailyWins[i] + data.dailyLosses[i] + data.dailyTies[i]);
    paint.setColor(LINE);
    paint.setStrokeWidth(dp(getContext(), 1));
    for (int i = 0; i < 3; i++) {
      float y = top + (bottom - top) * i / 2;
      c.drawLine(0, y, w, y, paint);
    }
    for (int i = 0; i < 7; i++) {
      float x = step * i + step * .2f, bw = step * .6f, y = bottom;
      int[] values = {data.dailyWins[i], data.dailyLosses[i], data.dailyTies[i]};
      int[] colors = {LIME, CORAL, MUTED};
      for (int j = 0; j < 3; j++) {
        float bh = (bottom - top) * values[j] / max;
        if (bh > 0) {
          paint.setColor(colors[j]);
          c.drawRoundRect(x, y - bh, x + bw, y, dp(getContext(), 3), dp(getContext(), 3), paint);
          y -= bh;
        }
      }
      if (y == bottom) {
        paint.setColor(RAISED);
        c.drawRoundRect(x, bottom - dp(getContext(), 3), x + bw, bottom, 2, 2, paint);
      }
      paint.setColor(i == 6 ? TEXT : MUTED);
      paint.setTextSize(dp(getContext(), 10));
      paint.setTextAlign(Paint.Align.CENTER);
      String day = LocalDate.now().minusDays(6 - i).format(DateTimeFormatter.ofPattern("EEEEE"));
      c.drawText(day, step * (i + .5f), h - dp(getContext(), 4), paint);
    }
  }
}

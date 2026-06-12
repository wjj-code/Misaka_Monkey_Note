package com.transsion.ledger.ui.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Locale;

public class LineChartView extends View {

    private String[] labels = new String[0];
    private float[] values = new float[0];
    private int lineColor = 0xFF2D9CDB;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public LineChartView(Context context) {
        super(context);
        init();
    }

    public LineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint.setColor(0xFFE5E7EB);
        gridPaint.setStrokeWidth(1f);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4f);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        pointPaint.setStyle(Paint.Style.FILL);

        labelPaint.setColor(0xFF9CA3AF);
        labelPaint.setTextSize(28f);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        emptyPaint.setColor(0xFF9CA3AF);
        emptyPaint.setTextSize(32f);
        emptyPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setLineColor(int color) {
        lineColor = color;
        invalidate();
    }

    public void setData(String[] labels, float[] values) {
        this.labels = labels != null ? labels : new String[0];
        this.values = values != null ? values : new float[0];
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (values.length == 0) {
            canvas.drawText("暂无数据", w / 2f, h / 2f, emptyPaint);
            return;
        }

        float left = 48f;
        float right = w - 16f;
        float bottom = h - 40f;
        float top = 24f;
        float chartH = bottom - top;
        float chartW = right - left;

        float max = 0;
        for (float v : values) max = Math.max(max, v);
        if (max <= 0) max = 1;

        for (int i = 0; i <= 4; i++) {
            float y = top + chartH * i / 4f;
            canvas.drawLine(left, y, right, y, gridPaint);
        }

        int n = values.length;
        float stepX = n > 1 ? chartW / (n - 1) : 0;

        linePaint.setColor(lineColor);
        pointPaint.setColor(lineColor);

        Path path = new Path();
        for (int i = 0; i < n; i++) {
            float x = n > 1 ? left + stepX * i : left + chartW / 2f;
            float y = bottom - (values[i] / max) * chartH;
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
            canvas.drawCircle(x, y, 6f, pointPaint);
            if (i < labels.length) {
                canvas.drawText(labels[i], x, h - 12f, labelPaint);
            }
        }
        canvas.drawPath(path, linePaint);
    }
}

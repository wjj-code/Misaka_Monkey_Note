package com.transsion.ledger.ui.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PieChartView extends View {

    private final List<StatNode> slices = new ArrayList<>();
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private double total;

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        textPaint.setColor(0xFF6B7280);
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<StatNode> nodes) {
        slices.clear();
        total = 0;
        if (nodes != null) {
            slices.addAll(nodes);
            for (StatNode n : nodes) total += n.amount;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (slices.isEmpty() || total <= 0) {
            canvas.drawText("暂无数据", w / 2f, h / 2f, textPaint);
            return;
        }

        float pad = 16f;
        float size = Math.min(w, h) - pad * 2;
        float left = (w - size) / 2f;
        float top = (h - size) / 2f;
        oval.set(left, top, left + size, top + size);

        float start = -90f;
        for (StatNode node : slices) {
            if (node.amount <= 0) continue;
            float sweep = (float) (node.amount / total * 360f);
            arcPaint.setColor(node.color);
            canvas.drawArc(oval, start, sweep, true, arcPaint);
            start += sweep;
        }

        float hole = size * 0.45f;
        arcPaint.setColor(0xFFFFFFFF);
        canvas.drawCircle(w / 2f, h / 2f, hole / 2f, arcPaint);

        textPaint.setTextSize(24f);
        textPaint.setColor(0xFF1A1A2E);
        canvas.drawText(String.format(Locale.getDefault(), "%.0f", total), w / 2f, h / 2f - 6, textPaint);
        textPaint.setTextSize(18f);
        textPaint.setColor(0xFF9CA3AF);
        canvas.drawText("合计", w / 2f, h / 2f + 18, textPaint);
    }
}

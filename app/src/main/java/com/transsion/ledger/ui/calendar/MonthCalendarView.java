package com.transsion.ledger.ui.calendar;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.transsion.ledger.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 月历网格视图，支持日支出标注和点击
 */
public class MonthCalendarView {

    private final GridLayout gridDays;
    private final TextView txtMonthTitle;
    private final Map<String, Double> dailyExpenses = new HashMap<>();
    private final Calendar calendar = Calendar.getInstance(Locale.getDefault());
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat yearMonthFmt = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
    private String currentYearMonth;
    private String selectedDate;
    private final boolean compact;
    private OnDayClickListener dayClickListener;
    private OnMonthChangedListener monthChangedListener;
    private OnMonthTitleClickListener monthTitleClickListener;

    public interface OnDayClickListener {
        void onDayClick(String date); // yyyy-MM-dd
    }

    public interface OnMonthChangedListener {
        void onMonthChanged(String yearMonth);
    }

    public interface OnMonthTitleClickListener {
        void onMonthTitleClick(String yearMonth);
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.dayClickListener = listener;
    }

    public void setOnMonthChangedListener(OnMonthChangedListener listener) {
        this.monthChangedListener = listener;
    }

    public void setOnMonthTitleClickListener(OnMonthTitleClickListener listener) {
        this.monthTitleClickListener = listener;
    }

    public MonthCalendarView(View rootView) {
        this(rootView, false);
    }

    /** @param compact 记账选日期等窄空间场景：更小格高、不展示日支出 */
    public MonthCalendarView(View rootView, boolean compact) {
        this.compact = compact;
        gridDays = rootView.findViewById(R.id.grid_days);
        txtMonthTitle = rootView.findViewById(R.id.txt_month_title);

        rootView.findViewById(R.id.btn_prev_month).setOnClickListener(v -> shiftMonth(-1));
        rootView.findViewById(R.id.btn_next_month).setOnClickListener(v -> shiftMonth(1));

        txtMonthTitle.setOnClickListener(v -> {
            if (monthTitleClickListener != null) {
                monthTitleClickListener.onMonthTitleClick(currentYearMonth);
            }
        });

        Calendar now = Calendar.getInstance();
        currentYearMonth = String.format(Locale.getDefault(), "%d-%02d",
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1);
        updateTitle();
    }

    public void setDailyExpenses(Map<String, Double> expenses) {
        dailyExpenses.clear();
        if (expenses != null) dailyExpenses.putAll(expenses);
        buildGrid();
    }

    public void setSelectedDate(String date) {
        selectedDate = date;
        buildGrid();
    }

    public String getCurrentYearMonth() {
        return currentYearMonth;
    }

    /** 记账选日期等场景：同步到指定 Calendar 的年月与选中高亮 */
    public void syncFromCalendar(Calendar cal) {
        if (cal == null) return;
        currentYearMonth = String.format(Locale.getDefault(), "%d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
        selectedDate = dateFmt.format(cal.getTime());
        updateTitle();
        buildGrid();
    }

    /** 跳转到指定年月并通知外部加载数据 */
    public void jumpToYearMonth(String yearMonth) {
        if (yearMonth == null || yearMonth.equals(currentYearMonth)) return;
        currentYearMonth = yearMonth;
        updateTitle();
        buildGrid();
        notifyMonthChanged();
    }

    private void shiftMonth(int delta) {
        String[] parts = currentYearMonth.split("-");
        int y = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);
        m += delta;
        if (m < 1) { y--; m = 12; }
        else if (m > 12) { y++; m = 1; }
        currentYearMonth = String.format(Locale.getDefault(), "%d-%02d", y, m);
        updateTitle();
        buildGrid();
        notifyMonthChanged();
    }

    private void notifyMonthChanged() {
        if (monthChangedListener != null) {
            monthChangedListener.onMonthChanged(currentYearMonth);
        }
    }

    private void updateTitle() {
        String[] parts = currentYearMonth.split("-");
        txtMonthTitle.setText(parts[0] + "年" + Integer.parseInt(parts[1]) + "月");
    }

    private void buildGrid() {
        gridDays.removeAllViews();
        String[] parts = currentYearMonth.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        calendar.set(year, month - 1, 1);
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        Context ctx = gridDays.getContext();
        String todayStr = dateFmt.format(new Date());
        boolean viewingCurrentMonth = currentYearMonth.equals(yearMonthFmt.format(new Date()));

        for (int i = 0; i < firstDayOfWeek; i++) {
            gridDays.addView(createEmptyCell(ctx));
        }

        for (int day = 1; day <= maxDay; day++) {
            String dateKey = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month, day);
            Double expense = dailyExpenses.get(dateKey);
            boolean isToday = viewingCurrentMonth && dateKey.equals(todayStr);
            boolean isSelected = dateKey.equals(selectedDate);
            View cell = createDayCell(ctx, day, expense, isToday, isSelected);
            final String dk = dateKey;
            cell.setOnClickListener(v -> {
                if (dayClickListener != null) dayClickListener.onDayClick(dk);
            });
            gridDays.addView(cell);
        }
    }

    private int cellHeightPx(Context ctx) {
        float dp = compact ? 34f : 50f;
        return (int) (dp * ctx.getResources().getDisplayMetrics().density);
    }

    private View createEmptyCell(Context ctx) {
        View v = new View(ctx);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = cellHeightPx(ctx);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        v.setLayoutParams(lp);
        return v;
    }

    private View createDayCell(Context ctx, int day, Double expense, boolean isToday, boolean isSelected) {
        LinearLayout cell = new LinearLayout(ctx);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(2, compact ? 3 : 6, 2, compact ? 3 : 6);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = cellHeightPx(ctx);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        cell.setLayoutParams(lp);

        if (isSelected) {
            cell.setBackgroundColor(Color.parseColor("#2D9CDB"));
        } else if (isToday) {
            cell.setBackgroundColor(Color.parseColor("#DBEAFE"));
        }

        TextView dayTv = new TextView(ctx);
        dayTv.setText(String.valueOf(day));
        dayTv.setGravity(Gravity.CENTER);
        dayTv.setTextSize(compact ? 13 : 15);
        if (isSelected) {
            dayTv.setTextColor(Color.WHITE);
        } else {
            dayTv.setTextColor(isToday ? Color.parseColor("#1565C0") : Color.BLACK);
        }
        cell.addView(dayTv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (!compact) {
            TextView amountTv = new TextView(ctx);
            if (expense != null && expense > 0) {
                amountTv.setText(String.format("%.0f", expense));
                amountTv.setTextSize(10);
                amountTv.setTextColor(Color.parseColor("#E5595A"));
                amountTv.setGravity(Gravity.CENTER);
            }
            cell.addView(amountTv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        return cell;
    }
}

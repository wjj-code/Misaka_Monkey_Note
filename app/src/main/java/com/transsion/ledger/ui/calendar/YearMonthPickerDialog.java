package com.transsion.ledger.ui.calendar;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import androidx.appcompat.app.AlertDialog;

import java.util.Calendar;
import java.util.Locale;

/** 年月快捷选择弹窗（日历页 / 记账页共用） */
public final class YearMonthPickerDialog {

    public interface Callback {
        void onPicked(String yearMonth); // yyyy-MM
    }

    private YearMonthPickerDialog() {}

    public static void show(Context context, String currentYearMonth, Callback callback) {
        Calendar cal = Calendar.getInstance();
        if (currentYearMonth != null) {
            String[] parts = currentYearMonth.split("-");
            cal.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, 1);
        }

        NumberPicker yearPicker = new NumberPicker(context);
        NumberPicker monthPicker = new NumberPicker(context);

        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(thisYear - 10);
        yearPicker.setMaxValue(thisYear + 5);
        yearPicker.setValue(cal.get(Calendar.YEAR));
        yearPicker.setWrapSelectorWheel(false);

        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(cal.get(Calendar.MONTH) + 1);
        monthPicker.setDisplayedValues(new String[]{
                "1月", "2月", "3月", "4月", "5月", "6月",
                "7月", "8月", "9月", "10月", "11月", "12月"
        });
        monthPicker.setWrapSelectorWheel(false);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(32, 16, 32, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        layout.addView(yearPicker, lp);
        layout.addView(monthPicker, lp);

        new AlertDialog.Builder(context)
                .setTitle("选择年月")
                .setView(layout)
                .setPositiveButton("确定", (d, w) -> {
                    if (callback != null) {
                        callback.onPicked(String.format(Locale.getDefault(), "%d-%02d",
                                yearPicker.getValue(), monthPicker.getValue()));
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}

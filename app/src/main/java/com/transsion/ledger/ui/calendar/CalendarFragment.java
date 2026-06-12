package com.transsion.ledger.ui.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.transsion.ledger.R;
import com.transsion.ledger.data.entity.DailySummary;
import com.transsion.ledger.data.entity.Transaction;
import com.transsion.ledger.ui.bills.BillAdapter;
import com.transsion.ledger.viewmodel.TransactionViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarFragment extends Fragment {

    private TransactionViewModel viewModel;
    private MonthCalendarView calendarView;
    private RecyclerView recyclerDaily;
    private TextView textEmpty;
    private BillAdapter adapter;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat yearMonthFmt = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

    private String selectedDate;
    private LiveData<List<DailySummary>> monthLiveData;
    private LiveData<List<Transaction>> dayLiveData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerDaily = view.findViewById(R.id.recycler_daily);
        textEmpty = view.findViewById(R.id.text_empty);
        recyclerDaily.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BillAdapter();
        adapter.setDisplayMode(false, false);
        recyclerDaily.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        View calendarRoot = view.findViewById(R.id.calendar_view_container);
        calendarView = new MonthCalendarView(calendarRoot);
        calendarView.setOnDayClickListener(this::onDayClicked);
        calendarView.setOnMonthChangedListener(this::onMonthChanged);
        calendarView.setOnMonthTitleClickListener(this::showMonthPickerDialog);

        selectedDate = dateFmt.format(new Date());
        loadMonth(calendarView.getCurrentYearMonth());
    }

    private void onMonthChanged(String yearMonth) {
        loadMonth(yearMonth);
    }

    private void loadMonth(String yearMonth) {
        if (monthLiveData != null) {
            monthLiveData.removeObservers(getViewLifecycleOwner());
        }
        monthLiveData = viewModel.getDailySummaryByMonth(yearMonth);
        monthLiveData.observe(getViewLifecycleOwner(), summaries -> {
            Map<String, Double> map = new HashMap<>();
            if (summaries != null) {
                for (DailySummary ds : summaries) {
                    map.put(ds.date, ds.expense);
                }
            }
            calendarView.setDailyExpenses(map);

            String dateToSelect = resolveAutoSelectDate(yearMonth, summaries);
            if (dateToSelect != null) {
                loadDay(dateToSelect);
            } else {
                selectedDate = null;
                calendarView.setSelectedDate(null);
                showEmpty(yearMonth + " 本月暂无记账");
            }
        });
    }

    /**
     * 当月 → 始终选中今天（修复切回当月仍停留在旧日期的问题）。
     * 非当月 → 选中该月最早有记账的日期。
     */
    private String resolveAutoSelectDate(String yearMonth, List<DailySummary> summaries) {
        if (isCurrentMonth(yearMonth)) {
            return dateFmt.format(new Date());
        }
        if (summaries == null || summaries.isEmpty()) return null;
        for (DailySummary ds : summaries) {
            if (ds.date != null && (ds.income > 0 || ds.expense > 0)) {
                return ds.date;
            }
        }
        return null;
    }

    private boolean isCurrentMonth(String yearMonth) {
        return yearMonth != null && yearMonth.equals(yearMonthFmt.format(new Date()));
    }

    private void onDayClicked(String date) {
        if (date == null) return;
        loadDay(date);
    }

    private void loadDay(String date) {
        selectedDate = date;
        calendarView.setSelectedDate(date);

        if (dayLiveData != null) {
            dayLiveData.removeObservers(getViewLifecycleOwner());
        }
        dayLiveData = viewModel.getByDate(date);
        dayLiveData.observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null && !transactions.isEmpty()) {
                adapter.setTransactions(transactions);
                recyclerDaily.setVisibility(View.VISIBLE);
                textEmpty.setVisibility(View.GONE);
            } else {
                adapter.setTransactions(null);
                recyclerDaily.setVisibility(View.GONE);
                showEmpty(date + " 无记录");
            }
        });
    }

    private void showMonthPickerDialog(String currentYearMonth) {
        YearMonthPickerDialog.show(requireContext(), currentYearMonth,
                calendarView::jumpToYearMonth);
    }

    private void showEmpty(String message) {
        recyclerDaily.setVisibility(View.GONE);
        textEmpty.setText(message);
        textEmpty.setVisibility(View.VISIBLE);
    }
}

package com.transsion.ledger.ui.bills;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.transsion.ledger.R;
import com.transsion.ledger.data.entity.MonthSummary;
import com.transsion.ledger.viewmodel.BudgetViewModel;
import com.transsion.ledger.viewmodel.TransactionViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BillsFragment extends Fragment {

    private TransactionViewModel transactionVM;
    private BudgetViewModel budgetVM;
    private RecyclerView recyclerView;
    private BillAdapter adapter;
    private TextView txtBudgetRemaining, txtHeaderIncome, txtHeaderExpense, txtMonthLabel;

    private final SimpleDateFormat yearMonthFmt = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
    private final SimpleDateFormat monthLabelFmt = new SimpleDateFormat("yyyy年M月", Locale.getDefault());

    /** 顶部汇总当前绑定的月份（随列表滚动更新） */
    private String headerYearMonth;
    private String summaryObservedMonth;
    private final Observer<MonthSummary> monthSummaryObserver = summary -> {
        if (summary != null) {
            txtHeaderIncome.setText(String.format("%.2f", summary.totalIncome));
            txtHeaderExpense.setText(String.format("%.2f", summary.totalExpense));
            budgetVM.applyMonthSummary(summary);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bills, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        headerYearMonth = yearMonthFmt.format(new Date());

        transactionVM = new ViewModelProvider(this).get(TransactionViewModel.class);
        budgetVM = new ViewModelProvider(this).get(BudgetViewModel.class);

        txtBudgetRemaining = view.findViewById(R.id.txt_budget_remaining);
        txtHeaderIncome    = view.findViewById(R.id.txt_header_income);
        txtHeaderExpense   = view.findViewById(R.id.txt_header_expense);
        txtMonthLabel      = view.findViewById(R.id.txt_month_label);

        budgetVM.getBudgetRemaining().observe(getViewLifecycleOwner(), v ->
                txtBudgetRemaining.setText(String.format("剩余 ¥ %.2f", v)));

        txtBudgetRemaining.setOnClickListener(v -> showBudgetDialog());

        view.findViewById(R.id.btn_goto_charts).setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.nav_statistics));

        view.findViewById(R.id.btn_goto_calendar).setOnClickListener(v -> {
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                    requireActivity().findViewById(R.id.bottom_nav);
            if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_calendar);
        });

        recyclerView = view.findViewById(R.id.recycler_bills);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);

        adapter = new BillAdapter();
        adapter.setShowMonthSections(true);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                syncHeaderToVisibleMonth(layoutManager);
            }
        });

        ItemTouchHelper helper = new ItemTouchHelper(adapter.getSwipeCallback());
        helper.attachToRecyclerView(recyclerView);

        adapter.setOnItemTapListener(transaction -> {
            Bundle args = new Bundle();
            args.putLong("transactionId", transaction.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.nav_transaction_edit, args);
        });

        adapter.setOnDeleteListener((transaction, position) -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("确认删除")
                    .setMessage("删除「" + transaction.getCategory2() + "」这笔记录？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        transactionVM.delete(transaction);
                        adapter.removeItemAt(position);
                    })
                    .setNegativeButton("取消", (dialog, which) -> adapter.notifyItemChanged(position))
                    .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                    .show();
        });

        transactionVM.getAll().observe(getViewLifecycleOwner(), transactions -> {
            adapter.setTransactions(transactions);
            recyclerView.post(() -> syncHeaderToVisibleMonth(layoutManager));
        });

        txtMonthLabel.setText(formatMonthLabel(headerYearMonth));
        budgetVM.loadBudget(headerYearMonth);
        summaryObservedMonth = headerYearMonth;
        transactionVM.getMonthSummary(headerYearMonth)
                .observe(getViewLifecycleOwner(), monthSummaryObserver);
    }

    /** 仅当可见月份变化时切换预算/汇总数据源，避免滚动时重复订阅 */
    private void syncHeaderToVisibleMonth(LinearLayoutManager lm) {
        if (lm == null) return;
        int first = lm.findFirstVisibleItemPosition();
        String ym = adapter.getYearMonthForPosition(first);
        if (ym == null) ym = headerYearMonth;
        bindHeaderMonth(ym);
    }

    private void bindHeaderMonth(String yearMonth) {
        if (yearMonth == null || yearMonth.equals(headerYearMonth)) return;
        headerYearMonth = yearMonth;
        txtMonthLabel.setText(formatMonthLabel(yearMonth));

        budgetVM.loadBudget(yearMonth);

        if (summaryObservedMonth != null) {
            transactionVM.getMonthSummary(summaryObservedMonth)
                    .removeObserver(monthSummaryObserver);
        }
        summaryObservedMonth = yearMonth;
        transactionVM.getMonthSummary(yearMonth)
                .observe(getViewLifecycleOwner(), monthSummaryObserver);
    }

    private String formatMonthLabel(String yearMonth) {
        try {
            Date d = yearMonthFmt.parse(yearMonth);
            if (d != null) return monthLabelFmt.format(d);
        } catch (ParseException ignored) {}
        return yearMonth;
    }

    private void showBudgetDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("输入月预算金额");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setSingleLine();

        Double current = budgetVM.getBudgetAmount().getValue();
        if (current != null && current > 0) {
            input.setText(formatAmount(current));
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("设置预算（" + formatMonthLabel(headerYearMonth) + "）")
                .setView(input)
                .setPositiveButton("确定", (d, w) -> {
                    try {
                        double val = Double.parseDouble(input.getText().toString().trim());
                        if (val >= 0) {
                            budgetVM.setBudget(headerYearMonth, val);
                        }
                    } catch (NumberFormatException ignored) {}
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private String formatAmount(double v) {
        if (v == (long) v) return String.valueOf((long) v);
        return String.valueOf(v);
    }
}

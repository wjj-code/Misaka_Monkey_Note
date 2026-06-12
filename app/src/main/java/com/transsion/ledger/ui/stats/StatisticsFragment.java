package com.transsion.ledger.ui.stats;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.transsion.ledger.R;
import com.transsion.ledger.data.entity.Transaction;
import com.transsion.ledger.viewmodel.TransactionViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StatisticsFragment extends Fragment {

    private TransactionViewModel viewModel;
    private List<Transaction> allInRange = new ArrayList<>();
    private List<StatNode> breakdownRoots = new ArrayList<>();

    private int currentType = 0;
    private StatisticsHelper.TimeRange pieRange = StatisticsHelper.TimeRange.MONTH;
    private StatisticsHelper.TimeRange lineRange = StatisticsHelper.TimeRange.HALF_YEAR;

    private PieChartView pieChart;
    private LineChartView lineChart;
    private TextView txtPieTotal;
    private LinearLayout layoutBreakdown;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        pieChart = view.findViewById(R.id.pie_chart);
        lineChart = view.findViewById(R.id.line_chart);
        txtPieTotal = view.findViewById(R.id.txt_pie_total);
        layoutBreakdown = view.findViewById(R.id.layout_breakdown);

        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> Navigation.findNavController(requireView()).navigateUp());

        MaterialButtonToggleGroup toggleType = view.findViewById(R.id.toggle_type);
        MaterialButtonToggleGroup togglePieRange = view.findViewById(R.id.toggle_pie_range);
        MaterialButtonToggleGroup toggleLineRange = view.findViewById(R.id.toggle_line_range);

        toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            currentType = checkedId == R.id.btn_type_income ? 1 : 0;
            refreshCharts();
        });

        togglePieRange.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            pieRange = pieRangeFromId(checkedId);
            refreshCharts();
        });

        toggleLineRange.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            lineRange = pieRangeFromId(checkedId);
            refreshLineChart();
        });

        long start = StatisticsHelper.rangeStartMillis(StatisticsHelper.TimeRange.YEAR);
        long end = StatisticsHelper.rangeEndMillis();
        viewModel.getByDateRange(start, end).observe(getViewLifecycleOwner(), list -> {
            allInRange = list != null ? list : new ArrayList<>();
            refreshCharts();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        View bottomNav = requireActivity().findViewById(R.id.bottom_nav);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);
    }

    @Override
    public void onPause() {
        View bottomNav = requireActivity().findViewById(R.id.bottom_nav);
        if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
        super.onPause();
    }

    private StatisticsHelper.TimeRange pieRangeFromId(int id) {
        if (id == R.id.btn_pie_half || id == R.id.btn_line_half) return StatisticsHelper.TimeRange.HALF_YEAR;
        if (id == R.id.btn_pie_year || id == R.id.btn_line_year) return StatisticsHelper.TimeRange.YEAR;
        return StatisticsHelper.TimeRange.MONTH;
    }

    private void refreshCharts() {
        long pieStart = StatisticsHelper.rangeStartMillis(pieRange);
        long pieEnd = StatisticsHelper.rangeEndMillis();

        breakdownRoots = StatisticsHelper.buildBreakdownTree(allInRange, currentType, pieStart, pieEnd);
        pieChart.setData(breakdownRoots);

        double total = StatisticsHelper.sumInRange(allInRange, currentType, pieStart, pieEnd);
        String typeLabel = currentType == 0 ? "支出" : "收入";
        String rangeLabel = rangeLabel(pieRange);
        txtPieTotal.setText(String.format(Locale.getDefault(),
                "%s · %s  合计 ¥ %.2f", typeLabel, rangeLabel, total));

        rebuildBreakdown();
        refreshLineChart();
    }

    private void refreshLineChart() {
        int color = currentType == 0
                ? ContextCompat.getColor(requireContext(), R.color.expense)
                : ContextCompat.getColor(requireContext(), R.color.income);
        lineChart.setLineColor(color);
        lineChart.setData(
                StatisticsHelper.buildMonthLabels(lineRange),
                StatisticsHelper.buildMonthlySeries(allInRange, currentType, lineRange)
        );
    }

    private String rangeLabel(StatisticsHelper.TimeRange range) {
        switch (range) {
            case HALF_YEAR: return "近半年";
            case YEAR: return "近1年";
            default: return "当月";
        }
    }

    private void rebuildBreakdown() {
        layoutBreakdown.removeAllViews();
        if (breakdownRoots.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("暂无分类数据");
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint));
            empty.setPadding(0, 16, 0, 16);
            layoutBreakdown.addView(empty);
            return;
        }
        for (StatNode root : breakdownRoots) {
            appendNode(layoutBreakdown, root, 0);
        }
    }

    private void appendNode(LinearLayout parent, StatNode node, int depth) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_stat_breakdown, parent, false);

        int indent = depth * (int) (24 * getResources().getDisplayMetrics().density);
        row.setPadding(indent, row.getPaddingTop(), row.getPaddingRight(), row.getPaddingBottom());

        TextView txtExpand = row.findViewById(R.id.txt_expand);
        View colorDot = row.findViewById(R.id.view_color_dot);
        TextView txtLabel = row.findViewById(R.id.txt_label);
        TextView txtPercent = row.findViewById(R.id.txt_percent);
        TextView txtAmount = row.findViewById(R.id.txt_amount);

        txtLabel.setText(node.label);
        txtPercent.setText(String.format(Locale.getDefault(), "%.0f%%", node.percent));
        txtAmount.setText(String.format(Locale.getDefault(), "¥%.2f", node.amount));

        if (depth == 0 && node.color != 0) {
            colorDot.setVisibility(View.VISIBLE);
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(node.color);
            colorDot.setBackground(dot);
        } else {
            colorDot.setVisibility(View.INVISIBLE);
        }

        if (node.hasChildren()) {
            txtExpand.setText(node.expanded ? "▼" : "▶");
            row.setOnClickListener(v -> {
                node.expanded = !node.expanded;
                rebuildBreakdown();
            });
        } else {
            txtExpand.setText("");
            row.setClickable(false);
        }

        parent.addView(row);
        if (node.expanded) {
            for (StatNode child : node.children) {
                appendNode(parent, child, depth + 1);
            }
        }
    }
}

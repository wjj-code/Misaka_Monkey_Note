package com.transsion.ledger.ui.profile;

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

import com.transsion.ledger.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 预算设置入口
        TextView budgetBtn = view.findViewById(R.id.btn_budget_setting);
        budgetBtn.setOnClickListener(v -> showBudgetDialog());
    }

    private void showBudgetDialog() {
        // 读取当前预算
        String yearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        android.content.SharedPreferences prefs = requireContext()
                .getSharedPreferences("budget_prefs", android.content.Context.MODE_PRIVATE);
        String saved = prefs.getString("budget_" + yearMonth, "0");

        EditText input = new EditText(requireContext());
        input.setHint("输入月预算金额");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setSingleLine();
        if (!"0".equals(saved)) {
            input.setText(formatAmount(Double.parseDouble(saved)));
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("设置预算（" + new SimpleDateFormat("yyyy年M月", Locale.getDefault()).format(new Date()) + "）")
                .setView(input)
                .setPositiveButton("确定", (d, w) -> {
                    try {
                        double val = Double.parseDouble(input.getText().toString().trim());
                        if (val >= 0) {
                            prefs.edit().putString("budget_" + yearMonth, String.valueOf(val)).apply();
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

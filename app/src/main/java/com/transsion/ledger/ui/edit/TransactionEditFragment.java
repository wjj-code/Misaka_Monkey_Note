package com.transsion.ledger.ui.edit;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.transsion.ledger.R;
import com.transsion.ledger.data.entity.Account;
import com.transsion.ledger.data.entity.Transaction;
import com.transsion.ledger.data.repository.AccountRepository;
import com.transsion.ledger.ui.voice.VoiceDraft;
import com.transsion.ledger.util.CategoryCatalog;
import com.transsion.ledger.viewmodel.TransactionViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TransactionEditFragment extends Fragment {

    private TransactionViewModel viewModel;
    private AccountRepository accountRepo;
    private List<Account> accountList = new ArrayList<>();

    private long transactionId = -1;
    private boolean voiceDraftMode;
    private Transaction original;
    private Calendar selectedDate;
    private boolean formReady;
    private String pendingAccountName = "";
    private String pendingCategory2 = "";
    private String customCategory2 = "";

    private MaterialButton btnTypeExpense, btnTypeIncome;
    private TextView txtEditTitle;
    private int currentType = 0;
    private Spinner spinnerCategory1, spinnerCategory2, spinnerAccount;
    private EditText editAmount, editNote;
    private MaterialCardView cardFinancial;
    private RadioGroup radioFinancial;
    private TextView textDate, textTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            transactionId = getArguments().getLong("transactionId", -1);
            voiceDraftMode = getArguments().getBoolean(VoiceDraft.KEY_VOICE_DRAFT, false);
        }

        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        accountRepo = new AccountRepository(requireActivity().getApplication());
        selectedDate = Calendar.getInstance();

        btnTypeExpense = view.findViewById(R.id.btn_type_expense);
        btnTypeIncome = view.findViewById(R.id.btn_type_income);
        spinnerCategory1 = view.findViewById(R.id.spinner_category1);
        spinnerCategory2 = view.findViewById(R.id.spinner_category2);
        spinnerAccount = view.findViewById(R.id.spinner_account);
        editAmount = view.findViewById(R.id.edit_amount);
        editNote = view.findViewById(R.id.edit_note);
        cardFinancial = view.findViewById(R.id.card_financial);
        radioFinancial = view.findViewById(R.id.radio_financial);
        textDate = view.findViewById(R.id.text_date);
        textTime = view.findViewById(R.id.text_time);

        txtEditTitle = view.findViewById(R.id.txt_edit_title);
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        MaterialButton btnSave = view.findViewById(R.id.btn_save);
        MaterialButton btnDelete = view.findViewById(R.id.btn_delete);

        btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());
        btnSave.setOnClickListener(v -> saveTransaction());
        btnDelete.setOnClickListener(v -> confirmDelete());

        btnTypeExpense.setOnClickListener(v -> selectType(0));
        btnTypeIncome.setOnClickListener(v -> selectType(1));

        textDate.setOnClickListener(v -> showDatePicker());
        textTime.setOnClickListener(v -> showTimePicker());

        spinnerCategory1.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view1, int position, long id) {
                if (!formReady) return;
                refreshCategory2Spinner(getSelectedCategory2Value());
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerCategory2.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view12, int position, long id) {
                if (!formReady) return;
                Object item = parent.getItemAtPosition(position);
                if (item instanceof String && "自定义".equals(item)) {
                    showCustomCategory2Dialog();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        loadAccounts();

        if (voiceDraftMode) {
            txtEditTitle.setText("补全账单");
            btnSave.setText("保存");
            btnDelete.setVisibility(View.GONE);
            VoiceDraft draft = VoiceDraft.fromBundle(getArguments());
            bindDraft(draft);
        } else if (transactionId > 0) {
            viewModel.getById(transactionId).observe(getViewLifecycleOwner(), t -> {
                if (t != null && original == null) {
                    original = t;
                    bindForm(t);
                }
            });
        }
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

    private void loadAccounts() {
        accountRepo.getAll().observe(getViewLifecycleOwner(), accounts -> {
            accountList = accounts != null ? accounts : new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (Account a : accountList) names.add(a.getName());
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerAccount.setAdapter(adapter);
            if (original != null) {
                selectAccount(original.getAccountId());
            } else if (!pendingAccountName.isEmpty()) {
                selectAccountByName(pendingAccountName);
            }
        });
    }

    private void bindDraft(VoiceDraft draft) {
        formReady = false;
        selectedDate.setTimeInMillis(draft.dateTime);
        pendingCategory2 = draft.category2 != null ? draft.category2 : "";
        customCategory2 = "";

        if (draft.type >= 0) {
            selectType(draft.type);
        } else {
            selectType(0);
        }
        refreshCategory1Spinner(currentType);
        if (draft.category1 != null) {
            selectCategory1(draft.category1);
        }
        refreshCategory2Spinner(pendingCategory2);
        selectCategory2(pendingCategory2);

        if (draft.amount > 0) {
            editAmount.setText(formatAmount(draft.amount));
        }
        editNote.setText(draft.note != null ? draft.note : "");

        cardFinancial.setVisibility(currentType == 0 ? View.VISIBLE : View.GONE);
        if (draft.category3 >= 0 && draft.category3 <= 3) {
            int[] ids = {R.id.radio_maintain, R.id.radio_consume, R.id.radio_improve, R.id.radio_social};
            radioFinancial.check(ids[draft.category3]);
        } else {
            radioFinancial.clearCheck();
        }

        pendingAccountName = draft.accountName != null ? draft.accountName : "";
        updateDateTimeDisplay();
        formReady = true;
    }

    private void bindForm(Transaction t) {
        formReady = false;
        selectedDate.setTimeInMillis(t.getDateTime());
        pendingCategory2 = t.getCategory2() != null ? t.getCategory2() : "";
        customCategory2 = "";

        selectType(t.getType());
        refreshCategory1Spinner(t.getType());
        selectCategory1(t.getCategory1());
        refreshCategory2Spinner(pendingCategory2);
        selectCategory2(pendingCategory2);

        editAmount.setText(formatAmount(t.getAmount()));
        editNote.setText(t.getNote() != null ? t.getNote() : "");

        cardFinancial.setVisibility(t.getType() == 0 ? View.VISIBLE : View.GONE);
        if (t.getType() == 0 && t.getCategory3() >= 0 && t.getCategory3() <= 3) {
            int[] ids = {R.id.radio_maintain, R.id.radio_consume, R.id.radio_improve, R.id.radio_social};
            radioFinancial.check(ids[t.getCategory3()]);
        } else {
            radioFinancial.clearCheck();
        }

        updateDateTimeDisplay();
        selectAccount(t.getAccountId());
        formReady = true;
    }

    private void selectType(int type) {
        currentType = type;
        if (type == 0) {
            btnTypeExpense.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5595A")));
            btnTypeExpense.setTextColor(Color.WHITE);
            btnTypeIncome.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnTypeIncome.setTextColor(Color.parseColor("#6B7280"));
        } else {
            btnTypeIncome.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2EAC68")));
            btnTypeIncome.setTextColor(Color.WHITE);
            btnTypeExpense.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnTypeExpense.setTextColor(Color.parseColor("#6B7280"));
        }
        if (formReady) {
            refreshCategory1Spinner(type);
            refreshCategory2Spinner("");
            cardFinancial.setVisibility(type == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void refreshCategory1Spinner(int type) {
        String[] labels = CategoryCatalog.cat1ForType(type);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory1.setAdapter(adapter);
    }

    private void refreshCategory2Spinner(String currentValue) {
        String category1 = (String) spinnerCategory1.getSelectedItem();
        List<String> options = CategoryCatalog.subsOptionsFor(category1, currentType, currentValue);
        if (!customCategory2.isEmpty() && !options.contains(customCategory2)) {
            options = new ArrayList<>(options);
            options.add(customCategory2);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory2.setAdapter(adapter);
    }

    private void selectCategory1(String category1) {
        int idx = CategoryCatalog.indexOfCategory1(category1, currentType);
        if (idx >= 0) spinnerCategory1.setSelection(idx);
    }

    private void selectCategory2(String category2) {
        if (category2 == null || category2.isEmpty()) return;
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinnerCategory2.getAdapter();
        if (adapter == null) return;
        int idx = CategoryCatalog.indexOfCategory2(category2, castAdapterItems(adapter));
        if (idx >= 0) spinnerCategory2.setSelection(idx);
    }

    private List<String> castAdapterItems(ArrayAdapter<?> adapter) {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < adapter.getCount(); i++) {
            Object o = adapter.getItem(i);
            if (o != null) items.add(o.toString());
        }
        return items;
    }

    private String getSelectedCategory2Value() {
        Object item = spinnerCategory2.getSelectedItem();
        if (item == null) return "";
        String value = item.toString().trim();
        if ("自定义".equals(value)) {
            return customCategory2;
        }
        return value;
    }

    private void showCustomCategory2Dialog() {
        EditText input = new EditText(requireContext());
        input.setHint("输入子项目名称");
        input.setSingleLine();
        if (!customCategory2.isEmpty()) input.setText(customCategory2);
        new AlertDialog.Builder(requireContext())
                .setTitle("自定义子项目")
                .setView(input)
                .setPositiveButton("确定", (d, w) -> {
                    String text = input.getText().toString().trim();
                    if (text.isEmpty()) {
                        Toast.makeText(requireContext(), "请输入子项目名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    customCategory2 = text;
                    refreshCategory2Spinner(text);
                    selectCategory2(text);
                })
                .setNegativeButton("取消", (d, w) -> {
                    if (!customCategory2.isEmpty()) {
                        selectCategory2(customCategory2);
                    } else if (spinnerCategory2.getAdapter() != null
                            && spinnerCategory2.getAdapter().getCount() > 0) {
                        spinnerCategory2.setSelection(0);
                    }
                })
                .show();
    }

    private void selectAccount(long accountId) {
        for (int i = 0; i < accountList.size(); i++) {
            if (accountList.get(i).getId() == accountId) {
                spinnerAccount.setSelection(i);
                return;
            }
        }
    }

    private void selectAccountByName(String name) {
        if (name == null || name.isEmpty()) return;
        for (int i = 0; i < accountList.size(); i++) {
            String n = accountList.get(i).getName();
            if (n != null && n.contains(name)) {
                spinnerAccount.setSelection(i);
                return;
            }
        }
    }

    private void updateDateTimeDisplay() {
        textDate.setText(String.format(Locale.getDefault(), "%d年%02d月%02d日",
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.DAY_OF_MONTH)));
        textTime.setText(String.format(Locale.getDefault(), "%02d时%02d分",
                selectedDate.get(Calendar.HOUR_OF_DAY),
                selectedDate.get(Calendar.MINUTE)));
    }

    private void showDatePicker() {
        new DatePickerDialog(requireContext(), (view, y, m, d) -> {
            selectedDate.set(Calendar.YEAR, y);
            selectedDate.set(Calendar.MONTH, m);
            selectedDate.set(Calendar.DAY_OF_MONTH, d);
            updateDateTimeDisplay();
        }, selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(requireContext(), (view, h, min) -> {
            selectedDate.set(Calendar.HOUR_OF_DAY, h);
            selectedDate.set(Calendar.MINUTE, min);
            selectedDate.set(Calendar.SECOND, 0);
            updateDateTimeDisplay();
        }, selectedDate.get(Calendar.HOUR_OF_DAY),
                selectedDate.get(Calendar.MINUTE), true).show();
    }

    private void saveTransaction() {
        if (!voiceDraftMode && original == null) return;

        int type = currentType;
        String category1 = (String) spinnerCategory1.getSelectedItem();
        String category2 = getSelectedCategory2Value();
        String amountStr = editAmount.getText().toString().trim();

        if (category1 == null || category1.isEmpty()) {
            Toast.makeText(requireContext(), "请选择一级分类 *", Toast.LENGTH_SHORT).show();
            return;
        }
        if (category2.isEmpty() || "自定义".equals(category2)) {
            Toast.makeText(requireContext(), "请选择子项目（细则）*", Toast.LENGTH_SHORT).show();
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "请填写有效金额 *", Toast.LENGTH_SHORT).show();
            return;
        }

        int category3 = -1;
        if (type == 0) {
            int checked = radioFinancial.getCheckedRadioButtonId();
            if (checked == R.id.radio_maintain) category3 = 0;
            else if (checked == R.id.radio_consume) category3 = 1;
            else if (checked == R.id.radio_improve) category3 = 2;
            else if (checked == R.id.radio_social) category3 = 3;
            else {
                Toast.makeText(requireContext(), "请选择财务分类 *", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        long accountId = 0;
        int accPos = spinnerAccount.getSelectedItemPosition();
        if (accPos >= 0 && accPos < accountList.size()) {
            accountId = accountList.get(accPos).getId();
        }

        Transaction updated = new Transaction(
                type, amount, category1, category2, category3,
                selectedDate.getTimeInMillis(),
                editNote.getText().toString().trim(),
                accountId
        );

        if (voiceDraftMode) {
            viewModel.insert(updated, () -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "已保存", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                });
            });
            return;
        }

        updated.setId(original.getId());
        viewModel.update(updated);
        Toast.makeText(requireContext(), "已保存", Toast.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).navigateUp();
    }

    private void confirmDelete() {
        if (original == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("确认删除")
                .setMessage("删除「" + original.getCategory2() + "」这笔记录？")
                .setPositiveButton("删除", (d, w) -> {
                    viewModel.delete(original);
                    Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private String formatAmount(double v) {
        if (v == (long) v) return String.valueOf((long) v);
        return String.valueOf(v);
    }
}

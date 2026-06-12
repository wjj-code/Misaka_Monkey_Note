package com.transsion.ledger.ui.accounts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.transsion.ledger.R;
import com.transsion.ledger.data.entity.Account;
import com.transsion.ledger.viewmodel.AccountViewModel;

import java.util.ArrayList;
import java.util.List;

public class AccountsFragment extends Fragment {

    private static final String TYPE_CURRENT = "活期";
    private static final String TYPE_ASSET = "资产";
    private static final String FILTER_ALL = "全部";

    private AccountViewModel viewModel;
    private RecyclerView recycler;
    private AccountAdapter adapter;
    private MaterialButtonToggleGroup toggleFilter;
    private List<Account> allAccounts = new ArrayList<>();
    private String currentFilter = TYPE_CURRENT;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_accounts, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b);
        recycler = v.findViewById(R.id.recycler_accounts);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AccountAdapter();
        recycler.setAdapter(adapter);

        toggleFilter = v.findViewById(R.id.toggle_account_filter);
        toggleFilter.check(R.id.btn_filter_current);
        toggleFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btn_filter_current) currentFilter = TYPE_CURRENT;
            else if (checkedId == R.id.btn_filter_asset) currentFilter = TYPE_ASSET;
            else if (checkedId == R.id.btn_filter_all) currentFilter = FILTER_ALL;
            applyFilter();
        });

        viewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        viewModel.getAll().observe(getViewLifecycleOwner(), accounts -> {
            allAccounts = accounts != null ? accounts : new ArrayList<>();
            applyFilter();
        });

        v.findViewById(R.id.btn_add_account).setOnClickListener(x -> showEditDialog(null));
    }

    private void applyFilter() {
        List<Account> filtered = new ArrayList<>();
        for (Account a : allAccounts) {
            if (FILTER_ALL.equals(currentFilter)) {
                filtered.add(a);
            } else if (currentFilter.equals(a.getType())) {
                filtered.add(a);
            }
        }
        adapter.setAccounts(filtered);
    }

    private void switchFilterToType(String accountType) {
        if (TYPE_ASSET.equals(accountType)) {
            currentFilter = TYPE_ASSET;
            toggleFilter.check(R.id.btn_filter_asset);
        } else {
            currentFilter = TYPE_CURRENT;
            toggleFilter.check(R.id.btn_filter_current);
        }
        applyFilter();
    }

    private String defaultTypeForNewAccount() {
        if (TYPE_ASSET.equals(currentFilter)) return TYPE_ASSET;
        return TYPE_CURRENT;
    }

    // ---- Adapter ----
    private class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.VH> {
        private List<Account> accounts = new ArrayList<>();

        void setAccounts(List<Account> list) {
            accounts = list != null ? list : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_account, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Account a = accounts.get(pos);
            h.name.setText(a.getName());
            h.balance.setText("¥ " + String.format("%.2f", a.getBalance()));
            h.type.setText(a.getType());
            h.note.setText(a.getNote());
            h.badge.setVisibility(a.isDefault() ? View.VISIBLE : View.GONE);
            h.itemView.setOnClickListener(v -> showEditDialog(a));
            h.itemView.setOnLongClickListener(v -> {
                showLongPressMenu(a); return true;
            });
        }
        @Override public int getItemCount() { return accounts.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView name, balance, type, note, badge;
            VH(View v) { super(v);
                name = v.findViewById(R.id.txt_account_name);
                balance = v.findViewById(R.id.txt_account_balance);
                type = v.findViewById(R.id.txt_account_type);
                note = v.findViewById(R.id.txt_account_note);
                badge = v.findViewById(R.id.txt_default_badge);
            }
        }
    }

    private void showLongPressMenu(Account a) {
        new AlertDialog.Builder(requireContext())
                .setTitle(a.getName())
                .setItems(new String[]{"设为默认", "编辑", "删除"}, (d, w) -> {
                    if (w == 0) {
                        viewModel.setAsDefault(a.getId());
                        Toast.makeText(requireContext(),
                                "已将「" + a.getName() + "」设为默认账户", Toast.LENGTH_SHORT).show();
                    }
                    else if (w == 1) showEditDialog(a);
                    else if (w == 2) {
                        if (a.isDefault()) {
                            Toast.makeText(requireContext(), "默认账户不可删除", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        new AlertDialog.Builder(requireContext())
                                .setTitle("确认删除")
                                .setMessage("删除「" + a.getName() + "」？\n余额将并入默认账户，关联账单也会改挂默认账户。")
                                .setPositiveButton("删除", (dd, ww) -> viewModel.delete(a, ok -> {
                                    if (!isAdded()) return;
                                    requireActivity().runOnUiThread(() -> {
                                        if (ok) {
                                            Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }))
                                .setNegativeButton("取消", null).show();
                    }
                }).show();
    }

    private void showEditDialog(@Nullable Account existing) {
        boolean isEdit = existing != null;
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 20, 40, 20);

        EditText nameEt = new EditText(requireContext()); nameEt.setHint("账户名称*");
        EditText balanceEt = new EditText(requireContext()); balanceEt.setHint("余额*");
        Spinner typeSp = new Spinner(requireContext());
        typeSp.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item,
                new String[]{TYPE_CURRENT, TYPE_ASSET}));
        EditText cardEt = new EditText(requireContext()); cardEt.setHint("卡号（选填）");
        EditText noteEt = new EditText(requireContext()); noteEt.setHint("备注（选填）");

        root.addView(label("账户名称"));
        root.addView(nameEt);
        root.addView(label("余额"));
        root.addView(balanceEt);
        root.addView(label("类型"));
        root.addView(typeSp);
        root.addView(label("卡号"));
        root.addView(cardEt);
        root.addView(label("备注"));
        root.addView(noteEt);

        CheckBox cbInWorth = new CheckBox(requireContext()); cbInWorth.setText("计入净资产");
        CheckBox cbExpense = new CheckBox(requireContext()); cbExpense.setText("可支出");
        CheckBox cbIncome = new CheckBox(requireContext()); cbIncome.setText("可收入");
        CheckBox cbTransIn = new CheckBox(requireContext()); cbTransIn.setText("可转入");
        CheckBox cbTransOut = new CheckBox(requireContext()); cbTransOut.setText("可转出");
        CheckBox cbActive = new CheckBox(requireContext()); cbActive.setText("启用");
        CheckBox cbDefault = new CheckBox(requireContext()); cbDefault.setText("设为默认账户");

        cbInWorth.setChecked(true); cbExpense.setChecked(true); cbIncome.setChecked(true);
        cbTransIn.setChecked(true); cbTransOut.setChecked(true); cbActive.setChecked(true);

        root.addView(cbInWorth); root.addView(cbExpense); root.addView(cbIncome);
        root.addView(cbTransIn); root.addView(cbTransOut); root.addView(cbActive);
        root.addView(cbDefault);

        if (isEdit) {
            nameEt.setText(existing.getName());
            balanceEt.setText(format(existing.getBalance()));
            typeSp.setSelection(TYPE_ASSET.equals(existing.getType()) ? 1 : 0);
            cardEt.setText(existing.getCardNumber() != null ? existing.getCardNumber() : "");
            noteEt.setText(existing.getNote() != null ? existing.getNote() : "");
            cbInWorth.setChecked(existing.isIncludeInNetWorth());
            cbExpense.setChecked(existing.isCanExpense());
            cbIncome.setChecked(existing.isCanIncome());
            cbTransIn.setChecked(existing.isCanTransferIn());
            cbTransOut.setChecked(existing.isCanTransferOut());
            cbActive.setChecked(existing.isActive());
            cbDefault.setChecked(existing.isDefault());
            if (existing.isDefault()) {
                cbDefault.setOnCheckedChangeListener((btn, checked) -> {
                    if (!checked) {
                        cbDefault.setChecked(true);
                        Toast.makeText(requireContext(),
                                "请先通过其他账户的「设为默认账户」切换默认", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else {
            cbDefault.setChecked(false);
            typeSp.setSelection(TYPE_ASSET.equals(defaultTypeForNewAccount()) ? 1 : 0);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(isEdit ? "编辑账户" : "添加账户")
                .setView(root)
                .setPositiveButton("保存", (d, w) -> {
                    String name = nameEt.getText().toString().trim();
                    String balStr = balanceEt.getText().toString().trim();
                    if (name.isEmpty() || balStr.isEmpty()) {
                        Toast.makeText(requireContext(), "名称和余额必填", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double bal = Double.parseDouble(balStr);
                    Account a = isEdit ? existing : new Account();
                    String savedType = (String) typeSp.getSelectedItem();
                    a.setName(name);
                    a.setBalance(bal);
                    a.setType(savedType);
                    a.setCardNumber(cardEt.getText().toString().trim());
                    a.setNote(noteEt.getText().toString().trim());
                    a.setIncludeInNetWorth(cbInWorth.isChecked());
                    a.setCanExpense(cbExpense.isChecked());
                    a.setCanIncome(cbIncome.isChecked());
                    a.setCanTransferIn(cbTransIn.isChecked());
                    a.setCanTransferOut(cbTransOut.isChecked());
                    a.setActive(cbActive.isChecked());
                    if (isEdit) {
                        viewModel.update(a);
                        if (cbDefault.isChecked() && !existing.isDefault()) {
                            viewModel.setAsDefault(a.getId());
                        }
                    } else {
                        boolean asDefault = cbDefault.isChecked();
                        viewModel.insert(a, () -> {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> {
                                if (asDefault) viewModel.setAsDefault(a.getId());
                                switchFilterToType(savedType);
                            });
                        });
                    }
                })
                .setNegativeButton("取消", null).show();
    }

    private TextView label(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(0xFF6B7280);
        tv.setPadding(0, 12, 0, 4);
        return tv;
    }

    private String format(double v) {
        return v == (long) v ? String.valueOf((long) v) : String.valueOf(v);
    }
}

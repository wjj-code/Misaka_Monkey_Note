package com.transsion.ledger.ui.bills;

import android.graphics.Canvas;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.transsion.ledger.R;
import com.transsion.ledger.data.entity.Transaction;
import com.transsion.ledger.ui.add.AddTransactionSheet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BillAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_MONTH_HEADER = 0;
    private static final int TYPE_DAY_HEADER = 1;
    private static final int TYPE_ITEM = 2;

    private final List<Item> items = new ArrayList<>();
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateHeaderFmt = new SimpleDateFormat("M月d日 EEEE", Locale.getDefault());
    private final SimpleDateFormat dateGroupFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat yearMonthFmt = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
    private final SimpleDateFormat monthSectionFmt = new SimpleDateFormat("yyyy年M月", Locale.getDefault());
    private final String[] CAT3_LABELS = {"维持类", "消费类", "提升类", "社交类"};
    private boolean withDayHeaders = true;
    private boolean showMonthSections = false;
    private boolean itemClickable = true;
    private OnDeleteListener deleteListener;
    private OnItemTapListener tapListener;

    interface OnDeleteListener {
        void onDelete(Transaction transaction, int position);
    }

    interface OnItemTapListener {
        void onItemTap(Transaction transaction);
    }

    void setOnDeleteListener(OnDeleteListener listener) { this.deleteListener = listener; }
    void setOnItemTapListener(OnItemTapListener listener) { this.tapListener = listener; }

    /** 日历等场景：无日期头、不可点击 */
    public void setDisplayMode(boolean withDayHeaders, boolean itemClickable) {
        this.withDayHeaders = withDayHeaders;
        this.itemClickable = itemClickable;
        this.showMonthSections = false;
    }

    /** 账单页：展示全部月份时插入月份分段头 */
    public void setShowMonthSections(boolean show) {
        this.showMonthSections = show;
    }

    public void setTransactions(List<Transaction> transactions) {
        items.clear();
        if (transactions == null || transactions.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        if (!withDayHeaders) {
            for (Transaction t : transactions) {
                items.add(new Item(t, yearMonthOf(t)));
            }
            notifyDataSetChanged();
            return;
        }

        String currentMonth = "";
        String currentDay = "";
        for (Transaction t : transactions) {
            String ym = yearMonthOf(t);
            String dayKey = dateGroupFmt.format(new Date(t.getDateTime()));

            if (showMonthSections && !ym.equals(currentMonth)) {
                currentMonth = ym;
                currentDay = "";
                double monthIncome = 0, monthExpense = 0;
                for (Transaction tt : transactions) {
                    if (ym.equals(yearMonthOf(tt))) {
                        if (tt.getType() == 1) monthIncome += tt.getAmount();
                        else monthExpense += tt.getAmount();
                    }
                }
                items.add(Item.monthSection(ym, monthSectionFmt.format(new Date(t.getDateTime())),
                        monthIncome, monthExpense));
            }

            if (!dayKey.equals(currentDay)) {
                currentDay = dayKey;
                double dayIncome = 0, dayExpense = 0;
                for (Transaction tt : transactions) {
                    if (dateGroupFmt.format(new Date(tt.getDateTime())).equals(currentDay)) {
                        if (tt.getType() == 1) dayIncome += tt.getAmount();
                        else dayExpense += tt.getAmount();
                    }
                }
                items.add(Item.daySection(dateHeaderFmt.format(t.getDateTime()), ym, dayIncome, dayExpense));
            }
            items.add(new Item(t, ym));
        }
        notifyDataSetChanged();
    }

    /** 根据列表可见位置解析 yyyy-MM，供顶部汇总联动 */
    @Nullable
    public String getYearMonthForPosition(int position) {
        if (position < 0 || position >= items.size()) return null;
        for (int i = position; i >= 0; i--) {
            String ym = items.get(i).yearMonth;
            if (ym != null && !ym.isEmpty()) return ym;
        }
        return null;
    }

    private static String yearMonthOf(Transaction t) {
        return new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(new Date(t.getDateTime()));
    }

    @Override
    public int getItemViewType(int position) {
        Item item = items.get(position);
        if (item.isMonthHeader) return TYPE_MONTH_HEADER;
        if (item.isDayHeader) return TYPE_DAY_HEADER;
        return TYPE_ITEM;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_MONTH_HEADER) {
            return new MonthSectionVH(inflater.inflate(R.layout.item_bill_month_section, parent, false));
        }
        if (viewType == TYPE_DAY_HEADER) {
            return new DayHeaderVH(inflater.inflate(R.layout.item_month_header, parent, false));
        }
        return new ItemVH(inflater.inflate(R.layout.item_transaction, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = items.get(position);
        if (holder instanceof MonthSectionVH) {
            MonthSectionVH h = (MonthSectionVH) holder;
            h.txtMonth.setText(item.headerLabel);
            h.txtIncome.setText("收 " + String.format("%.2f", item.sectionIncome));
            h.txtExpense.setText("支 " + String.format("%.2f", item.sectionExpense));
        } else if (holder instanceof DayHeaderVH) {
            DayHeaderVH h = (DayHeaderVH) holder;
            h.txtMonth.setText(item.headerLabel);
            h.txtIncome.setText("收 " + String.format("%.2f", item.sectionIncome));
            h.txtExpense.setText("支 " + String.format("%.2f", item.sectionExpense));
        } else {
            ItemVH iv = (ItemVH) holder;
            Transaction t = item.transaction;
            Map<String, String> emojiMap = AddTransactionSheet.CAT_EMOJI;
            iv.txtEmoji.setText(emojiMap.getOrDefault(t.getCategory1(), "💰"));
            iv.txtCategory2.setText(t.getCategory2());
            iv.txtTime.setText(timeFmt.format(new Date(t.getDateTime())));
            iv.txtCategory3.setText((t.getType() == 0 && t.getCategory3() >= 0
                    && t.getCategory3() < CAT3_LABELS.length)
                    ? CAT3_LABELS[t.getCategory3()] : "");
            iv.txtCategory3.setVisibility(t.getType() == 0 ? View.VISIBLE : View.GONE);
            double amount = t.getAmount();
            if (t.getType() == 0) {
                iv.txtAmount.setText("-" + String.format("%.2f", amount));
                iv.txtAmount.setTextColor(Color.parseColor("#E5595A"));
            } else {
                iv.txtAmount.setText("+" + String.format("%.2f", amount));
                iv.txtAmount.setTextColor(Color.parseColor("#2EAC68"));
            }
            iv.itemView.setClickable(itemClickable);
            iv.itemView.setFocusable(itemClickable);
            if (itemClickable) {
                iv.itemView.setOnClickListener(v -> {
                    if (tapListener != null) tapListener.onItemTap(t);
                });
            } else {
                iv.itemView.setOnClickListener(null);
            }
        }
    }

    @Override public int getItemCount() { return items.size(); }

    Transaction getTransactionAt(int pos) {
        if (pos < 0 || pos >= items.size()) return null;
        Item item = items.get(pos);
        return item.transaction;
    }

    void removeItemAt(int pos) {
        if (pos >= 0 && pos < items.size() && items.get(pos).transaction != null) {
            items.remove(pos);
            notifyItemRemoved(pos);
        }
    }

    ItemTouchHelper.SimpleCallback getSwipeCallback() {
        return new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                             @NonNull RecyclerView.ViewHolder t) { return false; }
            @Override public int getSwipeDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                return (vh instanceof ItemVH) ? super.getSwipeDirs(rv, vh) : 0;
            }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getAdapterPosition();
                Transaction t = getTransactionAt(pos);
                if (t != null && deleteListener != null) {
                    notifyItemChanged(pos);
                    deleteListener.onDelete(t, pos);
                }
            }
            @Override public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv,
                                              @NonNull RecyclerView.ViewHolder vh, float dX, float dY,
                                              int actionState, boolean active) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    vh.itemView.setAlpha(1 - Math.abs(dX) / vh.itemView.getWidth());
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, active);
            }
        };
    }

    static class MonthSectionVH extends RecyclerView.ViewHolder {
        TextView txtMonth, txtIncome, txtExpense;
        MonthSectionVH(View v) {
            super(v);
            txtMonth = v.findViewById(R.id.txt_section_month);
            txtIncome = v.findViewById(R.id.txt_section_income);
            txtExpense = v.findViewById(R.id.txt_section_expense);
        }
    }

    static class DayHeaderVH extends RecyclerView.ViewHolder {
        TextView txtMonth, txtIncome, txtExpense;
        DayHeaderVH(View v) {
            super(v);
            txtMonth = v.findViewById(R.id.txt_month);
            txtIncome = v.findViewById(R.id.txt_income);
            txtExpense = v.findViewById(R.id.txt_expense);
        }
    }

    static class ItemVH extends RecyclerView.ViewHolder {
        TextView txtEmoji, txtCategory2, txtTime, txtCategory3, txtAmount;
        ItemVH(View v) {
            super(v);
            txtEmoji = v.findViewById(R.id.txt_emoji);
            txtCategory2 = v.findViewById(R.id.txt_category2);
            txtTime = v.findViewById(R.id.txt_time);
            txtCategory3 = v.findViewById(R.id.txt_category3);
            txtAmount = v.findViewById(R.id.txt_amount);
        }
    }

    static class Item {
        boolean isMonthHeader;
        boolean isDayHeader;
        String headerLabel;
        String yearMonth;
        double sectionIncome, sectionExpense;
        Transaction transaction;

        static Item monthSection(String ym, String label, double income, double expense) {
            Item i = new Item();
            i.isMonthHeader = true;
            i.yearMonth = ym;
            i.headerLabel = label;
            i.sectionIncome = income;
            i.sectionExpense = expense;
            return i;
        }

        static Item daySection(String label, String ym, double income, double expense) {
            Item i = new Item();
            i.isDayHeader = true;
            i.yearMonth = ym;
            i.headerLabel = label;
            i.sectionIncome = income;
            i.sectionExpense = expense;
            return i;
        }

        Item(Transaction t, String ym) {
            transaction = t;
            yearMonth = ym;
        }

        private Item() {}
    }
}

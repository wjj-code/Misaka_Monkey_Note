package com.transsion.ledger.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.transsion.ledger.data.BudgetStorage;
import com.transsion.ledger.data.entity.MonthSummary;

public class BudgetViewModel extends AndroidViewModel {

    private final BudgetStorage budgetStorage;
    private final MutableLiveData<Double> budgetRemaining = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> budgetAmount = new MutableLiveData<>(0.0);

    public BudgetViewModel(Application application) {
        super(application);
        budgetStorage = new BudgetStorage(application);
    }

    public LiveData<Double> getBudgetRemaining() { return budgetRemaining; }
    public LiveData<Double> getBudgetAmount() { return budgetAmount; }

    /** 加载预算金额（不含计算，由外部根据月度汇总计算剩余） */
    public void loadBudget(String yearMonth) {
        double budget = budgetStorage.getBudget(yearMonth);
        budgetAmount.postValue(budget);
        budgetRemaining.postValue(budget); // 初始：未扣支出
    }

    /** 根据月度汇总更新剩余 */
    public void applyMonthSummary(MonthSummary summary) {
        Double budget = budgetAmount.getValue();
        if (budget == null) budget = 0.0;
        double remaining = budget - summary.totalExpense;
        budgetRemaining.postValue(remaining);
    }

    /** 设置预算 */
    public void setBudget(String yearMonth, double amount) {
        budgetStorage.setBudget(yearMonth, amount);
        budgetAmount.postValue(amount);
    }
}

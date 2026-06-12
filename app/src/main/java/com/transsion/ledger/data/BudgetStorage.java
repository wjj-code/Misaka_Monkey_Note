package com.transsion.ledger.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 预算持久化 —— SharedPreferences，按月存储
 * key: budget_2025-06 → value: "500"
 */
public class BudgetStorage {

    private static final String PREFS_NAME = "budget_prefs";
    private static final String KEY_PREFIX = "budget_";

    private final SharedPreferences prefs;

    public BudgetStorage(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** 获取某月预算，未设置返回 0 */
    public double getBudget(String yearMonth) {
        return Double.parseDouble(prefs.getString(KEY_PREFIX + yearMonth, "0"));
    }

    /** 设置某月预算 */
    public void setBudget(String yearMonth, double amount) {
        prefs.edit().putString(KEY_PREFIX + yearMonth, String.valueOf(amount)).apply();
    }
}

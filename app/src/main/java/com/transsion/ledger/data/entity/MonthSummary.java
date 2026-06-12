package com.transsion.ledger.data.entity;

import androidx.room.ColumnInfo;

/**
 * 月度汇总 POJO
 */
public class MonthSummary {

    @ColumnInfo(name = "month")
    public String month;   // yyyy-MM

    @ColumnInfo(name = "total_income")
    public double totalIncome;

    @ColumnInfo(name = "total_expense")
    public double totalExpense;

    public MonthSummary() {}
}

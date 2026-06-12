package com.transsion.ledger.data.entity;

import androidx.room.ColumnInfo;

/**
 * 每日汇总 POJO，用于日历视图展示每天支出合计
 */
public class DailySummary {

    @ColumnInfo(name = "date")
    public String date;   // yyyy-MM-dd

    @ColumnInfo(name = "total")
    public double total;

    @ColumnInfo(name = "income")
    public double income;

    @ColumnInfo(name = "expense")
    public double expense;

    // Room 需要的空构造
    public DailySummary() {}
}

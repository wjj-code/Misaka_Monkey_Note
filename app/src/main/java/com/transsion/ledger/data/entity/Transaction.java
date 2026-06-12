package com.transsion.ledger.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class Transaction {

    @PrimaryKey(autoGenerate = true)
    private long id;

    // 0 = 支出, 1 = 收入
    private int type;

    private double amount;

    // 一级分类：吃/住/娱/教育/交通/购物/医疗/其他
    private String category1;

    // 二级子项目：早餐/午餐/...
    private String category2;

    // 财务分类：0=维持类, 1=消费类, 2=提升类, 3=社交类
    private int category3;

    // 时间戳（毫秒）
    private long dateTime;

    // 备注（选填）
    private String note;

    // 关联账户ID（0 = 默认账户，后续由代码映射）
    private long accountId;

    public Transaction(int type, double amount, String category1,
                       String category2, int category3, long dateTime,
                       String note, long accountId) {
        this.type = type;
        this.amount = amount;
        this.category1 = category1;
        this.category2 = category2;
        this.category3 = category3;
        this.dateTime = dateTime;
        this.note = note;
        this.accountId = accountId;
    }

    // Getters
    public long getId() { return id; }
    public int getType() { return type; }
    public double getAmount() { return amount; }
    public String getCategory1() { return category1; }
    public String getCategory2() { return category2; }
    public int getCategory3() { return category3; }
    public long getDateTime() { return dateTime; }
    public String getNote() { return note; }
    public long getAccountId() { return accountId; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setType(int type) { this.type = type; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setCategory1(String category1) { this.category1 = category1; }
    public void setCategory2(String category2) { this.category2 = category2; }
    public void setCategory3(int category3) { this.category3 = category3; }
    public void setDateTime(long dateTime) { this.dateTime = dateTime; }
    public void setNote(String note) { this.note = note; }
    public void setAccountId(long accountId) { this.accountId = accountId; }
}

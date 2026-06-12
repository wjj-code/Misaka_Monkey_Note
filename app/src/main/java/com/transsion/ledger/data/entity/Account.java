package com.transsion.ledger.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "accounts")
public class Account {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /* ---- 必填 ---- */
    private String name;           // 账户名称
    private double balance;        // 余额
    // 类型：活期 / 资产
    private String type;           // "活期" / "资产"

    private boolean isActive;      // 是否可用
    private boolean includeInNetWorth; // 是否计入净资产
    private boolean canExpense;    // 是否可支出
    private boolean canIncome;     // 是否可收入
    private boolean canTransferIn; // 是否可转入
    private boolean canTransferOut;// 是否可转出
    private boolean isDefault;     // 是否默认账户

    /* ---- 选填 ---- */
    private String cardNumber;     // 卡号
    private String note;           // 备注

    // Room 需要空构造
    public Account() {}

    @Ignore
    public Account(String name, double balance, String type,
                   boolean isActive, boolean includeInNetWorth,
                   boolean canExpense, boolean canIncome,
                   boolean canTransferIn, boolean canTransferOut,
                   boolean isDefault, String cardNumber, String note) {
        this.name = name;
        this.balance = balance;
        this.type = type;
        this.isActive = isActive;
        this.includeInNetWorth = includeInNetWorth;
        this.canExpense = canExpense;
        this.canIncome = canIncome;
        this.canTransferIn = canTransferIn;
        this.canTransferOut = canTransferOut;
        this.isDefault = isDefault;
        this.cardNumber = cardNumber;
        this.note = note;
    }

    // Getters
    public long getId() { return id; }
    public String getName() { return name; }
    public double getBalance() { return balance; }
    public String getType() { return type; }
    public boolean isActive() { return isActive; }
    public boolean isIncludeInNetWorth() { return includeInNetWorth; }
    public boolean isCanExpense() { return canExpense; }
    public boolean isCanIncome() { return canIncome; }
    public boolean isCanTransferIn() { return canTransferIn; }
    public boolean isCanTransferOut() { return canTransferOut; }
    public boolean isDefault() { return isDefault; }
    public String getCardNumber() { return cardNumber; }
    public String getNote() { return note; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setBalance(double balance) { this.balance = balance; }
    public void setType(String type) { this.type = type; }
    public void setActive(boolean active) { isActive = active; }
    public void setIncludeInNetWorth(boolean b) { includeInNetWorth = b; }
    public void setCanExpense(boolean b) { canExpense = b; }
    public void setCanIncome(boolean b) { canIncome = b; }
    public void setCanTransferIn(boolean b) { canTransferIn = b; }
    public void setCanTransferOut(boolean b) { canTransferOut = b; }
    public void setDefault(boolean b) { isDefault = b; }
    public void setCardNumber(String s) { cardNumber = s; }
    public void setNote(String s) { note = s; }
}

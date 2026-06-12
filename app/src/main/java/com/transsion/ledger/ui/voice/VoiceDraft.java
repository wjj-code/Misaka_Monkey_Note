package com.transsion.ledger.ui.voice;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/** 语音解析结果 / 跳转编辑页草稿 */
public class VoiceDraft {

    public static final String KEY_VOICE_DRAFT = "voiceDraft";
    public static final String KEY_TYPE = "draftType";
    public static final String KEY_CATEGORY1 = "draftCategory1";
    public static final String KEY_CATEGORY2 = "draftCategory2";
    public static final String KEY_AMOUNT = "draftAmount";
    public static final String KEY_NOTE = "draftNote";
    public static final String KEY_ACCOUNT_NAME = "draftAccountName";
    public static final String KEY_CATEGORY3 = "draftCategory3";
    public static final String KEY_DATE_TIME = "draftDateTime";
    public static final String KEY_RAW_TEXT = "draftRawText";

    public int type = -1;
    public String category1;
    public String category2 = "";
    public double amount;
    public String note = "";
    public String accountName = "";
    public int category3 = -1;
    public long dateTime = System.currentTimeMillis();
    public String rawText = "";

    public boolean isExpense() {
        return type == 0;
    }

    public boolean hasRequired() {
        return type >= 0
                && category1 != null && !category1.isEmpty()
                && category2 != null && !category2.trim().isEmpty()
                && amount > 0;
    }

    /** 支出保存前还需财务分类 */
    public boolean canSaveDirectly() {
        if (!hasRequired()) return false;
        if (isExpense() && category3 < 0) return false;
        return true;
    }

    public List<String> getMissingOptionalLabels() {
        List<String> missing = new ArrayList<>();
        if (note == null || note.trim().isEmpty()) {
            missing.add("备注");
        }
        if (accountName == null || accountName.trim().isEmpty()) {
            missing.add("入账账户");
        }
        if (isExpense() && category3 < 0) {
            missing.add("财务分类");
        }
        return missing;
    }

    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putBoolean(KEY_VOICE_DRAFT, true);
        b.putInt(KEY_TYPE, type);
        if (category1 != null) b.putString(KEY_CATEGORY1, category1);
        b.putString(KEY_CATEGORY2, category2);
        b.putDouble(KEY_AMOUNT, amount);
        b.putString(KEY_NOTE, note);
        b.putString(KEY_ACCOUNT_NAME, accountName);
        b.putInt(KEY_CATEGORY3, category3);
        b.putLong(KEY_DATE_TIME, dateTime);
        b.putString(KEY_RAW_TEXT, rawText);
        return b;
    }

    public static VoiceDraft fromBundle(Bundle b) {
        VoiceDraft d = new VoiceDraft();
        if (b == null) return d;
        d.type = b.getInt(KEY_TYPE, -1);
        d.category1 = b.getString(KEY_CATEGORY1);
        d.category2 = b.getString(KEY_CATEGORY2, "");
        d.amount = b.getDouble(KEY_AMOUNT, 0);
        d.note = b.getString(KEY_NOTE, "");
        d.accountName = b.getString(KEY_ACCOUNT_NAME, "");
        d.category3 = b.getInt(KEY_CATEGORY3, -1);
        d.dateTime = b.getLong(KEY_DATE_TIME, System.currentTimeMillis());
        d.rawText = b.getString(KEY_RAW_TEXT, "");
        return d;
    }
}

package com.transsion.ledger.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 一级分类 + 子项目（细则）常量，与 {@link com.transsion.ledger.ui.add.AddTransactionSheet} 一致 */
public final class CategoryCatalog {

    public static final String[] CAT1_EXPENSE = {
            "🍚 吃", "🏠 住", "🎮 娱", "📚 教育", "🚗 交通", "🛒 购物", "🏥 医疗", "📌 其他"
    };
    public static final String[] CAT1_INCOME = {
            "💼 工资", "📈 投资", "💻 兼职", "↩️ 退款", "🎁 礼金", "📌 其他"
    };

    private static final String[] CAT1_SHORT_EXPENSE = {
            "吃", "住", "娱", "教育", "交通", "购物", "医疗", "其他"
    };
    private static final String[] CAT1_SHORT_INCOME = {
            "工资", "投资", "兼职", "退款", "礼金", "其他"
    };

    private static final Map<String, List<String>> SUBS_EXPENSE = new HashMap<>();
    private static final Map<String, List<String>> SUBS_INCOME = new HashMap<>();

    static {
        SUBS_EXPENSE.put("🍚 吃", Arrays.asList("早餐", "午餐", "晚餐", "零食", "水果", "饮品", "聚餐"));
        SUBS_EXPENSE.put("🏠 住", Arrays.asList("房租", "水电", "物业", "网费", "日用品", "维修"));
        SUBS_EXPENSE.put("🎮 娱", Arrays.asList("电影", "游戏", "KTV", "旅游", "运动", "订阅会员"));
        SUBS_EXPENSE.put("📚 教育", Arrays.asList("书籍", "课程", "培训", "文具", "考试费"));
        SUBS_EXPENSE.put("🚗 交通", Arrays.asList("公交", "地铁", "打车", "加油", "停车", "高铁", "飞机"));
        SUBS_EXPENSE.put("🛒 购物", Arrays.asList("服饰", "数码", "美妆", "家居", "超市"));
        SUBS_EXPENSE.put("🏥 医疗", Arrays.asList("门诊", "药品", "体检", "牙科"));
        SUBS_EXPENSE.put("📌 其他", Arrays.asList("自定义"));

        SUBS_INCOME.put("💼 工资", Arrays.asList("基本工资", "奖金", "加班费", "补贴"));
        SUBS_INCOME.put("📈 投资", Arrays.asList("股票", "基金", "利息", "分红", "房租收入"));
        SUBS_INCOME.put("💻 兼职", Arrays.asList("副业", "稿费", "咨询费", "设计费"));
        SUBS_INCOME.put("↩️ 退款", Arrays.asList("购物退款", "报销", "押金退还"));
        SUBS_INCOME.put("🎁 礼金", Arrays.asList("红包", "礼金", "赠与"));
        SUBS_INCOME.put("📌 其他", Arrays.asList("自定义"));
    }

    private CategoryCatalog() {}

    public static String[] cat1ForType(int type) {
        return type == 1 ? CAT1_INCOME : CAT1_EXPENSE;
    }

    public static List<String> subsFor(String category1, int type) {
        if (category1 == null) return Collections.emptyList();
        Map<String, List<String>> subs = type == 1 ? SUBS_INCOME : SUBS_EXPENSE;
        return subs.getOrDefault(category1, Collections.emptyList());
    }

    /** 从文本或简称解析一级分类（返回带 emoji 的完整名） */
    public static String matchCategory1(String text, int type) {
        if (text == null || text.isEmpty()) return null;
        String normalized = text.replace(" ", "");
        for (String full : cat1ForType(type)) {
            if (normalized.equals(full) || normalized.contains(full)) return full;
        }
        String[] shorts = type == 1 ? CAT1_SHORT_INCOME : CAT1_SHORT_EXPENSE;
        String[] fulls = type == 1 ? CAT1_INCOME : CAT1_EXPENSE;
        if (type < 0) {
            for (int i = 0; i < CAT1_SHORT_EXPENSE.length; i++) {
                if (normalized.contains(CAT1_SHORT_EXPENSE[i])) return CAT1_EXPENSE[i];
            }
            for (int i = 0; i < CAT1_SHORT_INCOME.length; i++) {
                if (normalized.contains(CAT1_SHORT_INCOME[i])) return CAT1_INCOME[i];
            }
            return null;
        }
        for (int i = 0; i < shorts.length; i++) {
            if (normalized.contains(shorts[i])) return fulls[i];
        }
        return null;
    }

    /** 在一级分类下匹配子项目（细则），如 吃 → 早餐 */
    public static String matchCategory2(String fullText, String category1, int type) {
        if (category1 == null || fullText == null) return "";
        List<String> items = subsFor(category1, type);
        String best = "";
        int bestLen = 0;
        String normalized = fullText.replace(" ", "");
        for (String item : items) {
            if ("自定义".equals(item)) continue;
            if (normalized.contains(item) && item.length() > bestLen) {
                best = item;
                bestLen = item.length();
            }
        }
        return best;
    }

    /** 将解析出的细则规范到当前一级分类的子项目列表中 */
    public static String normalizeCategory2(String raw, String fullText, String category1, int type) {
        String fromText = matchCategory2(fullText, category1, type);
        if (raw == null || raw.trim().isEmpty()) return fromText;
        String trimmed = raw.trim();
        List<String> items = subsFor(category1, type);
        for (String item : items) {
            if (item.equals(trimmed) || trimmed.contains(item) || item.contains(trimmed)) {
                return item;
            }
        }
        return fromText.isEmpty() ? trimmed : fromText;
    }

    /** 一级分类 Spinner 选中：支持「🍚 吃」或简称「吃」 */
    public static int indexOfCategory1(String category1, int type) {
        if (category1 == null) return -1;
        String[] labels = cat1ForType(type);
        for (int i = 0; i < labels.length; i++) {
            if (category1.equals(labels[i])) return i;
        }
        String shortName = stripEmojiPrefix(category1);
        for (int i = 0; i < labels.length; i++) {
            if (shortName.equals(stripEmojiPrefix(labels[i]))) return i;
        }
        return -1;
    }

    /** 子项目 Spinner 选项；若已有值不在列表中则追加（历史自定义项） */
    public static List<String> subsOptionsFor(String category1, int type, String currentValue) {
        List<String> base = new ArrayList<>(subsFor(category1, type));
        if (currentValue != null && !currentValue.isEmpty() && !base.contains(currentValue)) {
            base.add(currentValue);
        }
        return base;
    }

    public static int indexOfCategory2(String category2, List<String> options) {
        if (category2 == null || options == null) return -1;
        for (int i = 0; i < options.size(); i++) {
            if (category2.equals(options.get(i))) return i;
        }
        for (int i = 0; i < options.size(); i++) {
            String opt = options.get(i);
            if (opt.contains(category2) || category2.contains(opt)) return i;
        }
        return -1;
    }

    public static String stripEmojiPrefix(String cat) {
        if (cat == null || cat.trim().isEmpty()) return cat;
        int space = cat.indexOf(' ');
        if (space >= 0 && space < cat.length() - 1) {
            return cat.substring(space + 1).trim();
        }
        return cat.trim();
    }
}

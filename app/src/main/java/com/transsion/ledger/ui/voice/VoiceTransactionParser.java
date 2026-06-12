package com.transsion.ledger.ui.voice;

import com.transsion.ledger.util.CategoryCatalog;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 从中文语音/文本中提取记账字段 */
public class VoiceTransactionParser {

    public VoiceDraft parse(String text) {
        VoiceDraft d = new VoiceDraft();
        if (text == null) return d;
        String raw = text.trim();
        d.rawText = raw;
        if (raw.isEmpty()) return d;

        String normalized = raw.replace(" ", "")
                .replace("，", "").replace(",", "")
                .replace("。", "").replace("．", ".");

        if (normalized.contains("收入")) d.type = 1;
        else if (normalized.contains("支出")) d.type = 0;

        d.category1 = CategoryCatalog.matchCategory1(normalized, d.type);
        d.category3 = matchCategory3(normalized);

        String note = extractAfterKeyword(raw, "备注");
        if (note != null) d.note = note.trim();

        String account = extractAfterKeyword(raw, "账户");
        if (account == null) account = extractAfterKeyword(raw, "账号");
        if (account != null) d.accountName = account.trim();

        d.amount = parseAmount(normalized);

        String detail = extractAfterKeyword(raw, "细则");
        if (detail == null) detail = extractAfterKeyword(raw, "子项目");
        if (detail != null) d.category2 = detail.trim();

        if (d.category1 == null) {
            String cat = extractAfterKeyword(raw, "类别");
            if (cat != null) d.category1 = CategoryCatalog.matchCategory1(stripPunct(cat), d.type);
        }

        if (d.category1 != null) {
            d.category2 = CategoryCatalog.normalizeCategory2(d.category2, normalized, d.category1, d.type);
        } else {
            d.category2 = firstNonEmpty(d.category2, "");
        }

        String fin = extractAfterKeyword(raw, "财务分类");
        if (fin != null) {
            int c3 = matchCategory3(stripPunct(fin));
            if (c3 >= 0) d.category3 = c3;
        }

        return d;
    }

    private static int matchCategory3(String text) {
        if (text.contains("维持类") || text.contains("维持")) return 0;
        if (text.contains("消费类") || text.contains("消费")) return 1;
        if (text.contains("提升类") || text.contains("提升")) return 2;
        if (text.contains("社交类") || text.contains("社交")) return 3;
        return -1;
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && !a.trim().isEmpty()) return a.trim();
        if (b != null && !b.trim().isEmpty()) return b.trim();
        return "";
    }

    private static String stripPunct(String s) {
        return s.replace(" ", "").replace("，", "").replace(",", "")
                .replace("。", "").trim();
    }

    private static String extractAfterKeyword(String raw, String keyword) {
        int idx = raw.indexOf(keyword);
        if (idx < 0) return null;
        String tail = raw.substring(idx + keyword.length()).trim();
        if (tail.startsWith("：") || tail.startsWith(":")) tail = tail.substring(1).trim();
        if (tail.isEmpty()) return null;
        int cut = tail.length();
        for (String stop : new String[]{
                "备注", "账户", "账号", "支出", "收入", "金额",
                "类别", "细则", "子项目", "财务分类",
                "维持", "消费", "提升", "社交"
        }) {
            int p = tail.indexOf(stop);
            if (p > 0) cut = Math.min(cut, p);
        }
        return tail.substring(0, cut).trim();
    }

    private static double parseAmount(String text) {
        Pattern arabic = Pattern.compile("(?:金额|花了|付了|共)?([0-9]+(?:\\.[0-9]+)?)\\s*(?:元|块|块钱)?");
        Matcher m = arabic.matcher(text);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        Pattern cn = Pattern.compile("([零一二三四五六七八九十百千万两\\.]+)\\s*(?:元|块|块钱)");
        m = cn.matcher(text);
        if (m.find()) {
            double v = chineseToNumber(m.group(1));
            if (v > 0) return v;
        }
        return 0;
    }

    private static double chineseToNumber(String cn) {
        if (cn == null || cn.isEmpty()) return 0;
        if (cn.matches("[0-9]+(\\.[0-9]+)?")) {
            return Double.parseDouble(cn);
        }
        Map<Character, Integer> digit = new HashMap<>();
        digit.put('零', 0); digit.put('一', 1); digit.put('二', 2); digit.put('两', 2);
        digit.put('三', 3); digit.put('四', 4); digit.put('五', 5); digit.put('六', 6);
        digit.put('七', 7); digit.put('八', 8); digit.put('九', 9);

        if (cn.contains("点")) {
            String[] parts = cn.split("点");
            return chineseToNumber(parts[0]) + chineseToNumber(parts[1]) / Math.pow(10, parts[1].length());
        }

        if (cn.contains("万")) {
            String[] parts = cn.split("万");
            double left = parts.length > 0 ? chineseToNumber(parts[0]) : 0;
            double right = parts.length > 1 ? chineseToNumber(parts[1]) : 0;
            return left * 10000 + right;
        }
        if (cn.contains("千")) {
            String[] parts = cn.split("千");
            double left = parts.length > 0 ? chineseToNumber(parts[0]) : 0;
            double right = parts.length > 1 ? chineseToNumber(parts[1]) : 0;
            return left * 1000 + right;
        }
        if (cn.contains("百")) {
            String[] parts = cn.split("百");
            double left = parts.length > 0 ? chineseToNumber(parts[0]) : 0;
            double right = parts.length > 1 ? chineseToNumber(parts[1]) : 0;
            return left * 100 + right;
        }
        if (cn.contains("十")) {
            int idx = cn.indexOf('十');
            int tens = idx == 0 ? 1 : digit.getOrDefault(cn.charAt(0), 0);
            int ones = idx < cn.length() - 1 ? digit.getOrDefault(cn.charAt(idx + 1), 0) : 0;
            return tens * 10 + ones;
        }
        if (cn.length() == 1 && digit.containsKey(cn.charAt(0))) {
            return digit.get(cn.charAt(0));
        }
        return 0;
    }

    /** 将正则/关键词解析结果格式化为可读预览（在原文展示之后） */
    public String formatPreview(VoiceDraft d) {
        if (d == null || d.rawText == null || d.rawText.trim().isEmpty()) {
            return "（尚无解析结果）";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("收支：").append(labelType(d.type)).append('\n');
        sb.append("类别：").append(labelOrMissing(d.category1)).append('\n');
        sb.append("细则：").append(labelOrMissing(d.category2)).append('\n');
        sb.append("金额：").append(d.amount > 0 ? String.format("%.2f 元", d.amount) : "未识别 *").append('\n');
        if (d.type == 0) {
            sb.append("财务分类：").append(labelCategory3(d.category3)).append('\n');
        }
        sb.append("备注：").append(emptyOrValue(d.note)).append('\n');
        sb.append("账户：").append(emptyOrValue(d.accountName)).append('\n');
        sb.append('\n');
        if (d.hasRequired() && (d.type != 0 || d.category3 >= 0)) {
            sb.append("✓ 必填项已齐，可确认记账");
        } else if (d.hasRequired() && d.type == 0 && d.category3 < 0) {
            sb.append("! 还缺财务分类，确认后将进入补全页");
        } else {
            sb.append("! 必填未齐（收支/类别/细则/金额），确认后将进入补全页");
        }
        return sb.toString();
    }

    private static String labelType(int type) {
        if (type == 0) return "支出";
        if (type == 1) return "收入";
        return "未识别 *";
    }

    private static String labelCategory3(int cat3) {
        switch (cat3) {
            case 0: return "维持类";
            case 1: return "消费类";
            case 2: return "提升类";
            case 3: return "社交类";
            default: return "未识别（支出建议填写）";
        }
    }

    private static String labelOrMissing(String value) {
        if (value == null || value.trim().isEmpty()) return "未识别 *";
        return CategoryCatalog.stripEmojiPrefix(value);
    }

    private static String emptyOrValue(String value) {
        if (value == null || value.trim().isEmpty()) return "（未填，选填）";
        return value.trim();
    }
}

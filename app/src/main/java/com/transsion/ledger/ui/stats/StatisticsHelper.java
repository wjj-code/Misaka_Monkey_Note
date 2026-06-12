package com.transsion.ledger.ui.stats;

import com.transsion.ledger.data.entity.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class StatisticsHelper {

    public enum TimeRange {
        MONTH(0), HALF_YEAR(5), YEAR(11);

        /** 相对当前月向前推的月数（含当月） */
        public final int monthsBack;

        TimeRange(int monthsBack) {
            this.monthsBack = monthsBack;
        }
    }

    private static final String[] CAT3_LABELS = {"维持类", "消费类", "提升类", "社交类"};
    private static final int[] CAT3_COLORS = {
            0xFF5B8DEF, 0xFFF59E4B, 0xFFA855F7, 0xFF06B6D4
    };
    private static final int COLOR_OTHER = 0xFF9CA3AF;
    private static final int[] INCOME_COLORS = {
            0xFF2EAC68, 0xFF2D9CDB, 0xFFF59E4B, 0xFFA855F7, 0xFF06B6D4, 0xFFE5595A, 0xFF6B7280, 0xFF5B8DEF
    };

    private StatisticsHelper() {}

    public static long rangeStartMillis(TimeRange range) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.MONTH, -range.monthsBack);
        return cal.getTimeInMillis();
    }

    public static long rangeEndMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    public static List<StatNode> buildBreakdownTree(List<Transaction> transactions,
                                                    int type,
                                                    long startMillis,
                                                    long endMillis) {
        List<Transaction> filtered = filter(transactions, type, startMillis, endMillis);
        if (type == 0) {
            return buildExpenseTree(filtered);
        }
        return buildIncomeTree(filtered);
    }

    public static double sumInRange(List<Transaction> transactions,
                                    int type,
                                    long startMillis,
                                    long endMillis) {
        double sum = 0;
        for (Transaction t : filter(transactions, type, startMillis, endMillis)) {
            sum += t.getAmount();
        }
        return sum;
    }

    public static float[] buildMonthlySeries(List<Transaction> transactions,
                                             int type,
                                             TimeRange lineRange) {
        Calendar end = Calendar.getInstance();
        end.set(Calendar.DAY_OF_MONTH, 1);
        end.set(Calendar.HOUR_OF_DAY, 0);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);

        int count = lineRange.monthsBack + 1;
        float[] values = new float[count];
        Calendar cursor = (Calendar) end.clone();
        cursor.add(Calendar.MONTH, -lineRange.monthsBack);

        for (int i = 0; i < count; i++) {
            long monthStart = cursor.getTimeInMillis();
            cursor.add(Calendar.MONTH, 1);
            long monthEnd = cursor.getTimeInMillis() - 1;
            for (Transaction t : transactions) {
                if (t.getType() != type) continue;
                long dt = t.getDateTime();
                if (dt >= monthStart && dt <= monthEnd) {
                    values[i] += (float) t.getAmount();
                }
            }
        }
        return values;
    }

    public static String[] buildMonthLabels(TimeRange lineRange) {
        SimpleDateFormat fmt = new SimpleDateFormat("M月", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, -lineRange.monthsBack);

        int count = lineRange.monthsBack + 1;
        String[] labels = new String[count];
        for (int i = 0; i < count; i++) {
            labels[i] = fmt.format(cal.getTime());
            cal.add(Calendar.MONTH, 1);
        }
        return labels;
    }

    private static List<Transaction> filter(List<Transaction> transactions,
                                            int type,
                                            long startMillis,
                                            long endMillis) {
        List<Transaction> out = new ArrayList<>();
        if (transactions == null) return out;
        for (Transaction t : transactions) {
            if (t.getType() != type) continue;
            long dt = t.getDateTime();
            if (dt >= startMillis && dt <= endMillis) {
                out.add(t);
            }
        }
        return out;
    }

    private static List<StatNode> buildExpenseTree(List<Transaction> filtered) {
        Map<Integer, Map<String, Map<String, Double>>> tree = new LinkedHashMap<>();
        double total = 0;
        for (Transaction t : filtered) {
            total += t.getAmount();
            int cat3 = t.getCategory3();
            if (cat3 < 0 || cat3 > 3) cat3 = -1;
            String cat1 = shortLabel(t.getCategory1());
            String cat2 = emptyToDefault(t.getCategory2(), "其他");

            tree.computeIfAbsent(cat3, k -> new LinkedHashMap<>())
                    .computeIfAbsent(cat1, k -> new LinkedHashMap<>())
                    .merge(cat2, t.getAmount(), Double::sum);
        }

        List<StatNode> roots = new ArrayList<>();
        int[] order = {0, 1, 2, 3, -1};
        for (int cat3 : order) {
            Map<String, Map<String, Double>> cat1Map = tree.get(cat3);
            if (cat1Map == null || cat1Map.isEmpty()) continue;

            StatNode root = new StatNode(cat3Label(cat3));
            root.color = cat3Color(cat3);
            double cat3Sum = sumMap2(cat1Map);
            root.amount = cat3Sum;
            root.percent = total > 0 ? cat3Sum / total * 100 : 0;

            for (Map.Entry<String, Map<String, Double>> e1 : cat1Map.entrySet()) {
                StatNode n1 = new StatNode(e1.getKey());
                double cat1Sum = sumInner(e1.getValue());
                n1.amount = cat1Sum;
                n1.percent = cat3Sum > 0 ? cat1Sum / cat3Sum * 100 : 0;
                for (Map.Entry<String, Double> e2 : e1.getValue().entrySet()) {
                    StatNode n2 = new StatNode(e2.getKey());
                    n2.amount = e2.getValue();
                    n2.percent = cat1Sum > 0 ? e2.getValue() / cat1Sum * 100 : 0;
                    n1.children.add(n2);
                }
                root.children.add(n1);
            }
            roots.add(root);
        }
        return roots;
    }

    private static List<StatNode> buildIncomeTree(List<Transaction> filtered) {
        Map<String, Map<String, Double>> tree = new LinkedHashMap<>();
        double total = 0;
        for (Transaction t : filtered) {
            total += t.getAmount();
            String cat1 = shortLabel(t.getCategory1());
            String cat2 = emptyToDefault(t.getCategory2(), "其他");
            tree.computeIfAbsent(cat1, k -> new LinkedHashMap<>())
                    .merge(cat2, t.getAmount(), Double::sum);
        }

        List<StatNode> roots = new ArrayList<>();
        int colorIdx = 0;
        for (Map.Entry<String, Map<String, Double>> e1 : tree.entrySet()) {
            StatNode root = new StatNode(e1.getKey());
            root.color = INCOME_COLORS[colorIdx % INCOME_COLORS.length];
            colorIdx++;
            double cat1Sum = sumInner(e1.getValue());
            root.amount = cat1Sum;
            root.percent = total > 0 ? cat1Sum / total * 100 : 0;
            for (Map.Entry<String, Double> e2 : e1.getValue().entrySet()) {
                StatNode child = new StatNode(e2.getKey());
                child.amount = e2.getValue();
                child.percent = cat1Sum > 0 ? e2.getValue() / cat1Sum * 100 : 0;
                root.children.add(child);
            }
            roots.add(root);
        }
        return roots;
    }

    private static double sumMap2(Map<String, Map<String, Double>> map) {
        double sum = 0;
        for (Map<String, Double> inner : map.values()) {
            sum += sumInner(inner);
        }
        return sum;
    }

    private static double sumInner(Map<String, Double> map) {
        double sum = 0;
        for (double v : map.values()) sum += v;
        return sum;
    }

    private static String cat3Label(int cat3) {
        if (cat3 >= 0 && cat3 < CAT3_LABELS.length) return CAT3_LABELS[cat3];
        return "未分类";
    }

    private static int cat3Color(int cat3) {
        if (cat3 >= 0 && cat3 < CAT3_COLORS.length) return CAT3_COLORS[cat3];
        return COLOR_OTHER;
    }

    static String shortLabel(String cat) {
        if (cat == null || cat.trim().isEmpty()) return "未分类";
        int space = cat.indexOf(' ');
        if (space >= 0 && space < cat.length() - 1) {
            return cat.substring(space + 1).trim();
        }
        return cat.trim();
    }

    private static String emptyToDefault(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }
}

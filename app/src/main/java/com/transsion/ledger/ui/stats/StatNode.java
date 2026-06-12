package com.transsion.ledger.ui.stats;

import java.util.ArrayList;
import java.util.List;

/** 统计树节点：财务大类 → 一级类 → 二级子项 */
public class StatNode {

    public final String label;
    public double amount;
    public double percent;
    public int color;
    public boolean expanded;
    public final List<StatNode> children = new ArrayList<>();

    public StatNode(String label) {
        this.label = label;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }
}

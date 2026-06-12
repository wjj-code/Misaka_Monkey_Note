# 用户需求录入日志

> 按 `.agents/templates/user-request-intake.md` 整理；最新置顶。

---

## 2026-06-12 — 日历选月 / 账户筛选 / 录入规范

| 区块 | 内容 |
|------|------|
| **新增逻辑** | **日历**：① 切到非当月 → 自动选中该月**第一天有记账**的日期 ② 点击顶部「x年x月」→ 弹窗快捷选年月。**账户**：① 顶部筛选「活期 / 资产 / 全部」，默认活期 ② 新增某类型账户后自动切到该类型视图 |
| **Bug 检查** | 日历切回**当月**时偶发无法自动选中**今天** |
| **架构调整** | `.agents/`：用户每次输入自动整理为 `user-request-intake.md` 格式，未涉及项填 N/A |
| **其他** | 本区块用于普通对话等非开发说明 |

**实现摘要**：`resolveAutoSelectDate` 当月恒为今天；`OnMonthChangedListener` 替代 `onDayClick(null)`；账户 `MaterialButtonToggleGroup` 筛选。

---

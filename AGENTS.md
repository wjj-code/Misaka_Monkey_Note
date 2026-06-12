# 呱太记账 — Agent 指南

> 本文件由 Cursor **每次会话自动加载**，是 `.agents/` 知识库的强制入口。
> `.agents/SKILL.md` 等文件不会自动注入上下文，因此关键规则在此重申。

## 用户需求整理（每次收到用户消息后先做）

将用户输入整理为 `.agents/templates/user-request-intake.md` 四栏表格，**未涉及区块填 N/A**，并置顶追加到 `.agents/facts/user-request-log.md`，再开始改代码。

## 开工前必读

1. `.agents/SKILL.md` — 知识库索引与工作规则（**勿在 `.agents/` 新建平行文档**）
2. `.agents/facts/class-method-reference.md` — 改代码前必查
3. `.agents/facts/architecture-overview.md` — 架构 + ASCII 流程图

## 强制规则（每次改代码都要做）

1. **数据层改动**需同步 Entity → DAO → Database → Repository → ViewModel 五层。
2. 数据库 schema 变更**必须写 Migration**，禁止 `fallbackToDestructiveMigration()`。
3. UI 遵循 MVVM：Fragment 只做 View ↔ ViewModel 绑定。

## ⚠️ 收尾清单（完成任何代码改动后，必须执行，不可跳过）

每次改完代码（含 bug 修复、小改动），在结束前**逐条核对**：

- [ ] 受影响的事实文档已更新：
  - 功能变化 → `.agents/facts/README.md`
  - 方法/类变化 → `.agents/facts/class-method-reference.md`
  - 分类/常量 → `.agents/facts/category-constants-map.md`
  - 构建/Schema/版本 → `.agents/facts/system-properties-reference.md`
  - 架构/包结构 → `.agents/facts/architecture-overview.md`
- [ ] **`.agents/facts/CHANGELOG.md` 已追加本次修改记录**（最新条目置顶；bug 修复也要记，写清根因+修复）
- [ ] 版本号若变化 → 同步 `gradle.properties`

> 即使用户只说"改个 bug"，CHANGELOG 与相关文档同步**仍是任务的一部分**，默认执行、无需用户额外提醒。

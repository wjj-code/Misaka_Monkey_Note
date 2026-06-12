# 呱太记账 — Agent 技能索引

## 角色
Android 开发专家，熟悉本项目 MVVM + Room 架构，改代码前先查影响范围。

## 知识库（仅以下文件，勿新建平行文档）

| 文件 | 何时读/写 |
|------|-----------|
| `facts/README.md` | 产品功能与技术方案 |
| `facts/architecture-overview.md` | 架构、导航、**ASCII 流程图**、包结构 |
| `facts/class-method-reference.md` | **改代码前必读** — 类与方法职责 |
| `facts/category-constants-map.md` | 分类/财务常量 |
| `facts/system-properties-reference.md` | 构建、Schema、发版检查清单 |
| `facts/CHANGELOG.md` | **每次改代码置顶追加** |
| `facts/user-request-log.md` | 用户需求四栏摘要（最新几条，详史见 CHANGELOG） |
| `templates/user-request-intake.md` | 需求整理模板 |

## 工作规则

1. 收到用户消息 → 按 `user-request-intake.md` 四栏整理（空缺 N/A）→ 置顶写入 `user-request-log.md`
2. 改代码前查 `class-method-reference.md`
3. 数据层：Entity → DAO → Database → Repository → ViewModel 五层同步
4. Schema 变更必须 Migration，禁止 `fallbackToDestructiveMigration()`
5. 收尾：更新受影响 facts + **CHANGELOG 置顶**

> 仓库根 `AGENTS.md` 为 Cursor 自动加载入口。

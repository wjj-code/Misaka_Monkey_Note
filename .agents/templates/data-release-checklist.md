# 发版前数据安全检查清单

> 每次涉及 **Entity / Migration / Repository 写逻辑** 的改动，发版前逐条核对。

## 一、Schema 与迁移

- [ ] `AppDatabase` 的 `version` 与 `.agents/facts/system-properties-reference.md` 一致
- [ ] 每个 `N→N+1` 均有 `Migration`，且已在 `addMigrations(...)` 注册
- [ ] **未使用** `fallbackToDestructiveMigration()`
- [ ] Migration 中**无** `DROP TABLE` 业务表（除非先 `INSERT INTO new SELECT * FROM old` 再切换）
- [ ] 在测试机用**旧版样本库**覆盖升级验证（见 `androidTest/.../MigrationInstrumentedTest`）

## 二、写路径回归（手测）

- [ ] 新增账单 → 对应账户余额正确
- [ ] 编辑账单（改金额/改账户）→ 两账户余额正确
- [ ] 删除账单 → 余额恢复
- [ ] 删除非默认账户 → 余额并入默认、账单改挂默认
- [ ] 默认账户不可删除
- [ ] 语音直接入账后新增账户 → 不重复入账、不误弹选填框

## 三、备份（真机发版前）

```bash
adb exec-out run-as com.transsion.ledger cat databases/ledger.db > ledger_backup_$(date +%Y%m%d).db
```

## 四、仅 UI/解析改动（可简化）

- [ ] 不改 `@Entity` / `version` → 一般安全
- [ ] 仍建议抽测 1～2 笔记账 + 删改各 1 次

## 五、文档同步

- [ ] `CHANGELOG.md` 置顶记录
- [ ] `system-properties-reference.md`（version / Migration）
- [ ] `class-method-reference.md`（Repository 方法变更）

# 案例：数据迁移与保留方案

## 问题
版本升级时，如何确保已存储的账单数据不丢失？

## 当前状态（DB v3，已使用正式 Migration ✅）

```java
// AppDatabase.java
@Database(entities = {Transaction.class, Account.class}, version = 3, exportSchema = true)
...
Room.databaseBuilder(ctx, AppDatabase.class, "ledger.db")
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)   // ✅ 正式迁移，数据不丢失
    .build();
```

> 历史：v1 曾用 `.fallbackToDestructiveMigration()`（⚠️ schema 变更会删库），**已在 v1→v2 升级时替换为正式 `MIGRATION_1_2`**。

### 已落地：MIGRATION_1_2（v1 → v2，新增账户功能）

```java
static final Migration MIGRATION_1_2 = new Migration(1, 2) {
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        // 1) transactions 加 accountId 列
        database.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER NOT NULL DEFAULT 0");
        // 2) 新建 accounts 表
        database.execSQL("CREATE TABLE accounts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, balance REAL NOT NULL, type TEXT NOT NULL, " +
                "isActive INTEGER NOT NULL, includeInNetWorth INTEGER NOT NULL, " +
                "canExpense INTEGER NOT NULL, canIncome INTEGER NOT NULL, " +
                "canTransferIn INTEGER NOT NULL, canTransferOut INTEGER NOT NULL, " +
                "isDefault INTEGER NOT NULL, cardNumber TEXT, note TEXT)");
        // 3) 插入默认账户
        database.execSQL("INSERT INTO accounts (...) VALUES ('默认账户', 0, '活期', 1,1,1,1,1,1,1, '', '')");
    }
};
```

✅ 老用户从 v1 升级到 v2：历史账单全部保留，自动补 `accountId=0` 并生成「默认账户」。

### MIGRATION_2_3（v2 → v3，已修正）

> 曾错误使用 `DROP TABLE accounts`（会删光用户账户）。已改为**保留数据**的 no-op + 空表兜底。

```java
static final Migration MIGRATION_2_3 = new Migration(2, 3) {
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        insertDefaultAccountIfEmpty(database);  // 仅 accounts 为空时插入
    }
};
```

发版前检查清单见 `.agents/templates/data-release-checklist.md`。

## 方案 A：保持 Schema 不变（最简单，适用于 UI/逻辑改动）

如果只是修改 UI 或添加功能（不改 Entity 字段），直接覆盖安装即可：

```
旧 APK (version 1, schema A)
  → 新 APK (version 1, schema A)
    → Room 检测 version 相同 → 直接使用旧数据库 → 数据完整保留 ✓
```

**规则**：
- `@Entity` 字段无增减 → 安全
- 只改 UI / Fragment / Adapter → 安全
- 只改 Repository / ViewModel 内部逻辑 → 安全

## 方案 B：使用 Migration（Schema 变更时）

当需要新增字段（如给 Transaction 加 `tag` 字段）时：

### Step 1: 修改 Entity
```java
// Transaction.java - 新增
private String tag;  // 新增标签字段
public String getTag() { return tag; }
public void setTag(String tag) { this.tag = tag; }
```

### Step 2: 提升版本号
```java
@Database(entities = {Transaction.class}, version = 2)  // 1 → 2
```

### Step 3: 编写 Migration
```java
static final Migration MIGRATION_1_2 = new Migration(1, 2) {
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE transactions ADD COLUMN tag TEXT");
    }
};
```

### Step 4: 替换 fallbackToDestructiveMigration
```java
public static AppDatabase getInstance(Context context) {
    if (INSTANCE == null) {
        synchronized (AppDatabase.class) {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "ledger.db"
                ).addMigrations(MIGRATION_1_2)
                 .build();  // ← 删除 .fallbackToDestructiveMigration()
            }
        }
    }
    return INSTANCE;
}
```

## 方案 C：导出/导入（作为备份手段）

在「我的」页面添加数据导出功能：
1. 导出为 JSON / CSV 到外部存储
2. 升级后重新导入

此方案作为 Migration 的补充保险。

## 最佳实践

| 场景 | 操作 | 数据安全 |
|------|------|---------|
| 不改 Entity | 直接升级 | ✅ 安全 |
| 新增可选字段 | 写 Migration + ALTER TABLE | ✅ 安全 |
| 删除字段 | Migration（可保留列，代码忽略） | ✅ 安全 |
| 改字段类型 | 复杂 Migration（需临时表） | ⚠️ 需测试 |
| `fallbackToDestructiveMigration` | 任何 schema 变更 | ❌ 数据丢失 |

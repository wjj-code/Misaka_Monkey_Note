# 系统属性参考

## 构建配置

| 属性 | 值 | 定义位置 |
|------|-----|---------|
| `rootProject.name` | `"Ledger"` | `settings.gradle.kts:25` |
| `namespace` | `com.transsion.ledger` | `app/build.gradle.kts:6` |
| `applicationId` | `com.transsion.ledger` | `app/build.gradle.kts:14` |
| `minSdk` | `24` (Android 7.0) | `app/build.gradle.kts:15` |
| `targetSdk` | `36` (Android 14) | `app/build.gradle.kts:16` |
| `compileSdk` | `36` | `app/build.gradle.kts:7` |
| `versionCode` | 读取 `gradle.properties` → `APP_VERSION_CODE` (当前: 1) | `gradle.properties:12` → `app/build.gradle.kts:17` |
| `versionName` | 读取 `gradle.properties` → `APP_VERSION_NAME` (当前: "1.0") | `gradle.properties:13` → `app/build.gradle.kts:18` |
| `sourceCompatibility` | `JavaVersion.VERSION_11` | `app/build.gradle.kts:33` |
| `app_name` (显示名) | `"呱太记账"` | `res/values/strings.xml:2` |
| 主题 | `Theme.Ledger` (parent: `Material3.DayNight.NoActionBar`) | `AndroidManifest.xml:13` |

## Gradle 依赖版本 (libs.versions.toml)

| 依赖 | 版本 | 分类 |
|------|------|------|
| `agp` (Android Gradle Plugin) | 9.2.1 | 构建 |
| `material` (Material3) | 1.10.0 | UI |
| `appcompat` | 1.6.1 | UI |
| `constraintlayout` | 2.1.4 | UI |
| `activity-ktx` | 1.8.0 | UI |
| `navigation` (fragment + ui) | 2.7.5 | 导航 |
| `room` (runtime + compiler) | 2.6.1 | 数据库 |
| `fragment` | 1.6.2 | Fragment |
| `junit` | 4.13.2 | 测试 |
| `espresso-core` | 3.5.1 | 测试 |

## Room 数据库 Schema

| 属性 | 值 |
|------|-----|
| 数据库名 | `ledger.db` |
| 版本 | `3`（v1→v2→v3 已迁移） |
| Entity | `Transaction`, `Account` |
| 表名 | `transactions`, `accounts` |
| 迁移策略 | `addMigrations(MIGRATION_1_2, MIGRATION_2_3)` ✅；**禁止** `fallbackToDestructiveMigration` |
| Schema 导出 | `exportSchema = true`，输出至 `app/schemas/` |
| POJO | `DailySummary`, `MonthSummary` |

### transactions 表结构

| 列名 | 类型 | 约束 |
|------|------|------|
| `id` | INTEGER | PRIMARY KEY, AUTO GENERATE |
| `type` | INTEGER | NOT NULL (0=支出, 1=收入) |
| `amount` | REAL | NOT NULL |
| `category1` | TEXT | NOT NULL（含 emoji 前缀） |
| `category2` | TEXT | NOT NULL |
| `category3` | INTEGER | NOT NULL (0-3，收入为 -1) |
| `dateTime` | INTEGER | NOT NULL (unix 毫秒) |
| `note` | TEXT | 可空 |
| `accountId` | INTEGER | NOT NULL DEFAULT 0（**v2 新增**，0=默认账户） |

### accounts 表结构（v2 新增）

| 列名 | 类型 | 约束 |
|------|------|------|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT NOT NULL |
| `name` | TEXT | NOT NULL |
| `balance` | REAL | NOT NULL |
| `type` | TEXT | NOT NULL（活期/资产） |
| `isActive` | INTEGER | NOT NULL |
| `includeInNetWorth` | INTEGER | NOT NULL |
| `canExpense` / `canIncome` | INTEGER | NOT NULL |
| `canTransferIn` / `canTransferOut` | INTEGER | NOT NULL |
| `isDefault` | INTEGER | NOT NULL |
| `cardNumber` | TEXT | 可空 |
| `note` | TEXT | 可空 |

### MIGRATION_1_2（v1 → v2）
1. `ALTER TABLE transactions ADD COLUMN accountId INTEGER NOT NULL DEFAULT 0`
2. `CREATE TABLE accounts (...)`（见上表）
3. 无账户时 `INSERT` 一条「默认账户」

### MIGRATION_2_3（v2 → v3）
- **不 DROP** `accounts`；schema 与 v2 一致
- 仅当 `accounts` 为空时补默认账户（兜底）
- 仪器测试：`androidTest/.../MigrationInstrumentedTest`

### 写操作数据安全
- `TransactionRepository`：`runInTransaction` 保证余额+账单原子写入
- `AccountRepository.deleteSafe`：删非默认账户时余额并入默认、账单 `reassignAccount`

## 预算存储 (SharedPreferences)

> 预算**不入 Room**，独立用 SharedPreferences 按月持久化。定义位置：`data/BudgetStorage.java`

| 属性 | 值 |
|------|-----|
| Prefs 文件名 | `budget_prefs` |
| key 格式 | `budget_yyyy-MM`（如 `budget_2025-06`） |
| value | 预算金额字符串（如 `"500"`），未设置返回 `"0"` |
| 入口 | 「我的」页 `💰 预算设置` → `ProfileFragment.showBudgetDialog()` |

## 资源文件索引

| 文件 | 用途 |
|------|------|
| `res/layout/activity_main.xml` | 主布局：NavHost + BottomNav |
| `res/layout/fragment_accounts.xml` | 账户管理页：账户列表 `recycler_accounts` + `btn_add_account` |
| `res/layout/item_account.xml` | 账户列表项：名称/余额/类型/备注 + 默认徽标 |
| `res/layout/fragment_bills.xml` | 账单页：顶部预算/收支汇总栏（`txt_budget_remaining`/`txt_header_income`/`txt_header_expense`/`btn_goto_calendar`=「📅 日历」带文字按钮）+ `recycler_bills` |
| `res/layout/fragment_calendar.xml` | 日历页：include(日历View) + RecyclerView |
| `res/layout/fragment_profile.xml` | 我的页：用户卡片 + 功能列表（💰预算设置 / 📤数据导出 / ℹ️关于），预算设置已接线 `btn_budget_setting` |
| `res/layout/sheet_add_transaction.xml` | 记账面板：三级 Layer 布局；顶部整行分段「支出/收入」切换 |
| `res/drawable/bg_type_toggle_track.xml` | 收支切换条浅灰圆角轨道背景 |
| `res/layout/view_calendar.xml` | 月历组件：导航+星期头+GridLayout |
| `res/layout/item_transaction.xml` | 账单列表项 |
| `res/layout/item_month_header.xml` | 月度汇总 Header |
| `res/layout/item_daily_transaction.xml` | 日历日明细项（单个 TextView） |
| `res/menu/bottom_nav_menu.xml` | 底部导航 5 项菜单定义 |
| `res/navigation/nav_graph.xml` | 导航图：4 个 Fragment |
| `res/drawable/ic_*.xml` | 5 个底部导航图标（VectorDrawable） |

## 发版前数据安全检查（涉及 Schema/Repository 写逻辑时）

- [ ] `AppDatabase.version` 与本文件、Migration 注册一致；**无** `fallbackToDestructiveMigration`
- [ ] Migration 无裸 `DROP` 业务表；仪器测试 `MigrationInstrumentedTest` 通过
- [ ] 手测：新增/编辑/删除账单 → 账户余额正确；删非默认账户 → 余额并入默认
- [ ] `CHANGELOG.md` 已置顶记录
- [ ] 真机备份：`adb exec-out run-as com.transsion.ledger cat databases/ledger.db > backup.db`

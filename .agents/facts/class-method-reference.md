# 类与方法完整参考

> 本文档覆盖项目全部 Java 类的所有方法，含参数说明和调用关系。

---

## 1. MainActivity

**路径**: `com.transsion.ledger.MainActivity`  
**父类**: `AppCompatActivity`  
**职责**: 应用唯一入口 Activity，持有底部导航和 Fragment 容器。

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `onCreate(Bundle)` | `savedInstanceState` — 恢复状态 | `void` | 初始化：① `NavController` ② 底部导航 ③ 点击「+」→ `AddTransactionSheet` ④ **长按「+」**→ `VoiceInputSheet` ⑤ 语音结果写入或跳编辑补全 |
| `setupVoiceLongPress(BottomNavigationView)` (private) | `bottomNav` | `void` | 为 `nav_add` 绑定长按，防抖避免触发点击 |
| `handleVoiceResult(VoiceDraft)` (private) | `draft` | `void` | `canSaveDirectly()`→`insert`+选填浮窗；否则 `nav_transaction_edit` 带草稿 |
| `resolveAccountId(List, String)` (private) | 账户列表、语音账户名 | `long` | 模糊匹配账户名，无则 `0`（默认账户） |
| `handleVoiceResult` 入账 | — | — | 通过 `AccountRepository.fetchAll()` **一次性**读账户后 `insert`，避免 LiveData 在新增账户时重复触发 |

### onCreate 内部逻辑
```java
// 1. 获取 NavController
NavHostFragment navHost = getSupportFragmentManager()
    .findFragmentById(R.id.nav_host_fragment);
navController = navHost.getNavController();

// 2. 绑定 BottomNavigationView
NavigationUI.setupWithNavController(bottomNav, navController);

// 3. 拦截中间 + 号
bottomNav.setOnItemSelectedListener(item -> {
    if (item.getItemId() == R.id.nav_add) {
        new AddTransactionSheet().show(getSupportFragmentManager(), ...);
        return false;  // ← 返回 false 阻止导航切换
    }
    NavigationUI.onNavDestinationSelected(item, navController);
    return true;
});
```

---

## 2. Transaction (Entity)

**路径**: `com.transsion.ledger.data.entity.Transaction`  
**Room 表名**: `transactions`  
**职责**: 数据库实体，一条记账记录。

| 字段 | 类型 | 含义 | 取值 |
|------|------|------|------|
| `id` | `long` (PK, autoGen) | 主键 | 自增 |
| `type` | `int` | 收支类型 | `0`=支出, `1`=收入 |
| `amount` | `double` | 金额 | > 0 |
| `category1` | `String` | 一级分类（**含 emoji 前缀**） | `🍚 吃`/`🏠 住`/`🎮 娱`/… |
| `category2` | `String` | 二级子项目 | 早餐/午餐/房租/水电/... |
| `category3` | `int` | 财务分类 | `0`=维持, `1`=消费, `2`=提升, `3`=社交（收入存 `-1`） |
| `dateTime` | `long` | 时间戳 | 毫秒 |
| `note` | `String` | 备注 | 可选 |
| `accountId` | `long` | 关联账户ID（**DB v2 新增**） | `0`=默认账户 |

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `Transaction(...)` (构造) | type, amount, category1, category2, category3, dateTime, note, **accountId** | — | 构造一条记账记录（**8 参**，最后一个为 accountId） |
| `getId()` | — | `long` | 获取主键 |
| `getType()` | — | `int` | 获取收支类型 |
| `getAmount()` | — | `double` | 获取金额 |
| `getCategory1()` | — | `String` | 获取一级分类 |
| `getCategory2()` | — | `String` | 获取二级子项目 |
| `getCategory3()` | — | `int` | 获取财务分类 |
| `getDateTime()` | — | `long` | 获取时间戳（毫秒） |
| `getNote()` | — | `String` | 获取备注 |
| `setId(long)` | id — 主键 | `void` | Room 内部使用 |
| `setType(int)` | type | `void` | 设置收支类型 |
| `setAmount(double)` | amount | `void` | 设置金额 |
| `setCategory1(String)` | category1 | `void` | 设置一级分类 |
| `setCategory2(String)` | category2 | `void` | 设置二级子项目 |
| `setCategory3(int)` | category3 | `void` | 设置财务分类 |
| `setDateTime(long)` | dateTime | `void` | 设置时间戳 |
| `setNote(String)` | note | `void` | 设置备注 |
| `getAccountId()` / `setAccountId(long)` | accountId | `long` / `void` | 获取/设置关联账户ID |

---

## 3. DailySummary (POJO)

**路径**: `com.transsion.ledger.data.entity.DailySummary`  
**职责**: Room 聚合查询结果 POJO，每日收支汇总，用于日历视图。

| 字段 | 类型 | 含义 |
|------|------|------|
| `date` | `String` | 日期 `yyyy-MM-dd` |
| `total` | `double` | 当日合计 |
| `income` | `double` | 当日收入总计 |
| `expense` | `double` | 当日支出总计 |

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `DailySummary()` | — | — | Room 要求的空构造 |

---

## 4. MonthSummary (POJO)

**路径**: `com.transsion.ledger.data.entity.MonthSummary`  
**职责**: 月度收支汇总 POJO，用于账户页。

| 字段 | 类型 | 含义 |
|------|------|------|
| `month` | `String` | 月份 `yyyy-MM` |
| `totalIncome` | `double` | 当月收入总计 |
| `totalExpense` | `double` | 当月支出总计 |

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `MonthSummary()` | — | — | Room 要求的空构造 |

---

## 5. TransactionDao (DAO)

**路径**: `com.transsion.ledger.data.dao.TransactionDao`  
**职责**: Room 数据访问对象，定义所有数据库操作。

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `insert(Transaction)` | `transaction` — 要插入的记录 | `long` (新 ID) | 插入一条记录 |
| `update(Transaction)` | `transaction` — 要更新的记录 | `void` | 更新记录 |
| `delete(Transaction)` | `transaction` — 要删除的记录 | `void` | 删除记录 |
| `deleteById(long)` | `id` — 主键 | `void` | 按 ID 删除 |
| `getByIdSync(long)` | `id` — 主键 | `Transaction` | **同步**按 id 查询，供改/删时冲销账户余额（须在 IO 线程调用） |
| `getAll()` | — | `LiveData<List<Transaction>>` | 查询全部记录，时间降序 |
| `getByDateRange(long, long)` | `startMillis` — 起始毫秒, `endMillis` — 结束毫秒 | `LiveData<List<Transaction>>` | 按时间戳范围查询 |
| `getByYearMonth(String)` | `yearMonth` — 格式 `yyyy-MM` | `LiveData<List<Transaction>>` | 按年月查询（SQL: `strftime`） |
| `getByDate(String)` | `date` — 格式 `yyyy-MM-dd` | `LiveData<List<Transaction>>` | 按**本地时区**日期查询 |
| `getById(long)` | `id` — 主键 | `LiveData<Transaction>` | 单条记录（编辑页观察） |
| `getMonthSummary(String)` | `yearMonth` — 格式 `yyyy-MM` | `LiveData<MonthSummary>` | 按年月汇总收支（SUM + GROUP BY） |
| `getDailySummaryByMonth(String)` | `yearMonth` — 格式 `yyyy-MM` | `LiveData<List<DailySummary>>` | 按日汇总月内每天收支（用于日历） |

### SQL 说明
- 所有查询以毫秒时间戳存储，通过 `datetime(dateTime / 1000, 'unixepoch', 'localtime')` 转换为**本地**日期
- `strftime('%Y-%m', ...)` 提取年月用于分组
- `date(...)` 提取日期用于单日查询
- `CASE WHEN type = 1 THEN amount ELSE 0 END` 区分收入/支出做 SUM

---

## 6. AppDatabase

**路径**: `com.transsion.ledger.data.db.AppDatabase`  
**父类**: `RoomDatabase`  
**职责**: Room 数据库单例，管理连接生命周期。

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `transactionDao()` | — | `TransactionDao` | 抽象方法，Room 编译时生成实现，获取交易 DAO |
| `accountDao()` | — | `AccountDao` | 抽象方法，获取账户 DAO |
| `getInstance(Context)` | `context` — 应用 Context | `AppDatabase` (static) | **双重检查锁单例**，数据库名 `ledger.db`，`addMigrations(MIGRATION_1_2)`（✅ 已用正式迁移，**不再**破坏性迁移，数据不丢失） |

### 关键配置
- `version = 2` — 当前数据库版本（v1→v2 已迁移）
- `entities = {Transaction.class, Account.class}`
- `exportSchema = false` — 不导出 schema 文件
- `MIGRATION_1_2`（`static final Migration`）：① `ALTER TABLE transactions ADD COLUMN accountId INTEGER NOT NULL DEFAULT 0` ② `CREATE TABLE accounts (...)` ③ 插入一条「默认账户」

---

## 7. TransactionRepository

**路径**: `com.transsion.ledger.data.repository.TransactionRepository`  
**职责**: 数据仓库抽象层，封装 Room DAO（含 `AccountDao`），用单线程池异步执行写操作，对外暴露 LiveData。**写操作会联动调整关联账户余额**（收入 +金额 / 支出 −金额）。

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `TransactionRepository(Application)` | `application` — Application 实例 | — | 构造：获取 DB 实例 + `TransactionDao` + `AccountDao`，初始化单线程 Executor |
| `getAll()` | — | `LiveData<List<Transaction>>` | 透传 DAO.getAll() |
| `getByYearMonth(String)` | `yearMonth` — `yyyy-MM` | `LiveData<List<Transaction>>` | 透传 DAO.getByYearMonth() |
| `getByDate(String)` | `date` — `yyyy-MM-dd` | `LiveData<List<Transaction>>` | 透传 DAO.getByDate() |
| `getByDateRange(long, long)` | start/end 毫秒 | `LiveData<List<Transaction>>` | 透传 DAO.getByDateRange() |
| `getMonthSummary(String)` | `yearMonth` | `LiveData<MonthSummary>` | 透传 DAO.getMonthSummary() |
| `getDailySummaryByMonth(String)` | `yearMonth` | `LiveData<List<DailySummary>>` | 透传 DAO.getDailySummaryByMonth() |
| `insert(Transaction, Runnable)` | `transaction`, `onComplete` | `void` | **异步** + `runInTransaction`：解析账户→调余额→`dao.insert` |
| `update(Transaction)` | `transaction` | `void` | **异步** + 事务：冲销旧账→应用新账→`dao.update` |
| `delete(Transaction)` / `deleteById(long)` | — | `void` | **异步** + 事务：冲销余额→删除 |
| `signedAmount(Transaction)` (private) | `t` | `double` | 收入返回 `+amount`，支出返回 `-amount` |
| `resolveAccount(long)` (private) | `accountId` | `Account` | `accountId>0` 取指定账户，否则回落 `getDefaultSync()` 默认账户 |
| `applyBalance(Account, double)` (private) | `account`, `delta` | `void` | `account.balance += delta` 并 `accountDao.update` |

---

## 8. TransactionViewModel

**路径**: `com.transsion.ledger.viewmodel.TransactionViewModel`  
**父类**: `AndroidViewModel`（持有 Application 引用）  
**职责**: UI 层唯一数据入口，封装 Repository，Fragment 通过 `ViewModelProvider` 获取。

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `TransactionViewModel(Application)` | `application` | — | 构造：初始化 Repository |
| `getAll()` | — | `LiveData<List<Transaction>>` | 获取全部记录 |
| `getByYearMonth(String)` | `yearMonth` — `yyyy-MM` | `LiveData<List<Transaction>>` | 按年月查询 |
| `getByDate(String)` | `date` — `yyyy-MM-dd` | `LiveData<List<Transaction>>` | 按日期查询 |
| `getByDateRange(long, long)` | `startMillis`, `endMillis` | `LiveData<List<Transaction>>` | 按时间戳范围查询（图表页） |
| `getMonthSummary(String)` | `yearMonth` | `LiveData<MonthSummary>` | 获取月度收支汇总 |
| `getDailySummaryByMonth(String)` | `yearMonth` | `LiveData<List<DailySummary>>` | 获取月内每日汇总 |
| `insert(Transaction)` | `transaction` | `void` | 插入记录（无回调） |
| `insert(Transaction, Runnable)` | `transaction`, `onComplete` | `void` | 插入记录（带回调，用于 Toast 提示） |
| `update(Transaction)` | `transaction` | `void` | 更新记录 |
| `delete(Transaction)` | `transaction` | `void` | 删除记录 |
| `deleteById(long)` | `id` | `void` | 按 ID 删除 |

---

## 9. AccountsFragment

**路径**: `com.transsion.ledger.ui.accounts.AccountsFragment`  
**父类**: `Fragment`  
**职责**: 账户管理页——顶部 **活期/资产/全部** 筛选（默认活期）+ 列表 CRUD / 设为默认。

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `onViewCreated(View, Bundle)` | `view`, `savedInstanceState` | `void` | `toggle_account_filter` 筛选；`applyFilter()` 过滤 `allAccounts` |
| `switchFilterToType(String)` (private) | `accountType` | `void` | 新增账户保存后自动切到活期/资产视图 |
| `applyFilter()` (private) | — | `void` | 按 `currentFilter` 刷新列表 |
| `showLongPressMenu(Account)` (private) | `a` — 账户 | `void` | 长按弹菜单：设为默认 / 编辑 / 删除（删除二次确认） |
| `showEditDialog(Account)` (private) | `existing` — null=新增 | `void` | 动态构建表单（名称*/余额*/类型 Spinner[活期·资产]/卡号/备注 + 6 个 CheckBox 开关）→ 校验名称/余额必填 → `insert()` 或 `update()` |
| `label(String)` (private) | `text` | `TextView` | 生成表单字段灰色小标题 |
| `format(double)` (private) | `v` | `String` | 整数去尾格式化金额 |

### 内部类 AccountAdapter

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `setAccounts(List<Account>)` | `list` | `void` | 设置数据并刷新 |
| `onCreateViewHolder(ViewGroup, int)` | `parent`, `viewType` | `VH` | 加载 `item_account.xml` |
| `onBindViewHolder(VH, int)` | `holder`, `position` | `void` | 渲染名称/余额/类型/备注 + 默认徽标；点击→编辑，长按→菜单 |
| `getItemCount()` | — | `int` | 账户数 |

---

## 10. BillsFragment

**路径**: `com.transsion.ledger.ui.bills.BillsFragment`  
**父类**: `Fragment`  
**职责**: 账单列表页——**顶部预算/收支汇总栏（随滚动切换月份）** + **全部账单** RecyclerView（按月分段 + 按日分组）。

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `onViewCreated(View, Bundle)` | `view`, `savedInstanceState` | `void` | `getAll()` 加载全部账单；`OnScrollListener`→`syncHeaderToVisibleMonth`；单月汇总 LiveData **切换前先 removeObserver** |
| `syncHeaderToVisibleMonth(LinearLayoutManager)` (private) | `lm` | `void` | `adapter.getYearMonthForPosition(firstVisible)`→`bindHeaderMonth` |
| `bindHeaderMonth(String)` (private) | `yearMonth` | `void` | 更新 `txt_month_label`、`budgetVM.loadBudget`、换绑 `getMonthSummary` |
| `showBudgetDialog()` (private) | — | `void` | 对**当前顶部显示月份** `headerYearMonth` 设预算 |

> 跳日历用 `bottomNav.setSelectedItemId(R.id.nav_calendar)`（驱动底部导航，**非** `NavController.navigate`），保证导航选中项同步，避免点回「账单」时卡在日历。`btn_goto_calendar` 现为带「📅 + 日历」小文字的 `LinearLayout`。
| `formatAmount(double)` (private) | `v` | `String` | 整数去尾格式化 |

### 调用链
```
Room 数据变化 → LiveData 通知 → adapter.setTransactions()
  → 按**日**分组 → HeaderVH(日汇总) + ItemVH 渲染
  → 用户点击条目 → tapListener → Navigation 跳转 TransactionEditFragment
  → 用户左滑 → ItemTouchHelper.onSwiped()
    → 恢复视图 + deleteListener(tx, pos)
      → AlertDialog 确认
        → 确认: viewModel.delete(tx) + adapter.removeItemAt(pos)
        → 取消: adapter.notifyItemChanged(pos)
```

---

## 11. BillAdapter

**路径**: `com.transsion.ledger.ui.bills.BillAdapter`  
**父类**: `RecyclerView.Adapter<ViewHolder>`  
**职责**: 账单适配器：账单页为**月分段 + 日分组 + 条目**；日历页 `setDisplayMode(false,false)` 扁平列表。

| 方法 | 作用 |
|------|------|
| `setShowMonthSections(true)` | 账单页插入 `item_bill_month_section` 月份头 |
| `getYearMonthForPosition(int)` | 向上查找可见项所属 `yyyy-MM`，供顶部汇总联动 |

### 内部类

| 内部类 | 作用 |
|--------|------|
| `Item` | 数据模型：`isHeader` 标记 + Header 字段（`headerLabel`/`dayIncome`/`dayExpense`）或 Item 字段（`transaction`） |
| `HeaderVH` | Header ViewHolder：日期标题（`M月d日 EEEE`）+ 当日收/支汇总（复用 `item_month_header` 布局的 `txt_month`/`txt_income`/`txt_expense`） |
| `ItemVH` | Item ViewHolder：**emoji 图标** + 子项目名 + 时间 + 财务四分类标签（仅支出显示）+ 金额（`-`红/`+`绿） |
| `OnDeleteListener` | 删除回调接口 `onDelete(Transaction, position)` |
| `OnItemTapListener` | **点击回调接口** `onItemTap(Transaction)`（点击条目进入编辑） |

> **日期格式**：`dateGroupFmt = "yyyy-MM-dd"`（分组键）、`dateHeaderFmt = "M月d日 EEEE"`（Header 显示）、`timeFmt = "HH:mm"`（条目时间）。

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `setDisplayMode(boolean, boolean)` | `withDayHeaders`, `itemClickable` | `void` | 日历模式：无日期头、不可点击 |
| `setOnDeleteListener(OnDeleteListener)` | `listener` | `void` | 注册删除回调 |
| `setOnItemTapListener(OnItemTapListener)` | `listener` | `void` | 注册点击编辑回调 |
| `setTransactions(List<Transaction>)` | `transactions` — 全量数据 | `void` | **核心方法**：遍历交易列表，按 `yyyy-MM-dd`（**日**）分组，每组前插入 Header（含**当日**收/支汇总），刷新 Adapter |
| `getItemViewType(int)` | `position` | `int` | 返回 `TYPE_HEADER(0)` 或 `TYPE_ITEM(1)` |
| `onCreateViewHolder(ViewGroup, int)` | `parent`, `viewType` | `ViewHolder` | 按 ViewType 加载对应布局（Header=`item_month_header`，Item=`item_transaction`） |
| `onBindViewHolder(ViewHolder, int)` | `holder`, `position` | `void` | 绑定 Header（日期+收支）或 Item（emoji 取自 `AddTransactionSheet.CAT_EMOJI`，缺省 `💰`；点击 → `tapListener.onItemTap`） |
| `getItemCount()` | — | `int` | 返回 Item 总数 |
| `getTransactionAt(int)` | `position` | `Transaction` (可 null) | 获取指定位置对应的 Transaction（Header 位置返回 null） |
| `getSwipeCallback()` | — | `ItemTouchHelper.SimpleCallback` | **返回左滑删除配置**：① Header 不可滑动（`getSwipeDirs` 返回 0）② `onSwiped` → 恢复视图 + 通知 `OnDeleteListener`（带 position） ③ `onChildDraw` — 滑动渐隐效果 |
| `removeItemAt(int)` | `position` — 列表索引 | `void` | **确认删除后**由 Fragment 调用，实际移除 item 并刷新 |

---

## 12. CalendarFragment

**路径**: `com.transsion.ledger.ui.calendar.CalendarFragment`  
**父类**: `Fragment`  
**职责**: 日历页——月历 + 当日明细。当月自动选**今天**；其他月自动选**最早有账日**；点击标题选年月。

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `onViewCreated(View, Bundle)` | `view`, `savedInstanceState` | `void` | `OnMonthChangedListener` + 标题 `showMonthPickerDialog` |
| `loadMonth(String)` | `yearMonth` | `void` | 月汇总 → `resolveAutoSelectDate` → `loadDay` 或空态 |
| `resolveAutoSelectDate(String, List)` (private) | `yearMonth`, `summaries` | `String` | 当月→今天；非当月→最早有收入/支出的日期 |
| `showMonthPickerDialog(String)` (private) | `currentYearMonth` | `void` | NumberPicker 年+月 → `jumpToYearMonth` |
| `loadDay(String)` | `date` — `yyyy-MM-dd` | `void` | 观察当日记录，高亮选中格 |
| `onDayClicked(String)` | `date` 或 `null` | `void` | `null`=换月；否则加载当日列表 |

---

## 13. MonthCalendarView

**路径**: `com.transsion.ledger.ui.calendar.MonthCalendarView`  
**父类**: 无（纯 Java 类）  
**职责**: 月历网格逻辑（非 Android View），操作 `view_calendar.xml` 中的 GridLayout。

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `MonthCalendarView(View)` | `rootView` — `view_calendar.xml` 的根 | — | 构造：绑定 `grid_days`、`txt_month_title`、前后月按钮，初始化当月 |
| `setOnDayClickListener(OnDayClickListener)` | `listener` | `void` | 注册日期点击回调 |
| `setDailyExpenses(Map<String,Double>)` | `expenses` — key=`yyyy-MM-dd`, value=支出金额 | `void` | 接收每日支出数据，刷新网格 |
| `getCurrentYearMonth()` | — | `String` (`yyyy-MM`) | 获取当前显示的年月 |
| `prevMonth()` (private) | — | `void` | 月份 -1，跨年处理，重建网格，通知 listener 月份变了 |
| `nextMonth()` (private) | — | `void` | 月份 +1，跨年处理，重建网格，通知 listener 月份变了 |
| `updateTitle()` (private) | — | `void` | 更新标题为 `yyyy年M月` |
| `buildGrid()` (private) | — | `void` | 清空网格 → 计算首日星期偏移 → 填充空 cell → 逐日生成 `createDayCell()` |
| `createEmptyCell(Context)` | `ctx` | `View` | 创建空白占位 cell |
| `createDayCell(Context, int, Double, boolean)` | `ctx`, `day`(日期号), `expense`(可null), `isToday` | `View` | 创建日期 cell：今日蓝色高亮 + 日期号 + 支出金额（红色，有则显示） |

### 内部接口 OnDayClickListener
| 方法 | 参数 | 作用 |
|------|------|------|
| `onDayClick(String)` | `date` — `yyyy-MM-dd` 或 null（月份切换时） | 日期点击回调 |

---

## 14. AddTransactionSheet

**路径**: `com.transsion.ledger.ui.add.AddTransactionSheet`  
**父类**: `BottomSheetDialogFragment`  
**职责**: 记账弹窗——三级递进面板（合并页[收支+类别] → 子项目 → 记账表单）。

### 核心状态字段

| 字段 | 类型 | 含义 |
|------|------|------|
| `type` | `int` | 0=支出(默认) / 1=收入，**合并页左上角切换** |
| `selectedCategory1` | `String` | 当前选择的一级分类 |
| `selectedCategory2` | `String` | 当前选择的二级子项目 |
| `category3` | `int` | -1=未选 / 0=维持 / 1=消费 / 2=提升 / 3=社交 |
| `selectedDate` | `Calendar` | 选择的日期 + NumberPicker 时/分 |
| `amountBuilder` | `StringBuilder` | 金额输入缓冲区 |
| `editMode` / `editId` | `boolean` / `long` | 编辑模式标记 + 被编辑记录的ID |
| `pendingEdit` | `Transaction` | **编辑模式暂存**：`setEditTransaction()` 时可能 view 未创建，先存这里，待 `onViewCreated` 末尾再 `applyEditData()` 应用 |
| `pickerHour` / `pickerMinute` | — | **已删除**，改为弹窗内临时创建三列 NumberPicker |

### 分类常量

| 常量 | 说明 |
|------|------|
| `CAT1_EXPENSE[]` | 支出 8 类（**含 emoji 前缀**）：`🍚 吃`/`🏠 住`/`🎮 娱`/`📚 教育`/`🚗 交通`/`🛒 购物`/`🏥 医疗`/`📌 其他` |
| `CAT1_INCOME[]` | 收入 6 类（**含 emoji 前缀**）：`💼 工资`/`📈 投资`/`💻 兼职`/`↩️ 退款`/`🎁 礼金`/`📌 其他` |
| `SUBS_EXPENSE` | 支出子项目 Map（key 为带 emoji 的一级分类） |
| `SUBS_INCOME` | 收入子项目 Map（key 为带 emoji 的一级分类） |
| `CAT_EMOJI` (`public static`) | **emoji 映射表**：带文字分类 → 纯 emoji，供 `BillAdapter` 取图标用 |

> ⚠️ 一级分类字符串本身已带 emoji（如 `"🍚 吃"`），`category1` 入库即为该值。

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `onCreateDialog(Bundle)` | `savedInstanceState` | `BottomSheetDialog` | 创建 BottomSheet，peekHeight=**42%** 屏幕（`SHEET_HEIGHT_RATIO`，长屏下过 30% 会过挤）并设置 `bottomSheet` 高度，`skipCollapsed=true`，**`draggable=false`** |
| `onCreateView(...)` | 标准 | `View` | 加载 `sheet_add_transaction.xml` |
| `onViewCreated(View, Bundle)` | `view`, `savedInstanceState` | `void` | 初始化 ViewModel、日期、绑定 View、构建数字键盘、设置事件，默认支出类别网格，进入 level=1；**末尾若 `pendingEdit != null` 则调用 `applyEditData()` 并清空** |
| `bindViews(View)` (private) | `view` | `void` | 绑定 level01/level2_scroll/level3/level3_body、`grid_categories`/`grid_sub`、numpadContainer 等 |
| `showDatePickerPanel()` (private) | — | `void` | 隐藏 `level3_body`，**占满 Level3** 展示内嵌月历（`view_calendar` + `buildSheetCalendarGrid()`）；点日期即选中并关闭 |
| `showTimePickerPanel()` (private) | — | `void` | 隐藏 `level3_body`，**占满 Level3** 展示时分老虎机滚轮（`NumberPicker` 时/分）；确定写回 |
| `hidePickerPanels()` (private) | — | `void` | 关闭日期/时分面板，恢复 `level3_body`（含 4×4 键盘），刷新 `text_date`/`text_time` |
| `showNoteEditor()` (private) | — | `void` | 底部弹起备注栏 + 遮罩，不挤压主界面 |
| `dockNotePanel()` (private) | — | `void` | 键盘收起后备注栏停靠底部 |
| `collapseNotePanel()` (private) | — | `void` | 收起备注栏并 `restoreSheetLayout()` 恢复弹窗高度 |
| `restoreSheetLayout()` (private) | — | `void` | 键盘收起后强制恢复 BottomSheet 固定高度，避免 Level 3 错位 |
| `buildSubCategoryGrid(List)` (private) | `items` | `void` | Level 2：4 列网格渲染子项目，样式与 Level 1 `createGridCell()` 一致 |
| `createGridCell(String,String,Runnable)` (private) | `emoji`, `name`, `onClick` | `LinearLayout` | 生成圆角 emoji+文字小模块格 |
| `buildSheetCalendarGrid()` (private) | — | `void` | 在记账弹窗内绘制月历格，高亮今日/选中 |
| `buildNumpad()` (private) | — | `void` | 动态生成 **4×4** 键盘（`1-9 0 . + - C 确认`），确认键 `onConfirm()` |
| `updateAmountDisplay()` (private) | — | `void` | 刷新摘要金额（支出红/收入绿，支持 `-` 键切换负号显示） |
| `wrapPicker(NumberPicker, String)` (private) | `picker`, `label` | `LinearLayout` | 时分滚轮包装 |
| `onTypeToggle(int)` (private) | `t` — 0=支出/1=收入 | `void` | 收支切换；Level 2/3 时**点当前或另一类型均回 Level 1**（另一类型会先 `selectType`）；Level 1 仅类型变化时切换 |
| `selectType(int)` (private) | `t` — 0=支出/1=收入 | `void` | 刷新按钮样式 + 类别网格 + `applyFormLayoutWeights()` |
| `placeTypeToggle(boolean)` (private) | `compactInHeader` | `void` | `false`=Level 1 全宽条；`true`=缩至 `header_type_slot`（返回键右侧） |
| `applyFormLayoutWeights()` (private) | — | `void` | 支出 15/8/10/67；收入 18/12/70 比例分配摘要/财务分类/日期行/键盘（weight 合计 100） |
| `buildCategoryGrid(String[])` (private) | `labels` — 分类数组 | `void` | 动态生成 4 列分类网格；每格为**竖排卡片**（圆角底 + 上方大 emoji + 下方文字），按空格拆分 `"🍚 吃"` 分两行显示，避免单行省略号；自动计算 `rowCount` |
| `onCategorySelected(String)` (private) | `category` — 分类名 | `void` | 选中分类 → `buildSubCategoryGrid()` → 进入 level=2 子项目网格 |
| `onSubSelected(String)` (private) | `sub` — 子项目名 | `void` | 选中子项目 → 「自定义」弹出 AlertDialog 输入框 → 否则 `showForm()` |
| `showForm()` (private) | — | `void` | 进入 level=3（`level3_body` 含表单与 4×4 键盘一体布局） |
| `setupListeners()` (private) | — | `void` | 绑定返回键、收支切换按钮、`textDateTime`→三列滚轮、财务四选一、确认按钮 |
| `updateDateTimeDisplay()` (private) | — | `void` | 刷新 `text_date`（`yyyy年MM月dd日`）与 `text_time`（`HH时mm分`） |
| `buildNumpad()` (private) | — | `void` | 动态生成 3×4 数字键盘（1-9、0、`.`、`⌫`），插入 `numpadContainer`（固定在弹窗底部） |
| `onNumClick(String)` (private) | `digit` — 数字或 `.` | `void` | 数字输入：处理首位0替换、防止双小数点 |
| `onBackspace()` (private) | — | `void` | 退格：删末尾字符，剩空恢复 `0` |
| `selectCategory3(int)` (private) | `idx` — 0-3 | `void` | 高亮选中财务分类（维持蓝/消费橙/提升紫/社交青），取消其他 |
| `onConfirm()` (private) | — | `void` | **核心提交**：校验金额可解析且 >0、**仅支出时**校验财务分类已选（`category3==-1 && type==0` 拦截）→ 构造 Transaction（`accountId=0` 默认账户）→ **editMode 时**调用 `viewModel.update()`，**否则** `viewModel.insert()` → Toast + dismiss |
| `setEditTransaction(Transaction)` (public) | `t` — 待编辑记录 | `void` | **编辑模式入口（外部调用）**：仅把 `t` 暂存到 `pendingEdit`（此时 view 可能未创建），真正填充在 `onViewCreated` 里由 `applyEditData()` 完成 |
| `applyEditData(Transaction)` (private) | `t` — 待编辑记录 | `void` | **实际预填**：设 `editMode/editId`，回填 type/category1-3/amount/date/note，按钮高亮，确认按钮改「保存修改」、标题「编辑记录」，跳 Level 1 |
| `formatAmount(double)` (private) | `v` — 金额 | `String` | 整数去掉小数尾（`38.0→"38"`），否则原样字符串 |
| `showLevel(int)` (private) | `lvl` — 1/2/3 | `void` | 切换三级面板可见性；level=3 时 `content_scroll` 与 `level3` 互斥；level=1 显示返回键、标题「选择类别」 |
| `onBack()` (private) | — | `void` | 返回：3→2(隐藏键盘)，2→1(隐藏返回键)，1→dismiss |

### 交互流程
```
点击 + → BottomSheet 弹出(Level 1 合并页，固定约 42% 高度，禁止拖拽)
  → 默认支出类别网格，左上角可切换「收入」→ 网格即时刷新为收入类别
  → 点类别 → Level 2：子项目 4 列网格（可滚动）
    → 点子项目 → Level 3：表单 + 底部键盘
      → 输金额(底部键盘) → 选财务分类 → 点击日期时间(弹出三列滚轮弹窗：月日周/时/分) → 输备注 → 确认
        → Room 写入 → LiveData 通知 → 全自动刷新
```

---

## 15. ProfileFragment

**路径**: `com.transsion.ledger.ui.profile.ProfileFragment`  
**父类**: `Fragment`  
**职责**: 我的页面——用户卡片 + 功能列表（💰预算设置 / 📤数据导出 / ℹ️关于），**预算设置已实现**，其余为预留入口。

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `onCreateView(LayoutInflater, ViewGroup, Bundle)` | 标准 | `View` | 加载 `fragment_profile.xml` |
| `onViewCreated(View, Bundle)` | `view`, `savedInstanceState` | `void` | 绑定 `btn_budget_setting`，点击 → `showBudgetDialog()` |
| `showBudgetDialog()` (private) | — | `void` | 弹 AlertDialog：读取当月 `budget_prefs` 中 `budget_yyyy-MM` 旧值预填 → 输入月预算 → 校验 ≥0 → 写回 SharedPreferences |
| `formatAmount(double)` (private) | `v` — 金额 | `String` | 整数去尾（与 AddTransactionSheet 同逻辑），用于预填输入框 |

> 注：当前预算读写直接用 `SharedPreferences("budget_prefs")`，与 `BudgetStorage`/`BudgetViewModel` 同一套 key 约定（`budget_yyyy-MM`）。

---

## 16. TransactionEditFragment

**路径**: `com.transsion.ledger.ui.edit.TransactionEditFragment`  
**职责**: 账单条目问卷式全字段编辑页；支持已有记录编辑与**语音补全新建**（`voiceDraft` Bundle）。必填项标题带 `*`，未填不可提交。进入时隐藏底部导航。

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `onViewCreated(View, Bundle)` | `view`, `savedInstanceState` | `void` | `voiceDraft`→`bindDraft()` 补全模式；否则 `transactionId`→`getById()` 填表 |
| `bindDraft(VoiceDraft)` (private) | `draft` | `void` | 语音草稿预填表单；一级分类 Spinner + **子项目 Spinner**（联动 `CategoryCatalog`） |
| `refreshCategory2Spinner(String)` (private) | `currentValue` | `void` | 按当前一级分类加载子项目列表（细则） |
| `saveTransaction()` (private) | — | `void` | 校验必填（含支出财务分类 `*`）→ 补全模式 `insert` / 编辑模式 `update` |
| `confirmDelete()` (private) | — | `void` | 确认后 `viewModel.delete()`（补全模式不显示） |

---

## 17. VoiceInputSheet

**路径**: `com.transsion.ledger.ui.voice.VoiceInputSheet`  
**父类**: `DialogFragment`  
**职责**: 语音记账入口（长按底部 `+`）。用户在 `EditText` 内用**系统输入法自带语音转文字**或手动输入 →「解析预览」→「确认并记账」。不申请 `RECORD_AUDIO`。

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `setListener(Listener)` | `listener` | `void` | 设置 `onVoiceResult(VoiceDraft)` 回调（`MainActivity`） |
| `onViewCreated(View, Bundle)` | `view`, `savedInstanceState` | `void` | 绑定示例句切换、输入监听、自动弹出软键盘 |
| `runParsePreview()` (private) | — | `void` | `VoiceTransactionParser.parse` + `formatPreview`，启用确认按钮 |
| `confirmAndSubmit()` (private) | — | `void` | 回调 `Listener` 并 `dismiss()` |
| `clearInput()` (private) | — | `void` | 清空输入与预览状态 |

> 备研：`VoiceInputMicLegacy`、`SpeechRecognitionHelper`、`VoicePermissionHelper`、`MainActivity` 内麦克风权限块（注释）保留供后续研究。

---

## 18. BudgetViewModel

**路径**: `com.transsion.ledger.viewmodel.BudgetViewModel`  
**父类**: `AndroidViewModel`  
**职责**: 预算 ViewModel，持有 `BudgetStorage`，对外暴露「预算金额」「剩余预算」两个 LiveData。**不依赖 Repository / Room**。

### 字段

| 字段 | 类型 | 含义 |
|------|------|------|
| `budgetStorage` | `BudgetStorage` | 预算持久化（SharedPreferences） |
| `budgetAmount` | `MutableLiveData<Double>` | 当前月预算金额（默认 0.0） |
| `budgetRemaining` | `MutableLiveData<Double>` | 剩余预算（默认 0.0） |

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `BudgetViewModel(Application)` | `application` | — | 构造：初始化 `BudgetStorage` |
| `getBudgetRemaining()` | — | `LiveData<Double>` | 观察剩余预算 |
| `getBudgetAmount()` | — | `LiveData<Double>` | 观察预算金额 |
| `loadBudget(String)` | `yearMonth` — `yyyy-MM` | `void` | 从存储读取该月预算，`postValue` 到 amount/remaining（初始未扣支出） |
| `applyMonthSummary(MonthSummary)` | `summary` | `void` | 用 `budgetAmount - summary.totalExpense` 计算并 `postValue` 剩余预算 |
| `setBudget(String, double)` | `yearMonth`, `amount` | `void` | 写入存储并刷新 `budgetAmount` |

---

## 19. BudgetStorage

**路径**: `com.transsion.ledger.data.BudgetStorage`  
**职责**: 预算持久化层，基于 **SharedPreferences** 按月存储（**不入 Room**）。

| 常量 | 值 |
|------|-----|
| `PREFS_NAME` | `budget_prefs` |
| `KEY_PREFIX` | `budget_`（拼成 `budget_yyyy-MM`） |

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `BudgetStorage(Context)` | `context` | — | 构造：获取 `budget_prefs` SharedPreferences |
| `getBudget(String)` | `yearMonth` — `yyyy-MM` | `double` | 读取某月预算，未设置返回 `0` |
| `setBudget(String, double)` | `yearMonth`, `amount` | `void` | 写入某月预算（存为字符串） |

---

## 20. Account (Entity)

**路径**: `com.transsion.ledger.data.entity.Account`  
**Room 表名**: `accounts`（**DB v2 新增**）  
**职责**: 账户/账本实体。空构造给 Room，带参构造标 `@Ignore`。

| 字段 | 类型 | 含义 |
|------|------|------|
| `id` | `long` (PK, autoGen) | 主键 |
| `name` | `String` | 账户名称（必填） |
| `balance` | `double` | 余额（必填） |
| `type` | `String` | 类型：`活期` / `资产` |
| `isActive` | `boolean` | 是否启用 |
| `includeInNetWorth` | `boolean` | 是否计入净资产 |
| `canExpense` / `canIncome` | `boolean` | 是否可支出 / 可收入 |
| `canTransferIn` / `canTransferOut` | `boolean` | 是否可转入 / 可转出 |
| `isDefault` | `boolean` | 是否默认账户 |
| `cardNumber` | `String` | 卡号（选填） |
| `note` | `String` | 备注（选填） |

> 方法：全套 getter/setter（布尔型用 `isXxx()`/`setXxx()`，如 `isDefault()`/`setDefault()`、`isCanExpense()`/`setCanExpense()`）。

---

## 21. AccountDao (DAO)

**路径**: `com.transsion.ledger.data.dao.AccountDao`  
**职责**: 账户 Room 数据访问对象。

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `insert(Account)` | `account` | `long` | 插入，返回新 ID |
| `update(Account)` | `account` | `void` | 更新 |
| `delete(Account)` | `account` | `void` | 删除 |
| `getAll()` | — | `LiveData<List<Account>>` | 全部账户，默认账户置顶（`isDefault DESC, id ASC`） |
| `getDefault()` | — | `LiveData<Account>` | 默认账户（可观察） |
| `getDefaultSync()` | — | `Account` | 默认账户（**同步**，供后台线程取） |
| `clearDefaults()` | — | `void` | 全部置非默认（`UPDATE ... SET isDefault=0`） |
| `setDefault(long)` | `id` | `void` | 指定账户置默认 |
| `getById(long)` | `id` | `Account` | 按 ID 同步取 |

---

## 22. AccountRepository

**路径**: `com.transsion.ledger.data.repository.AccountRepository`  
**职责**: 账户数据仓库，封装 `AccountDao` + 单线程 Executor（写操作异步）。

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `AccountRepository(Application)` | `application` | — | 取 DB+DAO，建单线程池 |
| `getAll()` / `getDefault()` | — | `LiveData<...>` | 透传查询 |
| `getAllSync()` | — | `List<Account>` | 同步读全部账户（后台线程调用） |
| `fetchAll(Consumer<List<Account>>)` | `onResult` | `void` | 线程池内 `getAllSync` 后回调，供语音入账等一次性场景 |
| `getDefaultSync()` / `getById(long)` | — / `id` | `Account` | 透传同步查询 |
| `insert(Account, Runnable)` | `account`, `onComplete` | `void` | 异步插入 + 回调 |
| `update(Account)` | `account` | `void` | 异步更新 |
| `deleteSafe(Account)` | `account` | `boolean` | 事务：禁止删默认；余额并入默认 + `reassignAccount` + 删账户 |
| `delete(Account, Consumer<Boolean>)` | `account`, `onResult` | `void` | 异步包装 `deleteSafe` |
| `setAsDefault(long)` | `id` | `void` | `runInTransaction`：`clearDefaults` → `setDefault` |

---

## 23. AccountViewModel

**路径**: `com.transsion.ledger.viewmodel.AccountViewModel`  
**父类**: `AndroidViewModel`  
**职责**: 账户 UI 层数据入口，透传 `AccountRepository`。

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `AccountViewModel(Application)` | `application` | — | 构造：初始化 Repository |
| `getAll()` / `getDefault()` | — | `LiveData<...>` | 账户列表 / 默认账户 |
| `getDefaultSync()` / `getById(long)` | — / `id` | `Account` | 同步查询 |
| `insert(Account, Runnable)` | `account`, `onComplete` | `void` | 新增账户 |
| `update(Account)` | `account` | `void` | 更新 |
| `delete(Account, Consumer<Boolean>)` | `account`, `onResult` | `void` | 安全删除（回调是否成功） |
| `setAsDefault(long)` | `id` | `void` | 设为默认账户 |

---

## 24. StatisticsFragment / StatisticsHelper / 图表 View

**路径**: `com.transsion.ledger.ui.stats.*`  
**职责**: 账单页「📊 图表」入口对应的收支统计页——饼图分类占比 + 可折叠层级明细 + 月度折线趋势。

### StatisticsFragment

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `onCreateView(...)` | 标准 | `View` | 加载 `fragment_statistics.xml` |
| `onViewCreated(View, Bundle)` | `view`, `savedInstanceState` | `void` | 绑定收支/饼图范围/折线范围 Toggle；观察近 1 年 `getByDateRange()`；刷新图表 |
| `onResume()` / `onPause()` | — | `void` | 隐藏/恢复底部导航 |
| `refreshCharts()` (private) | — | `void` | 按饼图时间范围聚合树 + 更新饼图与折叠列表 |
| `refreshLineChart()` (private) | — | `void` | 按折线时间范围生成月度序列 |
| `rebuildBreakdown()` (private) | — | `void` | 根据 `StatNode.expanded` 动态填充 `layout_breakdown` |
| `appendNode(...)` (private) | `parent`, `node`, `depth` | `void` | 递归渲染折叠行（▶/▼ 展开子级） |

### StatisticsHelper

| 方法 | 作用 |
|------|------|
| `TimeRange` 枚举 | `MONTH`(当月) / `HALF_YEAR`(近 6 月) / `YEAR`(近 12 月) |
| `rangeStartMillis(TimeRange)` | 范围起始（当月 1 日 0 点向前推） |
| `rangeEndMillis()` | 范围结束（今日 23:59:59） |
| `buildBreakdownTree(transactions, type, start, end)` | 支出三级树（cat3→cat1→cat2），收入两级（cat1→cat2）；类内 `percent` 合计 100% |
| `sumInRange(...)` | 范围内指定类型金额合计 |
| `buildMonthlySeries(...)` | 按月桶汇总，供折线图 |
| `buildMonthLabels(TimeRange)` | X 轴「M月」标签 |

### PieChartView / LineChartView / StatNode

| 类 | 作用 |
|----|------|
| `StatNode` | 树节点：`label`/`amount`/`percent`/`color`/`expanded`/`children` |
| `PieChartView` | `setData(List<StatNode>)` 绘制环形饼图，中心显示合计 |
| `LineChartView` | `setData(labels, values)` + `setLineColor()` 绘制折线趋势 |

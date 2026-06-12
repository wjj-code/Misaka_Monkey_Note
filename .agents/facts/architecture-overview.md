# 架构总览

## 分层架构

```
┌─────────────────────────────────────────────────────┐
│  UI Layer (Fragment / Activity / Adapter)           │
│  MainActivity → 4 Fragments + 1 BottomSheet         │
├─────────────────────────────────────────────────────┤
│  ViewModel Layer                                    │
│  TransactionViewModel (AndroidViewModel)            │
├─────────────────────────────────────────────────────┤
│  Repository Layer (数据抽象层)                       │
│  TransactionRepository / AccountRepository          │
│  ┌── 当前: Room 本地 SQLite                         │
│  └── 未来: 云端 API（替换 Repository 实现即可）      │
├─────────────────────────────────────────────────────┤
│  Data Layer                                         │
│  Room DB → DAO → Entity / POJO                      │
│  BudgetStorage (SharedPreferences，预算独立存储)     │
└─────────────────────────────────────────────────────┘
```

> **注**：交易数据走 Room；**预算**走独立的 `BudgetStorage`（SharedPreferences，按月 key），两条存储路径互不耦合。`BudgetViewModel` 不依赖 Repository。

## 包结构

```
com.transsion.ledger/
├── MainActivity.java                    # 唯一 Activity，持有 NavController + BottomNav
├── data/
│   ├── entity/
│   │   ├── Transaction.java             # 记账记录 Entity (Room，含 accountId)
│   │   ├── Account.java                 # 账户 Entity (Room，v2 新增)
│   │   ├── DailySummary.java            # 每日汇总 POJO
│   │   └── MonthSummary.java            # 月度汇总 POJO
│   ├── dao/
│   │   ├── TransactionDao.java          # 交易 Room DAO 接口
│   │   └── AccountDao.java              # 账户 Room DAO 接口
│   ├── db/
│   │   └── AppDatabase.java             # Room Database 单例（v2 + MIGRATION_1_2）
│   ├── repository/
│   │   ├── TransactionRepository.java   # 交易数据仓库（封装 DAO + IO 线程池）
│   │   └── AccountRepository.java       # 账户数据仓库（封装 DAO + IO 线程池）
│   └── BudgetStorage.java               # 预算持久化（SharedPreferences，按月，不入 Room）
├── util/
│   └── CategoryCatalog.java             # 一级分类 + 子项目（细则）常量（记账/语音/编辑共用）
├── viewmodel/
│   ├── TransactionViewModel.java        # AndroidViewModel，UI 层交易数据入口
│   ├── AccountViewModel.java            # 账户 ViewModel
│   └── BudgetViewModel.java             # 预算 ViewModel（持有 BudgetStorage，算剩余预算）
└── ui/
    ├── accounts/AccountsFragment.java   # 账户管理页（列表 + 增删改 + 设默认 + AccountAdapter）
    ├── bills/
    │   ├── BillsFragment.java           # 账单列表页（📊 跳图表 / 📅 跳日历）
    │   └── BillAdapter.java             # 按【日】分组 + 左滑删除 + 点击编辑 Adapter
    ├── stats/
    │   ├── StatisticsFragment.java      # 收支图表页（饼图 + 折叠明细 + 折线图）
    │   ├── StatisticsHelper.java        # 时间范围与分类聚合
    │   ├── PieChartView.java            # 自定义饼图 View
    │   ├── LineChartView.java           # 自定义折线图 View
    │   └── StatNode.java                # 统计树节点
    ├── edit/
    │   └── TransactionEditFragment.java # 账单问卷式编辑页
    ├── calendar/
    │   ├── CalendarFragment.java        # 日历页（含 DailyDetailAdapter，点击明细可编辑）
    │   └── MonthCalendarView.java       # 月历网格 View（非 Fragment，纯 View 逻辑）
    ├── profile/ProfileFragment.java     # 我的页面（预算设置已实现 + 预留入口）
    ├── add/
    │   └── AddTransactionSheet.java     # 记账 BottomSheet（三级递进：合并页→子项目→表单）
    ├── edit/
    │   └── TransactionEditFragment.java # 编辑/语音补全账单（必填 * 校验；子项目 Spinner）
    └── voice/
        ├── VoiceInputSheet.java         # 语音记账遮罩（系统输入法语音转文字 + 解析预览）
        ├── VoiceTransactionParser.java  # 中文语音/文本字段解析
        ├── VoiceDraft.java              # 草稿 / Bundle 传递
        ├── OptionalFieldsDialog.java    # 选填未填浮动提示
        ├── VoiceInputMicLegacy.java     # 备研：App 内麦克风方案（已停用）
        ├── SpeechRecognitionHelper.java # 备研：SpeechRecognizer 探测（已停用）
        └── VoicePermissionHelper.java   # 备研：RECORD_AUDIO 校验（已停用）
```

## 数据流

```
用户点击「确认记账」
  → AddTransactionSheet.onConfirm()
    → 合并 NumberPicker 时分 → selectedDate
    → TransactionViewModel.insert(tx)
      → TransactionRepository.insert(tx, callback)
        → executor.execute:
            ① resolveAccount(tx.accountId)（回落默认账户，回写真实 id）
            ② 账户余额 += 带符号金额（收入+/支出-）→ accountDao.update
            ③ dao.insert(tx)
          → Room 写入 SQLite（transactions + accounts 均变化）
            → LiveData 自动通知观察者
              → BillsFragment / CalendarFragment / AccountsFragment 自动刷新（含账户余额）
```

## 记账面板流程（v1.0 三级递进，收支+类别合并）

```
Level 1 (合并页): 左上「支出/收入」切换 + 下方类别网格（默认支出）
Level 2: 二级子项目 → 4~7 项 + 自定义输入
Level 3: 记账表单 → 金额(底部键盘) + 财务四分类 + 日期时间(点击弹出三列滚轮) + 备注 → 确认
```

## 版本管理

版本号集中定义于 `gradle.properties`，`build.gradle.kts` 动态读取：
- `APP_VERSION_CODE` → `versionCode`
- `APP_VERSION_NAME` → `versionName`
- 修改日志记录于 `facts/CHANGELOG.md`

## 导航流

```
BottomNavigationView (5 项)
├── 账户 → AccountsFragment（账户管理）
├── 账单 → BillsFragment (startDestination，顶部预算栏 + 列表)
├── ＋   → 拦截，弹出 AddTransactionSheet (BottomSheetDialogFragment)
├── 日历 → CalendarFragment
└── 我的 → ProfileFragment
```
> 账单页顶部「📅」(`btn_goto_calendar`) 用 `Navigation` 主动跳转到日历页。

## 账户 / 预算 子系统

```
账户管理 (Room, accounts 表):
  AccountsFragment → AccountViewModel → AccountRepository → AccountDao → accounts 表
  - 增删改、设默认（setAsDefault = clearDefaults + setDefault 事务）
  - 记账时 accountId 默认 0（默认账户），后续可扩展为选择账户

预算 (SharedPreferences, 不入 Room):
  BillsFragment / ProfileFragment → BudgetViewModel → BudgetStorage(budget_prefs)
  - 按月 key：budget_yyyy-MM
  - 剩余预算 = 预算金额 − 当月 totalExpense（由 MonthSummary 计算）
```

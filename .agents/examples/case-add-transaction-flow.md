# 案例：完整记账流程走查

## 场景
用户从底部导航点击「＋」→ 选择「吃 → 晚餐」→ 输入金额 38.50 → 选择「消费类」→ 确认提交。

## 代码路径追踪

### Step 1: 触发 BottomSheet
```
MainActivity.onCreate()
  → bottomNav.setOnItemSelectedListener()
    → item.getItemId() == R.id.nav_add → new AddTransactionSheet().show(...)
```
**关键**：`return false` 阻止 NavController 切换 Fragment。

### Step 2: BottomSheet 初始化
```
AddTransactionSheet.onCreateDialog()
  → peekHeight = heightPixels * 0.60 (60% 屏幕)
  → STATE_EXPANDED, skipCollapsed, draggable=false

AddTransactionSheet.onViewCreated()
  → viewModel = new TransactionViewModel(getActivity().getApplication())
  → selectedDate = Calendar.getInstance()
  → bindViews(view)
  → buildNumpad()                 // 动态创建 3×4 数字键盘
  → setupListeners()              // 绑定所有按钮事件
  → updateDateTimeDisplay()
  → buildCategoryGrid(CAT1_EXPENSE)  // 默认支出，4 列网格
  → showLevel(1)                  // 进入合并页
  → (若 pendingEdit != null → applyEditData)
```

### Step 3: Level 1 → 点击「🍚 吃」
```
onCategorySelected("🍚 吃")          // 一级分类常量已含 emoji
  → selectedCategory1 = "🍚 吃"
  → subs = (type==0 ? SUBS_EXPENSE : SUBS_INCOME).get("🍚 吃")
  → titleText.setText("🍚 吃")
  → SubCategoryAdapter(subs, this::onSubSelected)
  → showLevel(2)  // 隐藏 Level1，显示 Level2
```

### Step 4: Level 2 → 点击「晚餐」
```
onSubSelected("晚餐")
  → selectedCategory2 = "晚餐"
  → showForm()
    → showLevel(3)  // 隐藏 Level2，显示 Level3
```

### Step 5: Level 3 → 填写表单
```
用户输入金额 38.50:
  → onNumClick("3") → amountBuilder = "3"
  → onNumClick("8") → amountBuilder = "38"
  → onNumClick(".") → amountBuilder = "38."
  → onNumClick("5") → amountBuilder = "38.5"
  → onNumClick("0") → amountBuilder = "38.50"

用户选择财务分类「消费类」:
  → selectCategory3(1)
    → category3Buttons[1] 高亮橙色，其他置灰

用户确认:
  → btnConfirm.onClick() → onConfirm()
```

### Step 6: 提交
```
onConfirm()
  1. Double.parseDouble("38.50") → amount = 38.50
  2. 校验：amount > 0 ✓；支出(type=0) 才要求 category3 != -1 ✓
  3. 构造 Transaction(type=0, 38.50, "🍚 吃", "晚餐", 1, selectedDate.getTimeInMillis(), "", 0)
     // 第 8 参 accountId=0 → 默认账户
  4. viewModel.insert(transaction, callback)
     → repository.insert(transaction, callback)
       → executor.execute(() -> {
           dao.insert(transaction);       // Room 写入
           callback.run();                // UI 线程 Toast + dismiss
         })
```

### Step 7: 自动刷新
```
Room 写入 → LiveData.invalidate()
  → BillsFragment 观察者触发
    → adapter.setTransactions(新数据)
      → RecyclerView 自动更新

同时 → CalendarFragment 观察者触发（如果用户切换到日历页）
  → loadMonth() → DailySummary LiveData 更新 → 日历网格刷新
```

## 关键设计点
1. **写操作在 IO 线程**：`executor.execute()` 避免主线程 SQLite 操作
2. **LiveData 自动通知**：无需手动刷新 UI
3. **回调在 UI 线程**：`getActivity().runOnUiThread()` 确保 Toast 安全
4. **三级面板切换**：`showLevel(n)` 纯 View 可见性切换，无 Fragment 事务开销

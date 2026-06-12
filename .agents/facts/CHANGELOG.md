# 修改日志

> 版本号集中定义于 `gradle.properties` → `APP_VERSION_CODE` / `APP_VERSION_NAME`

---

## v1.0 (2026-06-12) — 文档：精简 `.agents/` 知识库

### 变更
- ASCII 流程图并入 `architecture-overview.md`（删除 `architecture-diagram.txt`）
- 删除 `examples/`、`templates/bug-root-cause-report.md`、`new-feature-guide.md`、`data-release-checklist.md`（发版清单并入 `system-properties-reference.md`）
- 精简 `SKILL.md` 索引；`user-request-log.md` 只保留最近摘要，详史以 `CHANGELOG.md` 为准

---

## v1.0 (2026-06-12) — UX：键盘恢复 + - 底行布局

### 变更
- 4×4 底行：`+` `-` `0` `确认`（± 浅蓝底、20sp）
- `onPlusKey` / `onMinusKey`：正号复位、非零金额切换负号前缀显示

---

## v1.0 (2026-06-12) — 编译修复：键盘 setTypeface 参数类型

### 修复
- `setTypeface(Typeface, int)` 第二参须为 `Typeface.BOLD`/`NORMAL`，不可传 boolean

---

## v1.0 (2026-06-12) — UX：Level3 记账表单五项优化

### 变更
1. **选日期**：去掉「选择日期」标题；月历导航栏增高、去 inset，修复上月/下月显示不全
2. **确定按钮**：`minHeight=44dp`、白字、`inset=0`，修复文字不显示
3. **账户/金额**：拆成两个独立圆角 `MaterialCardView`
4. **财务分类**：维持/消费/提升/社交改为 `spinner_category3`，与账户/金额同一行（支出可见，收入隐藏）
5. **数字键盘**：去掉 ±；右侧列改为 ⌫(退格)/C/./确认；确认缩为单格；配色分区（数字白底、退格橙、清空红、小数紫、确认绿）

---

## v1.0 (2026-06-12) — 编译：消除 AddTransactionSheet deprecation 警告

### 修复
- `Window.setDecorFitsSystemWindows` → `WindowCompat.setDecorFitsSystemWindows`（targetSdk 36 已标记过时）
- `Context.getColor` → `ContextCompat.getColor`

---

## v1.0 (2026-06-12) — UX：记账 Level3 紧凑日期选择器

### 变更
- 新增 `view_calendar_sheet.xml`（更小导航栏/字号/内边距），仅记账页 `include`
- `MonthCalendarView(root, compact)`：`compact=true` 时格高 34dp、不显示日支出
- 日历页仍用 `view_calendar.xml` + 默认构造，**不受影响**

---

## v1.0 (2026-06-12) — 编译修复：MonthCalendarView 跨包访问

### 根因
- `AddTransactionSheet`（`ui.add`）复用 `MonthCalendarView`（`ui.calendar`）时，构造器与监听器为包内可见，编译失败

### 修复
- `MonthCalendarView` 构造器、`On*Listener` 接口及 `syncFromCalendar` / `jumpToYearMonth` 等对外方法改为 `public`

---

## v1.0 (2026-06-12) — Bug 修复：记账选日期日历无法翻页 + 复用 MonthCalendarView

### 根因
- 布局虽 `include view_calendar`，但 Java 里**另写了一套** `buildSheetCalendarGrid`
- `shiftSheetCalendarMonth` 翻页后又调 `syncSheetCalendarToSelectedDate()`，把年月**重置回已选日期**，表现为上月/下月点不动

### 修复
- 记账页改复用 `MonthCalendarView` + `YearMonthPickerDialog`（与日历页同组件）
- 删除 ~90 行重复网格代码

---

## v1.0 (2026-06-12) — 日历选月逻辑 + 账户类型筛选 + 需求录入规范

### 新增
- 日历：非当月自动选最早有账日；当月恒选今天；点击标题弹窗选年月
- 账户：顶部「活期/资产/全部」筛选（默认活期）；新增账户后自动切到对应类型
- `.agents/templates/user-request-intake.md` + `facts/user-request-log.md`；`AGENTS.md` 强制先整理需求

### Bug 修复
- 日历切回当月偶发未选中今天：根因 `onDayClick(null)` 沿用上月 `selectedDate`，`loadMonth` 月份不匹配则放弃选中；改为 `OnMonthChangedListener` + `resolveAutoSelectDate`

---

## v1.0 (2026-06-12) — 体验：默认账户 / 记账账户选择 / 账单全量 / 顶部随滚动换月

### 变更
- 账户编辑新增「设为默认账户」；长按设为默认有 Toast 反馈
- 记账 Level3：标题显示子项目，摘要行左侧改账户 Spinner（去掉重复子项目名）
- 日历「上月/下月」改为描边按钮，可见性提升
- 账单列表改 `getAll()` 展示全部；列表插月份分段头；滚动时顶部月份/收支/预算联动（单月 LiveData 订阅，防重复 observe）

---

## v1.0 (2026-06-12) — 数据安全：Migration 修复 + 账户删除 + 迁移测试

### 根因 / 变更
- **MIGRATION_2_3** 曾 `DROP TABLE accounts` 导致 v2→v3 升级丢账户；改为保留数据（仅空表时补默认账户）
- `TransactionRepository` 写操作改 `runInTransaction`，余额与账单原子提交
- `AccountRepository.deleteSafe`：删非默认账户时余额并入默认、`transactions.reassignAccount`
- `exportSchema = true`；新增 `MigrationInstrumentedTest`；发版清单 `templates/data-release-checklist.md`

---

## v1.0 (2026-06-12) — Bug 修复：语音细则解析 + 新增账户重复入账

### 根因
1. **细则**：编辑页用自由文本框，未与记账页二级子项目（吃→早餐）对齐；语音解析「类别」截断未在「细则」处停止
2. **重复入账 / 选填浮窗误弹**：`MainActivity.handleVoiceResult` 用 `accountRepo.getAll().observe()` 长期监听，新增账户时 LiveData 再次触发，重复 `insert` 并弹出 `OptionalFieldsDialog`

### 修复
- 新增 `CategoryCatalog` 统一一级/子项目常量；`VoiceTransactionParser` 规范细则到子项目列表
- `TransactionEditFragment` 子项目改为 Spinner（随一级分类联动，支持自定义）
- 语音直接入账改 `AccountRepository.fetchAll()` 一次性读账户，不再 observe

---

## v1.0 (2026-06-12) — 功能：语音记账改用语义输入法语音转文字

### 变更
- **不再**调用 App 内麦克风 / SpeechRecognizer / 系统 ASR Activity
- 长按 `+` 打开文本输入页：用户用**系统输入法自带语音转文字**录入 →「解析预览」→ 确认入账
- 原麦克风方案归档为备研：`VoiceInputMicLegacy`、`SpeechRecognitionHelper`、`VoicePermissionHelper`、`MainActivity` 内权限块（注释）

---

## v1.0 (2026-06-12) — Bug 修复：授权后仍 Toast 拒绝 + 记账页空账户闪退

### Bug 修复
- **授权后仍提示需要麦克风**：`VoicePermissionHelper` 去掉 MIUI 不可靠的 AppOps 二次校验，以系统弹窗 `checkSelfPermission` 为准
- **AddTransactionSheet 闪退**：`loadAccounts` 账户列表为空时访问 `accounts.get(0)` → `IndexOutOfBoundsException`

---

## v1.0 (2026-06-12) — Bug 修复：AppOps 拦截 RECORD_AUDIO（error=9）

### Bug 修复
- **根因**：仅在设置里开麦克风未走 Activity 运行时 `requestPermissions`，MIUI AppOps 未登记；Fragment 内申请无效
- **修复**：`MainActivity` 实现 `MicrophonePermissionHost`，长按 + / 按住 🎤 前均经 `ActivityCompat.requestPermissions`；已授权时仍 prime AppOps；`startListening` 捕获 `SecurityException`

---

## v1.0 (2026-06-12) — Bug 修复：已开麦克风仍弹权限引导

### Bug 修复
- **根因**：MIUI 上 `SpeechRecognizer` 返回 `ERROR_AUDIO`/`ERROR_INSUFFICIENT_PERMISSIONS` 时误当未授权；`DialogFragment` 用 `requireContext()` 校验也不稳
- **修复**：`VoicePermissionHelper` 以 Activity + AppOps 判断；已授权则提示「语音服务无法录音」并引导系统语音输入，不再误导去设置

---

## v1.0 (2026-06-12) — 功能：语音先显原文 + 照读提示 + 去掉 MIUI 系统 ASR 自动跳转

### 界面 / 功能
- 识别文字**先显示在屏幕白色区域**，松手后再展示关键词/正则解析预览，用户点「确认并记账」才提交
- 提示改为可照读的**支出/收入示例句**；可切换示例类型
- 取消松手自动打开小米 `mibrain.speech`（易报「出错了(2)」），改应用内 `SpeechRecognizer` 绑定 `com.xiaomi.mibrain.speech`；系统识别仅手动入口

### 其他
- 声明 `INTERNET` 权限（云端语音识别）

---

## v1.0 (2026-06-12) — Bug 修复：MIUI 按住说话立即识别失败

### Bug 修复
- **根因**：MIUI `AppOps RECORD_AUDIO` 未生效或内置 `SpeechRecognizer` 无法打开麦克风；`ERROR_SPEECH_TIMEOUT` 被误显示为「识别失败」
- **修复**：小米/红米等机型默认松手后调起**系统语音识别**；麦克风错误弹窗引导 MIUI 权限设置；每次按住重建 Recognizer；按住不足 0.5 秒提示重试；`MainActivity` 打开语音前先申请权限

---

## v1.0 (2026-06-11) — Bug 修复：语音记账误报「设备不支持」

### Bug 修复
- **根因**：`SpeechRecognizer.isRecognitionAvailable()` 在 Android 11+ 未声明 `<queries>` 时恒为 false，与麦克风权限无关
- **修复**：`AndroidManifest` 增加 `RecognitionService` / `RECOGNIZE_SPEECH` 查询；新增 `SpeechRecognitionHelper` 枚举系统语音服务并绑定；无内置服务时降级调起系统语音识别界面

---

## v1.0 (2026-06-11) — 功能：长按 + 语音记账

### 界面 / 功能
- **长按底部「+」**：弹出微信风格语音遮罩（按住说话/松开识别/上滑取消），含客制化字段提示
- **语音解析**：提取收支、类别、细则、金额、备注、账户、财务分类；必填（金额/收支/类别/细则）未齐或支出缺财务分类 → 跳转「补全账单」编辑页
- **选填提醒**：必填齐全且可直接保存时，未填备注/账户等以浮动卡片提示
- **编辑页**：必填项标题加 `*`；补全模式隐藏删除按钮，校验不通过不可提交

### 权限
- `AndroidManifest` 声明 `RECORD_AUDIO`；首次使用语音时运行时申请

### 代码
- 新增 `ui/voice/`：`VoiceInputSheet`、`VoiceTransactionParser`、`VoiceDraft`、`OptionalFieldsDialog`
- `MainActivity` 长按 `nav_add` + `TransactionViewModel` 写入
- `TransactionEditFragment` 支持 `voiceDraft` Bundle 新建保存

---

## v1.0 (2026-06-11) — 功能：账单页收支图表

### 界面 / 功能
- **账单页**：顶部月份栏日历旁新增「📊 图表」入口
- **收支图表页**（`StatisticsFragment`）：支出/收入切换；饼图按当月/半年/1年筛选，支出三级折叠明细（财务四类→一级类→二级子项，类内百分比合计 100%），收入两级（一级类→二级子项）
- **折线图**：默认近 6 个月趋势，可切换当月/半年/1年，随收支类型切换颜色与数据；时间窗口始终以当前月为终点

### 代码
- 新增 `ui/stats/`：`StatisticsFragment`、`StatisticsHelper`、`PieChartView`、`LineChartView`、`StatNode`
- `TransactionViewModel.getByDateRange()` 暴露给图表页
- 导航 `nav_statistics`；进入图表页隐藏底部导航

---

## v1.0 (2026-06-11) — 功能：日历修复 + 账单问卷式编辑页

### Bug 修复
- **日历 6/12 等新数据不显示**：Room 日期查询改为 SQLite `'localtime'`，与账单页本地日期一致
- **日历观察者堆积**：切换月/日时 `removeObservers` 后再观察
- **日历选中日期无高亮**：`MonthCalendarView.setSelectedDate()` 蓝色高亮

### 界面 / 功能
- **日历明细**：复用 `BillAdapter` + `item_transaction` 卡片（无日期头、不可点击）
- **账单编辑**：点击条目跳转 `TransactionEditFragment` 问卷式全字段编辑页（隐藏底部导航），不再弹出记账 BottomSheet

---

## v1.0 (2026-06-11) — Bug 修复：备注输入时栏位贴键盘上方

### Bug 修复
- **输入备注时备注框被键盘挡住**：`panel_note_bottom` 移至弹窗根层；`NOTE_EDITING` 时 `bottomMargin = 键盘高度`（WindowInsets + decor 双通道），栏位贴在键盘正上方

---

## v1.0 (2026-06-11) — Bug 修复：备注底部弹栏 + 键盘收起后布局恢复

### Bug 修复
- **备注后 Level 3 显示异常**：行内 `EditText` 调系统键盘会顶起 BottomSheet，收起后高度无法恢复
- **按原需求重做**：`text_note` 预览 + `panel_note_bottom` 底部输入；`SOFT_INPUT_ADJUST_NOTHING`；键盘收起后 `restoreSheetLayout()` 强制恢复弹窗高度与 `level3_body`

### 交互
- 点备注 → 底部弹栏 + 键盘；收起键盘 → 栏位停靠底部（长文增高）
- 停靠后点空白遮罩 → 收回，预览写回 `text_note`；返回键同理

---

## v1.0 (2026-06-11) — Bug 修复：弹窗高度 + Level 2/3 收支切换回一级

### Bug 修复
- **30% 屏高在小米 13 Ultra 等长屏上过挤**：`SHEET_HEIGHT_RATIO` 30%→**42%**，兼顾不占屏与 Level 3 键盘可读
- **Level 2/3 点当前收支无法回一级**：`onTypeToggle()` 在二级/三级时，点已选「支出/收入」也会 `showLevel(1)`；仅一级且类型未变时才忽略

---

## v1.0 (2026-06-11) — 调整：弹窗 30% 屏高 + Level 2 网格化

### 撤销
- 回退备注底部弹栏、日期/时分/备注三分区、键盘按压反馈等上一轮交互改动

### 布局 / 交互
- **弹窗高度**：三级界面统一最多占屏幕 **30%**（`SHEET_HEIGHT_RATIO=0.30`），标题栏/控件略压缩以适配
- **Level 2 子项目**：由列表改为与 Level 1 同款 4 列小模块网格（`grid_sub` + `createGridCell()`），一页放不下时可纵向滚动

---

## v1.0 (2026-06-11) — Bug 修复：Level 3 记账页被压扁看不清

### Bug 修复
- **进入三级记账页后摘要行/日期行/键盘被压成细线**：根因是 `content_host`（weight=1）与 `numpad_container`（weight=3）把弹窗主体拆成 25%/75% 两块，而表单子项 weight 合计仅 33，各行实际高度极小。
  - 修复：将键盘移入 `level3_body`，与摘要/财务分类/日期行共用 `weightSum=100`（支出 15+8+10+67，收入 18+12+70）；Level 3 与 Level 1/2 的 `content_scroll` 互斥显示，占满标题栏以下全部空间。
  - 日期/时分选择器改为隐藏 `level3_body` 覆盖展示，移除 `expandContentForPicker()` 的 weight 操纵。

---

## v1.0 (2026-06-11) — Bug 修复：点击「+」闪退 ClassCastException

### Bug 修复
- **点击「+」打开记账面板即崩溃**：`placeTypeToggle()` 把 `type_toggle_bar` 放进 `FrameLayout` 槽位时误用 `LinearLayout.LayoutParams`，测量阶段 `ClassCastException`。
  - 修复：父容器为 `FrameLayout` 时统一使用 `FrameLayout.LayoutParams`。

---

## v1.0 (2026-06-11) — Bug 修复 + 交互：Level3 禁滚动 / 收支切换常驻标题栏

### Bug 修复
- **编译错误**：`buildNumpad()` 中 `Button.setInsetTop()` 不存在 → 键盘键改为 `TextView` 实现，避免 Material inset API 问题

### 交互
- **Level 3 禁止上下滑动**：进入记账页时隐藏并禁用 `content_scroll`，所有内容固定在一屏
- **收入页比例**：无财务分类时摘要 18% + 日期行 12%（`applyFormLayoutWeights()`）；支出保持 15% + 8% + 10%
- **收支切换常驻**：Level 2/3 时 `type_toggle_bar` 缩小移至标题栏返回键右侧；点击另一类型回到 Level 1 对应类别网格（支出→吃/住…，收入→工资/投资…）

---

## v1.0 (2026-06-11) — 美化：记账数字填写页（Level 3）重构

### 界面
- **Level 3 分区布局**（弹窗总高度仍 60% 不变）：约 15% 子项目+金额摘要卡、8% 财务四分类（仅支出）、10% 日期/时分/备注行；键盘区约 75%（`content_host:numpad = 1:3`）
- **摘要行**：左侧子项目（早餐/旅游等）+ 右侧金额，支出红/收入绿分色
- **日期行**：`yyyy年MM月dd日` 与 `HH时mm分` 分列可点；右侧备注单行输入
- **内嵌选择器**（占满弹窗主体，高度不变）：点日期→月历网格；点时分→老虎机滚轮；返回键/确定关闭
- **4×4 键盘**：`1-9 0 . + - C 确认`（确认占底行两格），按键缩小、圆角分色；移除独立确认按钮与过大 3×4 键盘

### 代码
- `sheet_add_transaction.xml` 根布局改为纵向 `LinearLayout`；新增 `panel_date_picker`/`panel_time_picker`/`text_sub_item`/`text_date`/`text_time`
- `AddTransactionSheet`：`buildNumpad()` 4×4、`buildSheetCalendarGrid()`、`showDatePickerPanel()`/`showTimePickerPanel()`、`updateAmountDisplay()`

---

## v1.0 (2026-06-11) — Bug 修复：收支切换「支出/收入」省略号

### Bug 修复
- **记账面板顶部「支出/收入」显示省略号**（非类别格子）：
  - 根因：切换条容器仅占屏宽 **33%**，两枚 `Button` 再平分；Material `Button` 默认 **minWidth≈88dp** + 单行省略，「💰 支出」在窄格内被截断。
  - 修复：切换条改为**整行分段控件**（去掉右侧 80% 留白）；`MaterialButton` 设 `minWidth=0` + 代码 `setMinWidth(0)`；文案简化为「支出」「收入」；未选中白底灰字、选中红/绿底白字；轨道背景 `bg_type_toggle_track.xml`。

---

## v1.0 (2026-06-11) — Bug 修复 + 美化：记账类别格子省略号

### Bug 修复
- **记账面板类别文字显示为省略号**：类别格子原用 `Button`（默认单行 + 末尾省略 + 大内边距/最小宽度），4 列窄格子放不下「📚 教育」「🚗 交通」等"emoji+文字"被截断。
  - 修复 + 美化：`buildCategoryGrid()` 改为**竖排卡片**——圆角底 + 上方大号 emoji（24sp）+ 下方文字（13sp，按空格拆分 `"🍚 吃"` 分两行），文字短到不再被截，视觉更清爽。

---

## v1.0 (2026-06-11) — Bug 修复：日历跳转导航不同步 + 图标加文字

### Bug 修复
- **从账单页「📅」跳到日历后，点底部「账单」无法返回**（只能用系统返回键）：
  - 根因：`BillsFragment` 用 `NavController.navigate(R.id.nav_calendar)` 跳转，但底部导航选中项仍是「账单」；再点「账单」属于**重选已选中项**，只触发 `OnItemReselectedListener`，而 `MainActivity` 只设了 `OnItemSelectedListener`，故无响应。
  - 修复：跳转改为 `bottomNav.setSelectedItemId(R.id.nav_calendar)`，驱动底部导航正常切换并同步高亮，返回「账单」即正常。移除未用的 `Navigation` 导入。

### 界面修改
- 账单页右上角日历图标由纯 📅 改为「📅 + 日历」竖排带小文字按钮（`btn_goto_calendar` 改为 `LinearLayout`，id 不变）。

---

## v1.0 (2026-06-11) — Bug 修复：记账联动账户余额

### Bug 修复
- **记账后账户余额不变**：`TransactionRepository` 此前只写 `transactions` 表，从不更新账户余额；且 `AddTransactionSheet` 把 `accountId` 硬编码为 `0`，指不到真实默认账户（自增 id）。
  - 修复：`TransactionRepository` 注入 `AccountDao`，`insert/update/delete/deleteById` 联动调整关联账户余额——收入 +金额、支出 −金额；改账先冲销旧值再应用新值；删账冲销。
  - `accountId<=0` 自动回落默认账户（`getDefaultSync`），并把真实账户 id 回写进交易。
  - 新增 `TransactionDao.getByIdSync(long)` 供改/删时同步取旧记录冲销余额。
  - ⚠️ 仅对**修复后**的新增/编辑/删除生效；修复前已有的历史账单不会被追溯计入余额。

### 文档
- 同步 `class-method-reference.md`（TransactionDao/TransactionRepository）、`architecture-overview.md`（数据流）
- 新增仓库根 `AGENTS.md` 作为 `.agents/` 强制入口（解决"改完 bug 偶尔漏录 CHANGELOG"）

---

## v1.0 (2025-06-11)

### 新增
- 底部 5 栏导航：账户、账单、＋（记账）、日历、我的
- 三级递进记账面板：收支类型 → 类别 → 子项目 → 表单
- Room 本地 SQLite 存储，MVVM + Repository 架构
- 账单按月分组展示，支持左滑删除
- 月历视图，每日标注支出金额，点击查看当天明细
- 账户页月度收支结余三栏卡片
- 8 个支出大类及子项目（吃/住/娱/教育/交通/购物/医疗/其他）
- 6 个收入大类及子项目（工资/投资/兼职/退款/礼金/其他）
- 0-9 内嵌数字键盘，支持小数点
- 收支四分类：维持类/消费类/提升类/社交类
- 日期时分滚轮选择器（NumberPicker，老虎机风格）
- 版本号集中管理（gradle.properties）
- .agents/ 知识库文档体系

---

## v1.0 (2025-06-11) — 本轮更新

### 新增
- 应用更名为「呱太记账」
- 账单按**日**分组展示（替代按月分组）
- 点击账单/日历条目弹出编辑面板（复用 AddTransactionSheet）
- 日历页默认选中当天，自动展示今日明细
- 所有页面顶部留出状态栏空间（fitsSystemWindows）

### 修改
- 左滑删除增加确认对话框
- 日期时间改为三列滚轮弹窗（月日周/时/分），默认不展开
- 收支切换与类别网格合并为同一页
- 支出/收入分类分离，收入新增大类（工资/投资/兼职/退款/礼金/其他）
- 数字键盘固定在弹窗底部
- 版本号集中管理（gradle.properties）

### 界面美化
- **全面视觉升级**：柔和蓝主色调 (#2D9CDB)，温暖浅灰背景
- **卡片化设计**：MaterialCardView 圆角卡片（12~16dp）+ 细微描边阴影
- **Emoji 图标**：分类系统全程 emoji 化（🍚🏠🎮📚🚗🛒🏥）
- **字体层次**：标题 26sp Bold，正文 15sp，金额 18~22sp Bold
- **底部导航**：统一 primary 色图标和文字
- **日历卡片**：月历包裹在圆角卡片内，今日蓝色高亮
- **账单条目**：每笔记录独立卡片，左侧 emoji 图标

### Bug 修复
- 收支切换按钮太窄导致文字显示为省略号，容器宽度从 20% → 33%
- 日历日期文字裁切：格子增加最小高度 52dp（dpToPx 密度适配）
- 数字键盘数字裁切：按钮高度改为 56dp（密度适配，替代固定 px）
- 日历明细列表 `ClassCastException`：`item_daily_transaction` 美化后根元素从 TextView 变为 MaterialCardView，Adapter 同步修正

### 逻辑
- 记账面板新增返回按钮（每级可见），手机返回键同效果：Level 3→2→1→关闭
- `onCreateDialog` 拦截 `KEYCODE_BACK`，统一走 `onBack()`
- 账单页顶部 20% 新增预算余额 + 收入/支出汇总 + 日历跳转图标

### 新增功能
- **月度预算**：SharedPreferences 按月存储，只计支出；预算 - 当月支出 = 剩余
- 账单页顶部 20% 卡片：显示「剩余 ¥ xxx」+ 收入/支出 + 📅 日历跳转
- 点击预算余额弹出设置对话框；「我的」页面也有预算设置入口
- **账户/账本管理**：账户列表页，添加/编辑/删除/设默认，六个开关控制收支/转入转出/净资产
- Transaction 新增 accountId 列，DB v1→v2 Migration（数据不丢失）
- 新账单自动关联默认账户

### 架构
- 新增 `BudgetStorage` + `BudgetViewModel`
- 新增 `Account` Entity + `AccountDao` + `AccountRepository` + `AccountViewModel`
- DB v2：`MIGRATION_1_2`（ALTER TABLE + 新建 accounts 表 + 默认账户）

### 文档同步（.agents 知识库对齐，未改源码）
- **应用名**：`system-properties-reference.md` 由「我的记账」更正为「呱太记账」；README 主标题同步
- **账单分组**：`class-method-reference.md` BillAdapter 由「按 `yyyy年M月` 月分组」更正为「按 `yyyy-MM-dd` **日**分组」（Header `M月d日 EEEE` + 当日收支）
- **弹窗高度**：`case-add-transaction-flow.md` `peekHeight` 由 `heightPixels/3` 更正为 `*0.60`（60%）
- **分类常量**：标注一级分类已内嵌 emoji（`"🍚 吃"`）+ `CAT_EMOJI` 映射；财务四分类补「仅支出必选、收入隐藏置 -1」
- **AddTransactionSheet**：`updateDateDisplay`→`updateDateTimeDisplay`（`yyyy-MM-dd HH:mm`）；补 `pendingEdit`/`applyEditData`/`formatAmount`/`wrapPicker`；`Transaction` 构造补第 8 参 `accountId`
- **账户子系统补全**：新增 `Account`/`AccountDao`/`AccountRepository`/`AccountViewModel` 文档；重写 `AccountsFragment`（账户列表管理）、`BillsFragment`（顶部预算栏）说明
- **Schema**：`Transaction` 7→8 字段（+`accountId`）；新增 `accounts` 表结构；迁移策略 `fallbackToDestructiveMigration`→`MIGRATION_1_2`（v2）
- **数据保留案例**：`case-data-preservation.md` 由「建议改用 Migration」更新为「已落地 `MIGRATION_1_2`」
- **架构图/包结构**：补入账户子系统与预算独立 SharedPreferences 路径；`ProfileFragment` 由「骨架」更正为「预算设置已实现」

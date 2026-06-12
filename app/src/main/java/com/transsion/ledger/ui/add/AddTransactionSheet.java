package com.transsion.ledger.ui.add;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.transsion.ledger.R;
import com.transsion.ledger.data.entity.Account;
import com.transsion.ledger.data.entity.Transaction;
import com.transsion.ledger.data.repository.AccountRepository;
import com.transsion.ledger.ui.calendar.MonthCalendarView;
import com.transsion.ledger.ui.calendar.YearMonthPickerDialog;
import com.transsion.ledger.viewmodel.TransactionViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddTransactionSheet extends BottomSheetDialogFragment {

    /** 30% 在高分辨率长屏（如小米 13 Ultra）上过挤，略放宽以保障 Level 3 键盘可用 */
    private static final float SHEET_HEIGHT_RATIO = 0.42f;

    // ---- 视图 ----
    private View level01, level2, level3, level3Body, contentScroll, numpadContainer;
    private View sheetRoot, panelDatePicker, panelTimePicker, panelNoteBottom, noteScrim, summaryRow, metaRow;
    private View cardCategory3, spacerBeforeCategory3;
    private ViewGroup headerTypeSlot, level01TypeSlot;
    private LinearLayout typeToggleBar;
    private TextView textAmount, textDate, textTime, textNote, textDateTime, titleText;
    private GridLayout gridCategories, gridSub, numpad;
    private MonthCalendarView sheetCalendarView;
    private LinearLayout timePickerContainer;
    private ImageButton btnBack;
    private Button btnTypeExpense, btnTypeIncome, btnConfirm;
    private EditText editNote;
    private Spinner spinnerAccount, spinnerCategory3;
    private NumberPicker npHourSheet, npMinSheet;

    private final SimpleDateFormat dateKeyFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private boolean amountNegative = false;

    // ---- 备注栏 ----
    private static final int NOTE_COLLAPSED = 0;
    private static final int NOTE_EDITING = 1;
    private static final int NOTE_DOCKED = 2;
    private int noteState = NOTE_COLLAPSED;
    private boolean keyboardVisible = false;
    private int sheetPeekHeight = 0;
    private ViewTreeObserver.OnGlobalLayoutListener decorKeyboardListener;

    // ---- 状态 ----
    private TransactionViewModel viewModel;
    private AccountRepository accountRepo;
    private List<Account> accountList;
    private long selectedAccountId = 0;
    private String selectedCategory1;
    private String selectedCategory2;
    private int type = 0;                               // 0=支出, 1=收入
    private int category3 = -1;                          // 0=维持,1=消费,2=提升,3=社交
    private Calendar selectedDate;
    private StringBuilder amountBuilder = new StringBuilder("0");

    // ---- 编辑模式 ----
    private boolean editMode = false;
    private long editId = -1;
    private Transaction pendingEdit;          // 延迟应用：等 onViewCreated 执行完再填充

    // ---- 三列滚轮日期数据 ----
    // ---- 支出分类（含 emoji） ----
    private static final String[] CAT1_EXPENSE = {"🍚 吃", "🏠 住", "🎮 娱", "📚 教育", "🚗 交通", "🛒 购物", "🏥 医疗", "📌 其他"};
    // ---- 收入分类（含 emoji） ----
    private static final String[] CAT1_INCOME  = {"💼 工资", "📈 投资", "💻 兼职", "↩️ 退款", "🎁 礼金", "📌 其他"};
    private static final String[] CATEGORY3_LABELS = {"维持", "消费", "提升", "社交"};

    private static final Map<String, List<String>> SUBS_EXPENSE = new HashMap<>();
    private static final Map<String, List<String>> SUBS_INCOME  = new HashMap<>();
    // Emoji 映射表
    public static final Map<String, String> CAT_EMOJI = new HashMap<>();
    static {
        SUBS_EXPENSE.put("🍚 吃",   Arrays.asList("早餐", "午餐", "晚餐", "零食", "水果", "饮品", "聚餐"));
        SUBS_EXPENSE.put("🏠 住",   Arrays.asList("房租", "水电", "物业", "网费", "日用品", "维修"));
        SUBS_EXPENSE.put("🎮 娱",   Arrays.asList("电影", "游戏", "KTV", "旅游", "运动", "订阅会员"));
        SUBS_EXPENSE.put("📚 教育", Arrays.asList("书籍", "课程", "培训", "文具", "考试费"));
        SUBS_EXPENSE.put("🚗 交通", Arrays.asList("公交", "地铁", "打车", "加油", "停车", "高铁/飞机"));
        SUBS_EXPENSE.put("🛒 购物", Arrays.asList("服饰", "数码", "美妆", "家居", "超市"));
        SUBS_EXPENSE.put("🏥 医疗", Arrays.asList("门诊", "药品", "体检", "牙科"));
        SUBS_EXPENSE.put("📌 其他", Arrays.asList("自定义"));

        SUBS_INCOME.put("💼 工资", Arrays.asList("基本工资", "奖金", "加班费", "补贴"));
        SUBS_INCOME.put("📈 投资", Arrays.asList("股票", "基金", "利息", "分红", "房租收入"));
        SUBS_INCOME.put("💻 兼职", Arrays.asList("副业", "稿费", "咨询费", "设计费"));
        SUBS_INCOME.put("↩️ 退款", Arrays.asList("购物退款", "报销", "押金退还"));
        SUBS_INCOME.put("🎁 礼金", Arrays.asList("红包", "礼金", "赠与"));
        SUBS_INCOME.put("📌 其他", Arrays.asList("自定义"));

        // Emoji 映射（去掉文字只留 emoji，给 BillAdapter 用）
        CAT_EMOJI.put("🍚 吃", "🍚");
        CAT_EMOJI.put("🏠 住", "🏠");
        CAT_EMOJI.put("🎮 娱", "🎮");
        CAT_EMOJI.put("📚 教育", "📚");
        CAT_EMOJI.put("🚗 交通", "🚗");
        CAT_EMOJI.put("🛒 购物", "🛒");
        CAT_EMOJI.put("🏥 医疗", "🏥");
        CAT_EMOJI.put("📌 其他", "📌");
        CAT_EMOJI.put("💼 工资", "💼");
        CAT_EMOJI.put("📈 投资", "📈");
        CAT_EMOJI.put("💻 兼职", "💻");
        CAT_EMOJI.put("↩️ 退款", "↩️");
        CAT_EMOJI.put("🎁 礼金", "🎁");
    }

    // ══════════════════════════════════════════════
    //  onCreateDialog — 固定弹窗高度
    // ══════════════════════════════════════════════
    @NonNull
    @Override
    public BottomSheetDialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            // 禁止系统键盘顶起 BottomSheet，备注栏自行跟随 IME 上移到键盘上方
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
            WindowCompat.setDecorFitsSystemWindows(dialog.getWindow(), false);
        }
        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                sheetPeekHeight = (int) (getResources().getDisplayMetrics().heightPixels * SHEET_HEIGHT_RATIO);
                applySheetHeight(bottomSheet, sheetPeekHeight);
                behavior.setPeekHeight(sheetPeekHeight);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setDraggable(false);
            }
            setupDecorKeyboardListener(dialog);
        });
        // 手机返回键也走 onBack
        dialog.setCancelable(false);
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK
                    && event.getAction() == android.view.KeyEvent.ACTION_UP) {
                onBack();
                return true;
            }
            return false;
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_add_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() != null) {
            viewModel = new TransactionViewModel(getActivity().getApplication());
            accountRepo = new AccountRepository(getActivity().getApplication());
        }
        selectedDate = Calendar.getInstance();

        bindViews(view);
        setupNoteEditor();
        setupNoteInsets();
        // MaterialButton 默认 minWidth≈88dp，在窄容器里会把「支出/收入」挤成省略号
        btnTypeExpense.setMinWidth(0);
        btnTypeIncome.setMinWidth(0);
        btnTypeExpense.setMinimumWidth(0);
        btnTypeIncome.setMinimumWidth(0);
        loadAccounts();
        setupCategory3Spinner();
        buildNumpad();
        setupListeners();
        updateDateTimeDisplay();
        // 默认支出类别，直接进入合并页
        buildCategoryGrid(CAT1_EXPENSE);
        applyFormLayoutWeights();
        placeTypeToggle(false);
        showLevel(1);

        // 编辑模式：view 就绪后再填充数据
        if (pendingEdit != null) {
            applyEditData(pendingEdit);
            pendingEdit = null;
        }
    }

    // ══════════════════════════════════════════════
    //  绑定视图
    // ══════════════════════════════════════════════
    @Override
    public void onDestroyView() {
        if (sheetRoot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(sheetRoot, null);
        }
        if (getDialog() != null && getDialog().getWindow() != null && decorKeyboardListener != null) {
            getDialog().getWindow().getDecorView().getViewTreeObserver()
                    .removeOnGlobalLayoutListener(decorKeyboardListener);
        }
        decorKeyboardListener = null;
        super.onDestroyView();
    }

    private void bindViews(View view) {
        sheetRoot = view.findViewById(R.id.sheet_root);
        contentScroll = view.findViewById(R.id.content_scroll);
        headerTypeSlot = view.findViewById(R.id.header_type_slot);
        level01TypeSlot = view.findViewById(R.id.level01_type_slot);
        typeToggleBar = view.findViewById(R.id.type_toggle_bar);
        summaryRow = view.findViewById(R.id.summary_row);
        cardCategory3 = view.findViewById(R.id.card_category3);
        spacerBeforeCategory3 = view.findViewById(R.id.spacer_before_category3);
        metaRow = view.findViewById(R.id.meta_row);
        level01 = view.findViewById(R.id.level01);
        level2 = view.findViewById(R.id.level2_scroll);
        level3 = view.findViewById(R.id.level3);
        level3Body = view.findViewById(R.id.level3_body);
        numpadContainer = view.findViewById(R.id.numpad_container);
        panelDatePicker = view.findViewById(R.id.panel_date_picker);
        panelTimePicker = view.findViewById(R.id.panel_time_picker);

        titleText    = view.findViewById(R.id.title_text);
        btnBack      = view.findViewById(R.id.btn_back);
        gridCategories = view.findViewById(R.id.grid_categories);
        gridSub = view.findViewById(R.id.grid_sub);
        textAmount   = view.findViewById(R.id.text_amount);
        textDate     = view.findViewById(R.id.text_date);
        textTime     = view.findViewById(R.id.text_time);
        textNote     = view.findViewById(R.id.text_note);
        textDateTime = view.findViewById(R.id.text_datetime);
        numpad       = view.findViewById(R.id.numpad);
        editNote     = view.findViewById(R.id.edit_note);
        noteScrim    = view.findViewById(R.id.note_scrim);
        panelNoteBottom = view.findViewById(R.id.panel_note_bottom);
        btnConfirm   = view.findViewById(R.id.btn_confirm);
        btnTypeExpense = view.findViewById(R.id.btn_type_expense);
        btnTypeIncome  = view.findViewById(R.id.btn_type_income);
        spinnerAccount = view.findViewById(R.id.spinner_account);
        spinnerCategory3 = view.findViewById(R.id.spinner_category3);
        timePickerContainer = view.findViewById(R.id.time_picker_container);

        View calRoot = panelDatePicker.findViewById(R.id.sheet_calendar);
        if (calRoot != null) {
            sheetCalendarView = new MonthCalendarView(calRoot, true);
            sheetCalendarView.setOnDayClickListener(this::applyPickedDate);
            sheetCalendarView.setOnMonthTitleClickListener(ym ->
                    YearMonthPickerDialog.show(requireContext(), ym, sheetCalendarView::jumpToYearMonth));
        }

    }

    private void setupCategory3Spinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, CATEGORY3_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory3.setAdapter(adapter);
        spinnerCategory3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                category3 = pos;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        if (category3 >= 0 && category3 < CATEGORY3_LABELS.length) {
            spinnerCategory3.setSelection(category3);
        }
    }

    // ══════════════════════════════════════════════
    //  加载账户到 Spinner
    // ══════════════════════════════════════════════
    private void loadAccounts() {
        accountRepo.getAll().observe(getViewLifecycleOwner(), accounts -> {
            accountList = accounts != null ? accounts : new ArrayList<>();
            if (accountList.isEmpty()) {
                selectedAccountId = 0;
                return;
            }
            List<String> names = new ArrayList<>();
            int defaultIdx = 0;
            for (int i = 0; i < accountList.size(); i++) {
                Account a = accountList.get(i);
                names.add(a.getName());
                if (a.isDefault()) defaultIdx = i;
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerAccount.setAdapter(adapter);
            if (spinnerAccount.getSelectedItemPosition() < 0 && defaultIdx < accountList.size()) {
                spinnerAccount.setSelection(defaultIdx);
                selectedAccountId = accountList.get(defaultIdx).getId();
            }
            spinnerAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    if (pos >= 0 && pos < accountList.size())
                        selectedAccountId = accountList.get(pos).getId();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        });
    }

    private void selectAccountInSpinner(long accountId) {
        if (accountList != null) {
            for (int i = 0; i < accountList.size(); i++) {
                if (accountList.get(i).getId() == accountId) {
                    spinnerAccount.setSelection(i);
                    break;
                }
            }
        }
    }

    private void ensureDefaultAccountSelected() {
        if (accountList == null || accountList.isEmpty()) {
            selectedAccountId = 0;
            return;
        }
        if (selectedAccountId > 0) {
            selectAccountInSpinner(selectedAccountId);
            return;
        }
        for (int i = 0; i < accountList.size(); i++) {
            if (accountList.get(i).isDefault()) {
                spinnerAccount.setSelection(i);
                selectedAccountId = accountList.get(i).getId();
                return;
            }
        }
        spinnerAccount.setSelection(0);
        selectedAccountId = accountList.get(0).getId();
    }

    // ══════════════════════════════════════════════
    //  事件
    // ══════════════════════════════════════════════
    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBack());

        btnTypeExpense.setOnClickListener(v -> onTypeToggle(0));
        btnTypeIncome.setOnClickListener(v -> onTypeToggle(1));

        textDate.setOnClickListener(v -> showDatePickerPanel());
        textTime.setOnClickListener(v -> showTimePickerPanel());
        textNote.setOnClickListener(v -> showNoteEditor());
        noteScrim.setOnClickListener(v -> onNoteOutsideTap());
        panelNoteBottom.setOnClickListener(v -> { /* 吞掉点击 */ });
        panelDatePicker.findViewById(R.id.btn_date_confirm).setOnClickListener(v -> hidePickerPanels());
        panelTimePicker.findViewById(R.id.btn_time_confirm).setOnClickListener(v -> hidePickerPanels());

        btnConfirm.setOnClickListener(v -> onConfirm());
    }

    // ══════════════════════════════════════════════
    //  编辑模式入口 — 外部调用，view 可能未创建，暂存待 onViewCreated 应用
    // ══════════════════════════════════════════════
    public void setEditTransaction(Transaction t) {
        pendingEdit = t;
    }

    private void applyEditData(Transaction t) {
        editMode = true;
        editId = t.getId();
        type = t.getType();
        selectedCategory1 = t.getCategory1();
        selectedCategory2 = t.getCategory2();
        category3 = t.getCategory3();
        selectedDate = Calendar.getInstance();
        selectedDate.setTimeInMillis(t.getDateTime());
        amountBuilder = new StringBuilder(formatAmount(t.getAmount()));
        editNote.setText(t.getNote() != null ? t.getNote() : "");
        updateNotePreview();

        // 按钮高亮
        selectType(type);
        // 预填充金额
        updateAmountDisplay();
        // 预选财务分类（仅支出）
        if (t.getType() == 0 && category3 >= 0 && category3 < CATEGORY3_LABELS.length) {
            selectCategory3(category3);
        }
        titleText.setText("编辑记录");
        updateDateTimeDisplay();
        // 恢复账户选择
        selectedAccountId = t.getAccountId();
        selectAccountInSpinner(t.getAccountId());
        // 直接跳到 Level 1 合并页
        showLevel(1);
    }

    private String formatAmount(double v) {
        if (v == (long) v) return String.valueOf((long) v);
        return String.valueOf(v);
    }

    // ══════════════════════════════════════════════
    //  内嵌日期 / 时分选择（占满弹窗，高度不变）
    // ══════════════════════════════════════════════
    private void showDatePickerPanel() {
        collapseNotePanel();
        level3Body.setVisibility(View.GONE);
        panelTimePicker.setVisibility(View.GONE);
        panelDatePicker.setVisibility(View.VISIBLE);
        if (sheetCalendarView != null) {
            sheetCalendarView.syncFromCalendar(selectedDate);
            sheetCalendarView.setDailyExpenses(Collections.emptyMap());
        }
    }

    private void applyPickedDate(String yyyyMmDd) {
        if (yyyyMmDd == null) return;
        try {
            Date d = dateKeyFmt.parse(yyyyMmDd);
            if (d != null) {
                selectedDate.setTime(d);
                updateDateTimeDisplay();
                hidePickerPanels();
            }
        } catch (ParseException ignored) {}
    }

    private void showTimePickerPanel() {
        collapseNotePanel();
        level3Body.setVisibility(View.GONE);
        panelDatePicker.setVisibility(View.GONE);
        panelTimePicker.setVisibility(View.VISIBLE);
        ensureTimePickers();
        npHourSheet.setValue(selectedDate.get(Calendar.HOUR_OF_DAY));
        npMinSheet.setValue(selectedDate.get(Calendar.MINUTE));
    }

    // ══════════════════════════════════════════════
    //  备注底部弹栏（不挤压 Level 3 主界面）
    // ══════════════════════════════════════════════
    private void setupNoteEditor() {
        editNote.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideNoteKeyboard();
                return true;
            }
            return false;
        });
        editNote.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateNoteDockedHeight();
            }
        });
    }

    private void setupNoteInsets() {
        if (sheetRoot == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(sheetRoot, (v, insets) -> {
            int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            onKeyboardInsetsChanged(imeBottom, imeBottom > 0);
            return ViewCompat.onApplyWindowInsets(v, insets);
        });
        ViewCompat.requestApplyInsets(sheetRoot);
    }

    /** 小米等机型补充：用 decor 可视区域估算键盘高度 */
    private void setupDecorKeyboardListener(BottomSheetDialog dialog) {
        if (dialog.getWindow() == null) return;
        View decor = dialog.getWindow().getDecorView();
        decorKeyboardListener = () -> {
            if (noteState != NOTE_EDITING) return;
            Rect r = new Rect();
            decor.getWindowVisibleDisplayFrame(r);
            int screenH = decor.getRootView().getHeight();
            int kbHeight = screenH - r.bottom;
            boolean imeVisible = kbHeight > screenH * 0.12;
            if (imeVisible) {
                onKeyboardInsetsChanged(kbHeight, true);
            }
        };
        decor.getViewTreeObserver().addOnGlobalLayoutListener(decorKeyboardListener);
    }

    private void onKeyboardInsetsChanged(int imeBottom, boolean imeVisible) {
        if (noteState == NOTE_EDITING) {
            positionNotePanelAboveKeyboard(imeVisible ? imeBottom : 0);
        }
        if (keyboardVisible && !imeVisible && noteState == NOTE_EDITING) {
            dockNotePanel();
        }
        keyboardVisible = imeVisible;
    }

    /** 输入中：备注栏 bottomMargin = 键盘高度，贴在键盘正上方 */
    private void positionNotePanelAboveKeyboard(int imeBottom) {
        if (panelNoteBottom == null) return;
        ViewGroup.LayoutParams raw = panelNoteBottom.getLayoutParams();
        if (!(raw instanceof FrameLayout.LayoutParams)) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) raw;
        lp.bottomMargin = (noteState == NOTE_EDITING && imeBottom > 0) ? imeBottom : 0;
        panelNoteBottom.setLayoutParams(lp);
        panelNoteBottom.setTranslationY(0f);
    }

    private void applySheetHeight(View bottomSheet, int height) {
        if (bottomSheet == null || height <= 0) return;
        ViewGroup.LayoutParams sheetLp = bottomSheet.getLayoutParams();
        sheetLp.height = height;
        bottomSheet.setLayoutParams(sheetLp);
        bottomSheet.requestLayout();
    }

    private void restoreSheetLayout() {
        if (getDialog() == null) return;
        View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheetPeekHeight > 0) {
            applySheetHeight(bottomSheet, sheetPeekHeight);
        }
        if (level3Body != null && level3 != null && level3.getVisibility() == View.VISIBLE
                && panelDatePicker.getVisibility() != View.VISIBLE
                && panelTimePicker.getVisibility() != View.VISIBLE
                && noteState == NOTE_COLLAPSED) {
            level3Body.setVisibility(View.VISIBLE);
            level3Body.requestLayout();
        }
        if (level3 != null) {
            level3.requestLayout();
        }
    }

    private void showNoteEditor() {
        if (panelDatePicker.getVisibility() == View.VISIBLE
                || panelTimePicker.getVisibility() == View.VISIBLE) {
            return;
        }
        noteState = NOTE_EDITING;
        noteScrim.setBackgroundColor(0x40000000);
        noteScrim.setVisibility(View.VISIBLE);
        panelNoteBottom.setVisibility(View.VISIBLE);
        updateNoteDockedHeight();
        editNote.requestFocus();
        editNote.setSelection(editNote.getText().length());
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(editNote, InputMethodManager.SHOW_IMPLICIT);
        }
        if (sheetRoot != null) {
            sheetRoot.post(() -> ViewCompat.requestApplyInsets(sheetRoot));
        }
    }

    private void hideNoteKeyboard() {
        if (editNote != null) editNote.clearFocus();
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && editNote != null) {
            imm.hideSoftInputFromWindow(editNote.getWindowToken(), 0);
        }
    }

    private void dockNotePanel() {
        noteState = NOTE_DOCKED;
        hideNoteKeyboard();
        // 透明遮罩：点击主界面任意空白可收起停靠栏
        noteScrim.setBackgroundColor(Color.TRANSPARENT);
        noteScrim.setVisibility(View.VISIBLE);
        panelNoteBottom.setVisibility(View.VISIBLE);
        positionNotePanelAboveKeyboard(0);
        updateNoteDockedHeight();
        updateNotePreview();
        restoreSheetLayout();
    }

    private void collapseNotePanel() {
        noteState = NOTE_COLLAPSED;
        hideNoteKeyboard();
        if (editNote != null) editNote.clearFocus();
        if (noteScrim != null) noteScrim.setVisibility(View.GONE);
        if (panelNoteBottom != null) {
            panelNoteBottom.setVisibility(View.GONE);
            positionNotePanelAboveKeyboard(0);
        }
        updateNotePreview();
        restoreSheetLayout();
    }

    private void onNoteOutsideTap() {
        if (noteState == NOTE_EDITING) {
            dockNotePanel();
        } else if (noteState == NOTE_DOCKED) {
            collapseNotePanel();
        }
    }

    private void updateNoteDockedHeight() {
        if (editNote == null) return;
        String text = editNote.getText().toString();
        int lines = Math.max(1, text.split("\n", -1).length);
        if (text.length() > 20) lines = Math.max(lines, 2);
        if (text.length() > 40) lines = Math.max(lines, 3);
        int target = noteState == NOTE_COLLAPSED ? 1 : Math.min(lines, 5);
        editNote.setMinLines(target);
        editNote.setMaxLines(5);
    }

    private void updateNotePreview() {
        if (textNote == null || editNote == null) return;
        String note = editNote.getText().toString().trim();
        if (note.isEmpty()) {
            textNote.setText("备注");
            textNote.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint));
        } else {
            textNote.setText(note);
            textNote.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        }
    }

    private void hidePickerPanels() {
        if (panelTimePicker.getVisibility() == View.VISIBLE) {
            selectedDate.set(Calendar.HOUR_OF_DAY, npHourSheet.getValue());
            selectedDate.set(Calendar.MINUTE, npMinSheet.getValue());
            selectedDate.set(Calendar.SECOND, 0);
        }
        panelDatePicker.setVisibility(View.GONE);
        panelTimePicker.setVisibility(View.GONE);
        if (level3 != null && level3.getVisibility() == View.VISIBLE) {
            level3Body.setVisibility(View.VISIBLE);
        }
        updateDateTimeDisplay();
    }

    private void ensureTimePickers() {
        if (npHourSheet != null) return;
        timePickerContainer.removeAllViews();

        npHourSheet = new NumberPicker(requireContext());
        npHourSheet.setMinValue(0);
        npHourSheet.setMaxValue(23);
        npHourSheet.setWrapSelectorWheel(true);
        String[] hourLabels = new String[24];
        for (int i = 0; i < 24; i++) hourLabels[i] = i + "时";
        npHourSheet.setDisplayedValues(hourLabels);

        npMinSheet = new NumberPicker(requireContext());
        npMinSheet.setMinValue(0);
        npMinSheet.setMaxValue(59);
        npMinSheet.setWrapSelectorWheel(true);
        String[] minLabels = new String[60];
        for (int i = 0; i < 60; i++) minLabels[i] = i + "分";
        npMinSheet.setDisplayedValues(minLabels);

        timePickerContainer.addView(wrapPicker(npHourSheet, "时"), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        timePickerContainer.addView(wrapPicker(npMinSheet, "分"), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f));
    }

    private LinearLayout wrapPicker(NumberPicker picker, String label) {
        LinearLayout wrap = new LinearLayout(requireContext());
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setGravity(Gravity.CENTER);
        wrap.setPadding(12, 0, 12, 0);

        TextView lbl = new TextView(requireContext());
        lbl.setText(label);
        lbl.setTextSize(13);
        lbl.setTextColor(Color.parseColor("#6B7280"));
        lbl.setGravity(Gravity.CENTER);

        wrap.addView(lbl);
        wrap.addView(picker, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return wrap;
    }

    // ══════════════════════════════════════════════
    //  收支切换（Level 1 全宽 / Level 2+ 标题栏紧凑）
    // ══════════════════════════════════════════════
    private void onTypeToggle(int t) {
        // Level 2/3：点当前已选类型 → 回一级；点另一类型 → 切换后回一级
        if (level2.getVisibility() == View.VISIBLE || level3.getVisibility() == View.VISIBLE) {
            if (type != t) {
                selectType(t);
            }
            selectedCategory1 = null;
            if (level3.getVisibility() == View.VISIBLE) {
                collapseNotePanel();
                hidePickerPanels();
            }
            showLevel(1);
            return;
        }

        if (type == t) return;
        selectType(t);
    }

    private void selectType(int t) {
        type = t;
        if (t == 0) {
            btnTypeExpense.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5595A")));
            btnTypeExpense.setTextColor(Color.WHITE);
            btnTypeIncome.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
            btnTypeIncome.setTextColor(Color.parseColor("#6B7280"));
        } else {
            btnTypeIncome.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2EAC68")));
            btnTypeIncome.setTextColor(Color.WHITE);
            btnTypeExpense.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
            btnTypeExpense.setTextColor(Color.parseColor("#6B7280"));
        }
        buildCategoryGrid(type == 0 ? CAT1_EXPENSE : CAT1_INCOME);
        if (t == 1) category3 = -1;
        applyFormLayoutWeights();
        if (level3 != null && level3.getVisibility() == View.VISIBLE) {
            updateAmountDisplay();
        }
    }

    /** 支出：18+10+72；收入：15+12+73（无财务分类 Spinner），合计 100 */
    private void applyFormLayoutWeights() {
        if (summaryRow == null || metaRow == null || numpadContainer == null) return;
        LinearLayout.LayoutParams sumLp = (LinearLayout.LayoutParams) summaryRow.getLayoutParams();
        LinearLayout.LayoutParams metaLp = (LinearLayout.LayoutParams) metaRow.getLayoutParams();
        LinearLayout.LayoutParams numpadLp = (LinearLayout.LayoutParams) numpadContainer.getLayoutParams();
        if (type == 0) {
            sumLp.weight = 18f;
            cardCategory3.setVisibility(View.VISIBLE);
            spacerBeforeCategory3.setVisibility(View.VISIBLE);
            metaLp.weight = 10f;
            numpadLp.weight = 72f;
        } else {
            sumLp.weight = 15f;
            cardCategory3.setVisibility(View.GONE);
            spacerBeforeCategory3.setVisibility(View.GONE);
            metaLp.weight = 12f;
            numpadLp.weight = 73f;
        }
        summaryRow.setLayoutParams(sumLp);
        metaRow.setLayoutParams(metaLp);
        numpadContainer.setLayoutParams(numpadLp);
    }

    private void placeTypeToggle(boolean compactInHeader) {
        ViewGroup target = compactInHeader ? headerTypeSlot : level01TypeSlot;
        ViewGroup currentParent = (ViewGroup) typeToggleBar.getParent();
        if (currentParent != null && currentParent != target) {
            currentParent.removeView(typeToggleBar);
        }
        headerTypeSlot.setVisibility(compactInHeader ? View.VISIBLE : View.GONE);

        float d = getResources().getDisplayMetrics().density;
        // 父容器是 FrameLayout，必须用 FrameLayout.LayoutParams（不能用 LinearLayout.LayoutParams）
        FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(
                compactInHeader ? (int) (128 * d) : ViewGroup.LayoutParams.MATCH_PARENT,
                compactInHeader ? (int) (32 * d) : ViewGroup.LayoutParams.WRAP_CONTENT);
        if (typeToggleBar.getParent() != target) {
            target.addView(typeToggleBar, barLp);
        } else {
            typeToggleBar.setLayoutParams(barLp);
        }
        typeToggleBar.setPadding(compactInHeader ? (int) (2 * d) : (int) (3 * d),
                compactInHeader ? (int) (2 * d) : (int) (3 * d),
                compactInHeader ? (int) (2 * d) : (int) (3 * d),
                compactInHeader ? (int) (2 * d) : (int) (3 * d));

        LinearLayout.LayoutParams expenseLp = (LinearLayout.LayoutParams) btnTypeExpense.getLayoutParams();
        LinearLayout.LayoutParams incomeLp = (LinearLayout.LayoutParams) btnTypeIncome.getLayoutParams();
        int btnH = (int) ((compactInHeader ? 28 : 44) * d);
        expenseLp.height = btnH;
        incomeLp.height = btnH;
        btnTypeExpense.setLayoutParams(expenseLp);
        btnTypeIncome.setLayoutParams(incomeLp);
        btnTypeExpense.setTextSize(compactInHeader ? 11 : 15);
        btnTypeIncome.setTextSize(compactInHeader ? 11 : 15);
    }

    // ══════════════════════════════════════════════
    //  类别网格
    // ══════════════════════════════════════════════
    private void buildCategoryGrid(String[] labels) {
        gridCategories.removeAllViews();
        gridCategories.setRowCount((int) Math.ceil(labels.length / 4.0));
        for (String label : labels) {
            String[] parts = label.split(" ", 2);
            String emoji = parts.length > 0 ? parts[0] : label;
            String name = parts.length > 1 ? parts[1] : "";
            gridCategories.addView(createGridCell(emoji, name, () -> onCategorySelected(label)));
        }
    }

    private void buildSubCategoryGrid(List<String> items) {
        gridSub.removeAllViews();
        gridSub.setRowCount((int) Math.ceil(items.size() / 4.0));
        String emoji = emojiFromCategory(selectedCategory1);
        for (String sub : items) {
            gridSub.addView(createGridCell(emoji, sub, () -> onSubSelected(sub)));
        }
    }

    private String emojiFromCategory(String category) {
        if (category == null) return "📌";
        String[] parts = category.split(" ", 2);
        return parts.length > 0 ? parts[0] : "📌";
    }

    private LinearLayout createGridCell(String emoji, String name, Runnable onClick) {
        float d = getResources().getDisplayMetrics().density;
        LinearLayout cell = new LinearLayout(requireContext());
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding((int) (4 * d), (int) (10 * d), (int) (4 * d), (int) (10 * d));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F0F4FF"));
        bg.setCornerRadius(12 * d);
        cell.setBackground(bg);
        cell.setClickable(true);
        cell.setFocusable(true);

        TextView txtEmoji = new TextView(requireContext());
        txtEmoji.setText(emoji);
        txtEmoji.setTextSize(20);
        txtEmoji.setGravity(Gravity.CENTER);

        TextView txtName = new TextView(requireContext());
        txtName.setText(name);
        txtName.setTextSize(12);
        txtName.setTextColor(Color.parseColor("#2D5AA0"));
        txtName.setGravity(Gravity.CENTER);
        txtName.setSingleLine(true);
        txtName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        txtName.setPadding(0, (int) (3 * d), 0, 0);

        cell.addView(txtEmoji);
        cell.addView(txtName);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        params.setMargins((int) (4 * d), (int) (4 * d), (int) (4 * d), (int) (4 * d));
        cell.setLayoutParams(params);
        cell.setOnClickListener(v -> onClick.run());
        return cell;
    }

    private void onCategorySelected(String category) {
        selectedCategory1 = category;
        Map<String, List<String>> subs = type == 0 ? SUBS_EXPENSE : SUBS_INCOME;
        List<String> items = subs.getOrDefault(category, new ArrayList<>());
        buildSubCategoryGrid(items);
        String[] parts = category.split(" ", 2);
        titleText.setText(parts.length > 1 ? parts[1] : category);
        showLevel(2);
    }

    // ══════════════════════════════════════════════
    //  子项目
    // ══════════════════════════════════════════════
    private void onSubSelected(String sub) {
        if ("自定义".equals(sub)) {
            EditText input = new EditText(requireContext());
            input.setHint("输入项目名称");
            input.setSingleLine();
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("自定义项目")
                    .setView(input)
                    .setPositiveButton("确定", (d, w) -> {
                        String text = input.getText().toString().trim();
                        if (!text.isEmpty()) {
                            selectedCategory2 = text;
                            showForm();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            selectedCategory2 = sub;
            showForm();
        }
    }

    // ══════════════════════════════════════════════
    //  表单
    // ══════════════════════════════════════════════
    private void showForm() {
        titleText.setText(selectedCategory2);
        amountNegative = false;
        if (type == 0 && category3 < 0) {
            selectCategory3(0);
        }
        ensureDefaultAccountSelected();
        collapseNotePanel();
        updateAmountDisplay();
        updateDateTimeDisplay();
        hidePickerPanels();
        showLevel(3);
    }

    private void updateDateTimeDisplay() {
        textDate.setText(String.format(Locale.getDefault(), "%d年%02d月%02d日",
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.DAY_OF_MONTH)));
        textTime.setText(String.format(Locale.getDefault(), "%02d时%02d分",
                selectedDate.get(Calendar.HOUR_OF_DAY),
                selectedDate.get(Calendar.MINUTE)));
        if (textDateTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            textDateTime.setText(sdf.format(selectedDate.getTime()));
        }
    }

    private void updateAmountDisplay() {
        String raw = amountBuilder.toString();
        String shown = amountNegative && !"0".equals(raw) ? "-" + raw : raw;
        textAmount.setText(shown);
        int color = type == 1 ? Color.parseColor("#2EAC68") : Color.parseColor("#E5595A");
        textAmount.setTextColor(color);
    }

    // ══════════════════════════════════════════════
    //  数字键盘
    // ══════════════════════════════════════════════
    private void buildNumpad() {
        numpad.removeAllViews();
        float d = getResources().getDisplayMetrics().density;
        int margin = (int) (3 * d);
        // 4×4：右列功能键；底行 + - 0 确认
        String[][] keys = {
                {"1", "2", "3", "⌫"},
                {"4", "5", "6", "C"},
                {"7", "8", "9", "."},
                {"+", "-", "0", "确认"}
        };
        for (int r = 0; r < keys.length; r++) {
            for (int c = 0; c < keys[r].length; c++) {
                String key = keys[r][c];
                applyNumpadKeyStyle(keyViewForKey(key, r, c, margin), key);
            }
        }
    }

    private TextView keyViewForKey(String key, int r, int c, int margin) {
        TextView keyView = new TextView(requireContext());
        keyView.setGravity(Gravity.CENTER);
        keyView.setClickable(true);
        keyView.setFocusable(true);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = 0;
        params.rowSpec = GridLayout.spec(r, 1, 1f);
        params.columnSpec = GridLayout.spec(c, 1, 1f);
        params.setMargins(margin, margin, margin, margin);
        keyView.setLayoutParams(params);
        keyView.setText(key);
        numpad.addView(keyView);
        return keyView;
    }

    private void applyNumpadKeyStyle(TextView keyView, String key) {
        float d = getResources().getDisplayMetrics().density;
        boolean actionKey = "确认".equals(key) || "C".equals(key) || "⌫".equals(key)
                || "+".equals(key) || "-".equals(key);
        keyView.setTypeface(Typeface.DEFAULT, actionKey ? Typeface.BOLD : Typeface.NORMAL);
        keyView.setTextSize("确认".equals(key) ? 14 : ("⌫".equals(key) ? 18 : ("+".equals(key) || "-".equals(key) ? 20 : 16)));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12 * d);
        if ("确认".equals(key)) {
            bg.setColor(Color.parseColor("#2EAC68"));
            keyView.setTextColor(Color.WHITE);
        } else if ("⌫".equals(key)) {
            bg.setColor(Color.parseColor("#FFF3E0"));
            keyView.setTextColor(Color.parseColor("#E65100"));
        } else if ("C".equals(key)) {
            bg.setColor(Color.parseColor("#FEE2E2"));
            keyView.setTextColor(Color.parseColor("#C62828"));
        } else if ("+".equals(key) || "-".equals(key)) {
            bg.setColor(Color.parseColor("#E8F4FD"));
            keyView.setTextColor(Color.parseColor("#1565C0"));
        } else if (".".equals(key)) {
            bg.setColor(Color.parseColor("#EEF2FF"));
            keyView.setTextColor(Color.parseColor("#4338CA"));
        } else {
            bg.setColor(Color.parseColor("#FFFFFF"));
            keyView.setTextColor(Color.parseColor("#1F2937"));
        }
        bg.setStroke((int) (1 * d), Color.parseColor("#E5E7EB"));
        keyView.setBackground(bg);

        if ("确认".equals(key)) {
            keyView.setOnClickListener(v -> onConfirm());
        } else if ("C".equals(key)) {
            keyView.setOnClickListener(v -> onClearAmount());
        } else if ("⌫".equals(key)) {
            keyView.setOnClickListener(v -> onBackspaceAmount());
        } else if ("+".equals(key)) {
            keyView.setOnClickListener(v -> onPlusKey());
        } else if ("-".equals(key)) {
            keyView.setOnClickListener(v -> onMinusKey());
        } else if (".".equals(key)) {
            keyView.setOnClickListener(v -> onNumClick("."));
        } else {
            final String digit = key;
            keyView.setOnClickListener(v -> onNumClick(digit));
        }
    }

    /** +：强制为正号显示 */
    private void onPlusKey() {
        amountNegative = false;
        updateAmountDisplay();
    }

    /** -：在非零金额上切换正负号显示 */
    private void onMinusKey() {
        if (!"0".equals(amountBuilder.toString())) {
            amountNegative = !amountNegative;
            updateAmountDisplay();
        }
    }

    private void onBackspaceAmount() {
        if (amountBuilder.length() > 1) {
            amountBuilder.deleteCharAt(amountBuilder.length() - 1);
        } else {
            amountBuilder.setLength(0);
            amountBuilder.append("0");
            amountNegative = false;
        }
        updateAmountDisplay();
    }

    private void onNumClick(String digit) {
        if ("0".equals(amountBuilder.toString())) {
            if (".".equals(digit)) {
                amountBuilder.append(".");
            } else {
                amountBuilder.setLength(0);
                amountBuilder.append(digit);
            }
        } else {
            if (".".equals(digit) && amountBuilder.toString().contains(".")) return;
            amountBuilder.append(digit);
        }
        updateAmountDisplay();
    }

    private void onClearAmount() {
        amountBuilder.setLength(0);
        amountBuilder.append("0");
        amountNegative = false;
        updateAmountDisplay();
    }

    // ══════════════════════════════════════════════
    //  财务分类
    // ══════════════════════════════════════════════
    private void selectCategory3(int idx) {
        if (idx < 0 || idx >= CATEGORY3_LABELS.length) return;
        category3 = idx;
        if (spinnerCategory3 != null && spinnerCategory3.getSelectedItemPosition() != idx) {
            spinnerCategory3.setSelection(idx);
        }
    }

    // ══════════════════════════════════════════════
    //  确认记账
    // ══════════════════════════════════════════════
    private void onConfirm() {
        double amount;
        try {
            amount = Double.parseDouble(amountBuilder.toString());
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "请输入有效金额", Toast.LENGTH_SHORT).show();
            return;
        }
        amount = Math.abs(amount);
        if (amount <= 0) {
            Toast.makeText(requireContext(), "金额必须大于0", Toast.LENGTH_SHORT).show();
            return;
        }
        if (category3 == -1 && type == 0) {
            Toast.makeText(requireContext(), "请选择财务分类", Toast.LENGTH_SHORT).show();
            return;
        }

        Transaction transaction = new Transaction(
                type, amount,
                selectedCategory1, selectedCategory2,
                category3,
                selectedDate.getTimeInMillis(),
                editNote.getText().toString().trim(),
                selectedAccountId
        );

        if (editMode) {
            transaction.setId(editId);
            viewModel.update(transaction);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "已保存！", Toast.LENGTH_SHORT).show();
                    dismiss();
                });
            }
        } else {
            viewModel.insert(transaction, () -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "记账成功！", Toast.LENGTH_SHORT).show();
                        dismiss();
                    });
                }
            });
        }
    }

    // ══════════════════════════════════════════════
    //  层级导航
    // ══════════════════════════════════════════════
    private void showLevel(int lvl) {
        if (lvl == 3) {
            contentScroll.setVisibility(View.GONE);
            level2.setVisibility(View.GONE);
            level3.setVisibility(View.VISIBLE);
            placeTypeToggle(true);
            btnBack.setVisibility(View.VISIBLE);
            applyFormLayoutWeights();
        } else if (lvl == 2) {
            contentScroll.setVisibility(View.GONE);
            level2.setVisibility(View.VISIBLE);
            level3.setVisibility(View.GONE);
            placeTypeToggle(true);
            btnBack.setVisibility(View.VISIBLE);
            collapseNotePanel();
            hidePickerPanels();
        } else {
            contentScroll.setVisibility(View.VISIBLE);
            level2.setVisibility(View.GONE);
            level3.setVisibility(View.GONE);
            placeTypeToggle(false);
            btnBack.setVisibility(View.VISIBLE);
            titleText.setText("选择类别");
            collapseNotePanel();
            hidePickerPanels();
        }
    }

    private void onBack() {
        if (noteState == NOTE_EDITING || noteState == NOTE_DOCKED) {
            collapseNotePanel();
            return;
        }
        if (panelDatePicker.getVisibility() == View.VISIBLE
                || panelTimePicker.getVisibility() == View.VISIBLE) {
            hidePickerPanels();
            return;
        }
        if (level3.getVisibility() == View.VISIBLE) {
            hidePickerPanels();
            if (selectedCategory1 != null) {
                String[] parts = selectedCategory1.split(" ", 2);
                titleText.setText(parts.length > 1 ? parts[1] : selectedCategory1);
            }
            showLevel(2);
        } else if (level2.getVisibility() == View.VISIBLE) {
            btnBack.setVisibility(View.VISIBLE);
            titleText.setText("选择类别");
            showLevel(1);
        } else {
            // Level 1 → 关闭弹窗
            dismiss();
        }
    }

}

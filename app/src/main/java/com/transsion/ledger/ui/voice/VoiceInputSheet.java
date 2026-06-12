package com.transsion.ledger.ui.voice;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.transsion.ledger.R;

/**
 * 语音记账（输入法方案）：用户在输入框内用系统键盘自带的语音转文字，点「解析预览」后确认入账。
 *
 * <p>备研：App 内麦克风方案见 {@link VoiceInputMicLegacy}（已停用）。</p>
 */
public class VoiceInputSheet extends DialogFragment {

    public interface Listener {
        void onVoiceResult(VoiceDraft draft);
    }

    private Listener listener;
    private final VoiceTransactionParser parser = new VoiceTransactionParser();
    private VoiceDraft pendingDraft;

    private TextView txtStatus, txtParsePreview, txtExample;
    private EditText editRaw;
    private LinearLayout panelParsePreview, panelActions;
    private MaterialButton btnConfirm, btnParse;
    private MaterialButtonToggleGroup toggleExampleType;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Translucent_NoTitleBar);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.overlay_voice_input, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtStatus = view.findViewById(R.id.txt_voice_status);
        editRaw = view.findViewById(R.id.edit_voice_raw);
        txtParsePreview = view.findViewById(R.id.txt_voice_parse_preview);
        txtExample = view.findViewById(R.id.txt_voice_example);
        panelParsePreview = view.findViewById(R.id.panel_parse_preview);
        panelActions = view.findViewById(R.id.panel_actions);
        btnParse = view.findViewById(R.id.btn_voice_parse);
        btnConfirm = view.findViewById(R.id.btn_voice_confirm);
        toggleExampleType = view.findViewById(R.id.toggle_example_type);

        view.findViewById(R.id.btn_voice_close).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_voice_clear).setOnClickListener(v -> clearInput());
        btnParse.setOnClickListener(v -> runParsePreview());
        btnConfirm.setOnClickListener(v -> confirmAndSubmit());

        toggleExampleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            txtExample.setText(checkedId == R.id.btn_example_income
                    ? R.string.voice_example_income
                    : R.string.voice_example_expense);
        });

        editRaw.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                pendingDraft = null;
                panelParsePreview.setVisibility(View.GONE);
                btnConfirm.setEnabled(false);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        editRaw.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                runParsePreview();
                return true;
            }
            return false;
        });

        txtStatus.setText(R.string.voice_ime_hint);
        btnConfirm.setEnabled(false);

        editRaw.post(() -> {
            editRaw.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editRaw, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    private void clearInput() {
        editRaw.setText("");
        pendingDraft = null;
        panelParsePreview.setVisibility(View.GONE);
        btnConfirm.setEnabled(false);
        txtStatus.setText(R.string.voice_ime_hint);
        editRaw.requestFocus();
    }

    private void runParsePreview() {
        String raw = editRaw.getText().toString().trim();
        if (raw.isEmpty()) {
            Toast.makeText(requireContext(), R.string.voice_ime_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        pendingDraft = parser.parse(raw);
        txtParsePreview.setText(parser.formatPreview(pendingDraft));
        panelParsePreview.setVisibility(View.VISIBLE);
        btnConfirm.setEnabled(true);
        txtStatus.setText(R.string.voice_review_hint);
    }

    private void confirmAndSubmit() {
        if (pendingDraft == null) {
            runParsePreview();
            if (pendingDraft == null) return;
        }
        if (listener != null) listener.onVoiceResult(pendingDraft);
        dismiss();
    }
}

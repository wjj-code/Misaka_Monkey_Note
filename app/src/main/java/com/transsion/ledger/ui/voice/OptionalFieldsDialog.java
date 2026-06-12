package com.transsion.ledger.ui.voice;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.transsion.ledger.R;

import java.util.List;

/** 必填已齐、选填未填时的浮动提示 */
public class OptionalFieldsDialog extends DialogFragment {

    private static final String ARG_ITEMS = "items";

    public static OptionalFieldsDialog newInstance(List<String> labels) {
        OptionalFieldsDialog d = new OptionalFieldsDialog();
        Bundle b = new Bundle();
        b.putStringArray(ARG_ITEMS, labels.toArray(new String[0]));
        d.setArguments(b);
        return d;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_optional_reminder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String[] items = getArguments() != null
                ? getArguments().getStringArray(ARG_ITEMS) : null;
        TextView txtList = view.findViewById(R.id.txt_optional_list);
        if (items != null && items.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < items.length; i++) {
                if (i > 0) sb.append('\n');
                sb.append("· ").append(items[i]).append(" 未填写");
            }
            txtList.setText(sb.toString());
        }
        view.findViewById(R.id.btn_optional_ok).setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
}

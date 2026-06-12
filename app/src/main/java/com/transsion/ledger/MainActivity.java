package com.transsion.ledger;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.transsion.ledger.data.entity.Account;
import com.transsion.ledger.data.entity.Transaction;
import com.transsion.ledger.data.repository.AccountRepository;
import com.transsion.ledger.ui.add.AddTransactionSheet;
import com.transsion.ledger.ui.voice.OptionalFieldsDialog;
import com.transsion.ledger.ui.voice.VoiceDraft;
import com.transsion.ledger.ui.voice.VoiceInputSheet;
import com.transsion.ledger.viewmodel.TransactionViewModel;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private TransactionViewModel transactionVM;
    private boolean voiceLongPressConsumed;

    /* ═══ 备研：App 内麦克风权限（SpeechRecognizer 方案，已停用）═══
    private static final int REQ_RECORD_AUDIO = 1001;
    private Runnable pendingMicCallback;
    private final ActivityResultLauncher<String> micPermissionLauncher = ...

    @Override
    public void ensureRecordAudio(Runnable onGranted) { ... }

    private void onMicPermissionResult(boolean granted) { ... }

    @Override
    public void onRequestPermissionsResult(...) { ... }
    ═══ 备研结束 ═══ */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        transactionVM = new ViewModelProvider(this).get(TransactionViewModel.class);

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHost != null) {
            navController = navHost.getNavController();
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        NavigationUI.setupWithNavController(bottomNav, navController);

        setupVoiceLongPress(bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_add) {
                if (voiceLongPressConsumed) {
                    return false;
                }
                AddTransactionSheet sheet = new AddTransactionSheet();
                sheet.show(getSupportFragmentManager(), "AddTransactionSheet");
                return false;
            }
            NavigationUI.onNavDestinationSelected(item, navController);
            return true;
        });
    }

    private void setupVoiceLongPress(BottomNavigationView bottomNav) {
        bottomNav.post(() -> {
            View addItem = bottomNav.findViewById(R.id.nav_add);
            if (addItem == null) return;
            addItem.setOnLongClickListener(v -> {
                voiceLongPressConsumed = true;
                openVoiceInputSheet();
                v.postDelayed(() -> voiceLongPressConsumed = false, 500);
                return true;
            });
        });
    }

    private void openVoiceInputSheet() {
        VoiceInputSheet sheet = new VoiceInputSheet();
        sheet.setListener(this::handleVoiceResult);
        sheet.show(getSupportFragmentManager(), "VoiceInputSheet");
    }

    private void handleVoiceResult(VoiceDraft draft) {
        if (!draft.canSaveDirectly()) {
            Toast.makeText(this, R.string.voice_open_edit, Toast.LENGTH_SHORT).show();
            if (navController != null) {
                navController.navigate(R.id.nav_transaction_edit, draft.toBundle());
            }
            return;
        }

        AccountRepository accountRepo = new AccountRepository(getApplication());
        accountRepo.fetchAll(accounts -> {
            long accountId = resolveAccountId(accounts, draft.accountName);
            Transaction tx = new Transaction(
                    draft.type,
                    draft.amount,
                    draft.category1,
                    draft.category2,
                    draft.isExpense() ? draft.category3 : -1,
                    draft.dateTime,
                    draft.note,
                    accountId
            );
            transactionVM.insert(tx, () -> runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, R.string.voice_saved, Toast.LENGTH_SHORT).show();
                List<String> optional = draft.getMissingOptionalLabels();
                if (!optional.isEmpty()) {
                    OptionalFieldsDialog.newInstance(optional)
                            .show(getSupportFragmentManager(), "OptionalFieldsDialog");
                }
            }));
        });
    }

    private long resolveAccountId(List<Account> accounts, String accountName) {
        if (accounts == null || accounts.isEmpty()) return 0;
        if (accountName == null || accountName.trim().isEmpty()) return 0;
        String key = accountName.trim();
        for (Account a : accounts) {
            if (a.getName() != null && a.getName().contains(key)) {
                return a.getId();
            }
        }
        return 0;
    }
}

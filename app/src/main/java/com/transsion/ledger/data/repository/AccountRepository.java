package com.transsion.ledger.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.transsion.ledger.data.dao.AccountDao;
import com.transsion.ledger.data.dao.TransactionDao;
import com.transsion.ledger.data.db.AppDatabase;
import com.transsion.ledger.data.entity.Account;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AccountRepository {

    private final AppDatabase db;
    private final AccountDao dao;
    private final TransactionDao transactionDao;
    private final ExecutorService executor;

    public AccountRepository(Application application) {
        db = AppDatabase.getInstance(application);
        dao = db.accountDao();
        transactionDao = db.transactionDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Account>> getAll() { return dao.getAll(); }

    public List<Account> getAllSync() { return dao.getAllSync(); }

    /** 一次性读取账户列表，避免 LiveData 长期监听导致后续账户变更重复触发回调 */
    public void fetchAll(Consumer<List<Account>> onResult) {
        executor.execute(() -> onResult.accept(dao.getAllSync()));
    }

    public LiveData<Account> getDefault() { return dao.getDefault(); }
    public Account getDefaultSync() { return dao.getDefaultSync(); }
    public Account getById(long id) { return dao.getById(id); }

    public void insert(Account account, Runnable onComplete) {
        executor.execute(() -> {
            long id = dao.insert(account);
            account.setId(id);
            if (onComplete != null) onComplete.run();
        });
    }

    public void update(Account account) {
        executor.execute(() -> dao.update(account));
    }

    /**
     * 安全删除：禁止删默认账户；其余账户的余额并入默认账户，关联账单改挂默认账户。
     * @return true 表示已删除
     */
    public boolean deleteSafe(Account account) {
        if (account == null || account.isDefault()) return false;
        Account stored = dao.getById(account.getId());
        if (stored == null || stored.isDefault()) return false;

        db.runInTransaction(() -> {
            Account defaultAcc = dao.getDefaultSync();
            if (defaultAcc == null) return;

            defaultAcc.setBalance(defaultAcc.getBalance() + stored.getBalance());
            dao.update(defaultAcc);

            transactionDao.reassignAccount(stored.getId(), defaultAcc.getId());
            dao.delete(stored);
        });
        return true;
    }

    public void delete(Account account, Consumer<Boolean> onResult) {
        executor.execute(() -> {
            boolean ok = deleteSafe(account);
            if (onResult != null) onResult.accept(ok);
        });
    }

    public void setAsDefault(long id) {
        executor.execute(() -> db.runInTransaction(() -> {
            dao.clearDefaults();
            dao.setDefault(id);
        }));
    }
}

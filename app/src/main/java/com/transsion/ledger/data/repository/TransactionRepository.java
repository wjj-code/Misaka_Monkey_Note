package com.transsion.ledger.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.transsion.ledger.data.dao.AccountDao;
import com.transsion.ledger.data.dao.TransactionDao;
import com.transsion.ledger.data.db.AppDatabase;
import com.transsion.ledger.data.entity.Account;
import com.transsion.ledger.data.entity.DailySummary;
import com.transsion.ledger.data.entity.MonthSummary;
import com.transsion.ledger.data.entity.Transaction;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 数据仓库 — 当前封装 Room 本地存储，后续可替换为云端 API。
 * 写操作在单线程 Executor 内执行，并通过 {@link AppDatabase#runInTransaction(Runnable)}
 * 保证「账户余额 + 账单」联动原子性。
 */
public class TransactionRepository {

    private final AppDatabase db;
    private final TransactionDao dao;
    private final AccountDao accountDao;
    private final ExecutorService executor;

    public TransactionRepository(Application application) {
        db = AppDatabase.getInstance(application);
        dao = db.transactionDao();
        accountDao = db.accountDao();
        executor = Executors.newSingleThreadExecutor();
    }

    // ---- 查询（LiveData，UI 层直接观察） ----

    public LiveData<List<Transaction>> getAll() {
        return dao.getAll();
    }

    public LiveData<List<Transaction>> getByYearMonth(String yearMonth) {
        return dao.getByYearMonth(yearMonth);
    }

    public LiveData<List<Transaction>> getByDate(String date) {
        return dao.getByDate(date);
    }

    public LiveData<Transaction> getById(long id) {
        return dao.getById(id);
    }

    public LiveData<List<Transaction>> getByDateRange(long start, long end) {
        return dao.getByDateRange(start, end);
    }

    public LiveData<MonthSummary> getMonthSummary(String yearMonth) {
        return dao.getMonthSummary(yearMonth);
    }

    public LiveData<List<DailySummary>> getDailySummaryByMonth(String yearMonth) {
        return dao.getDailySummaryByMonth(yearMonth);
    }

    // ---- 写操作 ----

    public void insert(Transaction transaction, Runnable onComplete) {
        executor.execute(() -> {
            db.runInTransaction(() -> {
                Account acc = resolveAccount(transaction.getAccountId());
                if (acc != null) {
                    transaction.setAccountId(acc.getId());
                    applyBalance(acc, signedAmount(transaction));
                }
                dao.insert(transaction);
            });
            if (onComplete != null) onComplete.run();
        });
    }

    public void update(Transaction transaction) {
        executor.execute(() -> db.runInTransaction(() -> {
            Transaction old = dao.getByIdSync(transaction.getId());
            if (old != null) {
                Account oldAcc = resolveAccount(old.getAccountId());
                if (oldAcc != null) applyBalance(oldAcc, -signedAmount(old));
            }
            Account acc = resolveAccount(transaction.getAccountId());
            if (acc != null) {
                transaction.setAccountId(acc.getId());
                applyBalance(acc, signedAmount(transaction));
            }
            dao.update(transaction);
        }));
    }

    public void delete(Transaction transaction) {
        executor.execute(() -> db.runInTransaction(() -> {
            Account acc = resolveAccount(transaction.getAccountId());
            if (acc != null) applyBalance(acc, -signedAmount(transaction));
            dao.delete(transaction);
        }));
    }

    public void deleteById(long id) {
        executor.execute(() -> db.runInTransaction(() -> {
            Transaction t = dao.getByIdSync(id);
            if (t != null) {
                Account acc = resolveAccount(t.getAccountId());
                if (acc != null) applyBalance(acc, -signedAmount(t));
            }
            dao.deleteById(id);
        }));
    }

    // ---- 账户余额联动辅助（须在 runInTransaction 内调用） ----

    private double signedAmount(Transaction t) {
        return t.getType() == 1 ? t.getAmount() : -t.getAmount();
    }

    private Account resolveAccount(long accountId) {
        Account acc = accountId > 0 ? accountDao.getById(accountId) : null;
        if (acc == null) acc = accountDao.getDefaultSync();
        return acc;
    }

    private void applyBalance(Account account, double delta) {
        account.setBalance(account.getBalance() + delta);
        accountDao.update(account);
    }
}

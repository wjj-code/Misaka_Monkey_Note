package com.transsion.ledger.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.transsion.ledger.data.entity.DailySummary;
import com.transsion.ledger.data.entity.MonthSummary;
import com.transsion.ledger.data.entity.Transaction;
import com.transsion.ledger.data.repository.TransactionRepository;

import java.util.List;

public class TransactionViewModel extends AndroidViewModel {

    private final TransactionRepository repository;

    public TransactionViewModel(Application application) {
        super(application);
        repository = new TransactionRepository(application);
    }

    public LiveData<List<Transaction>> getAll() {
        return repository.getAll();
    }

    public LiveData<List<Transaction>> getByYearMonth(String yearMonth) {
        return repository.getByYearMonth(yearMonth);
    }

    public LiveData<List<Transaction>> getByDate(String date) {
        return repository.getByDate(date);
    }

    public LiveData<List<Transaction>> getByDateRange(long startMillis, long endMillis) {
        return repository.getByDateRange(startMillis, endMillis);
    }

    public LiveData<Transaction> getById(long id) {
        return repository.getById(id);
    }

    public LiveData<MonthSummary> getMonthSummary(String yearMonth) {
        return repository.getMonthSummary(yearMonth);
    }

    public LiveData<List<DailySummary>> getDailySummaryByMonth(String yearMonth) {
        return repository.getDailySummaryByMonth(yearMonth);
    }

    public void insert(Transaction transaction) {
        repository.insert(transaction, null);
    }

    public void insert(Transaction transaction, Runnable onComplete) {
        repository.insert(transaction, onComplete);
    }

    public void update(Transaction transaction) {
        repository.update(transaction);
    }

    public void delete(Transaction transaction) {
        repository.delete(transaction);
    }

    public void deleteById(long id) {
        repository.deleteById(id);
    }
}

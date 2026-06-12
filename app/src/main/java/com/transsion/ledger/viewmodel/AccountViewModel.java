package com.transsion.ledger.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.transsion.ledger.data.entity.Account;
import com.transsion.ledger.data.repository.AccountRepository;

import java.util.List;
import java.util.function.Consumer;

public class AccountViewModel extends AndroidViewModel {

    private final AccountRepository repository;

    public AccountViewModel(Application application) {
        super(application);
        repository = new AccountRepository(application);
    }

    public LiveData<List<Account>> getAll() { return repository.getAll(); }
    public LiveData<Account> getDefault() { return repository.getDefault(); }
    public Account getDefaultSync() { return repository.getDefaultSync(); }
    public Account getById(long id) { return repository.getById(id); }

    public void insert(Account account, Runnable onComplete) {
        repository.insert(account, onComplete);
    }

    public void update(Account account) { repository.update(account); }
    public void delete(Account account, Consumer<Boolean> onResult) {
        repository.delete(account, onResult);
    }
    public void setAsDefault(long id) { repository.setAsDefault(id); }
}

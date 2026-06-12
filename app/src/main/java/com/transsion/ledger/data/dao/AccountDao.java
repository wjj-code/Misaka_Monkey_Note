package com.transsion.ledger.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.transsion.ledger.data.entity.Account;

import java.util.List;

@Dao
public interface AccountDao {

    @Insert
    long insert(Account account);

    @Update
    void update(Account account);

    @Delete
    void delete(Account account);

    @Query("SELECT * FROM accounts ORDER BY isDefault DESC, id ASC")
    LiveData<List<Account>> getAll();

    @Query("SELECT * FROM accounts ORDER BY isDefault DESC, id ASC")
    List<Account> getAllSync();

    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    LiveData<Account> getDefault();

    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    Account getDefaultSync();

    @Query("UPDATE accounts SET isDefault = 0")
    void clearDefaults();

    @Query("UPDATE accounts SET isDefault = 1 WHERE id = :id")
    void setDefault(long id);

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    Account getById(long id);
}

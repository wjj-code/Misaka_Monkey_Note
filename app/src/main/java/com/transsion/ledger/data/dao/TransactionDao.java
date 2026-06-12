package com.transsion.ledger.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.transsion.ledger.data.entity.DailySummary;
import com.transsion.ledger.data.entity.MonthSummary;
import com.transsion.ledger.data.entity.Transaction;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    long insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("DELETE FROM transactions WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    int countByAccountId(long accountId);

    @Query("UPDATE transactions SET accountId = :newAccountId WHERE accountId = :oldAccountId")
    void reassignAccount(long oldAccountId, long newAccountId);

    @Query("SELECT * FROM transactions ORDER BY dateTime DESC")
    List<Transaction> getAllSync();

    /** 按 id 同步查询（用于改/删时冲销账户余额，须在 IO 线程调用） */
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    Transaction getByIdSync(long id);

    /** 所有记录，按时间降序 */
    @Query("SELECT * FROM transactions ORDER BY dateTime DESC")
    LiveData<List<Transaction>> getAll();

    /** 按月份范围查询（时间戳范围） */
    @Query("SELECT * FROM transactions WHERE dateTime BETWEEN :startMillis AND :endMillis ORDER BY dateTime DESC")
    LiveData<List<Transaction>> getByDateRange(long startMillis, long endMillis);

    /** 按年月查询（字符串 yyyy-MM，本地时区） */
    @Query("SELECT * FROM transactions WHERE strftime('%Y-%m', datetime(dateTime / 1000, 'unixepoch', 'localtime')) = :yearMonth ORDER BY dateTime DESC")
    LiveData<List<Transaction>> getByYearMonth(String yearMonth);

    /** 某天的记录（本地时区 yyyy-MM-dd） */
    @Query("SELECT * FROM transactions WHERE date(dateTime / 1000, 'unixepoch', 'localtime') = :date ORDER BY dateTime DESC")
    LiveData<List<Transaction>> getByDate(String date);

    /** 单条记录（编辑页观察） */
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    LiveData<Transaction> getById(long id);

    /** 按月汇总收支（本地时区） */
    @Query("SELECT strftime('%Y-%m', datetime(dateTime / 1000, 'unixepoch', 'localtime')) AS month, " +
            "SUM(CASE WHEN type = 1 THEN amount ELSE 0 END) AS total_income, " +
            "SUM(CASE WHEN type = 0 THEN amount ELSE 0 END) AS total_expense " +
            "FROM transactions " +
            "WHERE strftime('%Y-%m', datetime(dateTime / 1000, 'unixepoch', 'localtime')) = :yearMonth " +
            "GROUP BY month")
    LiveData<MonthSummary> getMonthSummary(String yearMonth);

    /** 按日汇总（用于日历视图，本地时区） */
    @Query("SELECT date(dateTime / 1000, 'unixepoch', 'localtime') AS date, " +
            "SUM(amount) AS total, " +
            "SUM(CASE WHEN type = 1 THEN amount ELSE 0 END) AS income, " +
            "SUM(CASE WHEN type = 0 THEN amount ELSE 0 END) AS expense " +
            "FROM transactions " +
            "WHERE strftime('%Y-%m', datetime(dateTime / 1000, 'unixepoch', 'localtime')) = :yearMonth " +
            "GROUP BY date " +
            "ORDER BY date")
    LiveData<List<DailySummary>> getDailySummaryByMonth(String yearMonth);
}

package com.transsion.ledger.data.db;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.transsion.ledger.data.dao.AccountDao;
import com.transsion.ledger.data.dao.TransactionDao;
import com.transsion.ledger.data.entity.Account;
import com.transsion.ledger.data.entity.Transaction;

@Database(entities = {Transaction.class, Account.class}, version = 3, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract TransactionDao transactionDao();
    public abstract AccountDao accountDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER NOT NULL DEFAULT 0");
            database.execSQL("CREATE TABLE IF NOT EXISTS accounts (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT, " +
                    "balance REAL NOT NULL, " +
                    "type TEXT, " +
                    "isActive INTEGER NOT NULL, " +
                    "includeInNetWorth INTEGER NOT NULL, " +
                    "canExpense INTEGER NOT NULL, " +
                    "canIncome INTEGER NOT NULL, " +
                    "canTransferIn INTEGER NOT NULL, " +
                    "canTransferOut INTEGER NOT NULL, " +
                    "isDefault INTEGER NOT NULL, " +
                    "cardNumber TEXT, " +
                    "note TEXT)");
            insertDefaultAccountIfEmpty(database);
        }
    };

    /**
     * v2→v3：Entity 与 v2 schema 一致，禁止 DROP 账户表。
     * 仅在没有账户时补一条默认账户（兜底）。
     */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            insertDefaultAccountIfEmpty(database);
        }
    };

    private static void insertDefaultAccountIfEmpty(@NonNull SupportSQLiteDatabase database) {
        int count = 0;
        try (Cursor c = database.query("SELECT COUNT(*) FROM accounts")) {
            if (c.moveToFirst()) count = c.getInt(0);
        }
        if (count > 0) return;
        database.execSQL("INSERT INTO accounts (name, balance, type, isActive, includeInNetWorth, " +
                "canExpense, canIncome, canTransferIn, canTransferOut, isDefault, cardNumber, note) " +
                "VALUES ('默认账户', 0, '活期', 1, 1, 1, 1, 1, 1, 1, '', '')");
    }

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "ledger.db"
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build();
                }
            }
        }
        return INSTANCE;
    }

    /** 测试或特殊场景下重置单例 */
    public static void resetInstance() {
        INSTANCE = null;
    }
}

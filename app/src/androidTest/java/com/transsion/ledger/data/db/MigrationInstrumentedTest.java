package com.transsion.ledger.data.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.transsion.ledger.data.entity.Account;
import com.transsion.ledger.data.entity.Transaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

/**
 * 验证 v2→v3 迁移不丢失账户与账单（回归旧版 DROP accounts 问题）。
 */
@RunWith(AndroidJUnit4.class)
public class MigrationInstrumentedTest {

    private static final String TEST_DB = "migration-test.db";

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(TEST_DB);
        AppDatabase.resetInstance();
    }

    @After
    public void tearDown() {
        context.deleteDatabase(TEST_DB);
        AppDatabase.resetInstance();
    }

    @Test
    public void migrate2To3_preservesAccountsAndTransactions() throws IOException {
        createVersion2DatabaseWithSampleData();

        AppDatabase db = Room.databaseBuilder(context, AppDatabase.class, TEST_DB)
                .addMigrations(AppDatabase.MIGRATION_2_3)
                .build();

        List<Account> accounts = db.accountDao().getAllSync();
        assertEquals(2, accounts.size());

        List<Transaction> txs = db.transactionDao().getAllSync();
        assertEquals(1, txs.size());
        assertEquals(15.0, txs.get(0).getAmount(), 0.001);
        assertTrue(txs.get(0).getAccountId() > 0);

        db.close();
    }

    private void createVersion2DatabaseWithSampleData() {
        SupportSQLiteOpenHelper helper = new FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(TEST_DB)
                        .callback(new SupportSQLiteOpenHelper.Callback(2) {
                            @Override
                            public void onCreate(SupportSQLiteDatabase db) {
                                db.execSQL("CREATE TABLE transactions (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                        "type INTEGER NOT NULL, amount REAL NOT NULL, " +
                                        "category1 TEXT, category2 TEXT, category3 INTEGER NOT NULL, " +
                                        "dateTime INTEGER NOT NULL, note TEXT, " +
                                        "accountId INTEGER NOT NULL DEFAULT 0)");
                                db.execSQL("CREATE TABLE accounts (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                        "name TEXT, balance REAL NOT NULL, type TEXT, " +
                                        "isActive INTEGER NOT NULL, includeInNetWorth INTEGER NOT NULL, " +
                                        "canExpense INTEGER NOT NULL, canIncome INTEGER NOT NULL, " +
                                        "canTransferIn INTEGER NOT NULL, canTransferOut INTEGER NOT NULL, " +
                                        "isDefault INTEGER NOT NULL, cardNumber TEXT, note TEXT)");
                                db.execSQL("INSERT INTO accounts (name, balance, type, isActive, " +
                                        "includeInNetWorth, canExpense, canIncome, canTransferIn, " +
                                        "canTransferOut, isDefault, cardNumber, note) " +
                                        "VALUES ('默认账户', 100, '活期', 1,1,1,1,1,1,1,'','')");
                                db.execSQL("INSERT INTO accounts (name, balance, type, isActive, " +
                                        "includeInNetWorth, canExpense, canIncome, canTransferIn, " +
                                        "canTransferOut, isDefault, cardNumber, note) " +
                                        "VALUES ('微信', 50, '活期', 1,1,1,1,1,1,0,'','')");
                                db.execSQL("INSERT INTO transactions (type, amount, category1, category2, " +
                                        "category3, dateTime, note, accountId) VALUES " +
                                        "(0, 15, '🍚 吃', '早餐', 1, 1718000000000, '测试', 2)");
                            }

                            @Override
                            public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
                                // not used
                            }
                        })
                        .build());
        SupportSQLiteDatabase db = helper.getWritableDatabase();
        db.close();
        helper.close();
    }
}

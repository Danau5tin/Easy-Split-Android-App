package com.splitreceipt.myapplication.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_DATE
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_ITEMS
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_ITEMS_VALUE
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_PAID_BY
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_ID
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_TABLE_NAME

class DbHelper(context: Context) : SQLiteOpenHelper(context,
    DATABASE_NAME, null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "receiptSplit.db"
        private const val DATABASE_VERSION = 1
        private const val CREATE_ACCOUNT_TABLE = "CREATE TABLE IF NOT EXISTS $ACCOUNT_TABLE_NAME (" +
                "$ACCOUNT_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$ACCOUNT_COL_DATE TEXT, " +
                "$ACCOUNT_COL_PAID_BY TEXT, " +
                "$ACCOUNT_COL_ITEMS TEXT, " +
                "$ACCOUNT_COL_ITEMS_VALUE TEXT)"
        private const val DELETE_ACCOUNT_ENTRIES = "DROP TABLE IF EXISTS $ACCOUNT_TABLE_NAME"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL(CREATE_ACCOUNT_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL(DELETE_ACCOUNT_ENTRIES)
    }

}
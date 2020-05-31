package com.splitreceipt.myapplication.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_BALANCES
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_CATEGORY
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_NAME
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_ID
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_PARTICIPANTS
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_UNIQUE_ID
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_TABLE_NAME
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptItemsTable.ITEMS_COL_FK_RECEIPT_ID
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptItemsTable.ITEMS_COL_ID
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptItemsTable.ITEMS_COL_NAME
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptItemsTable.ITEMS_COL_VALUE
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptItemsTable.ITEMS_COL_WHOME
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptItemsTable.ITEMS_TABLE_NAME
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_DATE
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_FK_ACCOUNT_ID
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_ID
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_PAID_BY
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_TITLE
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_TOTAL
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_UNIQUE_ID
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_TABLE_NAME

class DbHelper(context: Context) : SQLiteOpenHelper(context,
    DATABASE_NAME, null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "receiptSplit.db"
        private const val DATABASE_VERSION = 1

        private const val CREATE_ACCOUNT_TABLE = "CREATE TABLE $ACCOUNT_TABLE_NAME (" +
                "$ACCOUNT_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$ACCOUNT_COL_UNIQUE_ID TEXT, " +
                "$ACCOUNT_COL_NAME TEXT, " +
                "$ACCOUNT_COL_CATEGORY TEXT, " +
                "$ACCOUNT_COL_PARTICIPANTS TEXT, " +
                "$ACCOUNT_COL_BALANCES TEXT)"
        private const val DELETE_ACCOUNT_ENTRIES = "DROP TABLE IF EXISTS $ACCOUNT_TABLE_NAME"

        private const val CREATE_RECEIPT_TABLE = "CREATE TABLE $RECEIPT_TABLE_NAME (" +
                "$RECEIPT_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$RECEIPT_COL_UNIQUE_ID TEXT, " +
                "$RECEIPT_COL_DATE TEXT, " +
                "$RECEIPT_COL_TITLE TEXT, " +
                "$RECEIPT_COL_TOTAL REAL, " +
                "$RECEIPT_COL_PAID_BY TEXT, " +
                "$RECEIPT_COL_FK_ACCOUNT_ID INTEGER, " +
                "FOREIGN KEY ($RECEIPT_COL_FK_ACCOUNT_ID) REFERENCES $ACCOUNT_TABLE_NAME" +
                "($ACCOUNT_COL_ID) ON DELETE CASCADE)"
        private const val DELETE_RECEIPT_ENTRIES = "DROP TABLE IF EXISTS $RECEIPT_TABLE_NAME"

        private const val CREATE_RECEIPT_ITEMS_TABLE = "CREATE TABLE $ITEMS_TABLE_NAME (" +
                "$ITEMS_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$ITEMS_COL_NAME TEXT, " +
                "$ITEMS_COL_VALUE REAL, " +
                "$ITEMS_COL_WHOME TEXT, " +
                "$ITEMS_COL_FK_RECEIPT_ID INTEGER, " +
                "FOREIGN KEY ($ITEMS_COL_FK_RECEIPT_ID) REFERENCES $RECEIPT_TABLE_NAME" +
                "($RECEIPT_COL_ID) ON DELETE CASCADE)"
        private const val DELETE_RECEIPT_ITEMS_ENTRIES = "DROP TABLE IF EXISTS $ITEMS_TABLE_NAME"
    }

    override fun onOpen(db: SQLiteDatabase?) {
        db!!.execSQL("PRAGMA foreign_keys=ON")
        super.onOpen(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL(CREATE_ACCOUNT_TABLE)
        db.execSQL(CREATE_RECEIPT_TABLE)
        db.execSQL(CREATE_RECEIPT_ITEMS_TABLE)

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL(DELETE_ACCOUNT_ENTRIES)
        db.execSQL(DELETE_RECEIPT_ENTRIES)
        db.execSQL(DELETE_RECEIPT_ITEMS_ENTRIES)
    }

}
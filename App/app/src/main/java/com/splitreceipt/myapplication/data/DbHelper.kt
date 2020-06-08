package com.splitreceipt.myapplication.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_BALANCES
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_CATEGORY
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_NAME
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_ID
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_PARTICIPANTS
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_UNIQUE_ID
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_SETTLEMENTS
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_USER
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_TABLE_NAME
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_FK_RECEIPT_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_NAME
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_VALUE
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_WHOME
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_TABLE_NAME
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_CONTRIBUTIONS
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_DATE
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_FK_GROUP_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_PAID_BY
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_TITLE
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_TOTAL
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_UNIQUE_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_TABLE_NAME

class DbHelper(context: Context) : SQLiteOpenHelper(context,
    DATABASE_NAME, null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "newdata.db"
        private const val DATABASE_VERSION = 1

        private const val CREATE_GROUP_TABLE = "CREATE TABLE $GROUP_TABLE_NAME (" +
                "$GROUP_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$GROUP_COL_UNIQUE_ID TEXT, " +
                "$GROUP_COL_NAME TEXT, " +
                "$GROUP_COL_CATEGORY TEXT, " +
                "$GROUP_COL_PARTICIPANTS TEXT, " +
                "$GROUP_COL_BALANCES TEXT, " +
                "$GROUP_COL_SETTLEMENTS TEXT," +
                "$GROUP_COL_USER TEXT)"
        private const val DELETE_GROUP_ENTRIES = "DROP TABLE IF EXISTS $GROUP_TABLE_NAME"

        private const val CREATE_RECEIPT_TABLE = "CREATE TABLE $RECEIPT_TABLE_NAME (" +
                "$RECEIPT_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$RECEIPT_COL_UNIQUE_ID TEXT, " +
                "$RECEIPT_COL_DATE TEXT, " +
                "$RECEIPT_COL_TITLE TEXT, " +
                "$RECEIPT_COL_TOTAL REAL, " +
                "$RECEIPT_COL_PAID_BY TEXT, " +
                "$RECEIPT_COL_FK_GROUP_ID INTEGER, " +
                "$RECEIPT_COL_CONTRIBUTIONS TEXT, " +
                "FOREIGN KEY ($RECEIPT_COL_FK_GROUP_ID) REFERENCES $GROUP_TABLE_NAME" +
                "($GROUP_COL_ID) ON DELETE CASCADE)"
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
        db!!.execSQL(CREATE_GROUP_TABLE)
        db.execSQL(CREATE_RECEIPT_TABLE)
        db.execSQL(CREATE_RECEIPT_ITEMS_TABLE)

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL(DELETE_GROUP_ENTRIES)
        db.execSQL(DELETE_RECEIPT_ENTRIES)
        db.execSQL(DELETE_RECEIPT_ITEMS_ENTRIES)
    }

}
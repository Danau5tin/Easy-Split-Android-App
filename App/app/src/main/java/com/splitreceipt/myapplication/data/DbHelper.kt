package com.splitreceipt.myapplication.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.splitreceipt.myapplication.ExpenseViewActivity
import com.splitreceipt.myapplication.SplitReceiptManuallyFragment
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_BALANCES
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_CATEGORY
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_NAME
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_ID
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_PARTICIPANTS
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_FIREBASE_ID
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_SETTLEMENTS
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_USER
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_TABLE_NAME
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_FK_RECEIPT_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_NAME
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_VALUE
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_OWNERSHIP
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_TABLE_NAME
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_CONTRIBUTIONS
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_DATE
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_FK_GROUP_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_PAID_BY
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_SCANNED
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
                "$GROUP_COL_FIREBASE_ID TEXT, " +
                "$GROUP_COL_NAME TEXT, " +
                "$GROUP_COL_CATEGORY TEXT, " +
                "$GROUP_COL_PARTICIPANTS TEXT, " +
                "$GROUP_COL_BALANCES TEXT, " +
                "$GROUP_COL_SETTLEMENTS TEXT, " +
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
                "$RECEIPT_COL_SCANNED INTEGER, " + //Boolean: 0=False, 1=True
                "FOREIGN KEY ($RECEIPT_COL_FK_GROUP_ID) REFERENCES $GROUP_TABLE_NAME" +
                "($GROUP_COL_ID) ON DELETE CASCADE)"
        private const val DELETE_RECEIPT_ENTRIES = "DROP TABLE IF EXISTS $RECEIPT_TABLE_NAME"

        private const val CREATE_RECEIPT_ITEMS_TABLE = "CREATE TABLE $ITEMS_TABLE_NAME (" +
                "$ITEMS_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$ITEMS_COL_NAME TEXT, " +
                "$ITEMS_COL_VALUE REAL, " +
                "$ITEMS_COL_OWNERSHIP TEXT, " +
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

    fun insertNewGroup(fireBaseId: String, title: String, category: String, participants: String,
                       balances: String, settlements: String, sqlUser: String) : Int{
        val values: ContentValues = ContentValues().apply {
            put(GROUP_COL_FIREBASE_ID, fireBaseId)
            put(GROUP_COL_NAME, title)
            put(GROUP_COL_CATEGORY, category)
            put(GROUP_COL_PARTICIPANTS, participants)
            put(GROUP_COL_BALANCES, balances)
            put(GROUP_COL_SETTLEMENTS, settlements)
            put(GROUP_COL_USER, sqlUser)
        }
        val writer = writableDatabase
        val sqlRow = writer.insert(GROUP_TABLE_NAME, null, values)
        close()
        return sqlRow.toInt()
    }

    fun insertNewReceipt(sqlAccountId: String, recFirebaseId: String, date: String, title: String, total: Float, paidBy: String, contributions: String, scanned: Boolean) : Int{
        val write = writableDatabase
        val scannedInt: Int = if (scanned) { 1 } else { 0 }
        val values = ContentValues().apply {
            put(RECEIPT_COL_UNIQUE_ID, recFirebaseId)
            put(RECEIPT_COL_DATE, date)
            put(RECEIPT_COL_TITLE, title)
            put(RECEIPT_COL_TOTAL, total)
            put(RECEIPT_COL_PAID_BY, paidBy)
            put(RECEIPT_COL_CONTRIBUTIONS, contributions)
            put(RECEIPT_COL_SCANNED, scannedInt)
            put(RECEIPT_COL_FK_GROUP_ID, sqlAccountId)
        }
        val sqlId = write.insert(RECEIPT_TABLE_NAME, null, values)
        return sqlId.toInt()
    }

    fun insertReceiptItems(itemisedProductList: ArrayList<ScannedItemizedProductData>, receiptRowSql: Int){
        val write = writableDatabase
        val sqlFK = receiptRowSql.toString()
        for (product in itemisedProductList) {
            val productName = product.itemName
            val productValue = product.itemValue
            val productOwnership = product.ownership
            val values = ContentValues().apply {
                put(ITEMS_COL_NAME, productName)
                put(ITEMS_COL_VALUE, productValue)
                put(ITEMS_COL_OWNERSHIP, productOwnership)
                put(ITEMS_COL_FK_RECEIPT_ID, sqlFK)
            }
            write.insert(ITEMS_TABLE_NAME, null, values)
        }
    }

    fun updateItemsSql(writeableDB: SQLiteDatabase?, itemizedProductList: ArrayList<ScannedItemizedProductData>) {
        // Updates the products after user has edited and saved scanned receipt
        for (product in itemizedProductList) {
            val productName = product.itemName
            val productValue = product.itemValue
            val productOwnership = product.ownership
            val values = ContentValues().apply {
                put(ITEMS_COL_NAME, productName)
                put(ITEMS_COL_VALUE, productValue)
                put(ITEMS_COL_OWNERSHIP, productOwnership)
            }
            val whereClause = "$ITEMS_COL_ID = ?"
            val whereArgs = arrayOf(product.sqlRowId)
            writeableDB!!.update(ITEMS_TABLE_NAME, values, whereClause, whereArgs)
        }
        close()
    }

    fun retrieveParticipants(participantList: ArrayList<String>, sqlAccountId: String) : ArrayList<String> {
        /*
        Query the sql DB for the current group to find its participants
         */
        participantList.clear()
        val reader = readableDatabase
        val columns = arrayOf(GROUP_COL_PARTICIPANTS)
        val selectClause = "${GROUP_COL_ID} = ?"
        val selectArgs = arrayOf(sqlAccountId)
        val cursor: Cursor = reader.query(
            DbManager.GroupTable.GROUP_TABLE_NAME, columns, selectClause, selectArgs,
            null, null, null)
        val particColIndex = cursor.getColumnIndexOrThrow(GROUP_COL_PARTICIPANTS)
        while (cursor.moveToNext()){
            val participantsString = cursor.getString(particColIndex)
            val splitParticipants = participantsString.split(",")
            for (participant in splitParticipants) {
                participantList.add(participant)
            }
        }
        cursor.close()
        close()
        return participantList
    }

    fun updateReceiptSql(editSqlRowId: String, date: String, title: String, total: Float, paidBy: String, contributionsString: String): String {
        val write = writableDatabase
        val values = ContentValues().apply {
            put(RECEIPT_COL_DATE, date)
            put(RECEIPT_COL_TITLE, title)
            put(RECEIPT_COL_TOTAL, total)
            put(RECEIPT_COL_PAID_BY, paidBy)
            put(RECEIPT_COL_CONTRIBUTIONS, contributionsString)
        }
        val whereClause = "$RECEIPT_COL_ID = ?"
        val whereArgs = arrayOf(editSqlRowId)
        return write.update(RECEIPT_TABLE_NAME, values, whereClause, whereArgs).toString()
    }

    fun readAllGroups() : ArrayList<GroupData>{
        val groupList: ArrayList<GroupData> = ArrayList()
        val reader = readableDatabase
        val columns = arrayOf(GROUP_COL_ID, GROUP_COL_NAME, GROUP_COL_FIREBASE_ID, GROUP_COL_USER)
        val cursor: Cursor = reader.query(GROUP_TABLE_NAME, columns, null, null, null, null, null)
        val groupNameColIndex = cursor.getColumnIndexOrThrow(GROUP_COL_NAME)
        val groupSqlIdIndex = cursor.getColumnIndexOrThrow(GROUP_COL_ID)
        val groupFirebaseIdIndex = cursor.getColumnIndexOrThrow(GROUP_COL_FIREBASE_ID)
        val groupSqlUserIndex = cursor.getColumnIndexOrThrow(GROUP_COL_USER)
        while (cursor.moveToNext()) {
            val groupName = cursor.getString(groupNameColIndex)
            val groupSqlID = cursor.getString(groupSqlIdIndex)
            val groupFirebaseID = cursor.getString(groupFirebaseIdIndex)
            val groupSqlUser = cursor.getString(groupSqlUserIndex)
            groupList.add(GroupData(groupName, groupSqlID, groupFirebaseID, groupSqlUser))
        }
        cursor.close()
        close()
        return groupList
    }

    fun getReceiptProductDetails(sqlRowId: String, itemisedProductList: ArrayList<ScannedItemizedProductData>) : ArrayList<ScannedItemizedProductData> {
        //Takes an initialized list, clears it and updates it with the correct product items
        val reader = readableDatabase
        itemisedProductList.clear()
        val columns = arrayOf(ITEMS_COL_NAME, ITEMS_COL_VALUE, ITEMS_COL_OWNERSHIP, ITEMS_COL_ID)
        val selectClause = "$ITEMS_COL_FK_RECEIPT_ID = ?"
        val selectArgs = arrayOf(sqlRowId)
        val cursor: Cursor = reader.query(ITEMS_TABLE_NAME, columns, selectClause, selectArgs,
            null, null, null)
        val nameColIndex = cursor.getColumnIndexOrThrow(ITEMS_COL_NAME)
        val valueColIndex = cursor.getColumnIndexOrThrow(ITEMS_COL_VALUE)
        val ownershipColIndex = cursor.getColumnIndexOrThrow(ITEMS_COL_OWNERSHIP)
        val sqlRowColIndex = cursor.getColumnIndexOrThrow(ITEMS_COL_ID)
        while (cursor.moveToNext()) {
            val productName = cursor.getString(nameColIndex)
            val productValue = SplitReceiptManuallyFragment.addStringZerosForDecimalPlace(cursor.getString(valueColIndex))
            val productOwner = cursor.getString(ownershipColIndex)
            val productSqlRow = cursor.getInt(sqlRowColIndex).toString()
            itemisedProductList.add(ScannedItemizedProductData(productName, productValue,
                false, productOwner, productSqlRow))
        }
        cursor.close()
        return itemisedProductList
    }

    fun getExpenseDetails(sqlRowId: String){
        // Retrieves the date and contributions of the selected expense.
        var date = ""
        var contributions = ""
        val reader = readableDatabase
        val columns = arrayOf(RECEIPT_COL_DATE, RECEIPT_COL_CONTRIBUTIONS)
        val whereClause = "$RECEIPT_COL_ID = ?"
        val whereArgs = arrayOf(sqlRowId)
        val cursor: Cursor = reader.query(RECEIPT_TABLE_NAME, columns, whereClause, whereArgs, null, null, null)
        val dateIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_DATE)
        val contributionIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_CONTRIBUTIONS)
        while(cursor.moveToNext()) {
            date = cursor.getString(dateIndex)
            contributions = cursor.getString(contributionIndex)
        }
        cursor.close()
        ExpenseViewActivity.expenseDate = date
        ExpenseViewActivity.contributionString = contributions
    }

    fun deleteExpense(sqlRowId: String){
        val write = writableDatabase
        val whereClause = "$RECEIPT_COL_ID = ?"
        val whereArgs = arrayOf(sqlRowId)
        write.delete(RECEIPT_TABLE_NAME, whereClause, whereArgs)
        close()
    }

    fun locatePriorContributions(sqlRowId: String): String{
        val write = writableDatabase
        val columns = arrayOf(RECEIPT_COL_CONTRIBUTIONS)
        val selectClause = "$RECEIPT_COL_ID = ?"
        val selectArgs = arrayOf(sqlRowId)
        val cursor: Cursor = write.query(RECEIPT_TABLE_NAME, columns, selectClause,
            selectArgs, null, null, null)
        val contributionsColIndex = cursor.getColumnIndex(RECEIPT_COL_CONTRIBUTIONS)
        cursor.moveToNext()
        val priorContribs =  cursor.getString(contributionsColIndex).toString()
        cursor.close()
        return priorContribs
    }

}
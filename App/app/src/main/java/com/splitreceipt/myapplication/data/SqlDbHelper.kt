package com.splitreceipt.myapplication.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.splitreceipt.myapplication.ExpenseViewActivity
import com.splitreceipt.myapplication.SplitExpenseManuallyFragment
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_BALANCES
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_NAME
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_ID
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_PARTICIPANTS
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_FIREBASE_ID
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_SETTLEMENTS
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_USER
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_TABLE_NAME
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_FK_EXPENSE_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_NAME
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_VALUE
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_OWNERSHIP
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_TABLE_NAME
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_CONTRIBUTIONS
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_DATE
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_FK_GROUP_ID
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_ID
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_PAID_BY
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_SCANNED
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_TITLE
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_TOTAL
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_FIREBASE_ID
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_LAST_EDIT
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_TABLE_NAME
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_LAST_IMAGE_EDIT

class SqlDbHelper(context: Context) : SQLiteOpenHelper(context,
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
                "$GROUP_COL_PARTICIPANTS TEXT, " +
                "$GROUP_COL_BALANCES TEXT, " +
                "$GROUP_COL_SETTLEMENTS TEXT, " +
                "$GROUP_COL_USER TEXT, " +
                "$GROUP_COL_LAST_IMAGE_EDIT TEXT)"
        private const val DELETE_GROUP_ENTRIES = "DROP TABLE IF EXISTS $GROUP_TABLE_NAME"

        private const val CREATE_RECEIPT_TABLE = "CREATE TABLE $EXPENSE_TABLE_NAME (" +
                "$EXPENSE_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$EXPENSE_COL_FIREBASE_ID TEXT, " +
                "$EXPENSE_COL_DATE TEXT, " +
                "$EXPENSE_COL_TITLE TEXT, " +
                "$EXPENSE_COL_TOTAL REAL, " +
                "$EXPENSE_COL_PAID_BY TEXT, " +
                "$EXPENSE_COL_FK_GROUP_ID INTEGER, " +
                "$EXPENSE_COL_CONTRIBUTIONS TEXT, " +
                "$EXPENSE_COL_LAST_EDIT TEXT, " +
                "$EXPENSE_COL_SCANNED INTEGER, " + //Boolean: 0=False, 1=True
                "FOREIGN KEY ($EXPENSE_COL_FK_GROUP_ID) REFERENCES $GROUP_TABLE_NAME" +
                "($GROUP_COL_ID) ON DELETE CASCADE)"
        private const val DELETE_RECEIPT_ENTRIES = "DROP TABLE IF EXISTS $EXPENSE_TABLE_NAME"

        private const val CREATE_RECEIPT_ITEMS_TABLE = "CREATE TABLE $ITEMS_TABLE_NAME (" +
                "$ITEMS_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$ITEMS_COL_NAME TEXT, " +
                "$ITEMS_COL_VALUE REAL, " +
                "$ITEMS_COL_OWNERSHIP TEXT, " +
                "$ITEMS_COL_FK_EXPENSE_ID INTEGER, " +
                "FOREIGN KEY ($ITEMS_COL_FK_EXPENSE_ID) REFERENCES $EXPENSE_TABLE_NAME" +
                "($EXPENSE_COL_ID) ON DELETE CASCADE)"
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

    fun insertNewGroup(fireBaseId: String, title: String, participants: String,
                       balances: String, settlements: String, sqlUser: String, lastImageEdit: String) : Int{
        val values: ContentValues = ContentValues().apply {
            put(GROUP_COL_FIREBASE_ID, fireBaseId)
            put(GROUP_COL_NAME, title)
            put(GROUP_COL_PARTICIPANTS, participants)
            put(GROUP_COL_BALANCES, balances)
            put(GROUP_COL_SETTLEMENTS, settlements)
            put(GROUP_COL_USER, sqlUser)
            put(GROUP_COL_LAST_IMAGE_EDIT, lastImageEdit)
        }
        val writer = writableDatabase
        val sqlRow = writer.insert(GROUP_TABLE_NAME, null, values)
        close()
        return sqlRow.toInt()
    }

    fun setLastImageEdit(lastEdit: String, sqlGroupId: String?){
        val values = ContentValues().apply {
            put(GROUP_COL_LAST_IMAGE_EDIT, lastEdit)
        }
        val write = writableDatabase
        val where = "$GROUP_COL_ID = ?"
        val whereArgs = arrayOf(sqlGroupId)
        write.update(GROUP_TABLE_NAME, values, where, whereArgs)
        close()
    }

    fun insertNewExpense(sqlGroupId: String, firebaseId: String, date: String, title: String,
                         total: Float, paidBy: String, contributions: String, scanned: Boolean, lastEdit: String) : Int{
        val write = writableDatabase
        val scannedInt: Int = if (scanned) { 1 } else { 0 }
        val values = ContentValues().apply {
            put(EXPENSE_COL_FIREBASE_ID, firebaseId)
            put(EXPENSE_COL_DATE, date)
            put(EXPENSE_COL_TITLE, title)
            put(EXPENSE_COL_TOTAL, total)
            put(EXPENSE_COL_PAID_BY, paidBy)
            put(EXPENSE_COL_CONTRIBUTIONS, contributions)
            put(EXPENSE_COL_SCANNED, scannedInt)
            put(EXPENSE_COL_FK_GROUP_ID, sqlGroupId)
            put(EXPENSE_COL_LAST_EDIT, lastEdit)
        }
        val sqlId = write.insert(EXPENSE_TABLE_NAME, null, values)
        return sqlId.toInt()
    }

    fun setSqlUser(sqlUser: String, sqlAccountId: String){
        val write = writableDatabase
        val values = ContentValues().apply {
            put(GROUP_COL_USER, sqlUser)
        }
        val where = "$GROUP_COL_ID = ?"
        val whereArgs = arrayOf(sqlAccountId)
        write.update(GROUP_TABLE_NAME, values, where, whereArgs)
        close()
    }

    fun insertReceiptItems(itemisedProductList: ArrayList<ScannedItemizedProductData>, expenseRowSql: Int){
        val write = writableDatabase
        val sqlFK = expenseRowSql.toString()
        for (product in itemisedProductList) {
            val productName = product.itemName
            val productValue = product.itemValue
            val productOwnership = product.ownership
            val values = ContentValues().apply {
                put(ITEMS_COL_NAME, productName)
                put(ITEMS_COL_VALUE, productValue)
                put(ITEMS_COL_OWNERSHIP, productOwnership)
                put(ITEMS_COL_FK_EXPENSE_ID, sqlFK)
            }
            write.insert(ITEMS_TABLE_NAME, null, values)
        }
    }

    fun insertReceiptItems(productName:String, productValue: String, productOwnership: String, expenseRowSql: Int) {
        val write = writableDatabase
        val sqlFK = expenseRowSql.toString()
        val values = ContentValues().apply {
            put(ITEMS_COL_NAME, productName)
            put(ITEMS_COL_VALUE, productValue)
            put(ITEMS_COL_OWNERSHIP, productOwnership)
            put(ITEMS_COL_FK_EXPENSE_ID, sqlFK)
        }
        write.insert(ITEMS_TABLE_NAME, null, values)
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

    fun updateExpense(editSqlRowId: String, date: String, title: String, total: Float, paidBy: String, contributionsString: String, lastEdit: String): String {
        // Update expense in Sql db
        val write = writableDatabase
        val values = ContentValues().apply {
            put(EXPENSE_COL_DATE, date)
            put(EXPENSE_COL_TITLE, title)
            put(EXPENSE_COL_TOTAL, total)
            put(EXPENSE_COL_PAID_BY, paidBy)
            put(EXPENSE_COL_CONTRIBUTIONS, contributionsString)
            put(EXPENSE_COL_LAST_EDIT, lastEdit)
        }
        val whereClause = "$EXPENSE_COL_ID = ?"
        val whereArgs = arrayOf(editSqlRowId)
        return write.update(EXPENSE_TABLE_NAME, values, whereClause, whereArgs).toString()
    }

    fun updateGroupName(groupSqlId: String, groupName: String) {
        val write = writableDatabase
        val values = ContentValues().apply {
            put(GROUP_COL_NAME, groupName)
        }
        val where = "$GROUP_COL_ID = ?"
        val whereArgs = arrayOf(groupSqlId)
        write.update(GROUP_TABLE_NAME, values, where, whereArgs)
        close()
    }

    fun retrieveParticipants(participantList: ArrayList<String>, sqlAccountId: String) : ArrayList<String> {
        /*
        Query the sql DB for the current group to find its participants
         */
        participantList.clear()
        val reader = readableDatabase
        val columns = arrayOf(GROUP_COL_PARTICIPANTS)
        val selectClause = "$GROUP_COL_ID = ?"
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

    fun retrieveSqlAccountInfoData(sqlGroupId: String) : FirebaseAccountInfoData {
        val read = readableDatabase
        val columns = arrayOf(GROUP_COL_NAME, GROUP_COL_PARTICIPANTS, GROUP_COL_LAST_IMAGE_EDIT)
        val where = "$GROUP_COL_ID = ?"
        val whereArgs = arrayOf(sqlGroupId)
        val cursor: Cursor = read.query(GROUP_TABLE_NAME, columns, where, whereArgs, null, null, null)
        val nameIndex = cursor.getColumnIndexOrThrow(GROUP_COL_NAME)
        val participantIndex = cursor.getColumnIndexOrThrow(GROUP_COL_PARTICIPANTS)
        val lastImageIndex = cursor.getColumnIndexOrThrow(GROUP_COL_LAST_IMAGE_EDIT)
        cursor.moveToNext()
        val groupName = cursor.getString(nameIndex)
        val participants = cursor.getString(participantIndex)
        val lastEdit = cursor.getString(lastImageIndex)
        return FirebaseAccountInfoData(groupName, participants, lastEdit)
    }

    fun loadSqlSettlementString(sqlAccountId: String?): String {
        var settlementString = ""
        val reader = readableDatabase
        val columns = arrayOf(GROUP_COL_SETTLEMENTS)
        val selectClause = "$GROUP_COL_ID = ?"
        val selectArgs = arrayOf(sqlAccountId)
        val cursor: Cursor = reader.query(
            GROUP_TABLE_NAME, columns, selectClause, selectArgs,
            null, null, null
        )
        val settlementIndex = cursor.getColumnIndexOrThrow(GROUP_COL_SETTLEMENTS)
        while (cursor.moveToNext()) {
            settlementString = cursor.getString(settlementIndex)
        }
        cursor.close()
        close()
        return settlementString
    }

    fun loadPreviousReceipts(sqlId: String?, receiptList: ArrayList<ReceiptData>){
        val reader = readableDatabase
        val columns = arrayOf(
            EXPENSE_COL_DATE, EXPENSE_COL_TITLE, EXPENSE_COL_TOTAL,
            EXPENSE_COL_PAID_BY, EXPENSE_COL_ID, EXPENSE_COL_SCANNED
        )
        val selectClause = "$EXPENSE_COL_FK_GROUP_ID = ?"
        val selectArgs = arrayOf("$sqlId")
        val cursor: Cursor = reader.query(
            EXPENSE_TABLE_NAME, columns, selectClause, selectArgs,
            null, null, "$EXPENSE_COL_ID DESC"
        ) //TODO: Try to sort all expenses in date order. Maybe do this before passing to the adapter?
        val dateColIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_DATE)
        val titleColIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_TITLE)
        val totalColIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_TOTAL)
        val paidByColIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_PAID_BY)
        val idColIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_ID)
        val scannedColIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_SCANNED)
        while (cursor.moveToNext()) {
            val receiptDate = cursor.getString(dateColIndex)
            val receiptTitle = cursor.getString(titleColIndex)
            val receiptTotal = cursor.getFloat(totalColIndex)
            val receiptPaidBy = cursor.getString(paidByColIndex)
            val receiptSqlId = cursor.getInt(idColIndex).toString()
            val receiptScannedInt = cursor.getInt(scannedColIndex)
            val scanned = receiptScannedInt == 1
            receiptList.add(
                ReceiptData(
                    receiptDate,
                    receiptTitle,
                    receiptTotal,
                    receiptPaidBy,
                    receiptSqlId,
                    scanned
                )
            )
        }
        cursor.close()
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
        val selectClause = "$ITEMS_COL_FK_EXPENSE_ID = ?"
        val selectArgs = arrayOf(sqlRowId)
        val cursor: Cursor = reader.query(ITEMS_TABLE_NAME, columns, selectClause, selectArgs,
            null, null, null)
        val nameColIndex = cursor.getColumnIndexOrThrow(ITEMS_COL_NAME)
        val valueColIndex = cursor.getColumnIndexOrThrow(ITEMS_COL_VALUE)
        val ownershipColIndex = cursor.getColumnIndexOrThrow(ITEMS_COL_OWNERSHIP)
        val sqlRowColIndex = cursor.getColumnIndexOrThrow(ITEMS_COL_ID)
        while (cursor.moveToNext()) {
            val productName = cursor.getString(nameColIndex)
            val productValue = SplitExpenseManuallyFragment.addStringZerosForDecimalPlace(cursor.getString(valueColIndex))
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
        var firebaseId = ""
        val reader = readableDatabase
        val columns = arrayOf(EXPENSE_COL_DATE, EXPENSE_COL_CONTRIBUTIONS, EXPENSE_COL_FIREBASE_ID)
        val whereClause = "$EXPENSE_COL_ID = ?"
        val whereArgs = arrayOf(sqlRowId)
        val cursor: Cursor = reader.query(EXPENSE_TABLE_NAME, columns, whereClause, whereArgs, null, null, null)
        val dateIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_DATE)
        val contributionIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_CONTRIBUTIONS)
        val fireBaseIdIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_FIREBASE_ID)
        while(cursor.moveToNext()) {
            date = cursor.getString(dateIndex)
            contributions = cursor.getString(contributionIndex)
            firebaseId = cursor.getString(fireBaseIdIndex)
        }
        cursor.close()
        ExpenseViewActivity.expenseDate = date
        ExpenseViewActivity.contributionString = contributions
        ExpenseViewActivity.firebaseExpenseID = firebaseId
    }

    fun locatePriorContributions(sqlRowId: String): String{
        //Locates the previous contributions string in sql
        val write = writableDatabase
        val columns = arrayOf(EXPENSE_COL_CONTRIBUTIONS)
        val selectClause = "$EXPENSE_COL_ID = ?"
        val selectArgs = arrayOf(sqlRowId)
        val cursor: Cursor = write.query(EXPENSE_TABLE_NAME, columns, selectClause,
            selectArgs, null, null, null)
        val contributionsColIndex = cursor.getColumnIndex(EXPENSE_COL_CONTRIBUTIONS)
        cursor.moveToNext()
        val priorContribs =  cursor.getString(contributionsColIndex).toString()
        cursor.close()
        return priorContribs
    }

    fun retrieveBasicExpenseSqlData(expenseSqlRow: String) : ArrayList<BasicExpenseDataSql>{
        //Goes through the expense table and returns all the firebase expense ID's into a list
        val basicExpenseListSql: ArrayList <BasicExpenseDataSql> = ArrayList()
        val read = readableDatabase
        val columns = arrayOf(EXPENSE_COL_ID, EXPENSE_COL_FIREBASE_ID, EXPENSE_COL_LAST_EDIT)
        val where = "$EXPENSE_COL_FK_GROUP_ID = ?"
        val whereArgs = arrayOf(expenseSqlRow)
        val cursor: Cursor = read.query(EXPENSE_TABLE_NAME, columns, where, whereArgs, null, null, null)
        val sqlRowIdIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_ID)
        val fireIDColIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_FIREBASE_ID)
        val lastEditColIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_LAST_EDIT)
        while (cursor.moveToNext()){
            val sqlRowID: String = cursor.getString(sqlRowIdIndex)
            val fireBaseId: String = cursor.getString(fireIDColIndex)
            val lastEdit: String = cursor.getString(lastEditColIndex)
            basicExpenseListSql.add(BasicExpenseDataSql(sqlRowID, fireBaseId, lastEdit))
        }
        cursor.close()
        close()
        return basicExpenseListSql
    }

    fun deleteExpense(sqlRowId: String){
        //Deletes an entire expense from sql
        val write = writableDatabase
        val whereClause = "$EXPENSE_COL_ID = ?"
        val whereArgs = arrayOf(sqlRowId)
        write.delete(EXPENSE_TABLE_NAME, whereClause, whereArgs)
        close()
    }

    fun deleteReceiptProduct(sqlRowId: String){
        //Deletes an individual product from sql db
        val write = writableDatabase
        val where = "$ITEMS_COL_ID = ?"
        val whereArgs = arrayOf(sqlRowId)
        write.delete(ITEMS_TABLE_NAME, where, whereArgs)
        close()
    }

    fun getSettlementString(sqlGroupId: String?): String? {
        val read = readableDatabase
        val columns = arrayOf(GROUP_COL_BALANCES)
        val where = "$GROUP_COL_ID = ?"
        val whereArgs = arrayOf(sqlGroupId)
        val cursor: Cursor = read.query(GROUP_TABLE_NAME, columns, where, whereArgs, null, null, null)
        val balIndex = cursor.getColumnIndexOrThrow(GROUP_COL_BALANCES)
        cursor.moveToNext()
        val balance = cursor.getString(balIndex)
        cursor.close()
        close()
        return balance
    }

    fun updateParticipants(newParticipantString: String, groupSqlId: String) {
        val write = writableDatabase
        val values = ContentValues().apply {
            put(GROUP_COL_PARTICIPANTS, newParticipantString)
        }
        val where = "$GROUP_COL_ID = ?"
        val whereArgs = arrayOf(groupSqlId)
        write.update(GROUP_TABLE_NAME, values, where, whereArgs)
        close()
    }

    fun updateGroupInfo(firebaseGroupData: FirebaseAccountInfoData, sqlGroupId: String) {
        val write = writableDatabase
        val values = ContentValues().apply {
            put(GROUP_COL_NAME, firebaseGroupData.accName)
            put(GROUP_COL_PARTICIPANTS, firebaseGroupData.accParticipants)
            put(GROUP_COL_LAST_IMAGE_EDIT, firebaseGroupData.accLastImage)
        }
        val where = "$GROUP_COL_ID = ?"
        val whereArgs = arrayOf(sqlGroupId)
        write.update(GROUP_TABLE_NAME, values, where, whereArgs)
    }

}
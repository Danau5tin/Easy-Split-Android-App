package com.splitreceipt.myapplication.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.Telephony
import android.util.Log
import com.splitreceipt.myapplication.ExpenseViewActivity
import com.splitreceipt.myapplication.SplitExpenseManuallyFragment
import com.splitreceipt.myapplication.data.DbManager.CurrencyTable.CURRENCY_COL_BASE
import com.splitreceipt.myapplication.data.DbManager.CurrencyTable.CURRENCY_COL_CODE
import com.splitreceipt.myapplication.data.DbManager.CurrencyTable.CURRENCY_COL_ID
import com.splitreceipt.myapplication.data.DbManager.CurrencyTable.CURRENCY_COL_LAST_UPDATE
import com.splitreceipt.myapplication.data.DbManager.CurrencyTable.CURRENCY_COL_RATE
import com.splitreceipt.myapplication.data.DbManager.CurrencyTable.CURRENCY_TABLE_NAME
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_NAME
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_ID
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_PARTICIPANTS_LAST_EDIT
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
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_CURRENCY
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_DATE
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_EXCHANGE_RATE
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_FK_GROUP_ID
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_ID
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_PAID_BY
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_SCANNED
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_TITLE
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_TOTAL
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_FIREBASE_ID
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_LAST_EDIT
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_COL_UI_SYMBOL
import com.splitreceipt.myapplication.data.DbManager.ExpenseTable.EXPENSE_TABLE_NAME
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_BASE_CURRENCY
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_BASE_CURRENCY_UI_SYMBOL
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_LAST_IMAGE_EDIT
import com.splitreceipt.myapplication.data.DbManager.ParticipantTable.PARTICIPANTS_COL_FK_GROUP_ID
import com.splitreceipt.myapplication.data.DbManager.ParticipantTable.PARTICIPANT_COL_F_BASE_KEY
import com.splitreceipt.myapplication.data.DbManager.ParticipantTable.PARTICIPANT_COL_ID
import com.splitreceipt.myapplication.data.DbManager.ParticipantTable.PARTICIPANT_COL_U_BALANCE
import com.splitreceipt.myapplication.data.DbManager.ParticipantTable.PARTICIPANT_COL_U_NAME
import com.splitreceipt.myapplication.data.DbManager.ParticipantTable.PARTICIPANT_TABLE_NAME

class SqlDbHelper(context: Context) : SQLiteOpenHelper(context,
    DATABASE_NAME, null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "another.db"
        private const val DATABASE_VERSION = 1

        const val CURRENCY_NON_EXISTENT: Long = 0

        private const val CREATE_GROUP_TABLE = "CREATE TABLE $GROUP_TABLE_NAME (" +
                "$GROUP_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$GROUP_COL_FIREBASE_ID TEXT, " +
                "$GROUP_COL_NAME TEXT, " +
                "$GROUP_COL_PARTICIPANTS_LAST_EDIT TEXT, " +
                "$GROUP_COL_SETTLEMENTS TEXT, " +
                "$GROUP_COL_USER TEXT, " +
                "$GROUP_COL_LAST_IMAGE_EDIT TEXT, " +
                "$GROUP_COL_BASE_CURRENCY TEXT," +
                "$GROUP_COL_BASE_CURRENCY_UI_SYMBOL TEXT)"
        private const val DELETE_GROUP_ENTRIES = "DROP TABLE IF EXISTS $GROUP_TABLE_NAME"

        private const val CREATE_EXPENSE_TABLE = "CREATE TABLE $EXPENSE_TABLE_NAME (" +
                "$EXPENSE_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$EXPENSE_COL_FIREBASE_ID TEXT, " +
                "$EXPENSE_COL_DATE TEXT, " +
                "$EXPENSE_COL_TITLE TEXT, " +
                "$EXPENSE_COL_TOTAL REAL, " +
                "$EXPENSE_COL_PAID_BY TEXT, " +
                "$EXPENSE_COL_FK_GROUP_ID INTEGER, " +
                "$EXPENSE_COL_CONTRIBUTIONS TEXT, " +
                "$EXPENSE_COL_LAST_EDIT TEXT, " +
                "$EXPENSE_COL_CURRENCY TEXT, " +
                "$EXPENSE_COL_UI_SYMBOL TEXT, " +
                "$EXPENSE_COL_EXCHANGE_RATE REAL, " +
                "$EXPENSE_COL_SCANNED INTEGER, " + //Boolean: 0=False, 1=True
                "FOREIGN KEY ($EXPENSE_COL_FK_GROUP_ID) REFERENCES $GROUP_TABLE_NAME" +
                "($GROUP_COL_ID) ON DELETE CASCADE)"
        private const val DELETE_EXPENSE_ENTRIES = "DROP TABLE IF EXISTS $EXPENSE_TABLE_NAME"

        private const val CREATE_RECEIPT_ITEMS_TABLE = "CREATE TABLE $ITEMS_TABLE_NAME (" +
                "$ITEMS_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$ITEMS_COL_NAME TEXT, " +
                "$ITEMS_COL_VALUE REAL, " +
                "$ITEMS_COL_OWNERSHIP TEXT, " +
                "$ITEMS_COL_FK_EXPENSE_ID INTEGER, " +
                "FOREIGN KEY ($ITEMS_COL_FK_EXPENSE_ID) REFERENCES $EXPENSE_TABLE_NAME" +
                "($EXPENSE_COL_ID) ON DELETE CASCADE)"
        private const val DELETE_RECEIPT_ITEMS_ENTRIES = "DROP TABLE IF EXISTS $ITEMS_TABLE_NAME"

        private const val CREATE_CURRENCY_TABLE = "CREATE TABLE $CURRENCY_TABLE_NAME (" +
                "$CURRENCY_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$CURRENCY_COL_BASE TEXT, " +
                "$CURRENCY_COL_CODE TEXT, " +
                "$CURRENCY_COL_RATE REAL, " +
                "$CURRENCY_COL_LAST_UPDATE TEXT)"
        private const val DELETE_CURRENCY_ENTRIES = "DROP TABLE IF EXISTS $CURRENCY_TABLE_NAME"

        private const val CREATE_PARTICIPANT_TABLE = "CREATE TABLE $PARTICIPANT_TABLE_NAME (" +
                "$PARTICIPANT_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$PARTICIPANT_COL_F_BASE_KEY TEXT, " +
                "$PARTICIPANT_COL_U_NAME TEXT, " +
                "$PARTICIPANT_COL_U_BALANCE REAL, " +
                "$PARTICIPANTS_COL_FK_GROUP_ID INTEGER, " +
                "FOREIGN KEY ($PARTICIPANTS_COL_FK_GROUP_ID) REFERENCES $GROUP_TABLE_NAME" +
                "($GROUP_COL_ID) ON DELETE CASCADE)"
        private const val DELETE_PARTICIPANT_ENTRIES = "DROP TABLE IF EXISTS $PARTICIPANT_TABLE_NAME"
    }

    override fun onOpen(db: SQLiteDatabase?) {
        db!!.execSQL("PRAGMA foreign_keys=ON")
        super.onOpen(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL(CREATE_GROUP_TABLE)
        db.execSQL(CREATE_EXPENSE_TABLE)
        db.execSQL(CREATE_RECEIPT_ITEMS_TABLE)
        db.execSQL(CREATE_CURRENCY_TABLE)
        db.execSQL(CREATE_PARTICIPANT_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL(DELETE_GROUP_ENTRIES)
        db.execSQL(DELETE_EXPENSE_ENTRIES)
        db.execSQL(DELETE_RECEIPT_ITEMS_ENTRIES)
        db.execSQL(DELETE_CURRENCY_ENTRIES)
        db.execSQL(DELETE_PARTICIPANT_ENTRIES)
    }

    fun insertNewGroup(fireBaseId: String, title: String, participantLastEdit: String,
                       settlements: String, sqlUser: String, lastImageEdit: String,
                       baseCurrency: String, currencyUiSymbol: String) : Int{
        val values: ContentValues = ContentValues().apply {
            put(GROUP_COL_FIREBASE_ID, fireBaseId)
            put(GROUP_COL_NAME, title)
            put(GROUP_COL_PARTICIPANTS_LAST_EDIT, participantLastEdit)
            put(GROUP_COL_SETTLEMENTS, settlements)
            put(GROUP_COL_USER, sqlUser)
            put(GROUP_COL_LAST_IMAGE_EDIT, lastImageEdit)
            put(GROUP_COL_BASE_CURRENCY, baseCurrency)
            put(GROUP_COL_BASE_CURRENCY_UI_SYMBOL, currencyUiSymbol)
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

    fun insertNewExpense(sqlGroupId: String, firebaseId: String, date: String, title: String, total: Float,
                         paidBy: String, contributions: String, scanned: Boolean, lastEdit: String,
                         expenseCurrency: String, uiCurrencySymbol: String, exchangeRate: Float) : Int{
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
            put(EXPENSE_COL_CURRENCY, expenseCurrency)
            put(EXPENSE_COL_UI_SYMBOL, uiCurrencySymbol)
            put(EXPENSE_COL_EXCHANGE_RATE, exchangeRate)
        }
        val sqlId = write.insert(EXPENSE_TABLE_NAME, null, values)
        return sqlId.toInt()
    }

    fun insertCurrency(baseCurrency: String, currencyList: ArrayList<CurrencyData>, timestamp: Long) {
        val write = writableDatabase
        for (currency in currencyList) {
            val values = ContentValues().apply {
                put(CURRENCY_COL_BASE, baseCurrency)
                put(CURRENCY_COL_CODE, currency.currencyCode)
                put(CURRENCY_COL_RATE, currency.rate)
                put(CURRENCY_COL_LAST_UPDATE, timestamp)
            }
            write.insert(CURRENCY_TABLE_NAME, null, values)
            Log.i("Currency", "Inserted base: $baseCurrency for country: ${currency.currencyCode} at rate: ${currency.rate}" )
        }
        close()
    }

    fun updateCurrency(baseCurrency: String, currencyList: ArrayList<CurrencyData>, timestamp: Long) {
        val write = writableDatabase
        for (currency in currencyList) {
            val values = ContentValues().apply {
                put(CURRENCY_COL_RATE, currency.rate)
                put(CURRENCY_COL_LAST_UPDATE, timestamp)
            }
            val where = "$CURRENCY_COL_BASE = ? AND $CURRENCY_COL_CODE = ?"
            val whereArgs = arrayOf(baseCurrency, currency.currencyCode)
            write.update(CURRENCY_TABLE_NAME, values, where, whereArgs)
        }
        close()
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

    fun updateExpense(editSqlRowId: String, date: String, title: String, total: Float, paidBy: String,
                      contributionsString: String, lastEdit: String): String {
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

    fun retrieveParticipants(participantListObjects: ArrayList<ParticipantBalanceData>, sqlGroupId: String, objects: Boolean=true) : ArrayList<ParticipantBalanceData> {
        /*
        Query the sql DB for the current group to find its participants
         */
        participantListObjects.clear()
        val reader = readableDatabase
        val columns = arrayOf(PARTICIPANT_COL_ID, PARTICIPANT_COL_U_NAME, PARTICIPANT_COL_U_BALANCE, PARTICIPANT_COL_F_BASE_KEY)
        val where = "$PARTICIPANTS_COL_FK_GROUP_ID = ?"
        val whereArgs = arrayOf(sqlGroupId)
        val cursor: Cursor = reader.query(PARTICIPANT_TABLE_NAME, columns, where, whereArgs, null, null, null)
        val sqlRowIdIndex = cursor.getColumnIndexOrThrow(PARTICIPANT_COL_ID)
        val uNameIndex = cursor.getColumnIndexOrThrow(PARTICIPANT_COL_U_NAME)
        val uBalanceIndex = cursor.getColumnIndexOrThrow(PARTICIPANT_COL_U_BALANCE)
        val fBaseKeyIndex = cursor.getColumnIndexOrThrow(PARTICIPANT_COL_F_BASE_KEY)
        while (cursor.moveToNext()) {
            val sqlRow = cursor.getString(uNameIndex)
            val uName = cursor.getString(sqlRowIdIndex)
            val uBal = cursor.getFloat(uBalanceIndex)
            val fKey = cursor.getString(fBaseKeyIndex)
            participantListObjects.add(ParticipantBalanceData(uName, uBal, fKey, sqlRow))
        }
        cursor.close()
        close()
        return participantListObjects
    }

    fun retrieveParticipants(participantList: ArrayList<String>, sqlGroupId: String) : ArrayList<String> {
        /*
        Query the sql DB for the current group to find its participants
         */
        participantList.clear()
        val reader = readableDatabase
        val columns = arrayOf(PARTICIPANT_COL_U_NAME)
        val where = "$PARTICIPANTS_COL_FK_GROUP_ID = ?"
        val whereArgs = arrayOf(sqlGroupId)
        val cursor: Cursor = reader.query(PARTICIPANT_TABLE_NAME, columns, where, whereArgs, null, null, null)
        val userNameIdIndex = cursor.getColumnIndexOrThrow(PARTICIPANT_COL_U_NAME)
        while (cursor.moveToNext()) {
            val uName = cursor.getString(userNameIdIndex)
            participantList.add(uName)
        }
        cursor.close()
        close()
        return participantList
    }

    fun retrieveSqlAccountInfoData(sqlGroupId: String) : FirebaseAccountInfoData {
        val read = readableDatabase
        val columns = arrayOf(GROUP_COL_NAME, GROUP_COL_PARTICIPANTS_LAST_EDIT, GROUP_COL_LAST_IMAGE_EDIT, GROUP_COL_BASE_CURRENCY)
        val where = "$GROUP_COL_ID = ?"
        val whereArgs = arrayOf(sqlGroupId)
        val cursor: Cursor = read.query(GROUP_TABLE_NAME, columns, where, whereArgs, null, null, null)
        val nameIndex = cursor.getColumnIndexOrThrow(GROUP_COL_NAME)
        val participantIndex = cursor.getColumnIndexOrThrow(GROUP_COL_PARTICIPANTS_LAST_EDIT)
        val lastImageIndex = cursor.getColumnIndexOrThrow(GROUP_COL_LAST_IMAGE_EDIT)
        val baseCurrCol = cursor.getColumnIndexOrThrow(GROUP_COL_BASE_CURRENCY)
        cursor.moveToNext()
        val groupName = cursor.getString(nameIndex)
        val participants = cursor.getString(participantIndex)
        val lastEdit = cursor.getString(lastImageIndex)
        val baseCurr = cursor.getString(baseCurrCol)
        cursor.close()
        return FirebaseAccountInfoData(groupName, participants, lastEdit, baseCurr)
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
        receiptList.clear()
        val reader = readableDatabase
        val columns = arrayOf(
            EXPENSE_COL_DATE, EXPENSE_COL_TITLE, EXPENSE_COL_TOTAL,
            EXPENSE_COL_PAID_BY, EXPENSE_COL_ID, EXPENSE_COL_SCANNED, EXPENSE_COL_UI_SYMBOL,EXPENSE_COL_CURRENCY
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
        val currencyUiSymbolColIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_UI_SYMBOL)
        val currencyCodeColIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_CURRENCY)
        while (cursor.moveToNext()) {
            val receiptDate = cursor.getString(dateColIndex)
            val receiptTitle = cursor.getString(titleColIndex)
            val receiptTotal = cursor.getFloat(totalColIndex)
            val receiptPaidBy = cursor.getString(paidByColIndex)
            val receiptSqlId = cursor.getInt(idColIndex).toString()
            val receiptScannedInt = cursor.getInt(scannedColIndex)
            val scanned = receiptScannedInt == 1
            val currencyUiSymbol = cursor.getString(currencyUiSymbolColIndex)
            val currencyCode = cursor.getString(currencyCodeColIndex)
            receiptList.add(
                ReceiptData(
                    receiptDate,
                    receiptTitle,
                    receiptTotal,
                    receiptPaidBy,
                    receiptSqlId,
                    scanned,
                    currencyUiSymbol,
                    currencyCode
                )
            )
        }
        cursor.close()
    }

    fun readAllGroups() : ArrayList<GroupData>{
        val groupList: ArrayList<GroupData> = ArrayList()
        val reader = readableDatabase
        val columns = arrayOf(GROUP_COL_ID, GROUP_COL_NAME, GROUP_COL_FIREBASE_ID, GROUP_COL_USER, GROUP_COL_BASE_CURRENCY, GROUP_COL_BASE_CURRENCY_UI_SYMBOL)
        val cursor: Cursor = reader.query(GROUP_TABLE_NAME, columns, null, null, null, null, null)
        val groupNameColIndex = cursor.getColumnIndexOrThrow(GROUP_COL_NAME)
        val groupSqlIdIndex = cursor.getColumnIndexOrThrow(GROUP_COL_ID)
        val groupFirebaseIdIndex = cursor.getColumnIndexOrThrow(GROUP_COL_FIREBASE_ID)
        val groupSqlUserIndex = cursor.getColumnIndexOrThrow(GROUP_COL_USER)
        val groupCurrencyIndex = cursor.getColumnIndexOrThrow(GROUP_COL_BASE_CURRENCY)
        val groupCurrencySymbolIndex = cursor.getColumnIndexOrThrow(GROUP_COL_BASE_CURRENCY_UI_SYMBOL)
        while (cursor.moveToNext()) {
            val groupName = cursor.getString(groupNameColIndex)
            val groupSqlID = cursor.getString(groupSqlIdIndex)
            val groupFirebaseID = cursor.getString(groupFirebaseIdIndex)
            val groupSqlUser = cursor.getString(groupSqlUserIndex)
            val groupBaseCurrency = cursor.getString(groupCurrencyIndex)
            val groupBaseSymbol = cursor.getString(groupCurrencySymbolIndex)
            groupList.add(GroupData(groupName, groupSqlID, groupFirebaseID, groupSqlUser, groupBaseCurrency, groupBaseSymbol))
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
        // Retrieves the date, firebaseExpenseID, currency information and the contributions of the provided expense.
        val reader = readableDatabase
        val columns = arrayOf(EXPENSE_COL_DATE, EXPENSE_COL_CONTRIBUTIONS, EXPENSE_COL_FIREBASE_ID,
            EXPENSE_COL_CURRENCY, EXPENSE_COL_EXCHANGE_RATE)
        val whereClause = "$EXPENSE_COL_ID = ?"
        val whereArgs = arrayOf(sqlRowId)
        val cursor: Cursor = reader.query(EXPENSE_TABLE_NAME, columns, whereClause, whereArgs, null, null, null)
        val dateIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_DATE)
        val contributionIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_CONTRIBUTIONS)
        val fireBaseIdIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_FIREBASE_ID)
        val currencyColIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_CURRENCY)
        val exchangeRateColIndex = cursor.getColumnIndexOrThrow(EXPENSE_COL_EXCHANGE_RATE)
        cursor.moveToNext()
        ExpenseViewActivity.expenseDate = cursor.getString(dateIndex)
        ExpenseViewActivity.contributionString = cursor.getString(contributionIndex)
        ExpenseViewActivity.firebaseExpenseID = cursor.getString(fireBaseIdIndex)
        ExpenseViewActivity.expenseCurrency = cursor.getString(currencyColIndex)
        ExpenseViewActivity.expenseExchangeRate = cursor.getFloat(exchangeRateColIndex)
        cursor.close()
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
        // Goes through the expense table and returns some basic information to see if we should download or update expenses in firebase DB.
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


    fun setGroupParticipants(newParticipants: ArrayList<ParticipantBalanceData>, groupSqlId: String) {
        val write = writableDatabase
        for (participant in newParticipants) {
            val values = ContentValues().apply {
                put(PARTICIPANT_COL_F_BASE_KEY, participant.userKey)
                put(PARTICIPANT_COL_U_NAME, participant.userName)
                put(PARTICIPANT_COL_U_BALANCE, participant.userBalance)
                put(PARTICIPANTS_COL_FK_GROUP_ID, groupSqlId)
            }
            write.insert(PARTICIPANT_TABLE_NAME, null, values)
        }
        close()
    }

    fun setGroupParticipants(newParticipant: ParticipantBalanceData, groupSqlId: String, timestamp: String) {
        val write = writableDatabase
        val values = ContentValues().apply {
            put(PARTICIPANT_COL_F_BASE_KEY, newParticipant.userKey)
            put(PARTICIPANT_COL_U_NAME, newParticipant.userName)
            put(PARTICIPANT_COL_U_BALANCE, newParticipant.userBalance)
            put(PARTICIPANTS_COL_FK_GROUP_ID, groupSqlId)
        }
        write.insert(GROUP_TABLE_NAME, null, values)
        updateParticipantLastEdit(timestamp, write, groupSqlId)
        close()
    }

    fun updateParticipantsName(participant: ParticipantBalanceData, newName: String,
                               participantLastEdit: String, groupSqlId: String) {
        val write = writableDatabase
        val values = ContentValues().apply {
            put(PARTICIPANT_COL_U_NAME, newName)
        }
        val where = "$PARTICIPANT_COL_ID = ?"
        val whereArgs = arrayOf(participant.userSqlRow)
        write.update(PARTICIPANT_TABLE_NAME, values, where, whereArgs)
        updateParticipantLastEdit(participantLastEdit, write, groupSqlId)
    }

    private fun updateParticipantLastEdit(timestamp: String, writeableDB: SQLiteDatabase, groupSqlId: String) {
        val values = ContentValues().apply {
            put(GROUP_COL_PARTICIPANTS_LAST_EDIT, timestamp)
        }
        val where = "$GROUP_COL_ID = ?"
        val whereArgs = arrayOf(groupSqlId)
        writeableDB.update(GROUP_TABLE_NAME, values, where, whereArgs)
    }

    fun loadPreviousBalanceToObjects(groupSqlRowID: String) : ArrayList<ParticipantBalanceData> {
        val participantList: ArrayList<ParticipantBalanceData> = ArrayList()
        val reader = readableDatabase
        val columns = arrayOf(PARTICIPANT_COL_F_BASE_KEY, PARTICIPANT_COL_ID,
            PARTICIPANT_COL_U_NAME, PARTICIPANT_COL_U_BALANCE)
        val where = "$PARTICIPANTS_COL_FK_GROUP_ID = ?"
        val whereArgs = arrayOf(groupSqlRowID)
        val cursor: Cursor = reader.query(PARTICIPANT_TABLE_NAME, columns, where, whereArgs, null, null, null)
        val sqlRowIDIndex = cursor.getColumnIndexOrThrow(PARTICIPANT_COL_ID)
        val fBaseKeyIndex = cursor.getColumnIndexOrThrow(PARTICIPANT_COL_F_BASE_KEY)
        val userNameIndex = cursor.getColumnIndexOrThrow(PARTICIPANT_COL_U_NAME)
        val userBalanceIndex = cursor.getColumnIndexOrThrow(PARTICIPANT_COL_U_BALANCE)
        while (cursor.moveToNext()) {
            val sqlRow = cursor.getString(sqlRowIDIndex)
            val fBaseKey = cursor.getString(fBaseKeyIndex)
            val userName = cursor.getString(userNameIndex)
            val userBalance = cursor.getFloat(userBalanceIndex)
            participantList.add(ParticipantBalanceData(userName, userBalance, fBaseKey, sqlRow))
        }
        cursor.close()
        close()
        return participantList
    }

    fun updateSqlSettlementString(settlementStr: String, sqlGroupId: String) : String{
        val writer = writableDatabase
        val values = ContentValues().apply {
            put(GROUP_COL_SETTLEMENTS, settlementStr)
        }
        val where = "${GROUP_COL_ID} = ?"
        val whereargs = arrayOf(sqlGroupId)
        writer.update(GROUP_TABLE_NAME, values, where, whereargs)
        return settlementStr
    }

    fun updateSqlBalances(newBalancesObjects: ArrayList<ParticipantBalanceData>) {
        val writer = writableDatabase
        for (participant in newBalancesObjects) {
            val values = ContentValues().apply {
                put(PARTICIPANT_COL_U_BALANCE, participant.userBalance)
            }
            val where = "$PARTICIPANT_COL_ID = ?"
            val whereArgs = arrayOf(participant.userSqlRow)
            writer.update(PARTICIPANT_TABLE_NAME, values, where, whereArgs)
        }
    }

    fun updateGroupInfo(firebaseGroupData: FirebaseAccountInfoData, sqlGroupId: String) {
        val write = writableDatabase
        val values = ContentValues().apply {
            put(GROUP_COL_NAME, firebaseGroupData.accName)
            put(GROUP_COL_PARTICIPANTS_LAST_EDIT, firebaseGroupData.accParticipantLastEdit)
            put(GROUP_COL_LAST_IMAGE_EDIT, firebaseGroupData.accLastImage)
        }
        val where = "$GROUP_COL_ID = ?"
        val whereArgs = arrayOf(sqlGroupId)
        write.update(GROUP_TABLE_NAME, values, where, whereArgs)
    }

    fun checkIfCurrencyExists(currencyCode: String) : Long {
        // Checks if the base currency exists in the SQL db and if it does when it was last updated.
        val read = readableDatabase
        val columns = arrayOf(CURRENCY_COL_BASE, CURRENCY_COL_LAST_UPDATE)
        val where = "$CURRENCY_COL_BASE = ?"
        val whereArgs = arrayOf(currencyCode)
        val cursor: Cursor = read.query(CURRENCY_TABLE_NAME, columns, where, whereArgs, null, null, null)
        val exists = cursor.moveToNext()
        if (exists) {
            val lastUpdateIndex = cursor.getColumnIndexOrThrow(CURRENCY_COL_LAST_UPDATE)
            val lastUpdate = cursor.getLong(lastUpdateIndex)
            cursor.close()
            return lastUpdate
        }
        else {
            return CURRENCY_NON_EXISTENT
        }
    }

    fun retrieveExchangeRate(baseCurrency: String, expenseCurrency: String) : Float {
        val read = readableDatabase
        val columns = arrayOf(
            CURRENCY_COL_RATE
        )
        val where = "$CURRENCY_COL_BASE = ? AND $CURRENCY_COL_CODE = ?"
        val whereArgs = arrayOf(baseCurrency, expenseCurrency)
        val cursor: Cursor = read.query(CURRENCY_TABLE_NAME, columns, where, whereArgs, null, null, null)
        val rateColIndex = cursor.getColumnIndexOrThrow(CURRENCY_COL_RATE)
        val rate: Float
        if (cursor.moveToNext()) {
            rate = cursor.getFloat(rateColIndex)
            cursor.close()
            close()
            return rate
        }
        else {
            cursor.close()
            close()
            rate = 1.0F
            return rate
        }
    }

}
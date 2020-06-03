package com.splitreceipt.myapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_DATE
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_FK_ACCOUNT_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_PAID_BY
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_TITLE
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_TOTAL
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_TABLE_NAME
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_COL_BALANCES
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_COL_ID
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_TABLE_NAME
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_ID
import com.splitreceipt.myapplication.data.ParticpantBalanceData
import com.splitreceipt.myapplication.data.ReceiptData
import com.splitreceipt.myapplication.databinding.ActivityMainBinding
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ReceiptOverviewActivity : AppCompatActivity() {
    /*
    Activity shows the interior of a user account. Listing all prior expenses and
    offering the user to create a new expense.
     */

    lateinit var binding: ActivityMainBinding
    lateinit var receiptList: ArrayList<ReceiptData>
    private lateinit var adapter: ReceiptOverViewAdapter
    private val ADD_EXPENSE_RESULT = 20

    companion object {
        var getSqlAccountId: String? = "-1"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        receiptList = ArrayList()

        // TODO: Ensure these are static like variables to avoid errors
        getSqlAccountId = intent.getStringExtra(AccountScreenActivity.sqlIntentString)
        val getFirebaseId = intent.getStringExtra("FirebaseID")
        Toast.makeText(this, getSqlAccountId, Toast.LENGTH_SHORT).show()

        loadPreviousReceipts(getSqlAccountId)
        val todaysDate = getTodaysDate()
        adapter = ReceiptOverViewAdapter(receiptList, todaysDate)
        binding.mainActivityRecycler.layoutManager = LinearLayoutManager(this)
        binding.mainActivityRecycler.adapter = adapter
    }

    @SuppressLint("SimpleDateFormat")
    fun getTodaysDate(): Date? {
        val currentTime = Calendar.getInstance().time
        val date: Date?
        val dateFormat = SimpleDateFormat(getString(R.string.date_format_dd_MM_yyyy))
        val todaysDate = dateFormat.format(currentTime)
        date = dateFormat.parse(todaysDate)
        return date
    }

    private fun loadPreviousReceipts(sqlId: String?) {
        val dbHelper = DbHelper(this)
        val reader = dbHelper.readableDatabase
        val columns = arrayOf(RECEIPT_COL_DATE, RECEIPT_COL_TITLE, RECEIPT_COL_TOTAL, RECEIPT_COL_PAID_BY)
        val selectClause = "$RECEIPT_COL_FK_ACCOUNT_ID = ?"
        val selectArgs = arrayOf("$sqlId")
        val cursor: Cursor = reader.query(RECEIPT_TABLE_NAME, columns, selectClause, selectArgs,
                            null, null, RECEIPT_COL_ID + " DESC") //TODO: Try to sort all expenses in date order. Maybe do this before passing to the adapter?
        val dateColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_DATE)
        val titleColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_TITLE)
        val totalColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_TOTAL)
        val paidByColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_PAID_BY)
        while (cursor.moveToNext()) {
            val receiptDate = cursor.getString(dateColIndex)
            val receiptTitle = cursor.getString(titleColIndex)
            val receiptTotal = cursor.getFloat(totalColIndex)
            val receiptPaidBy = cursor.getString(paidByColIndex)
            receiptList.add(ReceiptData(receiptDate, receiptTitle, receiptTotal, receiptPaidBy))
        }
        cursor.close()
    }

    fun addNewReceiptButton(view: View) {
        val intent = Intent(this, NewReceiptCreationActivity::class.java)
        intent.putExtra("sqlID", getSqlAccountId)
        startActivityForResult(intent, ADD_EXPENSE_RESULT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_EXPENSE_RESULT){
            if (resultCode == Activity.RESULT_OK){
                //TODO: Add functionality
                val contributions = data?.getStringExtra(NewReceiptCreationActivity.
                                                            CONTRIBUTION_INTENT_DATA)
                recalculateBalances(contributions!!)
                receiptList.clear()
                loadPreviousReceipts(getSqlAccountId)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun recalculateBalances(newContributions: String) {
        /*
        Step 1: Convert prior balance string in SQL to data class objects.
        Step 2: Use the contribution string from the returned intent to update the objects balance values.
        Step 3: Convert those objects back to a balance string. Step 4: Update SQL.
         */
        val prevBalanceObjects = loadPreviousBalanceToObjects()
        val newBalanceObjects = updateBalancesWithContributions(prevBalanceObjects, newContributions)
        val newBalanceString = parseObjectsToString(newBalanceObjects)
        updateSqlWithBalances(newBalanceString)
        //TODO: Update the balances adapter

    }

    private fun updateSqlWithBalances(balString: String) {
        val dbHelper = DbHelper(this)
        val writer = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ACCOUNT_COL_BALANCES, balString)
        }
        val where = "$ACCOUNT_COL_ID = ?"
        val whereargs = arrayOf("$getSqlAccountId")
        val id = writer.update(ACCOUNT_TABLE_NAME, values, where, whereargs)
        if (id != -1) {
            Log.i("TEST", "Successful upload of new balance string")
        }
        dbHelper.close()
    }

    private fun parseObjectsToString(balancedObjects: ArrayList<ParticpantBalanceData>): String {
        // StringBuilder used to create deconstruct the data class into one solid balance string.
        val stringBuilder = StringBuilder()
        for (participant in balancedObjects) {
            val name = participant.name
            val nameString = "$name,"
            stringBuilder.append(nameString)
            val balance = participant.balance
            val balString = "$balance/"
            stringBuilder.append(balString)
        }
        stringBuilder.deleteCharAt(stringBuilder.lastIndex)
        return stringBuilder.toString()
    }

    private fun updateBalancesWithContributions(prevBalances: ArrayList<ParticpantBalanceData>, newContributions: String): ArrayList<ParticpantBalanceData> {
        // Deconstructs each contribution from a solid string and updates the values of the relevant data class objects depending on if they have lent or borrowed.
        val splitContributions = newContributions.split("/") // ["Dan,£1.00,Marie", "Marie,£1.00,Marie"]
        for (contribution in splitContributions) {
            val contribDetails = contribution.split(",") // ["Dan", "1.00", "Marie"]
            val contributor = contribDetails[0] // ["Dan"]
            val contributee = contribDetails[2] // ["Marie"]
            if (contributor != contributee) {
                val contribValue = contribDetails[1].toFloat()  // 1.00
                for (participantBalanceItem in prevBalances) {
                    if (contributor == participantBalanceItem.name) {
                        participantBalanceItem.balance -= contribValue
                    }
                    else if (contributee == participantBalanceItem.name) {
                        participantBalanceItem.balance += contribValue
                    }
                }
            }
        }
        return prevBalances
    }

    private fun loadPreviousBalanceToObjects(): ArrayList<ParticpantBalanceData> {
        // Loads the previous balance string from SQL and constructs each participant into an individual data class object.
        var previousBalString = ""
//        var previousBalString = "Dan,3.00/Marie,-3.00"
        val dbHelper = DbHelper(this)
        val reader = dbHelper.readableDatabase
        val columns = arrayOf(ACCOUNT_COL_BALANCES)
        val selectClause = "$ACCOUNT_COL_ID = ?"
        val selectArgs = arrayOf("$getSqlAccountId")
        val cursor: Cursor = reader.query(ACCOUNT_TABLE_NAME, columns, selectClause, selectArgs, null, null, null)
        val balColInd = cursor.getColumnIndexOrThrow(ACCOUNT_COL_BALANCES)
        while (cursor.moveToNext()) {
            previousBalString = cursor.getString(balColInd)
        }
        cursor.close()
        dbHelper.close()

        val particBalDataList : ArrayList<ParticpantBalanceData> = ArrayList()
        val participants = previousBalString.split("/") //  ["Dan,3.00", "Marie,-3.00"]
        for (participant in participants){
            val nameValueSplit = participant.split(",")// ["Dan", "3.00"]
            val name = nameValueSplit[0] // ["Dan"]
            val balance = nameValueSplit[1] // ["£3.00"]
            particBalDataList.add(ParticpantBalanceData(name, balance.toFloat()))
        }
        return particBalDataList
    }

}

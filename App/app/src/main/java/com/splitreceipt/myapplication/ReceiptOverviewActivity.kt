package com.splitreceipt.myapplication

import android.app.Activity
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
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_COL_ID
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_COL_SETTLEMENTS
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_TABLE_NAME
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_ID
import com.splitreceipt.myapplication.data.ReceiptData
import com.splitreceipt.myapplication.databinding.ActivityMainBinding
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.collections.ArrayList
import kotlin.text.StringBuilder

class ReceiptOverviewActivity : AppCompatActivity(), ReceiptOverViewAdapter.onReceRowClick {
    /*
    Activity shows the interior of a user account. Listing all prior expenses and
    offering the user to create a new expense.
     */

    lateinit var binding: ActivityMainBinding
    lateinit var receiptList: ArrayList<ReceiptData>
    private lateinit var adapter: ReceiptOverViewAdapter
    private val ADD_EXPENSE_RESULT = 20
    private val SEE_EXPENSE_RESULT = 10
    private var userSettlementString = ""

    companion object {
        var getSqlUser: String? = "unknown"
        var getSqlAccountId: String? = "-1"
        var settlementString: String  = ""
        var settlementArray: ArrayList<String> = ArrayList()
        const val balanced_string: String = "balanced"

        fun changeNameToYou(participantName: String, capitalize: Boolean): String {
            return if (participantName == getSqlUser) {
                if (capitalize){
                    "You"
                } else{
                    "you"
                } } else {
                if (capitalize){
                    participantName.capitalize()
                } else {
                    participantName
                } } }

        fun roundToTwoDecimalPlace(number: Float): Float {
            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.FLOOR
            return df.format(number).toFloat()
        }

        fun errorRate(balance: Float) : Float {
            if (balance in -0.04..0.04){
                // £0.04/$0.04 error rate allowed. Likely only to ever reach 0.02 error rate.
                return 0.0F
            }
            return balance
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        receiptList = ArrayList()

        // TODO: Ensure these are static like variables to avoid errors
        getSqlAccountId = intent.getStringExtra(AccountScreenActivity.sqlIntentString)
        getSqlUser = intent.getStringExtra(AccountScreenActivity.userIntentString)
        val getAccountName = intent.getStringExtra(AccountScreenActivity.accountNameIntentString)
        binding.accountNameTitleText.text = getAccountName
        val getFirebaseId = intent.getStringExtra("FirebaseID")
        Toast.makeText(this, getSqlAccountId, Toast.LENGTH_SHORT).show()

        loadPreviousReceipts(getSqlAccountId)
        settlementString = loadSqlSettlementString(getSqlAccountId)
        deconstructAndSetSettlementString(settlementString)

        adapter = ReceiptOverViewAdapter(receiptList, this)
        binding.mainActivityRecycler.layoutManager = LinearLayoutManager(this)
        binding.mainActivityRecycler.adapter = adapter
    }

    private fun loadSqlSettlementString(sqlAccountId: String?): String {
        var settlementString: String = ""
        val dbHelper = DbHelper(this)
        val reader = dbHelper.readableDatabase
        val columns = arrayOf(ACCOUNT_COL_SETTLEMENTS)
        val selectClause = "$ACCOUNT_COL_ID = ?"
        val selectArgs = arrayOf(sqlAccountId)
        val cursor: Cursor = reader.query(ACCOUNT_TABLE_NAME, columns, selectClause, selectArgs,
                        null, null, null)
        val settlemnetIndex = cursor.getColumnIndexOrThrow(ACCOUNT_COL_SETTLEMENTS)
        while (cursor.moveToNext()) {
            settlementString = cursor.getString(settlemnetIndex)
        }
        cursor.close()
        dbHelper.close()
        return settlementString
    }

    private fun loadPreviousReceipts(sqlId: String?) {
        val dbHelper = DbHelper(this)
        val reader = dbHelper.readableDatabase
        val columns = arrayOf(RECEIPT_COL_DATE, RECEIPT_COL_TITLE, RECEIPT_COL_TOTAL, RECEIPT_COL_PAID_BY, RECEIPT_COL_ID)
        val selectClause = "$RECEIPT_COL_FK_ACCOUNT_ID = ?"
        val selectArgs = arrayOf("$sqlId")
        val cursor: Cursor = reader.query(RECEIPT_TABLE_NAME, columns, selectClause, selectArgs,
                            null, null, "$RECEIPT_COL_ID DESC"
        ) //TODO: Try to sort all expenses in date order. Maybe do this before passing to the adapter?
        val dateColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_DATE)
        val titleColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_TITLE)
        val totalColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_TOTAL)
        val paidByColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_PAID_BY)
        val idColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_ID)
        while (cursor.moveToNext()) {
            val receiptDate = cursor.getString(dateColIndex)
            val receiptTitle = cursor.getString(titleColIndex)
            val receiptTotal = cursor.getFloat(totalColIndex)
            val receiptPaidBy = cursor.getString(paidByColIndex)
            val receiptSqlId =  cursor.getInt(idColIndex).toString()
            receiptList.add(ReceiptData(receiptDate, receiptTitle, receiptTotal, receiptPaidBy, receiptSqlId))
        }
        cursor.close()
        dbHelper.close()
    }

    fun addNewReceiptButton(view: View) {
        val intent = Intent(this, NewReceiptCreationActivity::class.java)
        intent.putExtra(NewReceiptCreationActivity.intentSqlIdString, getSqlAccountId)
        startActivityForResult(intent, ADD_EXPENSE_RESULT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val newSettlementString: String

        if (requestCode == ADD_EXPENSE_RESULT){
            if (resultCode == Activity.RESULT_OK){
                val contributions = data?.getStringExtra(NewReceiptCreationActivity.
                                                            CONTRIBUTION_INTENT_DATA)
                Log.i("Algorithm", "Contribution string returned from the recent transaction: $contributions \n\n")
                newSettlementString = newContributionUpdates(contributions!!)
                deconstructAndSetSettlementString(newSettlementString)
            }
        }
        else if (requestCode == SEE_EXPENSE_RESULT){
            if (resultCode == Activity.RESULT_OK) {

                if (data?.getStringExtra(ExpenseViewActivity
                        .expenseReturnNewSettlements) == null){
                    // This scenario means the user deleted the expense and re-balancing must be done
                    val reversedContributions = data?.getStringExtra(ExpenseViewActivity
                        .expenseReturnNewContributions)
                    Log.i("Algorithm", "Reversed contribution string returned from the recent deletion: $reversedContributions \n\n")
                    newSettlementString = newContributionUpdates(reversedContributions!!)
                }
                else {
                    // This scenario means the user edited the expense and re-balancing is already completed
                    newSettlementString = data?.getStringExtra(ExpenseViewActivity
                                                    .expenseReturnNewSettlements).toString()
                    reloadRecycler()
                }
                deconstructAndSetSettlementString(newSettlementString)

            }
        }
    }

    private fun deconstructAndSetSettlementString(settlementString: String){
        // This function will deconstruct a settlementString and produce an ArrayList of individual settlement strings
        settlementArray.clear()
        val sb: StringBuilder = java.lang.StringBuilder()
        val userDirectedSettlementIndexes: ArrayList<Int> =  ArrayList()
        var indexCount = 0
        if (settlementString == balanced_string) {
            settlementArray.add(getString(R.string.balanced))
            userDirectedSettlementIndexes.add(indexCount)
        }
        else {
            val splitIndividual = settlementString.split("/")
            for (settlement in splitIndividual) {
                val splitSettlement = settlement.split(",")
                val debtor = changeNameToYou(splitSettlement[0], true)
                val value = splitSettlement[1]
                val receiver = changeNameToYou(splitSettlement[2], false)
                val finalSettlementString = "$debtor owes $value to $receiver."
                settlementArray.add(finalSettlementString)
                if ("you" in finalSettlementString.toLowerCase()) {
                    userDirectedSettlementIndexes.add(indexCount)
                }
                indexCount ++
            }
        }
        for (index in userDirectedSettlementIndexes) {
            sb.append(settlementArray[index]).append("\n")
        }
        sb.deleteCharAt(sb.lastIndex)
        val newString = sb.toString()
        binding.receiptOverBalanceString.text = newString
        userSettlementString = newString
    }

    fun newContributionUpdates(newContributions: String) : String {
        val balanceSettlementHelper = BalanceSettlementHelper(this, getSqlAccountId.toString())
        val settlementString = balanceSettlementHelper.recalculateBalancesAndSettlements(newContributions)
        reloadRecycler()
        return settlementString
    }

    fun reloadRecycler(){
        // Clears the list and refreshes receipts from SQL db back into it.
        receiptList.clear()
        loadPreviousReceipts(getSqlAccountId)
        adapter.notifyDataSetChanged()
    }

    fun balancesButtonPressed(view: View) {
        val intent = Intent(this, BalanceOverviewActivity::class.java)
        startActivity(intent)
    }

    override fun onRowClick(pos: Int, title:String, total:String, sqlId: String, paidBy:String) {
        val intent = Intent(this, ExpenseViewActivity::class.java)
        intent.putExtra(ExpenseViewActivity.expenseTitleIntentString, title)
        intent.putExtra(ExpenseViewActivity.expenseTotalIntentString, total)
        intent.putExtra(ExpenseViewActivity.expenseSqlIntentString, sqlId)
        intent.putExtra(ExpenseViewActivity.expensePaidByIntentString, paidBy)
        startActivityForResult(intent, SEE_EXPENSE_RESULT)
    }

}

package com.splitreceipt.myapplication

import android.database.Cursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_CONTRIBUTIONS
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_DATE
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_TABLE_NAME
import com.splitreceipt.myapplication.data.ExpenseAdapterData
import com.splitreceipt.myapplication.databinding.ActivityExpenseViewBinding

class ExpenseViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseViewBinding
    private lateinit var expenseDate: String
    private lateinit var contributionString: String
    private lateinit var contributionList: ArrayList<ExpenseAdapterData>

    companion object {
        const val expenseSqlIntentString: String = "expense_sql_id"
        const val expenseTitleIntentString: String = "expense_title"
        const val expenseTotalIntentString: String = "expense_total"
        const val expensePaidByIntentString: String = "expense_paid_by"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        contributionList = ArrayList()

        val getTitleIntent = intent.getStringExtra(expenseTitleIntentString)
        val getTotalIntent = intent.getStringExtra(expenseTotalIntentString)
        val getPaidByIntent = intent.getStringExtra(expensePaidByIntentString).toLowerCase()
        val getSqlId = intent.getStringExtra(expenseSqlIntentString)
        getSqlDetails(getSqlId)
        deconstructContributionString()

        setSupportActionBar(findViewById(R.id.toolbar))
        val actionBar = supportActionBar
        actionBar?.title = getTitleIntent
        val paidByText = "Paid by $getPaidByIntent"
        val valueText = "£$getTotalIntent"
        binding.expenseValue.text = valueText
        binding.paidByText.text = paidByText

        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_back_arrow_white)
        }

        //TODO: Set the receipt title as the action bar title

        val adapter = ExpenseViewAdapter(contributionList)
        binding.expenseRecy.layoutManager = LinearLayoutManager(this)
        binding.expenseRecy.adapter = adapter
    }

    private fun deconstructContributionString() {
        val contributionsSplit = contributionString.split("/")
        for (contribution in contributionsSplit) {
            val individualContrib = contribution.split(",")
            val contributor = ReceiptOverviewActivity.changeNameToYou(individualContrib[0], true)
            val value = individualContrib[1]
            val newString = "$contributor contributed £$value" //TODO: Ensure the correct currency
            contributionList.add(ExpenseAdapterData(newString, value))
        }
    }

    private fun getSqlDetails(sqlId: String?) {
        // Retrieves the date and contributions of the selected expense
        var date = ""
        var contributions = ""
        val dbHelper = DbHelper(this)
        val reader = dbHelper.readableDatabase
        val columns = arrayOf(RECEIPT_COL_DATE, RECEIPT_COL_CONTRIBUTIONS)
        val whereClause = "$RECEIPT_COL_ID = ?"
        val whereArgs = arrayOf(sqlId)
        val cursor: Cursor = reader.query(RECEIPT_TABLE_NAME, columns, whereClause, whereArgs, null, null, null)
        val dateIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_DATE)
        val contributionIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_CONTRIBUTIONS)
        while(cursor.moveToNext()) {
            date = cursor.getString(dateIndex)
            contributions = cursor.getString(contributionIndex)
        }
        cursor.close()
        dbHelper.close()
        expenseDate = date
        contributionString = contributions
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}

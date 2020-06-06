package com.splitreceipt.myapplication

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    private var sqlRowId: String = "-1"
    private var getTitleIntent: String = ""
    private var getTotalIntent: String = ""
    private var getPaidByIntent: String = ""
    private lateinit var adapter: ExpenseViewAdapter

    companion object {
        const val expenseSqlIntentString: String = "expense_sql_id"
        const val expenseTitleIntentString: String = "expense_title"
        const val expenseTotalIntentString: String = "expense_total"
        const val expensePaidByIntentString: String = "expense_paid_by"
        const val expenseReturnNewContributions: String = "expense_return_contributions"
        const val expenseReturnEditSql: String = "expense_edit_sql"
        const val expenseReturnEditDate: String = "expense_edit_date"
        const val expenseReturnEditTitle: String = "expense_edit_title"
        const val expenseReturnEditTotal: String = "expense_edit_total"
        const val expenseReturnEditPaidBy: String = "expense_edit_paid_by"
        const val expenseReturnEditContributions: String = "expense_edit_contributions"
        const val EDIT_EXPENSE_INTENT_CODE = 30
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        contributionList = ArrayList()

        getTitleIntent = intent.getStringExtra(expenseTitleIntentString)
        getTotalIntent = intent.getStringExtra(expenseTotalIntentString)
        getPaidByIntent = intent.getStringExtra(expensePaidByIntentString).toLowerCase()
        sqlRowId = intent.getStringExtra(expenseSqlIntentString)
        getSqlDetails()
        deconstructContributionString()

        val paidByText = "Paid by $getPaidByIntent"
        val valueText = "£$getTotalIntent"
        binding.expenseValue.text = valueText
        binding.paidByText.text = paidByText

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getTitleIntent
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_back_arrow_white)
        }

        adapter = ExpenseViewAdapter(contributionList)
        binding.expenseRecy.layoutManager = LinearLayoutManager(this)
        binding.expenseRecy.adapter = adapter
    }

    private fun deconstructContributionString() {
        contributionList.clear()
        val contributionsSplit = contributionString.split("/")
        for (contribution in contributionsSplit) {
            val individualContrib = contribution.split(",")
            val contributor = ReceiptOverviewActivity.changeNameToYou(individualContrib[0], true)
            val value = individualContrib[1]
            val newString = "$contributor contributed £$value" //TODO: Ensure the correct currency
            contributionList.add(ExpenseAdapterData(newString, value))
        }
    }

    private fun getSqlDetails() {
        // Retrieves the date and contributions of the selected expense.
        var date = ""
        var contributions = ""
        val dbHelper = DbHelper(this)
        val reader = dbHelper.readableDatabase
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
        dbHelper.close()
        expenseDate = date
        contributionString = contributions
    }

    fun editExpense(view: View) {
        val intent = Intent(this, NewReceiptCreationActivity::class.java)
        intent.putExtra(NewReceiptCreationActivity.editIntentTitleString, getTitleIntent)
        intent.putExtra(NewReceiptCreationActivity.editIntentTotalString, getTotalIntent)
        intent.putExtra(NewReceiptCreationActivity.editIntentPaidByString, getPaidByIntent)
        intent.putExtra(NewReceiptCreationActivity.editIntentDateString, expenseDate)
        intent.putExtra(NewReceiptCreationActivity.editIntentContributionsString, contributionString)
        intent.putExtra(NewReceiptCreationActivity.editIntentSqlRowIdString, sqlRowId)
        startActivityForResult(intent, EDIT_EXPENSE_INTENT_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_EXPENSE_INTENT_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val row = data?.getStringExtra(expenseReturnEditSql)
                if (row == sqlRowId) {
                    Toast.makeText(this, "Expense edited", Toast.LENGTH_SHORT).show()
                    supportActionBar?.title = intent.getStringExtra(expenseReturnEditTitle)
                    binding.expenseValue.text = data.getStringExtra(expenseReturnEditTotal)
                    binding.paidByText.text = data.getStringExtra(expenseReturnEditPaidBy)
                    binding.dateText.text = data.getStringExtra(expenseReturnEditDate)
                    contributionString = data.getStringExtra(expenseReturnEditContributions).toString()
                    deconstructContributionString()
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    fun deleteExpense(view: View) {
        AlertDialog.Builder(this).apply {
            setIcon(R.drawable.vector_warning_yellow)
            setTitle("Are you sure?")
            setMessage("This expense will be deleted forever.")
            setPositiveButton("Yes delete", object: DialogInterface.OnClickListener{
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    val dbHelper = DbHelper(context)
                    val write = dbHelper.writableDatabase
                    val prevContrib: String = locatePriorContributions(write)

                    val whereClause = "$RECEIPT_COL_ID = ?"
                    val whereArgs = arrayOf(sqlRowId)
                    write.delete(RECEIPT_TABLE_NAME, whereClause, whereArgs)
                    dbHelper.close()

                    Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                    reversePriorExpense(prevContrib)
                    onBackPressed()
                }
            })
            setNegativeButton("No, cancel", object: DialogInterface.OnClickListener{
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    dialog?.cancel()
                }
            })
        }.show()
    }

    private fun reversePriorExpense(prevContribString: String) {
        // Reverse the expense contribution and add to the intent ready for
        val stringBuilder = StringBuilder()
        val spliPriorContrib = prevContribString.split("/")
        for (contribution in spliPriorContrib) {
            val individualContrib = contribution.split(",")
            val priorContributor = individualContrib[0]
            val priorContributee = individualContrib[2]
            val contribValue = individualContrib[1]
            stringBuilder.append("$priorContributee,")
            stringBuilder.append("$contribValue,")
            stringBuilder.append("$priorContributor/")
        }
        stringBuilder.deleteCharAt(stringBuilder.lastIndex)
        val newContribs = stringBuilder.toString()
        intent.putExtra(expenseReturnNewContributions, newContribs)
        setResult(Activity.RESULT_OK, intent)
        this.finish()
    }

    private fun locatePriorContributions(readWriteDB: SQLiteDatabase?): String {
        val columns = arrayOf(RECEIPT_COL_CONTRIBUTIONS)
        val selectClause = "$RECEIPT_COL_ID = ?"
        val selectArgs = arrayOf(sqlRowId)
        val cursor: Cursor = readWriteDB!!.query(RECEIPT_TABLE_NAME, columns, selectClause,
                                            selectArgs, null, null, null)
        val contributionsColIndex = cursor.getColumnIndex(RECEIPT_COL_CONTRIBUTIONS)
        cursor.moveToNext()
        val priorContribs =  cursor.getString(contributionsColIndex).toString()
        cursor.close()
        return priorContribs
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }


}

package com.splitreceipt.myapplication

import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.splitreceipt.myapplication.data.DbManager
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_DATE
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_FK_GROUP_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_PAID_BY
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_TITLE
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_TOTAL
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_UNIQUE_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_TABLE_NAME
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_CONTRIBUTIONS
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_ID
import com.splitreceipt.myapplication.data.ParticipantData
import com.splitreceipt.myapplication.databinding.ActivityNewReceiptCreationBinding
import java.lang.StringBuilder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList


class NewReceiptCreationActivity : AppCompatActivity() {

    /*
    Activity gives the user the ability to input a new receipt/ transaction
     */

    private lateinit var binding: ActivityNewReceiptCreationBinding
    private var editPaidBy: String = ""


    companion object {
        var sqlAccountId: String? = "-1"
        lateinit var participantList: ArrayList<String>
        lateinit var participantDataEditList: ArrayList<ParticipantData> //TODO: Currently being initialised even when it is not an edit. Fix this.
        const val CONTRIBUTION_INTENT_DATA = "contribution"
        const val intentSqlIdString = "sqlID"

        const val editIntentTitleString = "edit_title"
        const val editIntentTotalString = "edit_total"
        const val editIntentPaidByString = "edit_paid_by"
        const val editIntentDateString = "edit_date"
        const val editIntentContributionsString = "edit_contributions"
        const val editIntentSqlRowIdString = "edit_sql_id"
        var isEdit: Boolean = false
        var editTotal: String = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewReceiptCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        participantDataEditList = ArrayList()

        val editTitle = intent.getStringExtra(editIntentTitleString)
        if (editTitle != null) {
            isEdit = true
            editTotal = intent.getStringExtra(editIntentTotalString) //TODO: pass the total to the fragment
            editPaidBy = intent.getStringExtra(editIntentPaidByString) //TODO: Not currently doing anything as not sure how to change spinner
            val editDate = intent.getStringExtra(editIntentDateString)
            val editContributions = intent.getStringExtra(editIntentContributionsString) //TODO: Update the recyclerview in the fragment
            deconstructAndBuildEditContribs(editContributions)
            binding.receiptTitleEditText.setText(editTitle)
            binding.dateButton.text = editDate

        } else {
            Toast.makeText(this, "Not edit button", Toast.LENGTH_SHORT).show()
        }

        sqlAccountId = intent.getStringExtra(intentSqlIdString)

        participantList = ArrayList()
        retrieveParticipants()


        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Add expense"
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_x_white)
        }
        //TODO: Currently if a user wasn't contributing before they are ticked anyway when the user edits. Fix this.
        binding.paidBySpinner.adapter = ArrayAdapter(this,
            R.layout.support_simple_spinner_dropdown_item, participantList)

        val pagerAdapter = ReceiptPagerAdapter(supportFragmentManager)
        binding.receiptViewPager.adapter = pagerAdapter
        binding.receiptTabLayout.setupWithViewPager(binding.receiptViewPager)
    }

    private fun deconstructAndBuildEditContribs(editContributions: String) {
        //deconstruct the contrib string, convert to participant data and add to editParticipantData list
        val contributionsSplit = editContributions.split("/")
        for (contrib in contributionsSplit) {
            val individualContribution = contrib.split(",")
            val participantName = individualContribution[0]
            if (participantName == editPaidBy) {
                continue
            }
            val contributingValue = individualContribution[1]
            var contributing: Boolean
            contributing = contributingValue != "0.00"
            participantDataEditList.add(ParticipantData(participantName, contributingValue, contributing))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.add_expense_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.addExpenseSave -> {
                val okayToProceed = checkAllInputsAreValid()
                if (okayToProceed) {
                    val receiptFirebaseID = "rec0001" //TODO: Increment this number
                    val date = getDate()
                    val title =  binding.receiptTitleEditText.text.toString()
                    val total = findViewById<EditText>(R.id.currencyAmount).text.toString().toFloat()
                    val paidBy = binding.paidBySpinner.selectedItem.toString()
                    // TODO: Take all the itemized results

                    val updatedContribList = SplitReceiptManuallyFragment.fragmentManualParticipantList
                    val contributionsString = createContribString(updatedContribList, paidBy)
                    Log.i("TEST", contributionsString)

                    if (!isEdit) {
                        // Insert new entry to SQL DB.
                        val sqlRow = insertSql(receiptFirebaseID, date, title, total, paidBy, contributionsString)
                        intent.putExtra(CONTRIBUTION_INTENT_DATA, contributionsString)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                        return true
                    } else {
                        // Update previous entry in SQL DB
                        val sqlRow = updateSql(date, title, total, paidBy, contributionsString)
                        intent.putExtra(ExpenseViewActivity.expenseReturnEditSql, sqlRow)
                        intent.putExtra(ExpenseViewActivity.expenseReturnEditDate, date)
                        intent.putExtra(ExpenseViewActivity.expenseReturnEditTotal, total.toString())
                        intent.putExtra(ExpenseViewActivity.expenseReturnEditTitle, title)
                        intent.putExtra(ExpenseViewActivity.expenseReturnEditPaidBy, paidBy)
                        intent.putExtra(ExpenseViewActivity.expenseReturnEditContributions, contributionsString)
                        setResult(Activity.RESULT_OK, intent)
                        isEdit = false
                        finish()
                        return true
                    }

                } else
                {return false}
            }
            else -> return false
        }
    }



    private fun createContribString(updatedContribList: ArrayList<ParticipantData>, paidBy: String): String {
        val sb = StringBuilder()
        for (participant in updatedContribList) {
            val name = participant.name
            val nameString = "$name,"
            sb.append(nameString)
            val value = participant.contributionValue
            val valString = "$value,"
            sb.append(valString)
            val paidByString = "$paidBy/"
            sb.append(paidByString)
        }
        sb.deleteCharAt(sb.lastIndex)
        return sb.toString()
    }

    private fun checkAllInputsAreValid(): Boolean {
        val currencyAmount: EditText = findViewById(R.id.currencyAmount)
        //TODO: Animate the items which need input
        if (binding.receiptTitleEditText.text!!.isEmpty()) {
            Toast.makeText(this, "Please add a title", Toast.LENGTH_SHORT).show()
            return false
        }
        else if (currencyAmount.text.isEmpty()){
            Toast.makeText(this, "Please add an amount", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    fun datePicker(view: View){
        val cldr: Calendar = Calendar.getInstance()
        val day: Int = cldr.get(Calendar.DAY_OF_MONTH)
        val month: Int = cldr.get(Calendar.MONTH)
        val year: Int = cldr.get(Calendar.YEAR)
        // date picker dialog
        val picker = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                val dayString = cleanDay(dayOfMonth)
                val monthString = cleanMonth(monthOfYear)
                val yearString: String = year.toString()

                val string = "$dayString/$monthString/$yearString"
                binding.dateButton.setText(string)
            },
            year, month, day)
        picker.show()
    }

    private fun retrieveParticipants() {
        participantList.clear()
        val dbHelper = DbHelper(this)
        sqlAccountId = ReceiptOverviewActivity.getSqlGroupId
        val reader = dbHelper.readableDatabase
        val columns = arrayOf(DbManager.GroupTable.GROUP_COL_PARTICIPANTS)
        val selectClause = "${DbManager.GroupTable.GROUP_COL_ID} = ?"
        val selectArgs = arrayOf(sqlAccountId)
        val cursor: Cursor = reader.query(
            DbManager.GroupTable.GROUP_TABLE_NAME, columns, selectClause, selectArgs,
            null, null, null)
        val particColIndex = cursor.getColumnIndexOrThrow(DbManager.GroupTable.GROUP_COL_PARTICIPANTS)
        while (cursor.moveToNext()){
            val participantsString = cursor.getString(particColIndex)
            val splitParticipants = participantsString.split(",")
            for (participant in splitParticipants) {
                participantList.add(participant)
            }
        }
        cursor.close()
        dbHelper.close()
    }

    private fun getDate(): String{
        val dateSelection = binding.dateButton.text.toString()
        return if (dateSelection == getString(R.string.when_today)) {
            retrieveTodaysDate()
        } else {
            dateSelection
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        Toast.makeText(this, "Expense cancelled", Toast.LENGTH_SHORT).show()
        onBackPressed()
        return true
    }

    private fun retrieveTodaysDate(): String {
        val date = LocalDate.now()
        return date.format(DateTimeFormatter.ofPattern(getString(R.string.date_format_dd_MM_yyyy))).toString()
    }

    private fun insertSql(recFirebaseId: String, date: String, title: String, total: Float, paidBy: String, contributions: String) : Int{
        val dbHelper = DbHelper(this)
        val write = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(RECEIPT_COL_UNIQUE_ID, recFirebaseId)
            put(RECEIPT_COL_DATE, date)
            put(RECEIPT_COL_TITLE, title)
            put(RECEIPT_COL_TOTAL, total)
            put(RECEIPT_COL_PAID_BY, paidBy)
            put(RECEIPT_COL_CONTRIBUTIONS, contributions)
            put(RECEIPT_COL_FK_GROUP_ID, sqlAccountId)
        }
        val sqlId = write.insert(RECEIPT_TABLE_NAME, null, values)
        dbHelper.close()
        return sqlId.toInt()
    }

    private fun updateSql(date: String, title: String, total: Float, paidBy: String, contributionsString: String): String {
        val editSqlRowId = intent.getStringExtra(editIntentSqlRowIdString)
        val dbHelper = DbHelper(this)
        val writer = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(RECEIPT_COL_DATE, date)
            put(RECEIPT_COL_TITLE, title)
            put(RECEIPT_COL_TOTAL, total)
            put(RECEIPT_COL_PAID_BY, paidBy)
            put(RECEIPT_COL_CONTRIBUTIONS, contributionsString)
        }
        val whereClause = "$RECEIPT_COL_ID = ?"
        val whereArgs = arrayOf(editSqlRowId)
        return writer.update(RECEIPT_TABLE_NAME, values, whereClause, whereArgs).toString()
    }

    private fun cleanDay(dayOfMonth: Int): String {
        var dayString = ""
        val day = dayOfMonth.toString()
        dayString = if (day.length == 1) {
            "0$day"
        } else {
            day
        }
        return dayString
    }

    private fun cleanMonth(monthOfYear: Int): String {
        val monthOf = monthOfYear + 1
        var monthString = ""
        monthString = if (monthOf in 1..9) {
            "0$monthOf"
        } else {
            "$monthOf"
        }
        return monthString
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}

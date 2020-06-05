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
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_FK_ACCOUNT_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_PAID_BY
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_TITLE
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_TOTAL
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_UNIQUE_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_TABLE_NAME
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_CONTRIBUTIONS
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
    private lateinit var sqlId: String

    companion object {
        var sqlAccountId: String? = "-1"
        lateinit var participantList: ArrayList<String>
        const val CONTRIBUTION_INTENT_DATA = "contribution"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewReceiptCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sqlAccountId = intent.getStringExtra("sqlID")
        participantList = ArrayList()
        retrieveParticipants()

        setSupportActionBar(findViewById(R.id.toolbar))
        val actionBar : androidx.appcompat.app.ActionBar? = supportActionBar
        actionBar?.title = "Add expense"
        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_x_white)
        }

        binding.paidBySpinner.adapter = ArrayAdapter(this,
            R.layout.support_simple_spinner_dropdown_item, participantList)

        val pagerAdapter = ReceiptPagerAdapter(supportFragmentManager)
        binding.receiptViewPager.adapter = pagerAdapter
        binding.receiptTabLayout.setupWithViewPager(binding.receiptViewPager)
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

                    val sqlRow = updateSql(receiptFirebaseID, date, title, total, paidBy, contributionsString)
                    Toast.makeText(this, sqlRow.toString(), Toast.LENGTH_SHORT).show()
                    intent.putExtra(CONTRIBUTION_INTENT_DATA, contributionsString)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                    return true
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
        sqlAccountId = ReceiptOverviewActivity.getSqlAccountId
        val reader = dbHelper.readableDatabase
        val columns = arrayOf(DbManager.AccountTable.ACCOUNT_COL_PARTICIPANTS)
        val selectClause = "${DbManager.AccountTable.ACCOUNT_COL_ID} = ?"
        val selectArgs = arrayOf(sqlAccountId)
        val cursor: Cursor = reader.query(
            DbManager.AccountTable.ACCOUNT_TABLE_NAME, columns, selectClause, selectArgs,
            null, null, null)
        val particColIndex = cursor.getColumnIndexOrThrow(DbManager.AccountTable.ACCOUNT_COL_PARTICIPANTS)
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
        onBackPressed()
        return true
    }

    private fun retrieveTodaysDate(): String {
        val date = LocalDate.now()
        return date.format(DateTimeFormatter.ofPattern(getString(R.string.date_format_dd_MM_yyyy))).toString()
    }

    private fun updateSql(recFirebaseId: String, date: String, title: String, total: Float, paidBy: String, contributions: String) : Int{
        val dbHelper = DbHelper(this)
        val write = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(RECEIPT_COL_UNIQUE_ID, recFirebaseId)
            put(RECEIPT_COL_DATE, date)
            put(RECEIPT_COL_TITLE, title)
            put(RECEIPT_COL_TOTAL, total)
            put(RECEIPT_COL_PAID_BY, paidBy)
            put(RECEIPT_COL_CONTRIBUTIONS, contributions)
            put(RECEIPT_COL_FK_ACCOUNT_ID, sqlAccountId)
        }
        val sqlId = write.insert(RECEIPT_TABLE_NAME, null, values)
        dbHelper.close()
        return sqlId.toInt()
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

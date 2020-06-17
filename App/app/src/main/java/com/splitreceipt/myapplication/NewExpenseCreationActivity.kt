package com.splitreceipt.myapplication

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.splitreceipt.myapplication.SplitReceiptScanFragment.Companion.ownershipEqualString
import com.splitreceipt.myapplication.data.*
import com.splitreceipt.myapplication.databinding.ActivityNewReceiptCreationBinding
import kotlinx.android.synthetic.main.fragment_split_receipt_scan.*
import java.lang.StringBuilder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList


class NewExpenseCreationActivity : AppCompatActivity() {

    /*
    Activity gives the user the ability to input a new receipt/ transaction
     */

    private lateinit var binding: ActivityNewReceiptCreationBinding
    private var editPaidBy: String = ""

    companion object {
        var sqlAccountId: String? = "-1"
        var editSqlRowId: String = "-1"
        const val zeroCurrency: String = "0.00"
        lateinit var participantList: ArrayList<String>
        lateinit var participantDataEditList: ArrayList<ParticipantData> //TODO: Currently being initialised even when it is not an edit. Fix this.
        const val CONTRIBUTION_INTENT_DATA = "contribution"
        const val intentSqlIdString = "sqlID"
        const val intentFirebaseIdString = "firebaseID"

        var currencyCode = ""
        var currencySymbol = ""

        const val editIntentTitleString = "edit_title"
        const val editIntentTotalString = "edit_total"
        const val editIntentPaidByString = "edit_paid_by"
        const val editIntentDateString = "edit_date"
        const val editIntentContributionsString = "edit_contributions"
        const val editIntentSqlRowIdString = "edit_sql_id"
        const val editIntentScannedBoolean = "edit_scanned"
        var isEdit: Boolean = false
        var isScanned: Boolean = false
        var editTotal: String = ""

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewReceiptCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sqlAccountId = intent.getStringExtra(intentSqlIdString)
        participantList = ArrayList()
        participantDataEditList = ArrayList()
        participantList = SqlDbHelper(this).retrieveParticipants(participantList, sqlAccountId!!)

        val spinnerAdapter = ArrayAdapter(
            this,
            R.layout.support_simple_spinner_dropdown_item, participantList
        )
        binding.paidBySpinner.adapter = spinnerAdapter

        val pagerAdapter = ExpensePagerAdapter(this)
        binding.receiptViewPager.adapter = pagerAdapter
        TabLayoutMediator(binding.receiptTabLayout, binding.receiptViewPager) { tab, position ->
            val tabNames = arrayOf("Split manually", "Scan receipt")
            //To get the first name of doppelganger celebrities
            tab.text = tabNames[position]
        }.attach()
        binding.receiptViewPager.currentItem = 0

        val editTitle = intent.getStringExtra(editIntentTitleString)
        if (editTitle != null) {
            isEdit = true
            editSqlRowId = intent.getStringExtra(editIntentSqlRowIdString)!!
            val editScanned = intent.getBooleanExtra(editIntentScannedBoolean, false)
            if (editScanned) {
                // User is editing a scanned receipt. Split receipt fragment will see this and load the itemised products accordingly
                isScanned = true
                binding.receiptViewPager.currentItem = 1
            }
            else {
                // User is editing a manual expense. Get contributions ready for the manual fragment to show in UI.
                val editContributions = intent.getStringExtra(editIntentContributionsString)
                deconstructAndBuildEditContribs(editContributions!!)
            }
            editTotal = intent.getStringExtra(editIntentTotalString)!!
            editPaidBy = intent.getStringExtra(editIntentPaidByString)!!
            val paidByPosition = spinnerAdapter.getPosition(editPaidBy)
            binding.paidBySpinner.setSelection(paidByPosition)
            val editDate = intent.getStringExtra(editIntentDateString)
            binding.receiptTitleEditText.setText(editTitle)
            binding.dateButton.text = editDate
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Add expense"
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_x_white)
        }
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
                val currentPage = binding.receiptViewPager.currentItem
                val okayToProceed = checkAllInputsAreValid(currentPage)
                if (okayToProceed) {
                    val sqlDbHelper = SqlDbHelper(this)
                    val writeableDB = sqlDbHelper.writableDatabase
                    val firebaseGroupId = intent.getStringExtra(intentFirebaseIdString)
                    val firebaseDbHelper = FirebaseDbHelper(firebaseGroupId)
                    //Obtain global receipt details
                    val date = getDate()
                    val title =  binding.receiptTitleEditText.text.toString()
                    val total : Float
                    val paidBy = binding.paidBySpinner.selectedItem.toString()
                    val expenseFirebaseID: String?
                    val scanned: Boolean

                    //Check where the user is
                    if (currentPage == 0) {
                        //User is saving a manual expense
                        total = findViewById<EditText>(R.id.currencyAmount).text.toString().toFloat()
                        val participantDataList = SplitExpenseManuallyFragment.fragmentManualParticipantList
                        val participantBalDataList: ArrayList<ParticipantBalanceData> = particDataToParticBalData(participantDataList)
                        val contributionsString = createContribString(participantBalDataList, paidBy)
                        Log.i("TEST", contributionsString)

                        if (!isEdit) {
                            // Insert new entry to SQL DB.
                            scanned = false
                            expenseFirebaseID = newFirebaseReceiptID()
                            sqlDbHelper.insertNewExpense(sqlAccountId!!, expenseFirebaseID, date, title, total, paidBy, contributionsString, scanned)
                            sqlDbHelper.close()
                            firebaseDbHelper.createNewExpense(expenseFirebaseID, date, title, total, paidBy, contributionsString)
                            intent.putExtra(CONTRIBUTION_INTENT_DATA, contributionsString)
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                            return true
                        } else {
                            // Update previous entry in SQL DB
                            val sqlRow = sqlDbHelper.updateExpense(editSqlRowId, date, title, total, paidBy, contributionsString)
                            sqlDbHelper.close()
                            intent.putExtra(ExpenseViewActivity.expenseReturnEditSql, sqlRow) //TODO: Is this necessary?
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
                    }
                    else if (currentPage == 1){
                        //User is saving a scanned receipt
                        total = findViewById<EditText>(R.id.currencyAmountScan).text.toString().toFloat()
                        val itemizedProductList = SplitReceiptScanFragment.itemizedArrayList
                        val particBalDataList: ArrayList<ParticipantBalanceData> = productsToParticBalData(itemizedProductList)
                        val contributionsString = createContribString(particBalDataList, paidBy)

                        if (!isEdit) {
                            // User is inserting a new receipt expense
                            expenseFirebaseID = newFirebaseReceiptID()
                            val sqlRow = sqlDbHelper.insertNewExpense(sqlAccountId!!, expenseFirebaseID, date, title, total, paidBy, contributionsString, true)
                            firebaseDbHelper.createNewExpense(expenseFirebaseID, date, title, total, paidBy, contributionsString)
                            sqlDbHelper.insertReceiptItems(itemizedProductList, sqlRow)
                            firebaseDbHelper.addReceiptItems(expenseFirebaseID, itemizedProductList)
                            intent.putExtra(CONTRIBUTION_INTENT_DATA, contributionsString)
                            setResult(Activity.RESULT_OK, intent)
                            sqlDbHelper.close()
                            finish()
                            return true
                        } else {
                            // User is editing a previously saved receipt expense
                            val sqlRow = sqlDbHelper.updateExpense(editSqlRowId, date, title, total, paidBy, contributionsString)
                            sqlDbHelper.updateItemsSql(writeableDB, itemizedProductList)
                            intent.putExtra(ExpenseViewActivity.expenseReturnEditTitle, title)
                            intent.putExtra(ExpenseViewActivity.expenseReturnEditTotal, total.toString())
                            intent.putExtra(ExpenseViewActivity.expenseReturnEditPaidBy, paidBy)
                            intent.putExtra(ExpenseViewActivity.expenseReturnEditDate, date)
                            intent.putExtra(ExpenseViewActivity.expenseReturnEditContributions, contributionsString)
                            setResult(Activity.RESULT_OK, intent)
                            isEdit = false
                            finish()
                            return true
                        }
                    }
                sqlDbHelper.close()
                } else
                {return false}
            }
            else -> return false
        }
        return false
    }

    private fun productsToParticBalData(itemizedProductList: ArrayList<ScannedItemizedProductData>): ArrayList<ParticipantBalanceData> {
        val particBalDataList: ArrayList<ParticipantBalanceData> = ArrayList()
        for (participant in participantList) {
            //Populate a default particBalDataList using the groups participants
            if (participant != ownershipEqualString){
                particBalDataList.add(ParticipantBalanceData(participant, 0.0F))
            }
        }

        val numberParticipants = particBalDataList.size
        for (product in itemizedProductList){
            val itemValue = product.itemValue.toFloat()
            val itemOwnership = product.ownership
            if (itemOwnership == SplitReceiptScanFragment.ownershipEqualString) {
                // This product is going to be split evenly between all participants
                val equalSplit: Float = ExpenseOverviewActivity.roundToTwoDecimalPlace(itemValue / numberParticipants)
                for (participant in particBalDataList) {
                    participant.balance += equalSplit
                }
            }
            else {
                // This product is being paid for only by one participant
                for (participant in particBalDataList) {
                    if (participant.name == itemOwnership) {
                        participant.balance += itemValue
                        break
                    }
                }
            }
        }
        for (participant in particBalDataList){
            //Round to 2dp
            participant.balance = ExpenseOverviewActivity.roundToTwoDecimalPlace(participant.balance)
        }
        return particBalDataList
    }

    private fun particDataToParticBalData(participantDataList: ArrayList<ParticipantData>): ArrayList<ParticipantBalanceData> {
        val particBalDataList: ArrayList<ParticipantBalanceData> = ArrayList()
        for (participant in participantDataList) {
            val name = participant.name
            val contributions = participant.contributionValue.toFloat()
            particBalDataList.add(ParticipantBalanceData(name, contributions))
        }
        return particBalDataList
    }

    private fun newFirebaseReceiptID() : String {
        // Creates a timestamp for a unique identifier in the database to avoid any new receipt collisions
        return System.currentTimeMillis().toString()
    }

    private fun createContribString(updatedContribList: ArrayList<ParticipantBalanceData>, paidBy: String): String {
        val sb = StringBuilder()
        for (participant in updatedContribList) {
            val name = participant.name
            val nameString = "$name,"
            sb.append(nameString)
            val value = participant.balance.toString()
            val valString = "$value,"
            sb.append(valString)
            val paidByString = "$paidBy/"
            sb.append(paidByString)
        }
        sb.deleteCharAt(sb.lastIndex)
        return sb.toString()
    }

    private fun checkAllInputsAreValid(currentPage: Int): Boolean {
        //TODO: Animate the items which need input
        if (binding.receiptTitleEditText.text!!.isEmpty()) {
            Toast.makeText(this, "Please add a title", Toast.LENGTH_SHORT).show()
            return false
        }
        if (currentPage == 0){
            //Manual Expense
            val currencyAmount: EditText = findViewById(R.id.currencyAmount)
             if (currencyAmount.text.isEmpty()){
                        Toast.makeText(this, "Please add an amount", Toast.LENGTH_SHORT).show()
                        return false
                    }
        } else if (currentPage == 1) {
            if (currencyAmountScan.text!!.isEmpty()){
                Toast.makeText(this, "Please add an amount", Toast.LENGTH_SHORT).show()
                return false
            }
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

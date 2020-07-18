package com.splitreceipt.myapplication

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.splitreceipt.myapplication.CurrencyHelper.CurrencyDetails
import com.splitreceipt.myapplication.CurrencyHelper.EXCHANGE_RATE_OF_1
import com.splitreceipt.myapplication.CurrencyHelper.retrieveExchangeRate
import com.splitreceipt.myapplication.ExpenseOverviewActivity.Companion.firebaseDbHelper
import com.splitreceipt.myapplication.ExpenseOverviewActivity.Companion.roundToTwoDecimalPlace
import com.splitreceipt.myapplication.SplitReceiptScanFragment.Companion.itemizedArrayList
import com.splitreceipt.myapplication.SplitReceiptScanFragment.Companion.ownershipEqualString
import com.splitreceipt.myapplication.data.*
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_GROUP_CURRENCY_CODE
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_GROUP_CURRENCY_SYMBOL
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_NAME
import com.splitreceipt.myapplication.databinding.ActivityNewExpenseCreationBinding
import kotlinx.android.synthetic.main.fragment_split_receipt_scan.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList


class NewExpenseCreationActivity : AppCompatActivity() {

    /*
    Activity gives the user the ability to input a new receipt/ transaction
     */

    private lateinit var binding: ActivityNewExpenseCreationBinding
    private var editPaidBy: String = ""
    private var firebaseEditExpenseID: String = ""
    val MANUAL_PAGE_INDEX = 0
    val SCANNED_PAGE_INDEX = 1

    companion object {
        var sqlGroupId: String? = "-1"
        var editSqlRowId: String = "-1"
        const val zeroCurrency: String = "0.00"
        lateinit var participantList: ArrayList<String>
        lateinit var participantDataEditList: ArrayList<ParticipantData> //TODO: Currently being initialised even when it is not an edit. Fix this.
        const val CONTRIBUTION_INTENT_DATA = "contribution"
        const val intentSqlExpenseIdString = "sqlExpenseID"
        const val intentSqlGroupIdString = "sqlGroupID"
        const val intentFirebaseIdString = "firebaseID"
        const val intentManualOrScan = "manual_scan"

        var currencyCode = ""
        var currencySymbol = ""

        const val editIntentTitleString = "edit_title"
        const val editIntentTotalString = "edit_total"
        const val editIntentPaidByString = "edit_paid_by"
        const val editIntentDateString = "edit_date"
        const val editIntentContributionsString = "edit_contributions"
        const val editIntentCurrency = "edit_currency"
        const val editIntentCurrencyUiSymbol = "edit_ui_symbol"

        const val editIntentFirebaseExpenseIdString = "edit_firebase_id"
        const val editIntentScannedBoolean = "edit_scanned"
        var isEdit: Boolean = false
        var isScanned: Boolean = false
        var editTotal: String = ""
        var editExchangeRate: Float? = null

        fun retrieveTodaysDate(context: Context): String {
            val date = LocalDate.now()
            return date.format(DateTimeFormatter.ofPattern(context.getString(R.string.date_format_dd_MM_yyyy))).toString()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewExpenseCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sqlGroupId = intent.getStringExtra(intentSqlGroupIdString)
        participantList = ArrayList()
        participantDataEditList = ArrayList()
        participantList = SqlDbHelper(this).retrieveParticipants(participantList, sqlGroupId!!)

        val spinnerAdapter = ArrayAdapter(
            this,
            R.layout.support_simple_spinner_dropdown_item, participantList
        )
        binding.paidBySpinner.adapter = spinnerAdapter
        val pagerAdapter = ExpensePagerAdapter(this)
        binding.receiptViewPager.adapter = pagerAdapter
        TabLayoutMediator(binding.receiptTabLayout, binding.receiptViewPager) { tab, position ->
            val tabNames = arrayOf("Split manually", "Scan receipt")
            tab.text = tabNames[position]
        }.attach()

        val manualOrScan = intent.getBooleanExtra(intentManualOrScan, false)
        if (!manualOrScan) {
            //Show manual
            binding.receiptViewPager.currentItem = 0
        }
        else {
            //Show scan
            binding.receiptViewPager.currentItem = 1
        }

        binding.receiptViewPager.isUserInputEnabled = false

        val editTitle = intent.getStringExtra(editIntentTitleString)
        if (editTitle != null) {
            isEdit = true
            firebaseEditExpenseID = intent.getStringExtra(editIntentFirebaseExpenseIdString)!!
            editSqlRowId = intent.getStringExtra(intentSqlExpenseIdString)!!
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

        setCurrencyCode(isEdit)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = ""
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_x_white)
        }
    }

    private fun setCurrencyCode(edit: Boolean) {
        if (edit) {
            currencyCode = intent.getStringExtra(editIntentCurrency)!!
            currencySymbol = intent.getStringExtra(editIntentCurrencyUiSymbol)!!
        } else {
            val sharedPref = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            currencyCode = sharedPref.getString(SHARED_PREF_GROUP_CURRENCY_CODE, "USD")!!
            currencySymbol = sharedPref.getString(SHARED_PREF_GROUP_CURRENCY_SYMBOL, "$")!!
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
        menuInflater.inflate(R.menu.save_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuSave -> {
                val currentPage = binding.receiptViewPager.currentItem
                if (okayToProceed(currentPage)) {
                    val userExpense: ExpenseData
                    val sqlDbHelper = SqlDbHelper(this)
                    val writeableDB = sqlDbHelper.writableDatabase //TODO: Necessary?

                    if (currentPage == MANUAL_PAGE_INDEX) {
                        //User is saving a manual expense
                        userExpense = returnNewManualExpense(sqlDbHelper)
                        editExchangeRate = null //TODO: Necessary?

                        if (userIsAddingNewExpense()) {
                            sqlDbHelper.insertNewExpense(userExpense)
                            sqlDbHelper.close() //TODO: Closing correctly?
                            firebaseDbHelper!!.insertOrUpdateExpense(userExpense)
                            intent.putExtra(CONTRIBUTION_INTENT_DATA, userExpense.contribs)
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                            return true
                        } else {
                            userExpense.sqlRowId = editSqlRowId
                            sqlDbHelper.updateExpense(userExpense)
                            sqlDbHelper.close() //TODO: Closing correctly?
                            firebaseDbHelper!!.insertOrUpdateExpense(userExpense)
                            putExpenseEditDataInIntent(intent, userExpense)
                            setResult(Activity.RESULT_OK, intent)
                            isEdit = false
                            finish()
                            return true
                        }
                    }
                    else if (currentPage == SCANNED_PAGE_INDEX){
                        if (allTextRecogErrorsCleared()) {
                            userExpense = returnNewScannedExpense(sqlDbHelper)
                            editExchangeRate = null //TODO: Necessary?

                            if (userIsAddingNewExpense()) {
                                // User is inserting a new receipt expense
                                val sqlRow = sqlDbHelper.insertNewExpense(userExpense)
                                firebaseDbHelper!!.insertOrUpdateExpense(userExpense)
                                sqlDbHelper.insertReceiptItems(itemizedArrayList, sqlRow)
                                firebaseDbHelper!!.addUpdateReceiptItems(userExpense.firebaseIdentifier, itemizedArrayList)
                                intent.putExtra(CONTRIBUTION_INTENT_DATA, userExpense.contribs)
                                setResult(Activity.RESULT_OK, intent)
                                sqlDbHelper.close() //TODO: Closing correctly?
                                finish()
                                return true
                            } else {
                                userExpense.sqlRowId = editSqlRowId
                                sqlDbHelper.updateExpense(userExpense)
                                sqlDbHelper.updateItemsSql(writeableDB, itemizedArrayList)
                                firebaseDbHelper!!.insertOrUpdateExpense(userExpense)
                                firebaseDbHelper!!.addUpdateReceiptItems(firebaseEditExpenseID, itemizedArrayList)
                                putExpenseEditDataInIntent(intent, userExpense)
                                setResult(Activity.RESULT_OK, intent)
                                isEdit = false
                                finish()
                                return true
                            }
                        } else {
                            Toast.makeText(this, "Please click and edit all product errors", Toast.LENGTH_SHORT).show()
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

    private fun allTextRecogErrorsCleared() = SplitReceiptScanFragment.errorsCleared

    private fun putExpenseEditDataInIntent(intent: Intent, userExpense: ExpenseData) {
        intent.putExtra(ExpenseViewActivity.expenseReturnEditDate, userExpense.date)
        intent.putExtra(ExpenseViewActivity.expenseReturnEditTotal, userExpense.total.toString())
        intent.putExtra(ExpenseViewActivity.expenseReturnEditTitle, userExpense.title)
        intent.putExtra(ExpenseViewActivity.expenseReturnEditPaidBy, userExpense.paidBy)
        intent.putExtra(ExpenseViewActivity.expenseReturnEditContributions, userExpense.contribs)
    }

    private fun userIsAddingNewExpense() : Boolean {
        return !isEdit
    }

    private fun returnNewManualExpense(sqlDbHelper: SqlDbHelper): ExpenseData {
        val newExpense = returnBasicExpense(false)
        val participantList: ArrayList<ParticipantBalanceData> = participantsToParticipantBalance(SplitExpenseManuallyFragment.fragmentManualParticipantList)
        val exchangeRate = retrieveExchangeRate(currencyCode, editExchangeRate, sqlDbHelper)
        if (exchangeRate != EXCHANGE_RATE_OF_1) {
            exchangeContributionsToBaseCurrency(participantList, exchangeRate)
        }
        val currencyDetails = CurrencyDetails(currencyCode, currencySymbol, exchangeRate)

        newExpense.setUpNewExpense(findViewById(R.id.currencyAmountManual), participantList, currencyDetails)
        return newExpense
    }

    private fun returnNewScannedExpense(sqlDbHelper: SqlDbHelper): ExpenseData {
        val newExpense = returnBasicExpense(true)
        val participantList: ArrayList<ParticipantBalanceData> = productsToParticipantBalances(itemizedArrayList)

        val exchangeRate = retrieveExchangeRate(currencyCode, editExchangeRate, sqlDbHelper)
        if (exchangeRate != EXCHANGE_RATE_OF_1) {
            exchangeContributionsToBaseCurrency(participantList, exchangeRate)
        }
        val currencyDetails = CurrencyDetails(currencyCode, currencySymbol, exchangeRate)

        newExpense.setUpNewExpense(findViewById(R.id.currencyAmountScan), participantList, currencyDetails)
        return newExpense
    }

    private fun returnBasicExpense(scanned: Boolean) : ExpenseData{
        val date = getExpenseDate()
        val title = binding.receiptTitleEditText.text.toString()
        val paidBy = binding.paidBySpinner.selectedItem.toString()
        val lastEdit: String = System.currentTimeMillis().toString()
        return ExpenseData(date, title, paidBy, scanned, lastEdit)
    }

    private fun exchangeContributionsToBaseCurrency(participantList: ArrayList<ParticipantBalanceData>, exchangeRate: Float) {
        for (participant in participantList) {
            participant.contribsToBaseCurrency(exchangeRate)
        }
    }

    private fun productsToParticipantBalances(itemizedProductList: ArrayList<ScannedItemizedProductData>): ArrayList<ParticipantBalanceData> {
        val particBalDataList: ArrayList<ParticipantBalanceData> = participantsToParticipantBalance(participantList)

        val numberParticipants = particBalDataList.size
        for (product in itemizedProductList){
            val itemValue = product.itemValue.toFloat()
            val itemOwnership = product.ownership
            if (itemOwnership == ownershipEqualString) {
                splitProductEquallyBetweenGroup(itemValue, numberParticipants, particBalDataList)
            }
            else {
                for (participant in particBalDataList) {
                    if (participant.userName == itemOwnership) {
                        participant.userBalance += itemValue
                        break
                    }
                }
            }
        }
        return particBalDataList
    }

    private fun splitProductEquallyBetweenGroup(itemValue: Float, numberParticipants: Int, particBalDataList: ArrayList<ParticipantBalanceData>) {
        val equalSplit: Float = roundToTwoDecimalPlace(itemValue / numberParticipants)
        for (participant in particBalDataList) {
            participant.userBalance += equalSplit
        }
    }

    private fun participantsToParticipantBalance(participantList: ArrayList<ParticipantData>, data:Boolean=true): ArrayList<ParticipantBalanceData> {
        val particBalDataList: ArrayList<ParticipantBalanceData> = ArrayList()
        for (participant in participantList) {
            particBalDataList.add(participant.toParticipantBalanceData())
        }
        return particBalDataList
    }

    private fun participantsToParticipantBalance(participantList: ArrayList<String>): ArrayList<ParticipantBalanceData> {
        //TODO: Can we remove this area of the code by changing participantList from Strings to ParticipantBaqlanceData?
        val particBalDataList: ArrayList<ParticipantBalanceData> = ArrayList()
        for (participant in participantList) {
            if (participant != ownershipEqualString){
                particBalDataList.add(ParticipantBalanceData(participant))
            }
        }
        return particBalDataList
    }

    private fun okayToProceed(currentPage: Int): Boolean {
        //TODO: Animate the items which need input
        if (binding.receiptTitleEditText.text!!.isEmpty()) {
            Toast.makeText(this, "Please add a title", Toast.LENGTH_SHORT).show()
            return false
        }
        if (currentPage == 0){
            //Manual Expense
            val currencyAmount: EditText = findViewById(R.id.currencyAmountManual)
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
        val picker = DatePickerDialog(this,
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val dayString = cleanDay(dayOfMonth)
                val monthString = cleanMonth(monthOfYear)
                val yearString: String = year.toString()
                val string = "$dayString/$monthString/$yearString"
                binding.dateButton.text = string }, year, month, day)
        picker.show()
    }

    private fun getExpenseDate(): String{
        val dateSelection = binding.dateButton.text.toString()
        return if (dateSelection == getString(R.string.when_today)) {
            retrieveTodaysDate(this)
        } else {
            dateSelection
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        Toast.makeText(this, "Expense cancelled", Toast.LENGTH_SHORT).show()
        onBackPressed()
        return true
    }

    private fun cleanDay(dayOfMonth: Int): String {
        val dayString: String
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
        val monthString: String
        monthString = if (monthOf in 1..9) {
            "0$monthOf"
        } else {
            "$monthOf"
        }
        return monthString
    }

    override fun onBackPressed() {
        isEdit = false
        super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}

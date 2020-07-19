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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.splitreceipt.myapplication.helper_classes.CurrencyHelper.CurrencyDetails
import com.splitreceipt.myapplication.helper_classes.CurrencyHelper.EXCHANGE_RATE_OF_1
import com.splitreceipt.myapplication.helper_classes.CurrencyHelper.retrieveExchangeRate
import com.splitreceipt.myapplication.ExpenseOverviewActivity.Companion.firebaseDbHelper
import com.splitreceipt.myapplication.ExpenseOverviewActivity.Companion.roundToTwoDecimalPlace
import com.splitreceipt.myapplication.SplitReceiptScanFragment.Companion.itemizedArrayList
import com.splitreceipt.myapplication.SplitReceiptScanFragment.Companion.ownershipEqualString
import com.splitreceipt.myapplication.adapters.ExpensePagerAdapter
import com.splitreceipt.myapplication.data.*
import com.splitreceipt.myapplication.helper_classes.DateSelectionCleaner.returnDateString
import com.splitreceipt.myapplication.managers.SharedPrefManager.SHARED_PREF_GROUP_CURRENCY_CODE
import com.splitreceipt.myapplication.managers.SharedPrefManager.SHARED_PREF_GROUP_CURRENCY_SYMBOL
import com.splitreceipt.myapplication.managers.SharedPrefManager.SHARED_PREF_NAME
import com.splitreceipt.myapplication.databinding.ActivityNewExpenseCreationBinding
import com.splitreceipt.myapplication.helper_classes.DateSelectionCleaner.retrieveTodaysDate
import com.splitreceipt.myapplication.helper_classes.SqlDbHelper
import kotlinx.android.synthetic.main.fragment_split_receipt_manually.*
import kotlinx.android.synthetic.main.fragment_split_receipt_scan.*
import java.util.*
import kotlin.collections.ArrayList


class NewExpenseCreationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewExpenseCreationBinding
    private var editPaidBy: String = ""
    private var firebaseEditExpenseID: String = ""
    private val MANUAL_PAGE_INDEX = 0
    private val SCANNED_PAGE_INDEX = 1

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

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewExpenseCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sqlGroupId = intent.getStringExtra(intentSqlGroupIdString)
        participantList = ArrayList()
        participantDataEditList = ArrayList()
        participantList = SqlDbHelper(
            this
        ).retrieveParticipants(participantList, sqlGroupId!!)
        binding.receiptViewPager.isUserInputEnabled = false

        val spinnerAdapter = setUpPaidBySpinner()

        val editTitle = userIsEditingPrevExpense()
        if (editTitle != null) {
            setUpEditExpenseWithIntents(spinnerAdapter, editTitle)
        }

        setCurrencyCode(isEdit)
        setUpViewPagerWithCorrectPage()

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = ""
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_x_white)
        }
    }

    private fun setUpEditExpenseWithIntents(spinnerAdapter: ArrayAdapter<String>, editTitle: String?) {
        isEdit = true
        firebaseEditExpenseID = intent.getStringExtra(editIntentFirebaseExpenseIdString)!!
        editSqlRowId = intent.getStringExtra(intentSqlExpenseIdString)!!
        if (userIsEditingScannedExpense()) {
            isScanned = true
            binding.receiptViewPager.currentItem = SCANNED_PAGE_INDEX
        } else {
            val editContributions = intent.getStringExtra(editIntentContributionsString)!!
            participantDataEditList =
                ParticipantData.contribStringToParticipantData(editContributions, editPaidBy)
        }

        editTotal = intent.getStringExtra(editIntentTotalString)!!
        editPaidBy = intent.getStringExtra(editIntentPaidByString)!!
        val paidByPosition = spinnerAdapter.getPosition(editPaidBy)
        binding.paidBySpinner.setSelection(paidByPosition)
        val editDate = intent.getStringExtra(editIntentDateString)
        binding.receiptTitleEditText.setText(editTitle)
        binding.dateButton.text = editDate
    }

    private fun userIsEditingPrevExpense() = intent.getStringExtra(editIntentTitleString)

    private fun userIsEditingScannedExpense(): Boolean {
        return intent.getBooleanExtra(editIntentScannedBoolean, false)
    }

    private fun setUpPaidBySpinner(): ArrayAdapter<String> {
        val spinnerAdapter =
            ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, participantList)
        binding.paidBySpinner.adapter = spinnerAdapter
        return spinnerAdapter
    }

    private fun setUpViewPagerWithCorrectPage() {
        val pagerAdapter = ExpensePagerAdapter(this)
        binding.receiptViewPager.adapter = pagerAdapter

        if (userClickedScannedPage()) {
            binding.receiptViewPager.currentItem = SCANNED_PAGE_INDEX
        } else {
            binding.receiptViewPager.currentItem = MANUAL_PAGE_INDEX
        }
    }

    private fun userClickedScannedPage(): Boolean {
        return intent.getBooleanExtra(intentManualOrScan, false)
    }

    private fun setCurrencyCode(userIsEditingPrevExpense: Boolean) {
        if (userIsEditingPrevExpense) {
            currencyCode = intent.getStringExtra(editIntentCurrency)!!
            currencySymbol = intent.getStringExtra(editIntentCurrencyUiSymbol)!!
        } else {
            val sharedPref = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            currencyCode = sharedPref.getString(SHARED_PREF_GROUP_CURRENCY_CODE, "USD")!!
            currencySymbol = sharedPref.getString(SHARED_PREF_GROUP_CURRENCY_SYMBOL, "$")!!
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuSave -> {
                val currentUserPage = binding.receiptViewPager.currentItem
                if (okayToProceed(currentUserPage)) {
                    val userExpense: Expense
                    val sqlDbHelper =
                        SqlDbHelper(
                            this
                        )

                    if (currentUserPage == MANUAL_PAGE_INDEX) {
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
                            userExpense.sqlExpenseRowId = editSqlRowId
                            userExpense.firebaseIdentifier = firebaseEditExpenseID
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
                    else if (currentUserPage == SCANNED_PAGE_INDEX){
                        if (allTextRecogErrorsCleared()) {
                            userExpense = returnNewScannedExpense(sqlDbHelper)
                            editExchangeRate = null //TODO: Necessary?

                            if (userIsAddingNewExpense()) {
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
                                userExpense.sqlExpenseRowId = editSqlRowId
                                userExpense.firebaseIdentifier = firebaseEditExpenseID
                                sqlDbHelper.updateExpense(userExpense)
                                sqlDbHelper.updateItemsSql(itemizedArrayList)
                                firebaseDbHelper!!.insertOrUpdateExpense(userExpense)
                                firebaseDbHelper!!.addUpdateReceiptItems(userExpense.firebaseIdentifier, itemizedArrayList)
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

    private fun okayToProceed(currentPage: Int): Boolean {
        //TODO: Animate the items which need input
        if (binding.receiptTitleEditText.text!!.isEmpty()) {
            Toast.makeText(this, "Please add a title", Toast.LENGTH_SHORT).show()
            return false
        }
        if (currentPage == MANUAL_PAGE_INDEX){
            if (currencyAmountManual.text.isEmpty()){
                Toast.makeText(this, "Please add an amount", Toast.LENGTH_SHORT).show()
                return false
            }
        } else {
            if (currencyAmountScan.text!!.isEmpty()){
                Toast.makeText(this, "Please add an amount", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun returnNewManualExpense(sqlDbHelper: SqlDbHelper): Expense {
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

    private fun returnBasicExpense(scanned: Boolean) : Expense{
        val date = getExpenseDate()
        val title = binding.receiptTitleEditText.text.toString()
        val paidBy = binding.paidBySpinner.selectedItem.toString()
        val lastEdit: String = System.currentTimeMillis().toString()
        return Expense(date, title, paidBy, scanned, lastEdit)
    }

    private fun userIsAddingNewExpense() : Boolean {
        return !isEdit
    }

    private fun returnNewScannedExpense(sqlDbHelper: SqlDbHelper): Expense {
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

    private fun allTextRecogErrorsCleared() = SplitReceiptScanFragment.errorsCleared

    private fun putExpenseEditDataInIntent(intent: Intent, userExpense: Expense) {
        intent.putExtra(ExpenseViewActivity.expenseReturnEditDate, userExpense.date)
        intent.putExtra(ExpenseViewActivity.expenseReturnEditTotal, userExpense.total.toString())
        intent.putExtra(ExpenseViewActivity.expenseReturnEditTitle, userExpense.title)
        intent.putExtra(ExpenseViewActivity.expenseReturnEditPaidBy, userExpense.paidBy)
        intent.putExtra(ExpenseViewActivity.expenseReturnEditContributions, userExpense.contribs)
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
        //TODO: Can we remove this area of the code by changing participantList from Strings to ParticipantBalanceData?
        val particBalDataList: ArrayList<ParticipantBalanceData> = ArrayList()
        for (participant in participantList) {
            if (participant != ownershipEqualString){
                particBalDataList.add(ParticipantBalanceData(participant))
            }
        }
        return particBalDataList
    }

    fun datePicker(view: View){
        val cldr: Calendar = Calendar.getInstance()
        val day: Int = cldr.get(Calendar.DAY_OF_MONTH)
        val month: Int = cldr.get(Calendar.MONTH)
        val year: Int = cldr.get(Calendar.YEAR)
        val picker = DatePickerDialog(this,
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val dateString = returnDateString(year, monthOfYear, dayOfMonth)
                binding.dateButton.text = dateString }, year, month, day)
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

    override fun onBackPressed() {
        isEdit = false
        super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.save_menu, menu)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}

package com.splitreceipt.myapplication

import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
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
import com.splitreceipt.myapplication.CurrencySelectorActivity.Companion.SHARED_PREF_NAME
import com.splitreceipt.myapplication.SplitReceiptScanFragment.Companion.ownershipEqualString
import com.splitreceipt.myapplication.data.*
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_DATE
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_FK_GROUP_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_PAID_BY
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_TITLE
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_TOTAL
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_UNIQUE_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_TABLE_NAME
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_FK_RECEIPT_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_NAME
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_OWNERSHIP
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_COL_VALUE
import com.splitreceipt.myapplication.data.DbManager.ReceiptItemsTable.ITEMS_TABLE_NAME
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_CONTRIBUTIONS
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_SCANNED
import com.splitreceipt.myapplication.databinding.ActivityNewReceiptCreationBinding
import kotlinx.android.synthetic.main.fragment_split_receipt_scan.*
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
        var editSqlRowId: String = "-1"
        const val zeroCurrency: String = "0.00"
        lateinit var participantList: ArrayList<String>
        lateinit var participantDataEditList: ArrayList<ParticipantData> //TODO: Currently being initialised even when it is not an edit. Fix this.
        const val CONTRIBUTION_INTENT_DATA = "contribution"
        const val intentSqlIdString = "sqlID"

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

        fun retrieveParticipants(context: Context, participantList: ArrayList<String>) : ArrayList<String> {
            /*
            Query the sql DB for the current group to find its participants
             */
            participantList.clear()
            val dbHelper = DbHelper(context)
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
            return participantList
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewReceiptCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sqlAccountId = intent.getStringExtra(intentSqlIdString)
        participantList = ArrayList()
        participantDataEditList = ArrayList()
        participantList = retrieveParticipants(this, participantList)

        val spinnerAdapter = ArrayAdapter(
            this,
            R.layout.support_simple_spinner_dropdown_item, participantList
        )
        binding.paidBySpinner.adapter = spinnerAdapter

        val pagerAdapter = ReceiptPagerAdapter(this)
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
                    val dbHelper = DbHelper(this)
                    val writeableDB = dbHelper.writableDatabase
                    //Obtain global receipt details
                    val date = getDate()
                    val title =  binding.receiptTitleEditText.text.toString()
                    val total : Float
                    val paidBy = binding.paidBySpinner.selectedItem.toString()
                    val receiptFirebaseID: String?
                    val scanned: Boolean

                    //Check where the user is
                    if (currentPage == 0) {
                        //User is saving a manual expense
                        total = findViewById<EditText>(R.id.currencyAmount).text.toString().toFloat()
                        val participantDataList = SplitReceiptManuallyFragment.fragmentManualParticipantList
                        val participantBalDataList: ArrayList<ParticipantBalanceData> = particDataToParticBalData(participantDataList)
                        val contributionsString = createContribString(participantBalDataList, paidBy)
                        Log.i("TEST", contributionsString)

                        if (!isEdit) {
                            // Insert new entry to SQL DB.
                            scanned = false
                            receiptFirebaseID = newFirebaseReceiptID()
                            insertNewReceiptSql(writeableDB, receiptFirebaseID, date, title, total, paidBy, contributionsString, scanned)
                            dbHelper.close()
                            intent.putExtra(CONTRIBUTION_INTENT_DATA, contributionsString)
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                            return true
                        } else {
                            // Update previous entry in SQL DB
                            val sqlRow = updateReceiptSql(writeableDB, date, title, total, paidBy, contributionsString)
                            dbHelper.close()
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
                            receiptFirebaseID = newFirebaseReceiptID()
                            val sqlRow = insertNewReceiptSql(writeableDB, receiptFirebaseID, date, title, total, paidBy, contributionsString, true)
                            insertReceiptItemsSql(writeableDB, itemizedProductList, sqlRow)
                            intent.putExtra(CONTRIBUTION_INTENT_DATA, contributionsString)
                            setResult(Activity.RESULT_OK, intent)
                            dbHelper.close()
                            finish()
                            return true
                        } else {
                            // User is editing a previously saved receipt expense
                            val sqlRow = updateReceiptSql(writeableDB, date, title, total, paidBy, contributionsString)
                            updateItemsSql(writeableDB, itemizedProductList, editSqlRowId)
                            dbHelper.close()
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
                dbHelper.close()
                } else
                {return false}
            }
            else -> return false
        }
        return false
    }

    private fun updateItemsSql(writeableDB: SQLiteDatabase?, itemizedProductList: ArrayList<ScannedItemizedProductData>, editSqlRowId: String) {
        // Updates the products after user has edited and saved scanned receipt
        for (product in itemizedProductList){
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
    }

    private fun insertReceiptItemsSql(writeableDB: SQLiteDatabase, itemizedProductList: ArrayList<ScannedItemizedProductData>, receiptRowSql: Int) {
        // Parses the scanned products out of their data classes and inserts them into SQL DB TABLE ITEMS
        val sqlFK = receiptRowSql.toString()
        for (product in itemizedProductList) {
            val productName = product.itemName
            val productValue = product.itemValue
            val productOwnership = product.ownership
            val values = ContentValues().apply {
                put(ITEMS_COL_NAME, productName)
                put(ITEMS_COL_VALUE, productValue)
                put(ITEMS_COL_OWNERSHIP, productOwnership)
                put(ITEMS_COL_FK_RECEIPT_ID, sqlFK)
            }
            writeableDB.insert(ITEMS_TABLE_NAME, null, values)
        }
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
                val equalSplit: Float = ReceiptOverviewActivity.roundToTwoDecimalPlace(itemValue / numberParticipants)
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
            participant.balance = ReceiptOverviewActivity.roundToTwoDecimalPlace(participant.balance)
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

    private fun insertNewReceiptSql(writeableDB: SQLiteDatabase, recFirebaseId: String, date: String, title: String, total: Float, paidBy: String, contributions: String, scanned: Boolean) : Int{
        val scannedInt: Int = if (scanned) { 1 } else { 0 }
        val values = ContentValues().apply {
            put(RECEIPT_COL_UNIQUE_ID, recFirebaseId)
            put(RECEIPT_COL_DATE, date)
            put(RECEIPT_COL_TITLE, title)
            put(RECEIPT_COL_TOTAL, total)
            put(RECEIPT_COL_PAID_BY, paidBy)
            put(RECEIPT_COL_CONTRIBUTIONS, contributions)
            put(RECEIPT_COL_SCANNED, scannedInt)
            put(RECEIPT_COL_FK_GROUP_ID, sqlAccountId)
        }
        val sqlId = writeableDB.insert(RECEIPT_TABLE_NAME, null, values)
        return sqlId.toInt()
    }

    private fun updateReceiptSql(writeableDB: SQLiteDatabase, date: String, title: String, total: Float, paidBy: String, contributionsString: String): String {
        val values = ContentValues().apply {
            put(RECEIPT_COL_DATE, date)
            put(RECEIPT_COL_TITLE, title)
            put(RECEIPT_COL_TOTAL, total)
            put(RECEIPT_COL_PAID_BY, paidBy)
            put(RECEIPT_COL_CONTRIBUTIONS, contributionsString)
        }
        val whereClause = "$RECEIPT_COL_ID = ?"
        val whereArgs = arrayOf(editSqlRowId)
        return writeableDB.update(RECEIPT_TABLE_NAME, values, whereClause, whereArgs).toString()
    }

    private fun newFirebaseReceiptID() : String{
        val prefix = "rec"
        val sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        /*
        This will need multiple sharedPreferences for each account. When a new account is opened then
        a new shared pref file is created for this account.
        The accounts shared pref name will be stored as a companion object variable. Initialised as
        the current Account Sql ID is initialised.
        When initialized then we will check the most recent receipt number and see if we match this.
        We will repeat this just before inserting a new receipt.
         */

        return "rec0001" // TODO:(LOGIC THOUGHT OUT ALREADY) Find a way to increment this receipt number for the group.
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

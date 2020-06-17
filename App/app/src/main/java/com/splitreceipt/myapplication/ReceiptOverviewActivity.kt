package com.splitreceipt.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_ID
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_SETTLEMENTS
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_TABLE_NAME
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_DATE
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_FK_GROUP_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_ID
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_PAID_BY
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_SCANNED
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_TITLE
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_COL_TOTAL
import com.splitreceipt.myapplication.data.DbManager.ReceiptTable.RECEIPT_TABLE_NAME
import com.splitreceipt.myapplication.data.ReceiptData
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_ACCOUNT_CURRENCY_SYMBOL
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_NAME
import com.splitreceipt.myapplication.databinding.ActivityMainBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList


class ReceiptOverviewActivity : AppCompatActivity(), ReceiptOverViewAdapter.onReceRowClick {
    /*
    Activity shows the interior of a group. Listing all prior expenses and
    offering the user to create a new expense.
     */

    lateinit var binding: ActivityMainBinding
    lateinit var receiptList: ArrayList<ReceiptData>
    private lateinit var storageRef: StorageReference
    private lateinit var adapter: ReceiptOverViewAdapter
    private val seeExpenseResult = 10
    private val addExpenseResult = 20
    private val grouptSettingsResult = 30
    private var userSettlementString = ""

    companion object {
        var getSqlUser: String? = "unknown"
        var getSqlGroupId: String? = "-1"
        var getFirebaseId: String? = "-1"
        var settlementString: String  = ""
        var settlementArray: ArrayList<String> = ArrayList()
        const val balanced_string: String = "balanced"
        const val ImagePathIntent = "path_intent"
        const val UriIntent = "uri_intent"

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

        fun loadImageFromStorage(context: Context, profile:Boolean, fileName: String, extension: String=".jpg") : Bitmap? {
            val directory: File
            return try {
                directory = if (profile) {
                    context.getDir(ASyncSaveImage.profileImageDir, Context.MODE_PRIVATE)
                } else {
                    context.getDir(ASyncSaveImage.scannedImageDir, Context.MODE_PRIVATE)
                }

                val f = File(directory, "$fileName$extension")
                val b = BitmapFactory.decodeStream(FileInputStream(f))
                b
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        receiptList = ArrayList()
        storageRef = FirebaseStorage.getInstance().reference
        getSqlGroupId = intent.getStringExtra(GroupScreenActivity.sqlIntentString)
        getSqlUser = intent.getStringExtra(GroupScreenActivity.userIntentString)
        val getAccountName = intent.getStringExtra(GroupScreenActivity.groupNameIntentString)
        binding.accountNameTitleText.text = getAccountName
        getFirebaseId = intent.getStringExtra(GroupScreenActivity.firebaseIntentString)

        loadPreviousReceipts(getSqlGroupId)
        settlementString = loadSqlSettlementString(getSqlGroupId)
        deconstructAndSetSettlementString(settlementString)

        adapter = ReceiptOverViewAdapter(receiptList, this)
        binding.mainActivityRecycler.layoutManager = LinearLayoutManager(this)
        binding.mainActivityRecycler.adapter = adapter

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Your group"
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_back_arrow_white)
        }

        if (intent.getStringExtra(UriIntent) != null){
            /*
            New Group was just created by user, load the image Uri that has been passed as String as
            the image is likely still being saved in an AsyncTask.
             */
            val uriImage: Uri = Uri.parse(intent.getStringExtra(UriIntent))
            binding.groupProfileImage.setImageURI(uriImage)
        } else {
            //Group has already been created and user is re-entering the group, load internally saved image
            val b = loadImageFromStorage(this, true, getFirebaseId!!)
            binding.groupProfileImage.setImageBitmap(b)
        }

//        val localFile = File.createTempFile("images", "jpg")
//        val userStorageRef = storageRef.child("userID")
//        userStorageRef.getFile(localFile)
//            .addOnSuccessListener {
//                val my_image: Bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
//                binding.groupProfileImage.setImageBitmap(my_image)
//            }.addOnFailureListener {
//                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
//            }
    }


    private fun loadSqlSettlementString(sqlAccountId: String?): String {
        var settlementString = ""
        val dbHelper = DbHelper(this)
        val reader = dbHelper.readableDatabase
        val columns = arrayOf(GROUP_COL_SETTLEMENTS)
        val selectClause = "$GROUP_COL_ID = ?"
        val selectArgs = arrayOf(sqlAccountId)
        val cursor: Cursor = reader.query(GROUP_TABLE_NAME, columns, selectClause, selectArgs,
                        null, null, null)
        val settlementIndex = cursor.getColumnIndexOrThrow(GROUP_COL_SETTLEMENTS)
        while (cursor.moveToNext()) {
            settlementString = cursor.getString(settlementIndex)
        }
        cursor.close()
        dbHelper.close()
        return settlementString
    }

    private fun loadPreviousReceipts(sqlId: String?) {
        val dbHelper = DbHelper(this)
        val reader = dbHelper.readableDatabase
        val columns = arrayOf(RECEIPT_COL_DATE, RECEIPT_COL_TITLE, RECEIPT_COL_TOTAL,
            RECEIPT_COL_PAID_BY, RECEIPT_COL_ID, RECEIPT_COL_SCANNED)
        val selectClause = "$RECEIPT_COL_FK_GROUP_ID = ?"
        val selectArgs = arrayOf("$sqlId")
        val cursor: Cursor = reader.query(RECEIPT_TABLE_NAME, columns, selectClause, selectArgs,
                            null, null, "$RECEIPT_COL_ID DESC"
        ) //TODO: Try to sort all expenses in date order. Maybe do this before passing to the adapter?
        val dateColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_DATE)
        val titleColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_TITLE)
        val totalColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_TOTAL)
        val paidByColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_PAID_BY)
        val idColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_ID)
        val scannedColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_SCANNED)
        while (cursor.moveToNext()) {
            val receiptDate = cursor.getString(dateColIndex)
            val receiptTitle = cursor.getString(titleColIndex)
            val receiptTotal = cursor.getFloat(totalColIndex)
            val receiptPaidBy = cursor.getString(paidByColIndex)
            val receiptSqlId =  cursor.getInt(idColIndex).toString()
            val receiptScannedInt = cursor.getInt(scannedColIndex)
            val scanned = receiptScannedInt == 1
            receiptList.add(ReceiptData(receiptDate, receiptTitle, receiptTotal, receiptPaidBy, receiptSqlId, scanned))
        }
        cursor.close()
        dbHelper.close()
    }

    fun addNewReceiptButton(view: View) {
        val intent = Intent(this, NewReceiptCreationActivity::class.java)
        intent.putExtra(NewReceiptCreationActivity.intentSqlIdString, getSqlGroupId)
        startActivityForResult(intent, addExpenseResult)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val newSettlementString: String

        if (requestCode == addExpenseResult){
            if (resultCode == Activity.RESULT_OK){
                val contributions = data?.getStringExtra(NewReceiptCreationActivity.
                                                            CONTRIBUTION_INTENT_DATA)
                Log.i("Algorithm", "Contribution string returned from the recent transaction: $contributions \n\n")
                newSettlementString = newContributionUpdates(contributions!!)
                deconstructAndSetSettlementString(newSettlementString)
            }
        }
        else if (requestCode == seeExpenseResult){
            if (resultCode == Activity.RESULT_OK) {

                if (data?.getStringExtra(ExpenseViewActivity
                        .expenseReturnNewSettlements) == null){
                    // This scenario means the user deleted the expense and re-balancing must be done
                    val reversedContributions = data?.getStringExtra(ExpenseViewActivity
                        .expenseReturnNewContributions)
                    Log.i("Algorithm", "Reversed contribution string returned from the " +
                                                    "recent deletion: $reversedContributions")
                    newSettlementString = newContributionUpdates(reversedContributions!!)
                }
                else {
                    // This scenario means the user edited the expense and re-balancing is already completed
                    newSettlementString = data.getStringExtra(ExpenseViewActivity.
                                                        expenseReturnNewSettlements)!!.toString()
                    reloadRecycler()
                }
                deconstructAndSetSettlementString(newSettlementString)

            }
        }
        else if (requestCode == grouptSettingsResult) {
            if (resultCode == Activity.RESULT_OK) {
                //TODO: Create functionality to handle group settings changes.
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
            val sharedPref = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            val currencySymbol = sharedPref.getString(SHARED_PREF_ACCOUNT_CURRENCY_SYMBOL, "$")
            val splitIndividual = settlementString.split("/")
            for (settlement in splitIndividual) {
                val splitSettlement = settlement.split(",")
                val debtor = changeNameToYou(splitSettlement[0], true)
                val value = splitSettlement[1]
                val receiver = changeNameToYou(splitSettlement[2], false)
                val finalSettlementString = createSettlementString(debtor, value, receiver, currencySymbol!!)
                settlementArray.add(finalSettlementString)
                if ("you" in finalSettlementString.toLowerCase(Locale.ROOT)) {
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

    private fun createSettlementString(debtor: String, value: String, receiver: String, currencySymbol: String): String {
        val finalString: String
        val you = "you"
        val debtorLow = debtor.toLowerCase(Locale.ROOT)
        val receiverLow = receiver.toLowerCase(Locale.ROOT)
        finalString = if (you == debtorLow) {
            "$debtor owe $currencySymbol$value to $receiver."
        } else if (you == receiverLow){
            "$debtor owes $currencySymbol$value to $receiverLow."
        } else {
            "$debtor owes $currencySymbol$value to $receiver."
        }
        return finalString
    }

    private fun newContributionUpdates(newContributions: String) : String {
        val balanceSettlementHelper = BalanceSettlementHelper(this, getSqlGroupId.toString())
        val settlementString = balanceSettlementHelper.recalculateBalancesAndSettlements(newContributions)
        reloadRecycler()
        return settlementString
    }

    private fun reloadRecycler(){
        // Clears the list and refreshes receipts from SQL db back into it.
        receiptList.clear()
        loadPreviousReceipts(getSqlGroupId)
        adapter.notifyDataSetChanged()
    }

    fun balancesButtonPressed(view: View) {
        val intent = Intent(this, BalanceOverviewActivity::class.java)
        startActivity(intent)
    }

    override fun onRowClick(
        pos: Int,
        title: String,
        total: String,
        sqlID: String,
        paidBy: String,
        scanned: Boolean
    ) {
        val intent = Intent(this, ExpenseViewActivity::class.java)
        intent.putExtra(ExpenseViewActivity.expenseTitleIntentString, title)
        intent.putExtra(ExpenseViewActivity.expenseTotalIntentString, total)
        intent.putExtra(ExpenseViewActivity.expenseSqlIntentString, sqlID)
        intent.putExtra(ExpenseViewActivity.expensePaidByIntentString, paidBy)
        intent.putExtra(ExpenseViewActivity.expenseScannedIntentString, scanned)
        startActivityForResult(intent, seeExpenseResult)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.group_options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.groupSettings -> {
                val intent = Intent(this, GroupSettingsActivity::class.java)
                startActivityForResult(intent, grouptSettingsResult)
                return true
            }
            R.id.groupBalances -> {
                val intent = Intent(this, BalanceOverviewActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.export -> {
                //TODO: Create export functionality.
                return true
            }
            else -> return false
        }
    }

}

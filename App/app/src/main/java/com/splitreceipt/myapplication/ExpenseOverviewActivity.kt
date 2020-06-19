package com.splitreceipt.myapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.splitreceipt.myapplication.data.*
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


class ExpenseOverviewActivity : AppCompatActivity(), ExpenseOverViewAdapter.onReceRowClick {
    /*
    Activity shows the interior of a group. Listing all prior expenses and
    offering the user to create a new expense.
     */

    lateinit var binding: ActivityMainBinding
    lateinit var receiptList: ArrayList<ReceiptData>
    private lateinit var storageRef: StorageReference
    private lateinit var adapter: ExpenseOverViewAdapter
    private val seeExpenseResult = 10
    private val addExpenseResult = 20
    private val grouptSettingsResult = 30
    private var userSettlementString = ""

    companion object {
        var getSqlUser: String? = "unknown"
        var getSqlGroupId: String? = "-1"
        var getFirebaseId: String? = "-1"
        var settlementArray: ArrayList<String> = ArrayList()
        const val balanced_string: String = "balanced"
        const val ImagePathIntent = "path_intent"
        const val UriIntent = "uri_intent"

        @SuppressLint("DefaultLocale")
        fun changeNameToYou(participantName: String, capitalize: Boolean): String {
            return if (participantName == getSqlUser) {
                if (capitalize) {
                    "You"
                } else {
                    "you"
                }
            } else {
                if (capitalize) {
                    participantName.capitalize()
                } else {
                    participantName
                }
            }
        }

        fun roundToTwoDecimalPlace(number: Float): Float {
            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.FLOOR
            return df.format(number).toFloat()
        }

        fun errorRate(balance: Float): Float {
            if (balance in -0.04..0.04) {
                // £0.04/$0.04 error rate allowed. Likely only to ever reach 0.02 error rate.
                return 0.0F
            }
            return balance
        }

        fun loadImageFromStorage(
            context: Context,
            profile: Boolean,
            fileName: String,
            extension: String = ".jpg"
        ): Bitmap? {
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
        val sqlHelper = SqlDbHelper(this)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Your group"
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_back_arrow_white)
        }

        if (intent.getStringExtra(UriIntent) != null) {
            // New Group was just created by user, load the image Uri as image is likely still being saved in an AsyncTask.
            val uriImage: Uri = Uri.parse(intent.getStringExtra(UriIntent))
            binding.groupProfileImage.setImageURI(uriImage)
        } else {
            // Group has already been created and user is re-entering the group, load internally saved image
            val b = loadImageFromStorage(this, true, getFirebaseId!!)
            binding.groupProfileImage.setImageBitmap(b)
        }

        val firebaseDbHelper = FirebaseDbHelper(getFirebaseId!!)
        val accountInfoDbRef = firebaseDbHelper.getAccountInfoListeningRef()
        accountInfoDbRef.addValueEventListener(object : ValueEventListener {
            //Listens for changes to the account information
            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(baseContext, "Failed to sync changes", Toast.LENGTH_SHORT).show()
            }
            override fun onDataChange(data: DataSnapshot) {
                val account = data.getValue(FirebaseAccountInfoData::class.java)!!
                Log.i("Fbase", "name: ${account.accName}")
                Log.i("Fbase", "bal: ${account.accParticipants}")
                Log.i("Fbase", "--------------")
            }
        })

        val expenseInfoDbRef = firebaseDbHelper.getExpensesListeningRef()
        expenseInfoDbRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(baseContext, "Failed to sync changes", Toast.LENGTH_SHORT).show()
            }
            override fun onDataChange(data: DataSnapshot) {
                /*
                 Check if any of the expenses in Firebase are not in the users SQL db.
                 If they are not then we will recalculate the expenses.
                 */
                val sqlFirebaseIds = SqlDbHelper(baseContext).showAllFirebaseIds(getSqlGroupId!!)
                var settlementString: String? = null
                val balanceSettlementHelper = BalanceSettlementHelper(applicationContext, getSqlGroupId!!)
                for (expense in data.children){
                    val firebaseID = expense.key
                    var exists = false

                    for (firebaseSQLId in sqlFirebaseIds) {
                        if (firebaseID == firebaseSQLId){
                            // Receipt IS in the users SQL db
                            Log.i("Fbase-E", "Receipt $firebaseID IS in Sql db")
                            exists = true
                            break
                        }
                    }

                    if (!exists) {
                        // Receipt is NOT in the users SQL db
                        Log.i("Fbase-E", "Receipt $firebaseID is NOT in Sql db")

                        val expen = data.child(firebaseID!!)
                        val newExpense = expen.getValue(FirebaseExpenseData::class.java)!!
                        val date = newExpense.expenseDate
                        val title = newExpense.expenseTitle
                        val total = newExpense.expenseTotal
                        val paidBy = newExpense.expensePaidBy
                        val contribs = newExpense.expenseContribs
                        val sqlDbHelper = SqlDbHelper(baseContext)
                        //Save expense into SQL
                        sqlDbHelper.insertNewExpense(getSqlGroupId!!, firebaseID, date, title, total, paidBy, contribs, false) //TODO: Scanned is set to false as default, this will need editing at a later stage to accommodate for scanned receipts.
                        //Run the new contributions through the algorithm
                        settlementString = balanceSettlementHelper.balanceAndSettlementsFromSql(contribs)
                    }
                }

                if (settlementString != null) {
                    // User has updated new expenses from Firebase so update Firebase with new balance, settlement strings.
                    balanceSettlementHelper.updateFirebaseBalAndSettle(firebaseDbHelper)
                } else {
                    // User has no new updated expenses downloaded from the firebase database
                    settlementString = sqlHelper.loadSqlSettlementString(getSqlGroupId)
                }
                deconstructAndSetSettlementString(settlementString)
            }
        })
        sqlHelper.loadPreviousReceipts(getSqlGroupId, receiptList)
        adapter = ExpenseOverViewAdapter(receiptList, this)
        binding.mainActivityRecycler.layoutManager = LinearLayoutManager(this)
        binding.mainActivityRecycler.adapter = adapter
    }

    fun addNewReceiptButton(view: View) {
        val intent = Intent(this, NewExpenseCreationActivity::class.java)
        intent.putExtra(NewExpenseCreationActivity.intentSqlIdString, getSqlGroupId)
        intent.putExtra(NewExpenseCreationActivity.intentFirebaseIdString, getFirebaseId)
        startActivityForResult(intent, addExpenseResult)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val newSettlementString: String

        if (requestCode == addExpenseResult) {
            if (resultCode == Activity.RESULT_OK) {
                val contributions = data?.getStringExtra(
                    NewExpenseCreationActivity.CONTRIBUTION_INTENT_DATA
                )
                Log.i(
                    "Algorithm",
                    "Contribution string returned from the recent transaction: $contributions \n\n"
                )
                newSettlementString = newContributionUpdates(contributions!!)
                deconstructAndSetSettlementString(newSettlementString)
                reloadRecycler()
            }
        } else if (requestCode == seeExpenseResult) {
            if (resultCode == Activity.RESULT_OK) {

                if (data?.getStringExtra(
                        ExpenseViewActivity
                            .expenseReturnNewSettlements
                    ) == null
                ) {
                    // This scenario means the user deleted the expense and re-balancing must be done
                    val reversedContributions = data?.getStringExtra(
                        ExpenseViewActivity
                            .expenseReturnNewContributions
                    )
                    Log.i(
                        "Algorithm", "Reversed contribution string returned from the " +
                                "recent deletion: $reversedContributions"
                    )
                    newSettlementString = newContributionUpdates(reversedContributions!!)
                } else {
                    // This scenario means the user edited the expense and re-balancing is already completed
                    newSettlementString = data.getStringExtra(
                        ExpenseViewActivity.expenseReturnNewSettlements
                    )!!.toString()
                    reloadRecycler()
                }
                deconstructAndSetSettlementString(newSettlementString)

            }
        } else if (requestCode == grouptSettingsResult) {
            if (resultCode == Activity.RESULT_OK) {
                //TODO: Create functionality to handle group settings changes.
            }
        }
    }

    private fun deconstructAndSetSettlementString(settlementString: String) {
        /*
         This function will deconstruct a settlementString and produce an ArrayList of individual settlement strings.
         After this it will identify any strings relevant to the current user and add them to a separate list which will be showcased in UI
         */
        settlementArray.clear()
        val sb: StringBuilder = java.lang.StringBuilder()
        val userDirectedSettlementIndexes: ArrayList<Int> = ArrayList()
        var indexCount = 0
        if (settlementString == balanced_string) {
            settlementArray.add(getString(R.string.balanced))
            userDirectedSettlementIndexes.add(indexCount)
        } else {
            val sharedPref = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            val currencySymbol = sharedPref.getString(SHARED_PREF_ACCOUNT_CURRENCY_SYMBOL, "$")
            val splitIndividual = settlementString.split("/")
            for (settlement in splitIndividual) {
                val splitSettlement = settlement.split(",")
                val debtor = changeNameToYou(splitSettlement[0], true)
                val value = splitSettlement[1]
                val receiver = changeNameToYou(splitSettlement[2], false)
                val finalSettlementString =
                    createSettlementString(debtor, value, receiver, currencySymbol!!)
                settlementArray.add(finalSettlementString)
                if ("you" in finalSettlementString.toLowerCase(Locale.ROOT)) {
                    userDirectedSettlementIndexes.add(indexCount)
                }
                indexCount++
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

    private fun createSettlementString(
        debtor: String,
        value: String,
        receiver: String,
        currencySymbol: String
    ): String {
        val finalString: String
        val you = "you"
        val debtorLow = debtor.toLowerCase(Locale.ROOT)
        val receiverLow = receiver.toLowerCase(Locale.ROOT)
        val fixedVal = SplitExpenseManuallyFragment.addStringZerosForDecimalPlace(value)
        finalString = if (you == debtorLow) {
            "$debtor owe $currencySymbol$fixedVal to $receiver."
        } else if (you == receiverLow) {
            "$debtor owes $currencySymbol$fixedVal to $receiverLow."
        } else {
            "$debtor owes $currencySymbol$fixedVal to $receiver."
        }
        return finalString
    }

    private fun newContributionUpdates(newContributions: String): String {
        val balanceSettlementHelper = BalanceSettlementHelper(this, getSqlGroupId.toString())
        return balanceSettlementHelper.balanceAndSettlementsFromSql(newContributions)
    }

    private fun reloadRecycler() {
        // Clears the list and refreshes receipts from SQL db back into it.
        receiptList.clear()
        SqlDbHelper(this).loadPreviousReceipts(getSqlGroupId, receiptList)
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

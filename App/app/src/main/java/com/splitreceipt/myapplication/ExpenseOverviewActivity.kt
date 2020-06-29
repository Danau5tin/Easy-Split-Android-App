package com.splitreceipt.myapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.splitreceipt.myapplication.data.*
import com.splitreceipt.myapplication.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.alert_dialog_share_group.*
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList


class ExpenseOverviewActivity : AppCompatActivity(), ExpenseOverViewAdapter.OnReceRowClick {
    /*
    Activity shows the interior of a group. Listing all prior expenses and
    offering the user to create a new expense.
     */

    private lateinit var binding: ActivityMainBinding
    private lateinit var receiptList: ArrayList<ReceiptData>
    private lateinit var storageRef: StorageReference
    private lateinit var adapter: ExpenseOverViewAdapter

    private val seeExpenseResult = 1
    private val addExpenseResult = 2
    private val seeBalancesResult = 6
    private val settingsResult = 3
    private val requestStorage = 4
    private val pickImage = 5
    private var userSettlementString = ""

    companion object {
        var firebaseDbHelper: FirebaseDbHelper? = null // Globally used throughout the application

        var getSqlUser: String? = "unknown"
        var getSqlGroupId: String? = "-1"
        var getFirebaseId: String? = "-1"
        var getGroupName: String? = "?"
        var groupBaseCurrency: String? = ""
        lateinit var currencySymbol: String
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

        fun roundToTwoDecimalPlace(number: Float, ceiling: Boolean=true): Float {
            val df = DecimalFormat("#.##")
            if (ceiling) {
                df.roundingMode = RoundingMode.CEILING
            } else {
                df.roundingMode = RoundingMode.FLOOR
            }
            return df.format(number).toFloat()
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
        getGroupName = intent.getStringExtra(GroupScreenActivity.groupNameIntentString)
        binding.groupNameTitleText.text = getGroupName
        getFirebaseId = intent.getStringExtra(GroupScreenActivity.firebaseIntentString)
        groupBaseCurrency = intent.getStringExtra(GroupScreenActivity.groupBaseCurrencyIntent)!!
        currencySymbol = intent.getStringExtra(GroupScreenActivity.groupBaseCurrencyUiSymbolIntent)!!

        firebaseDbHelper = null
        firebaseDbHelper = if (NewGroupCreation.firebaseDbHelper != null) {
            NewGroupCreation.firebaseDbHelper!!
        } else if (GroupScreenActivity.firebaseDbHelper != null){
            GroupScreenActivity.firebaseDbHelper!!
        }
        else {
            FirebaseDbHelper(getFirebaseId!!)
        }

        val sqlHelper = SqlDbHelper(this)
        ASyncCurrencyDownload(sqlHelper).execute(groupBaseCurrency) // Checks if we need to update the latest currency conversions.

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = ""
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_back_arrow_white)
        }

        if (intent.getBooleanExtra(NewGroupCreation.newGroupCreatedIntent, false)){
            Log.i("ExpenseOverview", "Group entered was just created by user")
            showInviteDialog()
        }

        if (intent.getStringExtra(UriIntent) != null) {
            // New Group was just created by user, load the image Uri as image is likely still being saved in an AsyncTask.
            val uriImage: Uri = Uri.parse(intent.getStringExtra(UriIntent))
            binding.groupProfileImage.setImageURI(uriImage)
        } else {
            // Group has already been created and user is re-entering or joining the group, load internally saved image.
            Log.i("ExpenseOverview", "Group entered was just re-entered or joined")
            val b = loadImageFromStorage(this, true, getFirebaseId!!)
            binding.groupProfileImage.setImageBitmap(b)
        }

        binding.groupProfileImage.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), requestStorage)
            } else {
                openGallery()
            }
        }

        val accountInfoDbRef = firebaseDbHelper!!.getAccountInfoListeningRef()
        accountInfoDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            //Listens for changes to the account information
            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(baseContext, "Failed to sync changes", Toast.LENGTH_SHORT).show()
            }
            override fun onDataChange(data: DataSnapshot) {
                val firebaseGroupData = data.getValue(FirebaseAccountInfoData::class.java)!!
                val sqlGroupData = sqlHelper.retrieveSqlAccountInfoData(getSqlGroupId!!)
                var infoChanged = false
                var imageChanged = false
                if (firebaseGroupData.accName != sqlGroupData.accName) {
                    binding.groupNameTitleText.text = firebaseGroupData.accName
                    infoChanged = true
                }
                if (firebaseGroupData.accParticipants != sqlGroupData.accParticipants){
                    infoChanged = true
                }
                if (firebaseGroupData.accLastImage != sqlGroupData.accLastImage) {
                    imageChanged = true
                    infoChanged = true
                }
                if (infoChanged) {
                    sqlHelper.updateGroupInfo(firebaseGroupData, getSqlGroupId!!)
                }
                if (imageChanged) {
                    firebaseDbHelper!!.downloadGroupProfileImage(baseContext, binding.groupProfileImage)
                }
            }
        })


        adapter = ExpenseOverViewAdapter(receiptList, this)
        binding.mainActivityRecycler.layoutManager = LinearLayoutManager(this)
        binding.mainActivityRecycler.adapter = adapter

        val expenseInfoDbRef = firebaseDbHelper!!.getExpensesListeningRef()
        expenseInfoDbRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(baseContext, "Failed to sync changes", Toast.LENGTH_SHORT).show()
            }
            override fun onDataChange(data: DataSnapshot) {
                /*
                 Check if any of the expenses in Firebase are not in the users SQL db.
                 If they are not then we will recalculate the expenses.
                 */
                val sqlDbHelper = SqlDbHelper(baseContext)
                val sqlExpenses = sqlDbHelper.retrieveBasicExpenseSqlData(getSqlGroupId!!)
                var settlementString: String? = null
                val balanceSettlementHelper = BalanceSettlementHelper(applicationContext, getSqlGroupId!!)
                for (expense in data.children){
                    val firebaseID = expense.key
                    var exists = false
                    val expen = data.child(firebaseID!!)
                    val newExpense = expen.getValue(FirebaseExpenseData::class.java)!!
                    val lastEdit = newExpense.expLastEdit
                    val date = newExpense.expDate
                    val title = newExpense.expTitle
                    val total = newExpense.expTotal
                    val paidBy = newExpense.expPaidBy
                    val contribs = newExpense.expContribs
                    val scanned = newExpense.expScanned

                    var changesMade = false

                    for (sqlExpense in sqlExpenses) {
                        if (firebaseID == sqlExpense.firebaseId){
                            // Receipt IS in the users SQL db
                            Log.i("Fbase-E", "Receipt $firebaseID IS in Sql db")
                            if (lastEdit != sqlExpense.lastEdit) {
                                //The expense has been updated since the user has last used the app. Update changes locally.
                                changesMade = true
                                sqlDbHelper.updateExpense(sqlExpense.sqlRowId, date, title, total, paidBy, contribs, lastEdit)
                            }
                            exists = true
                            break
                        }
                    }
                    if (!exists) {
                        // Receipt is NOT in the users SQL db
                        Log.i("Fbase-E", "Receipt $firebaseID is NOT in Sql db")
                        changesMade = true
                        val currency = newExpense.expCurrency
                        val exchangeRate = newExpense.expExchRate
                        //Save expense into SQL
                        val expenseCurrencySymbol = CurrencyHelper.returnUiSymbol(currency)
                        val expenseSqlRow = sqlDbHelper.insertNewExpense(getSqlGroupId!!, firebaseID,
                            date, title, total, paidBy, contribs, scanned, lastEdit, currency, expenseCurrencySymbol, exchangeRate)
                        if (scanned){
                            // If the expense is a scanned receipt then download and add this receipt to sql
                            val scannedRef = firebaseDbHelper!!.getScannedListeningRef(firebaseID)
                            scannedRef.addListenerForSingleValueEvent(object : ValueEventListener{
                                override fun onCancelled(p0: DatabaseError) {}
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for (product in snapshot.children) {
                                        val productData = product.getValue(FirebaseProductData::class.java)!!
                                        sqlDbHelper.insertReceiptItems(productData.productName, productData.productValue, productData.productOwner, expenseSqlRow)
                                    }
                                }
                            })
                        }
                    }
                    if (changesMade) {
                        //Run the new contributions through the algorithm
                        settlementString = balanceSettlementHelper.balanceAndSettlementsFromSql(contribs)
                    }
                }

                if (settlementString != null) {
                    // User has updated expenses from Firebase so update Firebase with new balance, settlement strings.
                    balanceSettlementHelper.updateFirebaseBalAndSettle(firebaseDbHelper!!)
                } else {
                    // User has no new updated expenses downloaded from the firebase database
                    settlementString = sqlHelper.loadSqlSettlementString(getSqlGroupId)
                }
                sqlHelper.loadPreviousReceipts(getSqlGroupId, receiptList)
                deconstructAndSetSettlementString(settlementString)
                adapter.notifyDataSetChanged()
            }
        })

    }

    @SuppressLint("InflateParams")
    private fun showInviteDialog() {
        val diagView = LayoutInflater.from(this).inflate(R.layout.alert_dialog_share_group, null)
        val builder = AlertDialog.Builder(this).setTitle("Share")
            .setView(diagView).show()
        val shareGroupHelper = ShareGroupHelper(this, getFirebaseId!!)
        builder.copyLinkButton2.setOnClickListener {
            shareGroupHelper.clipboardShareCopy()
        }
        builder.whatsappShareButton2.setOnClickListener {
            shareGroupHelper.shareViaWhatsapp()
        }
        builder.shareEmailButton2.setOnClickListener {
            shareGroupHelper.shareViaEmail()
        }
        builder.shareContinue.setOnClickListener {
            builder.dismiss()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, pickImage)
    }

    fun addNewReceiptButton(view: View) {
        val intent = Intent(this, NewExpenseCreationActivity::class.java)
        intent.putExtra(NewExpenseCreationActivity.intentSqlGroupIdString, getSqlGroupId)
        intent.putExtra(NewExpenseCreationActivity.intentFirebaseIdString, getFirebaseId)
        startActivityForResult(intent, addExpenseResult)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var newSettlementString: String?

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

                if (data?.getStringExtra(ExpenseViewActivity.expenseReturnNewSettlements) == null) {
                    // This scenario means the user deleted the expense and re-balancing must be done
                    val reversedContributions = data?.getStringExtra(ExpenseViewActivity.expenseReturnNewContributions
                    )
                    Log.i(
                        "Algorithm", "Reversed contribution string returned from the " +
                                "recent deletion: $reversedContributions"
                    )
                    newSettlementString = newContributionUpdates(reversedContributions!!)
                } else {
                    // This scenario means the user edited the expense and re-balancing is already completed
                    newSettlementString = data.getStringExtra(ExpenseViewActivity.expenseReturnNewSettlements)
                    if (newSettlementString == null){
                        // This means the user did not change any contributions. Therefore retrieve the old string
                        newSettlementString = SqlDbHelper(this).getSettlementString(getSqlGroupId)
                    }
                    reloadRecycler()
                }
                deconstructAndSetSettlementString(newSettlementString!!)

            }
        } else if (requestCode == settingsResult) {
            if (resultCode == Activity.RESULT_OK) {
                val groupName = data?.getStringExtra(GroupSettingsActivity.groupNameReturnIntent)
                binding.groupNameTitleText.text = groupName
                val uriString: String? = data?.getStringExtra(GroupSettingsActivity.groupImageChangedUriIntent)!!
                if (uriString != null) {
                    val uri = Uri.parse(uriString)
                    binding.groupProfileImage.setImageURI(uri)
                }
            }
        } else if (requestCode == pickImage) {
            if (resultCode == Activity.RESULT_OK) {
                val uri: Uri? = data!!.data
                GroupSettingsActivity.handleNewImage(this, uri!!, binding.groupProfileImage)
            }
        } else if (requestCode == seeBalancesResult){
            if (resultCode == Activity.RESULT_OK){
                val settle = data?.getBooleanExtra(BalanceOverviewActivity.balanceResult, false)
                if (settle!!) {
                    val intent = Intent(this, SettleGroupActivity::class.java)
                    startActivityForResult(intent, addExpenseResult)
                }
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
            val splitIndividual = settlementString.split("/")
            for (settlement in splitIndividual) {
                val splitSettlement = settlement.split(",")
                val debtor = changeNameToYou(splitSettlement[0], true)
                val value = splitSettlement[1]
                val receiver = changeNameToYou(splitSettlement[2], false)
                val finalSettlementString =
                    createSettlementString(debtor, value, receiver, currencySymbol)
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
        var fixedVal = roundToTwoDecimalPlace(value.toFloat(), false).toString()
        fixedVal = SplitExpenseManuallyFragment.addStringZerosForDecimalPlace(fixedVal)

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
        val settlementString = balanceSettlementHelper.balanceAndSettlementsFromSql(newContributions)
        firebaseDbHelper!!.setGroupFinance(settlementString, balanceSettlementHelper.balanceString!!)
        return settlementString
    }

    private fun reloadRecycler() {
        // Clears the list and refreshes receipts from SQL db back into it.
        receiptList.clear()
        SqlDbHelper(this).loadPreviousReceipts(getSqlGroupId, receiptList)
        adapter.notifyDataSetChanged()
    }

    fun balancesButtonPressed(view: View) {
        val intent = Intent(this, BalanceOverviewActivity::class.java)
        startActivityForResult(intent, seeBalancesResult)
    }

    override fun onRowClick(
        pos: Int,
        title: String,
        total: String,
        sqlID: String,
        paidBy: String,
        uiSymbol: String,
        currencyCode: String,
        scanned: Boolean
    ) {
        val intent = Intent(this, ExpenseViewActivity::class.java)
        intent.putExtra(ExpenseViewActivity.expenseTitleIntentString, title)
        intent.putExtra(ExpenseViewActivity.expenseTotalIntentString, total)
        intent.putExtra(ExpenseViewActivity.expenseSqlIntentString, sqlID)
        intent.putExtra(ExpenseViewActivity.expensePaidByIntentString, paidBy)
        intent.putExtra(ExpenseViewActivity.expenseCurrencyUiSymbolIntentString, uiSymbol)
        intent.putExtra(ExpenseViewActivity.expenseCurrencyCodeIntentString, currencyCode)
        intent.putExtra(ExpenseViewActivity.expenseScannedIntentString, scanned)
        startActivityForResult(intent, seeExpenseResult)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            requestStorage -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "Permission required to add photo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.group_options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.groupSettleUp -> {
                val intent = Intent(this, SettleGroupActivity::class.java)
                startActivityForResult(intent, addExpenseResult)
                return true
            }
            R.id.groupAddParticipant -> {
                val intent = Intent(this, NewParticipantInviteActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.groupSettings -> {
                val intent = Intent(this, GroupSettingsActivity::class.java)
                intent.putExtra(GroupSettingsActivity.groupNameIntent, getGroupName)
                intent.putExtra(GroupSettingsActivity.groupSqlIdIntent, getSqlGroupId)
                startActivityForResult(intent, settingsResult)
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

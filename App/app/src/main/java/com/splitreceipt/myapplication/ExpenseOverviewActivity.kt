package com.splitreceipt.myapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.splitreceipt.myapplication.a_sync_classes.ASyncCurrencyDownload
import com.splitreceipt.myapplication.adapters.ExpenseOverViewAdapter
import com.splitreceipt.myapplication.data.*
import com.splitreceipt.myapplication.databinding.ActivityMainBinding
import com.splitreceipt.myapplication.helper_classes.*
import com.splitreceipt.myapplication.helper_classes.LocalStorageHelper.loadImageFromStorage
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.collections.ArrayList


class ExpenseOverviewActivity : AppCompatActivity(), ExpenseOverViewAdapter.OnReceRowClick {

    private lateinit var binding: ActivityMainBinding
    private lateinit var expenseList: ArrayList<ExpenseData>
    private lateinit var storageRef: StorageReference
    private lateinit var adapter: ExpenseOverViewAdapter

    private val VIEW_EXPENSE_RESULT = 1
    private val ADD_EXPENSE_RESULT = 2
    private val BALANCES_RESULT = 6
    private val SETTINGS_RESULT = 3
    private val requestStorage = 4
    private val PICK_IMAGE_RESULT = 5
    private var floatingButtonsShowing: Boolean = false

    companion object {
        var firebaseDbHelper: FirebaseDbHelper? = null
        lateinit var refreshHelper: ExpenseOverViewRefreshHelper

        var currentSqlUser: String? = "unknown"
        var currentSqlGroupId: String? = "-1"
        var currentGroupFirebaseId: String? = "-1"
        var currentGroupName: String? = "?"
        var currentGroupBaseCurrency: String? = ""
        lateinit var currencySymbol: String
        var settlementArray: ArrayList<String> = ArrayList()
        const val balanced_string: String = "balanced"
        const val ImagePathIntent = "path_intent"
        const val UriIntent = "uri_intent"

        fun roundToTwoDecimalPlace(number: Float, ceiling: Boolean=true): Float {
            val df = DecimalFormat("#.##")
            if (ceiling) {
                df.roundingMode = RoundingMode.CEILING
            } else {
                df.roundingMode = RoundingMode.FLOOR
            }
            return df.format(number).toFloat()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        expenseList = ArrayList()
        storageRef = FirebaseStorage.getInstance().reference
        getCurrentGroupInfoFromIntents()
        binding.groupNameTitleText.text = currentGroupName
        firebaseDbHelper = FirebaseDbHelper(currentGroupFirebaseId!!)
        val sqlHelper = SqlDbHelper(this)
        ASyncCurrencyDownload(sqlHelper).execute(currentGroupBaseCurrency)


        setUpActionBar()
        setGroupImageListener()

        if (groupJustCreatedByUser()){
            ShareGroupHelper(this, currentGroupFirebaseId!!)
            val uriImage: Uri? = Uri.parse(intent.getStringExtra(UriIntent))
            if (uriImage == null) {
                //TODO: refactor this 5 lines of code as their is duplication
                val b = loadImageFromStorage(this, true, currentGroupFirebaseId!!)
                binding.groupProfileImage.setImageBitmap(b)
            } else {
                binding.groupProfileImage.setImageURI(uriImage)
            }
        } else {
            val b = loadImageFromStorage(this, true, currentGroupFirebaseId!!)
            binding.groupProfileImage.setImageBitmap(b)
        }
        adapter = ExpenseOverViewAdapter(expenseList, this)
        binding.mainActivityRecycler.layoutManager = LinearLayoutManager(this)
        binding.mainActivityRecycler.adapter = adapter

        refreshHelper = ExpenseOverViewRefreshHelper(this, expenseList, adapter,
            binding.settlementStringTextView, binding.seeBalancesButton,
            binding.totalNumberExpensesText, binding.totalAmountExpensesText)

        FirebaseSyncHelper(this, sqlHelper, currentSqlGroupId!!, firebaseDbHelper!!,
            binding.groupNameTitleText, binding.groupProfileImage)
            .syncGroupWithFirebaseDB()
    }

    private fun setGroupImageListener() {
        binding.groupProfileImage.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission
                            .WRITE_EXTERNAL_STORAGE
                    ), requestStorage
                )
            } else {
                openGallery()
            }
        }
    }

    private fun groupJustCreatedByUser() =
        intent.getBooleanExtra(NewGroupCreationActivity.newGroupCreatedIntent, false)

    private fun setUpActionBar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = ""
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_back_arrow_white)
        }
    }

    private fun getCurrentGroupInfoFromIntents() {
        currentSqlGroupId = intent.getStringExtra(GroupScreenActivity.sqlIntentString)
        currentSqlUser = intent.getStringExtra(GroupScreenActivity.userIntentString)
        currentGroupName = intent.getStringExtra(GroupScreenActivity.groupNameIntentString)
        currentGroupFirebaseId = intent.getStringExtra(GroupScreenActivity.firebaseIntentString)
        currentGroupBaseCurrency = intent.getStringExtra(GroupScreenActivity.groupBaseCurrencyIntent)!!
        currencySymbol =
            intent.getStringExtra(GroupScreenActivity.groupBaseCurrencyUiSymbolIntent)!!
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_RESULT)
    }

    fun addNewReceiptButton(view: View?=null) {
        if (!floatingButtonsShowing) {
            binding.floatingActionButtonManual.visibility = View.VISIBLE
            binding.floatingActionButtonScan.visibility = View.VISIBLE
            binding.manualHintText.visibility = View.VISIBLE
            binding.scanHintText.visibility = View.VISIBLE
            binding.mainActivityRecycler.alpha = 0.5F
            floatingButtonsShowing = true
        } else {
            binding.floatingActionButtonManual.visibility = View.INVISIBLE
            binding.floatingActionButtonScan.visibility = View.INVISIBLE
            binding.manualHintText.visibility = View.INVISIBLE
            binding.scanHintText.visibility = View.INVISIBLE
            binding.mainActivityRecycler.alpha = 1.0F
            floatingButtonsShowing = false
        }
    }

    fun startManualExpense(view: View) {
        val intent = Intent(this, NewExpenseCreationActivity::class.java)
        intent.putExtra(NewExpenseCreationActivity.intentSqlGroupIdString, currentSqlGroupId)
        intent.putExtra(NewExpenseCreationActivity.intentFirebaseIdString, currentGroupFirebaseId)
        startActivityForResult(intent, ADD_EXPENSE_RESULT)
    }

    fun startScanExpense(view: View) {
        val intent = Intent(this, NewExpenseCreationActivity::class.java)
        intent.putExtra(NewExpenseCreationActivity.intentSqlGroupIdString, currentSqlGroupId)
        intent.putExtra(NewExpenseCreationActivity.intentFirebaseIdString, currentGroupFirebaseId)
        intent.putExtra(NewExpenseCreationActivity.intentManualOrScan, true)
        startActivityForResult(intent, ADD_EXPENSE_RESULT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var newSettlementString: String?
        if (floatingButtonsShowing) {
            addNewReceiptButton()
        }
        if (requestCode == ADD_EXPENSE_RESULT) {
            if (resultCode == Activity.RESULT_OK) {
                val contributions = data?.getStringExtra(NewExpenseCreationActivity.CONTRIBUTION_INTENT_DATA)
                Log.i("Algorithm", "Contribution string returned from the recent transaction: $contributions")
                newSettlementString = newContributionUpdates(contributions!!)
                refreshHelper.refreshEverything(newSettlementString)
            }
        } else if (requestCode == VIEW_EXPENSE_RESULT) {
            if (userMadeAnEditOrDeletion(resultCode)) {
                if (userDeletedExpense(data)) {
                    val reversedContributions = data?.getStringExtra(ExpenseViewActivity.expenseReturnNewContributions)
                    Log.i("Algorithm", "Reversed contribution string returned from the " +
                                "recent deletion: $reversedContributions")
                    newSettlementString = newContributionUpdates(reversedContributions!!)
                } else {
                    newSettlementString = data!!.getStringExtra(ExpenseViewActivity.expenseReturnNewSettlements)
                    if (userDidNotChangeFinancials(newSettlementString)){
                        newSettlementString = SqlDbHelper(this).loadSqlSettlementString(currentSqlGroupId)
                    }
                }
                refreshHelper.deconstructAndSetSettlementString(newSettlementString!!)

            }
            refreshHelper.reloadRecycler()
            refreshHelper.refreshStatistics()
        } else if (requestCode == SETTINGS_RESULT) {
            if (resultCode == Activity.RESULT_OK) {
                val groupName = data?.getStringExtra(GroupSettingsActivity.groupNameReturnIntent)
                binding.groupNameTitleText.text = groupName
                val uriString: String? = data?.getStringExtra(GroupSettingsActivity.groupImageChangedUriIntent)
                if (uriString != null) {
                    val uri = Uri.parse(uriString)
                    binding.groupProfileImage.setImageURI(uri)
                }
            }
        } else if (requestCode == PICK_IMAGE_RESULT) {
            if (resultCode == Activity.RESULT_OK) {
                val uri: Uri? = data!!.data
                GroupSettingsActivity.handleNewImage(this, uri!!, binding.groupProfileImage)
            }
            if (floatingButtonsShowing) {
                addNewReceiptButton()
            }
        } else if (requestCode == BALANCES_RESULT){
            if (resultCode == Activity.RESULT_OK){
                val settle = data?.getBooleanExtra(BalanceOverviewActivity.balanceResult, false)
                if (settle!!) {
                    val intent = Intent(this, SettleGroupActivity::class.java)
                    startActivityForResult(intent, ADD_EXPENSE_RESULT)
                }
            }
        }
    }

    private fun userMadeAnEditOrDeletion(resultCode: Int) = resultCode == Activity.RESULT_OK

    private fun userDeletedExpense(data: Intent?) =
        data?.getStringExtra(ExpenseViewActivity.expenseReturnNewSettlements) == null

    private fun userDidNotChangeFinancials(newSettlementString: String?) = newSettlementString == null


    private fun newContributionUpdates(newContributions: String): String {
        val balanceSettlementHelper = BalanceSettlementHelper(this, currentSqlGroupId.toString())
        val settlementString = balanceSettlementHelper.balanceAndSettlementsFromSql(newContributions)
        firebaseDbHelper!!.setGroupFinance(settlementString)
        return settlementString
    }

    fun balancesButtonPressed(view: View?=null) {
        val intent = Intent(this, BalanceOverviewActivity::class.java)
        startActivityForResult(intent, BALANCES_RESULT)
    }

    fun settingsButtonPressed(view: View?=null) {
        val intent = Intent(this, GroupSettingsActivity::class.java)
        intent.putExtra(GroupSettingsActivity.groupNameIntent, currentGroupName)
        intent.putExtra(GroupSettingsActivity.groupSqlIdIntent, currentSqlGroupId)
        startActivityForResult(intent, SETTINGS_RESULT)
    }

    fun settleButtonPressed(view: View?=null) {
        val intent = Intent(this, SettleGroupActivity::class.java)
        startActivityForResult(intent, ADD_EXPENSE_RESULT)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
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
        startActivityForResult(intent, VIEW_EXPENSE_RESULT)
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
                settleButtonPressed()
                return true
            }
            R.id.groupAddParticipant -> {
                val intent = Intent(this, NewParticipantInviteActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.groupSettings -> {
                settingsButtonPressed()
                return true
            }
            R.id.groupBalances -> {
                balancesButtonPressed()
                return true
            }
            R.id.feedback -> {
                val emailIntent = Intent(
                    Intent.ACTION_SENDTO, Uri.fromParts("mailto", "dan96.austin@gmail.com", null))
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback - 24hour response")
                emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("dan96.austin@gmail.com"));
                startActivity(Intent.createChooser(emailIntent, "Send e-mail to dev..."))
                return true
            }
            else -> return false
        }
    }

}

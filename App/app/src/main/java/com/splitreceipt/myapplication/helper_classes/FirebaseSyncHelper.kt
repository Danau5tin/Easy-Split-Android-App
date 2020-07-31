package com.splitreceipt.myapplication.helper_classes

import android.content.Context
import android.util.Log
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.splitreceipt.myapplication.ExpenseOverviewActivity
import com.splitreceipt.myapplication.ExpenseOverviewActivity.Companion.refreshHelper
import com.splitreceipt.myapplication.WelcomeJoinActivity
import com.splitreceipt.myapplication.data.*
import de.hdodenhof.circleimageview.CircleImageView

class FirebaseSyncHelper() {

    lateinit var context: Context
    lateinit var sqlDbHelper: SqlDbHelper
    lateinit var currentSqlGroupId: String
    lateinit var firebaseDbHelper: FirebaseDbHelper
    lateinit var groupNameTitleText: TextView
    lateinit var groupProfileImage: CircleImageView

    constructor (
        context: Context,
        sqlDbHelper: SqlDbHelper,
        currentSqlGroupId: String,
        firebaseDbHelper: FirebaseDbHelper,
        groupNameTitleText: TextView,
        groupProfileImage: CircleImageView) : this() {
        this.context = context
        this.sqlDbHelper = sqlDbHelper
        this.currentSqlGroupId = currentSqlGroupId
        this.firebaseDbHelper = firebaseDbHelper
        this.groupNameTitleText = groupNameTitleText
        this.groupProfileImage = groupProfileImage
    }

    fun syncGroupWithFirebaseDB() {
        syncCurrentGroupInformationWithFirebaseDB()
        syncCurrentGroupExpensesWithFirebaseDB()
    }

    fun syncCurrentGroupExpensesWithFirebaseDB() {

        val expenseInfoDbRef = firebaseDbHelper.getExpensesListeningRef()
        expenseInfoDbRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(context, "Failed to sync changes", Toast.LENGTH_SHORT).show()
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                val sqlExpenses = sqlDbHelper.retrieveBasicExpenseSqlData(ExpenseOverviewActivity.currentSqlGroupId!!)
                var settlementString: String? = null
                val balanceSettlementHelper = BalanceSettlementHelper(context, ExpenseOverviewActivity.currentSqlGroupId!!)

                for (expense in snapshot.children){
                    val firebaseID = expense.key
                    var expenseExistsInSql = false
                    val expenseSnapshot = snapshot.child(firebaseID!!)
                    val firebaseExpense = expenseSnapshot.getValue(Expense::class.java)!!
                    firebaseExpense.firebaseIdentifier = firebaseID
                    var changedSinceLastSync = false

                    for (sqlExpense in sqlExpenses) {
                        if (firebaseExpenseIsInSql(firebaseID, sqlExpense)){
                            expenseExistsInSql = true
                            firebaseExpense.sqlExpenseRowId = sqlExpense.sqlRowId
                            if (firebaseExpense.lastEdit != sqlExpense.lastEdit) {
                                changedSinceLastSync = true
                                sqlDbHelper.updateExpense(firebaseExpense)
                            }
                            break
                        }
                    }
                    if (!expenseExistsInSql) {
                        changedSinceLastSync = true
                        val currency = firebaseExpense.currencyCode
                        firebaseExpense.currencySymbol = CurrencyExchangeHelper.returnUiSymbol(currency)
                        val expenseSqlRow = sqlDbHelper.insertNewExpense(firebaseExpense)
                        if (firebaseExpense.scanned){
                            syncScannedExpenseProducts(firebaseDbHelper.firebaseGroupId, expenseSqlRow)
                        }
                    }
                    if (changedSinceLastSync) {
                        settlementString = balanceSettlementHelper.updateBalancesReturnSettlement(firebaseExpense.contribs)
                    }
                }

                if (settlementString != null) {
                    // User has updated expenses from Firebase so update Firebase with new balance, settlement strings.
                    balanceSettlementHelper.updateFirebaseSettle(ExpenseOverviewActivity.firebaseDbHelper!!)
                } else {
                    // User has no new updated expenses downloaded from the firebase database
                    settlementString = sqlDbHelper.loadSqlSettlementString(ExpenseOverviewActivity.currentSqlGroupId)
                }
                refreshHelper.refreshEverything(settlementString)
            }
        })
    }

    private fun syncScannedExpenseProducts(firebaseGroupID: String, expenseSqlRow: Int) {
        val scannedRef = firebaseDbHelper.getScannedListeningRef(firebaseGroupID)
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

    private fun firebaseExpenseIsInSql(
        firebaseID: String?,
        sqlExpense: BasicExpenseDataSql
    ) = firebaseID == sqlExpense.firebaseId

    private fun syncCurrentGroupInformationWithFirebaseDB(participantList: ArrayList<String>?=null) {
        val accountInfoDbRef = firebaseDbHelper.getAccountInfoListeningRef()
        accountInfoDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(context, "Failed to sync changes", Toast.LENGTH_SHORT).show()
            }
            override fun onDataChange(data: DataSnapshot) {
                val firebaseGroupData = data.getValue(FirebaseAccountInfoData::class.java)!!
                val sqlGroupData = sqlDbHelper.retrieveSqlAccountInfoData(currentSqlGroupId)
                var infoChanged = false
                var imageChanged = false
                var participantsChanged = false
                if (firebaseGroupData.name != sqlGroupData.name) {
                    groupNameTitleText.text = firebaseGroupData.name
                    infoChanged = true
                }
                if (groupInfoChanged(firebaseGroupData, sqlGroupData)){
                    infoChanged = true
                    participantsChanged = true
                }
                if (groupProfileImageChanged(firebaseGroupData, sqlGroupData)) {
                    imageChanged = true
                    infoChanged = true
                }
                if (infoChanged) {
                    sqlDbHelper.updateGroupInfo(firebaseGroupData, currentSqlGroupId)
                }
                if (imageChanged) {
                    firebaseDbHelper.downloadAndSetNewGroupProfileImage(context, groupProfileImage)
                }
                if (participantsChanged) {
                    syncParticipantsWithFirebase(
                        currentSqlGroupId, firebaseDbHelper, firebaseGroupData
                            .participantLastEdit, participantList)
                }
            }
        })
    }

    private fun groupProfileImageChanged(
        firebaseGroupData: FirebaseAccountInfoData,
        sqlGroupData: FirebaseAccountInfoData
    ) = firebaseGroupData.lastImageEdit != sqlGroupData.lastImageEdit

    private fun groupInfoChanged(
        firebaseGroupData: FirebaseAccountInfoData,
        sqlGroupData: FirebaseAccountInfoData
    ) = firebaseGroupData.participantLastEdit != sqlGroupData.participantLastEdit


    fun syncParticipantsWithFirebase(
        sqlGroupId: String,
        firebaseDbHelper: FirebaseDbHelper,
        groupParticipantLastEdit: String,
        participantList: ArrayList<String>? = null,
        context: Context? = null,
        radioGroup: RadioGroup? = null,
        sqlHelper: SqlDbHelper = sqlDbHelper
    ) {
        val usersDbRef = firebaseDbHelper.getUsersListeningRef()
        usersDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}
            override fun onDataChange(snapshot: DataSnapshot) {
                val allSqlUsers: ArrayList<ParticipantBalanceData> = ArrayList()
                sqlHelper.retrieveGroupParticipants(allSqlUsers, sqlGroupId)
                for (fbUser in snapshot.children) {
                    var userExists = false
                    val fBaseKey = fbUser.key.toString()
                    val downloadedUser = fbUser.getValue(ParticipantBalanceData::class.java)
                    downloadedUser!!.userKey = fBaseKey
                    for (sqlUser in allSqlUsers) {
                        if (userIsAlreadyInSql(sqlUser, fBaseKey)) {
                            userExists = true
                            if (userHasChangedName(sqlUser, downloadedUser)) {
                                sqlHelper.updateParticipantsName(sqlUser,
                                    downloadedUser.userName, groupParticipantLastEdit, sqlGroupId)
                                Log.i("Participants", "Participant: ${sqlUser.userName} exists in DB. Name has been changed to ${downloadedUser.userName}")
                            } else {
                                Log.i("Participants", "Participant: ${sqlUser.userName} exists in DB. Name unchanged.")
                            }
                            break
                        }
                    }
                    if (!userExists) {
                        sqlHelper.setGroupParticipants(downloadedUser, sqlGroupId, groupParticipantLastEdit)
                        Log.i("Participants", "Participant: ${downloadedUser
                            .userName} of key: ${downloadedUser.userKey} is new and has been added to SQL")
                    }
                    participantList?.add(downloadedUser.userName)
                }
                if (participantList != null) {
                    WelcomeJoinActivity.populateRadioButtons(
                        context!!,
                        participantList,
                        radioGroup!!
                    )
                }
            }
        })
    }

    private fun userHasChangedName(
        sqlUser: ParticipantBalanceData,
        downloadedUser: ParticipantBalanceData
    ) = sqlUser.userName != downloadedUser.userName

    private fun userIsAlreadyInSql(
        sqlUser: ParticipantBalanceData,
        fBaseKey: String
    ) = sqlUser.userKey == fBaseKey
}
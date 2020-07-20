package com.splitreceipt.myapplication.helper_classes

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.RadioGroup
import android.widget.Toast
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.splitreceipt.myapplication.a_sync_classes.ASyncSaveImage
import com.splitreceipt.myapplication.WelcomeJoinActivity
import com.splitreceipt.myapplication.data.*
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream

class FirebaseDbHelper(var firebaseGroupId: String) {

    var database = FirebaseDatabase.getInstance()
    private lateinit var currentPath : DatabaseReference

    private var groupInfo = "/info"
    private var groupFin = "/finance"
    private var expenses = "/expenses"
    private var scanned = "/scanned"
    private var participants = "/users"

    init{
        database = FirebaseDatabase.getInstance()
    }

    fun checkJoin(context: Context){
        // Checks whether a firebase group ID is valid. //TODO: Is this necessary? Why not just try and download the whole group?
        val groupInfoPath = "$firebaseGroupId$groupInfo"
        currentPath = database.getReference(groupInfoPath)
        currentPath.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onCancelled(error: DatabaseError) {Log.i("Join", "Failed due to ${error.message}")}
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    Log.i("Join", "SUCCESS")
                    val groupData = snapshot.getValue(FirebaseAccountInfoData::class.java)
                    if (groupData != null) {
                        val intent = Intent(context, WelcomeJoinActivity::class.java)
                        intent.putExtra(WelcomeJoinActivity.joinFireBaseParticipants, groupData.participantLastEdit)
                        intent.putExtra(WelcomeJoinActivity.joinFireBaseId, firebaseGroupId)
                        intent.putExtra(WelcomeJoinActivity.joinFireBaseName, groupData.name)
                        intent.putExtra(WelcomeJoinActivity.joinBaseCurrency, groupData.baseCurrencyCode)
                        context.startActivity(intent)
                    }else {
                        Toast.makeText(context, "No group exists. Check group identifier", Toast.LENGTH_SHORT).show()
                        Log.i("Join", "No group exists for $firebaseGroupId")
                    } }
                catch (e: Exception){
                    Log.i("Join", "Failed because of ${e.message}")
                } } })
    }

    fun syncEntireGroupFromFirebase(context: Context, participantList: ArrayList<String>? = null, joinRadioGroup: RadioGroup?=null){
        // TODO: Move this into the FirebaseSync Class
        currentPath = database.getReference(firebaseGroupId)
        currentPath.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                Log.i("Join", "joinGroupDownload failed: ${p0.details}")
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                val newGroup = returnNewGroupFromSnapshot(snapshot)
                val sqlHelper = SqlDbHelper(context)

                val sqlRow = sqlHelper.insertNewGroup(newGroup)

                newGroup.sqlGroupRowId = sqlRow.toString()
                WelcomeJoinActivity.sqlRow = newGroup.sqlGroupRowId

                FirebaseSyncHelper().syncParticipantsWithFirebase(newGroup.sqlGroupRowId,
                    this@FirebaseDbHelper, newGroup.lastParticipantEditTime,
                    participantList, context, joinRadioGroup!!, sqlHelper)

                val expensesChild = snapshot.child(expenses.substring(1))
                val scannedChild = snapshot.child(scanned.substring(1))
                for (expense in expensesChild.children) {
                    val expenseData = expense.getValue(Expense::class.java)!!
                    expenseData.sqlGroupRowId = newGroup.sqlGroupRowId
                    expenseData.firebaseIdentifier = expense.key!!
                    expenseData.currencySymbol = CurrencyHelper.returnUiSymbol(expenseData.currencyCode)
                    expenseData.sqlExpenseRowId = sqlHelper.insertNewExpense(expenseData).toString() //TODO: Change expense to carry an Int for the group ID

                    if (expenseData.scanned) {
                        for (receipt in scannedChild.children) {
                            val scannedRecExpId = receipt.key
                            if (scannedRecExpId == expenseData.sqlExpenseRowId) {
                                val productlist: ArrayList<FirebaseProductData> = ArrayList()
                                for (product in receipt.children){
                                    val productData = product.getValue(FirebaseProductData::class.java)!!
                                    productlist.add(productData)
                                }
                                sqlHelper.insertReceiptItems(productlist, expenseData.sqlExpenseRowId.toInt())
                                break
                            }
                        }
                    }
                }
            }
        })
    }

    private fun returnNewGroupFromSnapshot(snapshot: DataSnapshot) : GroupData {
        val infoChild = snapshot.child(groupInfo.substring(1))
        val financeChild = snapshot.child(groupFin.substring(1))
        val infoData = infoChild.getValue(FirebaseAccountInfoData::class.java)!!
        val financeData = financeChild.getValue(FirebaseAccountFinancialData::class.java)!!
        val baseCurrencyUiSymbol = CurrencyHelper.returnUiSymbol(infoData.baseCurrencyCode)
        return GroupData(infoData.name, firebaseGroupId, infoData.baseCurrencyCode,
            baseCurrencyUiSymbol, infoData.participantLastEdit, infoData.lastImageEdit, financeData.accSettle)
    }

    fun createNewGroup(newGroup: GroupData){
        setGroupInfo(newGroup.name, newGroup.lastParticipantEditTime, newGroup.lastGroupImageEditTime, newGroup.baseCurrencyCode)
        setGroupFinance(newGroup.settlementString)
    }

    fun setGroupParticipants(newParticipants: ArrayList<ParticipantBalanceData>) {
        val participantPath = "$firebaseGroupId$participants"
        for (participant in newParticipants) {
            currentPath = database.getReference(participantPath).child(participant.userKey)
            currentPath.setValue(participant)
        }
    }

    fun setGroupParticipants(newParticipant: ParticipantBalanceData, timeStamp: String) {
        val participantPath = "$firebaseGroupId$participants"
        currentPath = database.getReference(participantPath).child(newParticipant.userKey)
        currentPath.setValue(newParticipant)
        updateParticipantLastEdit(timeStamp)
    }

    fun updateParticipantName(participant: ParticipantBalanceData, newName: String, timeStamp: String) {
        val participantPath = "$firebaseGroupId$participants"
        currentPath = database.getReference(participantPath).child(participant.userKey).child("userName")
        currentPath.setValue(newName)
        updateParticipantLastEdit(timeStamp)
    }

    private fun updateParticipantLastEdit(timeStamp: String) {
        val participantLastEditPath = "$firebaseGroupId$groupInfo"
        currentPath = database.getReference(participantLastEditPath).child("accParticipantLastEdit")
        currentPath.setValue(timeStamp)
    }

    fun updateParticipantBalances(newBalanceObjects: ArrayList<ParticipantBalanceData>) {
        // This function will update the groups balances with the most recent balances.
        val participantPath = "$firebaseGroupId$participants"
        for (participant in newBalanceObjects) {
            currentPath = database.getReference(participantPath).child(participant.userKey)
            currentPath.setValue(participant)
        }
    }

    fun setGroupFinance(groupSettlement: String) {
        val groupFinancePath = "$firebaseGroupId$groupFin"
        val accountData =
            FirebaseAccountFinancialData(
                groupSettlement
            )
        currentPath = database.getReference(groupFinancePath)
        currentPath.setValue(accountData)
    }

    fun setGroupImageLastEdit(lastEdit: String) {
        val groupInfoPath = "$firebaseGroupId$groupInfo"
        currentPath = database.getReference(groupInfoPath).child("accLastImage")
        currentPath.setValue(lastEdit)
    }

    private fun setGroupInfo(groupName: String, participantString: String, lastImageEdit: String, baseCurrency: String) {
        val groupInfoPath = "$firebaseGroupId$groupInfo"
        val accountData =
            FirebaseAccountInfoData(
                groupName,
                participantString,
                lastImageEdit,
                baseCurrency
            )
        currentPath = database.getReference(groupInfoPath)
        currentPath.setValue(accountData)
    }

    fun insertOrUpdateExpense(expense: Expense) {
        //Creates a new expense if not exists and if exists updates.
        val expensePath = "$firebaseGroupId$expenses/${expense.firebaseIdentifier}"
        currentPath = database.getReference(expensePath)
        currentPath.setValue(expense)
    }

    fun getAccountInfoListeningRef() : DatabaseReference{
        currentPath = database.getReference("$firebaseGroupId$groupInfo")
        return currentPath
    }

    fun getExpensesListeningRef() : DatabaseReference {
        currentPath = database.getReference("$firebaseGroupId$expenses")
        return currentPath
    }

    fun getScannedListeningRef(firebaseExpenseId: String) : DatabaseReference {
        val path = "$firebaseGroupId$scanned/$firebaseExpenseId"
        currentPath = database.getReference(path)
        return currentPath
    }

    fun getUsersListeningRef() : DatabaseReference {
        currentPath = database.getReference("$firebaseGroupId$participants")
        return currentPath
    }

    fun addUpdateReceiptItems(expenseId: String, itemizedProductData: ArrayList<ScannedItemizedProductData>){
        //Add all new receipt items if not exists. If exists remove prior receipts and add new updates.
        val receiptPath = "$firebaseGroupId$scanned/$expenseId"
        currentPath = database.getReference(receiptPath)
        currentPath.removeValue()
        var count = 1
        for (product in itemizedProductData){
            val productPath = "$receiptPath/$count"
            val firebaseProductData =
                FirebaseProductData(
                    product.itemName,
                    product.itemValue,
                    product.ownership
                )
            currentPath = database.getReference(productPath)
            currentPath.setValue(firebaseProductData)
            count ++
        }
    }

    fun updateGroupName(groupName: String) {
        val groupInfoPath = "$firebaseGroupId$groupInfo"
        currentPath = database.getReference(groupInfoPath).child("accName")
        currentPath.setValue(groupName)
    }

    fun uploadGroupProfileImage(imageRef: Bitmap?) {
        val storageReference = FirebaseStorage.getInstance().getReference(firebaseGroupId)
        val userStorageRef = storageReference.child(firebaseGroupId)

        val stream = ByteArrayOutputStream()
        imageRef!!.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val byteArray: ByteArray = stream.toByteArray()

        val uploadTask = userStorageRef.putBytes(byteArray)
        uploadTask.addOnSuccessListener {
            Log.i("FirebaseImages", "Successful upload of new profile image")
        }
    }

    fun downloadAndSetNewGroupProfileImage(context: Context, circleImageView: CircleImageView) {
        // Download the group profile image. Save it locally. Set the image to a view.
        val storageReference = FirebaseStorage.getInstance().getReference(firebaseGroupId)
        val userStorageRef = storageReference.child(firebaseGroupId)
        val downloadTask = userStorageRef.getBytes(1000000000)
        downloadTask.addOnSuccessListener{
            val async =
                ASyncSaveImage(
                    true,
                    context,
                    firebaseGroupId
                )
            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            async.execute(bitmap)
            circleImageView.setImageBitmap(bitmap)
            Log.i("FirebaseImages", "Successful download of existing profile image")
        }
        downloadTask.addOnFailureListener {
            Log.i("FirebaseImages", "Failed ${it.printStackTrace()}")
        }
    }


    fun deleteExpense(expenseId: String, scan: Boolean) {
        val expensePath = "$firebaseGroupId$expenses/$expenseId"
        currentPath = database.getReference(expensePath)
        currentPath.removeValue()
        if (scan) {
            val expenseScannedPath = "$firebaseGroupId$scanned/$expenseId"
            currentPath = database.getReference(expenseScannedPath)
            currentPath.removeValue()
        }
    }

}
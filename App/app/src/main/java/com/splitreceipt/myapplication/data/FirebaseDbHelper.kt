package com.splitreceipt.myapplication.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.splitreceipt.myapplication.ASyncSaveImage
import com.splitreceipt.myapplication.WelcomeJoinActivity
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream

class FirebaseDbHelper(private var firebaseGroupId: String) {

    var database = FirebaseDatabase.getInstance()
    private lateinit var currentPath : DatabaseReference

    //Common paths
    private var groupInfo = "/info"
    private var groupLastEdit = "/lastEdit"
    private var groupFin = "/finance"
    private var expenses = "/expenses"
    private var scanned = "/scanned"

    init{
        database = FirebaseDatabase.getInstance()
    }

    fun checkJoin(context: Context){
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
                        intent.putExtra(WelcomeJoinActivity.joinFireBaseParticipants, groupData.accParticipants)
                        intent.putExtra(WelcomeJoinActivity.joinFireBaseId, firebaseGroupId)
                        intent.putExtra(WelcomeJoinActivity.joinFireBaseName, groupData.accName)
                        context.startActivity(intent)
                    }else {
                        Toast.makeText(context, "No group exists. Check group identifier", Toast.LENGTH_SHORT).show()
                        Log.i("Join", "No group exists for $firebaseGroupId")
                    } }
                catch (e: Exception){
                    Log.i("Join", "Failed because of ${e.message}")
                } } })
    }

    fun downloadToSql(context: Context){
        currentPath = database.getReference(firebaseGroupId)
        currentPath.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                Log.i("Join", "joinGroupDownload failed: ${p0.details}")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                //Substring is to remove the "/" path indicator in the strings.
                val infoChild = snapshot.child(groupInfo.substring(1))
                val financeChild = snapshot.child(groupFin.substring(1))
                val expensesChild = snapshot.child(expenses.substring(1))
                val scannedChild = snapshot.child(scanned.substring(1))
                val infoData = infoChild.getValue(FirebaseAccountInfoData::class.java)!!
                val financeData = financeChild.getValue(FirebaseAccountFinancialData::class.java)!!
                val sqlHelper = SqlDbHelper(context)
                val sqlRow = sqlHelper.insertNewGroup(firebaseGroupId, infoData.accName,
                    infoData.accParticipants, financeData.accBal, financeData.accSettle, "u", infoData.accLastImage)

                val sqlRowString = sqlRow.toString()
                WelcomeJoinActivity.sqlRow = sqlRowString

                for (expense in expensesChild.children) {
                    val expenseData = expense.getValue(FirebaseExpenseData::class.java)!!
                    val expenseId = expense.key!!
                    val expenseSqlRow = sqlHelper.insertNewExpense(sqlRowString, expenseId, expenseData.expDate,
                        expenseData.expTitle, expenseData.expTotal, expenseData.expPaidBy,
                        expenseData.expContribs, expenseData.expScanned, expenseData.expLastEdit)

                    if (expenseData.expScanned) {
                        for (receipt in scannedChild.children) {
                            val scannedRecExpId = receipt.key
                            if (scannedRecExpId == expenseId) {
                                for (product in receipt.children){
                                    val productData = product.getValue(FirebaseProductData::class.java)!!
                                    sqlHelper.insertReceiptItems(productData.productName, productData.productValue, productData.productOwner, expenseSqlRow)
                                }
                                break
                            }
                        }
                    }
                }
            }
        })
    }

    fun createNewGroup(groupName: String, groupBalance: String, groupSettlement: String, participantString: String, lastImageEdit: String){
        setGroupInfo(groupName, participantString, lastImageEdit)
        setGroupFinance(groupSettlement, groupBalance)
    }

    fun setGroupFinance(groupSettlement: String, groupBalance: String) {
        val groupFinancePath = "$firebaseGroupId$groupFin"
        val accountData = FirebaseAccountFinancialData(groupSettlement, groupBalance)
        currentPath = database.getReference(groupFinancePath)
        currentPath.setValue(accountData)
    }

    fun setGroupImageLastEdit(lastEdit: String) {
        val groupInfoPath = "$firebaseGroupId$groupInfo"
        currentPath = database.getReference(groupInfoPath).child("accLastImage")
        currentPath.setValue(lastEdit)
    }

    fun setGroupInfo(groupName: String, participantString: String, lastImageEdit: String) {
        val groupInfoPath = "$firebaseGroupId$groupInfo"
        val accountData = FirebaseAccountInfoData(groupName, participantString, lastImageEdit)
        currentPath = database.getReference(groupInfoPath)
        currentPath.setValue(accountData)
    }

    fun createUpdateNewExpense(expenseId: String, date: String, title: String, total: Float,
                               paidBy: String, contributions: String, scanned: Boolean, lastEdit: String) {
        //Creates a new expense if not exists and if exists updates.
        val expenseData = FirebaseExpenseData(date, title, total, paidBy, contributions, scanned, lastEdit)
        val expensePath = "$firebaseGroupId$expenses/$expenseId"
        currentPath = database.getReference(expensePath)
        currentPath.setValue(expenseData)
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

    fun addUpdateReceiptItems(expenseId: String, itemizedProductData: ArrayList<ScannedItemizedProductData>){
        //Add all new receipt items if not exists. If exists remove prior receipts and add new updates.
        val receiptPath = "$firebaseGroupId$scanned/$expenseId"
        currentPath = database.getReference(receiptPath)
        currentPath.removeValue()
        var count = 1
        for (product in itemizedProductData){
            val productPath = "$receiptPath/$count"
            val firebaseProductData = FirebaseProductData(product.itemName, product.itemValue, product.ownership)
            currentPath = database.getReference(productPath)
            currentPath.setValue(firebaseProductData)
            count ++
        }
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

    fun downloadGroupProfileImage(context: Context, circleImageView: CircleImageView) {
        val storageReference = FirebaseStorage.getInstance().getReference(firebaseGroupId)
        val userStorageRef = storageReference.child(firebaseGroupId)
        val downloadTask = userStorageRef.getBytes(1000000000)
        downloadTask.addOnSuccessListener{
            val async = ASyncSaveImage(true, context, firebaseGroupId)
            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            async.execute(bitmap)
            circleImageView.setImageBitmap(bitmap)
            Log.i("FirebaseImages", "Successful download of existing profile image")
        }
        downloadTask.addOnFailureListener {
            Log.i("FirebaseImages", "Failed ${it.printStackTrace()}")
        }
    }

}
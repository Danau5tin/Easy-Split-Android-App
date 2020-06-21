package com.splitreceipt.myapplication.data

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.*
import com.splitreceipt.myapplication.WelcomeJoinActivity
import kotlin.system.exitProcess

class FirebaseDbHelper(private var firebaseGroupId: String) {

    var database = FirebaseDatabase.getInstance()
    private lateinit var currentPath : DatabaseReference

    //Common paths
    private var groupInfo = "/info"
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
                val sqlRow = sqlHelper.insertNewGroup(firebaseGroupId, infoData.accName, infoData.accCat,
                    infoData.accParticipants, financeData.accBal, financeData.accSettle, "u")

                val sqlRowString = sqlRow.toString()
                WelcomeJoinActivity.sqlRow = sqlRowString

                for (expense in expensesChild.children) {
                    val expenseData = expense.getValue(FirebaseExpenseData::class.java)!!
                    val expenseId = expense.key!!
                    val expenseSqlRow = sqlHelper.insertNewExpense(sqlRowString, expenseId, expenseData.expDate,
                        expenseData.expTitle, expenseData.expTotal, expenseData.expPaidBy,
                        expenseData.expContribs, expenseData.expScanned)

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

    fun createNewAccount(groupName: String, groupCat: String,
                        groupBalance: String, groupSettlement: String, participantString: String){
        setAccountInfo(groupName, groupCat, participantString)
        setAccountFinance(groupSettlement, groupBalance)
    }

    fun setAccountFinance(groupSettlement: String, groupBalance: String) {
        val groupFinancePath = "$firebaseGroupId$groupFin"
        val accountData = FirebaseAccountFinancialData(groupSettlement, groupBalance)
        currentPath = database.getReference(groupFinancePath)
        currentPath.setValue(accountData)
    }

    private fun setAccountInfo(groupName: String, groupCat: String, participantString: String) {
        val groupInfoPath = "$firebaseGroupId$groupInfo"
        val accountData = FirebaseAccountInfoData(groupName, groupCat, participantString)
        currentPath = database.getReference(groupInfoPath)
        currentPath.setValue(accountData)
    }

    fun createNewExpense(expenseId: String, date: String, title: String, total: Float,
                    paidBy: String, contributions: String, scanned: Boolean) {
        //Creates a new expense
        val expenseData = FirebaseExpenseData(date, title, total, paidBy, contributions, scanned)
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

    fun addReceiptItems(expenseId: String, itemizedProductData: ArrayList<ScannedItemizedProductData>){
        //Add all new receipt items
        val receiptPath = "$firebaseGroupId$scanned/$expenseId"
        var count = 1
        for (product in itemizedProductData){
            val productPath = "$receiptPath/$count"
            val firebaseProductData = FirebaseProductData(product.itemName, product.itemValue, product.ownership)
            currentPath = database.getReference(productPath)
            currentPath.setValue(firebaseProductData)
            count ++
        }
    }


    //TODO: Are any of the below functions required?

//    fun setDate(){
//        val datePath = "$expensePath/date"
//        currentPath = database.getReference(datePath)
//        currentPath.setValue(date)
//    }
//
//    fun setTitle() {
//        val titlePath = "$expensePath/title"
//        currentPath = database.getReference(titlePath)
//        currentPath.setValue(title)
//    }
//
//    fun setTotal(){
//        val totalPath = "$expensePath/total"
//        currentPath = database.getReference(totalPath)
//        currentPath.setValue(total)
//    }
//
//    fun setPaidBy(){
//        val paidByPath = "$expensePath/paidBy"
//        currentPath = database.getReference(paidByPath)
//        currentPath.setValue(paidBy)
//    }
//
//    fun setContributions(){
//        val contribPath = "$expensePath/contribs"
//        currentPath = database.getReference(contribPath)
//        currentPath.setValue(contributions)
//    }

//    private fun setParticipants(participantString: String) {
//        //Sets the group participants
//        val participantPath = "$firebaseGroupId/participants"
//        currentPath = database.getReference(participantPath)
//        currentPath.setValue(participantString)
//    }

}
package com.splitreceipt.myapplication.data

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class FirebaseDbHelper(private var firebaseGroupId: String) {

    var database = FirebaseDatabase.getInstance()
    private lateinit var currentPath : DatabaseReference

    //Slightly common end paths
    private var groupInfo = "/info"
    private var groupFin = "/finance"
    private var expenses = "/expenses"
    private var scanned = "/scanned"

    init{
        database = FirebaseDatabase.getInstance()
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
                    paidBy: String, contributions: String) {
        //Creates a new expense
        val expenseData = FirebaseExpenseData(date, title, total, paidBy, contributions)
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


    fun addReceiptItems(expenseId: String, itemizedProductData: ArrayList<ScannedItemizedProductData>){
        //Add all new receipt items
        val receiptPath = "$firebaseGroupId/$expenses/$expenseId$scanned"
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


    //    fun setSettlementString(settlementString: String){
//        //Sets the group settlement string
//        val settlementPath = "$firebaseGroupId$groupInfo"
//        currentPath = database.getReference(settlementPath)
//        currentPath.setValue(settlementString)
//    }

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
//

//
//    fun setBalanceString(groupBalance: String) {
//        //Sets the group balance string
//        val balanceBranchPath = "$firebaseGroupId/balance"
//        currentPath = database.getReference(balanceBranchPath)
//        currentPath.setValue(groupBalance)
//    }

}
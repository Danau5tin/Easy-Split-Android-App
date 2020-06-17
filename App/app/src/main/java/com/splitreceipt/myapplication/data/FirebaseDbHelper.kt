package com.splitreceipt.myapplication.data

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class FirebaseDbHelper(private var firebaseGroupId: String) {

    var database = FirebaseDatabase.getInstance()
    private lateinit var currentPath : DatabaseReference

    //Slightly common end paths
    private var groupInfo = "/info"
    private var expenses = "/expenses"
    private var scanned = "/scanned"

    init{
        database = FirebaseDatabase.getInstance()
    }

    fun createNewAccount(groupName: String, groupCat: String,
                        groupBalance: String, groupSettlement: String, participantString: String){

        fun setInfoBranch(){
            //Sets the initial group name and category
            val infoBranchPath = firebaseGroupId + groupInfo
            val groupNamePath = "/name"
            val groupCatPath = "/cat"
            currentPath = database.getReference(infoBranchPath + groupNamePath)
            currentPath.setValue(groupName)
            currentPath = database.getReference(infoBranchPath + groupCatPath)
            currentPath.setValue(groupCat)
        }

        setInfoBranch()
        setBalanceString(groupBalance)
        setSettlementString(groupSettlement)
        setParticipants(participantString)
    }

    fun createNewExpense(expenseId: String, date: String, title: String, total: Float,
                    paidBy: String, contributions: String) {
        //Creates a new expense
        val expensePath = "$firebaseGroupId$expenses/$expenseId"

        fun setDate(){
            val datePath = "$expensePath/date"
            currentPath = database.getReference(datePath)
            currentPath.setValue(date)
        }

        fun setTitle() {
            val titlePath = "$expensePath/title"
            currentPath = database.getReference(titlePath)
            currentPath.setValue(title)
        }

        fun setTotal(){
            val totalPath = "$expensePath/total"
            currentPath = database.getReference(totalPath)
            currentPath.setValue(total)
        }

        fun setPaidBy(){
            val paidByPath = "$expensePath/paidBy"
            currentPath = database.getReference(paidByPath)
            currentPath.setValue(paidBy)
        }

        fun setContributions(){
            val contribPath = "$expensePath/contribs"
            currentPath = database.getReference(contribPath)
            currentPath.setValue(contributions)
        }

        setDate()
        setTitle()
        setTotal()
        setPaidBy()
        setContributions()
    }

    fun addReceiptItems(expenseId: String, itemizedProductData: ArrayList<ScannedItemizedProductData>){
        //Add all new receipt items
        fun setProductName(name: String, path: String){
            val productNamePath = "$path/name"
            currentPath = database.getReference(productNamePath)
            currentPath.setValue(name)
        }

        fun setProductValue(value: String, path: String){
            val productValuePath = "$path/value"
            currentPath = database.getReference(productValuePath)
            currentPath.setValue(value)
        }

        fun setProductOwnership(owner: String, path: String){
            val productOwnerPath = "$path/owner"
            currentPath = database.getReference(productOwnerPath)
            currentPath.setValue(owner)
        }

        val receiptPath = "$firebaseGroupId/$expenses/$expenseId$scanned"
        var count = 1
        for (product in itemizedProductData){
            val productName = product.itemName
            val productValue = product.itemValue
            val productOwnership = product.ownership
            val productPath = "$receiptPath/$count"

            setProductName(productName, productPath)
            setProductValue(productValue, productPath)
            setProductOwnership(productOwnership, productPath)

            count ++
        }
    }

    private fun setParticipants(participantString: String) {
        //Sets the group participants
        val participantPath = "$firebaseGroupId/participants"
        currentPath = database.getReference(participantPath)
        currentPath.setValue(participantString)
    }

    fun setSettlementString(settlementString: String){
        //Sets the group settlement string
        val settlementPath = "$firebaseGroupId/settlements"
        currentPath = database.getReference(settlementPath)
        currentPath.setValue(settlementString)
    }

    fun setBalanceString(groupBalance: String) {
        //Sets the group balance string
        val balanceBranchPath = "$firebaseGroupId/balance"
        currentPath = database.getReference(balanceBranchPath)
        currentPath.setValue(groupBalance)
    }

}
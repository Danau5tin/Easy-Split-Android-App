package com.splitreceipt.myapplication.data

import android.widget.EditText
import com.google.firebase.database.Exclude
import com.splitreceipt.myapplication.ExpenseOverviewActivity.Companion.currentSqlGroupId
import com.splitreceipt.myapplication.helper_classes.CurrencyHelper.CurrencyDetails
import com.splitreceipt.myapplication.ExpenseOverviewActivity.Companion.roundToTwoDecimalPlace

class Expense () {

    @Exclude @set:Exclude @get:Exclude
    var sqlGroupRowId: String = ""
    @Exclude @set:Exclude @get:Exclude
    var sqlExpenseRowId: String = ""
    @Exclude @set:Exclude @get:Exclude
    var firebaseIdentifier: String = ""
    @Exclude @set:Exclude @get:Exclude
    var currencySymbol: String = ""

    var date: String = ""
    var title: String = ""
    var total: Float = 0.0F
    var paidBy: String = ""
    var contribs: String = ""
    var scanned: Boolean = false
    var lastEdit: String = ""
    var currencyCode: String = ""
    var exchRate: Float = 0.0F

    constructor(date: String, title: String, paidBy: String, scanned: Boolean, lastEdit: String, sqlExpenseRow: String="", firebaseId: String="", total: Float=0.0F) : this(){

        this.date = date
        this.total = total
        this.title = title
        this.paidBy = paidBy
        this.scanned = scanned
        this.lastEdit = lastEdit

        this.sqlExpenseRowId = sqlExpenseRow
        this.firebaseIdentifier = firebaseId
    }


    fun setUpNewExpense(totalEditText: EditText, participantList: ArrayList<ParticipantBalanceData>, currencyDetails: CurrencyDetails) {
        this.sqlGroupRowId = currentSqlGroupId!!
        createUniqueFirebaseId()
        setExpenseTotalByView(totalEditText)
        setContribString(participantList)
        setCurrencyDetails(currencyDetails)
    }

    fun setUpNewExpense(totalEditText: EditText, paidTo: String, currencyDetails: CurrencyDetails) {
        this.sqlGroupRowId = currentSqlGroupId!!
        createUniqueFirebaseId()
        setExpenseTotalByView(totalEditText)

        setContribString(paidTo)
        setCurrencyDetails(currencyDetails)
    }

    private fun createUniqueFirebaseId() {
        this.firebaseIdentifier = System.currentTimeMillis().toString()
    }

    private fun setExpenseTotalByView(totalEditText: EditText) {
        val total = totalEditText.text.toString().toFloat()
        this.total = roundToTwoDecimalPlace(total)
    }


    private fun setContribString(participantList: ArrayList<ParticipantBalanceData>) {
        val sb = StringBuilder()
        for (participant in participantList) {
            val name = participant.userName
            val nameString = "$name,"
            sb.append(nameString)
            val value = participant.userBalance.toString()
            val valString = "$value,"
            sb.append(valString)
            val paidByString = "$paidBy/"
            sb.append(paidByString)
        }
        sb.deleteCharAt(sb.lastIndex)
        this.contribs = sb.toString()
    }

    private fun setContribString(paidTo: String) {
        val contributions = "$paidTo,${this.total},$paidBy"
        this.contribs = contributions
    }


    private fun setCurrencyDetails(currencyDetails: CurrencyDetails) {
        this.currencyCode = currencyDetails.currencyCode
        this.currencySymbol = currencyDetails.currencySymbol
        this.exchRate = currencyDetails.exchangeRate
    }
}
package com.splitreceipt.myapplication.data

import android.widget.EditText
import com.google.firebase.database.Exclude
import com.splitreceipt.myapplication.CurrencyHelper
import com.splitreceipt.myapplication.CurrencyHelper.CurrencyDetails
import com.splitreceipt.myapplication.ExpenseOverviewActivity.Companion.roundToTwoDecimalPlace

class ExpenseData () {

    @Exclude @set:Exclude @get:Exclude
    var sqlRowId: String = ""
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

    constructor(date: String, title: String, paidBy: String, contributions: String,
                scanned: Boolean, lastEdit: String, exchangeRate: Float, expenseCurrencyCode: String,
                expenseCurrencySymbol: String="", sqlRow: String="", firebaseId: String="", total: Float=0.0F) : this(){

        this.date = date
        this.title = title
        this.total = total
        this.paidBy = paidBy
        this.contribs = contributions
        this.scanned = scanned
        this.lastEdit = lastEdit
        this.exchRate = exchangeRate
        this.currencyCode = expenseCurrencyCode

        this.currencySymbol = expenseCurrencySymbol
        this.sqlRowId = sqlRow
        this.firebaseIdentifier = firebaseId
    }

    constructor(date: String, title: String, paidBy: String, scanned: Boolean, lastEdit: String, sqlRow: String="", firebaseId: String="", total: Float=0.0F) : this(){

        this.date = date
        this.total = total
        this.title = title
        this.paidBy = paidBy
        this.scanned = scanned
        this.lastEdit = lastEdit

        this.sqlRowId = sqlRow
        this.firebaseIdentifier = firebaseId
    }

    fun setUpNewExpense(totalEditText: EditText, participantList: ArrayList<ParticipantBalanceData>, currencyDetails: CurrencyDetails) {
        createUniqueFirebaseId()
        setExpenseTotalByView(totalEditText)
        setContribString(participantList)
        setCurrencyDetails(currencyDetails)
    }

    private fun createUniqueFirebaseId() {
        this.firebaseIdentifier = System.currentTimeMillis().toString()
    }

    private fun setExpenseTotalByView(totalEditText: EditText) {
        val total = totalEditText.text.toString().toFloat()
        this.total = roundToTwoDecimalPlace(total)
    }


    private fun setContribString(participantList: ArrayList<ParticipantBalanceData>): String {
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
        return sb.toString()
    }

    private fun setCurrencyDetails(currencyDetails: CurrencyDetails) {
        this.currencyCode = currencyDetails.currencyCode
        this.currencySymbol = currencyDetails.currencySymbol
        this.exchRate = currencyDetails.exchangeRate
    }
}
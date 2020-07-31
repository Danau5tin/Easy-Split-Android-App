package com.splitreceipt.myapplication.data

import com.splitreceipt.myapplication.ExpenseOverviewActivity
import com.splitreceipt.myapplication.ExpenseOverviewActivity.Companion.currentCurrencySymbol
import com.splitreceipt.myapplication.SplitExpenseManuallyFragment
import com.splitreceipt.myapplication.helper_classes.DecimalPlaceFixer
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

class Transaction {

    var contributor: String? = null
    var contribValue: Float? = null
    var receiver: String? = null

    companion object {
        fun createTransactionsFromString(entireString: String) : ArrayList<Transaction>{
            val contribArray: ArrayList<Transaction> = ArrayList()
            val splitTransactions = entireString.split("/")
            for (transactionString in splitTransactions) {
                val transaction = Transaction()
                val details = transactionString.split(",")
                transaction.contributor = details[0]
                transaction.contribValue = details[1].toFloat()
                transaction.receiver = details[2]
                contribArray.add(transaction)
            }
            return contribArray
        }
    }

    fun createSettlementString(): String {
        val finalString: String
        val you = "you"
        val contributorLower = contributor!!.toLowerCase(Locale.ROOT)
        val receiverLower = receiver!!.toLowerCase(Locale.ROOT)
        val fixedVal = DecimalPlaceFixer.fixDecimalPlace(contribValue!!, false)

        finalString = when (you) {
            contributorLower -> {
                "$contributor owe $currentCurrencySymbol$fixedVal to $receiver."
            }
            receiverLower -> {
                "$contributor owes $currentCurrencySymbol$fixedVal to $receiverLower."
            }
            else -> {
                "$contributor owes $currentCurrencySymbol$fixedVal to $receiver."
            }
        }
        return finalString
    }
}
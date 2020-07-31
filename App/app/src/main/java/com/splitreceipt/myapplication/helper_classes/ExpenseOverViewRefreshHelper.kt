package com.splitreceipt.myapplication.helper_classes

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.splitreceipt.myapplication.ExpenseOverviewActivity
import com.splitreceipt.myapplication.ExpenseOverviewActivity.Companion.roundToTwoDecimalPlace
import com.splitreceipt.myapplication.R
import com.splitreceipt.myapplication.SplitExpenseManuallyFragment
import com.splitreceipt.myapplication.adapters.ExpenseOverViewAdapter
import com.splitreceipt.myapplication.data.ExpenseData
import com.splitreceipt.myapplication.data.ParticipantData
import java.util.*
import kotlin.collections.ArrayList

class ExpenseOverViewRefreshHelper(
    var context:Context,
    var expenseList: ArrayList<ExpenseData>,
    var adapter: ExpenseOverViewAdapter,
    var settlementStringTextView: TextView,
    var seeBalancesButton: Button,
    var totalNumberExpensesText: TextView,
    var totalAmountExpensesText: TextView) {

    fun refreshEverything(settlementString: String) {
        deconstructAndSetSettlementString(settlementString)
        reloadRecycler()
        refreshStatistics()
    }

    fun deconstructAndSetSettlementString(settlementString: String) {
        /*
         This function will deconstruct a settlementString and produce an ArrayList of individual settlement strings.
         After this it will identify any strings relevant to the current user and add them to a separate list which will be showcased in UI
         */
        ExpenseOverviewActivity.settlementArray.clear()
        val userDirectedSettlementIndexes: ArrayList<Int> = ArrayList()
        var indexCount = 0
        if (settlementString == context.getString(R.string.balanced)) {
            ExpenseOverviewActivity.settlementArray.add("This account is balanced") //TODO: Source this raw string from Strings.xml
            userDirectedSettlementIndexes.add(indexCount)
        } else {
            val splitIndividual = settlementString.split("/")
            for (settlement in splitIndividual) {
                val splitSettlement = settlement.split(",")
                val debtor = ParticipantData.changeNameToYou(splitSettlement[0], true)
                val value = splitSettlement[1]
                val receiver = ParticipantData.changeNameToYou(splitSettlement[2], false)
                val finalSettlementString = createSettlementString(debtor, value, receiver,
                        ExpenseOverviewActivity.currencySymbol)
                ExpenseOverviewActivity.settlementArray.add(finalSettlementString)
                if ("you" in finalSettlementString.toLowerCase(Locale.ROOT)) {
                    userDirectedSettlementIndexes.add(indexCount)
                }
                indexCount++
            }
        }
        if (userDirectedSettlementIndexes.size > 1) {
            settlementStringTextView.visibility = View.INVISIBLE
            seeBalancesButton.visibility = View.VISIBLE
            Log.i("BalancesButton", "Button IS visible. settlement list size: ${userDirectedSettlementIndexes.size}")
        }
        else {
            settlementStringTextView.visibility = View.VISIBLE
            seeBalancesButton.visibility = View.GONE
            val newString: String
            //TODO: change if statement to say: ifUserNotSettledUp
            if (userDirectedSettlementIndexes.isNotEmpty()){
                newString = ExpenseOverviewActivity.settlementArray[userDirectedSettlementIndexes[0]]
            }
            else {
                newString = "You are settled up."
            }

            settlementStringTextView.text = newString
            Log.i("BalancesButton", "Button is NOT visible. settlement list size: ${userDirectedSettlementIndexes.size}")
        }
    }

    private fun createSettlementString(debtor: String, value: String, receiver: String, currencySymbol: String): String {
        val finalString: String
        val you = "you"
        val debtorLow = debtor.toLowerCase(Locale.ROOT)
        val receiverLow = receiver.toLowerCase(Locale.ROOT)
        var fixedVal = roundToTwoDecimalPlace(value.toFloat(), false).toString()
        fixedVal = SplitExpenseManuallyFragment.addStringZerosForDecimalPlace(fixedVal)

        finalString = if (you == debtorLow) {
            "$debtor owe $currencySymbol$fixedVal to $receiver."
        } else if (you == receiverLow) {
            "$debtor owes $currencySymbol$fixedVal to $receiverLow."
        } else {
            "$debtor owes $currencySymbol$fixedVal to $receiver."
        }
        return finalString
    }

    fun reloadRecycler() {
        expenseList.clear()
        SqlDbHelper(context).loadPreviousExpenses(ExpenseOverviewActivity.currentSqlGroupId, expenseList)
        adapter.notifyDataSetChanged()
    }

    fun refreshStatistics() {
        totalNumberExpensesText.text = expenseList.size.toString()
        var expensesTotal = 0.0F
        for (expense in expenseList){
            val expenseTotal = expense.total
            val expenseExchangeRate = expense.exchangeRate
            val baseTotal = CurrencyHelper.quickExchange(expenseExchangeRate, expenseTotal)
            expensesTotal += baseTotal
            Log.i("Statistics", "Expense total: $expenseTotal, exchangeRate: $expenseExchangeRate, base total: $baseTotal... ExpensesTOTAL = $expensesTotal")
        }
        expensesTotal = roundToTwoDecimalPlace(expensesTotal)
        val total = SplitExpenseManuallyFragment.addStringZerosForDecimalPlace(expensesTotal.toString())
        val expenseTotalString = "${ExpenseOverviewActivity.currencySymbol}$total"
        totalAmountExpensesText.text = expenseTotalString
    }
}
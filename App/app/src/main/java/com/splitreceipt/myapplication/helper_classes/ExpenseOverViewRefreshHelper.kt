package com.splitreceipt.myapplication.helper_classes

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.splitreceipt.myapplication.ExpenseOverviewActivity
import com.splitreceipt.myapplication.R
import com.splitreceipt.myapplication.SplitExpenseManuallyFragment
import com.splitreceipt.myapplication.adapters.ExpenseOverViewAdapter
import com.splitreceipt.myapplication.data.ExpenseData
import com.splitreceipt.myapplication.data.Transaction
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

    private var userDirectedSettlementIndexes: ArrayList<Int> = ArrayList()

    fun refreshEverything(settlementString: String) {
        deconstructAndSetSettlementString(settlementString)
        reloadRecycler()
        refreshStatistics()
    }

    fun deconstructAndSetSettlementString(settlementString: String) {
        ExpenseOverviewActivity.settlementArray.clear()
        updateSettlementArrays(settlementString)

        if (userDirectedSettlementIndexes.size > 1) {
            settlementStringTextView.visibility = View.INVISIBLE
            seeBalancesButton.visibility = View.VISIBLE
            Log.i("BalancesButton", "Button IS visible. settlement list size: ${userDirectedSettlementIndexes.size}")
        }
        else {
            settlementStringTextView.visibility = View.VISIBLE
            seeBalancesButton.visibility = View.GONE
            val newString: String = if (userDirectedSettlementIndexes.isNotEmpty()){
                ExpenseOverviewActivity.settlementArray[userDirectedSettlementIndexes[0]]
            } else {
                "You are settled up."
            }
            settlementStringTextView.text = newString
            Log.i("BalancesButton", "Button is NOT visible. settlement list size: ${userDirectedSettlementIndexes.size}")
        }
    }

    private fun updateSettlementArrays(settlementString: String) {
        var indexCount = 0
        if (settlementString == context.getString(R.string.balanced)) {
            ExpenseOverviewActivity.settlementArray.add(context.getString(R.string.account_is_balanced))
            userDirectedSettlementIndexes.add(indexCount)
        } else {
            val settlements = Transaction.createTransactionsFromString(settlementString)
            for (settlement in settlements) {
                val finalSettlementString = settlement.createSettlementString()
                ExpenseOverviewActivity.settlementArray.add(finalSettlementString)

                if ("you" in finalSettlementString.toLowerCase(Locale.ROOT)) {
                    userDirectedSettlementIndexes.add(indexCount)
                }
                indexCount++
            }
        }

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
            val baseTotal = CurrencyExchangeHelper.quickExchange(expenseExchangeRate, expenseTotal)
            expensesTotal += baseTotal
            Log.i("Statistics", "Expense total: $expenseTotal, exchangeRate: $expenseExchangeRate, base total: $baseTotal... ExpensesTOTAL = $expensesTotal")
        }
        val total = DecimalPlaceFixer.fixDecimalPlace(expensesTotal)
        val expenseTotalString = "${ExpenseOverviewActivity.currentCurrencySymbol}$total"
        totalAmountExpensesText.text = expenseTotalString
    }
}
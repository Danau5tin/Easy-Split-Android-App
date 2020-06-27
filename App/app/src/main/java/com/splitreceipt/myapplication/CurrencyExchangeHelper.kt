package com.splitreceipt.myapplication

import com.splitreceipt.myapplication.data.ParticipantBalanceData
import com.splitreceipt.myapplication.data.ParticipantData
import com.splitreceipt.myapplication.data.SqlDbHelper

object CurrencyExchangeHelper {

    private var baseCurrency = ExpenseOverviewActivity.groupBaseCurrency!!

    fun exchangeParticipantContributionsToBase(expenseCurrency:String,
                                               participantBalDataList: ArrayList<ParticipantBalanceData>,
                                               sqlDbHelper: SqlDbHelper, priorExchangeRate: Float?) : Float {
        /*
        If the user is CREATING an expense in a different currency to the groups base currency then
        convert the contributions back into base currency for the algorithm.
         */
        if (expenseCurrency != baseCurrency) {
            // Take the exchange rate.
            val exchangeRate: Float
            if (priorExchangeRate == null) {
                // User is creating a new expense
                exchangeRate = sqlDbHelper.retrieveExchangeRate(baseCurrency, expenseCurrency)
            } else {
                // User is editing a prior expense
                exchangeRate = priorExchangeRate
            }
            // Take the participant data contributions and exchange them to base currency
            for (participant in participantBalDataList) {
                val particBalance = participant.balance
                if (particBalance> 0.0F) {
                    participant.balance = particBalance / exchangeRate
                }
            }
            return exchangeRate
        } else {
            // Base currency is the same as the expense currency.
            return 1.0F
        }
    }

    fun reversePreviousExchange(exchangeRate: Float, baseContribution: Float) : Float {
        if (exchangeRate == 1.0F) {
            return baseContribution
        } else {
            //Using the exchange rate at the time of the transaction, re-convert back to the original currency.
            return baseContribution * exchangeRate
        }
    }
}
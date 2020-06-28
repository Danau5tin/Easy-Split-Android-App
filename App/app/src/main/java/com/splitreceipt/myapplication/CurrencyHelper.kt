package com.splitreceipt.myapplication

import android.util.Log
import com.splitreceipt.myapplication.data.CurrencyUiData
import com.splitreceipt.myapplication.data.ParticipantBalanceData
import com.splitreceipt.myapplication.data.SqlDbHelper

object CurrencyHelper {

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
                Log.i("Currency", "Retrieved exchange rate for $expenseCurrency is: $exchangeRate")
            } else {
                // User is editing a prior expense
                exchangeRate = priorExchangeRate
                Log.i("Currency", "Prior exchange rate: $priorExchangeRate")
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

    fun returnUiSymbol(countryCode: String) : String{
        for (currency in currencyArray) {
            if (countryCode == currency.countryCode) {
                return currency.currencyUiSymbol
            }
        }
        return "$" //default to $
    }

    val currencyArray = arrayOf<CurrencyUiData>(
        CurrencyUiData("AUD","Australian Dollar", "$", "A$"),
        CurrencyUiData("CAD", "Canadian Dollar", "$", "CA$"),
        CurrencyUiData("EUR", "Euro", "€"),
        CurrencyUiData( "GBP", "Great British Pound","£"),
        CurrencyUiData("USD", "US Dollar", "$")
    )
}
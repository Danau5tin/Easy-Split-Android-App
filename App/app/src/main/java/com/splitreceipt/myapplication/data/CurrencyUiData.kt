package com.splitreceipt.myapplication.data

data class CurrencyUiData(var countryCode: String,
                          var currencyName: String,
                          var currencySelectorSymbol: String = countryCode,
                          var currencyUiSymbol: String = currencySelectorSymbol)
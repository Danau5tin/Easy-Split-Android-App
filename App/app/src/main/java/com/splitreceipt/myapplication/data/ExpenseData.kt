package com.splitreceipt.myapplication.data

data class ExpenseData(var date: String, var title: String, var total: Float, var paidBy: String,
                       var sqlRowId: String, var scanned: Boolean, var currencyUiSymbol: String,
                       var currencyCode: String, var exchangeRate: Float)
package com.splitreceipt.myapplication.data

data class ReceiptData(var date: String, var title: String, var total: Float, var paidBy: String,
                       var sqlRowId: String, var scanned: Boolean, var currencyUiSymbol: String, var currencyCode: String)
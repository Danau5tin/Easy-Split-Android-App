package com.splitreceipt.myapplication.data

class FirebaseExpenseData () {

    var expDate: String = ""
    var expTitle: String = ""
    var expTotal: Float = 0.0F
    var expPaidBy: String = ""
    var expContribs: String = ""
    var expScanned: Boolean = false
    var expLastEdit: String = ""
    var expCurrency: String = ""
    var expExchRate: Float = 0.0F

    constructor(date: String, title: String, total: Float, paidBy: String, contributions: String,
                scanned: Boolean, lastEdit: String, expenseCurrency: String, exchangeRate: Float) : this(){
        expDate = date
        expTitle = title
        expTotal = total
        expPaidBy = paidBy
        expContribs = contributions
        expScanned = scanned
        expLastEdit = lastEdit
        expCurrency = expenseCurrency
        expExchRate = exchangeRate
    }
}
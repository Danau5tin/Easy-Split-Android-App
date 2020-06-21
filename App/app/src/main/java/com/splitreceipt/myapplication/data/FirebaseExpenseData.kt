package com.splitreceipt.myapplication.data

class FirebaseExpenseData () {

    var expDate: String = ""
    var expTitle: String = ""
    var expTotal: Float = 0.0F
    var expPaidBy: String = ""
    var expContribs: String = ""
    var expScanned: Boolean = false

    constructor(date: String, title: String, total: Float,
                paidBy: String, contributions: String, scanned: Boolean) : this(){
        expDate = date
        expTitle = title
        expTotal = total
        expPaidBy = paidBy
        expContribs = contributions
        expScanned = scanned
    }
}
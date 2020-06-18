package com.splitreceipt.myapplication.data

class FirebaseExpenseData () {

    var expenseDate: String = ""
    var expenseTitle: String = ""
    var expenseTotal: Float = 0.0F
    var expensePaidBy: String = ""
    var expenseContribs: String = ""

    constructor(date: String, title: String, total: Float,
                paidBy: String, contributions: String) : this(){
        expenseDate = date
        expenseTitle = title
        expenseTotal = total
        expensePaidBy = paidBy
        expenseContribs = contributions
    }
}
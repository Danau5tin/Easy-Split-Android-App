package com.splitreceipt.myapplication.data

class FirebaseAccountFinancialData() {

    var accSettle: String = ""
    var accBal: String = ""

    constructor(settlement: String, balance: String): this(){

        accSettle = settlement
        accBal = balance
    }
}
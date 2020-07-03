package com.splitreceipt.myapplication.data

class FirebaseAccountFinancialData() {

    var accSettle: String = ""

    constructor(settlement: String): this(){

        accSettle = settlement
    }
}
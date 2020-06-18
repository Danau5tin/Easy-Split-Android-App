package com.splitreceipt.myapplication.data

class FirebaseAccountData() {

    var accountName: String = ""
    var accountCat: String = ""
    var accountBal: String = ""
    var accountSettle: String = ""
    var accountPart: String = ""

    constructor(name: String, cat: String, balance: String,
                settlement: String, participants: String): this(){
        accountName = name
        accountCat = cat
        accountBal = balance
        accountSettle = settlement
        accountPart = participants
    }

}
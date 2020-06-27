package com.splitreceipt.myapplication.data

class FirebaseAccountInfoData() {

    var accName: String = ""
    var accParticipants: String = ""
    var accLastImage: String = ""
    var accCurrency: String = ""

    constructor(name: String, participants: String, lastImageEdit: String, baseCurrency: String): this(){
        accName = name
        accParticipants = participants
        accLastImage = lastImageEdit
        accCurrency = baseCurrency
    }

}
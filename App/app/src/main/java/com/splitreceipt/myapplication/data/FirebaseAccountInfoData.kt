package com.splitreceipt.myapplication.data

class FirebaseAccountInfoData() {

    var accName: String = ""
    var accParticipantLastEdit: String = ""
    var accLastImage: String = ""
    var accCurrency: String = ""

    constructor(name: String, participantLastEdit: String, lastImageEdit: String, baseCurrency: String): this(){
        accName = name
        accParticipantLastEdit = participantLastEdit
        accLastImage = lastImageEdit
        accCurrency = baseCurrency
    }

}
package com.splitreceipt.myapplication.data

class FirebaseAccountInfoData() {

    var name: String = ""
    var participantLastEdit: String = ""
    var lastImageEdit: String = ""
    var baseCurrencyCode: String = ""

    constructor(name: String, participantLastEdit: String, lastImageEdit: String, baseCurrency: String): this(){
        this.name = name
        this.participantLastEdit = participantLastEdit
        this.lastImageEdit = lastImageEdit
        this.baseCurrencyCode = baseCurrency
    }

}
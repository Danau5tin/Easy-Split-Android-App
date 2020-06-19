package com.splitreceipt.myapplication.data

class FirebaseAccountInfoData() {

    var accName: String = ""
    var accCat: String = ""
    var accParticipants: String = ""


    constructor(name: String, cat: String, participants: String): this(){
        accName = name
        accCat = cat
        accParticipants = participants
    }

}
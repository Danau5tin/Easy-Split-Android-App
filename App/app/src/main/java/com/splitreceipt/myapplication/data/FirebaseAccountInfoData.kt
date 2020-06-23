package com.splitreceipt.myapplication.data

class FirebaseAccountInfoData() {

    var accName: String = ""
    var accParticipants: String = ""
    var accLastImage: String = ""

    constructor(name: String, participants: String, lastImageEdit: String): this(){
        accName = name
        accParticipants = participants
        accLastImage = lastImageEdit
    }

}
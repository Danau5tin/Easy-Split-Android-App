package com.splitreceipt.myapplication.data

class FirebaseProductData () {

    var productName: String = ""
    var productValue: String = ""
    var productOwner: String = ""

    constructor(name: String, value: String, owner: String) : this(){
        productName = name
        productValue = value
        productOwner = owner
    }


}
package com.splitreceipt.myapplication.data

import com.splitreceipt.myapplication.SplitReceiptScanFragment.Companion.ownershipEqualString

class ScannedItemizedProductData (var itemName: String, var itemValue: String) {

    var potentialError: Boolean = false
    var ownership: String = "Equal"
    var sqlRowId: String = "-1"


    constructor(itemName: String, itemValue: String,
                potentialError: Boolean, ownership: String = ownershipEqualString,
                sqlRowId: String) : this(itemName, itemValue) {
        this.itemName = itemName
        this.itemValue = itemValue
        this.potentialError = potentialError
        this.ownership = ownership
        this.sqlRowId = sqlRowId
    }
}
package com.splitreceipt.myapplication.data

data class GroupData(var name: String, var firebaseId: String,
                     var baseCurrencyCode: String, var baseCurrencySymbol: String,
                     var lastParticipantEditTime: String, var lastGroupImageEditTime: String,
                     var settlementString: String, var sqlUser: String="", var sqlGroupRowId: String="")
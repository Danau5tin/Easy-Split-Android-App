package com.splitreceipt.myapplication.data

import com.google.firebase.database.Exclude

class ParticipantBalanceData () {

    var userName: String = ""
    var userBalance: Float = 0.0F
    @Exclude @set:Exclude @get:Exclude
    var userKey: String = ""
    @Exclude @set:Exclude @get:Exclude
    var userSqlRow: String = ""

    constructor(name: String, balance: Float=0.0F, fBaseKey: String="", sqlRowId: String="") : this() {
        userName = name
        userBalance = balance
        userKey = fBaseKey
        userSqlRow = sqlRowId
    }

    fun contribsToBaseCurrency(exchangeRate: Float) {
            if (userBalance > 0.0F) {
                userBalance /= exchangeRate
            }
    }
}
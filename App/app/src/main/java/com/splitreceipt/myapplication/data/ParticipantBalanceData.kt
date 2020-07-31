package com.splitreceipt.myapplication.data

import com.google.firebase.database.Exclude
import java.util.*

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

    constructor(name: String) : this() {
        userName = name
        generateFbaseUserKey()
    }

    fun contribsToBaseCurrency(exchangeRate: Float) {
            if (userBalance > 0.0F) {
                userBalance /= exchangeRate
            }
    }

    private fun generateFbaseUserKey() {
        val timestamp = System.currentTimeMillis().toString().substring(7,9)
        val randomGen = UUID.randomUUID().toString().replace("-", "").substring(5, 7)
        this.userKey = "${userName[0]}$timestamp$randomGen"
    }
}
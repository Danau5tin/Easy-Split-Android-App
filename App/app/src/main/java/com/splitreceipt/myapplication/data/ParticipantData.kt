package com.splitreceipt.myapplication.data

class ParticipantData (var name: String, var contributionValue: String, var contributing: Boolean) {

    fun toParticipantBalanceData(): ParticipantBalanceData {
        val contributions = contributionValue.toFloat()
        return ParticipantBalanceData(name, contributions)
    }
}
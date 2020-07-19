package com.splitreceipt.myapplication.data

import android.annotation.SuppressLint
import com.splitreceipt.myapplication.ExpenseOverviewActivity

class ParticipantData (var name: String, var contributionValue: String, var contributing: Boolean) {

    companion object {
        fun contribStringToParticipantData(editContributions: String, paidBy: String) : ArrayList<ParticipantData>{
            val participantList: ArrayList<ParticipantData> = ArrayList()
            val contributionsSplit = editContributions.split("/")
            for (contrib in contributionsSplit) {
                val individualContribution = contrib.split(",")
                val participantName = individualContribution[0]
                if (participantName == paidBy) {
                    continue
                }
                val contributingValue = individualContribution[1]
                var contributing: Boolean
                contributing = contributingValue != "0.00"
                participantList.add(ParticipantData(participantName, contributingValue, contributing))
            }
            return participantList
        }

        @SuppressLint("DefaultLocale")
        fun changeNameToYou(participantName: String, capitalize: Boolean): String {
            return if (participantName == ExpenseOverviewActivity.currentSqlUser) {
                if (capitalize) {
                    "You"
                } else {
                    "you"
                }
            } else {
                if (capitalize) {
                    participantName.capitalize()
                } else {
                    participantName
                }
            }
        }
    }

    fun toParticipantBalanceData(): ParticipantBalanceData {
        val contributions = contributionValue.toFloat()
        return ParticipantBalanceData(name, contributions)
    }
}
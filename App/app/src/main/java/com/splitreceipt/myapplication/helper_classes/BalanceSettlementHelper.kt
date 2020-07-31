package com.splitreceipt.myapplication.helper_classes

import android.content.Context
import android.util.Log
import com.splitreceipt.myapplication.ExpenseOverviewActivity
import com.splitreceipt.myapplication.ExpenseOverviewActivity.Companion.firebaseDbHelper
import com.splitreceipt.myapplication.R
import com.splitreceipt.myapplication.data.Contribution
import com.splitreceipt.myapplication.data.ParticipantBalanceData
import kotlin.math.abs

class BalanceSettlementHelper(var context: Context, private var groupSqlRow: String) {

    private var settlementString: String? = null
    private var newBalanceObjects: ArrayList<ParticipantBalanceData>? = null

    private var largestNegative = ParticipantBalanceData("dummy", 0.0F)
    private var largestPositive = ParticipantBalanceData("dummy", 0.0F)

    fun updateBalancesReturnSettlement(newContributions: String) : String {
        val sqlDbHelper = SqlDbHelper(context)
        val participants = sqlDbHelper.retrieveGroupParticipants(groupSqlRow)
        val contributions = Contribution.createContributionsFromString(newContributions)
        newBalanceObjects = updateBalancesWithContributions(participants, contributions)

        firebaseDbHelper!!.updateParticipantBalances(newBalanceObjects!!) //TODO: Find a way to only update this at the end of all transactions.
        sqlDbHelper.updateSqlBalances(newBalanceObjects!!)

        val isBalanced = checkIfBalanced(newBalanceObjects!!)
        settlementString = if (!isBalanced){
            settlementAlgorithm(newBalanceObjects!!)
            } else {
            context.getString(R.string.balanced)
            }

        Log.i("Algorithm", "Settlement string created after new contributions: $settlementString")
        sqlDbHelper.updateSqlSettlementString(settlementString!!, groupSqlRow)
        return settlementString!!
    }


    fun updateFirebaseSettle(firebaseDbHelper: FirebaseDbHelper){
        if (settlementString != null && newBalanceObjects != null){
            firebaseDbHelper.setGroupFinance(settlementString!!)
        }
    }

    private fun updateBalancesWithContributions(
        participants: ArrayList<ParticipantBalanceData>, newContributions: ArrayList<Contribution>)
            : ArrayList<ParticipantBalanceData> {
        for (contribution in newContributions) {
            if (contribution.contribValue == 0.0F ||
                contribution.contributor == contribution.contributee){
                continue
            }
            for (participant in participants) {
                if (contribution.contributor == participant.userName) {
                    participant.userBalance += contribution.contribValue!!
                }
                else if (contribution.contributee == participant.userName) {
                    participant.userBalance -= contribution.contribValue!!
                }
                participant.userBalance = errorRate(participant.userBalance) //TODO: Necessary now?
            }
        }
        return participants
    }

    private fun checkIfBalanced(participantBalanceDataList: ArrayList<ParticipantBalanceData>): Boolean {
        for (participant in participantBalanceDataList) {
            participant.userBalance = errorRate(participant.userBalance)
            if (participant.userBalance != 0.0F) {
                return false
            }
        }
        return true
    }

    private fun settlementAlgorithm(participants: ArrayList<ParticipantBalanceData>): String{
        val settlementStringBuilder = StringBuilder()
        var balanced = false

        while (!balanced) {
            findLargestBalances(participants)
            //Step 2: Confirm if the largest negative balance is less than largest positive balance
            val absOfLargestNegative = abs(largestNegative.userBalance)
            val absIsLess: Boolean = absOfLargestNegative < largestPositive.userBalance
            val largestPosName = largestPositive.userName
            val largestNegName = largestNegative.userName
            settlementStringBuilder.append("$largestPosName,")

            var negativeCompleted = false
            var positiveCompleted = false

            if (absIsLess) {
                //Step 3: ABS IS LESS -> take the abs of the largest negative from the largest positive and put it into the largest negatives balance
                settlementStringBuilder.append("$absOfLargestNegative,")
                settlementStringBuilder.append("$largestNegName/")

                for (participant in participants) {
                    if (participant.userName == largestNegName) {
                        participant.userBalance = 0.0F
                        negativeCompleted = true
                    } else if (participant.userName == largestPosName) {
                        participant.userBalance -= absOfLargestNegative
                        positiveCompleted = true

                    }
                    if (negativeCompleted && positiveCompleted) {
                        break
                    }
                } }
            else {
                //Step 3: ABS IS NOT LESS -> take the largest positive balance and put it into the largest negatives balance
                val largestPositiveBalance = largestPositive.userBalance
                settlementStringBuilder.append("$largestPositiveBalance,")
                settlementStringBuilder.append("$largestNegName/")

                for (participant in participants) {
                    if (participant.userName == largestNegative.userName) {
                        participant.userBalance += largestPositiveBalance
                        negativeCompleted = true
                    } else if (participant.userName == largestPositive.userName) {
                        participant.userBalance = 0.0F
                        positiveCompleted = true
                    }
                    if (negativeCompleted && positiveCompleted) {
                        break
                    }
                } }
            balanced = checkIfBalanced(participants)
        }
        settlementStringBuilder.deleteCharAt(settlementStringBuilder.lastIndex)
        return settlementStringBuilder.toString()
    }

    private fun findLargestBalances(participants: ArrayList<ParticipantBalanceData>) {
        for (participant in participants) {

            val participantBalance = participant.userBalance
            if (participantBalance <= 0) {
                if (abs(participantBalance) > abs(largestNegative.userBalance))
                    largestNegative = participant
            } else {
                if (participantBalance > largestPositive.userBalance) {
                    largestPositive = participant
                }
            }
        }
    }

    private fun errorRate(balance: Float): Float {
        if (balance in -0.03..0.03) {
            Log.i("Algorithm", "Balance set to 0 as participant balance: $balance is in error rate range .03")
            return 0.0F
        }
        return balance
    }

}
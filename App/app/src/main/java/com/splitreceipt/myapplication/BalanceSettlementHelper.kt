package com.splitreceipt.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.splitreceipt.myapplication.ExpenseOverviewActivity.Companion.firebaseDbHelper
import com.splitreceipt.myapplication.data.SqlDbHelper
import com.splitreceipt.myapplication.data.DbManager
import com.splitreceipt.myapplication.data.DbManager.ParticipantTable.PARTICIPANT_COL_ID
import com.splitreceipt.myapplication.data.DbManager.ParticipantTable.PARTICIPANT_COL_U_BALANCE
import com.splitreceipt.myapplication.data.DbManager.ParticipantTable.PARTICIPANT_TABLE_NAME
import com.splitreceipt.myapplication.data.FirebaseDbHelper
import com.splitreceipt.myapplication.data.ParticipantBalanceData
import kotlin.math.abs

class BalanceSettlementHelper(var context: Context, private var groupSqlRow: String) {

    private var settlementString: String? = null
    var newBalanceObjects: ArrayList<ParticipantBalanceData>? = null

    fun balanceAndSettlementsFromSql(newContributions: String) : String {
        /*
        Step 1: Retrieve the participants data from SQL and parse to data class objects.
        Step 2: Use the contribution string from the returned intent to update the objects balance values.
        Step 3: Convert those objects back to a balance string.
        Step 4: Workout who owes who via the algorithm and output a settlement string.
        Step 5: Update SQL with new balances and settlement strings
        Step 6: return the newSettlementString for receipt overview Balance
         */
        val sqlDbHelper = SqlDbHelper(context)
        val newSettlementString: String
        val prevBalanceObjects = sqlDbHelper.loadPreviousBalanceToObjects(groupSqlRow)
        newBalanceObjects = updateBalancesWithContributions(prevBalanceObjects, newContributions)
        firebaseDbHelper!!.updateParticipantBalances(newBalanceObjects!!) //TODO: Find a way to only update this at the end of all transactions.
        sqlDbHelper.updateSqlBalances(newBalanceObjects!!)
        val isBalanced = checkIfBalanced(newBalanceObjects!!)
        if (!isBalanced){
            newSettlementString = settlementAlgorithm(newBalanceObjects!!)
        }
        else {
            newSettlementString = ExpenseOverviewActivity.balanced_string
        }
        Log.i("Algorithm", "Settlement string created after the algorithm has balanced everyones balances: $newSettlementString")
        settlementString = sqlDbHelper.updateSqlSettlementString(newSettlementString, groupSqlRow)
        return newSettlementString
    }

    fun updateFirebaseSettle(firebaseDbHelper: FirebaseDbHelper){
        /*
        This will take the most recent balance objects & settlement strings that're already in the
        SQL database and upload them to the Firebase DB.
         */
        if (settlementString != null && newBalanceObjects != null){
            firebaseDbHelper.setGroupFinance(settlementString!!)
        }
    }


    private fun updateBalancesWithContributions(prevBalances: ArrayList<ParticipantBalanceData>, newContributions: String): ArrayList<ParticipantBalanceData> {
        // Deconstructs each contribution from a solid string and updates the values of the relevant data class objects depending on if they have lent or borrowed.
        val splitContributions = newContributions.split("/") // ["Dan,£1.00,Marie", "Marie,£1.00,Marie"]
        for (contribution in splitContributions) {
            val contribDetails = contribution.split(",") // ["Dan", "1.00", "Marie"]
            val contribValue = contribDetails[1].toFloat()  // 1.00
            if (contribValue == 0.0F){
                continue
            }
            val contributor = contribDetails[0] // ["Dan"]
            val contributee = contribDetails[2] // ["Marie"]
            if (contributor != contributee) {
                for (participantBalanceItem in prevBalances) {
                    if (contributor == participantBalanceItem.userName) {
                        participantBalanceItem.userBalance += contribValue
                    }
                    else if (contributee == participantBalanceItem.userName) {
                        participantBalanceItem.userBalance -= contribValue
                    }
                    participantBalanceItem.userBalance = errorRate(participantBalanceItem.userBalance)
                }
            }
        }
        return prevBalances
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

    private fun settlementAlgorithm(participantBalanceDataList: ArrayList<ParticipantBalanceData>): String{
        val settlementStringBuilder = StringBuilder()
        var balanced = false

        while (!balanced) {
            var largestNegative = ParticipantBalanceData("dummy", 0.0F)
            var largestPositive = ParticipantBalanceData("dummy", 0.0F)

            //Step 1: Identify the participants with the largest negative balance & largest positive balance
            for (participant in participantBalanceDataList) {


                val participantBalance = participant.userBalance
                if (participantBalance <= 0) {
                    if (abs(participantBalance) > abs(largestNegative.userBalance))
                        largestNegative = participant
                } else {
                    if (participantBalance > largestPositive.userBalance) {
                        largestPositive = participant
                    } }
            }
            //Step 2: Confirm if the largest negative balance is less than largest positive balance
            val absIsLess: Boolean = abs(largestNegative.userBalance) < largestPositive.userBalance
            val largestPosName = largestPositive.userName
            val largestNegName = largestNegative.userName
            settlementStringBuilder.append("$largestPosName,")

            var negativeCompleted = false
            var positiveCompleted = false

            if (absIsLess) {
                //Step 3: ABS IS LESS -> take the abs of the largest negative from the largest positive and put it into the largest negatives balance
                val absOfLargestNegative = abs(largestNegative.userBalance)

                settlementStringBuilder.append("$absOfLargestNegative,")
                settlementStringBuilder.append("$largestNegName/")

                for (participant in participantBalanceDataList) {
                    if (participant.userName == largestNegName) {
                        participant.userBalance = 0.0F
                        negativeCompleted = true
                        if (negativeCompleted && positiveCompleted) {
                            break
                        }
                    } else if (participant.userName == largestPosName) {
                        participant.userBalance -= absOfLargestNegative
                        positiveCompleted = true
                        if (negativeCompleted && positiveCompleted) {
                            break
                        }
                    } } }
            else {
                //Step 3: ABS IS NOT LESS -> take the largest positive balance and put it into the largest negatives balance
                val largestPositiveBalance = largestPositive.userBalance

                settlementStringBuilder.append("$largestPositiveBalance,")
                settlementStringBuilder.append("$largestNegName/")

                for (participant in participantBalanceDataList) {

                    if (participant.userName == largestNegative.userName) {
                        participant.userBalance += largestPositiveBalance
                        negativeCompleted = true
                        if (negativeCompleted && positiveCompleted) {
                            break
                        }
                    } else if (participant.userName == largestPositive.userName) {
                        participant.userBalance = 0.0F
                        positiveCompleted = true
                        if (negativeCompleted && positiveCompleted) {
                            break
                        } }
                } }
            //Step 4: Check if the accounts are balanced
            balanced = checkIfBalanced(participantBalanceDataList)
            if (balanced) {
                settlementStringBuilder.deleteCharAt(settlementStringBuilder.lastIndex)
            }
        }
        return settlementStringBuilder.toString()
    }

    private fun stringToParticData(testBalanceString: String): ArrayList<ParticipantBalanceData> {
        // Deconstructs the solid balance string into individual participant data classes
        val participantBalanceDataList: ArrayList<ParticipantBalanceData> = ArrayList()
        val splitParticipants = testBalanceString.split("/")
        for (participant in splitParticipants){
            val nameBalSplit = participant.split(",")
            participantBalanceDataList.add(ParticipantBalanceData(nameBalSplit[0], nameBalSplit[1].toFloat()))
        }
        return participantBalanceDataList
    }


    fun errorRate(balance: Float): Float {
        if (balance in -0.05..0.05) {
            // error rate allowed.
            Log.i("Algorithm", "Balance set to 0 as participant balance: $balance is in error rate range .05")
            return 0.0F
        }
        return balance
    }

}
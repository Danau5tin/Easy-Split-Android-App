package com.splitreceipt.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import com.splitreceipt.myapplication.data.SqlDbHelper
import com.splitreceipt.myapplication.data.DbManager
import com.splitreceipt.myapplication.data.FirebaseDbHelper
import com.splitreceipt.myapplication.data.ParticipantBalanceData
import kotlin.math.abs

class BalanceSettlementHelper(var context: Context, var groupSqlRow: String) {

    var balanceString: String? = null
    private var settlementString: String? = null

    fun balanceAndSettlementsFromSql(newContributions: String) : String {
        /*
        Step 1: Convert prior balance string in SQL to data class objects.
        Step 2: Use the contribution string from the returned intent to update the objects balance values.
        Step 3: Convert those objects back to a balance string.
        Step 4: Workout who owes who via the algorithm and output a settlement string.
        Step 5: Update SQL with new balances and settlement strings
        Step 6: return the newSettlementString for receipt overview Balance
         */
        Log.i("ClassTest", "Entered")
        val newSettlementString: String
        val prevBalanceObjects = loadPreviousBalanceToObjects()
        val newBalanceObjects = updateBalancesWithContributions(prevBalanceObjects, newContributions)
        val newBalanceString = parseObjectsToString(newBalanceObjects)
        Log.i("Algorithm", "Balance string after contributions have been added: $newBalanceString \n\n")
        val isBalanced = checkIfBalanced(newBalanceObjects)
        if (!isBalanced){
            newSettlementString = settlementAlgorithm(newBalanceString)
        }
        else {
            newSettlementString = ExpenseOverviewActivity.balanced_string
        }
        Log.i("Algorithm", "Settlement string created after the algorithm has balanced everyones balances: $newSettlementString \n\n")
        updateSqlBalAndSettlementStrings(newBalanceString, newSettlementString)

        return newSettlementString
    }

    fun updateFirebaseBalAndSettle(firebaseDbHelper: FirebaseDbHelper){
        /*
        This will take the most recent balance & settlement strings that're already in the
        SQL database and upload them to the Firebase DB.
         */
        if (settlementString != null && balanceString != null){
            firebaseDbHelper.setAccountFinance(settlementString!!, balanceString!!)
        }
    }

    private fun loadPreviousBalanceToObjects(): ArrayList<ParticipantBalanceData> {
        // Loads the previous balance string from SQL and constructs each participant into an individual data class object.
        var previousBalString = ""
        val dbHelper = SqlDbHelper(context)
        val reader = dbHelper.readableDatabase
        val columns = arrayOf(DbManager.GroupTable.GROUP_COL_BALANCES)
        val selectClause = "${DbManager.GroupTable.GROUP_COL_ID} = ?"
        val selectArgs = arrayOf(groupSqlRow)
        val cursor: Cursor = reader.query(DbManager.GroupTable.GROUP_TABLE_NAME, columns, selectClause, selectArgs, null, null, null)
        val balColInd = cursor.getColumnIndexOrThrow(DbManager.GroupTable.GROUP_COL_BALANCES)
        while (cursor.moveToNext()) {
            previousBalString = cursor.getString(balColInd)
        }
        cursor.close()
        dbHelper.close()
        Log.i("Algorithm", "Balance string retrieved from the sql table: $previousBalString \n\n")

        val particBalDataList : ArrayList<ParticipantBalanceData> = ArrayList()
        val participants = previousBalString.split("/") //  ["Dan,3.00", "Marie,-3.00"]
        for (participant in participants){
            val nameValueSplit = participant.split(",")// ["Dan", "3.00"]
            val name = nameValueSplit[0] // ["Dan"]
            val balance = nameValueSplit[1] // ["£3.00"]
            particBalDataList.add(ParticipantBalanceData(name, balance.toFloat()))
        }
        return particBalDataList
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
                    if (contributor == participantBalanceItem.name) {
                        participantBalanceItem.balance += contribValue
                        participantBalanceItem.balance =
                            ExpenseOverviewActivity.roundToTwoDecimalPlace(participantBalanceItem.balance)
                    }
                    else if (contributee == participantBalanceItem.name) {
                        participantBalanceItem.balance -= contribValue
                        participantBalanceItem.balance =
                            ExpenseOverviewActivity.roundToTwoDecimalPlace(participantBalanceItem.balance)
                    }
                    participantBalanceItem.balance =
                        ExpenseOverviewActivity.errorRate(participantBalanceItem.balance)
                }
            }
        }
        return prevBalances
    }

    private fun parseObjectsToString(balancedObjects: ArrayList<ParticipantBalanceData>): String {
        // StringBuilder used to create deconstruct the data class into one solid balance string.
        val stringBuilder = StringBuilder()
        for (participant in balancedObjects) {
            val name = participant.name
            val nameString = "$name,"
            stringBuilder.append(nameString)
            val balance = participant.balance
            val balString = "$balance/"
            stringBuilder.append(balString)
        }
        stringBuilder.deleteCharAt(stringBuilder.lastIndex)
        return stringBuilder.toString()
    }

    private fun checkIfBalanced(participantBalanceDataList: ArrayList<ParticipantBalanceData>): Boolean {
        for (participant in participantBalanceDataList) {
            participant.balance = ExpenseOverviewActivity.errorRate(participant.balance)
            if (participant.balance != 0.0F) {
                return false
            }
        }
        return true
    }

    private fun settlementAlgorithm(upToDateBalanceString: String): String{
        val settlementStringBuilder = StringBuilder()
        val participantBalanceDataList: ArrayList<ParticipantBalanceData> = stringToParticData(upToDateBalanceString)
        var balanced = false

        while (!balanced) {
            var largestNegative = ParticipantBalanceData("dummy", 0.0F)
            var largestPositive = ParticipantBalanceData("dummy", 0.0F)

            //Step 1: Identify the participants with the largest negative balance & largest positive balance
            for (participant in participantBalanceDataList) {

                participant.balance =
                    ExpenseOverviewActivity.roundToTwoDecimalPlace(participant.balance)

                val participantBalance = participant.balance
                if (participantBalance <= 0) {
                    if (abs(participantBalance) > abs(largestNegative.balance))
                        largestNegative = participant
                } else {
                    if (participantBalance > largestPositive.balance) {
                        largestPositive = participant
                    } }
            }
            //Step 2: Confirm if the largest negative balance is less than largest positive balance
            val absIsLess: Boolean = abs(largestNegative.balance) < largestPositive.balance
            val largestPosName = largestPositive.name
            val largestNegName = largestNegative.name
            settlementStringBuilder.append("$largestPosName,")

            var negativeCompleted = false
            var positiveCompleted = false

            if (absIsLess) {
                //Step 3: ABS IS LESS -> take the abs of the largest negative from the largest positive and put it into the largest negatives balance
                val absOfLargestNegative = abs(largestNegative.balance)

                settlementStringBuilder.append("$absOfLargestNegative,")
                settlementStringBuilder.append("$largestNegName/")

                for (participant in participantBalanceDataList) {
                    if (participant.name == largestNegName) {
                        participant.balance = 0.0F
                        negativeCompleted = true
                        if (negativeCompleted && positiveCompleted) {
                            break
                        }
                    } else if (participant.name == largestPosName) {
                        participant.balance -= absOfLargestNegative
                        positiveCompleted = true
                        if (negativeCompleted && positiveCompleted) {
                            break
                        }
                    } } }
            else {
                //Step 3: ABS IS NOT LESS -> take the largest positive balance and put it into the largest negatives balance
                val largestPositiveBalance = largestPositive.balance

                settlementStringBuilder.append("$largestPositiveBalance,")
                settlementStringBuilder.append("$largestNegName/")

                for (participant in participantBalanceDataList) {

                    if (participant.name == largestNegative.name) {
                        participant.balance += largestPositiveBalance
                        negativeCompleted = true
                        if (negativeCompleted && positiveCompleted) {
                            break
                        }
                    } else if (participant.name == largestPositive.name) {
                        participant.balance = 0.0F
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

    private fun updateSqlBalAndSettlementStrings(balString: String, settlementStr: String) {

        balanceString = balString
        settlementString = settlementStr

        val dbHelper = SqlDbHelper(context)
        val writer = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DbManager.GroupTable.GROUP_COL_BALANCES, balString)
            put(DbManager.GroupTable.GROUP_COL_SETTLEMENTS, settlementStr)
        }
        val where = "${DbManager.GroupTable.GROUP_COL_ID} = ?"
        val whereargs = arrayOf(groupSqlRow)
        val id = writer.update(DbManager.GroupTable.GROUP_TABLE_NAME, values, where, whereargs)
        if (id != -1) {
            Log.i("TEST", "Successful upload of new balance string & settlement string")
        }
        dbHelper.close()
    }

}
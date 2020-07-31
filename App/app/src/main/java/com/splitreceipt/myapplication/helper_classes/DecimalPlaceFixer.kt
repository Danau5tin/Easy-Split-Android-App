package com.splitreceipt.myapplication.helper_classes

import com.splitreceipt.myapplication.ExpenseOverviewActivity
import java.math.RoundingMode
import java.text.DecimalFormat

object DecimalPlaceFixer {

    fun fixDecimalPlace(number: Float, ceiling: Boolean=true): String {
        val rounded = roundToTwoDecimalPlace(number, ceiling).toString()
        return addStringZerosForDecimalPlace(rounded)
    }

    fun roundToTwoDecimalPlace(number: Float, ceiling: Boolean=true): Float {
        val df = DecimalFormat("#.##")
        if (ceiling) {
            df.roundingMode = RoundingMode.CEILING
        } else {
            df.roundingMode = RoundingMode.FLOOR
        }
        return df.format(number).toFloat()
    }

    fun addStringZerosForDecimalPlace(value: String): String {
        var fixedValue = ""
        if (value.contains(".")) {
            if (value.length - value.indexOf(".") == 2) {
                fixedValue = value + "0"
                return fixedValue }
            else { return value } }
        else { return "$value.00"
        }}
}
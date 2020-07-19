package com.splitreceipt.myapplication.helper_classes

object DateSelectionCleaner {

    fun returnDateString(year: Int, monthOfYear: Int, dayOfMonth: Int): String{
        val dayString =
            cleanDay(
                dayOfMonth
            )
        val monthString =
            cleanMonth(
                monthOfYear
            )
        val yearString: String = year.toString()
        return "$dayString/$monthString/$yearString"
    }

    private fun cleanDay(dayOfMonth: Int): String {
        val dayString: String
        val day = dayOfMonth.toString()
        dayString = if (day.length == 1) {
            "0$day"
        } else {
            day
        }
        return dayString
    }

    private fun cleanMonth(monthOfYear: Int): String {
        val monthOf = monthOfYear + 1
        val monthString: String
        monthString = if (monthOf in 1..9) {
            "0$monthOf"
        } else {
            "$monthOf"
        }
        return monthString
    }


}
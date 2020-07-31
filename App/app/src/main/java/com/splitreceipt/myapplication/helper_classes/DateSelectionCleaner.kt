package com.splitreceipt.myapplication.helper_classes

import android.content.Context
import com.splitreceipt.myapplication.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateSelectionCleaner {

    fun retrieveTodaysDate(context: Context): String {
        val date = LocalDate.now()
        return date.format(DateTimeFormatter.ofPattern(context.getString(R.string.date_format_dd_MM_yyyy))).toString()
    }

    fun returnDateString(year: Int, monthOfYear: Int, dayOfMonth: Int): String{
        val dayString = cleanDay(dayOfMonth)
        val monthString = cleanMonth(monthOfYear)
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
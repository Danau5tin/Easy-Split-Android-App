package com.splitreceipt.myapplication

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.splitreceipt.myapplication.data.ReceiptData
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class ReceiptOverViewAdapter(var receiptList: ArrayList<ReceiptData>, var todaysDate: Date?) : RecyclerView.Adapter<ReceiptOverViewAdapter.ReceiptOverviewViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptOverviewViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.receipt_overview_recy_row, parent, false)
        return ReceiptOverviewViewHolder(view)
    }

    override fun getItemCount(): Int {
        return receiptList.size
    }

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: ReceiptOverviewViewHolder, position: Int) {
        val dateOfReceipt = receiptList.get(position).date
        val dateFormat = SimpleDateFormat("dd/MM/yyyy")
        val date = dateFormat.parse(dateOfReceipt)
        val difference = abs(date!!.time - todaysDate!!.time)
        val differenceDates = difference / (24 * 60 * 60 * 1000)
        val dayDiff = differenceDates.toString()
        var dayDiffText = ""
        if (dayDiff == "0"){
            dayDiffText = "Today"
        } else if (dayDiff == "1"){
            dayDiffText = "Yesterday"
        } else {
            dayDiffText = "$dayDiff days ago"
        }
        holder.daysAgoTextView.text = dayDiffText

        holder.receiptTitleTextView.text = receiptList.get(position).title

        val totalToString = receiptList.get(position).total.toString()
        val totalFixedString = SplitReceiptManuallyFragment.fixDecimalPlace(totalToString)
        val totalString = "£$totalFixedString" //TODO: Ensure the correct currency symbol used here is the users preference
        holder.receiptTotalTextView.text = totalString

        val paidBy = receiptList.get(position).paidBy
        val paidByString = "Paid by $paidBy"
        holder.receiptPaidByTextView.text = paidByString
    }

    class ReceiptOverviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val receiptTitleTextView: TextView = itemView.findViewById(R.id.receiptTitle)
        val receiptTotalTextView: TextView = itemView.findViewById(R.id.receiptTotal)
        val receiptPaidByTextView: TextView = itemView.findViewById(R.id.receiptPaidBy)
        val daysAgoTextView: TextView = itemView.findViewById(R.id.receiptDaysAgo)

    }
}
package com.splitreceipt.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReceiptOverViewAdapter : RecyclerView.Adapter<ReceiptOverViewAdapter.ReceiptOverviewViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptOverviewViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.receipt_overview_recy_row, parent, false)
        return ReceiptOverviewViewHolder(view)
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: ReceiptOverviewViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    class ReceiptOverviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val receiptTitleTextView: TextView = itemView.findViewById(R.id.receiptTitle)
        val receiptTotalTextView: TextView = itemView.findViewById(R.id.receiptTotal)
        val receiptPaidByTextView: TextView = itemView.findViewById(R.id.receiptPaidBy)
        val daysAgoTextView: TextView = itemView.findViewById(R.id.receiptDaysAgo)

    }
}
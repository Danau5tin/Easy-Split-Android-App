package com.splitreceipt.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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

class ReceiptOverViewAdapter(var receiptList: ArrayList<ReceiptData>) : RecyclerView.Adapter<ReceiptOverViewAdapter.ReceiptOverviewViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptOverviewViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.receipt_overview_recy_row, parent, false)
        return ReceiptOverviewViewHolder(parent.context, view)
    }

    override fun getItemCount(): Int {
        return receiptList.size
    }

    override fun onBindViewHolder(holder: ReceiptOverviewViewHolder, position: Int) {
        holder.receiptTitleTextView.text = receiptList[position].title

        val totalToString = receiptList[position].total.toString()
        val totalFixedString = SplitReceiptManuallyFragment.fixDecimalPlace(totalToString)
        val totalString = "£$totalFixedString" //TODO: Ensure the correct currency symbol used here is the users preference
        holder.receiptTotalTextView.text = totalString

        val paidBy = ReceiptOverviewActivity.changeNameToYou(receiptList[position].paidBy, true)
        val paidByString = "$paidBy paid $totalString"
        holder.receiptPaidByTextView.text = paidByString

        holder.total = totalFixedString
        holder.paidBy = paidBy
        holder.sqlId = receiptList[position].sqlRowId
    }

    class ReceiptOverviewViewHolder(var context: Context, itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val receiptTitleTextView: TextView = itemView.findViewById(R.id.receiptTitle)
        val receiptTotalTextView: TextView = itemView.findViewById(R.id.receiptTotal)
        val receiptPaidByTextView: TextView = itemView.findViewById(R.id.receiptPaidBy)
        var sqlId = "-1"
        var total = "0"
        var paidBy = "unknown"

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val intent = Intent(context, ExpenseViewActivity::class.java)
            intent.putExtra(ExpenseViewActivity.expenseTitleIntentString, receiptTitleTextView.text.toString())
            intent.putExtra(ExpenseViewActivity.expenseTotalIntentString, total)
            intent.putExtra(ExpenseViewActivity.expenseSqlIntentString, sqlId)
            intent.putExtra(ExpenseViewActivity.expensePaidByIntentString, paidBy)
            context.startActivity(intent)
        }

    }
}
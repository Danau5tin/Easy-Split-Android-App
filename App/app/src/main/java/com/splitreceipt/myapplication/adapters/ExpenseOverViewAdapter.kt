package com.splitreceipt.myapplication.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.splitreceipt.myapplication.ExpenseOverviewActivity
import com.splitreceipt.myapplication.R
import com.splitreceipt.myapplication.SplitExpenseManuallyFragment
import com.splitreceipt.myapplication.data.ReceiptData
import kotlin.collections.ArrayList

class ExpenseOverViewAdapter(var receiptList: ArrayList<ReceiptData>, var onRecRowClick: OnReceRowClick) : RecyclerView.Adapter<ExpenseOverViewAdapter.ReceiptOverviewViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptOverviewViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.expense_overview_recy_row, parent, false)
        return ReceiptOverviewViewHolder(
            parent.context,
            view,
            onRecRowClick
        )
    }

    override fun getItemCount(): Int {
        return receiptList.size
    }

    override fun onBindViewHolder(holder: ReceiptOverviewViewHolder, position: Int) {
        holder.receiptTitleTextView.text = receiptList[position].title

        val totalToString = receiptList[position].total.toString()
        val totalFixedString =
            SplitExpenseManuallyFragment.addStringZerosForDecimalPlace(
                totalToString
            )
        val currencyUiSymbol = receiptList[position].currencyUiSymbol
        val totalString = "$currencyUiSymbol$totalFixedString"
        holder.receiptTotalTextView.text = totalString

        val paidBy =
            ExpenseOverviewActivity.changeNameToYou(
                receiptList[position].paidBy,
                true
            )
        val paidByString = "$paidBy paid $totalString"
        holder.receiptPaidByTextView.text = paidByString

        holder.total = totalFixedString
        holder.paidBy = paidBy
        holder.sqlId = receiptList[position].sqlRowId
        holder.uiSymbol = currencyUiSymbol
        holder.scanned = receiptList[position].scanned
    }

    class ReceiptOverviewViewHolder(var context: Context, itemView: View, var onRecRowClick: OnReceRowClick) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val receiptTitleTextView: TextView = itemView.findViewById(R.id.receiptTitle)
        val receiptTotalTextView: TextView = itemView.findViewById(R.id.receiptTotal)
        val receiptPaidByTextView: TextView = itemView.findViewById(R.id.receiptPaidBy)
        var sqlId = "-1"
        var total = "0"
        var paidBy = "unknown"
        var uiSymbol = ""
        var currencyCode = ""
        var scanned = false

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val title = receiptTitleTextView.text.toString()
            onRecRowClick.onRowClick(adapterPosition, title, total, sqlId, paidBy, uiSymbol, currencyCode, scanned)
        }

    }

    interface OnReceRowClick{
        fun onRowClick(pos: Int, title: String="", total: String="", sqlID: String="",
                       paidBy: String="", uiSymbol: String="", currencyCode: String="",
                       scanned: Boolean = false)
    }
}
package com.splitreceipt.myapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.splitreceipt.myapplication.R

class BalanceOverviewAdapter(var settlementList: ArrayList<String>, var balRowClick: BalanceRowClick): RecyclerView.Adapter<BalanceOverviewAdapter.BalanceViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BalanceViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.balance_overview_row, parent, false)
        return BalanceViewHolder(
            view,
            balRowClick
        )
    }

    override fun getItemCount(): Int {
        return settlementList.size
    }

    override fun onBindViewHolder(holder: BalanceViewHolder, position: Int) {
        holder.settlementTextView.text = settlementList[position]
    }

    class BalanceViewHolder(itemView: View, balRowClick: BalanceRowClick) : RecyclerView.ViewHolder(itemView) {
        val settlementTextView: TextView = itemView.findViewById(R.id.balanceRowText)
        private val settleButton: Button = itemView.findViewById(R.id.balanceSettleButton)

        init {
            settleButton.setOnClickListener {
                balRowClick.onBalRowClick(adapterPosition)
            }
        }
    }

    interface BalanceRowClick {
        fun onBalRowClick(pos: Int)
    }
}
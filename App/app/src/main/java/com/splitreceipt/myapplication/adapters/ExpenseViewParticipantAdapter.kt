package com.splitreceipt.myapplication.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.splitreceipt.myapplication.R
import com.splitreceipt.myapplication.data.ExpenseAdapterData

class ExpenseViewParticipantAdapter(var contributionList: ArrayList<ExpenseAdapterData>): RecyclerView.Adapter<ExpenseViewParticipantAdapter.ExpenseViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.expense_recy_row, parent, false)
        return ExpenseViewHolder(
            parent.context,
            view
        )
    }

    override fun getItemCount(): Int {
        return contributionList.size
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        if (contributionList[position].value == "0.00"){
            holder.tickImageView.setImageDrawable(ContextCompat.getDrawable(holder.context,
                R.drawable.vector_cross_red
            ))
        }
        else{
            holder.tickImageView.setImageDrawable(ContextCompat.getDrawable(holder.context,
                R.drawable.vector_tick_green
            ))
        }
        holder.expenseText.text = contributionList[position].contribString
    }

    class ExpenseViewHolder(var context: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tickImageView: ImageView = itemView.findViewById(R.id.tickImageView)
        val expenseText: TextView = itemView.findViewById(R.id.expenseContributionRecyRow)
    }
}
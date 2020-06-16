package com.splitreceipt.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.splitreceipt.myapplication.data.ScannedItemizedProductData

class ExpenseViewProductAdapter(var itemizedProductData: ArrayList<ScannedItemizedProductData>) : RecyclerView.Adapter<ExpenseViewProductAdapter.ExpenseProductViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewProductAdapter.ExpenseProductViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.scanned_product_expense_recy_row, parent, false)
        return ExpenseProductViewHolder(view)
    }

    override fun getItemCount(): Int {
        return itemizedProductData.size
    }

    override fun onBindViewHolder(holder: ExpenseProductViewHolder, position: Int) {
        holder.productTitleText.text = itemizedProductData[position].itemName
        holder.productValueText.text = itemizedProductData[position].itemValue
        holder.productOwnershipText.text = itemizedProductData[position].ownership
    }

    class ExpenseProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productTitleText: TextView = itemView.findViewById(R.id.expenseProductTitle)
        val productValueText: TextView = itemView.findViewById(R.id.expenseProductValue)
        val productOwnershipText: TextView = itemView.findViewById(R.id.expenseProductOwnership)
    }
}
package com.splitreceipt.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItemizedRecyclerAdapter : RecyclerView.Adapter<ItemizedRecyclerAdapter.ItemizedViewholder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemizedViewholder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.itemised_receipt_recy_row, parent, false)
        return ItemizedViewholder(view)
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(
        holder: ItemizedViewholder,
        position: Int
    ) {
        TODO("Not yet implemented")
    }

    class ItemizedViewholder(itemView: View) : RecyclerView.ViewHolder(itemView){

        val itemWhomeSpinner: Spinner = itemView.findViewById(R.id.itemisedWhomSpinner)
        val itemNameText: TextView = itemView.findViewById(R.id.itemisedItemName)
        val itemValueText: TextView = itemView.findViewById(R.id.itemisedItemValue)

    }
}
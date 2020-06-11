package com.splitreceipt.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.splitreceipt.myapplication.data.ScannedItemizedProductData

class NewScannedReceiptRecyclerAdapter(var participantList: ArrayList<String>,
                                       var itemizedList: ArrayList<ScannedItemizedProductData>,
                                       var passedOnScannedClick: onScannedClick) :
                    RecyclerView.Adapter<NewScannedReceiptRecyclerAdapter.ItemizedViewholder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemizedViewholder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.itemised_new_scanned_receipt_recy_row,
                                            parent, false)
        return ItemizedViewholder(parent.context, view, passedOnScannedClick)
    }

    override fun getItemCount(): Int {
        return itemizedList.size
    }

    override fun onBindViewHolder(holder: ItemizedViewholder, position: Int) {
        //TODO: Add as many radio buttons as there are participants by wrapping the radio group in a horizontal scroll view and programatically add the radio buttons.
        //TODO: Set the checked radio button according to the data classes ownership status
        holder.itemNameText.text = itemizedList[position].itemName
        holder.itemCurrencyText.text = "£"
        holder.itemValueText.text = itemizedList[position].itemValue
        if (itemizedList[position].potentialError){
            holder.constraintHolder.setBackgroundResource(R.drawable.confident_not_scanned_row_otline)
        } else {
            holder.constraintHolder.setBackgroundResource(R.drawable.confident_scanned_row_outline)
        }
    }

    class ItemizedViewholder(val context: Context, itemView: View, var onScannedClick: onScannedClick) : RecyclerView.ViewHolder(itemView), View.OnClickListener{
        val itemNameText: TextView = itemView.findViewById(R.id.scannedItemName)
        val itemCurrencyText: TextView = itemView.findViewById(R.id.scannedCurrencySymbol)
        val itemValueText: TextView = itemView.findViewById(R.id.scannedItemValue)
        val constraintHolder: ConstraintLayout = itemView.findViewById(R.id.scannedConstraint)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            onScannedClick.editProduct(adapterPosition)
        }
    }

    interface onScannedClick{
        fun editProduct(position: Int)
    }
}
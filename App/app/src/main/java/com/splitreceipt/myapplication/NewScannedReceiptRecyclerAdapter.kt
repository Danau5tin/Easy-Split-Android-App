package com.splitreceipt.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.splitreceipt.myapplication.NewReceiptCreationActivity.Companion.currencySymbol
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
        holder.itemNameText.text = itemizedList[position].itemName
        holder.itemCurrencyText.text = currencySymbol
        holder.itemValueText.text = itemizedList[position].itemValue
        if (itemizedList[position].potentialError){
            holder.constraintHolder.setBackgroundResource(R.drawable.confident_not_scanned_row_otline)
        } else {
            holder.constraintHolder.setBackgroundResource(R.drawable.confident_scanned_row_outline)
        }
        holder.radioGroup.removeAllViews()
        for (participant in participantList) {
            val radioButton = RadioButton(holder.context)
            radioButton.text = participant
            radioButton.id = View.generateViewId()
            val rprms = RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT)
            radioButton.isChecked = itemizedList[position].ownership == participant
            holder.radioGroup.addView(radioButton, rprms)
        }

    }

    class ItemizedViewholder(val context: Context, itemView: View, var onScannedClick: onScannedClick) : RecyclerView.ViewHolder(itemView), View.OnClickListener{
        val itemNameText: TextView = itemView.findViewById(R.id.scannedItemName)
        val itemCurrencyText: TextView = itemView.findViewById(R.id.scannedCurrencySymbol)
        val itemValueText: TextView = itemView.findViewById(R.id.scannedItemValue)
        val constraintHolder: ConstraintLayout = itemView.findViewById(R.id.scannedConstraint)
        val radioGroup: RadioGroup = itemView.findViewById(R.id.scannedRadioGroup)

        init {
            itemView.setOnClickListener(this)
            radioGroup.setOnCheckedChangeListener { group, checkedId ->
                onScannedClick.radioChecked(adapterPosition, group, checkedId)
            }
        }

        override fun onClick(v: View?) {
            onScannedClick.editProduct(adapterPosition)
        }
    }

    interface onScannedClick{
        fun editProduct(position: Int)
        fun radioChecked(position: Int, group: RadioGroup, checkedId: Int)
    }
}
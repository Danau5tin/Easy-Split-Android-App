package com.splitreceipt.myapplication

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.splitreceipt.myapplication.NewExpenseCreationActivity.Companion.currencySymbol
import com.splitreceipt.myapplication.data.ParticipantData

class NewManualExpenseRecyclerAdapter(var participantList: ArrayList<ParticipantData>, private var onRecyInt: OnRecyRowCheked) : RecyclerView.Adapter<NewManualExpenseRecyclerAdapter.ItemizedViewholder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemizedViewholder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.itemised_new_manual_expense_recy_row, parent, false)
        return ItemizedViewholder(view, onRecyInt)
    }

    override fun getItemCount(): Int {
        return participantList.size
    }

    override fun onBindViewHolder(holder: ItemizedViewholder, position: Int) {
        holder.participantCheckBox.text = participantList[position].name

        if (participantList[position].contributing){
            holder.participantCheckBox.isChecked = true
        } else {
            holder.participantCheckBox.isChecked = false
        }

        holder.participantCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked){
                holder.onRec.onRecyUnCheck(holder.adapterPosition)
            } else {
                holder.onRec.onRecyChecked(holder.adapterPosition)
            }
        }

        holder.currencySymbol.text = currencySymbol
        holder.participantContribution.text = participantList[position].contributionValue
        Log.i("DEBUG", "Position of adapter: $position")
    }

    class ItemizedViewholder(itemView: View, onRecyInt: OnRecyRowCheked) : RecyclerView.ViewHolder(itemView){

        val participantCheckBox: CheckBox = itemView.findViewById(R.id.participantCheckbox)
        val participantContribution: TextView = itemView.findViewById(R.id.participantContribution)
        val currencySymbol: TextView = itemView.findViewById(R.id.symbolText)
        val onRec = onRecyInt
    }

    interface OnRecyRowCheked {
        fun onRecyUnCheck(pos: Int)
        fun onRecyChecked(pos: Int)
    }
}

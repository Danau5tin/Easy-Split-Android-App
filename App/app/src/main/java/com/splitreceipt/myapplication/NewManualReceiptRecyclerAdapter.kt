package com.splitreceipt.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.splitreceipt.myapplication.data.ParticipantData

class NewManualReceiptRecyclerAdapter(var participantList: ArrayList<ParticipantData>) : RecyclerView.Adapter<NewManualReceiptRecyclerAdapter.ItemizedViewholder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemizedViewholder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.itemised_new_manual_receipt_recy_row, parent, false)
        return ItemizedViewholder(view)
    }

    override fun getItemCount(): Int {
        return participantList.size
    }

    override fun onBindViewHolder(
        holder: ItemizedViewholder,
        position: Int
    ) {
        holder.participantCheckBox.text = participantList.get(position).name
        holder.participantCheckBox.isChecked = true
        holder.particpantContribution.text = participantList.get(position).contribution
    }

    class ItemizedViewholder(itemView: View) : RecyclerView.ViewHolder(itemView){

        val participantCheckBox: CheckBox = itemView.findViewById(R.id.participantCheckbox)
        val particpantContribution: TextView = itemView.findViewById(R.id.participantContribution)

    }
}
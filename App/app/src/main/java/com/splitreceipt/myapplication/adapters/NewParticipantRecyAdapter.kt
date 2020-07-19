package com.splitreceipt.myapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.splitreceipt.myapplication.R

class NewParticipantRecyAdapter(var participantList: ArrayList<String>, var onPartRowClickInter: OnPartRowClick) : RecyclerView.Adapter<NewParticipantRecyAdapter.ParticipantViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.new_participant_recy_row, parent, false)
        return ParticipantViewHolder(
            view,
            onPartRowClickInter
        )
    }

    override fun getItemCount(): Int {
        return participantList.size
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.nameTextView.text = participantList[position]
    }

    class ParticipantViewHolder(itemView: View, onPartRowClickInter: OnPartRowClick) : RecyclerView.ViewHolder(itemView) {

        val nameTextView: TextView = itemView.findViewById(R.id.newAccountParticipantName)
        val deleteparticipantButton: ImageButton = itemView.findViewById(R.id.newParticipantDelete)

        init {
            deleteparticipantButton.setOnClickListener{
                onPartRowClickInter.onRowclick(adapterPosition)
            }
        }
    }

    interface OnPartRowClick{
        fun onRowclick(position: Int)
    }

}
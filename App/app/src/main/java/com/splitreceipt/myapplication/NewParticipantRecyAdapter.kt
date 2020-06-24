package com.splitreceipt.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NewParticipantRecyAdapter(var participantList: ArrayList<String>, var onPartRowClickInter: onPartRowClick) : RecyclerView.Adapter<NewParticipantRecyAdapter.ParticipantViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.new_participant_recy_row, parent, false)
        return ParticipantViewHolder(view, onPartRowClickInter)
    }

    override fun getItemCount(): Int {
        return participantList.size
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.nameTextView.text = participantList[position]
    }

    class ParticipantViewHolder(itemView: View, onPartRowClickInter: onPartRowClick) : RecyclerView.ViewHolder(itemView) {

        val nameTextView: TextView = itemView.findViewById(R.id.newAccountParticipantName)
        val deleteparticipantButton: ImageButton = itemView.findViewById(R.id.newParticipantDelete)

        init {
            deleteparticipantButton.setOnClickListener{
                onPartRowClickInter.onRowclick(adapterPosition)
            }
        }
    }

    interface onPartRowClick{
        fun onRowclick(position: Int)
    }

}
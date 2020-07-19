package com.splitreceipt.myapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.splitreceipt.myapplication.R
import com.splitreceipt.myapplication.data.ParticipantBalanceData

class InviteParticipantRecyAdapter(var participantList: ArrayList<ParticipantBalanceData>, var inviteRecyClick: InviteRecyClick) :
    RecyclerView.Adapter<InviteParticipantRecyAdapter.InviteViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.invite_participant_recy_row, parent, false)
        return InviteViewHolder(
            view,
            inviteRecyClick
        )
    }

    override fun getItemCount(): Int {
        return participantList.size
    }

    override fun onBindViewHolder(holder: InviteViewHolder, position: Int) {
        holder.participantNameText.text = participantList[position].userName
    }

    class InviteViewHolder(itemView: View, inviteRecyClick: InviteRecyClick) : RecyclerView.ViewHolder(itemView) {
        val participantNameText: TextView = itemView.findViewById(R.id.participantName)
        private val optionsButton: ImageView = itemView.findViewById(R.id.inviteParticipantEdit)

        init {
            optionsButton.setOnClickListener {
                inviteRecyClick.onInviteRowClick(adapterPosition)
            }
        }
    }

    interface InviteRecyClick{
        fun onInviteRowClick(pos: Int)
    }
}
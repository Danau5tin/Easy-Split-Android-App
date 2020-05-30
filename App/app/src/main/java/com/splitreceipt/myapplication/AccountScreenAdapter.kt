package com.splitreceipt.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AccountScreenAdapter : RecyclerView.Adapter<AccountScreenAdapter.AccountViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.account_screen_recy_row, parent, false)
        return AccountViewHolder(view)
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val titleText: TextView = itemView.findViewById(R.id.titleText)
        val descrText: TextView = itemView.findViewById(R.id.descText)

    }
}
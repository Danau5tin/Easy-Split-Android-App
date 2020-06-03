package com.splitreceipt.myapplication

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.splitreceipt.myapplication.data.AccountData

class AccountScreenAdapter(val accountNameList: ArrayList<AccountData>) : RecyclerView.Adapter<AccountScreenAdapter.AccountViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.account_screen_recy_row, parent, false)
        return AccountViewHolder(parent.context, view)
    }

    override fun getItemCount(): Int {
        return accountNameList.size
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.titleText.text = accountNameList.get(position).name
        holder.sqlId = accountNameList.get(position).sqlId
        holder.firebaseId = accountNameList.get(position).firebaseId
    }

    class AccountViewHolder(var context: Context, itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val titleText: TextView = itemView.findViewById(R.id.titleText)
        var sqlId: String = "0"
        var firebaseId: String = "0"

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val intent = Intent(context, ReceiptOverviewActivity::class.java)
            intent.putExtra(AccountScreenActivity.sqlIntentString, sqlId)
            intent.putExtra("FirebaseID", sqlId)
            context.startActivity(intent)
        }
    }
}